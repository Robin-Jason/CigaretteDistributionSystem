package org.example.infrastructure.repository.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.domain.repository.CigaretteDistributionInfoRepository;
import org.example.infrastructure.persistence.po.CigaretteDistributionInfoPO;
import org.example.infrastructure.persistence.mapper.CigaretteDistributionInfoMapper;
import org.example.shared.util.PartitionTableManager;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;

/**
 * 基于 MyBatis-Plus Mapper 的投放信息仓储实现。
 * <p>
 * 负责封装分区表管理、日志记录等横切关注点。
 * </p>
 *
 * @author Robin
 * @version 1.0
 * @since 2025-12-12
 */
@Slf4j
@Repository
@RequiredArgsConstructor
public class CigaretteDistributionInfoRepositoryImpl implements CigaretteDistributionInfoRepository {

    private static final String TABLE_NAME = "cigarette_distribution_info";

    private final CigaretteDistributionInfoMapper cigaretteDistributionInfoMapper;
    private final PartitionTableManager partitionTableManager;

    /**
     * 根据查询条件获取卷烟投放信息列表
     *
     * @param queryWrapper 查询条件封装对象
     * @return 匹配的卷烟投放信息列表，以 Map 形式返回
     */
    @Override
    public List<Map<String, Object>> selectMaps(QueryWrapper<CigaretteDistributionInfoPO> queryWrapper) {
        // 如果查询条件包含年月周，确保分区存在
        Object year = queryWrapper.getEntity() != null ? queryWrapper.getEntity().getYear() : null;
        Object month = queryWrapper.getEntity() != null ? queryWrapper.getEntity().getMonth() : null;
        Object weekSeq = queryWrapper.getEntity() != null ? queryWrapper.getEntity().getWeekSeq() : null;
        if (year != null && month != null && weekSeq != null) {
            partitionTableManager.ensurePartitionExists(TABLE_NAME, (Integer) year, (Integer) month, (Integer) weekSeq);
        }
        return cigaretteDistributionInfoMapper.selectMaps(queryWrapper);
    }

    /**
     * 根据查询条件删除卷烟投放信息
     *
     * @param queryWrapper 查询条件封装对象
     * @return 删除的行数
     */
    @Override
    public int delete(QueryWrapper<CigaretteDistributionInfoPO> queryWrapper) {
        log.debug("删除卷烟投放信息");
        int count = cigaretteDistributionInfoMapper.delete(queryWrapper);
        log.info("删除卷烟投放信息完成, 删除 {} 条记录", count);
        return count;
    }

    /**
     * 批量 UPSERT 卷烟投放信息数据
     *
     * @param list 待 UPSERT 的数据列表
     * @return 影响的行数
     */
    @Override
    public int batchUpsert(List<CigaretteDistributionInfoPO> list) {
        if (list == null || list.isEmpty()) {
            log.debug("批量UPSERT卷烟投放信息: 记录列表为空");
            return 0;
        }

        // 从第一条记录获取年月周，确保分区存在
        CigaretteDistributionInfoPO first = list.get(0);
        if (first.getYear() != null && first.getMonth() != null && first.getWeekSeq() != null) {
            partitionTableManager.ensurePartitionExists(TABLE_NAME, first.getYear(), first.getMonth(), first.getWeekSeq());
        }

        int count = cigaretteDistributionInfoMapper.batchUpsert(list);
        log.info("批量UPSERT卷烟投放信息完成, 插入 {} 条记录", count);
        return count;
    }

    /**
     * 查询指定分区的不重复投放组合
     *
     * @param year    年份
     * @param month   月份
     * @param weekSeq 周序号
     * @return 列表，每条包含 DELIVERY_METHOD/DELIVERY_ETYPE/TAG
     */
    @Override
    public List<Map<String, Object>> findDistinctCombinations(Integer year, Integer month, Integer weekSeq) {
        // 确保分区存在
        partitionTableManager.ensurePartitionExists(TABLE_NAME, year, month, weekSeq);
        List<Map<String, Object>> result = cigaretteDistributionInfoMapper.findDistinctCombinations(year, month, weekSeq);
        log.debug("查询不重复投放组合: {}-{}-{}, 返回 {} 条记录", year, month, weekSeq, result.size());
        return result;
    }

    /**
     * 查询指定分区内“按价位段自选投放”卷烟的候选列表（含批发价）。
     *
     * @param year    年份
     * @param month   月份
     * @param weekSeq 周序号
     * @return 候选卷烟列表，每条至少包含 CIG_CODE、CIG_NAME、DELIVERY_AREA、DELIVERY_METHOD、DELIVERY_ETYPE、WHOLESALE_PRICE 等字段
     */
    @Override
    public List<Map<String, Object>> findPriceBandCandidates(Integer year, Integer month, Integer weekSeq) {
        partitionTableManager.ensurePartitionExists(TABLE_NAME, year, month, weekSeq);
        List<Map<String, Object>> result = cigaretteDistributionInfoMapper.findPriceBandCandidates(year, month, weekSeq);
        log.debug("查询价位段自选投放候选卷烟: {}-{}-{}, 返回 {} 条记录", year, month, weekSeq, result.size());
        return result;
    }

    /**
     * 按卷烟代码和卷烟名称查询指定分区的投放信息。
     *
     * @param year    年份
     * @param month   月份
     * @param weekSeq 周序号
     * @param cigCode 卷烟代码
     * @param cigName 卷烟名称
     * @return 该卷烟的投放信息，不存在时返回 null
     */
    @Override
    public Map<String, Object> findByCigCodeAndName(Integer year, Integer month, Integer weekSeq, String cigCode, String cigName) {
        partitionTableManager.ensurePartitionExists(TABLE_NAME, year, month, weekSeq);
        Map<String, Object> result = cigaretteDistributionInfoMapper.findByCigCodeAndName(year, month, weekSeq, cigCode, cigName);
        log.debug("按卷烟代码和名称查询投放信息: {}-{}-{}, cigCode={}, cigName={}, 结果={}", 
                year, month, weekSeq, cigCode, cigName, result != null ? "找到" : "未找到");
        return result;
    }

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
    @Override
    public int updateRemark(Integer year, Integer month, Integer weekSeq, String cigCode, String cigName, String remark) {
        log.debug("更新卷烟备注: {}-{}-{}, 卷烟: {}-{}, 备注: {}", year, month, weekSeq, cigCode, cigName, remark);
        int count = cigaretteDistributionInfoMapper.updateRemark(year, month, weekSeq, cigCode, cigName, remark);
        log.info("更新卷烟备注完成: {}-{}-{}, 卷烟: {}-{}, 影响 {} 条记录", year, month, weekSeq, cigCode, cigName, count);
        return count;
    }
}

