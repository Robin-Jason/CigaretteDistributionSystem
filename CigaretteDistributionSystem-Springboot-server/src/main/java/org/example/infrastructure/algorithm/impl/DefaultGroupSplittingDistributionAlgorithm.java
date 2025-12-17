package org.example.infrastructure.algorithm.impl;

import lombok.extern.slf4j.Slf4j;
import org.example.infrastructure.algorithm.ColumnWiseAdjustmentAlgorithm;
import org.example.infrastructure.algorithm.GroupSplittingDistributionAlgorithm;
import org.example.infrastructure.algorithm.SingleLevelDistributionAlgorithm;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

/**
 * 默认分组拆 target 算法实现（历史最新算法，实现已迁移到领域服务，仅用于对比与回归验证）。
 * <p>
 * 支持任意数量的分组与权重，常用于：
 * <ul>
 *     <li>城网/农网比例</li>
 *     <li>诚信互助小组（上千个分组）</li>
 *     <li>其他标签、组织结构</li>
 * </ul>
 * <p>
 * 算法选择逻辑：
 * - 如果分组内只有1个区域 → 使用SINGLE_LEVEL算法
 * - 如果分组内有多个区域 → 使用COLUMN_WISE算法
 */
@Slf4j
@Component
public class DefaultGroupSplittingDistributionAlgorithm implements GroupSplittingDistributionAlgorithm {

    private static final int GRADE_COUNT = 30;
    private static final MathContext MATH_CONTEXT = new MathContext(16, RoundingMode.HALF_UP);
    
    private final SingleLevelDistributionAlgorithm singleLevelAlgorithm;
    private final ColumnWiseAdjustmentAlgorithm columnWiseAlgorithm;
    
    @Autowired
    public DefaultGroupSplittingDistributionAlgorithm(SingleLevelDistributionAlgorithm singleLevelAlgorithm,
                                                      ColumnWiseAdjustmentAlgorithm columnWiseAlgorithm) {
        this.singleLevelAlgorithm = singleLevelAlgorithm;
        this.columnWiseAlgorithm = columnWiseAlgorithm;
    }

    @Override
    public BigDecimal[][] distribute(List<String> regions,
                                     BigDecimal[][] customerMatrix,
                                     BigDecimal targetAmount,
                                     Function<String, String> groupingFunction,
                                     Map<String, BigDecimal> groupRatios) {
        BigDecimal normalizedTarget = roundToWholeNumber(targetAmount);
        if (regions == null || regions.isEmpty()
                || customerMatrix == null
                || normalizedTarget == null
                || normalizedTarget.compareTo(BigDecimal.ZERO) <= 0) {
            log.warn("GroupSplitting 输入非法，返回空矩阵");
            return new BigDecimal[0][0];
        }
        validateMatrixDimensions(regions.size(), customerMatrix);

        // 防御性检查：如果存在某个区域30个档位客户数全部为0，则该卷烟无法进行有效分配
        for (int r = 0; r < customerMatrix.length; r++) {
            BigDecimal[] row = customerMatrix[r];
            boolean allZero = true;
            for (int g = 0; g < row.length; g++) {
                if (row[g] != null && row[g].compareTo(BigDecimal.ZERO) > 0) {
                    allZero = false;
                    break;
                }
            }
            if (allZero) {
                throw new IllegalStateException(
                        "GroupSplitting 分配失败：区域索引 " + r + " 的30个档位客户数全部为0，已停止本卷烟分配以避免死循环");
            }
        }

        Function<String, String> safeGrouping = groupingFunction != null
                ? groupingFunction
                : region -> "DEFAULT";

        Map<String, GroupContext> groups = buildGroups(regions, safeGrouping);
        if (groups.isEmpty()) {
            log.warn("GroupSplitting 未获取到任何分组，返回空矩阵");
            return new BigDecimal[0][0];
        }

        Map<String, BigDecimal> weights = computeGroupWeights(groups.keySet(), groupRatios);
        BigDecimal totalWeight = weights.values().stream()
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        if (totalWeight.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("所有分组权重均为 0，无法拆分目标量");
        }

        BigDecimal[][] finalMatrix = initMatrix(regions.size());
        for (Map.Entry<String, GroupContext> entry : groups.entrySet()) {
            String groupId = entry.getKey();
            GroupContext context = entry.getValue();
            BigDecimal weight = weights.get(groupId);
            BigDecimal groupTarget = roundToWholeNumber(
                    normalizedTarget.multiply(weight.divide(totalWeight, MATH_CONTEXT)));

            BigDecimal[][] groupMatrix = extractSubMatrix(context.indices(), customerMatrix);
            List<String> groupRegions = extractGroupRegions(context.indices(), regions);
            BigDecimal[][] groupAllocation = runStandaloneAlgorithm(groupRegions, groupMatrix, groupTarget);
            copyGroupResult(groupAllocation, context.indices(), finalMatrix);
        }

        enforceMonotonicConstraint(finalMatrix);
        return finalMatrix;
    }

