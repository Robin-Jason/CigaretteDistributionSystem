package org.example.strategy.orchestrator;

import lombok.extern.slf4j.Slf4j;
import org.example.algorithm.ColumnWiseAdjustmentAlgorithm;
import org.example.algorithm.GroupSplittingDistributionAlgorithm;
import org.example.algorithm.SingleLevelDistributionAlgorithm;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Decides which algorithm to run based on the actual target region set (not the delivery combination).
 */
@Slf4j
@Component
public class DistributionAlgorithmEngine {

    public static final String EXTRA_GROUP_RATIOS = "groupRatios";
    public static final String EXTRA_REGION_GROUP_MAPPING = "regionGroupMapping";
    public static final String EXTRA_SEGMENT_ORDER = "segmentOrder";

    private final SingleLevelDistributionAlgorithm singleLevelAlgorithm;
    private final ColumnWiseAdjustmentAlgorithm columnWiseAlgorithm;
    private final GroupSplittingDistributionAlgorithm groupSplittingAlgorithm;
    private final List<GroupRatioProvider> groupRatioProviders;

    public DistributionAlgorithmEngine(SingleLevelDistributionAlgorithm singleLevelAlgorithm,
                                       ColumnWiseAdjustmentAlgorithm columnWiseAlgorithm,
                                       GroupSplittingDistributionAlgorithm groupSplittingAlgorithm,
                                       List<GroupRatioProvider> groupRatioProviders) {
        this.singleLevelAlgorithm = singleLevelAlgorithm;
        this.columnWiseAlgorithm = columnWiseAlgorithm;
        this.groupSplittingAlgorithm = groupSplittingAlgorithm;
        this.groupRatioProviders = groupRatioProviders != null ? groupRatioProviders : Collections.emptyList();
    }

    public StrategyExecutionResult execute(StrategyContext context, StrategyExecutionRequest request) {
        RegionCustomerMatrix customerMatrix = context.getCustomerMatrix();
        if (customerMatrix == null || customerMatrix.isEmpty()) {
            return StrategyExecutionResult.failure("区域客户矩阵为空");
        }

        List<RegionCustomerMatrix.Row> rows = customerMatrix.getRows();
        List<String> regions = rows.stream()
                .map(RegionCustomerMatrix.Row::getRegion)
                .collect(Collectors.toList());
        BigDecimal[][] matrix = buildMatrix(rows);
        GradeRange gradeRange = resolveGradeRange(request.getMaxGrade(), request.getMinGrade());
        BigDecimal[][] compactCustomerMatrix = compactMatrixToRange(matrix, gradeRange);

        // 使用可扩展的比例提供者计算分组比例和映射
        Map<String, BigDecimal> groupRatios = calculateGroupRatios(request, regions, compactCustomerMatrix);
        Map<String, String> regionGroupMapping = calculateRegionGroupMapping(request, regions);
        Comparator<Integer> customOrder = extractSegmentOrderComparator(request, regions);

        AlgorithmType type = decideAlgorithmType(rows.size(), groupRatios, regionGroupMapping);
        log.debug("DistributionAlgorithmEngine 选择算法: {} (regions={}, groupRatios={}, uniqueGroups={})",
                type, rows.size(), groupRatios.keySet(), 
                regionGroupMapping.values().stream().collect(java.util.stream.Collectors.toSet()));

        BigDecimal roundedTarget = roundTargetAmount(context.getTargetAmount());
        BigDecimal[][] allocationCompact;
        switch (type) {
            case SINGLE_LEVEL:
                allocationCompact = runSingleLevel(regions, compactCustomerMatrix, roundedTarget);
                break;
            case GROUP_SPLITTING:
                allocationCompact = runGroupSplitting(
                        regions,
                        compactCustomerMatrix,
                        roundedTarget,
                        region -> regionGroupMapping.getOrDefault(region, region),
                        groupRatios
                );
                break;
            case COLUMN_WISE:
            default:
                allocationCompact = runColumnWise(regions, compactCustomerMatrix, roundedTarget, customOrder);
                break;
        }
        BigDecimal[][] finalAllocation = expandMatrixFromRange(allocationCompact, gradeRange);
        // matrix 构建自 RegionCustomerMatrix（已包含上浮等处理），此处作为“实际使用的客户矩阵”回传
        return StrategyExecutionResult.success(regions, matrix, finalAllocation);
    }

    private BigDecimal[][] runSingleLevel(List<String> regions,
                                          BigDecimal[][] customerMatrix,
                                          BigDecimal targetAmount) {
        BigDecimal[][] allocation = singleLevelAlgorithm.distribute(regions, customerMatrix, targetAmount);
        enforceMonotonicRows(allocation);
        return allocation;
    }

