package org.example.shared.util;

import org.example.infrastructure.persistence.po.CigaretteDistributionPredictionPO;

import java.math.BigDecimal;
import java.util.Map;

/**
 * 写回服务公共工具类。
 * <p>提供写回过程中使用的公共方法，包括档位值设置、数据类型转换等。</p>
 *
 * @author Robin
 * @since 2025-12-19
 */
public final class WriteBackHelper {

    private WriteBackHelper() {
        // 工具类，禁止实例化
    }

    /**
     * 将 30 档位值设置到实体。
     * <p>档位数组索引对应关系：索引0=D30，索引29=D1。</p>
     *
     * @param predictionData 实体对象
     * @param grades 档位数组（长度必须为30）
     * @throws IllegalArgumentException 如果档位数组为null或长度不为30
     */
    public static void setGradesToEntity(CigaretteDistributionPredictionPO predictionData, BigDecimal[] grades) {
        if (grades == null || grades.length != 30) {
            throw new IllegalArgumentException("档位数组必须包含30个值");
        }
        predictionData.setD30(grades[0]);
        predictionData.setD29(grades[1]);
        predictionData.setD28(grades[2]);
        predictionData.setD27(grades[3]);
        predictionData.setD26(grades[4]);
        predictionData.setD25(grades[5]);
        predictionData.setD24(grades[6]);
        predictionData.setD23(grades[7]);
        predictionData.setD22(grades[8]);
        predictionData.setD21(grades[9]);
        predictionData.setD20(grades[10]);
        predictionData.setD19(grades[11]);
        predictionData.setD18(grades[12]);
        predictionData.setD17(grades[13]);
        predictionData.setD16(grades[14]);
        predictionData.setD15(grades[15]);
        predictionData.setD14(grades[16]);
        predictionData.setD13(grades[17]);
        predictionData.setD12(grades[18]);
        predictionData.setD11(grades[19]);
        predictionData.setD10(grades[20]);
        predictionData.setD9(grades[21]);
        predictionData.setD8(grades[22]);
        predictionData.setD7(grades[23]);
        predictionData.setD6(grades[24]);
        predictionData.setD5(grades[25]);
        predictionData.setD4(grades[26]);
        predictionData.setD3(grades[27]);
        predictionData.setD2(grades[28]);
        predictionData.setD1(grades[29]);
    }

    /**
     * 从 Map 中安全获取字符串值。
     *
     * @param map Map对象
     * @param key 键名
     * @return 字符串值，如果不存在或为null则返回null
     */
    public static String getString(Map<String, Object> map, String key) {
        if (map == null) {
            return null;
        }
        Object value = map.get(key);
        if (value == null) {
            return null;
        }
        return value.toString();
    }

    /**
     * 将对象转换为 BigDecimal。
     * <p>支持 BigDecimal、Number 类型以及可转换为数字的字符串。</p>
     *
     * @param value 待转换的值
     * @return BigDecimal 对象，如果转换失败则返回null
     */
    public static BigDecimal toBigDecimal(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof BigDecimal) {
            return (BigDecimal) value;
        }
        try {
            return new BigDecimal(value.toString());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    /**
     * 将对象转换为 Integer。
     * <p>支持 Integer、Number 类型以及可转换为整数的字符串。</p>
     *
     * @param value 待转换的值
     * @return Integer 对象，如果转换失败则返回null
     */
    public static Integer toInteger(Object value) {
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
            return null;
        }
    }
}