    private Map<String, GroupContext> buildGroups(List<String> regions,
                                                  Function<String, String> groupingFunction) {
        Map<String, GroupContext> groups = new HashMap<>();
        for (int i = 0; i < regions.size(); i++) {
            String region = regions.get(i);
            String groupId = groupingFunction.apply(region);
            if (groupId == null || groupId.trim().isEmpty()) {
                groupId = "UNSPECIFIED";
            }
            groups.computeIfAbsent(groupId, key -> new GroupContext())
                    .indices().add(i);
        }
        return groups;
    }

    private Map<String, BigDecimal> computeGroupWeights(Iterable<String> groupIds,
                                                        Map<String, BigDecimal> groupRatios) {
        Map<String, BigDecimal> weights = new HashMap<>();
        for (String groupId : groupIds) {
            BigDecimal ratio = groupRatios != null ? groupRatios.get(groupId) : null;
            if (ratio == null || ratio.compareTo(BigDecimal.ZERO) <= 0) {
                ratio = BigDecimal.ONE;
            }
            weights.put(groupId, ratio);
        }
        return weights;
    }

    private BigDecimal[][] extractSubMatrix(List<Integer> indices,
                                            BigDecimal[][] customerMatrix) {
        BigDecimal[][] subMatrix = new BigDecimal[indices.size()][GRADE_COUNT];
        for (int i = 0; i < indices.size(); i++) {
            subMatrix[i] = Arrays.copyOf(customerMatrix[indices.get(i)], GRADE_COUNT);
        }
        return subMatrix;
    }

    /**
     * 对分组运行独立分配算法
     * 根据分组内区域数选择算法：
     * - 如果只有1个区域 → SINGLE_LEVEL
     * - 如果有多个区域 → COLUMN_WISE
     * 
     * @param groupRegions 分组内的区域列表
     * @param customerMatrix 分组内的客户数矩阵
     * @param targetAmount 分组的目标预投放量
     * @return 分配矩阵
     */
    private BigDecimal[][] runStandaloneAlgorithm(List<String> groupRegions,
                                                  BigDecimal[][] customerMatrix,
                                                  BigDecimal targetAmount) {
        int regionCount = groupRegions.size();
        
        if (regionCount <= 1) {
            // 分组内只有1个区域，使用SINGLE_LEVEL算法
            log.debug("分组内只有1个区域，使用SINGLE_LEVEL算法");
            return singleLevelAlgorithm.distribute(groupRegions, customerMatrix, targetAmount);
            } else {
            // 分组内有多个区域，使用COLUMN_WISE算法
            log.debug("分组内有{}个区域，使用COLUMN_WISE算法", regionCount);
            return columnWiseAlgorithm.distribute(groupRegions, customerMatrix, targetAmount, null);
    }
    }
    
    /**
     * 提取分组内的区域列表
     * 
     * @param indices 分组内的区域索引列表
     * @param regions 完整的区域列表
     * @return 分组内的区域列表
     */
    private List<String> extractGroupRegions(List<Integer> indices, List<String> regions) {
        List<String> groupRegions = new ArrayList<>();
        for (Integer index : indices) {
            if (index >= 0 && index < regions.size()) {
                groupRegions.add(regions.get(index));
            }
        }
        return groupRegions;
    }


    private void copyGroupResult(BigDecimal[][] groupResult,
                                 List<Integer> indices,
                                 BigDecimal[][] finalMatrix) {
        for (int localRow = 0; localRow < groupResult.length; localRow++) {
            int originalIndex = indices.get(localRow);
            finalMatrix[originalIndex] = Arrays.copyOf(groupResult[localRow], GRADE_COUNT);
        }
    }

    private void enforceMonotonicConstraint(BigDecimal[][] matrix) {
        for (BigDecimal[] row : matrix) {
            for (int grade = 1; grade < GRADE_COUNT; grade++) {
                if (row[grade].compareTo(row[grade - 1]) > 0) {
                    row[grade] = row[grade - 1];
                }
            }
        }
    }

    private BigDecimal[][] initMatrix(int rowCount) {
        BigDecimal[][] matrix = new BigDecimal[rowCount][GRADE_COUNT];
        for (int i = 0; i < rowCount; i++) {
            Arrays.fill(matrix[i] = new BigDecimal[GRADE_COUNT], BigDecimal.ZERO);
        }
        return matrix;
    }

    private void validateMatrixDimensions(int regionCount, BigDecimal[][] matrix) {
        if (matrix.length != regionCount) {
            throw new IllegalArgumentException("客户矩阵行数与区域数不一致");
        }
        for (BigDecimal[] row : matrix) {
            if (row == null || row.length != GRADE_COUNT) {
                throw new IllegalArgumentException("客户矩阵列数必须为 30");
            }
            for (int i = 0; i < row.length; i++) {
                if (row[i] == null) {
                    row[i] = BigDecimal.ZERO;
                }
            }
        }
    }

    private BigDecimal roundToWholeNumber(BigDecimal value) {
        if (value == null) {
            return BigDecimal.ZERO;
        }
        return value.setScale(0, RoundingMode.HALF_UP);
    }

    private static class GroupContext {
        private final List<Integer> indices = new ArrayList<>();

        List<Integer> indices() {
            return indices;
        }
    }
}

