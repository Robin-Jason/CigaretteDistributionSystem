package org.example.infrastructure.persistence.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.example.infrastructure.persistence.po.CigaretteDistributionPredictionPricePO;

import java.util.List;
import java.util.Map;

/**
 * 按价位段自选投放预测分区表 Mapper（表：cigarette_distribution_prediction_price）
 * <p>用途：查询、批量 UPSERT、按卷烟删除等操作，SQL 位于 resources/mapper/CigaretteDistributionPredictionPriceMapper.xml。</p>
 *
 * @author Robin
 * @version 1.0
 * @since 2025-12-10
 */
@Mapper
public interface CigaretteDistributionPredictionPriceMapper {

    /**
     * 查询指定分区的全部记录
     *
     * @param year    年
     * @param month   月
     * @param weekSeq 周序
     * @return 结果列表
     */
    List<Map<String, Object>> findAll(@Param("year") Integer year,
                                      @Param("month") Integer month,
                                      @Param("weekSeq") Integer weekSeq);

    /**
     * 单条 UPSERT（主键：YEAR/MONTH/WEEK_SEQ + CIG_CODE/CIG_NAME + DELIVERY_AREA）
     *
     * @param data 预测数据
     * @return 影响行数
     */
    int upsert(CigaretteDistributionPredictionPricePO data);

    /**
     * 批量 UPSERT（多值 INSERT ... ON DUPLICATE KEY UPDATE）
     * <p>接受 CigaretteDistributionPredictionPO 及其子类</p>
     *
     * @param list 数据列表
     * @return 影响行数
     */
    int batchUpsert(@Param("list") List<? extends org.example.infrastructure.persistence.po.CigaretteDistributionPredictionPO> list);

    /**
     * 全字段定点更新（按年月周 + 卷烟 + 区域）
     *
     * @param data 预测数据
     * @return 影响行数
     */
    int updateOne(CigaretteDistributionPredictionPricePO data);

    /**
     * 仅更新30个档位值（按年月周 + 卷烟 + 区域）
     *
     * @param year 年
     * @param month 月
     * @param weekSeq 周序
     * @param cigCode 卷烟代码
     * @param cigName 卷烟名称
     * @param deliveryArea 投放区域
     * @param grades 30档位值（D30-D1）
     * @return 影响行数
     */
    int updateGrades(@Param("year") Integer year,
                     @Param("month") Integer month,
                     @Param("weekSeq") Integer weekSeq,
                     @Param("cigCode") String cigCode,
                     @Param("cigName") String cigName,
                     @Param("deliveryArea") String deliveryArea,
                     @Param("grades") java.math.BigDecimal[] grades);

    /**
     * 联表 info 表，补充 ADV 字段
     *
     * @param year 年
     * @param month 月
     * @param weekSeq 周序
     * @return 结果列表（含 ADV）
     */
    List<Map<String, Object>> findAllWithAdv(@Param("year") Integer year,
                                             @Param("month") Integer month,
                                             @Param("weekSeq") Integer weekSeq);

    /**
     * 按卷烟删除指定分区的记录
     *
     * @param year    年
     * @param month   月
     * @param weekSeq 周序
     * @param cigCode 卷烟代码
     * @param cigName 卷烟名称
     * @return 删除行数
     */
    int deleteByCig(@Param("year") Integer year,
                    @Param("month") Integer month,
                    @Param("weekSeq") Integer weekSeq,
                    @Param("cigCode") String cigCode,
                    @Param("cigName") String cigName);

    /**
     * 按投放方式删除指定分区的记录
     *
     * @param year           年
     * @param month          月
     * @param weekSeq        周序
     * @param deliveryMethod 投放方式
     * @return 删除行数
     */
    int deleteByDeliveryMethod(@Param("year") Integer year,
                               @Param("month") Integer month,
                               @Param("weekSeq") Integer weekSeq,
                               @Param("deliveryMethod") String deliveryMethod);

    /**
     * 删除指定卷烟的特定区域记录
     *
     * @param year         年
     * @param month        月
     * @param weekSeq      周序
     * @param cigCode      卷烟代码
     * @param cigName      卷烟名称
     * @param deliveryArea 投放区域
     * @return 删除行数
     */
    int deleteByDeliveryArea(@Param("year") Integer year,
                             @Param("month") Integer month,
                             @Param("weekSeq") Integer weekSeq,
                             @Param("cigCode") String cigCode,
                             @Param("cigName") String cigName,
                             @Param("deliveryArea") String deliveryArea);

    /**
     * 统计指定卷烟的区域记录数
     *
     * @param year    年
     * @param month   月
     * @param weekSeq 周序
     * @param cigCode 卷烟代码
     * @param cigName 卷烟名称
     * @return 区域记录数
     */
    int countByCigarette(@Param("year") Integer year,
                         @Param("month") Integer month,
                         @Param("weekSeq") Integer weekSeq,
                         @Param("cigCode") String cigCode,
                         @Param("cigName") String cigName);

    /**
     * 查询指定分区内按价位段自选投放的分配结果（联表获取价位段信息）
     *
     * @param year    年
     * @param month   月
     * @param weekSeq 周序
     * @return 分配结果列表，包含 PRICE_BAND 和 D30~D1 字段
     */
    List<Map<String, Object>> findPriceBandAllocations(@Param("year") Integer year,
                                                       @Param("month") Integer month,
                                                       @Param("weekSeq") Integer weekSeq);

    /**
     * 查询指定分区的不重复投放组合
     *
     * @param year    年
     * @param month   月
     * @param weekSeq 周序
     * @return 投放组合列表，包含 DELIVERY_METHOD、DELIVERY_ETYPE、TAG 字段
     */
    List<Map<String, Object>> findDistinctCombinations(@Param("year") Integer year,
                                                       @Param("month") Integer month,
                                                       @Param("weekSeq") Integer weekSeq);
}


