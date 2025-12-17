package org.example.application.service.encode;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.domain.service.rule.EncodingRule;
import org.example.domain.service.rule.impl.EncodingRuleImpl;
import org.example.domain.repository.CigaretteDistributionPredictionRepository;
import org.example.infrastructure.config.encoding.EncodingRuleRepository;
import org.example.infrastructure.persistence.po.CigaretteDistributionPredictionPO;
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
            if (rec.cityWide) {
                cityWide.add(rec);
            } else {
                normal.add(rec);
            }
        }

        List<String> result = new ArrayList<>();

        // 1) 全市：不参与多区域聚合，直接输出（按稳定排序）
        cityWide.stream()
                .sorted(Comparator.comparing((Rec r) -> r.header).thenComparing(r -> r.gradeKey))
                .forEach(r -> result.add(r.header + "（" + r.gradeKey + "）"));

        // 2) 两层聚合：先主扩展聚合，再子扩展聚合
        // 第一层：key = header + gradeKey + subCode
        Map<Key1, LinkedHashSet<String>> mainCodesByKey = new LinkedHashMap<>();
        for (Rec r : normal) {
            Key1 k = new Key1(r.header, r.gradeKey, normalizeNullable(r.subCode));
            mainCodesByKey.computeIfAbsent(k, ignore -> new LinkedHashSet<>()).add(normalizeNullable(r.mainCode));
        }

        List<Stage1Row> stage1 = new ArrayList<>();
        mainCodesByKey.forEach((k, mainCodes) -> {
            String mainAgg = mergeCodes(mainCodes);
            stage1.add(new Stage1Row(k.header, k.gradeKey, k.subCode, mainAgg));
        });

        // 第二层：key = header + gradeKey + mainAgg
        Map<Key2, LinkedHashSet<String>> subCodesByKey = new LinkedHashMap<>();
        for (Stage1Row row : stage1) {
            Key2 k = new Key2(row.header, row.gradeKey, row.mainAgg);
            subCodesByKey.computeIfAbsent(k, ignore -> new LinkedHashSet<>()).add(normalizeNullable(row.subCode));
        }

        List<String> aggregated = new ArrayList<>();
        subCodesByKey.forEach((k, subCodes) -> {
            String subAgg = mergeCodes(subCodes);
            String regionAgg = k.mainAgg;
            if (StringUtils.hasText(subAgg)) {
                regionAgg = regionAgg + " +" + subAgg;
            }
            aggregated.add(k.header + "（" + regionAgg + "）" + "（" + k.gradeKey + "）");
        });

        // 稳定排序，便于前端展示与人工对照
        aggregated.sort(Comparator.naturalOrder());
        result.addAll(aggregated);
        return result;
    }

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
        return new Rec(header, gradeKey, parts.main, parts.sub, false);
    }

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

        // 优先按“前缀+数字”的紧凑写法合并：Q2+4+5
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

    private String normalizeNullable(String s) {
        return s == null ? "" : s.trim();
    }

    private CigaretteDistributionPredictionPO mapRowToEntity(Map<String, Object> row) {
        if (row == null) {
            return null;
        }
        CigaretteDistributionPredictionPO data = new CigaretteDistributionPredictionPO();
        data.setId(extractInteger(row.get("id")));
        data.setCigCode((String) row.get("CIG_CODE"));
        data.setCigName((String) row.get("CIG_NAME"));
        data.setYear(extractInteger(row.get("YEAR")));
        data.setMonth(extractInteger(row.get("MONTH")));
        data.setWeekSeq(extractInteger(row.get("WEEK_SEQ")));
        data.setDeliveryArea((String) row.get("DELIVERY_AREA"));
        data.setDeliveryMethod((String) row.get("DELIVERY_METHOD"));
        data.setDeliveryEtype((String) row.get("DELIVERY_ETYPE"));
        data.setD30(getBigDecimal(row.get("D30")));
        data.setD29(getBigDecimal(row.get("D29")));
        data.setD28(getBigDecimal(row.get("D28")));
        data.setD27(getBigDecimal(row.get("D27")));
        data.setD26(getBigDecimal(row.get("D26")));
        data.setD25(getBigDecimal(row.get("D25")));
        data.setD24(getBigDecimal(row.get("D24")));
        data.setD23(getBigDecimal(row.get("D23")));
        data.setD22(getBigDecimal(row.get("D22")));
        data.setD21(getBigDecimal(row.get("D21")));
        data.setD20(getBigDecimal(row.get("D20")));
        data.setD19(getBigDecimal(row.get("D19")));
        data.setD18(getBigDecimal(row.get("D18")));
        data.setD17(getBigDecimal(row.get("D17")));
        data.setD16(getBigDecimal(row.get("D16")));
        data.setD15(getBigDecimal(row.get("D15")));
        data.setD14(getBigDecimal(row.get("D14")));
        data.setD13(getBigDecimal(row.get("D13")));
        data.setD12(getBigDecimal(row.get("D12")));
        data.setD11(getBigDecimal(row.get("D11")));
        data.setD10(getBigDecimal(row.get("D10")));
        data.setD9(getBigDecimal(row.get("D9")));
        data.setD8(getBigDecimal(row.get("D8")));
        data.setD7(getBigDecimal(row.get("D7")));
        data.setD6(getBigDecimal(row.get("D6")));
        data.setD5(getBigDecimal(row.get("D5")));
        data.setD4(getBigDecimal(row.get("D4")));
        data.setD3(getBigDecimal(row.get("D3")));
        data.setD2(getBigDecimal(row.get("D2")));
        data.setD1(getBigDecimal(row.get("D1")));
        data.setBz((String) row.get("BZ"));
        data.setActualDelivery(getBigDecimal(row.get("ACTUAL_DELIVERY")));
        data.setDeployinfoCode((String) row.get("DEPLOYINFO_CODE"));
        data.setTag((String) row.get("TAG"));
        data.setTagFilterConfig(row.get("TAG_FILTER_CONFIG") != null ? row.get("TAG_FILTER_CONFIG").toString() : null);
        return data;
    }

    private Integer extractInteger(Object obj) {
        if (obj == null) return null;
        if (obj instanceof Integer) return (Integer) obj;
        if (obj instanceof Number) return ((Number) obj).intValue();
        try {
            return Integer.parseInt(obj.toString());
        } catch (Exception e) {
            return null;
        }
    }

    private BigDecimal getBigDecimal(Object obj) {
        if (obj == null) return null;
        if (obj instanceof BigDecimal) return (BigDecimal) obj;
        if (obj instanceof Number) return new BigDecimal(obj.toString());
        try {
            return new BigDecimal(obj.toString());
        } catch (Exception e) {
            return null;
        }
    }

    private static final class Rec {
        private final String header;
        private final String gradeKey;
        private final String mainCode;
        private final String subCode;
        private final boolean cityWide;

        private Rec(String header, String gradeKey, String mainCode, String subCode, boolean cityWide) {
            this.header = header;
            this.gradeKey = gradeKey;
            this.mainCode = mainCode;
            this.subCode = subCode;
            this.cityWide = cityWide;
        }
    }

    private static final class RegionParts {
        private final String main;
        private final String sub;

        private RegionParts(String main, String sub) {
            this.main = main;
            this.sub = sub;
        }
    }

    private static final class Key1 {
        private final String header;
        private final String gradeKey;
        private final String subCode;

        private Key1(String header, String gradeKey, String subCode) {
            this.header = header;
            this.gradeKey = gradeKey;
            this.subCode = subCode;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof Key1)) return false;
            Key1 key1 = (Key1) o;
            return Objects.equals(header, key1.header)
                    && Objects.equals(gradeKey, key1.gradeKey)
                    && Objects.equals(subCode, key1.subCode);
        }

        @Override
        public int hashCode() {
            return Objects.hash(header, gradeKey, subCode);
        }
    }

    private static final class Stage1Row {
        private final String header;
        private final String gradeKey;
        private final String subCode;
        private final String mainAgg;

        private Stage1Row(String header, String gradeKey, String subCode, String mainAgg) {
            this.header = header;
            this.gradeKey = gradeKey;
            this.subCode = subCode;
            this.mainAgg = mainAgg;
        }
    }

    private static final class Key2 {
        private final String header;
        private final String gradeKey;
        private final String mainAgg;

        private Key2(String header, String gradeKey, String mainAgg) {
            this.header = header;
            this.gradeKey = gradeKey;
            this.mainAgg = mainAgg;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof Key2)) return false;
            Key2 key2 = (Key2) o;
            return Objects.equals(header, key2.header)
                    && Objects.equals(gradeKey, key2.gradeKey)
                    && Objects.equals(mainAgg, key2.mainAgg);
        }

        @Override
        public int hashCode() {
            return Objects.hash(header, gradeKey, mainAgg);
        }
    }
}


