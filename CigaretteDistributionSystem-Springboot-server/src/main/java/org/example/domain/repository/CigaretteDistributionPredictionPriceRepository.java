package org.example.domain.repository;

import org.example.infrastructure.persistence.po.CigaretteDistributionPredictionPO;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * 预测价格表仓储接口（cigarette_distribution_prediction_price）
 */
public interface CigaretteDistributionPredictionPriceRepository {

    int upsert(CigaretteDistributionPredictionPO data);

    int batchUpsert(List<CigaretteDistributionPredictionPO> list);

    int updateOne(CigaretteDistributionPredictionPO data);

    int updateGrades(Integer year, Integer month, Integer weekSeq,
                    String cigCode, String cigName, String deliveryArea, BigDecimal[] grades);

    int deleteByCig(Integer year, Integer month, Integer weekSeq, String cigCode, String cigName);

    /**
     * 查询指定年月周的所有预测数据（价格分区表）
     *
     * @param year    年份
     * @param month   月份
     * @param weekSeq 周序号
     * @return 预测数据列表
     */
    List<Map<String, Object>> findAll(Integer year, Integer month, Integer weekSeq);
}

