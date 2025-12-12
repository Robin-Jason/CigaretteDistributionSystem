package org.example.service.util;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;

import java.math.BigDecimal;
import java.util.Map;

/**
 * 分配流程数据转换与校验工具类
 * <p>
 * 职责：对 Map 数据做字段提取、格式清洗与安全转换，提供可复用的静态方法，减轻主流程代码体积。
 * 主要用于处理数据库查询返回的Map结果，提供类型安全的字段提取和转换功能。
 * </p>
 *
 * @author Robin
 * @version 1.0
 * @since 2025-12-11
 */
@Slf4j
public final class DistributionDataConverter {

    private static final String[] GRADE_NAMES = {
            "D30", "D29", "D28", "D27", "D26", "D25", "D24", "D23", "D22", "D21",
            "D20", "D19", "D18", "D17", "D16", "D15", "D14", "D13", "D12", "D11",
            "D10", "D9", "D8", "D7", "D6", "D5", "D4", "D3", "D2", "D1"
    };

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private DistributionDataConverter() {
    }

    /**
     * 清洗并验证卷烟代码。
     * <p>
     * 去除前后空格，验证长度至少为4个字符。
     * </p>
     *
     * @param rawCode 原始卷烟代码
     * @param cigName 卷烟名称（仅用于日志）
     * @return 清洗后的卷烟代码
     * @throws IllegalArgumentException 如果代码为空或长度不足
     * @example
     * <pre>
     *     String code = DistributionDataConverter.sanitizeAndValidateCigaretteCode(" 42020181 ", "黄鹤楼");
     *     // 返回: "42020181"
     *     String code2 = DistributionDataConverter.sanitizeAndValidateCigaretteCode("123", "测试");
     *     // 抛出: IllegalArgumentException("卷烟代码长度不足: 123")
     * </pre>
     */
    public static String sanitizeAndValidateCigaretteCode(String rawCode, String cigName) {
        if (rawCode == null || rawCode.trim().isEmpty()) {
            throw new IllegalArgumentException("卷烟代码不能为空");
        }
        String sanitized = rawCode.trim();
        if (sanitized.length() < 4) {
            throw new IllegalArgumentException("卷烟代码长度不足: " + sanitized);
        }
        return sanitized;
    }

    /**
     * 从 Map 中提取年份。
     * <p>
     * 从Map中提取指定键的年份值，支持多种数字类型转换。
     * </p>
     *
     * @param data Map 数据
     * @param key  字段键
     * @return 年份，解析失败返回 null
     * @example
     * <pre>
     *     Map<String, Object> data = new HashMap<>();
     *     data.put("year", 2025);
     *     Integer year = DistributionDataConverter.extractYear(data, "year");
     *     // 返回: 2025
     * </pre>
     */
    public static Integer extractYear(Map<String, Object> data, String key) {
        return extractInteger(data, key, "年份");
    }

    /**
     * 从 Map 中提取整数。
     * <p>
     * 从Map中提取指定键的整数值，支持多种数字类型转换。
     * </p>
     *
     * @param data Map 数据
     * @param key  字段键
     * @return 整数，解析失败返回 null
     * @example
     * <pre>
     *     Map<String, Object> data = new HashMap<>();
     *     data.put("month", 9);
     *     Integer month = DistributionDataConverter.extractInteger(data, "month");
     *     // 返回: 9
     * </pre>
     */
    public static Integer extractInteger(Map<String, Object> data, String key) {
        return extractInteger(data, key, "整数");
    }

    /**
     * 解析备注字段（兼容 remark/bz/BZ）。
     * <p>
     * 按优先级查找备注字段：remark -> bz -> BZ。
     * </p>
     *
     * @param advData 预投放数据 Map
     * @return 备注内容，未找到返回 null
     * @example
     * <pre>
     *     Map<String, Object> data = new HashMap<>();
     *     data.put("bz", "双周上浮100%");
     *     String remark = DistributionDataConverter.resolveRemark(data);
     *     // 返回: "双周上浮100%"
     * </pre>
     */
    public static String resolveRemark(Map<String, Object> advData) {
        if (advData == null) {
            return null;
        }
        Object remarkObj = advData.get("remark");
        if (remarkObj == null) {
            remarkObj = advData.get("bz");
        }
        if (remarkObj == null) {
            remarkObj = advData.get("BZ");
        }
        return remarkObj != null ? remarkObj.toString() : null;
    }

