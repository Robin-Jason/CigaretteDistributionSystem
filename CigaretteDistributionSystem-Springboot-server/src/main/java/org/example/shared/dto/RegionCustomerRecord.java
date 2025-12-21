package org.example.shared.dto;

import java.math.BigDecimal;

/**
 * 区域客户记录DTO
 * <p>
 * 用于表示区域客户数统计记录，包含区域名称、30个档位值和总客户数。
 * </p>
 *
 * @author Robin
 * @version 1.0
 * @since 2025-12-12
 */
public class RegionCustomerRecord {
    private final String region;
    private final BigDecimal[] grades; // 30个档位值（D30到D1）
    private final BigDecimal total;

    public RegionCustomerRecord(String region, BigDecimal[] grades, BigDecimal total) {
        this.region = region;
        this.grades = grades;
        this.total = total;
    }

    public String getRegion() {
        return region;
    }

    public BigDecimal[] getGrades() {
        return grades;
    }

    public BigDecimal getTotal() {
        return total;
    }

    /**
     * 获取指定索引的档位值（用于MyBatis映射）
     * 
     * @param index 档位索引（0=D30, 1=D29, ..., 29=D1）
     * @return 档位值，如果索引越界返回BigDecimal.ZERO
     */
    public BigDecimal getGrade(int index) {
        if (grades == null || index < 0 || index >= grades.length) {
            return BigDecimal.ZERO;
        }
        return grades[index] != null ? grades[index] : BigDecimal.ZERO;
    }

    /**
     * 转换为SQL插入参数数组
     * 顺序：YEAR, MONTH, WEEK_SEQ, REGION, D30, D29, ..., D1, TOTAL
     *
     * @param year    年份
     * @param month   月份
     * @param weekSeq 周序号
     * @return SQL参数数组
     */
    public Object[] toSqlParams(Integer year, Integer month, Integer weekSeq) {
        Object[] params = new Object[35]; // 3个时间字段 + 1个region + 30个档位 + 1个total
        params[0] = year;
        params[1] = month;
        params[2] = weekSeq;
        params[3] = region;
        for (int i = 0; i < 30; i++) {
            params[4 + i] = grades[i];
        }
        params[34] = total;
        return params;
    }
}

