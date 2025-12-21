package org.example.shared.util;

import org.example.shared.constants.GradeConstants;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Map;

/**
 * 档位提取工具类。
 * <p>提供从不同数据源提取30个档位值的统一方法。</p>
 *
 * @author Robin
 * @since 2025-12-20
 */
public final class GradeExtractor {

    private GradeExtractor() {
        // 工具类，禁止实例化
    }

    /**
     * 从Map提取30个档位值（D30-D1），支持大小写不敏感。
     * <p>档位数组索引对应关系：索引0=D30，索引29=D1。</p>
     *
     * @param row Map对象，包含D30到D1的键值对
     * @return 30个档位的BigDecimal数组，空值补0
     * @example
     * <pre>
     *     Map&lt;String, Object&gt; row = new HashMap&lt;&gt;();
     *     row.put("D30", BigDecimal.valueOf(100));
     *     row.put("D29", BigDecimal.valueOf(200));
     *     BigDecimal[] grades = GradeExtractor.extractFromMap(row);
     *     // grades[0] = 100 (D30), grades[1] = 200 (D29), grades[2-29] = 0
     * </pre>
     */
    public static BigDecimal[] extractFromMap(Map<String, Object> row) {
        if (row == null) {
            BigDecimal[] grades = new BigDecimal[GradeConstants.GRADE_COUNT];
            Arrays.fill(grades, BigDecimal.ZERO);
            return grades;
        }

        BigDecimal[] grades = new BigDecimal[GradeConstants.GRADE_COUNT];
        Arrays.fill(grades, BigDecimal.ZERO);

        for (int i = 0; i < GradeConstants.GRADE_COUNT; i++) {
            String column = GradeConstants.GRADE_NAMES[i];
            Object value = MapValueExtractor.getObjectIgnoreCase(row, column);
            if (value != null) {
                grades[i] = WriteBackHelper.toBigDecimal(value);
                if (grades[i] == null) {
                    grades[i] = BigDecimal.ZERO;
                }
            }
        }
        return grades;
    }
}