    private BigDecimal[][] runColumnWise(List<String> regions,
                                         BigDecimal[][] customerMatrix,
                                         BigDecimal targetAmount,
                                         Comparator<Integer> segmentComparator) {
        BigDecimal[][] allocation = columnWiseAlgorithm.distribute(regions, customerMatrix, targetAmount, segmentComparator);
        enforceMonotonicRows(allocation);
        return allocation;
    }

    private BigDecimal[][] runGroupSplitting(List<String> regions,
                                             BigDecimal[][] customerMatrix,
                                             BigDecimal targetAmount,
                                             Function<String, String> groupingFunction,
                                             Map<String, BigDecimal> groupRatios) {
        BigDecimal[][] allocation = groupSplittingAlgorithm.distribute(
                regions,
                customerMatrix,
                targetAmount,
                groupingFunction,
                groupRatios
        );
        enforceMonotonicRows(allocation);
        return allocation;
    }

    /**
     * 确保每个区域的分配矩阵严格非递增（D30 >= ... >= D1）。
     * 如遇到低档位大于高档位的情况，将其截断为上一个档位值，
     * 并把多余的量回补到 D30，既保证单调性，也保持总量不变。
     */
    private void enforceMonotonicRows(BigDecimal[][] allocationMatrix) {
        if (allocationMatrix == null) {
            return;
        }
        for (BigDecimal[] row : allocationMatrix) {
            if (row == null || row.length == 0) {
                continue;
            }
            normalizeNulls(row);
            for (int i = 1; i < row.length; i++) {
                if (row[i].compareTo(row[i - 1]) > 0) {
                    BigDecimal excess = row[i].subtract(row[i - 1]);
                    row[i] = row[i - 1];
                    redistributeExcessBackward(row, i - 1, excess);
                }
            }
        }
    }

    private void normalizeNulls(BigDecimal[] row) {
        for (int i = 0; i < row.length; i++) {
            if (row[i] == null) {
                row[i] = BigDecimal.ZERO;
            }
        }
    }

    private void redistributeExcessBackward(BigDecimal[] row, int startIndex, BigDecimal excess) {
        if (excess.compareTo(BigDecimal.ZERO) <= 0) {
            return;
        }
        BigDecimal remaining = excess;
        for (int idx = startIndex; idx > 0 && remaining.compareTo(BigDecimal.ZERO) > 0; idx--) {
            BigDecimal upper = row[idx - 1];
            BigDecimal current = row[idx];
            BigDecimal capacity = upper.subtract(current);
            if (capacity.compareTo(BigDecimal.ZERO) <= 0) {
                continue;
            }
            BigDecimal delta = remaining.min(capacity);
            row[idx] = row[idx].add(delta);
            remaining = remaining.subtract(delta);
        }
        if (remaining.compareTo(BigDecimal.ZERO) > 0) {
            row[0] = row[0].add(remaining);
        }
    }

