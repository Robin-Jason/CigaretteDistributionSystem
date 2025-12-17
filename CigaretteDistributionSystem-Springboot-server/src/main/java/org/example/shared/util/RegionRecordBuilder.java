package org.example.shared.util;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.shared.dto.RegionCustomerRecord;
import org.example.domain.repository.TemporaryCustomerTableRepository;
import org.example.application.service.coordinator.TagExtractionService;
import org.example.domain.model.valueobject.DeliveryExtensionType;
import org.example.domain.model.tag.TagFilterRule;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.util.*;

import static org.example.shared.util.GradeParser.parseGradeToIndex;
import static org.example.shared.util.MapValueExtractor.getLongValue;
import static org.example.shared.util.MapValueExtractor.getStringValue;
import static org.example.shared.util.RegionNameBuilder.buildRegionName;

/**
 * 区域客户数记录构建器
 * <p>
 * 负责根据投放组合策略构建区域客户数统计记录。
 * </p>
 *
 * @author Robin
 * @version 1.0
 * @since 2025-12-11
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RegionRecordBuilder {

    private static final String REGION_FULL_CITY = "全市";

    // 扩展类型到base_customer_info列名的映射
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
     * 为指定的投放组合构建区域客户数统计记录。
     *
     * @param deliveryMethod   投放方式（如："按档位投放"、"按价位段自选投放"）
     * @param deliveryEtype    扩展投放类型（如："区县公司+市场类型"）
     * @param tagRules         标签过滤规则列表
     * @param temporaryTableName 临时表名
     * @return 区域客户数统计记录列表
     * @example
     * <pre>
     *     List<TagFilterRule> tagRules = Collections.emptyList();
     *     List<RegionCustomerRecord> records = builder.buildRecordsForCombination(
     *         "按档位投放", null, tagRules, "temp_customer_filter_2025_9_3"
     *     );
     *     // 返回: [RegionCustomerRecord(region="全市", gradeCounts=[...], total=...)]
     * </pre>
     */
    public List<RegionCustomerRecord> buildRecordsForCombination(
            String deliveryMethod, String deliveryEtype, List<TagFilterRule> tagRules, String temporaryTableName) {

        CombinationStrategyAnalyzer.CombinationStrategy strategy = strategyAnalyzer.analyzeCombination(deliveryMethod, deliveryEtype);
        if (strategy.mode == CombinationStrategyAnalyzer.CombinationMode.SKIPPED) {
            log.info("投放类型 {} 当前不需要处理，已跳过", deliveryMethod);
            return Collections.emptyList();
        }

        if (strategy.mode == CombinationStrategyAnalyzer.CombinationMode.CITY) {
            return buildRecordsForFullCity(temporaryTableName, tagRules);
        }

        if (strategy.extensionTypes.isEmpty()) {
            log.warn("投放类型 {} 缺少可识别的扩展类型，默认按照全市处理", deliveryMethod);
            return buildRecordsForFullCity(temporaryTableName, tagRules);
        }

        return buildRecordsForExtensions(strategy.extensionTypes, temporaryTableName, tagRules);
    }

    /**
     * 构建全市区域的客户数统计记录。
     *
     * @param temporaryTableName 临时表名
     * @param tagRules           标签过滤规则列表
     * @return 区域客户数统计记录列表
     * @example
     * <pre>
     *     List<TagFilterRule> tagRules = Collections.emptyList();
     *     List<RegionCustomerRecord> records = builder.buildRecordsForFullCity(
     *         "temp_customer_filter_2025_9_3", tagRules
     *     );
     *     // 返回: [RegionCustomerRecord(region="全市", ...)]
     * </pre>
     */
    public List<RegionCustomerRecord> buildRecordsForFullCity(
            String temporaryTableName, List<TagFilterRule> tagRules) {
        return buildRecordsWithTags(REGION_FULL_CITY, temporaryTableName, Collections.emptyMap(), tagRules);
    }

    /**
     * 按扩展维度构建区域客户数统计记录。
     *
     * @param extensionTypes    扩展类型列表（如：[COUNTY, MARKET_TYPE]）
     * @param temporaryTableName 临时表名
     * @param tagRules          标签过滤规则列表
     * @return 区域客户数统计记录列表
     * @example
     * <pre>
     *     List<DeliveryExtensionType> types = Arrays.asList(
     *         DeliveryExtensionType.COUNTY, DeliveryExtensionType.MARKET_TYPE
     *     );
     *     List<RegionCustomerRecord> records = builder.buildRecordsForExtensions(
     *         types, "temp_customer_filter_2025_9_3", Collections.emptyList()
     *     );
     *     // 返回: [RegionCustomerRecord(region="江汉区（城区）", ...), ...]
     * </pre>
     */
    public List<RegionCustomerRecord> buildRecordsForExtensions(
            List<DeliveryExtensionType> extensionTypes,
            String temporaryTableName,
            List<TagFilterRule> tagRules) {

        List<RegionCustomerRecord> records = new ArrayList<>();
        DeliveryExtensionType primaryType = strategyAnalyzer.determinePrimaryExtension(extensionTypes);
        String primaryColumn = EXTENSION_TYPE_TO_COLUMN.get(primaryType);
        if (!StringUtils.hasText(primaryColumn)) {
            log.warn("无法识别主扩展类型对应的列: {}", primaryType);
            return records;
        }

        List<String> selectColumns = new ArrayList<>();
        selectColumns.add(primaryColumn);

        extensionTypes.stream()
                .filter(type -> type != primaryType)
                .map(EXTENSION_TYPE_TO_COLUMN::get)
                .filter(StringUtils::hasText)
                .forEach(column -> {
                    if (!selectColumns.contains(column)) {
                        selectColumns.add(column);
                    }
                });

        if (selectColumns.isEmpty()) {
            return records;
        }

        List<Map<String, Object>> combinations = temporaryCustomerTableRepository.listDistinctCombinations(temporaryTableName, selectColumns);
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
            records.addAll(buildRecordsWithTags(regionName, temporaryTableName, filters, tagRules));
        }

        return records;
    }

    /**
     * 按标签规则构建区域客户数统计记录。
     *
     * @param baseRegionName    基础区域名称（如："全市"、"江汉区"）
     * @param temporaryTableName 临时表名
     * @param filters           过滤条件（列名 -> 值）
     * @param tagRules          标签过滤规则列表
     * @return 区域客户数统计记录列表
     * @example
     * <pre>
     *     Map<String, String> filters = new HashMap<>();
     *     filters.put("COMPANY_DISTRICT", "江汉区");
     *     List<TagFilterRule> tagRules = Arrays.asList(
     *         new TagFilterRule("tag1", "COLUMN1", "=", "value1")
     *     );
     *     List<RegionCustomerRecord> records = builder.buildRecordsWithTags(
     *         "江汉区", "temp_customer_filter_2025_9_3", filters, tagRules
     *     );
     *     // 返回: [RegionCustomerRecord(region="江汉区-tag1", ...)]
     * </pre>
     */
    public List<RegionCustomerRecord> buildRecordsWithTags(
            String baseRegionName,
            String temporaryTableName,
            Map<String, String> filters,
            List<TagFilterRule> tagRules) {

        List<RegionCustomerRecord> records = new ArrayList<>();
        if (tagRules == null || tagRules.isEmpty()) {
            RegionCustomerRecord record =
                    buildRecordForRegion(baseRegionName, temporaryTableName, filters, null);
            if (record != null) {
                records.add(record);
            }
            return records;
        }

        for (TagFilterRule rule : tagRules) {
            String regionName = tagExtractionService.combineRegionWithTag(baseRegionName, rule.getTagName());
            RegionCustomerRecord record =
                    buildRecordForRegion(regionName, temporaryTableName, filters, rule);
            if (record != null) {
                records.add(record);
            }
        }
        return records;
    }

    /**
     * 为指定区域构建客户数统计记录。
     *
     * @param regionName        区域名称（可含标签，如："全市"、"江汉区-tag1"）
     * @param temporaryTableName 临时表名
     * @param filters           过滤条件（列名 -> 值，可为空）
     * @param tagRule           标签过滤规则（可为 null）
     * @return 区域客户数统计记录，若无数据返回 null
     * @example
     * <pre>
     *     Map<String, String> filters = new HashMap<>();
     *     filters.put("COMPANY_DISTRICT", "江汉区");
     *     TagFilterRule tagRule = new TagFilterRule("tag1", "MARKET_TYPE", "=", "城区");
     *     RegionCustomerRecord record = builder.buildRecordForRegion(
     *         "江汉区-tag1", "temp_customer_filter_2025_9_3", filters, tagRule
     *     );
     *     // 返回: RegionCustomerRecord(region="江汉区-tag1", gradeCounts=[30档位数组], total=总客户数)
     * </pre>
     */
    public RegionCustomerRecord buildRecordForRegion(
            String regionName, String temporaryTableName,
            Map<String, String> filters, TagFilterRule tagRule) {

        List<Map<String, Object>> gradeStats = temporaryCustomerTableRepository.statGrades(
                temporaryTableName, filters,
                tagRule != null ? tagRule.getColumn() : null,
                tagRule != null ? tagRule.getOperator() : null,
                tagRule != null ? tagRule.toSqlValue() : null,
                null); // orderCyclePattern 暂不需要，传 null

        // 构建30个档位的客户数数组
        BigDecimal[] gradeCounts = new BigDecimal[30];
        Arrays.fill(gradeCounts, BigDecimal.ZERO);
        BigDecimal total = BigDecimal.ZERO;

        for (Map<String, Object> stat : gradeStats) {
            String grade = getStringValue(stat, "GRADE");
            Long count = getLongValue(stat, "CUSTOMER_COUNT");

            if (grade != null && count != null) {
                // 将档位转换为索引（D30=0, D29=1, ..., D1=29）
                int gradeIndex = parseGradeToIndex(grade);
                if (gradeIndex >= 0 && gradeIndex < 30) {
                    gradeCounts[gradeIndex] = BigDecimal.valueOf(count);
                    total = total.add(BigDecimal.valueOf(count));
                }
            }
        }

        return new RegionCustomerRecord(regionName, gradeCounts, total);
    }
}

