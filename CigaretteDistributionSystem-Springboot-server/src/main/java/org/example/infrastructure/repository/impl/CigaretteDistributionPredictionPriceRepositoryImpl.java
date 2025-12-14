package org.example.infrastructure.repository.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.domain.repository.CigaretteDistributionPredictionPriceRepository;
import org.example.infrastructure.persistence.po.CigaretteDistributionPredictionPO;
import org.example.infrastructure.persistence.mapper.CigaretteDistributionPredictionPriceMapper;
import org.example.shared.util.PartitionTableManager;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * 基于 MyBatis-Plus Mapper 的预测价格表仓储实现。
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
public class CigaretteDistributionPredictionPriceRepositoryImpl implements CigaretteDistributionPredictionPriceRepository {

    private static final String TABLE_NAME = "cigarette_distribution_prediction_price";

    private final CigaretteDistributionPredictionPriceMapper predictionPriceMapper;
    private final PartitionTableManager partitionTableManager;

    @Override
    public int upsert(CigaretteDistributionPredictionPO data) {
        return predictionPriceMapper.upsert(data);
    }

    @Override
    public int updateOne(CigaretteDistributionPredictionPO data) {
        return predictionPriceMapper.updateOne(data);
    }

    @Override
    public int updateGrades(Integer year, Integer month, Integer weekSeq, String cigCode, String cigName, String deliveryArea, BigDecimal[] grades) {
        return predictionPriceMapper.updateGrades(year, month, weekSeq, cigCode, cigName, deliveryArea, grades);
    }

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
    @Override
    public int deleteByCig(Integer year, Integer month, Integer weekSeq, String cigCode, String cigName) {
        log.debug("删除预测价格数据: {}-{}-{}, 卷烟: {}-{}", year, month, weekSeq, cigCode, cigName);
        int count = predictionPriceMapper.deleteByCig(year, month, weekSeq, cigCode, cigName);
        log.info("删除预测价格数据完成: {}-{}-{}, 卷烟: {}-{}, 删除 {} 条记录", year, month, weekSeq, cigCode, cigName, count);
        return count;
    }

    /**
     * 查询指定年月周的所有预测数据（价格分区表）
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
        List<Map<String, Object>> result = predictionPriceMapper.findAll(year, month, weekSeq);
        log.debug("查询预测价格数据: {}-{}-{}, 返回 {} 条记录", year, month, weekSeq, result.size());
        return result;
    }

    @Override
    public int batchUpsert(List<CigaretteDistributionPredictionPO> list) {
        if (list == null || list.isEmpty()) {
            log.debug("批量UPSERT预测价格数据: 记录列表为空");
            return 0;
        }

        // 从第一条记录获取年月周，确保分区存在
        CigaretteDistributionPredictionPO first = list.get(0);
        if (first.getYear() != null && first.getMonth() != null && first.getWeekSeq() != null) {
            partitionTableManager.ensurePartitionExists(TABLE_NAME, first.getYear(), first.getMonth(), first.getWeekSeq());
        }

        int count = predictionPriceMapper.batchUpsert(list);
        log.info("批量UPSERT预测价格数据完成, 插入 {} 条记录", count);
        return count;
    }
}

