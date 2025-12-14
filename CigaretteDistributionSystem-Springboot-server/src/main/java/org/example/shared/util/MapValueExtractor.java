package org.example.shared.util;

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
        Object value = map.get(key);
        if (value == null) {
            for (Map.Entry<String, Object> entry : map.entrySet()) {
                if (entry.getKey().equalsIgnoreCase(key)) {
                    value = entry.getValue();
                    break;
                }
            }
        }
        return value != null ? value.toString().trim() : null;
    }
}

