package org.example.application.orchestrator;

import lombok.extern.slf4j.Slf4j;
import org.example.domain.repository.RegionCustomerStatisticsRepository;
import org.example.application.service.BiWeeklyVisitBoostService;
import org.example.domain.model.valueobject.DeliveryCombination;
import org.example.domain.service.delivery.DeliveryCombinationParser;
import org.example.shared.util.KmpMatcher;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Central entry point for executing delivery strategies based on combinations.
 */
@Slf4j
@Component
public class StrategyOrchestrator {

    private final DeliveryCombinationParser combinationParser;
    private final StrategyContextBuilder contextBuilder;
    private final RegionCustomerStatisticsRepository regionCustomerStatisticsRepository;
    private final DistributionAlgorithmEngine algorithmEngine;
    private final KmpMatcher kmpMatcher;
    private final BiWeeklyVisitBoostService biWeeklyVisitBoostService;

    public StrategyOrchestrator(DeliveryCombinationParser combinationParser,
                                StrategyContextBuilder contextBuilder,
                                RegionCustomerStatisticsRepository regionCustomerStatisticsRepository,
                                DistributionAlgorithmEngine algorithmEngine,
                                KmpMatcher kmpMatcher,
                                BiWeeklyVisitBoostService biWeeklyVisitBoostService) {
        this.combinationParser = combinationParser;
        this.contextBuilder = contextBuilder;
        this.regionCustomerStatisticsRepository = regionCustomerStatisticsRepository;
        this.algorithmEngine = algorithmEngine;
        this.kmpMatcher = kmpMatcher;
        this.biWeeklyVisitBoostService = biWeeklyVisitBoostService;
    }

    public StrategyExecutionResult execute(StrategyExecutionRequest request) {
        StrategyExecutionResult validationResult = validateRequest(request);
        if (validationResult != null) {
            return validationResult;
        }

        DeliveryCombination combination;
        try {
            combination = combinationParser.parse(
                    request.getDeliveryMethod(),
                    request.getDeliveryEtype(),
                    request.getTag()
            );
        } catch (IllegalArgumentException ex) {
            log.error("组合解析失败: {}", ex.getMessage());
            return StrategyExecutionResult.failure("组合解析失败: " + ex.getMessage());
        }

        // 根据投放区域从region_customer_statistics表获取区域客户数矩阵
        RegionCustomerMatrix baseMatrix = buildCustomerMatrix(
                request.getYear(),
                request.getMonth(),
                request.getWeekSeq(),
                request.getDeliveryArea()
        );

        // 对需要执行“两周一访上浮100%”的卷烟，交给专门的上浮服务做矩阵再处理
        RegionCustomerMatrix customerMatrix = biWeeklyVisitBoostService.applyBiWeeklyBoostIfNeeded(
                baseMatrix,
                request.getYear(),
                request.getMonth(),
                request.getWeekSeq(),
                request.getDeliveryMethod(),
                request.getDeliveryEtype(),
                request.getTag(),
                request.getDeliveryArea(),
                request.getRemark(),
                request.getExtraInfo()
        );

        StrategyContext context = contextBuilder.build(combination, request, customerMatrix);

        if (!combination.isImplemented()) {
            String msg = "策略未实现: " + combination.getMethodType().getDisplayName();
            log.warn(msg);
            return StrategyExecutionResult.failure(msg);
        }
        if (!combination.requiresStatistics()) {
            String msg = "该组合不需要策略执行";
            log.info(msg);
            return StrategyExecutionResult.failure(msg);
        }

        StrategyExecutionResult modernResult = algorithmEngine.execute(context, request);
        if (modernResult.isSuccess()) {
            return modernResult;
        }
        log.error("算法执行失败: {}", modernResult.getMessage());
        return modernResult;
    }

    private StrategyExecutionResult validateRequest(StrategyExecutionRequest request) {
        if (request.getYear() == null || request.getMonth() == null || request.getWeekSeq() == null) {
            return StrategyExecutionResult.failure("年份、月份、周序号不能为空");
        }
        if (request.getTargetAmount() == null || request.getTargetAmount().compareTo(BigDecimal.ZERO) <= 0) {
            return StrategyExecutionResult.failure("预投放量必须大于0");
        }
        return null;
    }
    