    /**
     * 决定使用哪种算法
     * 
     * 算法选择逻辑：
     * 1. 如果待投放区域只有1个 → SINGLE_LEVEL
     * 2. 如果存在带权重扩展类型，检查带权重扩展类型的区域种类：
     *    - 如果只有1种（如只有城网） → COLUMN_WISE（退化）
     *    - 如果有多种，且笛卡尔积前区域数 > 1 → GROUP_SPLITTING
     * 3. 如果没有带权重扩展类型 → COLUMN_WISE
     * 
     * @param regionCount 待投放区域数量
     * @param groupRatios 分组比例（如果不为空，说明存在带权重扩展类型）
     * @param regionGroupMapping 区域到分组的映射（用于检查带权重扩展类型的区域种类）
     * @return 算法类型
     */
    private AlgorithmType decideAlgorithmType(int regionCount, 
                                               Map<String, BigDecimal> groupRatios,
                                               Map<String, String> regionGroupMapping) {
        // 步骤1：检查待投放区域数量
        if (regionCount <= 1) {
            log.debug("算法选择：待投放区域只有1个，选择SINGLE_LEVEL");
            return AlgorithmType.SINGLE_LEVEL;
        }
        
        // 步骤2：检查是否存在带权重扩展类型
        if (groupRatios.isEmpty()) {
            log.debug("算法选择：不存在带权重扩展类型，选择COLUMN_WISE");
            return AlgorithmType.COLUMN_WISE;
        }
        
        // 步骤3：检查带权重扩展类型的区域种类
        // 统计不同的分组ID数量（即带权重扩展类型的区域种类数）
        java.util.Set<String> uniqueGroups = new java.util.HashSet<>(regionGroupMapping.values());
        
        if (uniqueGroups.size() <= 1) {
            // 带权重扩展类型只有1种，退化为COLUMN_WISE
            log.debug("算法选择：带权重扩展类型只有1种（{}），退化为COLUMN_WISE", uniqueGroups);
            return AlgorithmType.COLUMN_WISE;
        }
        
        // 步骤4：检查带权重扩展类型的笛卡尔积前区域数
        // 这里需要统计每个分组对应的区域数量
        // 如果所有分组都只有1个区域，则不需要GROUP_SPLITTING
        Map<String, Integer> groupRegionCounts = new java.util.HashMap<>();
        for (String groupId : regionGroupMapping.values()) {
            groupRegionCounts.put(groupId, groupRegionCounts.getOrDefault(groupId, 0) + 1);
        }
        
        // 检查是否存在至少一个分组有多个区域
        boolean hasMultipleRegionsInGroup = groupRegionCounts.values().stream()
                .anyMatch(count -> count > 1);
        
        if (!hasMultipleRegionsInGroup) {
            // 所有分组都只有1个区域，实际上可以退化为SINGLE_LEVEL（每个分组）
            // 但为了保持一致性，仍然使用GROUP_SPLITTING，让它在内部选择SINGLE_LEVEL
            log.debug("算法选择：带权重扩展类型有{}种，但每个分组都只有1个区域，使用GROUP_SPLITTING（内部会选择SINGLE_LEVEL）", 
                     uniqueGroups.size());
            return AlgorithmType.GROUP_SPLITTING;
        }
        
        // 存在带权重扩展类型，且至少有一个分组有多个区域
        log.debug("算法选择：带权重扩展类型有{}种，且存在多区域分组，选择GROUP_SPLITTING", uniqueGroups.size());
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
     * 档位范围
     */
    private static class GradeRange {
        private final int startIndex; // D30=0, D29=1, ..., D1=29
        private final int endIndex;
        private final int length;
        
        public GradeRange(int startIndex, int endIndex) {
            this.startIndex = startIndex;
            this.endIndex = endIndex;
            this.length = endIndex - startIndex + 1;
        }
        
        public int getStartIndex() {
            return startIndex;
        }
        
        @SuppressWarnings("unused")
        public int getEndIndex() {
            return endIndex;
        }
        
        public int getLength() {
            return length;
        }
    }
    
    /**
     * 解析档位范围
     */
    private GradeRange resolveGradeRange(String maxGrade, String minGrade) {
        int maxIndex = parseGradeToIndex(maxGrade);
        int minIndex = parseGradeToIndex(minGrade);
        
        if (maxIndex < 0) {
            maxIndex = 0; // D30
        }
        if (minIndex < 0 || minIndex > maxIndex) {
            minIndex = 29; // D1
        }
        
        return new GradeRange(maxIndex, minIndex);
    }
    
    /**
     * 将档位字符串转换为索引（D30=0, D29=1, ..., D1=29）
     */
    private int parseGradeToIndex(String grade) {
        if (grade == null || !grade.startsWith("D")) {
            return -1;
        }
        
        try {
            int gradeNum = Integer.parseInt(grade.substring(1));
            // D30对应索引0，D1对应索引29
            return 30 - gradeNum;
        } catch (NumberFormatException e) {
            return -1;
        }
    }
    
    /**
     * 压缩矩阵到指定档位范围
     */
    private BigDecimal[][] compactMatrixToRange(BigDecimal[][] matrix, GradeRange range) {
        if (matrix == null || matrix.length == 0) {
            return matrix;
        }
        
        BigDecimal[][] compact = new BigDecimal[matrix.length][range.getLength()];
        for (int i = 0; i < matrix.length; i++) {
            for (int j = 0; j < range.getLength(); j++) {
                int originalIndex = range.getStartIndex() + j;
                compact[i][j] = matrix[i][originalIndex];
            }
        }
        return compact;
    }
    
    /**
     * 从压缩范围扩展矩阵到30个档位
     */
    private BigDecimal[][] expandMatrixFromRange(BigDecimal[][] compactMatrix, GradeRange range) {
        if (compactMatrix == null || compactMatrix.length == 0) {
            return compactMatrix;
        }
        
        BigDecimal[][] expanded = new BigDecimal[compactMatrix.length][30];
        for (int i = 0; i < compactMatrix.length; i++) {
            // 初始化所有档位为0
            Arrays.fill(expanded[i], BigDecimal.ZERO);
            
            // 填充压缩范围内的值
            for (int j = 0; j < compactMatrix[i].length; j++) {
                int targetIndex = range.getStartIndex() + j;
                if (targetIndex >= 0 && targetIndex < 30) {
                    expanded[i][targetIndex] = compactMatrix[i][j];
                }
            }
        }
        return expanded;
    }

    private BigDecimal roundTargetAmount(BigDecimal amount) {
        if (amount == null) {
            return BigDecimal.ZERO;
        }
        return amount.setScale(0, RoundingMode.HALF_UP);
    }

    /**
     * 使用可扩展的比例提供者计算分组比例
     */
    private Map<String, BigDecimal> calculateGroupRatios(StrategyExecutionRequest request,
                                                         List<String> regions,
                                                         BigDecimal[][] customerMatrix) {
        // 查找支持当前扩展类型的比例提供者
        for (GroupRatioProvider provider : groupRatioProviders) {
            if (provider.supports(request.getDeliveryEtype())) {
                Map<String, BigDecimal> ratios = provider.calculateGroupRatios(request, regions, customerMatrix);
                if (!ratios.isEmpty()) {
                    log.debug("使用比例提供者 {} 计算分组比例: {}", provider.getClass().getSimpleName(), ratios);
                    return ratios;
                }
            }
        }

        // 如果没有找到提供者，尝试从ExtraInfo中提取（向后兼容）
        return extractGroupRatiosFromExtraInfo(request);
    }

    /**
     * 使用可扩展的比例提供者计算区域到分组的映射
     */
    private Map<String, String> calculateRegionGroupMapping(StrategyExecutionRequest request,
                                                             List<String> regions) {
        // 查找支持当前扩展类型的比例提供者
        for (GroupRatioProvider provider : groupRatioProviders) {
            if (provider.supports(request.getDeliveryEtype())) {
                Map<String, String> mapping = provider.getRegionGroupMapping(request, regions);
                if (!mapping.isEmpty()) {
                    log.debug("使用比例提供者 {} 计算区域分组映射: {}", provider.getClass().getSimpleName(), mapping);
                    return mapping;
                }
            }
        }

        // 如果没有找到提供者，尝试从ExtraInfo中提取（向后兼容）
        return extractRegionGroupMappingFromExtraInfo(request);
    }

    /**
     * 从ExtraInfo中提取分组比例（向后兼容）
     */
    @SuppressWarnings("unchecked")
    private Map<String, BigDecimal> extractGroupRatiosFromExtraInfo(StrategyExecutionRequest request) {
        if (request.getExtraInfo() == null) {
            return Collections.emptyMap();
        }
        Object raw = request.getExtraInfo().get(EXTRA_GROUP_RATIOS);
        if (!(raw instanceof Map)) {
            return Collections.emptyMap();
        }
        Map<String, Object> source = (Map<String, Object>) raw;
        Map<String, BigDecimal> result = new LinkedHashMap<>();
        for (Map.Entry<String, Object> entry : source.entrySet()) {
            BigDecimal value = toBigDecimal(entry.getValue());
            if (value != null && value.compareTo(BigDecimal.ZERO) > 0) {
                result.put(entry.getKey(), value);
            }
        }
        return result;
    }

    /**
     * 从ExtraInfo中提取区域分组映射（向后兼容）
     */
    @SuppressWarnings("unchecked")
    private Map<String, String> extractRegionGroupMappingFromExtraInfo(StrategyExecutionRequest request) {
        if (request.getExtraInfo() == null) {
            return Collections.emptyMap();
        }
        Object raw = request.getExtraInfo().get(EXTRA_REGION_GROUP_MAPPING);
        if (!(raw instanceof Map)) {
            return Collections.emptyMap();
        }
        Map<String, Object> source = (Map<String, Object>) raw;
        Map<String, String> result = new LinkedHashMap<>();
        for (Map.Entry<String, Object> entry : source.entrySet()) {
            if (entry.getValue() != null) {
                result.put(entry.getKey(), entry.getValue().toString());
            }
        }
        return result;
    }


    private Comparator<Integer> extractSegmentOrderComparator(StrategyExecutionRequest request,
                                                              List<String> regions) {
        Object raw = request.getExtraInfo().get(EXTRA_SEGMENT_ORDER);
        if (!(raw instanceof List)) {
            return null;
        }
        List<?> orderList = (List<?>) raw;
        List<String> desiredOrder = orderList.stream()
                .filter(item -> item instanceof String)
                .map(Object::toString)
                .collect(Collectors.toList());

        Map<String, Integer> indexMap = new LinkedHashMap<>();
        for (int i = 0; i < desiredOrder.size(); i++) {
            indexMap.put(desiredOrder.get(i), i);
        }
        return Comparator.comparingInt(idx -> indexMap.getOrDefault(regions.get(idx), Integer.MAX_VALUE));
    }

    private BigDecimal toBigDecimal(Object raw) {
        if (raw == null) {
            return null;
        }
        if (raw instanceof BigDecimal) {
            return (BigDecimal) raw;
        }
        if (raw instanceof Number) {
            return new BigDecimal(((Number) raw).toString());
        }
        try {
            return new BigDecimal(raw.toString());
        } catch (NumberFormatException ex) {
            log.warn("无法解析比例值: {}", raw, ex);
            return null;
        }
    }

    private enum AlgorithmType {
        SINGLE_LEVEL,
        COLUMN_WISE,
        GROUP_SPLITTING
    }
}

