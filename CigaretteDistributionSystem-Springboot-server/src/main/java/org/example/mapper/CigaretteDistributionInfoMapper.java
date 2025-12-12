package org.example.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.example.entity.CigaretteDistributionInfoData;

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
public interface CigaretteDistributionInfoMapper extends BaseMapper<CigaretteDistributionInfoData> {

    /**
     * 单条 UPSERT（INSERT ... ON DUPLICATE KEY UPDATE）
     *
     * @param data 卷烟投放数据
     * @return 影响行数
     */
    int upsert(CigaretteDistributionInfoData data);

    /**
     * 批量 UPSERT（多值 INSERT ... ON DUPLICATE KEY UPDATE）
     *
     * @param list 数据列表
     * @return 影响行数
     */
    int batchUpsert(@Param("list") List<CigaretteDistributionInfoData> list);

    /**
     * 定点更新（按年月周 + 卷烟键）
     *
     * @param data 卷烟投放数据
     * @return 影响行数
     */
    int updateOne(CigaretteDistributionInfoData data);

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
}


