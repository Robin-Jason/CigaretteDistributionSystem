package org.example.domain.service.algorithm.impl;

import org.example.domain.model.valueobject.GradeRange;
import org.example.domain.service.algorithm.ColumnWiseAdjustmentService;
import org.example.domain.service.algorithm.GroupSplittingDistributionService;
import org.example.domain.service.algorithm.SingleLevelDistributionService;

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
 * 多区域带权重分配领域服务实现。
 * <p>
 * 封装了多区域带权重分配的核心算法逻辑，不依赖于Spring框架或持久化层。
 * 该实现复制自 {@link org.example.infrastructure.algorithm.impl.DefaultGroupSplittingDistributionAlgorithm}，
 * 移除了Spring注解和日志依赖，保持算法逻辑完全一致。
 * </p>
 *
 * @author Robin
 * @version 1.0
 * @since 2025-12-14
 */
public class GroupSplittingDistributionServiceImpl implements GroupSplittingDistributionService {

    private static final int GRADE_COUNT = 30;
    private static final MathContext MATH_CONTEXT = new MathContext(16, RoundingMode.HALF_UP);
    
    private final SingleLevelDistributionService singleLevelService;
    private final ColumnWiseAdjustmentService columnWiseService;
    
    public GroupSplittingDistributionServiceImpl(SingleLevelDistributionService singleLevelService,
                                                  ColumnWiseAdjustmentService columnWiseService) {
        this.singleLevelService = singleLevelService;
        this.columnWiseService = columnWiseService;
    }

    @Override
    public BigDecimal[][] distribute(List<String> regions,
                                     BigDecimal[][] customerMatrix,
                                     BigDecimal targetAmount,
                                     GradeRange gradeRange,
                                     Function<String, String> groupingFunction,
                                     Map<String, BigDecimal> groupRatios) {
        BigDecimal normalizedTarget = roundToWholeNumber(targetAmount);
        if (regions == null || regions.isEmpty()
                || customerMatrix == null
                || normalizedTarget == null
                || normalizedTarget.compareTo(BigDecimal.ZERO) <= 0) {
            return new BigDecimal[0][0];
        }

        // 处理 null 参数，使用默认范围
        GradeRange range = gradeRange != null ? gradeRange : GradeRange.full();
        int maxIndex = range.getMaxIndex();
        int minIndex = range.getMinIndex();

        validateMatrixDimensions(regions.size(), customerMatrix);

        // 防御性检查：如果存在某个区域在范围内所有档位客户数全部为0，则该卷烟无法进行有效分配
        for (int r = 0; r < customerMatrix.length; r++) {
            BigDecimal[] row = customerMatrix[r];
            boolean allZero = true;
            for (int g = maxIndex; g <= minIndex && g < row.length; g++) {
                if (row[g] != null && row[g].compareTo(BigDecimal.ZERO) > 0) {
                    allZero = false;
                    break;
                }
            }
            if (allZero) {
                throw new IllegalStateException(
                        "GroupSplitting 分配失败：区域索引 " + r + " 在档位范围内客户数全部为0，已停止本卷烟分配以避免死循环");
            }
        }

        Function<String, String> safeGrouping = groupingFunction != null
                ? groupingFunction
                : region -> "DEFAULT";

        Map<String, GroupContext> groups = buildGroups(regions, safeGrouping);
        if (groups.isEmpty()) {
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
            // 传递 GradeRange 给内部算法
            BigDecimal[][] groupAllocation = runStandaloneAlgorithm(groupRegions, groupMatrix, groupTarget, range);
            copyGroupResult(groupAllocation, context.indices(), finalMatrix);
        }

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
     * @param gradeRange 档位范围
     * @return 分配矩阵
     */
    private BigDecimal[][] runStandaloneAlgorithm(List<String> groupRegions,
                                                  BigDecimal[][] customerMatrix,
                                                  BigDecimal targetAmount,
                                                  GradeRange gradeRange) {
        int regionCount = groupRegions.size();
        
        if (regionCount <= 1) {
            // 分组内只有1个区域，使用SINGLE_LEVEL算法
            return singleLevelService.distribute(groupRegions, customerMatrix, targetAmount, gradeRange);
        } else {
            // 分组内有多个区域，使用COLUMN_WISE算法
            return columnWiseService.distribute(groupRegions, customerMatrix, targetAmount, gradeRange, null);
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

