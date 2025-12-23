package org.example.application.service.coordinator;

import lombok.extern.slf4j.Slf4j;
import org.example.domain.model.valueobject.GradeRange;
import org.example.domain.model.valueobject.RegionCustomerMatrix;
import org.example.application.service.coordinator.provider.GroupRatioProvider;
import org.example.domain.model.valueobject.DeliveryCombination;
import org.example.domain.model.valueobject.DeliveryMethodType;
import org.example.domain.service.algorithm.ColumnWiseAdjustmentService;
import org.example.domain.service.algorithm.GroupSplittingDistributionService;
import org.example.domain.service.algorithm.SingleLevelDistributionService;
import org.example.domain.service.delivery.DeliveryCombinationParser;
import org.example.shared.util.AllocationMatrixUtils;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 分配算法选择器。
 * <p>
 * 职责：解析投放组合、选择并执行具体分配算法。
 * 支持通过 GroupRatioProvider 扩展带权重扩展类型的分组比例计算。
 * </p>
 *
 * @author Robin
 * @since 2025-12-22
 */
@Slf4j
@Component
public class AllocationAlgorithmSelector {

    private final DeliveryCombinationParser combinationParser;
    private final SingleLevelDistributionService singleLevelService;
    private final ColumnWiseAdjustmentService columnWiseService;
    private final GroupSplittingDistributionService groupSplittingService;
    private final List<GroupRatioProvider> groupRatioProviders;

    public AllocationAlgorithmSelector(DeliveryCombinationParser combinationParser,
                                       SingleLevelDistributionService singleLevelService,
                                       ColumnWiseAdjustmentService columnWiseService,
                                       GroupSplittingDistributionService groupSplittingService,
                                       List<GroupRatioProvider> groupRatioProviders) {
        this.combinationParser = combinationParser;
        this.singleLevelService = singleLevelService;
        this.columnWiseService = columnWiseService;
        this.groupSplittingService = groupSplittingService;
        this.groupRatioProviders = groupRatioProviders != null ? groupRatioProviders : Collections.emptyList();
    }

