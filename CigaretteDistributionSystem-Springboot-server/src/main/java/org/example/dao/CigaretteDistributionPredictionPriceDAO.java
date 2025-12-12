package org.example.dao;

import org.example.entity.CigaretteDistributionPredictionData;

import java.util.List;
import java.util.Map;

/**
 * 按价位段自选投放预测分区表 DAO。
 */
public interface CigaretteDistributionPredictionPriceDAO {

    /**
     * 查询指定年月周的所有预测数据（价格分区表）。
     */
    List<Map<String, Object>> findAll(Integer year, Integer month, Integer weekSeq);

    /**
     * 写回分配矩阵数据到价格分区表（按卷烟覆盖逻辑）。
     * 先删除该卷烟的所有现有记录，再批量插入新数据。
     */
    int writeBackAllocationMatrix(Integer year, Integer month, Integer weekSeq,
                                  String cigCode, String cigName,
                                  List<CigaretteDistributionPredictionData> dataList);
}

