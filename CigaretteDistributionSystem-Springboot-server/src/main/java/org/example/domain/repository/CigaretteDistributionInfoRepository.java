package org.example.domain.repository;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import org.example.infrastructure.persistence.po.CigaretteDistributionInfoPO;

import java.util.List;
import java.util.Map;

/**
 * 投放信息表仓储接口（cigarette_distribution_info）
 */
public interface CigaretteDistributionInfoRepository {

    List<Map<String, Object>> selectMaps(QueryWrapper<CigaretteDistributionInfoPO> queryWrapper);

    int delete(QueryWrapper<CigaretteDistributionInfoPO> queryWrapper);

    /**
     * 批量 UPSERT 卷烟投放信息数据
     *
     * @param list 待 UPSERT 的数据列表
     * @return 影响的行数
     */
    int batchUpsert(List<CigaretteDistributionInfoPO> list);

    /**
     * 查询指定分区的不重复投放组合
     *
     * @param year    年份
     * @param month   月份
     * @param weekSeq 周序号
     * @return 列表，每条包含 DELIVERY_METHOD/DELIVERY_ETYPE/TAG
     */
    List<Map<String, Object>> findDistinctCombinations(Integer year, Integer month, Integer weekSeq);

    /**
     * 查询指定分区内“按价位段自选投放”卷烟的候选列表（含批发价）。
     *
     * @param year    年份
     * @param month   月份
     * @param weekSeq 周序号
     * @return 候选卷烟列表，每条至少包含 CIG_CODE、CIG_NAME、DELIVERY_AREA、DELIVERY_METHOD、DELIVERY_ETYPE、WHOLESALE_PRICE 等字段
     */
    List<Map<String, Object>> findPriceBandCandidates(Integer year, Integer month, Integer weekSeq);
}

