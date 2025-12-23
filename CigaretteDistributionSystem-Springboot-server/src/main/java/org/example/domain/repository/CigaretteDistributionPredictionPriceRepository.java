package org.example.domain.repository;

import org.example.infrastructure.persistence.po.CigaretteDistributionPredictionPO;
import org.example.infrastructure.persistence.po.CigaretteDistributionPredictionPricePO;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * 预测价格表仓储接口（cigarette_distribution_prediction_price）
 */
public interface CigaretteDistributionPredictionPriceRepository {

    int upsert(CigaretteDistributionPredictionPricePO data);

    /**
     * 批量 UPSERT
     * <p>接受 CigaretteDistributionPredictionPO 及其子类（如 CigaretteDistributionPredictionPricePO）</p>
     */
    int batchUpsert(List<? extends CigaretteDistributionPredictionPO> list);

    int updateOne(CigaretteDistributionPredictionPricePO data);

    int updateGrades(Integer year, Integer month, Integer weekSeq,
                    String cigCode, String cigName, String deliveryArea, BigDecimal[] grades);

    int deleteByCig(Integer year, Integer month, Integer weekSeq, String cigCode, String cigName);

    /**
     * 按投放方式删除指定分区的记录
     *
     * @param year          年份
     * @param month         月份
     * @param weekSeq       周序号
     * @param deliveryMethod 投放方式
     * @return 删除行数
     */
    int deleteByDeliveryMethod(Integer year, Integer month, Integer weekSeq, String deliveryMethod);

    /**
     * 查询指定年月周的所有预测数据（价格分区表）
     *
     * @param year    年份
     * @param month   月份
     * @param weekSeq 周序号
     * @return 预测数据列表
     */
    List<Map<String, Object>> findAll(Integer year, Integer month, Integer weekSeq);

    /**
     * 删除指定卷烟的特定区域记录
     *
     * @param year         年份
     * @param month        月份
     * @param weekSeq      周序号
     * @param cigCode      卷烟代码
     * @param cigName      卷烟名称
     * @param deliveryArea 投放区域
     * @return 删除行数
     */
    int deleteByDeliveryArea(Integer year, Integer month, Integer weekSeq, String cigCode, String cigName, String deliveryArea);

    /**
     * 统计指定卷烟的区域记录数
     *
     * @param year    年份
     * @param month   月份
     * @param weekSeq 周序号
     * @param cigCode 卷烟代码
     * @param cigName 卷烟名称
     * @return 区域记录数
     */
    int countByCigarette(Integer year, Integer month, Integer weekSeq, String cigCode, String cigName);

    /**
     * 查询指定分区内按价位段自选投放的分配结果（用于订购量上限统计）
     *
     * @param year    年份
     * @param month   月份
     * @param weekSeq 周序号
     * @return 分配结果列表，包含 PRICE_BAND 和 D30~D1 字段
     */
    List<Map<String, Object>> findPriceBandAllocations(Integer year, Integer month, Integer weekSeq);

    /**
     * 查询指定分区的不重复投放组合
     *
     * @param year    年份
     * @param month   月份
     * @param weekSeq 周序号
     * @return 投放组合列表，包含 DELIVERY_METHOD、DELIVERY_ETYPE、TAG 字段
     */
    List<Map<String, Object>> findDistinctCombinations(Integer year, Integer month, Integer weekSeq);
}

