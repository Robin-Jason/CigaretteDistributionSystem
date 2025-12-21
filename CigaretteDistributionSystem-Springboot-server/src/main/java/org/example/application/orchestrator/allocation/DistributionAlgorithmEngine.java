package org.example.application.orchestrator.allocation;

import lombok.extern.slf4j.Slf4j;
import org.example.application.orchestrator.allocation.provider.GroupRatioProvider;
import org.example.application.orchestrator.strategy.StrategyContext;
import org.example.application.orchestrator.strategy.StrategyExecutionRequest;
import org.example.application.orchestrator.strategy.StrategyExecutionResult;
import org.example.domain.service.algorithm.ColumnWiseAdjustmentService;
import org.example.domain.service.algorithm.GroupSplittingDistributionService;
import org.example.domain.service.algorithm.SingleLevelDistributionService;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 分配算法引擎（算法选择 + 执行）。
 *
 * <p>职责：</p>
 * <ul>
 *   <li>根据“实际参与分配的区域集合”与扩展类型/比例参数，决定使用哪一种抽象算法实现；</li>
 *   <li>在需要时，通过 {@link org.example.application.orchestrator.allocation.provider.GroupRatioProvider}
 *   计算分组比例与区域分组映射；</li>
 *   <li>对输入矩阵做档位范围裁剪/扩展与必要的单调性纠偏。</li>
 * </ul>
 *
 * <p>说明：算法选择依赖“最终区域集合”，而不是仅依赖投放组合字符串。</p>
 *
 * @author Robin
 * @since 2025-12-11
 */
@Slf4j
@Component
public class DistributionAlgorithmEngine {

    public static final String EXTRA_GROUP_RATIOS = "groupRatios";
    public static final String EXTRA_REGION_GROUP_MAPPING = "regionGroupMapping";
    public static final String EXTRA_SEGMENT_ORDER = "segmentOrder";

    private final SingleLevelDistributionService singleLevelService;
    private final ColumnWiseAdjustmentService columnWiseService;
    private final GroupSplittingDistributionService groupSplittingService;
    private final List<GroupRatioProvider> groupRatioProviders;

    public DistributionAlgorithmEngine(SingleLevelDistributionService singleLevelService,
                                       ColumnWiseAdjustmentService columnWiseService,
                                       GroupSplittingDistributionService groupSplittingService,
                                       List<GroupRatioProvider> groupRatioProviders) {
        this.singleLevelService = singleLevelService;
        this.columnWiseService = columnWiseService;
        this.groupSplittingService = groupSplittingService;
        this.groupRatioProviders = groupRatioProviders != null ? groupRatioProviders : Collections.emptyList();
    }

    /**
     * 根据上下文与请求选择并执行分配算法。
     *
     * @param context 预处理后的策略上下文（含目标量、客户矩阵等）
     * @param request 分配请求（包含扩展类型、比例、排序等信息）
     * @return 执行结果，包含最终分配矩阵与成功标记
     * @example 传入 2 个区域的矩阵与按档位扩展类型，自动选择 COLUMN_WISE 返回分配矩阵
     */
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

        // 统一检查：在调用具体算法前，检查是否存在某一行全为0的情况
        String zeroRowCheckResult = validateNoZeroRows(regions, compactCustomerMatrix);
        if (zeroRowCheckResult != null) {
            log.error("分配算法执行前检查失败: {}", zeroRowCheckResult);
            return StrategyExecutionResult.failure(zeroRowCheckResult);
        }

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

        // 统一记录一次“算法结果”日志，方便线上排查：
        // 含：算法类型、区域数、目标量、实际量、误差。
        BigDecimal actualAmount = calculateTotalAmount(allocationCompact, compactCustomerMatrix);
        BigDecimal error = roundedTarget.subtract(actualAmount).abs();
        log.info("DistributionAlgorithmEngine 分配完成，算法={}, regions={}, target={}, actual={}, error={}",
                type, regions.size(), roundedTarget, actualAmount, error);