    /**
     * 执行分配算法。
     * <p>
     * 根据投放组合解析结果，自动选择并执行合适的分配算法：
     * <ul>
     *   <li>SINGLE_LEVEL：单区域分配，适用于区域数 ≤ 1 的场景</li>
     *   <li>COLUMN_WISE：多区域无权重分配，适用于无分组比例的多区域场景</li>
     *   <li>GROUP_SPLITTING：多区域带权重分配，适用于有分组比例的多区域场景</li>
     * </ul>
     * </p>
     *
     * @param customerMatrix     客户矩阵，包含各区域的客户数分布
     * @param targetAmount       目标投放量（条）
     * @param deliveryMethod     投放方式，如 "按档位投放"、"按档位扩展投放"
     * @param deliveryEtype      扩展类型，如 "市场类型"、"诚信互助小组"
     * @param tag                标签，如 "城网"、"农网"
     * @param maxGrade           最高档位，如 "D1"（为空则默认 D30）
     * @param minGrade           最低档位，如 "D30"（为空则默认 D1）
     * @param groupRatios        分组比例映射，key 为分组名，value 为比例（可选，优先使用）
     * @param regionGroupMapping 区域分组映射，key 为区域名，value 为分组名（可选，优先使用）
     * @param extraInfo          额外信息，用于 GroupRatioProvider 计算比例
     * @return 分配结果，包含成功标志、区域列表、客户矩阵和分配矩阵
     * @example
     * <pre>{@code
     * // 执行市场类型分组分配
     * Map<String, BigDecimal> ratios = Map.of("城网", new BigDecimal("0.4"), "农网", new BigDecimal("0.6"));
     * Map<String, String> mapping = Map.of("城区", "城网", "郊区", "农网");
     * 
     * AllocationResult result = selector.execute(
     *     customerMatrix, new BigDecimal("10000"),
     *     "按档位扩展投放", "市场类型", "城网",
     *     "D1", "D30", ratios, mapping, null
     * );
     * 
     * if (result.isSuccess()) {
     *     BigDecimal[][] allocation = result.getAllocationMatrix();
     * }
     * }</pre>
     */
    public AllocationResult execute(RegionCustomerMatrix customerMatrix,
                                    BigDecimal targetAmount,
                                    String deliveryMethod,
                                    String deliveryEtype,
                                    String tag,
                                    String maxGrade,
                                    String minGrade,
                                    Map<String, BigDecimal> groupRatios,
                                    Map<String, String> regionGroupMapping,
                                    Map<String, Object> extraInfo) {
        
        if (customerMatrix == null || customerMatrix.isEmpty()) {
            return AllocationResult.failure("区域客户矩阵为空");
        }

        // 解析投放组合
        DeliveryCombination combination;
        try {
            combination = combinationParser.parse(deliveryMethod, deliveryEtype, tag);
        } catch (IllegalArgumentException ex) {
            log.error("组合解析失败: {}", ex.getMessage());
            return AllocationResult.failure("组合解析失败: " + ex.getMessage());
        }

        if (!combination.isImplemented()) {
            return AllocationResult.failure("策略未实现: " + combination.getMethodType().getDisplayName());
        }
        if (!combination.requiresStatistics()) {
            return AllocationResult.failure("该组合不需要策略执行");
        }
        if (combination.getMethodType() == DeliveryMethodType.PRICE_SEGMENT) {
            return AllocationResult.failure("按价位段自选投放由独立服务处理");
        }

        // 构建矩阵数据
        List<RegionCustomerMatrix.Row> rows = customerMatrix.getRows();
        List<String> regions = rows.stream()
                .map(RegionCustomerMatrix.Row::getRegion)
                .collect(Collectors.toList());
        BigDecimal[][] matrix = buildMatrix(rows);

        // 构建 GradeRange
        GradeRange gradeRange = GradeRange.of(maxGrade, minGrade);

        // 基于 GradeRange 验证零行（只检查范围内的列）
        String zeroRowCheck = AllocationMatrixUtils.validateNoZeroRowsInRange(regions, matrix, gradeRange);
        if (zeroRowCheck != null) {
            log.error("分配算法执行前检查失败: {}", zeroRowCheck);
            return AllocationResult.failure(zeroRowCheck);
        }

        // 使用 provider 计算分组比例（如果未传入）
        Map<String, BigDecimal> finalGroupRatios = groupRatios;
        Map<String, String> finalRegionGroupMapping = regionGroupMapping;
        
        if ((finalGroupRatios == null || finalGroupRatios.isEmpty()) && deliveryEtype != null) {
            for (GroupRatioProvider provider : groupRatioProviders) {
                if (provider.supports(deliveryEtype)) {
                    finalGroupRatios = provider.calculateGroupRatios(deliveryEtype, regions, matrix, extraInfo);
                    finalRegionGroupMapping = provider.getRegionGroupMapping(deliveryEtype, regions);
                    log.debug("使用 {} 计算分组比例: {}", provider.getClass().getSimpleName(), finalGroupRatios);
                    break;
                }
            }
        }

        // 选择并执行算法
        BigDecimal roundedTarget = targetAmount.setScale(0, RoundingMode.HALF_UP);
        AlgorithmType type = decideAlgorithmType(rows.size(), finalGroupRatios, finalRegionGroupMapping);
        log.debug("选择算法: {} (regions={}, groupRatios={})", type, rows.size(), 
                finalGroupRatios != null ? finalGroupRatios.keySet() : "null");

        BigDecimal[][] finalAllocation;
        final Map<String, String> mappingForLambda = finalRegionGroupMapping;
        switch (type) {
            case SINGLE_LEVEL:
                finalAllocation = singleLevelService.distribute(regions, matrix, roundedTarget, gradeRange);
                break;
            case GROUP_SPLITTING:
                finalAllocation = groupSplittingService.distribute(
                        regions, matrix, roundedTarget, gradeRange,
                        region -> mappingForLambda.getOrDefault(region, region),
                        finalGroupRatios);
                break;
            case COLUMN_WISE:
            default:
                finalAllocation = columnWiseService.distribute(regions, matrix, roundedTarget, gradeRange, null);
                break;
        }

        // 确保范围外的档位为0（算法应该已经保证，但这里再次确认）
        enforceGradeRangeBounds(finalAllocation, gradeRange);

        BigDecimal actualAmount = AllocationMatrixUtils.calculateTotalAmount(finalAllocation, matrix);
        log.info("分配完成，算法={}, regions={}, target={}, actual={}", 
                type, regions.size(), roundedTarget, actualAmount);

        return AllocationResult.success(regions, matrix, finalAllocation);
    }

    /**
     * 执行分配算法（简化版）。
     * <p>
     * 不使用 GroupRatioProvider 自动计算分组比例，需要显式传入 groupRatios 和 regionGroupMapping。
     * </p>
     *
     * @param customerMatrix     客户矩阵
     * @param targetAmount       目标投放量（条）
     * @param deliveryMethod     投放方式
     * @param deliveryEtype      扩展类型
     * @param tag                标签
     * @param maxGrade           最高档位
     * @param minGrade           最低档位
     * @param groupRatios        分组比例映射
     * @param regionGroupMapping 区域分组映射
     * @return 分配结果
     * @see #execute(RegionCustomerMatrix, BigDecimal, String, String, String, String, String, Map, Map, Map)
     */
    public AllocationResult execute(RegionCustomerMatrix customerMatrix,
                                    BigDecimal targetAmount,
                                    String deliveryMethod,
                                    String deliveryEtype,
                                    String tag,
                                    String maxGrade,
                                    String minGrade,
                                    Map<String, BigDecimal> groupRatios,
                                    Map<String, String> regionGroupMapping) {
        return execute(customerMatrix, targetAmount, deliveryMethod, deliveryEtype, tag,
                maxGrade, minGrade, groupRatios, regionGroupMapping, null);
    }

