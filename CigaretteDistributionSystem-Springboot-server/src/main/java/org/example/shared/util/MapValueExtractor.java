package org.example.shared.util;

import java.math.BigDecimal;
import java.util.Map;

/**
 * Map值提取工具类
 * <p>
 * 提供从Map中提取值的通用方法，支持大小写不敏感的键匹配和类型转换。
 * 主要用于处理数据库查询返回的Map结果，提供类型安全的提取方法。
 * </p>
 *
 * @author Robin
 * @version 1.0
 * @since 2025-12-11
 */
public final class MapValueExtractor {

    private MapValueExtractor() {
        // 工具类，禁止实例化
    }

    /**
     * 将对象转换为 BigDecimal。
     * <p>
     * 支持 BigDecimal、Number、String 类型的转换。
     * </p>
     *
     * @param value 原始对象
     * @return BigDecimal；无法解析时返回 null
     * @example
     * <pre>
     *     BigDecimal bd = MapValueExtractor.toBigDecimal("1.23");
     *     // 返回: 1.23
     *     BigDecimal bd2 = MapValueExtractor.toBigDecimal(100);
     *     // 返回: 100
     * </pre>
     */
    public static BigDecimal toBigDecimal(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof BigDecimal) {
            return (BigDecimal) value;
        }
        if (value instanceof Number) {
            return new BigDecimal(value.toString());
        }
        try {
            return new BigDecimal(value.toString().trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    /**
     * 将对象转换为 Integer。
     * <p>
     * 支持 Integer、Long、Number、String 类型的转换。
     * </p>
     *
     * @param value 原始对象
     * @return Integer；无法解析时返回 null
     * @example
     * <pre>
     *     Integer i = MapValueExtractor.toInteger(100L);
     *     // 返回: 100
     *     Integer i2 = MapValueExtractor.toInteger("200");
     *     // 返回: 200
     * </pre>
     */
    public static Integer toInteger(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Integer) {
            return (Integer) value;
        }
        if (value instanceof Long) {
            return ((Long) value).intValue();
        }
        if (value instanceof Number) {
            return ((Number) value).intValue();
        }
        try {
            return Integer.valueOf(value.toString().trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    /**
     * 将对象转换为 Integer，如果为 null 则返回默认值。
     *
     * @param value        原始对象
     * @param defaultValue 默认值
     * @return Integer；无法解析时返回默认值
     */
    public static Integer toInteger(Object value, Integer defaultValue) {
        Integer result = toInteger(value);
        return result != null ? result : defaultValue;
    }

    /**
     * 从Map中获取Long类型的值。
     * <p>
     * 支持大小写不敏感的键匹配，并自动转换Number类型为Long。
     * </p>
     *
     * @param map Map对象（通常是从数据库查询返回的结果Map）
     * @param key 键名（大小写不敏感）
     * @return Long值，如果键不存在或无法转换则返回 null
     * @example
     * <pre>
     *     Map<String, Object> map = new HashMap<>();
     *     map.put("CUSTOMER_COUNT", 100L);
     *     Long count = MapValueExtractor.getLongValue(map, "customer_count");
     *     // 返回: 100L（大小写不敏感）
     *     Long count2 = MapValueExtractor.getLongValue(map, "NOT_EXIST");
     *     // 返回: null
     * </pre>
     */
    public static Long getLongValue(Map<String, Object> map, String key) {
        Object value = map.get(key);
        if (value == null) {
            for (Map.Entry<String, Object> entry : map.entrySet()) {
                if (entry.getKey().equalsIgnoreCase(key)) {
                    value = entry.getValue();
                    break;
                }
            }
        }
        if (value == null) {
            return null;
        }
        if (value instanceof Long) {
            return (Long) value;
        }
        if (value instanceof Number) {
            return ((Number) value).longValue();
        }
        try {
            return Long.parseLong(value.toString());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    /**
     * 从Map中获取String类型的值。
     * <p>
     * 支持大小写不敏感的键匹配，并自动转换为字符串（去除前后空格）。
     * </p>
     *
     * @param map Map对象（通常是从数据库查询返回的结果Map）
     * @param key 键名（大小写不敏感）
     * @return 字符串值（已trim），如果键不存在则返回 null
     * @example
     * <pre>
     *     Map<String, Object> map = new HashMap<>();
     *     map.put("GRADE", " D30 ");
     *     String grade = MapValueExtractor.getStringValue(map, "grade");
     *     // 返回: "D30"（大小写不敏感，已trim）
     *     String grade2 = MapValueExtractor.getStringValue(map, "NOT_EXIST");
     *     // 返回: null
     * </pre>
     */
    public static String getStringValue(Map<String, Object> map, String key) {
        Object value = getObjectIgnoreCase(map, key);
        return value != null ? value.toString().trim() : null;
    }

    /**
     * 从Map中获取Object类型的值（大小写不敏感）。
     * <p>
     * 支持大小写不敏感的键匹配，返回原始对象值。
     * </p>
     *
     * @param map Map对象（通常是从数据库查询返回的结果Map）
     * @param key 键名（大小写不敏感）
     * @return 对象值，如果键不存在则返回 null
     * @example
     * <pre>
     *     Map&lt;String, Object&gt; map = new HashMap&lt;&gt;();
     *     map.put("CIG_CODE", "42020181");
     *     Object code = MapValueExtractor.getObjectIgnoreCase(map, "cig_code");
     *     // 返回: "42020181"（大小写不敏感）
     * </pre>
     */
    public static Object getObjectIgnoreCase(Map<String, Object> map, String key) {
        if (map == null || key == null) {
            return null;
        }
        Object value = map.get(key);
        if (value != null) {
            return value;
        }
            for (Map.Entry<String, Object> entry : map.entrySet()) {
            if (entry.getKey() != null && entry.getKey().equalsIgnoreCase(key)) {
                return entry.getValue();
            }
        }
        return null;
    }
}

