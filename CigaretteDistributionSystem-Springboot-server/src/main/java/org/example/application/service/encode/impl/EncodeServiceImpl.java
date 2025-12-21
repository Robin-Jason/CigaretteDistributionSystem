package org.example.application.service.encode.impl;

import lombok.extern.slf4j.Slf4j;
import org.example.application.service.encode.EncodeService;
import org.example.domain.model.valueobject.DeliveryExtensionType;
import org.example.domain.service.rule.EncodingRule;
import org.example.infrastructure.config.encoding.EncodingRuleRepository;
import org.example.infrastructure.persistence.po.CigaretteDistributionPredictionPO;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

/**
 * 语义编码解码服务实现（精简版，仅保留分配流程所需的单区域编码）。
 *
 * @author Robin
 * @version 4.1
 * @since 2025-12-11
 */
@Slf4j
@Service
public class EncodeServiceImpl implements EncodeService {

    private static final EncodingRule ENCODING_RULE = new org.example.domain.service.rule.impl.EncodingRuleImpl();

    private final EncodingRuleRepository encodingRuleRepository;

    public EncodeServiceImpl(EncodingRuleRepository encodingRuleRepository) {
        this.encodingRuleRepository = encodingRuleRepository;
    }

    /**
     * 为特定区域生成编码表达式（单卷烟单区域）。
     * <p>
     * 根据投放方式、扩展类型、标签和目标区域，生成语义编码表达式。
     * 编码格式：&lt;投放类型&gt;&lt;扩展投放类型+标签&gt;（&lt;区域编码&gt;）（&lt;投放量编码&gt;）
     * <p>
     * - 投放类型：A/B/C/D/E（按档位投放 / 按档位扩展投放 / 按价位段自选投放 / ...）<br/>
     * - 扩展投放类型：如 1、2、1-4 等，由配置中的扩展类型编码决定<br/>
     * - 标签：例如"优质数据共享客户" → 记为 {@code +a}（可扩展更多标签映射）<br/>
     * - 区域编码：如 Q2、M1、Y5 等，由区域维度与区域名称映射得到<br/>
     * - 投放量编码：对该区域 30 个档位投放量按连续相同值聚合（由 {@link EncodingRule#encodeGradeSequences} 实现）<br/>
     * 特殊规则：当投放区域为"全市"时，不再单独编码区域部分，直接写入投放量编码。
     * </p>
     * </p>
     *
     * @param cigCode             卷烟编码（如："6901028221234"）
     * @param cigName             卷烟名称（如："黄鹤楼软蓝"）
     * @param deliveryMethod      投放方式（如："按档位投放"、"按价位段自选投放"）
     * @param deliveryEtype       扩展投放类型（如："区县公司+市场类型"，可为空）
     * @param targetArea          目标区域（如："全市"、"江汉区"）
     * @param allCigaretteRecords 该卷烟的所有投放记录列表
     * @return 编码表达式字符串，如果参数无效或无法编码则返回空字符串
     * @example
     * <pre>
     *     List&lt;CigaretteDistributionPredictionPO&gt; records = Arrays.asList(record1, record2);
     *     String code = encodeForSpecificArea(
     *         "6901028221234", "黄鹤楼软蓝", "按档位投放", null, "全市", records
     *     );
     *     // 返回: "A（5×10+3×20+...）"
     * </pre>
     */
    @Override
    public String encodeForSpecificArea(String cigCode, String cigName, String deliveryMethod, String deliveryEtype,
                                        String targetArea, List<CigaretteDistributionPredictionPO> allCigaretteRecords) {
        if (targetArea == null || allCigaretteRecords == null || allCigaretteRecords.isEmpty()) {
            return "";
        }

        String methodCode = encodingRuleRepository.findDeliveryMethodCode(deliveryMethod);
        if (methodCode == null) {
            log.warn("无法编码投放方式: {}", deliveryMethod);
            return "";
        }

        String etypeCode = "";
        if (methodCode.startsWith("B")) {
            etypeCode = resolveExtensionTypeCode(deliveryEtype);
            if (etypeCode == null) {
                log.warn("无法编码扩展投放类型: {}", deliveryEtype);
                return "";
            }
        }

        CigaretteDistributionPredictionPO targetRecord = allCigaretteRecords.stream()
                .filter(r -> targetArea.equals(r.getDeliveryArea()))
                .findFirst()
                .orElse(null);
        if (targetRecord == null) {
            log.warn("未找到区域 {} 在卷烟 {} - {} 的记录中", targetArea, cigCode, cigName);
            return "";
        }

        String gradeCodes = ENCODING_RULE.encodeGradeSequences(extractGrades(targetRecord));
        String tagSuffix = buildTagSuffix(targetRecord.getTag());

        StringBuilder sb = new StringBuilder()
                .append(methodCode)
                .append(etypeCode)
                .append(tagSuffix);

        // 当区域为"全市"时，不再单独编码区域部分，直接编码投放量
        if (!"全市".equals(targetArea)) {
            String regionCode = resolveRegionCode(deliveryEtype, targetArea);
            sb.append("（").append(regionCode).append("）");
        }

        sb.append("（").append(gradeCodes).append("）");
        return sb.toString();
    }

