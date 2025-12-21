package org.example.shared.util;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.domain.repository.FilterCustomerTableRepository;
import org.example.application.service.coordinator.TagExtractionService;
import org.example.domain.model.valueobject.DeliveryExtensionType;
import org.example.domain.model.tag.TagFilterRule;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.util.*;

import static org.example.shared.util.MapValueExtractor.getLongValue;
import static org.example.shared.util.MapValueExtractor.getStringValue;
import static org.example.shared.util.GradeParser.parseGradeToIndex;
import static org.example.shared.util.RegionNameBuilder.buildRegionName;

/**
 * 订单周期矩阵计算器
 * <p>
 * 负责计算单周/双周客户的区域客户数增量矩阵。
 * </p>
 *
 * @author Robin
 * @version 1.0
 * @since 2025-12-11
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OrderCycleMatrixCalculator {

    private static final String REGION_FULL_CITY = "全市";
    private static final Map<DeliveryExtensionType, String> EXTENSION_TYPE_TO_COLUMN = new HashMap<>();

    static {
        EXTENSION_TYPE_TO_COLUMN.put(DeliveryExtensionType.COUNTY, "COMPANY_DISTRICT");
        EXTENSION_TYPE_TO_COLUMN.put(DeliveryExtensionType.MARKET_TYPE, "MARKET_TYPE");
        EXTENSION_TYPE_TO_COLUMN.put(DeliveryExtensionType.URBAN_RURAL_CODE, "CLASSIFICATION_CODE");
        EXTENSION_TYPE_TO_COLUMN.put(DeliveryExtensionType.BUSINESS_FORMAT, "CUST_FORMAT");
        EXTENSION_TYPE_TO_COLUMN.put(DeliveryExtensionType.MARKET_DEPARTMENT, "MARKET_DEPARTMENT");
        EXTENSION_TYPE_TO_COLUMN.put(DeliveryExtensionType.BUSINESS_DISTRICT, "BUSINESS_DISTRICT_TYPE");
        EXTENSION_TYPE_TO_COLUMN.put(DeliveryExtensionType.INTEGRITY_GROUP, "GROUP_NAME");
        EXTENSION_TYPE_TO_COLUMN.put(DeliveryExtensionType.CREDIT_LEVEL, "CREDIT_LEVEL");
    }

    private final FilterCustomerTableRepository filterCustomerTableRepository;
    private final TagExtractionService tagExtractionService;
    private final CombinationStrategyAnalyzer strategyAnalyzer;

    /**
     * 针对单周/双周客户重新统计区域矩阵（按投放组合策略）。
     * <p>
     * 根据投放组合策略（城市级或扩展维度）统计单周/双周客户的区域客户数增量。
     * </p>
     *
     * @param strategy       组合策略（包含模式：CITY/EXTENSION/SKIPPED，以及扩展类型列表）
     * @param year           年份
     * @param month          月份
     * @param weekSeq        周序号
     * @param tagRules       标签过滤规则列表（可为空）
     * @param orderCycleType 单周或双周类型
     * @return 区域名称到30档位客户数增量数组的映射
     */
    public Map<String, BigDecimal[]> calculateOrderCycleMatrix(
            CombinationStrategyAnalyzer.CombinationStrategy strategy,
            Integer year, Integer month, Integer weekSeq,
            List<TagFilterRule> tagRules,
            OrderCycleType orderCycleType) {
        Map<String, BigDecimal[]> records = new LinkedHashMap<>();
        if (strategy.mode == CombinationStrategyAnalyzer.CombinationMode.CITY) {
            records.putAll(buildRecordsWithTags(
                    REGION_FULL_CITY, year, month, weekSeq, Collections.emptyMap(), tagRules, orderCycleType));
            return records;
        }

        if (strategy.mode == CombinationStrategyAnalyzer.CombinationMode.EXTENSION) {
            records.putAll(buildRecordsForExtensions(strategy.extensionTypes, year, month, weekSeq, tagRules, orderCycleType));
            return records;
        }

        return records;
    }

    /**
     * 按扩展维度组合统计区域客户数。
     *
     * @param extensionTypes 扩展维度列表（如：[COUNTY, MARKET_TYPE]）
     * @param year           年份
     * @param month          月份
     * @param weekSeq        周序号
     * @param tagRules       标签过滤规则列表（可为空）
     * @param type           单周/双周类型
     * @return 区域名称到30档位客户数增量数组的映射
     */
    public Map<String, BigDecimal[]> buildRecordsForExtensions(
            List<DeliveryExtensionType> extensionTypes,
            Integer year, Integer month, Integer weekSeq,
            List<TagFilterRule> tagRules,
            OrderCycleType type) {
        Map<String, BigDecimal[]> records = new LinkedHashMap<>();
        if (extensionTypes == null || extensionTypes.isEmpty()) {
            records.putAll(buildRecordsWithTags(
                    REGION_FULL_CITY, year, month, weekSeq, Collections.emptyMap(), tagRules, type));
            return records;
        }

        DeliveryExtensionType primaryType = strategyAnalyzer.determinePrimaryExtension(extensionTypes);
        String primaryColumn = EXTENSION_TYPE_TO_COLUMN.get(primaryType);
        if (!StringUtils.hasText(primaryColumn)) {
            log.warn("两周一访上浮100%：无法识别主扩展类型列 {}", primaryType);
            return records;
        }

        List<String> selectColumns = new ArrayList<>();
        selectColumns.add(primaryColumn);
        extensionTypes.stream()
                .filter(t -> t != primaryType)
                .map(EXTENSION_TYPE_TO_COLUMN::get)
                .filter(StringUtils::hasText)
                .forEach(col -> {
                    if (!selectColumns.contains(col)) {
                        selectColumns.add(col);
                    }
                });

        List<Map<String, Object>> combinations = queryDistinctCombinations(year, month, weekSeq, selectColumns);
        for (Map<String, Object> combo : combinations) {
            String primaryValue = getStringValue(combo, primaryColumn);
            if (!StringUtils.hasText(primaryValue)) {
                continue;
            }
            Map<String, String> filters = new LinkedHashMap<>();
            filters.put(primaryColumn, primaryValue);

            List<String> subExtensions = new ArrayList<>();
            for (int i = 1; i < selectColumns.size(); i++) {
                String column = selectColumns.get(i);
                String value = getStringValue(combo, column);
                if (!StringUtils.hasText(value)) {
                    continue;
                }
                filters.put(column, value);
                subExtensions.add(value);
            }

            String regionName = buildRegionName(primaryType, primaryValue, subExtensions);
            records.putAll(buildRecordsWithTags(regionName, year, month, weekSeq, filters, tagRules, type));
        }
        return records;
    }

    /**
     * 按 TAG 组合生成区域记录。
     *
     * @param baseRegionName 区域基名（如："全市"、"江汉区"）
     * @param year           年份
     * @param month          月份
     * @param weekSeq        周序号
     * @param filters        额外过滤条件（列名 -> 值）
     * @param tagRules       标签规则列表（可为空）
     * @param type           单周/双周类型
     * @return 区域名称到30档位客户数增量数组的映射
     */
    public Map<String, BigDecimal[]> buildRecordsWithTags(
            String baseRegionName,
            Integer year, Integer month, Integer weekSeq,
            Map<String, String> filters,
            List<TagFilterRule> tagRules,
            OrderCycleType type) {
        Map<String, BigDecimal[]> records = new LinkedHashMap<>();
        if (tagRules == null || tagRules.isEmpty()) {
            BigDecimal[] grades = buildRecordForRegion(baseRegionName, year, month, weekSeq, filters, null, type);
            if (grades != null) {
                records.put(baseRegionName, grades);
            }
            return records;
        }

        for (TagFilterRule rule : tagRules) {
            String regionName = tagExtractionService.combineRegionWithTag(baseRegionName, rule.getTagName());
            BigDecimal[] grades = buildRecordForRegion(regionName, year, month, weekSeq, filters, rule, type);
            if (grades != null) {
                records.put(regionName, grades);
            }
        }
        return records;
    }

    /**
     * 针对单个区域（可含 TAG）统计 30 档客户数。
     *
     * @param regionName     区域名称（可拼 TAG，如："全市"、"江汉区-tag1"）
     * @param year           年份
     * @param month          月份
     * @param weekSeq        周序号
     * @param filters        额外过滤条件（列名 -> 值，可为空）
     * @param tagRule        标签过滤规则（可为 null）
     * @param orderCycleType 单周/双周类型
     * @return 长度30的档位数组（索引0对应D30，索引29对应D1），若无数据返回 null
     */
    public BigDecimal[] buildRecordForRegion(
            String regionName,
            Integer year, Integer month, Integer weekSeq,
            Map<String, String> filters,
            TagFilterRule tagRule,
            OrderCycleType orderCycleType) {
        String tagColumn = null;
        String tagOperator = null;
        Object tagValue = null;
        if (tagRule != null && tagRule.hasColumn()) {
            tagColumn = tagRule.getColumn();
            tagOperator = tagRule.getOperator();
            tagValue = tagRule.toSqlValue();
        }

        String orderCyclePattern = orderCycleType != null ? orderCycleType.getSqlPattern() : null;

        List<Map<String, Object>> gradeStats = filterCustomerTableRepository.statGradesPartition(
                year, month, weekSeq, filters, tagColumn, tagOperator, tagValue, orderCyclePattern);

        if (gradeStats.isEmpty()) {
            return null;
        }

        BigDecimal[] gradeCounts = new BigDecimal[30];
        Arrays.fill(gradeCounts, BigDecimal.ZERO);
        for (Map<String, Object> stat : gradeStats) {
            String grade = getStringValue(stat, "GRADE");
            Long count = getLongValue(stat, "CUSTOMER_COUNT");
            if (grade != null && count != null) {
                int index = parseGradeToIndex(grade);
                if (index >= 0 && index < 30) {
                    gradeCounts[index] = BigDecimal.valueOf(count);
                }
            }
        }
        log.debug("两周一访上浮100%：区域 {} ({}) 统计完成", regionName, orderCycleType);
        return gradeCounts;
    }

    /**
     * 查询分区表中指定列的去重组合。
     *
     * @param year    年份
     * @param month   月份
     * @param weekSeq 周序号
     * @param columns 列名列表（如：["COMPANY_DISTRICT", "MARKET_TYPE"]）
     * @return 去重组合列表，每个Map包含列名和对应的值
     */
    private List<Map<String, Object>> queryDistinctCombinations(Integer year, Integer month, Integer weekSeq, List<String> columns) {
        if (columns.isEmpty()) {
            return Collections.emptyList();
        }
        return filterCustomerTableRepository.listDistinctCombinationsPartition(year, month, weekSeq, columns);
    }

    /**
     * 订单周期类型枚举
     */
    public enum OrderCycleType {
        SINGLE("单周%"),
        DOUBLE("双周%");

        private final String sqlPattern;

        OrderCycleType(String sqlPattern) {
            this.sqlPattern = sqlPattern;
        }

        public String getSqlPattern() {
            return sqlPattern;
        }
    }
}