    /**
     * 将 Mapper 查询得到的行记录转换为 30 档客户数数组。
     * <p>
     * 从Map中提取D30到D1的客户数，转换为BigDecimal数组。
     * 索引0对应D30，索引29对应D1。
     * </p>
     *
     * @param row Mapper 查询结果行
     * @return 按 D30-D1 顺序的客户数数组，空值补 0
     * @example
     * <pre>
     *     Map<String, Object> row = new HashMap<>();
     *     row.put("D30", BigDecimal.valueOf(100));
     *     row.put("D29", BigDecimal.valueOf(200));
     *     BigDecimal[] counts = DistributionDataConverter.extractCustomerCounts(row);
     *     // counts[0] = 100 (D30), counts[1] = 200 (D29), counts[2-29] = 0
     * </pre>
     */
    public static BigDecimal[] extractCustomerCounts(Map<String, Object> row) {
        BigDecimal[] customerCounts = new BigDecimal[GRADE_NAMES.length];
        for (int i = 0; i < GRADE_NAMES.length; i++) {
            Object value = getObjectIgnoreCase(row, GRADE_NAMES[i]);
            if (value == null) {
                customerCounts[i] = BigDecimal.ZERO;
            } else if (value instanceof BigDecimal) {
                customerCounts[i] = (BigDecimal) value;
            } else {
                customerCounts[i] = new BigDecimal(value.toString());
            }
        }
        return customerCounts;
    }

    /**
     * 不区分大小写获取字符串字段。
     * <p>
     * 从Map中获取字符串值，支持大小写不敏感的键匹配。
     * </p>
     *
     * @param data Map 数据
     * @param key  目标键（不区分大小写）
     * @return 字符串值，若不存在返回 null
     * @example
     * <pre>
     *     Map<String, Object> data = new HashMap<>();
     *     data.put("CIG_CODE", "42020181");
     *     String code = DistributionDataConverter.getStringIgnoreCase(data, "cig_code");
     *     // 返回: "42020181"
     * </pre>
     */
    public static String getStringIgnoreCase(Map<String, Object> data, String key) {
        Object value = getObjectIgnoreCase(data, key);
        return value != null ? value.toString() : null;
    }

    /**
     * 不区分大小写获取 BigDecimal 字段。
     * <p>
     * 从Map中获取BigDecimal值，支持大小写不敏感的键匹配和类型转换。
     * </p>
     *
     * @param data Map 数据
     * @param key  目标键（不区分大小写）
     * @return BigDecimal 值，若不存在返回 null
     * @example
     * <pre>
     *     Map<String, Object> data = new HashMap<>();
     *     data.put("ADV", BigDecimal.valueOf(1000));
     *     BigDecimal adv = DistributionDataConverter.getBigDecimalIgnoreCase(data, "adv");
     *     // 返回: BigDecimal(1000)
     * </pre>
     */
    public static BigDecimal getBigDecimalIgnoreCase(Map<String, Object> data, String key) {
        Object value = getObjectIgnoreCase(data, key);
        if (value == null) {
            return null;
        }
        if (value instanceof BigDecimal) {
            return (BigDecimal) value;
        }
        try {
            return new BigDecimal(value.toString());
        } catch (NumberFormatException e) {
            log.warn("无法解析数值字段 {}: {}", key, value);
            return null;
        }
    }