    /**
     * 解析区域编码。
     * <p>
     * 根据扩展投放类型查找区域对应的编码，如果找不到则返回原区域名称。
     * </p>
     *
     * @param deliveryEtype 扩展投放类型字符串（如："区县公司"、"市场类型"）
     * @param targetArea    目标区域名称（如："全市"、"丹江"）
     * @return 区域编码（如果找到）或原区域名称（如果未找到）
     */
    String resolveRegionCode(String deliveryEtype, String targetArea) {
        // 解析复合区域名称：形如 "丹江（城网）" → mainRegionName="丹江"，subRegionName="城网"
        String mainRegionName = targetArea;
        String subRegionName = null;
        int leftIdx = targetArea.indexOf('（');
        int rightIdx = targetArea.indexOf('）');
        if (leftIdx > 0 && rightIdx > leftIdx) {
            mainRegionName = targetArea.substring(0, leftIdx);
            subRegionName = targetArea.substring(leftIdx + 1, rightIdx);
        }

        // 识别所有扩展类型（支持双扩展）
        List<DeliveryExtensionType> allTypes = findAllExtensionTypes(deliveryEtype);
        if (allTypes.isEmpty()) {
            // 如果findAllExtensionTypes找不到，尝试使用from方法
            Optional<DeliveryExtensionType> singleTypeOpt = DeliveryExtensionType.from(deliveryEtype);
            if (singleTypeOpt.isPresent()) {
                allTypes = Collections.singletonList(singleTypeOpt.get());
            }
        }

        if (allTypes.isEmpty()) {
            log.warn("无法识别扩展类型: deliveryEtype={}, targetArea={}", deliveryEtype, targetArea);
            return targetArea;
        }

        // 主扩展类型（通常是第一个，区县优先）
        DeliveryExtensionType mainType = allTypes.get(0);
        
        // 主扩展编码（只允许精确匹配）
        String mainCode = null;
        java.util.Map<String, String> mainRegionMap = encodingRuleRepository.getRegionCodeMap(mainType);
        if (mainRegionMap != null && !mainRegionMap.isEmpty()) {
            mainCode = mainRegionMap.get(mainRegionName);
            if (mainCode == null) {
                log.warn("无法找到主区域编码: mainRegionName={}, mainType={}, 可用键: {}", 
                        mainRegionName, mainType, mainRegionMap.keySet());
            }
        }

        // 子扩展编码（仅当存在子扩展名称和子扩展类型时，只允许精确匹配）
        String subCode = null;
        if (subRegionName != null && allTypes.size() > 1) {
            DeliveryExtensionType subType = allTypes.get(1);
            java.util.Map<String, String> subRegionMap = encodingRuleRepository.getRegionCodeMap(subType);
            if (subRegionMap != null && !subRegionMap.isEmpty()) {
                subCode = subRegionMap.get(subRegionName);
                if (subCode == null) {
                    log.warn("无法找到子区域编码: subRegionName={}, subType={}, 可用键: {}", 
                            subRegionName, subType, subRegionMap.keySet());
                }
            }
        }

        if (mainCode != null && subCode != null) {
            return mainCode + " +" + subCode;
        }
        if (mainCode != null) {
            return mainCode;
        }
        
        log.warn("无法解析区域编码: deliveryEtype={}, targetArea={}, mainRegionName={}, subRegionName={}, mainType={}, mainCode={}, subCode={}", 
                deliveryEtype, targetArea, mainRegionName, subRegionName, mainType, mainCode, subCode);
        return targetArea;
    }

    /**
     * 根据标签生成编码后缀。
     * <p>
     * 目前仅实现文档中的示例规则：标签"优质数据共享客户"记为 {@code +a}。
     * </p>
     *
     * @param tag 预测记录中的标签字段
     * @return 标签编码后缀（如 "+a"），无标签或未识别标签时返回空字符串
     */
    String buildTagSuffix(String tag) {
        if (tag == null || tag.trim().isEmpty()) {
            return "";
        }
        String trimmed = tag.trim();
        if (trimmed.contains("优质数据共享客户")) {
            return "+a";
        }
        return "";
    }

