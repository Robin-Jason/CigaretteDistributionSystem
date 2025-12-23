package org.example.infrastructure.persistence.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.example.infrastructure.persistence.po.CigaretteDistributionInfoPO;

import java.util.List;
import java.util.Map;

/**
 * 卷烟投放信息 Mapper（表：cigarette_distribution_info）
 * <p>用途：投放信息的通用 CRUD 与 UPSERT、定点更新。</p>
 * <p>特性：固定表名；SQL 存放在 resources/mapper/CigaretteDistributionInfoMapper.xml。</p>
 *
 * @author Robin
 * @version 1.0
 * @since 2025-12-10
 */
@Mapper
public interface CigaretteDistributionInfoMapper extends BaseMapper<CigaretteDistributionInfoPO> {

    /**
     * 单条 UPSERT（INSERT ... ON DUPLICATE KEY UPDATE）
     *
     * @param data 卷烟投放数据
     * @return 影响行数
     */
    int upsert(CigaretteDistributionInfoPO data);

    /**
     * 批量 UPSERT（多值 INSERT ... ON DUPLICATE KEY UPDATE）
     *
     * @param list 数据列表
     * @return 影响行数
     */
    int batchUpsert(@Param("list") List<CigaretteDistributionInfoPO> list);

    /**
     * 定点更新（按年月周 + 卷烟键）
     *
     * @param data 卷烟投放数据
     * @return 影响行数
     */
    int updateOne(CigaretteDistributionInfoPO data);

    /**
     * 查询指定分区的不重复投放组合。
     *
     * @param year    年份
     * @param month   月份
     * @param weekSeq 周序号
     * @return 列表，每条包含 DELIVERY_METHOD/DELIVERY_ETYPE/TAG
     */
    List<Map<String, Object>> findDistinctCombinations(@Param("year") Integer year,
                                                       @Param("month") Integer month,
                                                       @Param("weekSeq") Integer weekSeq);

    /**
     * 查询指定分区内“按价位段自选投放”卷烟的候选列表（含批发价）。
     *
     * @param year    年份
     * @param month   月份
     * @param weekSeq 周序号
     * @return 候选卷烟列表，每条包含 CIG_CODE、CIG_NAME、DELIVERY_AREA、DELIVERY_METHOD、DELIVERY_ETYPE、WHOLESALE_PRICE 等字段
     */
    List<Map<String, Object>> findPriceBandCandidates(@Param("year") Integer year,
                                                      @Param("month") Integer month,
                                                      @Param("weekSeq") Integer weekSeq);

    /**
     * 按卷烟代码和卷烟名称查询指定分区的投放信息。
     *
     * @param year    年份
     * @param month   月份
     * @param weekSeq 周序号
     * @param cigCode 卷烟代码
     * @param cigName 卷烟名称
     * @return 该卷烟的投放信息
     */
    Map<String, Object> findByCigCodeAndName(@Param("year") Integer year,
                                              @Param("month") Integer month,
                                              @Param("weekSeq") Integer weekSeq,
                                              @Param("cigCode") String cigCode,
                                              @Param("cigName") String cigName);

    /**
     * 更新指定卷烟的备注字段
     *
     * @param year    年份
     * @param month   月份
     * @param weekSeq 周序号
     * @param cigCode 卷烟代码
     * @param cigName 卷烟名称
     * @param remark  备注内容
     * @return 影响行数
     */
    int updateRemark(@Param("year") Integer year,
                     @Param("month") Integer month,
                     @Param("weekSeq") Integer weekSeq,
                     @Param("cigCode") String cigCode,
                     @Param("cigName") String cigName,
                     @Param("remark") String remark);
}


