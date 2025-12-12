package org.example.dao;

import org.example.entity.CigaretteDistributionPredictionData;
import java.util.List;
import java.util.Map;
import java.math.BigDecimal;

/**
 * 卷烟分配预测表DAO接口
 * 
 * 封装对cigarette_distribution_prediction分区表的所有数据库操作
 * 
 * @author System
 * @version 1.0
 * @since 2025-11-29
 */
public interface CigaretteDistributionPredictionDAO {
    
    /**
     * 查询指定年月周的所有预测数据
     * 
     * @param year 年份
     * @param month 月份
     * @param weekSeq 周序号
     * @return 预测数据列表
     */
    List<Map<String, Object>> findAll(Integer year, Integer month, Integer weekSeq);
    
    /**
     * 查询指定卷烟的预测数据
     * 
     * @param year 年份
     * @param month 月份
     * @param weekSeq 周序号
     * @param cigCode 卷烟代码
     * @param cigName 卷烟名称
     * @return 预测数据列表
     */
    List<Map<String, Object>> findByCigCodeAndName(Integer year, Integer month, Integer weekSeq, 
                                                    String cigCode, String cigName);
    
    /**
     * 查询指定卷烟和区域的预测数据
     * 
     * @param year 年份
     * @param month 月份
     * @param weekSeq 周序号
     * @param cigCode 卷烟代码
     * @param cigName 卷烟名称
     * @param deliveryArea 投放区域
     * @return 预测数据，如果不存在返回null
     */
    Map<String, Object> findByCigCodeNameAndArea(Integer year, Integer month, Integer weekSeq, 
                                                  String cigCode, String cigName, String deliveryArea);

    /**
     * 联表基础信息表，返回预测数据并附带预投放量 ADV（同时间分区）。
     */
    List<Map<String, Object>> findAllWithAdv(Integer year, Integer month, Integer weekSeq);
    
    /**
     * 插入预测数据
     * 
     * @param year 年份
     * @param month 月份
     * @param weekSeq 周序号
     * @param data 预测数据
     * @return 插入的记录数
     */
    int insert(Integer year, Integer month, Integer weekSeq, CigaretteDistributionPredictionData data);
    
    /**
     * 批量插入预测数据
     * 
     * @param year 年份
     * @param month 月份
     * @param weekSeq 周序号
     * @param dataList 预测数据列表
     * @return 插入的记录数
     */
    int batchInsert(Integer year, Integer month, Integer weekSeq, List<CigaretteDistributionPredictionData> dataList);
    
    /**
     * 更新预测数据
     * 
     * @param year 年份
     * @param month 月份
     * @param weekSeq 周序号
     * @param data 预测数据
     * @return 更新的记录数
     */
    int update(Integer year, Integer month, Integer weekSeq, CigaretteDistributionPredictionData data);
    
    /**
     * 删除指定卷烟的预测数据
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
     * 删除指定卷烟和区域的预测数据
     * 
     * @param year 年份
     * @param month 月份
     * @param weekSeq 周序号
     * @param cigCode 卷烟代码
     * @param cigName 卷烟名称
     * @param deliveryArea 投放区域
     * @return 删除的记录数
     */
    int deleteByCigCodeNameAndArea(Integer year, Integer month, Integer weekSeq, 
                                   String cigCode, String cigName, String deliveryArea);

    /**
     * 仅更新指定卷烟区域的30个档位值。
     */
    int updateGrades(Integer year, Integer month, Integer weekSeq,
                     String cigCode, String cigName, String deliveryArea,
                     BigDecimal[] grades);
    
    /**
     * 删除指定年月周的所有预测数据
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
    
    /**
     * 写回分配矩阵数据到数据库（按卷烟覆盖逻辑）
     * 先删除该卷烟的所有现有记录，再批量插入新数据
     * 
     * @param year 年份
     * @param month 月份
     * @param weekSeq 周序号
     * @param cigCode 卷烟代码
     * @param cigName 卷烟名称
     * @param dataList 预测数据列表
     * @return 写入的记录数
     */
    int writeBackAllocationMatrix(Integer year, Integer month, Integer weekSeq,
                                  String cigCode, String cigName,
                                  List<CigaretteDistributionPredictionData> dataList);
}