    /**
     * 解析扩展投放类型编码，支持单扩展与双扩展。
     *
     * @param deliveryEtype 扩展投放类型原始字符串（如 "档位+区县"、"档位+区县+市场类型"）
     * @return 扩展投放类型编码（如 "1" 或 "1-2"），无法解析时返回 null
     */
    String resolveExtensionTypeCode(String deliveryEtype) {
        if (deliveryEtype == null || deliveryEtype.trim().isEmpty()) {
            return null;
        }

        // 首先尝试精确匹配（适用于单扩展或已定义的双扩展组合）
        Optional<DeliveryExtensionType> mainTypeOpt = DeliveryExtensionType.from(deliveryEtype);
        DeliveryExtensionType mainType;
        
        if (mainTypeOpt.isPresent()) {
            mainType = mainTypeOpt.get();
        } else {
            // 如果精确匹配失败，尝试从字符串中提取所有扩展类型
            // 适用于未在别名列表中定义的双扩展组合（如 "档位+区县+城乡分类代码"）
            List<DeliveryExtensionType> foundTypes = findAllExtensionTypes(deliveryEtype);
            if (foundTypes.isEmpty()) {
            return null;
            }
            // 选择第一个作为主扩展类型
            mainType = foundTypes.get(0);
        }

        // 主扩展编码
        String mainLabel = "档位+" + mainType.getDisplayName();
        String mainCode = encodingRuleRepository.findExtensionTypeCode(mainLabel);
        if (mainCode == null) {
            return null;
        }

        // 尝试解析子扩展
        Optional<DeliveryExtensionType> subTypeOpt = findSubExtensionType(deliveryEtype, mainType);
        if (!subTypeOpt.isPresent()) {
            return mainCode;
        }
        DeliveryExtensionType subType = subTypeOpt.get();
        String subLabel = "档位+" + subType.getDisplayName();
        String subCode = encodingRuleRepository.findExtensionTypeCode(subLabel);
        if (subCode == null) {
            return mainCode;
        }
        return mainCode + "-" + subCode;
    }

    /**
     * 从扩展投放类型字符串中提取所有匹配的扩展类型。
     * 用于处理未在别名列表中定义的双扩展组合。
     *
     * @param rawEtype 原始扩展类型字符串
     * @return 找到的所有扩展类型列表（按优先级排序，区县优先）
     */
    private List<DeliveryExtensionType> findAllExtensionTypes(String rawEtype) {
        if (rawEtype == null) {
            return Collections.emptyList();
        }
        String normalized = rawEtype.replaceAll("\\s+", "");
        List<DeliveryExtensionType> foundTypes = new ArrayList<>();
        
        // 定义扩展类型的优先级：区县 > 市场类型 > 其他类型
        // 区县应该总是作为主扩展类型，因为它是地理区域的基础
        java.util.Map<DeliveryExtensionType, Integer> priorityMap = new java.util.HashMap<>();
        priorityMap.put(DeliveryExtensionType.COUNTY, 1);  // 最高优先级
        priorityMap.put(DeliveryExtensionType.MARKET_TYPE, 2);
        priorityMap.put(DeliveryExtensionType.URBAN_RURAL_CODE, 3);
        priorityMap.put(DeliveryExtensionType.BUSINESS_FORMAT, 4);
        priorityMap.put(DeliveryExtensionType.MARKET_DEPARTMENT, 5);
        priorityMap.put(DeliveryExtensionType.BUSINESS_DISTRICT, 6);
        priorityMap.put(DeliveryExtensionType.CREDIT_LEVEL, 7);
        priorityMap.put(DeliveryExtensionType.INTEGRITY_GROUP, 8);
        
        // 按显示名称长度从长到短排序，优先匹配更长的名称（避免误匹配）
        List<DeliveryExtensionType> sortedTypes = Arrays.stream(DeliveryExtensionType.values())
                .filter(type -> type != DeliveryExtensionType.UNKNOWN)
                .sorted((a, b) -> Integer.compare(b.getDisplayName().length(), a.getDisplayName().length()))
                .collect(java.util.stream.Collectors.toList());
        
        for (DeliveryExtensionType type : sortedTypes) {
            if (normalized.contains(type.getDisplayName())) {
                foundTypes.add(type);
            }
        }
        
        // 按优先级排序，区县优先
        foundTypes.sort((a, b) -> {
            int priorityA = priorityMap.getOrDefault(a, 999);
            int priorityB = priorityMap.getOrDefault(b, 999);
            return Integer.compare(priorityA, priorityB);
        });
        
        return foundTypes;
    }

    /**
     * 从扩展投放类型原始字符串中解析子扩展类型（双扩展场景）。
     *
     * @param rawEtype 原始扩展类型字符串
     * @param mainType 已识别的主扩展类型
     * @return 子扩展类型（若不存在或无法解析则返回 empty）
     */
    private Optional<DeliveryExtensionType> findSubExtensionType(String rawEtype, DeliveryExtensionType mainType) {
        if (rawEtype == null) {
            return Optional.empty();
        }
        String normalized = rawEtype.replaceAll("\\s+", "");
        for (DeliveryExtensionType type : DeliveryExtensionType.values()) {
            if (type == DeliveryExtensionType.UNKNOWN || type == mainType) {
                continue;
            }
            if (normalized.contains(type.getDisplayName())) {
                return Optional.of(type);
            }
        }
        return Optional.empty();
    }

    /**
     * 从投放记录中提取30个档位的客户数数组（D30-D1）。
     */
    BigDecimal[] extractGrades(CigaretteDistributionPredictionPO record) {
        return org.example.application.converter.DistributionDataConverter.extractGradesFromPO(record);
    }

}

