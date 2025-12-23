package org.example.application.service.coordinator;

import lombok.extern.slf4j.Slf4j;
import org.example.domain.model.valueobject.RegionCustomerMatrix;
import org.example.domain.repository.RegionCustomerStatisticsRepository;
import org.example.shared.util.GradeExtractor;
import org.example.shared.util.KmpMatcher;
import org.example.shared.util.MapValueExtractor;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.*;

/**
 * 客户矩阵构建器。
 * <p>
 * 职责：根据投放区域从 region_customer_statistics 表构建区域客户数矩阵。
 * </p>
 *
 * @author Robin
 * @since 2025-12-22
 */
@Slf4j
@Component
public class CustomerMatrixBuilder {

    private final RegionCustomerStatisticsRepository regionCustomerStatisticsRepository;
    private final KmpMatcher kmpMatcher;
    private final BiWeeklyVisitBoostService biWeeklyVisitBoostService;

    public CustomerMatrixBuilder(RegionCustomerStatisticsRepository regionCustomerStatisticsRepository,
                                 KmpMatcher kmpMatcher,
                                 BiWeeklyVisitBoostService biWeeklyVisitBoostService) {
        this.regionCustomerStatisticsRepository = regionCustomerStatisticsRepository;
        this.kmpMatcher = kmpMatcher;
        this.biWeeklyVisitBoostService = biWeeklyVisitBoostService;
    }

    /**
     * 构建区域客户矩阵。
     * <p>
     * 根据投放区域字符串从 region_customer_statistics 表查询并构建客户数矩阵。
     * 如果未指定区域，则返回全部区域的矩阵。
     * </p>
     *
     * @param year         年份，如 2025
     * @param month        月份，1-12
     * @param weekSeq      周序号，1-5
     * @param deliveryArea 投放区域字符串，多个区域用逗号分隔，如 "城区,郊区"；为空则返回全部区域
     * @return 区域客户矩阵，包含匹配区域的客户数分布（D1-D30）
     * @example
     * <pre>{@code
     * // 构建指定区域的客户矩阵
     * RegionCustomerMatrix matrix = customerMatrixBuilder.build(2025, 12, 3, "城区,郊区");
     * 
     * // 构建全部区域的客户矩阵
     * RegionCustomerMatrix fullMatrix = customerMatrixBuilder.build(2025, 12, 3, null);
     * }</pre>
     */
    public RegionCustomerMatrix build(Integer year, Integer month, Integer weekSeq, String deliveryArea) {
        List<Map<String, Object>> allStats = regionCustomerStatisticsRepository.findAll(year, month, weekSeq);
        if (allStats == null || allStats.isEmpty()) {
            log.warn("region_customer_statistics 在 {}-{}-{} 分区无数据", year, month, weekSeq);
            return new RegionCustomerMatrix(new ArrayList<>());
        }

        List<String> targetAreas = parseDeliveryAreas(deliveryArea);
        log.debug("【区域匹配】解析后的目标区域列表: {}", targetAreas);

        if (targetAreas.isEmpty()) {
            return buildFullMatrix(allStats);
        }

        return buildFilteredMatrix(allStats, targetAreas);
    }

    /**
     * 构建区域客户矩阵（带两周一访上浮处理）。
     * <p>
     * 在基础矩阵构建完成后，根据投放方式、扩展类型、标签等信息判断是否需要
     * 应用两周一访客户上浮逻辑。
     * </p>
     *
     * @param year           年份，如 2025
     * @param month          月份，1-12
     * @param weekSeq        周序号，1-5
     * @param deliveryArea   投放区域字符串
     * @param deliveryMethod 投放方式，如 "按档位投放"
     * @param deliveryEtype  扩展类型，如 "市场类型"
     * @param tag            标签，如 "城网"
     * @param remark         备注信息
     * @param extraInfo      额外信息 Map，用于传递动态标签等
     * @return 处理后的区域客户矩阵（可能已应用两周一访上浮）
     * @example
     * <pre>{@code
     * Map<String, Object> extraInfo = new HashMap<>();
     * extraInfo.put("DYNAMIC_TAG", "两周一访");
     * 
     * RegionCustomerMatrix matrix = customerMatrixBuilder.buildWithBoost(
     *     2025, 12, 3, "城区",
     *     "按档位投放", "市场类型", "城网",
     *     null, extraInfo
     * );
     * }</pre>
     */
    public RegionCustomerMatrix buildWithBoost(Integer year, Integer month, Integer weekSeq,
                                                String deliveryArea, String deliveryMethod,
                                                String deliveryEtype, String tag,
                                                String remark, Map<String, Object> extraInfo) {
        RegionCustomerMatrix baseMatrix = build(year, month, weekSeq, deliveryArea);
        
        return biWeeklyVisitBoostService.applyBiWeeklyBoostIfNeeded(
                baseMatrix, year, month, weekSeq,
                deliveryMethod, deliveryEtype, tag, deliveryArea,
                remark, extraInfo);
    }

