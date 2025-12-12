package org.example.dao.impl;

import lombok.extern.slf4j.Slf4j;
import org.example.dao.RegionCustomerStatisticsDAO;
import org.example.mapper.RegionCustomerStatisticsMapper;
import org.example.util.PartitionTableManager;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * 区域客户统计表DAO实现类
 * 
 * @author System
 * @version 1.0
 * @since 2025-11-29
 */
@Slf4j
@Repository
public class RegionCustomerStatisticsDAOImpl implements RegionCustomerStatisticsDAO {
    
    private static final String TABLE_NAME = "region_customer_statistics";
    private static final int BATCH_SIZE = 1000;
    
    // 30个档位名称（D30到D1）
    private static final String[] GRADE_NAMES = {
        "D30", "D29", "D28", "D27", "D26", "D25", "D24", "D23", "D22", "D21",
        "D20", "D19", "D18", "D17", "D16", "D15", "D14", "D13", "D12", "D11",
        "D10", "D9", "D8", "D7", "D6", "D5", "D4", "D3", "D2", "D1"
    };
    
    private final RegionCustomerStatisticsMapper mapper;
    private final PartitionTableManager partitionTableManager;
    
    public RegionCustomerStatisticsDAOImpl(RegionCustomerStatisticsMapper mapper,
                                           PartitionTableManager partitionTableManager) {
        this.mapper = mapper;
        this.partitionTableManager = partitionTableManager;
    }
    
    @Override
    public List<Map<String, Object>> findAll(Integer year, Integer month, Integer weekSeq) {
        // 确保分区存在
        partitionTableManager.ensurePartitionExists(TABLE_NAME, year, month, weekSeq);
        
        return mapper.findAll(year, month, weekSeq);
    }
    
    @Override
    public Map<String, Object> findByRegion(Integer year, Integer month, Integer weekSeq, String region) {
        // 确保分区存在
        partitionTableManager.ensurePartitionExists(TABLE_NAME, year, month, weekSeq);
        
        return mapper.findByRegion(year, month, weekSeq, region);
    }
    
    @Override
    public BigDecimal[] findCustomerCountsByRegion(Integer year, Integer month, Integer weekSeq, String region) {
        Map<String, Object> row = findByRegion(year, month, weekSeq, region);
        
        if (row == null) {
            return null;
        }
        
        BigDecimal[] customerCounts = new BigDecimal[30];
        for (int i = 0; i < 30; i++) {
            String fieldName = GRADE_NAMES[i];
            Object value = row.get(fieldName);
            
            if (value != null) {
                if (value instanceof BigDecimal) {
                    customerCounts[i] = (BigDecimal) value;
                } else {
                    customerCounts[i] = new BigDecimal(value.toString());
                }
            } else {
                customerCounts[i] = BigDecimal.ZERO;
            }
        }
        
        return customerCounts;
    }
    
    @Override
    public int batchInsert(Integer year, Integer month, Integer weekSeq, List<RegionCustomerRecord> records) {
        if (records == null || records.isEmpty()) {
            return 0;
        }
        
        // 确保分区存在
        partitionTableManager.ensurePartitionExists(TABLE_NAME, year, month, weekSeq);
        
        int totalInserted = 0;
        int totalSize = records.size();
        for (int offset = 0; offset < totalSize; offset += BATCH_SIZE) {
            int endIndex = Math.min(offset + BATCH_SIZE, totalSize);
            List<RegionCustomerRecord> batch = records.subList(offset, endIndex);
            int count = mapper.batchUpsert(year, month, weekSeq, batch);
            totalInserted += count;
                }
        log.info("批量插入区域客户统计完成: {}-{}-{}, 插入 {} 条记录", year, month, weekSeq, totalInserted);
        return totalInserted;
    }
    
    @Override
    public int deleteByYearMonthWeekSeq(Integer year, Integer month, Integer weekSeq) {
        int count = mapper.deleteByYearMonthWeekSeq(year, month, weekSeq);
        log.info("删除区域客户统计完成: {}-{}-{}, 删除 {} 条记录", year, month, weekSeq, count);
        return count;
    }
    
    @Override
    public int deleteByRegion(Integer year, Integer month, Integer weekSeq, String region) {
        int count = mapper.deleteByRegion(year, month, weekSeq, region);
        log.debug("删除区域客户统计完成: {}-{}-{}, 区域: {}, 删除 {} 条记录", year, month, weekSeq, region, count);
        return count;
    }
    
    @Override
    public long count(Integer year, Integer month, Integer weekSeq) {
        Long count = mapper.count(year, month, weekSeq);
        return count != null ? count : 0L;
    }
    
    @Override
    public boolean exists(Integer year, Integer month, Integer weekSeq) {
        return count(year, month, weekSeq) > 0;
    }
}

