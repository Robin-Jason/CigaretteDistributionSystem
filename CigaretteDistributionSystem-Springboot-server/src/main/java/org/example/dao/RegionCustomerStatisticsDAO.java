package org.example.dao;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * 区域客户统计表DAO接口
 * 
 * 封装对region_customer_statistics分区表的所有数据库操作
 * 
 * @author System
 * @version 1.0
 * @since 2025-11-29
 */
public interface RegionCustomerStatisticsDAO {
    
    /**
     * 查询指定年月周的所有区域客户统计
     * 
     * @param year 年份
     * @param month 月份
     * @param weekSeq 周序号
     * @return 区域客户统计列表，每个Map包含region、D30-D1、TOTAL字段
     */
    List<Map<String, Object>> findAll(Integer year, Integer month, Integer weekSeq);
    
    /**
     * 查询指定区域的客户统计
     * 
     * @param year 年份
     * @param month 月份
     * @param weekSeq 周序号
     * @param region 区域名称
     * @return 客户统计Map，包含D30-D1、TOTAL字段，如果不存在返回null
     */
    Map<String, Object> findByRegion(Integer year, Integer month, Integer weekSeq, String region);
    
    /**
     * 查询指定区域的30个档位客户数
     * 
     * @param year 年份
     * @param month 月份
     * @param weekSeq 周序号
     * @param region 区域名称
     * @return 30个档位的客户数数组（D30到D1），如果不存在返回null
     */
    BigDecimal[] findCustomerCountsByRegion(Integer year, Integer month, Integer weekSeq, String region);
    
    /**
     * 批量插入区域客户统计记录
     * 
     * @param year 年份
     * @param month 月份
     * @param weekSeq 周序号
     * @param records 区域客户记录列表，每个记录包含region和30个档位值
     * @return 插入的记录数
     */
    int batchInsert(Integer year, Integer month, Integer weekSeq, List<RegionCustomerRecord> records);
    
    /**
     * 删除指定年月周的所有区域客户统计
     * 
     * @param year 年份
     * @param month 月份
     * @param weekSeq 周序号
     * @return 删除的记录数
     */
    int deleteByYearMonthWeekSeq(Integer year, Integer month, Integer weekSeq);
    
    /**
     * 删除指定区域的客户统计
     * 
     * @param year 年份
     * @param month 月份
     * @param weekSeq 周序号
     * @param region 区域名称
     * @return 删除的记录数
     */
    int deleteByRegion(Integer year, Integer month, Integer weekSeq, String region);
    
    /**
     * 统计指定年月周的记录数
     * 
     * @param year 年份
     * @param month 月份
     * @param weekSeq 周序号
     * @return 记录数
     */
    long count(Integer year, Integer month, Integer weekSeq);
    
    /**
     * 检查指定年月周是否有数据
     * 
     * @param year 年份
     * @param month 月份
     * @param weekSeq 周序号
     * @return true如果有数据
     */
    boolean exists(Integer year, Integer month, Integer weekSeq);
    
    /**
     * 区域客户记录内部类
     */
    class RegionCustomerRecord {
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
         * 转换为SQL插入参数数组
         * 顺序：YEAR, MONTH, WEEK_SEQ, REGION, D30, D29, ..., D1, TOTAL
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
}

