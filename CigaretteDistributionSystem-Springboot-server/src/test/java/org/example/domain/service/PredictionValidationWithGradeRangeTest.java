package org.example.domain.service;

import org.example.domain.service.rule.PredictionValidationRule;
import org.example.domain.service.rule.PredictionValidationRule.ValidationResult;
import org.example.domain.service.rule.impl.PredictionValidationRuleImpl;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 预测数据单调性验证（基于 HG/LG 范围）测试。
 * <p>
 * 验证新的单调性验证逻辑：
 * 1. HG 之前的档位必须全为 0
 * 2. HG~LG 范围内满足单调递减
 * 3. LG 之后的档位必须全为 0
 * </p>
 */
public class PredictionValidationWithGradeRangeTest {

    private final PredictionValidationRule validationRule = new PredictionValidationRuleImpl();

    @Test
    public void testValidGradesWithinRange() {
        // 准备档位数据：D25~D15 范围内有值，其他为 0
        List<BigDecimal> grades = new ArrayList<>();
        for (int i = 0; i < 30; i++) {
            grades.add(BigDecimal.ZERO);
        }
        
        // D25(索引5) ~ D15(索引15) 范围内设置单调递减的值
        grades.set(5, new BigDecimal("100"));  // D25
        grades.set(6, new BigDecimal("90"));   // D24
        grades.set(7, new BigDecimal("80"));   // D23
        grades.set(8, new BigDecimal("70"));   // D22
        grades.set(9, new BigDecimal("60"));   // D21
        grades.set(10, new BigDecimal("50"));  // D20
        grades.set(11, new BigDecimal("40"));  // D19
        grades.set(12, new BigDecimal("30"));  // D18
        grades.set(13, new BigDecimal("20"));  // D17
        grades.set(14, new BigDecimal("10"));  // D16
        grades.set(15, new BigDecimal("5"));   // D15

        ValidationResult result = validationRule.validateGradesMonotonicityWithRange(grades, "D25", "D15");
        
        assertTrue(result.isValid(), "范围内单调递减应该通过验证");
        System.out.println("✅ 测试通过：范围内单调递减");
    }

    @Test
    public void testInvalidGradesBeforeHG() {
        // 准备档位数据：D30 有值（HG 之前）
        List<BigDecimal> grades = new ArrayList<>();
        for (int i = 0; i < 30; i++) {
            grades.add(BigDecimal.ZERO);
        }
        
        grades.set(0, new BigDecimal("10"));  // D30（范围外）
        grades.set(5, new BigDecimal("100")); // D25（范围内）

        ValidationResult result = validationRule.validateGradesMonotonicityWithRange(grades, "D25", "D15");
        
        assertFalse(result.isValid(), "HG 之前有非零值应该验证失败");
        assertTrue(result.getErrorMessage().contains("D30"), "错误信息应该包含 D30");
        assertTrue(result.getErrorMessage().contains("范围外"), "错误信息应该说明是范围外");
        System.out.println("✅ 测试通过：HG 之前有非零值被正确拒绝");
        System.out.println("   错误信息：" + result.getErrorMessage());
    }

    @Test
    public void testInvalidGradesAfterLG() {
        // 准备档位数据：D10 有值（LG 之后）
        List<BigDecimal> grades = new ArrayList<>();
        for (int i = 0; i < 30; i++) {
            grades.add(BigDecimal.ZERO);
        }
        
        grades.set(5, new BigDecimal("100"));  // D25（范围内）
        grades.set(6, new BigDecimal("90"));   // D24（范围内）
        grades.set(7, new BigDecimal("80"));   // D23（范围内）
        // ... 其他范围内档位都为 0（允许）
        grades.set(15, BigDecimal.ZERO);       // D15（范围内，边界）
        grades.set(20, new BigDecimal("10"));  // D10（范围外，不应该有值）

        ValidationResult result = validationRule.validateGradesMonotonicityWithRange(grades, "D25", "D15");
        
        assertFalse(result.isValid(), "LG 之后有非零值应该验证失败");
        System.out.println("   实际错误信息：" + result.getErrorMessage());
        assertTrue(result.getErrorMessage().contains("范围外"), "错误信息应该说明是范围外");
        System.out.println("✅ 测试通过：LG 之后有非零值被正确拒绝");
    }

    @Test
    public void testInvalidMonotonicityWithinRange() {
        // 准备档位数据：范围内不满足单调递减
        List<BigDecimal> grades = new ArrayList<>();
        for (int i = 0; i < 30; i++) {
            grades.add(BigDecimal.ZERO);
        }
        
        grades.set(5, new BigDecimal("100"));  // D25
        grades.set(6, new BigDecimal("110"));  // D24 > D25（违反单调性）
        grades.set(7, new BigDecimal("80"));   // D23

        ValidationResult result = validationRule.validateGradesMonotonicityWithRange(grades, "D25", "D15");
        
        assertFalse(result.isValid(), "范围内不满足单调递减应该验证失败");
        assertTrue(result.getErrorMessage().contains("D24"), "错误信息应该包含 D24");
        assertTrue(result.getErrorMessage().contains("D25"), "错误信息应该包含 D25");
        assertTrue(result.getErrorMessage().contains("单调递减"), "错误信息应该说明单调递减约束");
        System.out.println("✅ 测试通过：范围内违反单调性被正确拒绝");
        System.out.println("   错误信息：" + result.getErrorMessage());
    }

    @Test
    public void testFullRange() {
        // 测试全范围（D30~D1）
        List<BigDecimal> grades = new ArrayList<>();
        for (int i = 0; i < 30; i++) {
            grades.add(new BigDecimal(String.valueOf(30 - i)));  // D30=30, D29=29, ..., D1=1
        }

        ValidationResult result = validationRule.validateGradesMonotonicityWithRange(grades, "D30", "D1");
        
        assertTrue(result.isValid(), "全范围单调递减应该通过验证");
        System.out.println("✅ 测试通过：全范围单调递减");
    }

    @Test
    public void testPartialZerosWithinRange() {
        // 测试范围内部分为 0（这是允许的）
        List<BigDecimal> grades = new ArrayList<>();
        for (int i = 0; i < 30; i++) {
            grades.add(BigDecimal.ZERO);
        }
        
        grades.set(5, new BigDecimal("100"));  // D25
        grades.set(6, new BigDecimal("90"));   // D24
        grades.set(7, BigDecimal.ZERO);        // D23 = 0（允许）
        grades.set(8, BigDecimal.ZERO);        // D22 = 0（允许）
        grades.set(15, BigDecimal.ZERO);       // D15 = 0（允许）

        ValidationResult result = validationRule.validateGradesMonotonicityWithRange(grades, "D25", "D15");
        
        assertTrue(result.isValid(), "范围内部分为 0 应该通过验证");
        System.out.println("✅ 测试通过：范围内部分为 0");
    }
}
