package org.example.shared.util;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.shared.dto.RegionCustomerRecord;
import org.example.shared.exception.RegionNoCustomerException;
import org.example.shared.exception.IntegrityGroupMappingEmptyException;
import org.example.domain.repository.FilterCustomerTableRepository;
import org.example.domain.repository.IntegrityGroupMappingRepository;
import org.example.application.service.coordinator.TagExtractionService;
import org.example.domain.model.valueobject.DeliveryExtensionType;
import org.example.domain.model.tag.TagFilterRule;
import org.example.infrastructure.config.encoding.EncodingRuleRepository;
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

    private final FilterCustomerTableRepository filterCustomerTableRepository;
    private final TagExtractionService tagExtractionService;
    private final CombinationStrategyAnalyzer strategyAnalyzer;
    private final IntegrityGroupMappingRepository integrityGroupMappingRepository;
    private final EncodingRuleRepository encodingRuleRepository;

    /**
     * 为指定的投放组合构建区域客户数统计记录。
     *
     * @param deliveryMethod 投放方式（如："按档位投放"、"按价位段自选投放"）
     * @param deliveryEtype  扩展投放类型（如："区县公司+市场类型"）
     * @param tagRules       标签过滤规则列表
     * @param year           年份
     * @param month          月份
     * @param weekSeq        周序号
     * @return 区域客户数统计记录列表
     */
    public List<RegionCustomerRecord> buildRecordsForCombination(
            String deliveryMethod, String deliveryEtype, List<TagFilterRule> tagRules,
            Integer year, Integer month, Integer weekSeq) {

        CombinationStrategyAnalyzer.CombinationStrategy strategy = strategyAnalyzer.analyzeCombination(deliveryMethod, deliveryEtype);
        if (strategy.mode == CombinationStrategyAnalyzer.CombinationMode.SKIPPED) {
            log.info("投放类型 {} 当前不需要处理，已跳过", deliveryMethod);
            return Collections.emptyList();
        }

        if (strategy.mode == CombinationStrategyAnalyzer.CombinationMode.CITY) {
            return buildRecordsForFullCity(year, month, weekSeq, tagRules);
        }

        if (strategy.extensionTypes.isEmpty()) {
            log.warn("投放类型 {} 缺少可识别的扩展类型，默认按照全市处理", deliveryMethod);
            return buildRecordsForFullCity(year, month, weekSeq, tagRules);
        }

        return buildRecordsForExtensions(strategy.extensionTypes, year, month, weekSeq, tagRules);
    }

    /**
     * 构建全市区域的客户数统计记录。
     *
     * @param year    年份
     * @param month   月份
     * @param weekSeq 周序号
     * @param tagRules 标签过滤规则列表
     * @return 区域客户数统计记录列表
     */
    public List<RegionCustomerRecord> buildRecordsForFullCity(
            Integer year, Integer month, Integer weekSeq, List<TagFilterRule> tagRules) {
        return buildRecordsWithTags(REGION_FULL_CITY, year, month, weekSeq, Collections.emptyMap(), tagRules);
    }

    /**
     * 按扩展维度构建区域客户数统计记录。
     * <p>
     * 修改说明：
     * - 从 encoding-rules.yml 获取理论上的笛卡尔积全集
     * - 对于每个理论区域，查询 customer_filter 表
     * - 如果没有客户数据，将30个档位都置为0，并抛出 RegionNoCustomerException
     * </p>
     *
     * @param extensionTypes 扩展类型列表（如：[COUNTY, MARKET_TYPE]）
     * @param year           年份
     * @param month          月份
     * @param weekSeq        周序号
     * @param tagRules       标签过滤规则列表
     * @return 区域客户数统计记录列表
     * @throws RegionNoCustomerException 如果某个区域在 customer_filter 表中没有客户数据
     */
    public List<RegionCustomerRecord> buildRecordsForExtensions(
            List<DeliveryExtensionType> extensionTypes,
            Integer year, Integer month, Integer weekSeq,
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

        // 获取理论上的笛卡尔积全集
        // 强制要求：对于诚信互助小组，必须从 integrity_group_code_mapping 表获取所有可能的 GROUP_CODE 来计算理论全集
        // 如果查询失败或表为空，直接抛出异常，不允许使用后备方案
        List<TheoreticalRegion> theoreticalRegions = buildTheoreticalRegions(extensionTypes, primaryType);
        log.debug("构建理论区域全集: 扩展类型={}, 理论区域数={}", extensionTypes, theoreticalRegions.size());

        // 对于每个理论区域，查询 customer_filter 表并构建记录
        List<RegionNoCustomerException> noCustomerExceptions = new ArrayList<>();
        for (TheoreticalRegion theoreticalRegion : theoreticalRegions) {
            try {
            Map<String, String> filters = new LinkedHashMap<>();
                filters.put(primaryColumn, theoreticalRegion.primaryValue);
                for (int i = 0; i < theoreticalRegion.subColumns.size(); i++) {
                    filters.put(theoreticalRegion.subColumns.get(i), theoreticalRegion.subValues.get(i));
                }

                String regionName = buildRegionName(primaryType, theoreticalRegion.primaryValue, theoreticalRegion.subValues);
                List<RegionCustomerRecord> regionRecords = buildRecordsWithTags(regionName, year, month, weekSeq, filters, tagRules);
                
                // 检查是否有客户数据（30个档位是否全为0）
                for (RegionCustomerRecord record : regionRecords) {
                    if (isAllGradesZero(record)) {
                        // 30个档位全为0，抛出异常
                        RegionNoCustomerException exception = new RegionNoCustomerException(
                                record.getRegion(), year, month, weekSeq);
                        noCustomerExceptions.add(exception);
                        log.warn("区域 '{}' 在时间分区 {}-{}-{} 中没有客户数据（30个档位全为0）", 
                                record.getRegion(), year, month, weekSeq);
                        // 仍然将记录添加到列表中（写入 region_customer_statistics 表，30个档位全为0）
                        records.add(record);
                    } else {
                        records.add(record);
                    }
                }
            } catch (Exception e) {
                log.error("构建区域记录时发生错误: 理论区域={}, 错误={}", theoreticalRegion, e.getMessage(), e);
            }
        }

        // 如果有区域无客户数据，抛出异常
        if (!noCustomerExceptions.isEmpty()) {
            // 记录所有无客户数据的区域
            StringBuilder errorMsg = new StringBuilder("以下区域在 customer_filter 表中没有客户数据（30个档位全为0）:\n");
            for (RegionNoCustomerException ex : noCustomerExceptions) {
                errorMsg.append("  - ").append(ex.getRegionName()).append("\n");
            }
            log.error(errorMsg.toString());
            
            // 抛出第一个异常（调用者可以捕获并处理）
            throw noCustomerExceptions.get(0);
        }

        return records;
    }

    /**
     * 构建理论上的区域全集（笛卡尔积）
     */
    private List<TheoreticalRegion> buildTheoreticalRegions(
            List<DeliveryExtensionType> extensionTypes,
            DeliveryExtensionType primaryType) {
        
        List<TheoreticalRegion> regions = new ArrayList<>();
        
        // 获取主扩展类型的理论区域集合
        List<String> primaryRegions = getTheoreticalRegionsForType(primaryType);
        if (primaryRegions.isEmpty()) {
            log.warn("主扩展类型 {} 没有理论区域定义", primaryType);
            return regions;
        }

        // 获取子扩展类型的理论区域集合
        List<DeliveryExtensionType> subTypes = new ArrayList<>();
        List<String> subColumns = new ArrayList<>();
        for (DeliveryExtensionType type : extensionTypes) {
            if (type != primaryType) {
                subTypes.add(type);
                String column = EXTENSION_TYPE_TO_COLUMN.get(type);
                if (StringUtils.hasText(column)) {
                    subColumns.add(column);
                }
            }
        }

        // 计算笛卡尔积
        if (subTypes.isEmpty()) {
            // 单扩展类型
            for (String primaryValue : primaryRegions) {
                regions.add(new TheoreticalRegion(primaryValue, Collections.emptyList(), Collections.emptyList()));
            }
        } else if (subTypes.size() == 1) {
            // 双扩展类型：计算笛卡尔积
            DeliveryExtensionType subType = subTypes.get(0);
            List<String> subRegions = getTheoreticalRegionsForType(subType);
            for (String primaryValue : primaryRegions) {
                for (String subValue : subRegions) {
                    regions.add(new TheoreticalRegion(primaryValue, 
                            Collections.singletonList(subColumns.get(0)), 
                            Collections.singletonList(subValue)));
                }
            }
        } else {
            log.warn("暂不支持超过2个扩展类型的笛卡尔积计算: {}", extensionTypes);
        }

        return regions;
    }

    /**
     * 获取指定扩展类型的理论区域集合（从 encoding-rules.yml 或数据库）
     */
    private List<String> getTheoreticalRegionsForType(DeliveryExtensionType type) {
        // 特殊处理：诚信互助小组从数据库获取
        if (type == DeliveryExtensionType.INTEGRITY_GROUP) {
            return getIntegrityGroupCodes();
        }

        Map<String, String> regionCodeMap = encodingRuleRepository.getRegionCodeMap(type);
        if (regionCodeMap == null || regionCodeMap.isEmpty()) {
            return Collections.emptyList();
        }

        // 获取所有唯一的区域名称（去重：多个别名可能映射到同一个编码）
        Set<String> uniqueRegions = new LinkedHashSet<>();
        Map<String, String> reverseMap = encodingRuleRepository.getReverseRegionCodeMap(type);
        
        // 优先使用规范标签（reverseMap 中的值）
        for (String canonicalLabel : reverseMap.values()) {
            uniqueRegions.add(canonicalLabel);
        }
        
        // 如果 reverseMap 为空，使用 regionCodeMap 的 keySet（可能包含别名）
        if (uniqueRegions.isEmpty()) {
            uniqueRegions.addAll(regionCodeMap.keySet());
        }

        return new ArrayList<>(uniqueRegions);
    }

    /**
     * 从数据库获取诚信互助小组编码列表
     * 
     * 强制要求：integrity_group_code_mapping 表必须有数据，如果查询失败或表为空，直接抛出异常
     * 
     * @return 诚信互助小组编码列表
     * @throws IntegrityGroupMappingEmptyException 如果查询失败或表为空
     */
    private List<String> getIntegrityGroupCodes() {
        try {
            // 从 integrity_group_code_mapping 表获取所有 GROUP_CODE
            List<org.example.infrastructure.persistence.po.IntegrityGroupMappingPO> mappings = 
                    integrityGroupMappingRepository.selectAllOrderBySort();
            
            if (mappings == null || mappings.isEmpty()) {
                throw new IntegrityGroupMappingEmptyException(
                        "integrity_group_code_mapping 表为空，无法获取诚信互助小组编码列表。" +
                        "请确保已导入 base_customer_info 数据并同步生成 integrity_group_code_mapping 表。");
            }
            
            List<String> codes = new ArrayList<>();
            for (org.example.infrastructure.persistence.po.IntegrityGroupMappingPO mapping : mappings) {
                if (mapping.getGroupCode() != null && !mapping.getGroupCode().trim().isEmpty()) {
                    codes.add(mapping.getGroupCode());
                }
            }
            
            if (codes.isEmpty()) {
                throw new IntegrityGroupMappingEmptyException(
                        "integrity_group_code_mapping 表中没有有效的 GROUP_CODE，" +
                        "所有记录的 GROUP_CODE 均为空。请检查数据完整性。");
            }
            
            log.debug("从 integrity_group_code_mapping 表获取到 {} 个诚信互助小组编码", codes.size());
            return codes;
        } catch (IntegrityGroupMappingEmptyException e) {
            // 重新抛出已知异常
            throw e;
        } catch (Exception e) {
            log.error("获取诚信互助小组编码列表失败", e);
            throw new IntegrityGroupMappingEmptyException(
                    "查询 integrity_group_code_mapping 表失败: " + e.getMessage(), e);
        }
    }


    /**
     * 检查区域记录的30个档位是否全为0
     */
    private boolean isAllGradesZero(RegionCustomerRecord record) {
        if (record == null || record.getGrades() == null) {
            return true;
        }
        BigDecimal[] grades = record.getGrades();
        for (BigDecimal count : grades) {
            if (count != null && count.compareTo(BigDecimal.ZERO) > 0) {
                return false;
            }
        }
        return true;
    }

    /**
     * 理论区域内部类
     */
    private static class TheoreticalRegion {
        final String primaryValue;
        final List<String> subColumns;
        final List<String> subValues;

        TheoreticalRegion(String primaryValue, List<String> subColumns, List<String> subValues) {
            this.primaryValue = primaryValue;
            this.subColumns = subColumns;
            this.subValues = subValues;
        }

        @Override
        public String toString() {
            return String.format("TheoreticalRegion{primary=%s, sub=%s}", primaryValue, subValues);
        }
    }

    /**
     * 按标签规则构建区域客户数统计记录。
     *
     * @param baseRegionName 基础区域名称（如："全市"、"江汉区"）
     * @param year           年份
     * @param month          月份
     * @param weekSeq        周序号
     * @param filters        过滤条件（列名 -> 值）
     * @param tagRules       标签过滤规则列表
     * @return 区域客户数统计记录列表
     */
    public List<RegionCustomerRecord> buildRecordsWithTags(
            String baseRegionName,
            Integer year, Integer month, Integer weekSeq,
            Map<String, String> filters,
            List<TagFilterRule> tagRules) {

        List<RegionCustomerRecord> records = new ArrayList<>();
        if (tagRules == null || tagRules.isEmpty()) {
            RegionCustomerRecord record =
                    buildRecordForRegion(baseRegionName, year, month, weekSeq, filters, null);
            if (record != null) {
                records.add(record);
            }
            return records;
        }

        for (TagFilterRule rule : tagRules) {
            String regionName = tagExtractionService.combineRegionWithTag(baseRegionName, rule.getTagName());
            RegionCustomerRecord record =
                    buildRecordForRegion(regionName, year, month, weekSeq, filters, rule);
            if (record != null) {
                records.add(record);
            }
        }
        return records;
    }

    /**
     * 为指定区域构建客户数统计记录。
     *
     * @param regionName 区域名称（可含标签，如："全市"、"江汉区-tag1"）
     * @param year       年份
     * @param month      月份
     * @param weekSeq    周序号
     * @param filters    过滤条件（列名 -> 值，可为空）
     * @param tagRule    标签过滤规则（可为 null）
     * @return 区域客户数统计记录，若无数据返回 null
     */
    public RegionCustomerRecord buildRecordForRegion(
            String regionName,
            Integer year, Integer month, Integer weekSeq,
            Map<String, String> filters, TagFilterRule tagRule) {

        List<Map<String, Object>> gradeStats = filterCustomerTableRepository.statGradesPartition(
                year, month, weekSeq, filters,
                tagRule != null ? tagRule.getColumn() : null,
                tagRule != null ? tagRule.getOperator() : null,
                tagRule != null ? tagRule.toSqlValue() : null,
                null); // orderCyclePattern 暂不需要，传 null

        // 构建30个档位的客户数数组
        BigDecimal[] gradeCounts = new BigDecimal[30];
        Arrays.fill(gradeCounts, BigDecimal.ZERO);
        BigDecimal total = BigDecimal.ZERO;
        
        // 记录查询到的档位数量，用于调试（仅对"全市"区域）
        if ("全市".equals(regionName) || regionName.contains("全市")) {
            log.warn("【构建区域记录】区域={}, filters={}, tagRule={}, 查询到的档位记录数={}", 
                    regionName, filters, tagRule != null ? tagRule.getColumn() + " " + tagRule.getOperator() + " " + tagRule.toSqlValue() : "null", gradeStats.size());
            // 打印所有档位数据用于调试
            for (int i = 0; i < gradeStats.size(); i++) {
                Map<String, Object> stat = gradeStats.get(i);
                String grade = getStringValue(stat, "GRADE");
                Long count = getLongValue(stat, "CUSTOMER_COUNT");
                int gradeIndex = parseGradeToIndex(grade);
                log.warn("  -> SQL查询结果[{}]: 档位={}, 客户数={}, 解析索引={}", i, grade, count, gradeIndex);
            }
        }
        
        // 统计解析失败的档位（用于诊断问题）
        Map<String, Long> failedGrades = new HashMap<>();
        long totalFailedCount = 0;
        long totalSuccessCount = 0;

        for (Map<String, Object> stat : gradeStats) {
            String grade = getStringValue(stat, "GRADE");
            Long count = getLongValue(stat, "CUSTOMER_COUNT");

            if (grade != null && count != null) {
                // 将档位转换为索引（D30=0, D29=1, ..., D1=29）
                int gradeIndex = parseGradeToIndex(grade);
                if (gradeIndex >= 0 && gradeIndex < 30) {
                    // 使用累加而不是直接赋值，避免同一档位索引被多次赋值时覆盖
                    BigDecimal oldValue = gradeCounts[gradeIndex];
                    gradeCounts[gradeIndex] = gradeCounts[gradeIndex].add(BigDecimal.valueOf(count));
                    total = total.add(BigDecimal.valueOf(count));
                    totalSuccessCount += count;
                    
                    // 调试日志：仅对"全市"区域记录所有档位的映射情况
                    if ("全市".equals(regionName) || regionName.contains("全市")) {
                        log.warn("【档位映射】区域={}, 档位={}, 客户数={}, 解析索引={}, 旧值={}, 新值={}", 
                                regionName, grade, count, gradeIndex, oldValue, gradeCounts[gradeIndex]);
                    }
                } else {
                    // 记录解析失败的档位（这是导致D30-D1统计不正确的原因）
                    failedGrades.put(grade, count);
                    totalFailedCount += count;
                    log.warn("【档位解析失败】区域={}, 档位={}, 客户数={}, 解析索引={} (索引无效，无法映射到D30-D1)", 
                            regionName, grade, count, gradeIndex);
                }
            }
        }
        
        // 如果有解析失败的档位，记录汇总信息
        if (!failedGrades.isEmpty()) {
            log.warn("【档位统计问题】区域 {} 存在解析失败的档位: 失败档位数={}, 失败客户总数={}, 成功客户总数={}, 失败档位详情={}", 
                    regionName, failedGrades.size(), totalFailedCount, totalSuccessCount, failedGrades);
        }
        
        // 计算实际映射到D30-D1的客户总数
        BigDecimal mappedTotal = BigDecimal.ZERO;
        for (BigDecimal count : gradeCounts) {
            if (count != null) {
                mappedTotal = mappedTotal.add(count);
            }
        }
        
        // 验证：如果mappedTotal与total不一致，说明有档位解析失败
        if (mappedTotal.compareTo(total) != 0) {
            log.warn("【档位统计不一致】区域 {}: mappedTotal(D30-D1之和)={}, calculatedTotal(成功解析的档位之和)={}, 差异={}", 
                    regionName, mappedTotal, total, mappedTotal.subtract(total).abs());
        }
        
        // 调试日志：仅对"全市"区域打印档位数组的详细信息
        if ("全市".equals(regionName) || regionName.contains("全市")) {
            int nonZeroCount = 0;
            BigDecimal sum = BigDecimal.ZERO;
            for (int i = 0; i < gradeCounts.length; i++) {
                if (gradeCounts[i] != null && gradeCounts[i].compareTo(BigDecimal.ZERO) > 0) {
                    nonZeroCount++;
                    sum = sum.add(gradeCounts[i]);
                    log.warn("【档位数组】区域={}, D{}={}", regionName, 30-i, gradeCounts[i]);
                }

            }
            log.warn("【档位数组汇总】区域={}, 非零档位数={}, D30-D1之和={}, TOTAL={}", 
                    regionName, nonZeroCount, sum, total);
        }

        return new RegionCustomerRecord(regionName, gradeCounts, total);
    }
}