    /**
     * 根据投放区域构建区域客户数矩阵
     * 1. 从region_customer_statistics对应分区获取本次分配的“区域全集”
     * 2. 按投放区域字符串解析出“待投放区域列表”
     * 3. 使用KMP在区域全集中做子串匹配，筛选出匹配成功的区域
     * 4. 按TOTAL降序排序生成子矩阵
     */
    private RegionCustomerMatrix buildCustomerMatrix(Integer year, Integer month, Integer weekSeq,
                                                     String deliveryArea) {
        // 1. 全量区域客户数列表
        List<Map<String, Object>> allStats = regionCustomerStatisticsRepository.findAll(year, month, weekSeq);
        if (allStats == null || allStats.isEmpty()) {
            log.warn("region_customer_statistics在 {}-{}-{} 分区无数据", year, month, weekSeq);
            return new RegionCustomerMatrix(new ArrayList<>());
        }

        // 2. 解析待投放区域列表
        List<String> targetAreas = parseDeliveryAreas(deliveryArea);
        if (targetAreas.isEmpty()) {
            // 未指定区域时，默认使用全集
            List<RegionCustomerMatrix.Row> allRows = new ArrayList<>();
            for (Map<String, Object> row : allStats) {
                String region = getString(row, "REGION");
                BigDecimal[] grades = extractGrades(row);
                allRows.add(new RegionCustomerMatrix.Row(region, grades));
            }
            sortRowsByTotalDesc(allRows);
            return new RegionCustomerMatrix(allRows);
        }

        // 3. 使用KMP在区域全集中做子串匹配，构造子矩阵
        Map<String, RegionCustomerMatrix.Row> rowMap = new LinkedHashMap<>();
        for (Map<String, Object> stat : allStats) {
            String regionName = getString(stat, "REGION");
            if (regionName == null || regionName.trim().isEmpty()) {
                continue;
            }
            String regionTrimmed = regionName.trim();
            // 使用KmpMatcher做精确匹配：只有区域名与待投放区域完全一致时才认为匹配成功
            List<String> matched = kmpMatcher.matchPatterns(regionTrimmed, targetAreas);
            if (!matched.isEmpty()) {
                if (!rowMap.containsKey(regionTrimmed)) {
                    BigDecimal[] grades = extractGrades(stat);
                    rowMap.put(regionTrimmed, new RegionCustomerMatrix.Row(regionTrimmed, grades));
                }
            }
        }

        List<RegionCustomerMatrix.Row> rows = new ArrayList<>(rowMap.values());
        sortRowsByTotalDesc(rows);
        return new RegionCustomerMatrix(rows);
    }

    /**
     * 解析投放区域字符串
     * 支持分隔符：,  ，  。  、  ;  ；  /  |  -  +
     */
    private List<String> parseDeliveryAreas(String deliveryArea) {
        List<String> regions = new ArrayList<>();
        if (deliveryArea == null) {
            return regions;
        }
        String normalized = deliveryArea.trim();
        if (normalized.isEmpty()) {
            return regions;
        }
        String[] parts = normalized.split("[,，。\\.、;；/|\\-+]+");
        for (String part : parts) {
            String trimmed = part.trim();
            if (!trimmed.isEmpty()) {
                regions.add(trimmed);
            }
        }
        return regions;
    }

    private void sortRowsByTotalDesc(List<RegionCustomerMatrix.Row> rows) {
        rows.sort(Comparator.comparing(this::computeTotal).reversed());
    }

    private BigDecimal computeTotal(RegionCustomerMatrix.Row row) {
        if (row == null || row.getGrades() == null) {
            return BigDecimal.ZERO;
        }
        BigDecimal total = BigDecimal.ZERO;
        for (BigDecimal v : row.getGrades()) {
            if (v != null) {
                total = total.add(v);
            }
        }
        return total;
    }

    private BigDecimal[] extractGrades(Map<String, Object> row) {
        BigDecimal[] grades = new BigDecimal[30];
        Arrays.fill(grades, BigDecimal.ZERO);
        for (int i = 0; i < 30; i++) {
            String column = "D" + (30 - i);
            Object value = row.get(column);
            if (value instanceof BigDecimal) {
                grades[i] = (BigDecimal) value;
            } else if (value instanceof Number) {
                grades[i] = BigDecimal.valueOf(((Number) value).doubleValue());
            }
        }
        return grades;
    }

    private String getString(Map<String, Object> map, String key) {
        if (map == null) {
            return null;
        }
        Object value = map.get(key);
        if (value == null) {
            for (Map.Entry<String, Object> entry : map.entrySet()) {
                if (entry.getKey().equalsIgnoreCase(key)) {
                    value = entry.getValue();
                    break;
                }
            }
        }
        return value != null ? value.toString() : null;
    }

}