    /**
     * 构建"全市"单行客户矩阵。
     * <p>
     * 汇总所有区域的客户数，生成单行矩阵，区域名称固定为"全市"。
     * 主要用于价位段自选投放场景。
     * </p>
     *
     * @param year    年份，如 2025
     * @param month   月份，1-12
     * @param weekSeq 周序号，1-5
     * @return 全市汇总的单行客户矩阵
     * @example
     * <pre>{@code
     * RegionCustomerMatrix cityMatrix = customerMatrixBuilder.buildCityWideMatrix(2025, 12, 4);
     * // cityMatrix.getRows() 返回单行，region = "全市"
     * BigDecimal[] grades = cityMatrix.getRows().get(0).getGrades();
     * }</pre>
     */
    public RegionCustomerMatrix buildCityWideMatrix(Integer year, Integer month, Integer weekSeq) {
        List<Map<String, Object>> allStats = regionCustomerStatisticsRepository.findAll(year, month, weekSeq);
        if (allStats == null || allStats.isEmpty()) {
            return new RegionCustomerMatrix(new ArrayList<>());
        }

        // 直接查找"全市"区域的数据，而不是累加所有区域
        for (Map<String, Object> stat : allStats) {
            String region = MapValueExtractor.getStringValue(stat, "REGION");
            if ("全市".equals(region)) {
                BigDecimal[] grades = GradeExtractor.extractFromMap(stat);
                List<RegionCustomerMatrix.Row> rows = new ArrayList<>();
                rows.add(new RegionCustomerMatrix.Row("全市", grades));
                return new RegionCustomerMatrix(rows);
            }
        }

        // 如果没有找到"全市"区域，返回空矩阵
        log.warn("未找到'全市'区域的客户统计数据");
        return new RegionCustomerMatrix(new ArrayList<>());
    }

    private RegionCustomerMatrix buildFullMatrix(List<Map<String, Object>> allStats) {
        List<RegionCustomerMatrix.Row> allRows = new ArrayList<>();
        for (Map<String, Object> row : allStats) {
            String region = MapValueExtractor.getStringValue(row, "REGION");
            BigDecimal[] grades = GradeExtractor.extractFromMap(row);
            allRows.add(new RegionCustomerMatrix.Row(region, grades));
        }
        sortRowsByTotalDesc(allRows);
        log.debug("【区域匹配】未指定区域，使用全集，共 {} 个区域", allRows.size());
        return new RegionCustomerMatrix(allRows);
    }

    private RegionCustomerMatrix buildFilteredMatrix(List<Map<String, Object>> allStats, List<String> targetAreas) {
        Map<String, RegionCustomerMatrix.Row> rowMap = new LinkedHashMap<>();
        Set<String> matchedRegions = new HashSet<>();
        Set<String> unmatchedTargetAreas = new HashSet<>(targetAreas);

        for (Map<String, Object> stat : allStats) {
            String regionName = MapValueExtractor.getStringValue(stat, "REGION");
            if (regionName == null || regionName.trim().isEmpty()) {
                continue;
            }
            String regionTrimmed = regionName.trim();
            List<String> matched = kmpMatcher.matchPatterns(regionTrimmed, targetAreas);
            if (!matched.isEmpty() && !rowMap.containsKey(regionTrimmed)) {
                BigDecimal[] grades = GradeExtractor.extractFromMap(stat);
                rowMap.put(regionTrimmed, new RegionCustomerMatrix.Row(regionTrimmed, grades));
                matchedRegions.add(regionTrimmed);
                unmatchedTargetAreas.removeAll(matched);
            }
        }

        if (!unmatchedTargetAreas.isEmpty()) {
            log.warn("【区域匹配失败】以下目标区域未找到匹配: {}", unmatchedTargetAreas);
        }
        log.debug("【区域匹配】目标区域数={}, 匹配成功数={}", targetAreas.size(), matchedRegions.size());

        List<RegionCustomerMatrix.Row> rows = new ArrayList<>(rowMap.values());
        sortRowsByTotalDesc(rows);
        return new RegionCustomerMatrix(rows);
    }

    private List<String> parseDeliveryAreas(String deliveryArea) {
        List<String> regions = new ArrayList<>();
        if (deliveryArea == null || deliveryArea.trim().isEmpty()) {
            return regions;
        }
        String[] parts = deliveryArea.trim().split("[,，。\\.、;；/|\\-+]+");
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
}
