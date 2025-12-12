package org.example.dao.impl;

import lombok.extern.slf4j.Slf4j;
import org.example.dao.CigaretteDistributionPredictionPriceDAO;
import org.example.entity.CigaretteDistributionPredictionData;
import org.example.mapper.CigaretteDistributionPredictionPriceMapper;
import org.example.util.PartitionTableManager;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;

/**
 * 按价位段自选投放预测分区表 DAO。
 */
@Slf4j
@Repository
public class CigaretteDistributionPredictionPriceDAOImpl implements CigaretteDistributionPredictionPriceDAO {

    private static final String TABLE_NAME = "cigarette_distribution_prediction_price";
    private static final int BATCH_SIZE = 1000;

    private final CigaretteDistributionPredictionPriceMapper mapper;
    private final PartitionTableManager partitionTableManager;

    public CigaretteDistributionPredictionPriceDAOImpl(CigaretteDistributionPredictionPriceMapper mapper,
                                                       PartitionTableManager partitionTableManager) {
        this.mapper = mapper;
        this.partitionTableManager = partitionTableManager;
    }

    @Override
    public int writeBackAllocationMatrix(Integer year, Integer month, Integer weekSeq,
                                         String cigCode, String cigName,
                                         List<CigaretteDistributionPredictionData> dataList) {
        if (dataList == null || dataList.isEmpty()) {
            log.warn("价位段写回数据列表为空，卷烟: {} - {}", cigCode, cigName);
            return 0;
        }

        partitionTableManager.ensurePartitionExists(TABLE_NAME, year, month, weekSeq);

        long startTime = System.currentTimeMillis();
        // 删除旧数据
        int deletedCount = mapper.deleteByCig(year, month, weekSeq, cigCode, cigName);

        // 批量插入（分批避免过大集合）
        int totalInserted = 0;
        int totalSize = dataList.size();
        for (int offset = 0; offset < totalSize; offset += BATCH_SIZE) {
            int endIndex = Math.min(offset + BATCH_SIZE, totalSize);
            List<CigaretteDistributionPredictionData> batch = dataList.subList(offset, endIndex);
            int inserted = mapper.batchUpsert(batch);
            totalInserted += inserted;
        }

        log.info("价位段写回完成: {}-{}-{}, 卷烟: {} - {}, 删除: {}, 插入: {}, 耗时: {}ms",
                year, month, weekSeq, cigCode, cigName, deletedCount, totalInserted, System.currentTimeMillis() - startTime);
        return totalInserted;
    }

    @Override
    public List<Map<String, Object>> findAll(Integer year, Integer month, Integer weekSeq) {
        partitionTableManager.ensurePartitionExists(TABLE_NAME, year, month, weekSeq);
        return mapper.findAll(year, month, weekSeq);
    }

    // 以下方法暂未在旧调用链使用，可按需暴露以对齐 PredictionMapper 能力

    public int upsert(CigaretteDistributionPredictionData data) {
        return mapper.upsert(data);
    }

    public int updateOne(CigaretteDistributionPredictionData data) {
        return mapper.updateOne(data);
    }

    public int updateGrades(Integer year, Integer month, Integer weekSeq,
                            String cigCode, String cigName, String deliveryArea,
                            java.math.BigDecimal[] grades) {
        return mapper.updateGrades(year, month, weekSeq, cigCode, cigName, deliveryArea, grades);
    }

    public List<Map<String, Object>> findAllWithAdv(Integer year, Integer month, Integer weekSeq) {
        partitionTableManager.ensurePartitionExists(TABLE_NAME, year, month, weekSeq);
        partitionTableManager.ensurePartitionExists("cigarette_distribution_info", year, month, weekSeq);
        return mapper.findAllWithAdv(year, month, weekSeq);
    }
}

