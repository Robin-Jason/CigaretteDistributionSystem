package org.example.infrastructure.repository.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.domain.repository.CigaretteDistributionPredictionRepository;
import org.example.infrastructure.persistence.po.CigaretteDistributionPredictionPO;
import org.example.infrastructure.persistence.mapper.CigaretteDistributionPredictionMapper;
import org.example.shared.util.PartitionTableManager;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * 基于 MyBatis-Plus Mapper 的预测表仓储实现。
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
public class CigaretteDistributionPredictionRepositoryImpl implements CigaretteDistributionPredictionRepository {

    private static final String TABLE_NAME = "cigarette_distribution_prediction";

    private final CigaretteDistributionPredictionMapper predictionMapper;
    private final PartitionTableManager partitionTableManager;

    @Override
    public int upsert(CigaretteDistributionPredictionPO data) {
        return predictionMapper.upsert(data);
    }

    @Override
    public int batchUpsert(List<CigaretteDistributionPredictionPO> list) {
        return predictionMapper.batchUpsert(list);
    }

    @Override
    public int updateOne(CigaretteDistributionPredictionPO data) {
        return predictionMapper.updateOne(data);
    }

    @Override
    public int updateGrades(Integer year, Integer month, Integer weekSeq, String cigCode, String cigName, String deliveryArea, BigDecimal[] grades) {
        return predictionMapper.updateGrades(year, month, weekSeq, cigCode, cigName, deliveryArea, grades);
    }

    @Override
    public List<Map<String, Object>> findAllWithAdv(Integer year, Integer month, Integer weekSeq) {
        return predictionMapper.findAllWithAdv(year, month, weekSeq);
    }

    @Override
    public List<Map<String, Object>> findByCigCode(Integer year, Integer month, Integer weekSeq, String cigCode) {
        partitionTableManager.ensurePartitionExists(TABLE_NAME, year, month, weekSeq);
        List<Map<String, Object>> result = predictionMapper.findByCigCode(year, month, weekSeq, cigCode);
        log.debug("查询卷烟预测数据: {}-{}-{}, cigCode={}, 返回 {} 条记录", year, month, weekSeq, cigCode, result.size());
        return result;
    }

    @Override
    public int delete(QueryWrapper<CigaretteDistributionPredictionPO> queryWrapper) {
        return predictionMapper.delete(queryWrapper);
    }

    /**
     * 查询指定年月周的所有预测数据
     *
     * @param year    年份
     * @param month   月份
     * @param weekSeq 周序号
     * @return 预测数据列表
     */
    @Override
    public List<Map<String, Object>> findAll(Integer year, Integer month, Integer weekSeq) {
        // 确保分区存在
        partitionTableManager.ensurePartitionExists(TABLE_NAME, year, month, weekSeq);
        List<Map<String, Object>> result = predictionMapper.findAll(year, month, weekSeq);
        log.debug("查询预测数据: {}-{}-{}, 返回 {} 条记录", year, month, weekSeq, result.size());
        return result;
    }

    /**
     * 统计指定年月周的记录数
     *
     * @param year    年份
     * @param month   月份
     * @param weekSeq 周序号
     * @return 记录数
     */
    @Override
    public long count(Integer year, Integer month, Integer weekSeq) {
        // 确保分区存在
        partitionTableManager.ensurePartitionExists(TABLE_NAME, year, month, weekSeq);
        Long result = predictionMapper.count(year, month, weekSeq);
        return result != null ? result : 0L;
    }

    /**
     * 删除指定年月周的所有预测数据
     *
     * @param year    年份
     * @param month   月份
     * @param weekSeq 周序号
     * @return 删除的记录数
     */
    @Override
    public int deleteByYearMonthWeekSeq(Integer year, Integer month, Integer weekSeq) {
        log.debug("删除预测数据: {}-{}-{}", year, month, weekSeq);
        int count = predictionMapper.deleteByYearMonthWeekSeq(year, month, weekSeq);
        log.info("删除预测数据完成: {}-{}-{}, 删除 {} 条记录", year, month, weekSeq, count);
        return count;
    }
}

