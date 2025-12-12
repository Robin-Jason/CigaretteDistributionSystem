package org.example.dao.impl;

import lombok.extern.slf4j.Slf4j;
import org.example.dao.CigaretteDistributionPredictionDAO;
import org.example.entity.CigaretteDistributionPredictionData;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import org.example.mapper.CigaretteDistributionPredictionMapper;
import org.example.util.PartitionTableManager;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * 卷烟分配预测表DAO实现类
 * 
 * @author System
 * @version 1.0
 * @since 2025-11-29
 */
@Slf4j
@Repository
public class CigaretteDistributionPredictionDAOImpl implements CigaretteDistributionPredictionDAO {
    
    private static final String TABLE_NAME = "cigarette_distribution_prediction";
    private static final int BATCH_SIZE = 1000;
    
    private final CigaretteDistributionPredictionMapper mapper;
    private final PartitionTableManager partitionTableManager;
    
    public CigaretteDistributionPredictionDAOImpl(CigaretteDistributionPredictionMapper mapper, PartitionTableManager partitionTableManager) {
        this.mapper = mapper;
        this.partitionTableManager = partitionTableManager;
    }
    
    @Override
    public List<Map<String, Object>> findAll(Integer year, Integer month, Integer weekSeq) {
        partitionTableManager.ensurePartitionExists(TABLE_NAME, year, month, weekSeq);
        
        QueryWrapper<CigaretteDistributionPredictionData> qw = new QueryWrapper<>();
        qw.eq("YEAR", year).eq("MONTH", month).eq("WEEK_SEQ", weekSeq)
                .orderByAsc("CIG_CODE", "CIG_NAME", "DELIVERY_AREA");
        return mapper.selectMaps(qw);
    }
    
    @Override
    public List<Map<String, Object>> findAllWithAdv(Integer year, Integer month, Integer weekSeq) {
        partitionTableManager.ensurePartitionExists(TABLE_NAME, year, month, weekSeq);
        partitionTableManager.ensurePartitionExists("cigarette_distribution_info", year, month, weekSeq);
        return mapper.findAllWithAdv(year, month, weekSeq);
    }
    
    @Override
    public List<Map<String, Object>> findByCigCodeAndName(Integer year, Integer month, Integer weekSeq, 
                                                          String cigCode, String cigName) {
        partitionTableManager.ensurePartitionExists(TABLE_NAME, year, month, weekSeq);
        
        QueryWrapper<CigaretteDistributionPredictionData> qw = new QueryWrapper<>();
        qw.eq("YEAR", year).eq("MONTH", month).eq("WEEK_SEQ", weekSeq)
                .eq("CIG_CODE", cigCode).eq("CIG_NAME", cigName)
                .orderByAsc("DELIVERY_AREA");
        return mapper.selectMaps(qw);
    }
    
    @Override
    public Map<String, Object> findByCigCodeNameAndArea(Integer year, Integer month, Integer weekSeq, 
                                                        String cigCode, String cigName, String deliveryArea) {
        partitionTableManager.ensurePartitionExists(TABLE_NAME, year, month, weekSeq);
        
        QueryWrapper<CigaretteDistributionPredictionData> qw = new QueryWrapper<>();
        qw.eq("YEAR", year).eq("MONTH", month).eq("WEEK_SEQ", weekSeq)
                .eq("CIG_CODE", cigCode).eq("CIG_NAME", cigName).eq("DELIVERY_AREA", deliveryArea)
                .last("LIMIT 1");
        List<Map<String, Object>> results = mapper.selectMaps(qw);
        return results.isEmpty() ? null : results.get(0);
    }
    
    @Override
    public int insert(Integer year, Integer month, Integer weekSeq, CigaretteDistributionPredictionData data) {
        partitionTableManager.ensurePartitionExists(TABLE_NAME, year, month, weekSeq);
        
        int count = mapper.upsert(data);
        log.debug("插入预测数据: {}-{}-{}, 卷烟: {} - {}, 区域: {}", year, month, weekSeq, 
                 data.getCigCode(), data.getCigName(), data.getDeliveryArea());
        return count;
    }
    
    @Override
    public int batchInsert(Integer year, Integer month, Integer weekSeq, 
                          List<CigaretteDistributionPredictionData> dataList) {
        if (dataList == null || dataList.isEmpty()) {
            return 0;
        }
        
        partitionTableManager.ensurePartitionExists(TABLE_NAME, year, month, weekSeq);
        
        int totalInserted = 0;
        int totalSize = dataList.size();
        
        for (int offset = 0; offset < totalSize; offset += BATCH_SIZE) {
            int endIndex = Math.min(offset + BATCH_SIZE, totalSize);
            List<CigaretteDistributionPredictionData> batch = dataList.subList(offset, endIndex);
            int count = mapper.batchUpsert(batch);
            totalInserted += count;
        }
        
        log.info("批量插入预测数据完成: {}-{}-{}, 插入 {} 条记录", year, month, weekSeq, totalInserted);
        return totalInserted;
    }
    
    @Override
    public int update(Integer year, Integer month, Integer weekSeq, CigaretteDistributionPredictionData data) {
        partitionTableManager.ensurePartitionExists(TABLE_NAME, year, month, weekSeq);
        
        data.setYear(year);
        data.setMonth(month);
        data.setWeekSeq(weekSeq);
        int count = mapper.updateOne(data);
        log.debug("更新预测数据: {}-{}-{}, 卷烟: {} - {}, 区域: {}", year, month, weekSeq, 
                 data.getCigCode(), data.getCigName(), data.getDeliveryArea());
        return count;
    }
    