        BigDecimal[][] finalAllocation = expandMatrixFromRange(allocationCompact, gradeRange);
        // matrix 构建自 RegionCustomerMatrix（已包含上浮等处理），此处作为“实际使用的客户矩阵”回传
        return StrategyExecutionResult.success(regions, matrix, finalAllocation);
    }

    /**
     * 执行单区域/单层分配。
     *
     * @param regions 区域列表
     * @param customerMatrix 客户矩阵（区域 x 档位）
     * @param targetAmount 目标投放量
     * @return 分配结果矩阵
     * @example 仅 1 个区域时调用，返回同维度分配矩阵
     */
    private BigDecimal[][] runSingleLevel(List<String> regions,
                                          BigDecimal[][] customerMatrix,
                                          BigDecimal targetAmount) {
        log.info("SingleLevel 分配开始，regions={}, target={}", regions.size(), targetAmount);
        BigDecimal[][] allocation = singleLevelService.distribute(regions, customerMatrix, targetAmount);
        BigDecimal actual = calculateTotalAmount(allocation, customerMatrix);
        BigDecimal error = targetAmount.subtract(actual).abs();
        log.info("SingleLevel 分配完成，regions={}, target={}, actual={}, error={}",
                regions.size(), targetAmount, actual, error);
        enforceMonotonicRows(allocation);
        return allocation;
    }

    /**
     * 执行按列（档位）分配。
     *
     * @param regions 区域列表
     * @param customerMatrix 客户矩阵
     * @param targetAmount 目标投放量
     * @param segmentComparator 档位排序规则（可为空）
     * @return 分配结果矩阵
     * @example 多区域且无带权重扩展类型时调用
     */
    private BigDecimal[][] runColumnWise(List<String> regions,
                                         BigDecimal[][] customerMatrix,
                                         BigDecimal targetAmount,
                                         Comparator<Integer> segmentComparator) {
        log.info("ColumnWise 分配开始，regions={}, target={}", regions.size(), targetAmount);
        BigDecimal[][] allocation = columnWiseService.distribute(regions, customerMatrix, targetAmount, segmentComparator);
        BigDecimal actual = calculateTotalAmount(allocation, customerMatrix);
        BigDecimal error = targetAmount.subtract(actual).abs();
        log.info("ColumnWise 分配完成，regions={}, target={}, actual={}, error={}",
                regions.size(), targetAmount, actual, error);
        enforceMonotonicRows(allocation);
        return allocation;
    }

    /**
     * 执行分组拆分分配（带权重扩展类型）。
     *
     * @param regions 区域列表
     * @param customerMatrix 客户矩阵
     * @param targetAmount 目标投放量
     * @param groupingFunction 区域到分组的映射函数
     * @param groupRatios 分组比例
     * @return 分配结果矩阵
     * @example 带权重扩展类型且存在多组区域时调用
     */
    private BigDecimal[][] runGroupSplitting(List<String> regions,
                                             BigDecimal[][] customerMatrix,
                                             BigDecimal targetAmount,
                                             Function<String, String> groupingFunction,
                                             Map<String, BigDecimal> groupRatios) {
        log.info("GroupSplitting 分配开始，regions={}, target={}", regions.size(), targetAmount);
        BigDecimal[][] allocation = groupSplittingService.distribute(
                regions,
                customerMatrix,
                targetAmount,
                groupingFunction,
                groupRatios
        );
        BigDecimal actual = calculateTotalAmount(allocation, customerMatrix);
        BigDecimal error = targetAmount.subtract(actual).abs();
        log.info("GroupSplitting 分配完成，regions={}, target={}, actual={}, error={}",
                regions.size(), targetAmount, actual, error);
        enforceMonotonicRows(allocation);
        return allocation;
    }

    /**
     * 确保每个区域的分配矩阵严格非递增（D30 >= ... >= D1）。
     * 如遇到低档位大于高档位的情况，将其截断为上一个档位值，
     * 并把多余的量回补到 D30，既保证单调性，也保持总量不变。
     *
     * @param allocationMatrix 分配矩阵
     * @example 对单区域矩阵 [10,9,8,12] 纠偏为单调并回补余量
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

    /**
     * 将行中的 null 归零。
     *
     * @param row 档位数组
     * @example [null, 1, null] → [0,1,0]
     */
    private void normalizeNulls(BigDecimal[] row) {
        for (int i = 0; i < row.length; i++) {
            if (row[i] == null) {
                row[i] = BigDecimal.ZERO;
            }
        }
    }

    /**
     * 将多余的量向前（高档位）回补，保持总量不变。
     *
     * @param row 档位数组
     * @param startIndex 回补起点（含）
     * @param excess 需要回补的余量
     * @example 在 D10 产生的 2.0 余量向 D30 回补
     */
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
     * 决定使用哪种算法。
     * 
     * 算法选择逻辑：
     * 1. 仅 1 个区域 → SINGLE_LEVEL
     * 2. 有带权重扩展类型：
     *    - 只有 1 种分组 → COLUMN_WISE
     *    - 存在多分组且至少一组多区域 → GROUP_SPLITTING
     * 3. 无带权重扩展类型 → COLUMN_WISE
     * 
     * @param regionCount 待投放区域数量
     * @param groupRatios 分组比例（非空表示存在带权重扩展类型）
     * @param regionGroupMapping 区域到分组映射
     * @return 算法类型
     * @example regionCount=1 → SINGLE_LEVEL；多区域且 groupRatios 为空 → COLUMN_WISE
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

    /**
     * 将行数据转换为二维矩阵。
     *
     * @param rows 区域行列表
     * @return 区域 x 档位矩阵
     * @example rows.size()==2 → 返回 2 行矩阵
     */
    private BigDecimal[][] buildMatrix(List<RegionCustomerMatrix.Row> rows) {
        BigDecimal[][] matrix = new BigDecimal[rows.size()][];
        for (int i = 0; i < rows.size(); i++) {
            BigDecimal[] grades = rows.get(i).getGrades();
            matrix[i] = grades == null ? new BigDecimal[0] : Arrays.copyOf(grades, grades.length);
        }
        return matrix;
    }
    
    /**
     * 档位范围。
     *
     * @param startIndex 起始索引
     * @param endIndex 结束索引
     * @example startIndex=0,endIndex=29 表示 D30~D1 全量
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
     * 解析档位范围。
     *
     * @param maxGrade 最大档位（如 D30）
     * @param minGrade 最小档位（如 D1）
     * @return 档位范围对象
     * @example maxGrade=D30,minGrade=D10 → 取 D30~D10
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
     * 将档位字符串转换为索引（D30=0, D29=1, ..., D1=29）。
     *
     * @param grade 档位字符串
     * @return 索引；无法解析时返回 -1
     * @example "D30" → 0；"D1" → 29
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
     * 压缩矩阵到指定档位范围。
     *
     * @param matrix 原始矩阵
     * @param range 档位范围
     * @return 压缩后的矩阵
     * @example 输入 30 列矩阵，截取 D30~D10 返回 20 列矩阵
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
     * 从压缩范围扩展矩阵到 30 个档位。
     *
     * @param compactMatrix 压缩矩阵
     * @param range 压缩时使用的档位范围
     * @return 30 列完整矩阵
     * @example 将 D30~D10 矩阵扩展回 D30~D1，其他档位填 0
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

    /**
     * 对目标量四舍五入为整数。
     *
     * @param amount 目标量
     * @return 取整后的目标量
     * @example 1234.6 → 1235
     */
    private BigDecimal roundTargetAmount(BigDecimal amount) {
        if (amount == null) {
            return BigDecimal.ZERO;
        }
        return amount.setScale(0, RoundingMode.HALF_UP);
    }

    /**
     * 使用可扩展的比例提供者计算分组比例。
     *
     * @param request 分配请求
     * @param regions 区域列表
     * @param customerMatrix 客户矩阵
     * @return 分组比例映射
     * @example deliveryEtype=档位+区县 → 调用支持该类型的 provider 产出比例
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
     * 使用可扩展的比例提供者计算区域到分组的映射。
     *
     * @param request 分配请求
     * @param regions 区域列表
     * @return 区域→分组映射
     * @example deliveryEtype=档位+区县 → provider 产出区域分组表
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
     * 从 ExtraInfo 中提取分组比例（向后兼容）。
     *
     * @param request 分配请求
     * @return 分组比例映射
     * @example ExtraInfo 中包含 {"groupRatios": {"A":0.6,"B":0.4}}
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
     * 从 ExtraInfo 中提取区域分组映射（向后兼容）。
     *
     * @param request 分配请求
     * @return 区域→分组映射
     * @example ExtraInfo 中包含 {"regionGroupMapping": {"R1":"G1"}}
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


    /**
     * 从请求 ExtraInfo 中提取档位排序规则。
     *
     * @param request 分配请求
     * @param regions 区域列表
     * @return 档位排序比较器；不存在时返回 null
     * @example ExtraInfo.segmentOrder=["R2","R1"] → 返回自定义排序
     */
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

    /**
     * 将对象转换为 BigDecimal。
     *
     * @param raw 原始对象
     * @return BigDecimal；无法解析时返回 null
     * @example 输入字符串 \"1.23\" → 1.23
     */
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

    /**
     * 验证客户矩阵中不存在全为0的行。
     * <p>
     * 如果发现某一行（区域）的所有档位客户数全为0，返回错误信息。
     * 这是分配算法的前置条件，因为全为0的区域无法进行有效分配。
     * </p>
     *
     * @param regions 区域列表（与矩阵行一一对应）
     * @param customerMatrix 客户矩阵（区域 x 档位）
     * @return 如果验证通过返回 null；如果发现全为0的行，返回错误信息
     */
    private String validateNoZeroRows(List<String> regions, BigDecimal[][] customerMatrix) {
        if (customerMatrix == null || customerMatrix.length == 0) {
            return "客户矩阵为空，无法进行分配";
        }
        
        if (regions == null || regions.size() != customerMatrix.length) {
            return String.format("区域列表与客户矩阵行数不匹配: regions=%d, matrix=%d", 
                    regions != null ? regions.size() : 0, customerMatrix.length);
        }
        
        List<String> zeroRowRegions = new ArrayList<>();
        for (int r = 0; r < customerMatrix.length; r++) {
            BigDecimal[] row = customerMatrix[r];
            if (row == null || row.length == 0) {
                zeroRowRegions.add(regions.get(r));
                continue;
            }
            
            // 检查该行的所有档位是否全为0
            boolean allZero = true;
            for (BigDecimal value : row) {
                if (value != null && value.compareTo(BigDecimal.ZERO) > 0) {
                    allZero = false;
                    break;
                }
            }
            
            if (allZero) {
                zeroRowRegions.add(regions.get(r));
            }
        }
        
        if (!zeroRowRegions.isEmpty()) {
            return String.format("以下区域在客户矩阵中30个档位客户数全为0，无法进行分配: %s。请确保这些区域在 region_customer_statistics 表中有有效的客户数据。",
                    String.join(", ", zeroRowRegions));
        }
        
        return null; // 验证通过
    }

    /**
     * 计算分配矩阵在给定客户矩阵下的实际投放量。
     *
     * @param allocationMatrix 分配矩阵
     * @param customerMatrix   客户矩阵
     * @return 实际投放量
     */
    private BigDecimal calculateTotalAmount(BigDecimal[][] allocationMatrix,
                                            BigDecimal[][] customerMatrix) {
        if (allocationMatrix == null || customerMatrix == null) {
            return BigDecimal.ZERO;
        }
        BigDecimal total = BigDecimal.ZERO;
        int rowCount = Math.min(allocationMatrix.length, customerMatrix.length);
        for (int r = 0; r < rowCount; r++) {
            BigDecimal[] allocRow = allocationMatrix[r];
            BigDecimal[] custRow = customerMatrix[r];
            if (allocRow == null || custRow == null) {
                continue;
            }
            int colCount = Math.min(allocRow.length, custRow.length);
            for (int c = 0; c < colCount; c++) {
                BigDecimal a = allocRow[c];
                BigDecimal k = custRow[c];
                if (a != null && k != null) {
                    total = total.add(a.multiply(k));
                }
            }
        }
        return total;
    }
}

