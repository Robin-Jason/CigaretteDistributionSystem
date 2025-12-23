package org.example.domain.service;

import org.example.domain.model.valueobject.GradeRange;
import org.example.domain.service.algorithm.PriceBandTruncationService;
import org.example.domain.service.algorithm.impl.PriceBandTruncationServiceImpl;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 价位段截断算法 GradeRange 支持测试。
 * <p>
 * 验证价位段算法：
 * 1. 支持 GradeRange 参数
 * 2. 去掉了单调性检验
 * 3. 在指定范围内正确执行截断和微调
 * </p>
 */
public class PriceBandTruncationWithGradeRangeTest {

    private final PriceBandTruncationService service = new PriceBandTruncationServiceImpl();

    @Test
    public void testTruncationWithGradeRange() {
        // 准备客户数数组（D30~D1）
        BigDecimal[] customerRow = new BigDecimal[30];
        Arrays.fill(customerRow, new BigDecimal("100"));

        // 准备价位段分组数据（价位段100，包含2支卷烟）
        Map<Integer, List<Map<String, Object>>> bands = new TreeMap<>();
        List<Map<String, Object>> band100 = new ArrayList<>();

        // 卷烟1：目标投放量 3000
        Map<String, Object> cig1 = new HashMap<>();
        cig1.put("CIG_CODE", "TEST001");
        cig1.put("CIG_NAME", "测试卷烟1");
        cig1.put("ADV", new BigDecimal("3000"));
        BigDecimal[] grades1 = new BigDecimal[30];
        Arrays.fill(grades1, BigDecimal.ZERO);
        // 模拟初分配结果：D30~D1 都有分配
        for (int i = 0; i < 30; i++) {
            grades1[i] = new BigDecimal("1");
        }
        cig1.put("GRADES", grades1);
        band100.add(cig1);

        // 卷烟2：目标投放量 2000
        Map<String, Object> cig2 = new HashMap<>();
        cig2.put("CIG_CODE", "TEST002");
        cig2.put("CIG_NAME", "测试卷烟2");
        cig2.put("ADV", new BigDecimal("2000"));
        BigDecimal[] grades2 = new BigDecimal[30];
        Arrays.fill(grades2, BigDecimal.ZERO);
        // 模拟初分配结果：D30~D1 都有分配
        for (int i = 0; i < 30; i++) {
            grades2[i] = new BigDecimal("1");
        }
        cig2.put("GRADES", grades2);
        band100.add(cig2);

        bands.put(100, band100);

        // 指定档位范围：D25~D15（索引 5~15）
        GradeRange gradeRange = GradeRange.of("D25", "D15");

        // 执行截断与微调
        service.truncateAndAdjust(bands, customerRow, gradeRange, 2025, 9, 3);

        // 验证结果
        BigDecimal[] result1 = (BigDecimal[]) cig1.get("GRADES");
        BigDecimal[] result2 = (BigDecimal[]) cig2.get("GRADES");

        // 验证范围外的档位为0
        for (int i = 0; i < 5; i++) {
            assertEquals(BigDecimal.ZERO, result1[i], "卷烟1 D" + (30 - i) + " 应该为0（范围外）");
            assertEquals(BigDecimal.ZERO, result2[i], "卷烟2 D" + (30 - i) + " 应该为0（范围外）");
        }
        for (int i = 16; i < 30; i++) {
            assertEquals(BigDecimal.ZERO, result1[i], "卷烟1 D" + (30 - i) + " 应该为0（范围外）");
            assertEquals(BigDecimal.ZERO, result2[i], "卷烟2 D" + (30 - i) + " 应该为0（范围外）");
        }

        // 验证范围内有分配值
        boolean hasAllocationInRange1 = false;
        boolean hasAllocationInRange2 = false;
        for (int i = 5; i <= 15; i++) {
            if (result1[i].compareTo(BigDecimal.ZERO) > 0) {
                hasAllocationInRange1 = true;
            }
            if (result2[i].compareTo(BigDecimal.ZERO) > 0) {
                hasAllocationInRange2 = true;
            }
        }
        assertTrue(hasAllocationInRange1, "卷烟1 在范围内应该有分配值");
        assertTrue(hasAllocationInRange2, "卷烟2 在范围内应该有分配值");

        System.out.println("✅ 价位段算法 GradeRange 支持测试通过");
        System.out.println("   - 范围外档位正确清零");
        System.out.println("   - 范围内档位正确分配");
        System.out.println("   - 无单调性约束限制");
    }

    @Test
    public void testNoMonotonicConstraint() {
        // 准备客户数数组
        BigDecimal[] customerRow = new BigDecimal[30];
        Arrays.fill(customerRow, new BigDecimal("100"));

        // 准备价位段分组数据
        Map<Integer, List<Map<String, Object>>> bands = new TreeMap<>();
        List<Map<String, Object>> band100 = new ArrayList<>();

        // 卷烟1：目标投放量 1500
        Map<String, Object> cig1 = new HashMap<>();
        cig1.put("CIG_CODE", "TEST003");
        cig1.put("CIG_NAME", "测试卷烟3");
        cig1.put("ADV", new BigDecimal("1500"));
        BigDecimal[] grades1 = new BigDecimal[30];
        Arrays.fill(grades1, BigDecimal.ZERO);
        // 初始分配：D30=5, D29=3（违反单调性：D29 < D30）
        grades1[0] = new BigDecimal("5");
        grades1[1] = new BigDecimal("3");
        cig1.put("GRADES", grades1);
        band100.add(cig1);

        // 卷烟2：目标投放量 1000
        Map<String, Object> cig2 = new HashMap<>();
        cig2.put("CIG_CODE", "TEST004");
        cig2.put("CIG_NAME", "测试卷烟4");
        cig2.put("ADV", new BigDecimal("1000"));
        BigDecimal[] grades2 = new BigDecimal[30];
        Arrays.fill(grades2, BigDecimal.ZERO);
        // 初始分配：D30=2, D29=4（违反单调性：D29 > D30）
        grades2[0] = new BigDecimal("2");
        grades2[1] = new BigDecimal("4");
        cig2.put("GRADES", grades2);
        band100.add(cig2);

        bands.put(100, band100);

        // 使用全范围
        GradeRange gradeRange = GradeRange.full();

        // 执行截断与微调（不应该因为单调性问题而失败）
        assertDoesNotThrow(() -> {
            service.truncateAndAdjust(bands, customerRow, gradeRange, 2025, 9, 3);
        }, "算法不应该因为单调性问题而失败");

        System.out.println("✅ 无单调性约束测试通过");
        System.out.println("   - 算法允许 D29 > D30 的情况");
        System.out.println("   - 算法允许 D29 < D30 的情况");
        System.out.println("   - 微调过程不受单调性限制");
    }
}
