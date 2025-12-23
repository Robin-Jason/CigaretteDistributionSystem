package org.example.application.service.encode.impl;

import lombok.RequiredArgsConstructor;
import lombok.Value;
import lombok.extern.slf4j.Slf4j;
import org.example.application.service.encode.AggregatedEncodingQueryService;
import org.example.domain.service.rule.EncodingRule;
import org.example.domain.service.rule.impl.EncodingRuleImpl;
import org.example.domain.repository.CigaretteDistributionPredictionRepository;
import org.example.infrastructure.config.encoding.EncodingRuleRepository;
import org.example.infrastructure.persistence.po.CigaretteDistributionPredictionPO;
import org.example.shared.util.GradeExtractor;
import org.example.shared.util.GradeSetter;
import org.example.shared.util.WriteBackHelper;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * 单卷烟多区域投放聚合编码查询服务实现（面向前端懒加载）。
 *
 * <p>聚合规则来源：{@code docs/编码规则表.md} 第二部分。</p>
 *
 * @author Robin
 * @since 2025-12-17
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AggregatedEncodingQueryServiceImpl implements AggregatedEncodingQueryService {

    private static final EncodingRule ENCODING_RULE = new EncodingRuleImpl();
    private static final Pattern CODE_PREFIX_PATTERN = Pattern.compile("^([A-Za-z]+)(\\d+)$");

    private final CigaretteDistributionPredictionRepository predictionRepository;
    private final EncodingRuleRepository encodingRuleRepository;
    private final EncodeServiceImpl encodeSupport;

    /**
     * 查询指定卷烟的聚合编码表达式列表。
     * <p>
     * 将同一卷烟在多个区域的投放记录聚合为紧凑的编码表达式，便于前端展示。
     * </p>
     *
     * @param year    年份
     * @param month   月份
     * @param weekSeq 周序号
     * @param cigCode 卷烟代码
     * @return 聚合编码表达式列表，格式如 "B1（Q2+3+4）（1×3+2×2+27×0）"
     *
     * @example
     * <pre>{@code
     * List<String> encodings = service.listAggregatedEncodings(2025, 9, 3, "42010020");
     * // 返回：
     * // ["B1-2（Q2+3+4+5+6+7 +M2）（1×7+28×5+1×4）",
     * //  "B1-2（Q2+3+5+7 +M1）（1×6+28×5+1×4）",
     * //  "B1-2（Q4+6 +M1）（1×7+28×5+1×4）"]
     * }</pre>
     */
    @Override
    public List<String> listAggregatedEncodings(Integer year, Integer month, Integer weekSeq, String cigCode) {
        if (year == null || month == null || weekSeq == null || !StringUtils.hasText(cigCode)) {
            return Collections.emptyList();
        }

        List<Map<String, Object>> rows = predictionRepository.findByCigCode(year, month, weekSeq, cigCode.trim());
        if (rows == null || rows.isEmpty()) {
            return Collections.emptyList();
        }

        List<CigaretteDistributionPredictionPO> records = rows.stream()
                .map(this::mapRowToEntity)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());

        return aggregate(records);
    }

    /**
     * 对分配记录列表执行两层聚合，生成聚合编码表达式。
     * <p>
     * 聚合流程：
     * <ol>
     *   <li>分离全市记录和非全市记录</li>
     *   <li>全市记录直接输出，不参与聚合</li>
     *   <li>第一层聚合：按 header + gradeKey + subCode 分组，合并 mainCode</li>
     *   <li>第二层聚合：按 header + gradeKey + mainAgg 分组，合并 subCode</li>
     *   <li>生成最终编码表达式</li>
     * </ol>
     * </p>
     *
     * @param records 分配记录列表
     * @return 聚合编码表达式列表
     *
     * @example
     * <pre>{@code
     * // 输入：12条区域记录（丹江城网、郧阳城网、...、丹江农网）
     * // 输出：3条聚合编码
     * // "B1-2（Q2+3+4+5+6+7 +M2）（1×7+28×5+1×4）"
     * // "B1-2（Q2+3+5+7 +M1）（1×6+28×5+1×4）"
     * // "B1-2（Q4+6 +M1）（1×7+28×5+1×4）"
     * }</pre>
     */
    private List<String> aggregate(List<CigaretteDistributionPredictionPO> records) {
        if (records == null || records.isEmpty()) {
            return Collections.emptyList();
        }

        List<Rec> cityWide = new ArrayList<>();
        List<Rec> normal = new ArrayList<>();

        for (CigaretteDistributionPredictionPO r : records) {
            Rec rec = buildRec(r);
            if (rec == null) {
                continue;
            }
            if (rec.isCityWide()) {
                cityWide.add(rec);
            } else {
                normal.add(rec);
            }
        }

        List<String> result = new ArrayList<>();

        // 1) 全市：不参与多区域聚合，直接输出（按稳定排序）
        cityWide.stream()
                .sorted(Comparator.comparing(Rec::getHeader).thenComparing(Rec::getGradeKey))
                .forEach(r -> result.add(r.getHeader() + "（" + r.getGradeKey() + "）"));

        // 2) 两层聚合：先主扩展聚合，再子扩展聚合
        // 第一层：key = header + gradeKey + subCode
        Map<Key1, LinkedHashSet<String>> mainCodesByKey = new LinkedHashMap<>();
        for (Rec r : normal) {
            Key1 k = new Key1(r.getHeader(), r.getGradeKey(), normalizeNullable(r.getSubCode()));
            mainCodesByKey.computeIfAbsent(k, ignore -> new LinkedHashSet<>()).add(normalizeNullable(r.getMainCode()));
        }

        List<Stage1Row> stage1 = new ArrayList<>();
        mainCodesByKey.forEach((k, mainCodes) -> {
            String mainAgg = mergeCodes(mainCodes);
            stage1.add(new Stage1Row(k.getHeader(), k.getGradeKey(), k.getSubCode(), mainAgg));
        });

        // 第二层：key = header + gradeKey + mainAgg
        Map<Key2, LinkedHashSet<String>> subCodesByKey = new LinkedHashMap<>();
        for (Stage1Row row : stage1) {
            Key2 k = new Key2(row.getHeader(), row.getGradeKey(), row.getMainAgg());
            subCodesByKey.computeIfAbsent(k, ignore -> new LinkedHashSet<>()).add(normalizeNullable(row.getSubCode()));
        }

        List<String> aggregated = new ArrayList<>();
        subCodesByKey.forEach((k, subCodes) -> {
            String subAgg = mergeCodes(subCodes);
            String regionAgg = k.getMainAgg();
            if (StringUtils.hasText(subAgg)) {
                regionAgg = regionAgg + " +" + subAgg;
            }
            aggregated.add(k.getHeader() + "（" + regionAgg + "）" + "（" + k.getGradeKey() + "）");
        });

        // 稳定排序，便于前端展示与人工对照
        aggregated.sort(Comparator.naturalOrder());
        result.addAll(aggregated);
        return result;
    }

    /**
     * 将单条分配记录转换为编码信息对象。
     * <p>
     * 编码组成：投放方式编码 + 扩展类型编码 + 标签后缀 + 区域编码 + 档位序列
     * </p>
     *
     * @param record 分配记录
     * @return 编码信息对象；如果无法编码则返回 null
     *
     * @example
     * <pre>{@code
     * // 输入：deliveryMethod="按档位扩展投放", deliveryEtype="档位+区县+市场类型",
     * //       deliveryArea="丹江（城网）", D30=1, D29=1, ...
     * // 输出：Rec(header="B1-2", gradeKey="1×7+28×5+1×4", mainCode="Q2", subCode="M2", cityWide=false)
     * }</pre>
     */
    private Rec buildRec(CigaretteDistributionPredictionPO record) {
        if (record == null) {
            return null;
        }

        String methodCode = encodingRuleRepository.findDeliveryMethodCode(record.getDeliveryMethod());
        if (!StringUtils.hasText(methodCode)) {
            log.warn("无法编码投放方式: deliveryMethod={}, cigCode={}, area={}",
                    record.getDeliveryMethod(), record.getCigCode(), record.getDeliveryArea());
            return null;
        }

        String etypeCode = "";
        if (methodCode.startsWith("B")) {
            etypeCode = encodeSupport.resolveExtensionTypeCode(record.getDeliveryEtype());
            if (!StringUtils.hasText(etypeCode)) {
                log.warn("无法编码扩展投放类型: deliveryEtype={}, cigCode={}, area={}",
                        record.getDeliveryEtype(), record.getCigCode(), record.getDeliveryArea());
                return null;
            }
        }

        String tagSuffix = encodeSupport.buildTagSuffix(record.getTag());
        String header = methodCode + etypeCode + tagSuffix;

        String gradeKey = ENCODING_RULE.encodeGradeSequences(encodeSupport.extractGrades(record));

        boolean cityWide = "全市".equals(record.getDeliveryArea());
        if (cityWide) {
            return new Rec(header, gradeKey, null, null, true);
        }

        String regionCode = encodeSupport.resolveRegionCode(record.getDeliveryEtype(), record.getDeliveryArea());
        RegionParts parts = parseRegionParts(regionCode);
        return new Rec(header, gradeKey, parts.getMain(), parts.getSub(), false);
    }

    /**
     * 解析区域编码，拆分为主编码和子编码。
     * <p>
     * 区域编码格式：主编码 + 子编码，如 "Q2 +M1" 拆分为 main="Q2", sub="M1"
     * </p>
     *
     * @param regionCode 区域编码字符串
     * @return 拆分后的主编码和子编码
     *
     * @example
     * <pre>{@code
     * parseRegionParts("Q2 +M1")  // -> RegionParts(main="Q2", sub="M1")
     * parseRegionParts("Q2")      // -> RegionParts(main="Q2", sub="")
     * parseRegionParts("")        // -> RegionParts(main="", sub="")
     * }</pre>
     */
    private RegionParts parseRegionParts(String regionCode) {
        if (!StringUtils.hasText(regionCode)) {
            return new RegionParts("", "");
        }
        String compact = regionCode.replaceAll("\\s+", "");
        String[] parts = compact.split("\\+");
        String main = parts.length >= 1 ? parts[0] : "";
        String sub = parts.length >= 2 ? parts[1] : "";
        return new RegionParts(main, sub);
    }

    /**
     * 合并多个区域编码为紧凑表达式。
     * <p>
     * 如果编码具有相同前缀，则合并为紧凑写法：Q2, Q3, Q4 → Q2+3+4
     * </p>
     *
     * @param codes 区域编码集合
     * @return 合并后的紧凑表达式
     *
     * @example
     * <pre>{@code
     * mergeCodes(["Q2", "Q3", "Q4", "Q5"])  // -> "Q2+3+4+5"
     * mergeCodes(["M1", "M2"])              // -> "M1+2"
     * mergeCodes(["Q2", "M1"])              // -> "Q2+M1"（前缀不同，无法紧凑合并）
     * }</pre>
     */
    private String mergeCodes(Collection<String> codes) {
        if (codes == null) {
            return "";
        }
        List<String> list = codes.stream()
                .filter(StringUtils::hasText)
                .distinct()
                .sorted(this::compareCodes)
                .collect(Collectors.toList());
        if (list.isEmpty()) {
            return "";
        }

        // 优先按"前缀+数字"的紧凑写法合并：Q2+4+5
        Matcher m0 = CODE_PREFIX_PATTERN.matcher(list.get(0));
        if (!m0.matches()) {
            return String.join("+", list);
        }
        String prefix = m0.group(1);
        String firstNum = m0.group(2);

        StringBuilder sb = new StringBuilder(prefix).append(firstNum);
        for (int i = 1; i < list.size(); i++) {
            Matcher mi = CODE_PREFIX_PATTERN.matcher(list.get(i));
            if (mi.matches() && prefix.equals(mi.group(1))) {
                sb.append("+").append(mi.group(2));
            } else {
                sb.append("+").append(list.get(i));
            }
        }
        return sb.toString();
    }

    /**
     * 比较两个区域编码，用于排序。
     * <p>
     * 排序规则：先按前缀字母排序，再按数字排序。
     * </p>
     *
     * @param a 编码 A
     * @param b 编码 B
     * @return 比较结果（负数表示 a < b，正数表示 a > b，0 表示相等）
     *
     * @example
     * <pre>{@code
     * compareCodes("Q2", "Q10")  // -> 负数（2 < 10）
     * compareCodes("M1", "Q1")   // -> 正数（M > Q 按字母序）
     * compareCodes("Q2", "Q2")   // -> 0
     * }</pre>
     */
    private int compareCodes(String a, String b) {
        if (a == null && b == null) return 0;
        if (a == null) return -1;
        if (b == null) return 1;

        Matcher ma = CODE_PREFIX_PATTERN.matcher(a);
        Matcher mb = CODE_PREFIX_PATTERN.matcher(b);
        if (ma.matches() && mb.matches()) {
            int p = ma.group(1).compareTo(mb.group(1));
            if (p != 0) return p;
            try {
                return Integer.compare(Integer.parseInt(ma.group(2)), Integer.parseInt(mb.group(2)));
            } catch (NumberFormatException ignore) {
                return ma.group(2).compareTo(mb.group(2));
            }
        }
        return a.compareTo(b);
    }

    /**
     * 将 null 或空白字符串规范化为空字符串。
     *
     * @param s 输入字符串
     * @return 规范化后的字符串（null → ""，其他去除首尾空白）
     */
    private String normalizeNullable(String s) {
        return s == null ? "" : s.trim();
    }

    /**
     * 将数据库查询结果行转换为 PO 对象。
     *
     * @param row 数据库查询结果行（Map 形式）
     * @return PO 对象；如果输入为 null 则返回 null
     */
    private CigaretteDistributionPredictionPO mapRowToEntity(Map<String, Object> row) {
        if (row == null) {
            return null;
        }
        CigaretteDistributionPredictionPO data = new CigaretteDistributionPredictionPO();
        data.setId(WriteBackHelper.toInteger(row.get("id")));
        data.setCigCode((String) row.get("CIG_CODE"));
        data.setCigName((String) row.get("CIG_NAME"));
        data.setYear(WriteBackHelper.toInteger(row.get("YEAR")));
        data.setMonth(WriteBackHelper.toInteger(row.get("MONTH")));
        data.setWeekSeq(WriteBackHelper.toInteger(row.get("WEEK_SEQ")));
        data.setDeliveryArea((String) row.get("DELIVERY_AREA"));
        data.setDeliveryMethod((String) row.get("DELIVERY_METHOD"));
        data.setDeliveryEtype((String) row.get("DELIVERY_ETYPE"));

        // D30~D1 批量设置
        BigDecimal[] grades = GradeExtractor.extractFromMap(row);
        GradeSetter.setGrades(data, grades);

        data.setBz((String) row.get("BZ"));
        data.setActualDelivery(WriteBackHelper.toBigDecimal(row.get("ACTUAL_DELIVERY")));
        data.setDeployinfoCode((String) row.get("DEPLOYINFO_CODE"));
        data.setTag((String) row.get("TAG"));
        data.setTagFilterConfig(row.get("TAG_FILTER_CONFIG") != null ? row.get("TAG_FILTER_CONFIG").toString() : null);
        return data;
    }


    /** 单条记录的编码信息 */
    @Value
    private static class Rec {
        String header;
        String gradeKey;
        String mainCode;
        String subCode;
        boolean cityWide;
    }

    /** 区域编码拆分结果 */
    @Value
    private static class RegionParts {
        String main;
        String sub;
    }

    /** 第一层聚合 Key：header + gradeKey + subCode */
    @Value
    private static class Key1 {
        String header;
        String gradeKey;
        String subCode;
    }

    /** 第一层聚合结果行 */
    @Value
    private static class Stage1Row {
        String header;
        String gradeKey;
        String subCode;
        String mainAgg;
    }

    /** 第二层聚合 Key：header + gradeKey + mainAgg */
    @Value
    private static class Key2 {
        String header;
        String gradeKey;
        String mainAgg;
    }
}

