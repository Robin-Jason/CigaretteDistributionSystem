package org.example.application.service.encode;

import lombok.extern.slf4j.Slf4j;
import org.example.domain.model.valueobject.DeliveryExtensionType;
import org.example.domain.service.rule.EncodingRule;
import org.example.infrastructure.config.encoding.EncodingRuleRepository;
import org.example.infrastructure.persistence.po.CigaretteDistributionPredictionPO;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
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
     * - 标签：例如“优质数据共享客户” → 记为 {@code +a}（可扩展更多标签映射）<br/>
     * - 区域编码：如 Q2、M1、Y5 等，由区域维度与区域名称映射得到<br/>
     * - 投放量编码：对该区域 30 个档位投放量按连续相同值聚合（由 {@link EncodingRule#encodeGradeSequences} 实现）<br/>
     * 特殊规则：当投放区域为“全市”时，不再单独编码区域部分，直接写入投放量编码。
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

        // 当区域为“全市”时，不再单独编码区域部分，直接编码投放量
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
        // 解析主/子扩展类型
        Optional<DeliveryExtensionType> mainTypeOpt = DeliveryExtensionType.from(deliveryEtype);

        // 解析复合区域名称：形如 "丹江（城网）" → mainRegionName="丹江"，subRegionName="城网"
        String mainRegionName = targetArea;
        String subRegionName = null;
        int leftIdx = targetArea.indexOf('（');
        int rightIdx = targetArea.indexOf('）');
        if (leftIdx > 0 && rightIdx > leftIdx) {
            mainRegionName = targetArea.substring(0, leftIdx);
            subRegionName = targetArea.substring(leftIdx + 1, rightIdx);
        }

        // 主扩展编码
        String mainCode = null;
        if (mainTypeOpt.isPresent()) {
            mainCode = encodingRuleRepository
                    .getRegionCodeMap(mainTypeOpt.get())
                    .get(mainRegionName);
        }

        // 子扩展编码（仅当存在子扩展名称和子扩展类型时）
        String subCode = null;
        if (subRegionName != null) {
            Optional<DeliveryExtensionType> subTypeOpt = findSubExtensionType(deliveryEtype, mainTypeOpt.orElse(null));
            if (subTypeOpt.isPresent()) {
                subCode = encodingRuleRepository
                        .getRegionCodeMap(subTypeOpt.get())
                        .get(subRegionName);
            }
        }

        if (mainCode != null && subCode != null) {
            return mainCode + " +" + subCode;
        }
        if (mainCode != null) {
            return mainCode;
        }
        return targetArea;
    }

    /**
     * 根据标签生成编码后缀。
     * <p>
     * 目前仅实现文档中的示例规则：标签“优质数据共享客户”记为 {@code +a}。
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

        Optional<DeliveryExtensionType> mainTypeOpt = DeliveryExtensionType.from(deliveryEtype);
        if (!mainTypeOpt.isPresent()) {
            return null;
        }
        DeliveryExtensionType mainType = mainTypeOpt.get();

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
        return new BigDecimal[]{
                record.getD30(), record.getD29(), record.getD28(), record.getD27(), record.getD26(),
                record.getD25(), record.getD24(), record.getD23(), record.getD22(), record.getD21(),
                record.getD20(), record.getD19(), record.getD18(), record.getD17(), record.getD16(),
                record.getD15(), record.getD14(), record.getD13(), record.getD12(), record.getD11(),
                record.getD10(), record.getD9(), record.getD8(), record.getD7(), record.getD6(),
                record.getD5(), record.getD4(), record.getD3(), record.getD2(), record.getD1()
        };
    }
}