    @Override
    public int deleteByCigCodeAndName(Integer year, Integer month, Integer weekSeq, 
                                      String cigCode, String cigName) {
        QueryWrapper<CigaretteDistributionPredictionData> qw = new QueryWrapper<>();
        qw.eq("YEAR", year).eq("MONTH", month).eq("WEEK_SEQ", weekSeq)
                .eq("CIG_CODE", cigCode).eq("CIG_NAME", cigName);
        int count = mapper.delete(qw);
        log.info("删除预测数据: {}-{}-{}, 卷烟: {} - {}, 删除 {} 条记录", 
                year, month, weekSeq, cigCode, cigName, count);
        return count;
    }
    
    @Override
    public int deleteByCigCodeNameAndArea(Integer year, Integer month, Integer weekSeq, 
                                          String cigCode, String cigName, String deliveryArea) {
        QueryWrapper<CigaretteDistributionPredictionData> qw = new QueryWrapper<>();
        qw.eq("YEAR", year).eq("MONTH", month).eq("WEEK_SEQ", weekSeq)
                .eq("CIG_CODE", cigCode).eq("CIG_NAME", cigName).eq("DELIVERY_AREA", deliveryArea);
        int count = mapper.delete(qw);
        log.debug("删除预测数据: {}-{}-{}, 卷烟: {} - {}, 区域: {}, 删除 {} 条记录", 
                 year, month, weekSeq, cigCode, cigName, deliveryArea, count);
        return count;
    }

    @Override
    public int updateGrades(Integer year, Integer month, Integer weekSeq,
                            String cigCode, String cigName, String deliveryArea,
                            BigDecimal[] grades) {
        if (grades == null || grades.length != 30) {
            throw new IllegalArgumentException("grades必须包含30个档位值");
        }
        partitionTableManager.ensurePartitionExists(TABLE_NAME, year, month, weekSeq);

        return mapper.updateGrades(year, month, weekSeq, cigCode, cigName, deliveryArea, grades);
    }
    
    @Override
    public int deleteByYearMonthWeekSeq(Integer year, Integer month, Integer weekSeq) {
        long recordCount = count(year, month, weekSeq);
        if (recordCount == 0) {
            log.info("deleteByYearMonthWeekSeq: {}-{}-{} 无数据，无需删除", year, month, weekSeq);
            return 0;
        }
        
        // 优先使用 TRUNCATE PARTITION，避免长事务
        boolean truncated = partitionTableManager.truncatePartition(TABLE_NAME, year, month, weekSeq);
        if (truncated) {
            int affected = recordCount > Integer.MAX_VALUE ? Integer.MAX_VALUE : (int) recordCount;
            log.info("通过 TRUNCATE PARTITION 清空 {}-{}-{}，估算删除 {} 条记录", year, month, weekSeq, recordCount);
            return affected;
        }
        
        QueryWrapper<CigaretteDistributionPredictionData> qw = new QueryWrapper<>();
        qw.eq("YEAR", year).eq("MONTH", month).eq("WEEK_SEQ", weekSeq);
        int count = mapper.delete(qw);
        log.info("删除预测数据完成: {}-{}-{}, 删除 {} 条记录", year, month, weekSeq, count);
        return count;
    }
    
    @Override
    public long count(Integer year, Integer month, Integer weekSeq) {
        QueryWrapper<CigaretteDistributionPredictionData> qw = new QueryWrapper<>();
        qw.eq("YEAR", year).eq("MONTH", month).eq("WEEK_SEQ", weekSeq);
        Long count = mapper.selectCount(qw);
        return count != null ? count : 0L;
    }
    
    @Override
    public boolean exists(Integer year, Integer month, Integer weekSeq) {
        return count(year, month, weekSeq) > 0;
    }
    
    @Override
    public int writeBackAllocationMatrix(Integer year, Integer month, Integer weekSeq,
                                        String cigCode, String cigName,
                                        List<CigaretteDistributionPredictionData> dataList) {
        if (dataList == null || dataList.isEmpty()) {
            log.warn("写回数据列表为空，卷烟: {} - {}", cigCode, cigName);
            return 0;
        }
        
        partitionTableManager.ensurePartitionExists(TABLE_NAME, year, month, weekSeq);
        
        long startTime = System.currentTimeMillis();
        try {
            // 第1步：删除该卷烟的所有现有记录（按卷烟覆盖逻辑）
            int deletedCount = deleteByCigCodeAndName(year, month, weekSeq, cigCode, cigName);
            log.debug("删除卷烟 {} - {} 的旧数据: {} 条", cigCode, cigName, deletedCount);
            
            // 第2步：批量插入新数据
            int insertedCount = batchInsert(year, month, weekSeq, dataList);
            log.info("写回分配矩阵完成: 卷烟 {} - {}, 删除: {} 条, 插入: {} 条, 耗时: {}ms", 
                    cigCode, cigName, deletedCount, insertedCount, 
                    System.currentTimeMillis() - startTime);
            
            return insertedCount;
        } catch (Exception e) {
            log.error("写回分配矩阵失败: 卷烟 {} - {}, 耗时: {}ms", 
                     cigCode, cigName, System.currentTimeMillis() - startTime, e);
            throw e;
        }
    }
    
}