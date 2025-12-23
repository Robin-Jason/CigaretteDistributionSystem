package org.example.domain.model.valueobject;

import org.example.shared.util.GradeParser;

/**
 * 档位范围值对象。
 * <p>
 * 封装最高档位（HG）和最低档位（LG），提供索引转换和范围判断功能。
 * 档位范围用于指定分配算法的计算范围，算法只在 HG 到 LG 范围内进行分配计算。
 * </p>
 *
 * @author Robin
 * @version 1.0
 * @since 2025-12-23
 * @example
 * <pre>{@code
 * // 创建默认范围（D30-D1）
 * GradeRange fullRange = GradeRange.full();
 * 
 * // 创建指定范围（D20-D5）
 * GradeRange customRange = GradeRange.of("D20", "D5");
 * 
 * // 获取索引
 * int maxIndex = customRange.getMaxIndex();  // 10 (D20对应索引10)
 * int minIndex = customRange.getMinIndex();  // 25 (D5对应索引25)
 * 
 * // 判断索引是否在范围内
 * boolean inRange = customRange.contains(15);  // true (15在10-25之间)
 * }</pre>
 */
public class GradeRange {
    
    private static final String DEFAULT_MAX_GRADE = "D30";
    private static final String DEFAULT_MIN_GRADE = "D1";
    
    private final String maxGrade;  // HG，如 "D30"
    private final String minGrade;  // LG，如 "D1"
    
    private GradeRange(String maxGrade, String minGrade) {
        this.maxGrade = normalizeGrade(maxGrade, DEFAULT_MAX_GRADE);
        this.minGrade = normalizeGrade(minGrade, DEFAULT_MIN_GRADE);
    }
    
    /**
     * 创建默认范围（D30-D1）。
     * <p>
     * 默认范围覆盖所有30个档位，适用于未指定 HG/LG 的场景。
     * </p>
     *
     * @return 默认档位范围（D30-D1）
     */
    public static GradeRange full() {
        return new GradeRange(DEFAULT_MAX_GRADE, DEFAULT_MIN_GRADE);
    }
    
    /**
     * 创建指定范围。
     * <p>
     * 如果 maxGrade 或 minGrade 为 null 或空字符串，将使用默认值。
     * </p>
     *
     * @param maxGrade 最高档位（HG），如 "D30"，为 null 或空时默认为 "D30"
     * @param minGrade 最低档位（LG），如 "D1"，为 null 或空时默认为 "D1"
     * @return 指定的档位范围
     */
    public static GradeRange of(String maxGrade, String minGrade) {
        return new GradeRange(maxGrade, minGrade);
    }
    
    /**
     * 获取 HG 索引（D30=0）。
     * <p>
     * 将最高档位字符串转换为数组索引，D30对应索引0，D29对应索引1，以此类推。
     * </p>
     *
     * @return HG 对应的数组索引（0-29）
     */
    public int getMaxIndex() {
        return GradeParser.parseGradeToIndex(maxGrade);
    }
    
    /**
     * 获取 LG 索引（D1=29）。
     * <p>
     * 将最低档位字符串转换为数组索引，D1对应索引29，D2对应索引28，以此类推。
     * </p>
     *
     * @return LG 对应的数组索引（0-29）
     */
    public int getMinIndex() {
        return GradeParser.parseGradeToIndex(minGrade);
    }
    
    /**
     * 判断索引是否在范围内。
     * <p>
     * 检查给定的档位索引是否在 HG 到 LG 范围内（包含边界）。
     * </p>
     *
     * @param gradeIndex 档位索引（0-29）
     * @return 如果索引在范围内返回 true，否则返回 false
     */
    public boolean contains(int gradeIndex) {
        return gradeIndex >= getMaxIndex() && gradeIndex <= getMinIndex();
    }
    
    /**
     * 获取最高档位字符串。
     *
     * @return 最高档位（HG），如 "D30"
     */
    public String getMaxGrade() {
        return maxGrade;
    }
    
    /**
     * 获取最低档位字符串。
     *
     * @return 最低档位（LG），如 "D1"
     */
    public String getMinGrade() {
        return minGrade;
    }
    
    /**
     * 规范化档位字符串。
     * <p>
     * 处理 null 或空字符串，使用默认值；去除首尾空格并转换为大写。
     * </p>
     *
     * @param grade        档位字符串
     * @param defaultValue 默认值
     * @return 规范化后的档位字符串
     */
    private static String normalizeGrade(String grade, String defaultValue) {
        if (grade == null || grade.trim().isEmpty()) {
            return defaultValue;
        }
        return grade.trim().toUpperCase();
    }
    
    @Override
    public String toString() {
        return "GradeRange{" +
                "maxGrade='" + maxGrade + '\'' +
                ", minGrade='" + minGrade + '\'' +
                ", maxIndex=" + getMaxIndex() +
                ", minIndex=" + getMinIndex() +
                '}';
    }
}
