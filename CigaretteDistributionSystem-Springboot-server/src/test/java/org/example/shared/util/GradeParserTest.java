package org.example.shared.util;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 * GradeParser解析能力测试
 * 用于验证问题2：档位解析是否正确
 */
public class GradeParserTest {

    @Test
    public void testParseChineseGrades() {
        // 测试base_customer_info中实际出现的档位格式
        String[] testGrades = {
            "九档",      // 应该解析为9 -> D9 (索引21)
            "十四档",    // 应该解析为14 -> D14 (索引16)
            "二十三档",  // 应该解析为23 -> D23 (索引7)
            "三十档",    // 应该解析为30 -> D30 (索引0)
            "三档",      // 应该解析为3 -> D3 (索引27)
            "十档",      // 应该解析为10 -> D10 (索引20)
            "二十档",    // 应该解析为20 -> D20 (索引10)
        };

        System.out.println("=== GradeParser解析能力测试 ===");
        for (String grade : testGrades) {
            int gradeNumber = GradeParser.extractGradeNumber(grade);
            int gradeIndex = GradeParser.parseGradeToIndex(grade);
            
            System.out.printf("档位: %-8s -> 数字: %2d -> 索引: %2d (D%d)%n", 
                grade, gradeNumber, gradeIndex, gradeNumber);
            
            // 验证解析结果
            assertTrue(gradeNumber > 0 && gradeNumber <= 30, 
                String.format("档位 %s 解析失败，数字: %d", grade, gradeNumber));
            assertTrue(gradeIndex >= 0 && gradeIndex < 30, 
                String.format("档位 %s 索引无效，索引: %d", grade, gradeIndex));
            
            // 验证索引计算：D30=0, D29=1, ..., D1=29
            int expectedIndex = 30 - gradeNumber;
            assertEquals(expectedIndex, gradeIndex, 
                String.format("档位 %s 索引计算错误，期望: %d, 实际: %d", 
                    grade, expectedIndex, gradeIndex));
        }
    }

    @Test
    public void testParseChineseNumber() {
        // 测试parseChineseNumber方法对复杂中文数字的解析
        String[] testNumbers = {
            "九", "十", "十一", "十二", "十三", "十四", "十五",
            "十六", "十七", "十八", "十九", "二十", "二十一",
            "二十二", "二十三", "二十四", "二十五", "二十六",
            "二十七", "二十八", "二十九", "三十"
        };

        System.out.println("\n=== parseChineseNumber解析能力测试 ===");
        for (String numStr : testNumbers) {
            int result = GradeParser.parseChineseNumber(numStr);
            System.out.printf("中文数字: %-6s -> %2d%n", numStr, result);
            
            // 验证1-30都能正确解析
            if (numStr.equals("九")) {
                assertEquals(9, result, "九应该解析为9");
            } else if (numStr.equals("十")) {
                assertEquals(10, result, "十应该解析为10");
            } else if (numStr.equals("二十三")) {
                assertEquals(23, result, "二十三应该解析为23");
            } else if (numStr.equals("三十")) {
                assertEquals(30, result, "三十应该解析为30");
            }
        }
    }

    @Test
    public void testParseGradeWithSuffix() {
        // 测试带后缀的档位格式
        String[] testGrades = {
            "九档", "第9档", "D9", "9档", "第九档"
        };

        System.out.println("\n=== 不同格式档位解析测试 ===");
        for (String grade : testGrades) {
            int gradeNumber = GradeParser.extractGradeNumber(grade);
            int gradeIndex = GradeParser.parseGradeToIndex(grade);
            
            System.out.printf("格式: %-8s -> 数字: %2d -> 索引: %2d%n", 
                grade, gradeNumber, gradeIndex);
            
            // 所有格式都应该解析为9
            assertEquals(9, gradeNumber, 
                String.format("档位 %s 应该解析为9，实际: %d", grade, gradeNumber));
            assertEquals(21, gradeIndex, 
                String.format("档位 %s 索引应该为21，实际: %d", grade, gradeIndex));
        }
    }
}

