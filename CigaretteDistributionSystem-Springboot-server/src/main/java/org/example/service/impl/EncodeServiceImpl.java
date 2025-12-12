package org.example.service.impl;

import lombok.extern.slf4j.Slf4j;
import org.example.config.encoding.EncodingRuleRepository;
import org.example.entity.CigaretteDistributionPredictionData;
import org.example.service.EncodeService;
import org.example.service.delivery.DeliveryExtensionType;
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

    private final EncodingRuleRepository encodingRuleRepository;

    public EncodeServiceImpl(EncodingRuleRepository encodingRuleRepository) {
        this.encodingRuleRepository = encodingRuleRepository;
    }

    /**
     * 为特定区域生成编码表达式（单卷烟单区域）。
     * <p>
     * 根据投放方式、扩展类型和目标区域，生成语义编码表达式。
     * 编码格式：{方法码}{扩展类型码}（{区域码}）（{档位序列码}）
     * </p>
     *
     * @param cigCode             卷烟编码（如："6901028221234"）
     * @param cigName             卷烟名称（如："黄鹤楼软蓝"）
     * @param deliveryMethod      投放方式（如："按档位投放"、"按价位段自选投放"）
     * @param deliveryEtype       扩展投放类型（如："区县公司+市场类型"，可为空）
     * @param targetArea          目标区域（如："全市"、"江汉区"）
     * @param allCigaretteRecords 该卷烟的所有投放记录列表
     * @return 编码表达式字符串，格式如："A（全市）（5×10+3×20+...）"，如果参数无效或无法编码则返回空字符串
     * @example
     * <pre>
     *     List<CigaretteDistributionPredictionData> records = Arrays.asList(record1, record2);
     *     String code = encodeForSpecificArea(
     *         "6901028221234", "黄鹤楼软蓝", "按档位投放", null, "全市", records
     *     );
     *     // 返回: "A（全市）（5×10+3×20+...）"
     * </pre>
     */
    @Override
    public String encodeForSpecificArea(String cigCode, String cigName, String deliveryMethod, String deliveryEtype,
                                        String targetArea, List<CigaretteDistributionPredictionData> allCigaretteRecords) {
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
            etypeCode = encodingRuleRepository.findExtensionTypeCode(deliveryEtype);
            if (etypeCode == null) {
                log.warn("无法编码扩展投放类型: {}", deliveryEtype);
                return "";
            }
        }

        CigaretteDistributionPredictionData targetRecord = allCigaretteRecords.stream()
                .filter(r -> targetArea.equals(r.getDeliveryArea()))
                .findFirst()
                .orElse(null);
        if (targetRecord == null) {
            log.warn("未找到区域 {} 在卷烟 {} - {} 的记录中", targetArea, cigCode, cigName);
            return "";
        }

        String regionCode = resolveRegionCode(deliveryEtype, targetArea);
        String gradeCodes = encodeGradeSequences(extractGrades(targetRecord));

        return new StringBuilder()
                .append(methodCode)
                .append(etypeCode)
                .append("（").append(regionCode).append("）")
                .append("（").append(gradeCodes).append("）")
                .toString();
    }

    /**
     * 解析区域编码。
     * <p>
     * 根据扩展投放类型查找区域对应的编码，如果找不到则返回原区域名称。
     * </p>
     *
     * @param deliveryEtype 扩展投放类型字符串（如："区县公司"、"市场类型"）
     * @param targetArea    目标区域名称（如："全市"、"江汉区"）
     * @return 区域编码（如果找到）或原区域名称（如果未找到）
     * @example
     * <pre>
     *     String code = resolveRegionCode("区县公司", "江汉区");
     *     // 如果编码规则中存在映射，返回编码（如："JH"）
     *     // 否则返回原区域名称（"江汉区"）
     * </pre>
     */
    private String resolveRegionCode(String deliveryEtype, String targetArea) {
        Optional<DeliveryExtensionType> typeOpt = DeliveryExtensionType.from(deliveryEtype);
        if (typeOpt.isPresent()) {
            String code = encodingRuleRepository
                    .getRegionCodeMap(typeOpt.get())
                    .get(targetArea);
            if (code != null) {
                return code;
            }
        }
        return targetArea;
    }

    /**
     * 编码档位序列。
     * <p>
     * 将30个档位的客户数数组编码为压缩格式，连续相同值的档位用"数量×值"表示，不同值之间用"+"连接。
     * </p>
     *
     * @param grades 30个档位的客户数数组（索引0对应D30，索引29对应D1）
     * @return 编码后的档位序列字符串，格式如："5×10+3×20+2×15"
     * @example
     * <pre>
     *     BigDecimal[] grades = new BigDecimal[30];
     *     Arrays.fill(grades, 0, 5, BigDecimal.valueOf(10));  // D30-D26: 10
     *     Arrays.fill(grades, 5, 8, BigDecimal.valueOf(20)); // D25-D23: 20
     *     Arrays.fill(grades, 8, 10, BigDecimal.valueOf(15)); // D22-D21: 15
     *     String encoded = encodeGradeSequences(grades);
     *     // 返回: "5×10+3×20+2×15+20×0"（剩余20个档位为0）
     * </pre>
     */
    private String encodeGradeSequences(BigDecimal[] grades) {
        StringBuilder sb = new StringBuilder();
        int start = 0;
        while (start < grades.length) {
            BigDecimal current = grades[start] == null ? BigDecimal.ZERO : grades[start];
            int end = start;
            while (end < grades.length) {
                BigDecimal val = grades[end] == null ? BigDecimal.ZERO : grades[end];
                if (val.compareTo(current) == 0) {
                    end++;
                } else {
                    break;
                }
            }
            sb.append(end - start).append("×").append(formatAsInteger(current));
            if (end < grades.length) {
                sb.append("+");
            }
            start = end;
        }
        return sb.toString();
    }

    /**
     * 从投放记录中提取30个档位的客户数数组。
     * <p>
     * 将记录中的D30到D1档位客户数提取为数组，数组索引0对应D30，索引29对应D1。
     * </p>
     *
     * @param record 卷烟投放预测记录
     * @return 长度为30的BigDecimal数组，索引0对应D30，索引29对应D1
     * @example
     * <pre>
     *     CigaretteDistributionPredictionData record = new CigaretteDistributionPredictionData();
     *     record.setD30(BigDecimal.valueOf(10));
     *     record.setD29(BigDecimal.valueOf(20));
     *     // ... 设置其他档位
     *     BigDecimal[] grades = extractGrades(record);
     *     // grades[0] = 10 (D30)
     *     // grades[1] = 20 (D29)
     *     // ...
     *     // grades[29] = D1的值
     * </pre>
     */
    private BigDecimal[] extractGrades(CigaretteDistributionPredictionData record) {
        return new BigDecimal[]{
                record.getD30(), record.getD29(), record.getD28(), record.getD27(), record.getD26(),
                record.getD25(), record.getD24(), record.getD23(), record.getD22(), record.getD21(),
                record.getD20(), record.getD19(), record.getD18(), record.getD17(), record.getD16(),
                record.getD15(), record.getD14(), record.getD13(), record.getD12(), record.getD11(),
                record.getD10(), record.getD9(), record.getD8(), record.getD7(), record.getD6(),
                record.getD5(), record.getD4(), record.getD3(), record.getD2(), record.getD1()
        };
    }

    /**
     * 将BigDecimal格式化为整数字符串。
     * <p>
     * 去除小数点和尾随零，返回纯数字字符串。如果值为null则返回"0"。
     * </p>
     *
     * @param value BigDecimal值（可为null）
     * @return 格式化后的字符串（如："10"、"0"），如果值为null则返回"0"
     * @example
     * <pre>
     *     String str1 = formatAsInteger(BigDecimal.valueOf(10));
     *     // 返回: "10"
     *     String str2 = formatAsInteger(BigDecimal.valueOf(10.0));
     *     // 返回: "10"（去除尾随零）
     *     String str3 = formatAsInteger(BigDecimal.valueOf(10.5));
     *     // 返回: "10.5"
     *     String str4 = formatAsInteger(null);
     *     // 返回: "0"
     * </pre>
     */
    private String formatAsInteger(BigDecimal value) {
        if (value == null) {
            return "0";
        }
        return value.stripTrailingZeros().toPlainString();
    }
}

