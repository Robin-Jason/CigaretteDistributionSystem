package org.example.shared.util;

import java.math.BigDecimal;

/**
 * 实际投放量计算工具类。
 * <p>提供统一的实际投放量计算方法，计算公式：实际投放量 = Σ(分配值[i] × 客户数[i])</p>
 *
 * @author Robin
 * @since 2025-12-19
 */
public final class ActualDeliveryCalculator {

    private ActualDeliveryCalculator() {
        // 工具类，禁止实例化
    }

    /**
     * 计算实际投放量。
     * <p>计算公式：实际投放量 = Σ(分配值[i] × 客户数[i])</p>
     * <p>数组长度必须一致，如果长度不一致，将使用较短数组的长度进行计算。</p>
     *
     * @param allocationValues 分配值数组（30个档位，D30-D1）
     * @param customerCounts   客户数数组（30个档位，D30-D1）
     * @return 实际投放量
     * @throws IllegalArgumentException 如果任一数组为null
     *
     * @example
     * <pre>
     * BigDecimal[] allocation = {new BigDecimal("10"), new BigDecimal("20"), ...}; // 30个值
     * BigDecimal[] customers = {new BigDecimal("5"), new BigDecimal("8"), ...}; // 30个值
     * BigDecimal actualDelivery = ActualDeliveryCalculator.calculate(allocation, customers);
     * // 结果 = 10*5 + 20*8 + ...
     * </pre>
     */
    public static BigDecimal calculate(BigDecimal[] allocationValues, BigDecimal[] customerCounts) {
        if (allocationValues == null) {
            throw new IllegalArgumentException("分配值数组不能为null");
        }
        if (customerCounts == null) {
            throw new IllegalArgumentException("客户数数组不能为null");
        }

        BigDecimal total = BigDecimal.ZERO;
        int length = Math.min(allocationValues.length, customerCounts.length);

        for (int i = 0; i < length; i++) {
            BigDecimal allocation = allocationValues[i] != null ? allocationValues[i] : BigDecimal.ZERO;
            BigDecimal customerCount = customerCounts[i] != null ? customerCounts[i] : BigDecimal.ZERO;
            total = total.add(allocation.multiply(customerCount));
        }

        return total;
    }

    /**
     * 计算实际投放量（固定30个档位）。
     * <p>适用于已知数组长度为30的场景，性能略优于通用方法。</p>
     *
     * @param allocationValues 分配值数组（必须包含30个值，对应D30-D1）
     * @param customerCounts   客户数数组（必须包含30个值，对应D30-D1）
     * @return 实际投放量
     * @throws IllegalArgumentException 如果任一数组为null或长度不为30
     *
     * @example
     * <pre>
     * BigDecimal[] allocation = new BigDecimal[30]; // 30个档位
     * BigDecimal[] customers = new BigDecimal[30]; // 30个档位
     * BigDecimal actualDelivery = ActualDeliveryCalculator.calculateFixed30(allocation, customers);
     * </pre>
     */
    public static BigDecimal calculateFixed30(BigDecimal[] allocationValues, BigDecimal[] customerCounts) {
        if (allocationValues == null || allocationValues.length != 30) {
            throw new IllegalArgumentException("分配值数组必须包含30个值");
        }
        if (customerCounts == null || customerCounts.length != 30) {
            throw new IllegalArgumentException("客户数数组必须包含30个值");
        }

        BigDecimal total = BigDecimal.ZERO;
        for (int i = 0; i < 30; i++) {
            BigDecimal allocation = allocationValues[i] != null ? allocationValues[i] : BigDecimal.ZERO;
            BigDecimal customerCount = customerCounts[i] != null ? customerCounts[i] : BigDecimal.ZERO;
            total = total.add(allocation.multiply(customerCount));
        }

        return total;
    }
}