    /**
     * 不区分大小写获取对象字段。
     * <p>
     * 从Map中获取对象值，支持大小写不敏感的键匹配。
     * </p>
     *
     * @param data Map 数据
     * @param key  目标键（不区分大小写）
     * @return 对象，若不存在返回 null
     * @example
     * <pre>
     *     Map<String, Object> data = new HashMap<>();
     *     data.put("TAG_FILTER_CONFIG", "{\"tag\":\"value\"}");
     *     Object config = DistributionDataConverter.getObjectIgnoreCase(data, "tag_filter_config");
     *     // 返回: "{\"tag\":\"value\"}"
     * </pre>
     */
    public static Object getObjectIgnoreCase(Map<String, Object> data, String key) {
        if (data == null || key == null) {
            return null;
        }
        Object value = data.get(key);
        if (value != null) {
            return value;
        }
        for (Map.Entry<String, Object> entry : data.entrySet()) {
            if (entry.getKey() != null && entry.getKey().equalsIgnoreCase(key)) {
                return entry.getValue();
            }
        }
        return null;
    }

    /**
     * 规范化 TAG_FILTER_CONFIG 字段为字符串。
     * <p>
     * 将TAG_FILTER_CONFIG字段转换为JSON字符串，支持多种类型转换。
     * </p>
     *
     * @param raw 原始对象
     * @return 字符串表示，序列化失败则使用 toString
     * @example
     * <pre>
     *     Map<String, Object> config = new HashMap<>();
     *     config.put("tag", "value");
     *     String json = DistributionDataConverter.normalizeTagFilterConfig(config);
     *     // 返回: "{\"tag\":\"value\"}"
     * </pre>
     */
    public static String normalizeTagFilterConfig(Object raw) {
        if (raw == null) {
            return null;
        }
        if (raw instanceof String) {
            return (String) raw;
        }
        if (raw instanceof Number || raw instanceof Boolean) {
            return raw.toString();
        }
        try {
            return OBJECT_MAPPER.writeValueAsString(raw);
        } catch (Exception e) {
            log.warn("TAG_FILTER_CONFIG 序列化失败: {}", raw, e);
            return raw.toString();
        }
    }

    /**
     * 从 Map 中提取年份（大小写不敏感）。
     * <p>
     * 从Map中提取年份值，支持大小写不敏感的键匹配。
     * </p>
     *
     * @param data Map 数据
     * @param key  字段键
     * @return 年份，解析失败返回 null
     * @example
     * <pre>
     *     Map<String, Object> data = new HashMap<>();
     *     data.put("YEAR", 2025);
     *     Integer year = DistributionDataConverter.extractYearIgnoreCase(data, "year");
     *     // 返回: 2025
     * </pre>
     */
    public static Integer extractYearIgnoreCase(Map<String, Object> data, String key) {
        return extractIntegerInternal(getObjectIgnoreCase(data, key), "年份");
    }

    /**
     * 从 Map 中提取整数（大小写不敏感）。
     * <p>
     * 从Map中提取整数值，支持大小写不敏感的键匹配。
     * </p>
     *
     * @param data Map 数据
     * @param key  字段键
     * @return 整数，解析失败返回 null
     * @example
     * <pre>
     *     Map<String, Object> data = new HashMap<>();
     *     data.put("MONTH", 9);
     *     Integer month = DistributionDataConverter.extractIntegerIgnoreCase(data, "month");
     *     // 返回: 9
     * </pre>
     */
    public static Integer extractIntegerIgnoreCase(Map<String, Object> data, String key) {
        return extractIntegerInternal(getObjectIgnoreCase(data, key), "整数");
    }

    private static Integer extractInteger(Map<String, Object> data, String key, String label) {
        Object value = data.get(key);
        return extractIntegerInternal(value, label);
    }

    private static Integer extractIntegerInternal(Object value, String label) {
        if (value == null) {
            return null;
        }
        if (value instanceof Integer) {
            return (Integer) value;
        }
        if (value instanceof Number) {
            return ((Number) value).intValue();
        }
        try {
            return Integer.parseInt(value.toString());
        } catch (NumberFormatException e) {
            log.warn("无法解析{}: {}", label, value);
            return null;
        }
    }
}

