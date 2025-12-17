package org.example.infrastructure.persistence.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.example.infrastructure.persistence.po.CigaretteDistributionPredictionPO;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * 卷烟分配预测表 Mapper（表：cigarette_distribution_prediction）
 * <p>用途：预测表的通用 CRUD 及 UPSERT、联表取 ADV、局部档位更新。</p>
 * <p>特性：固定表名，时间分区键需在调用方保证传入。</p>
 *
 * @author Robin
 * @version 1.0
 * @since 2025-12-10
 */
@Mapper
public interface CigaretteDistributionPredictionMapper extends BaseMapper<CigaretteDistributionPredictionPO> {

    /**
     * 单条 UPSERT（主键：YEAR/MONTH/WEEK_SEQ + CIG_CODE/CIG_NAME + DELIVERY_AREA）
     *
     * @param data 预测数据
     * @return 影响行数
     */
    int upsert(CigaretteDistributionPredictionPO data);

    /**
     * 批量 UPSERT（多值 INSERT ... ON DUPLICATE KEY UPDATE）
     *
     * @param list 预测数据列表
     * @return 影响行数
     */
    int batchUpsert(@Param("list") List<CigaretteDistributionPredictionPO> list);

    /**
     * 全字段定点更新（按年月周 + 卷烟 + 区域）
     *
     * @param data 预测数据
     * @return 影响行数
     */
    int updateOne(CigaretteDistributionPredictionPO data);

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
                     @Param("grades") BigDecimal[] grades);

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
     * 查询指定年月周的所有预测数据
     *
     * @param year 年
     * @param month 月
     * @param weekSeq 周序
     * @return 预测数据列表
     */
    List<Map<String, Object>> findAll(@Param("year") Integer year,
                                      @Param("month") Integer month,
                                      @Param("weekSeq") Integer weekSeq);

    /**
     * 查询指定年月周下某支卷烟的所有预测记录（按区域）。
     *
     * @param year    年
     * @param month   月
     * @param weekSeq 周序
     * @param cigCode 卷烟代码
     * @return 预测数据列表
     */
    List<Map<String, Object>> findByCigCode(@Param("year") Integer year,
                                            @Param("month") Integer month,
                                            @Param("weekSeq") Integer weekSeq,
                                            @Param("cigCode") String cigCode);

    /**
     * 统计指定年月周的记录数
     *
     * @param year 年
     * @param month 月
     * @param weekSeq 周序
     * @return 记录数
     */
    Long count(@Param("year") Integer year,
               @Param("month") Integer month,
               @Param("weekSeq") Integer weekSeq);

    /**
     * 删除指定年月周的所有预测数据
     *
     * @param year 年
     * @param month 月
     * @param weekSeq 周序
     * @return 删除的记录数
     */
    int deleteByYearMonthWeekSeq(@Param("year") Integer year,
                                 @Param("month") Integer month,
                                 @Param("weekSeq") Integer weekSeq);
}