    private AlgorithmType decideAlgorithmType(int regionCount,
                                               Map<String, BigDecimal> groupRatios,
                                               Map<String, String> regionGroupMapping) {
        if (regionCount <= 1) {
            return AlgorithmType.SINGLE_LEVEL;
        }
        if (groupRatios == null || groupRatios.isEmpty()) {
            return AlgorithmType.COLUMN_WISE;
        }
        if (regionGroupMapping == null || regionGroupMapping.isEmpty()) {
            return AlgorithmType.COLUMN_WISE;
        }
        Set<String> uniqueGroups = new HashSet<>(regionGroupMapping.values());
        if (uniqueGroups.size() <= 1) {
            return AlgorithmType.COLUMN_WISE;
        }
        return AlgorithmType.GROUP_SPLITTING;
    }

    private BigDecimal[][] buildMatrix(List<RegionCustomerMatrix.Row> rows) {
        BigDecimal[][] matrix = new BigDecimal[rows.size()][];
        for (int i = 0; i < rows.size(); i++) {
            BigDecimal[] grades = rows.get(i).getGrades();
            matrix[i] = grades == null ? new BigDecimal[0] : Arrays.copyOf(grades, grades.length);
        }
        return matrix;
    }

    /**
     * 确保分配矩阵在GradeRange范围外的档位为0。
     * <p>
     * 算法应该已经保证范围外为0，但这里再次确认，防止算法bug导致范围外有分配值。
     * </p>
     *
     * @param allocationMatrix 分配矩阵（会被原地修改）
     * @param gradeRange       档位范围
     */
    private void enforceGradeRangeBounds(BigDecimal[][] allocationMatrix, GradeRange gradeRange) {
        if (allocationMatrix == null || gradeRange == null) {
            return;
        }

        int maxIndex = gradeRange.getMaxIndex();
        int minIndex = gradeRange.getMinIndex();

        for (BigDecimal[] row : allocationMatrix) {
            if (row == null) {
                continue;
            }

            // 清零范围外的档位
            for (int i = 0; i < maxIndex && i < row.length; i++) {
                if (row[i] != null && row[i].compareTo(BigDecimal.ZERO) != 0) {
                    log.warn("检测到范围外档位有分配值: 索引={}, 值={}, 已清零", i, row[i]);
                    row[i] = BigDecimal.ZERO;
                }
            }
            for (int i = minIndex + 1; i < row.length; i++) {
                if (row[i] != null && row[i].compareTo(BigDecimal.ZERO) != 0) {
                    log.warn("检测到范围外档位有分配值: 索引={}, 值={}, 已清零", i, row[i]);
                    row[i] = BigDecimal.ZERO;
                }
            }
        }
    }

    private enum AlgorithmType {
        SINGLE_LEVEL,
        COLUMN_WISE,
        GROUP_SPLITTING
    }

    /**
     * 分配结果。
     * <p>
     * 封装分配算法执行结果，包含成功/失败状态、区域列表、客户矩阵和分配矩阵。
     * </p>
     *
     * @example
     * <pre>{@code
     * AllocationResult result = selector.execute(...);
     * if (result.isSuccess()) {
     *     List<String> regions = result.getRegions();
     *     BigDecimal[][] allocation = result.getAllocationMatrix();
     *     // 处理分配结果
     * } else {
     *     log.error("分配失败: {}", result.getMessage());
     * }
     * }</pre>
     */
    public static class AllocationResult {
        private final boolean success;
        private final String message;
        private final List<String> regions;
        private final BigDecimal[][] customerMatrix;
        private final BigDecimal[][] allocationMatrix;

        private AllocationResult(boolean success, String message, List<String> regions,
                                 BigDecimal[][] customerMatrix, BigDecimal[][] allocationMatrix) {
            this.success = success;
            this.message = message;
            this.regions = regions;
            this.customerMatrix = customerMatrix;
            this.allocationMatrix = allocationMatrix;
        }

        public static AllocationResult success(List<String> regions, BigDecimal[][] customerMatrix,
                                               BigDecimal[][] allocationMatrix) {
            return new AllocationResult(true, null, regions, customerMatrix, allocationMatrix);
        }

        public static AllocationResult failure(String message) {
            return new AllocationResult(false, message, null, null, null);
        }

        public boolean isSuccess() { return success; }
        public String getMessage() { return message; }
        public List<String> getRegions() { return regions; }
        public BigDecimal[][] getCustomerMatrix() { return customerMatrix; }
        public BigDecimal[][] getAllocationMatrix() { return allocationMatrix; }
    }
}
