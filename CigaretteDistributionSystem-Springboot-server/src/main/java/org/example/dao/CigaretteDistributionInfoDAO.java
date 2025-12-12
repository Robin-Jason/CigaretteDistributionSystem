package org.example.dao;

import org.example.entity.CigaretteDistributionInfoData;
import java.util.List;
import java.util.Map;

/**
 * 卷烟投放信息表DAO接口
 * 
 * 封装对cigarette_distribution_info分区表的所有数据库操作
 * 
 * @author System
 * @version 1.0
 * @since 2025-11-29
 */
public interface CigaretteDistributionInfoDAO {
    
    /**
     * 查询指定年月周的所有卷烟投放信息
     * 
     * @param year 年份
     * @param month 月份
     * @param weekSeq 周序号
     * @return 卷烟投放信息列表
     */
    List<Map<String, Object>> findAll(Integer year, Integer month, Integer weekSeq);
    
    /**
     * 查询指定卷烟的投放信息
     * 
     * @param year 年份
     * @param month 月份
     * @param weekSeq 周序号
     * @param cigCode 卷烟代码
     * @param cigName 卷烟名称
     * @return 卷烟投放信息列表
     */
    List<Map<String, Object>> findByCigCodeAndName(Integer year, Integer month, Integer weekSeq, 
                                                   String cigCode, String cigName);
    
    /**
     * 查询所有不重复的投放组合
     * 
     * @param year 年份
     * @param month 月份
     * @param weekSeq 周序号
     * @return 投放组合列表，每个Map包含DELIVERY_METHOD, DELIVERY_ETYPE, TAG
     */
    List<Map<String, Object>> findDistinctCombinations(Integer year, Integer month, Integer weekSeq);
    
    /**
     * 插入卷烟投放信息
     * 
     * @param year 年份
     * @param month 月份
     * @param weekSeq 周序号
     * @param data 卷烟投放信息数据
     * @return 插入的记录数
     */
    int insert(Integer year, Integer month, Integer weekSeq, CigaretteDistributionInfoData data);
    
    /**
     * 批量插入卷烟投放信息
     * 
     * @param year 年份
     * @param month 月份
     * @param weekSeq 周序号
     * @param dataList 卷烟投放信息数据列表
     * @return 插入的记录数
     */
    int batchInsert(Integer year, Integer month, Integer weekSeq, List<CigaretteDistributionInfoData> dataList);
    
    /**
     * 更新卷烟投放信息
     * 
     * @param year 年份
     * @param month 月份
     * @param weekSeq 周序号
     * @param data 卷烟投放信息数据
     * @return 更新的记录数
     */
    int update(Integer year, Integer month, Integer weekSeq, CigaretteDistributionInfoData data);
    
    /**
     * 删除指定卷烟的投放信息
     * 
     * @param year 年份
     * @param month 月份
     * @param weekSeq 周序号
     * @param cigCode 卷烟代码
     * @param cigName 卷烟名称
     * @return 删除的记录数
     */
    int deleteByCigCodeAndName(Integer year, Integer month, Integer weekSeq, 
                               String cigCode, String cigName);
    
    /**
     * 删除指定年月周的所有投放信息
     * 
     * @param year 年份
     * @param month 月份
     * @param weekSeq 周序号
     * @return 删除的记录数
     */
    int deleteByYearMonthWeekSeq(Integer year, Integer month, Integer weekSeq);
    
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
}

