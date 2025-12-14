package org.example.shared.util;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.domain.repository.TemporaryCustomerTableRepository;
import org.example.application.service.TagExtractionService;
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

    private final TemporaryCustomerTableRepository temporaryCustomerTableRepository;
    private final TagExtractionService tagExtractionService;
    private final CombinationStrategyAnalyzer strategyAnalyzer;

    /**
     * 针对单周/双周客户重新统计区域矩阵（按投放组合策略）。
     * <p>
     * 根据投放组合策略（城市级或扩展维度）统计单周/双周客户的区域客户数增量。
     * </p>
     *
     * @param strategy       组合策略（包含模式：CITY/EXTENSION/SKIPPED，以及扩展类型列表）
     * @param temporaryTable 临时表名
     * @param tagRules       标签过滤规则列表（可为空）
     * @param orderCycleType 单周或双周类型
     * @return 区域名称到30档位客户数增量数组的映射
     * @example
     * <pre>
     *     CombinationStrategyAnalyzer.CombinationStrategy strategy = new CombinationStrategyAnalyzer.CombinationStrategy();
     *     strategy.mode = CombinationStrategyAnalyzer.CombinationMode.CITY;
     *     List<TagFilterRule> tagRules = Collections.emptyList();
     *     Map<String, BigDecimal[]> increments = calculator.calculateOrderCycleMatrix(
     *         strategy, "temp_customer_filter_2025_9_3", tagRules, OrderCycleType.SINGLE
     *     );
     *     // 返回: {"全市": [BigDecimal数组，30个档位的单周客户数]}
     * </pre>
     */
    public Map<String, BigDecimal[]> calculateOrderCycleMatrix(
            CombinationStrategyAnalyzer.CombinationStrategy strategy,
            String temporaryTable,
            List<TagFilterRule> tagRules,
            OrderCycleType orderCycleType) {
        Map<String, BigDecimal[]> records = new LinkedHashMap<>();
        if (strategy.mode == CombinationStrategyAnalyzer.CombinationMode.CITY) {
            records.putAll(buildRecordsWithTags(
                    REGION_FULL_CITY, temporaryTable, Collections.emptyMap(), tagRules, orderCycleType));
            return records;
        }

        if (strategy.mode == CombinationStrategyAnalyzer.CombinationMode.EXTENSION) {
            records.putAll(buildRecordsForExtensions(strategy.extensionTypes, temporaryTable, tagRules, orderCycleType));
            return records;
        }

        return records;
    }

    /**
     * 按扩展维度组合统计区域客户数。
     * <p>
     * 根据扩展类型（如区县公司、市场类型等）组合查询临时表中的去重组合，
     * 然后为每个组合统计单周/双周客户的30档位客户数。
     * </p>
     *
     * @param extensionTypes 扩展维度列表（如：[COUNTY, MARKET_TYPE]）
     * @param temporaryTable  临时表名
     * @param tagRules        标签过滤规则列表（可为空）
     * @param type            单周/双周类型
     * @return 区域名称到30档位客户数增量数组的映射
     * @example
     * <pre>
     *     List<DeliveryExtensionType> types = Arrays.asList(
     *         DeliveryExtensionType.COUNTY, DeliveryExtensionType.MARKET_TYPE
     *     );
     *     Map<String, BigDecimal[]> records = calculator.buildRecordsForExtensions(
     *         types, "temp_customer_filter_2025_9_3", Collections.emptyList(), OrderCycleType.SINGLE
     *     );
     *     // 返回: {"江汉区（城区）": [30档位数组], "江汉区（郊区）": [30档位数组], ...}
     * </pre>
     */
    public Map<String, BigDecimal[]> buildRecordsForExtensions(
            List<DeliveryExtensionType> extensionTypes,
            String temporaryTable,
            List<TagFilterRule> tagRules,
            OrderCycleType type) {
        Map<String, BigDecimal[]> records = new LinkedHashMap<>();
        if (extensionTypes == null || extensionTypes.isEmpty()) {
            records.putAll(buildRecordsWithTags(
                    REGION_FULL_CITY, temporaryTable, Collections.emptyMap(), tagRules, type));
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

        List<Map<String, Object>> combinations = queryDistinctCombinations(temporaryTable, selectColumns);
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
            records.putAll(buildRecordsWithTags(regionName, temporaryTable, filters, tagRules, type));
        }
        return records;
    }

    /**
     * 按 TAG 组合生成区域记录。
     * <p>
     * 如果存在标签规则，则为每个标签规则生成带标签的区域记录；
     * 否则直接使用基础区域名生成记录。
     * </p>
     *
     * @param baseRegionName 区域基名（如："全市"、"江汉区"）
     * @param temporaryTable 临时表名
     * @param filters        额外过滤条件（列名 -> 值）
     * @param tagRules       标签规则列表（可为空）
     * @param type           单周/双周类型
     * @return 区域名称到30档位客户数增量数组的映射
     * @example
     * <pre>
     *     Map<String, String> filters = new HashMap<>();
     *     filters.put("COMPANY_DISTRICT", "江汉区");
     *     List<TagFilterRule> tagRules = Arrays.asList(
     *         new TagFilterRule("tag1", "COLUMN1", "=", "value1")
     *     );
     *     Map<String, BigDecimal[]> records = calculator.buildRecordsWithTags(
     *         "江汉区", "temp_customer_filter_2025_9_3", filters, tagRules, OrderCycleType.SINGLE
     *     );
     *     // 返回: {"江汉区-tag1": [30档位数组]}
     * </pre>
     */
    public Map<String, BigDecimal[]> buildRecordsWithTags(
            String baseRegionName,
            String temporaryTable,
            Map<String, String> filters,
            List<TagFilterRule> tagRules,
            OrderCycleType type) {
        Map<String, BigDecimal[]> records = new LinkedHashMap<>();
        if (tagRules == null || tagRules.isEmpty()) {
            BigDecimal[] grades = buildRecordForRegion(baseRegionName, temporaryTable, filters, null, type);
            if (grades != null) {
                records.put(baseRegionName, grades);
            }
            return records;
        }

        for (TagFilterRule rule : tagRules) {
            String regionName = tagExtractionService.combineRegionWithTag(baseRegionName, rule.getTagName());
            BigDecimal[] grades = buildRecordForRegion(regionName, temporaryTable, filters, rule, type);
            if (grades != null) {
                records.put(regionName, grades);
            }
        }
        return records;
    }

    /**
     * 针对单个区域（可含 TAG）统计 30 档客户数。
     * <p>
     * 根据过滤条件和标签规则，从临时表中统计指定订单周期类型（单周/双周）的30档位客户数。
     * </p>
     *
     * @param regionName     区域名称（可拼 TAG，如："全市"、"江汉区-tag1"）
     * @param temporaryTable 临时表名
     * @param filters        额外过滤条件（列名 -> 值，可为空）
     * @param tagRule        标签过滤规则（可为 null）
     * @param orderCycleType 单周/双周类型
     * @return 长度30的档位数组（索引0对应D30，索引29对应D1），若无数据返回 null
     * @example
     * <pre>
     *     Map<String, String> filters = new HashMap<>();
     *     filters.put("COMPANY_DISTRICT", "江汉区");
     *     TagFilterRule tagRule = new TagFilterRule("tag1", "MARKET_TYPE", "=", "城区");
     *     BigDecimal[] grades = calculator.buildRecordForRegion(
     *         "江汉区-tag1", "temp_customer_filter_2025_9_3", filters, tagRule, OrderCycleType.SINGLE
     *     );
     *     // 返回: [BigDecimal数组，30个档位的单周客户数]
     *     // grades[0] 对应 D30 档位，grades[29] 对应 D1 档位
     * </pre>
     */
    public BigDecimal[] buildRecordForRegion(
            String regionName,
            String temporaryTable,
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

        List<Map<String, Object>> gradeStats = temporaryCustomerTableRepository.statGrades(
                temporaryTable, filters, tagColumn, tagOperator, tagValue, orderCyclePattern);

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
     * 查询临时表中指定列的去重组合。
     *
     * @param tableName 临时表名
     * @param columns   列名列表（如：["COMPANY_DISTRICT", "MARKET_TYPE"]）
     * @return 去重组合列表，每个Map包含列名和对应的值
     * @example
     * <pre>
     *     List<String> columns = Arrays.asList("COMPANY_DISTRICT", "MARKET_TYPE");
     *     List<Map<String, Object>> combinations = calculator.queryDistinctCombinations(
     *         "temp_customer_filter_2025_9_3", columns
     *     );
     *     // 返回: [{"COMPANY_DISTRICT": "江汉区", "MARKET_TYPE": "城区"}, ...]
     * </pre>
     */
    private List<Map<String, Object>> queryDistinctCombinations(String tableName, List<String> columns) {
        if (columns.isEmpty()) {
            return Collections.emptyList();
        }
        return temporaryCustomerTableRepository.listDistinctCombinations(tableName, columns);
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

