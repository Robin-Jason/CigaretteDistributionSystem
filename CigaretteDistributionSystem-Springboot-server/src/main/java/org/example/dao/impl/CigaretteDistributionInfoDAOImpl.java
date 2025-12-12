package org.example.dao.impl;

import lombok.extern.slf4j.Slf4j;
import org.example.dao.CigaretteDistributionInfoDAO;
import org.example.mapper.CigaretteDistributionInfoMapper;
import org.example.entity.CigaretteDistributionInfoData;
import org.example.util.PartitionTableManager;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;

/**
 * 卷烟投放信息表DAO实现类
 * 
 * @author System
 * @version 1.0
 * @since 2025-11-29
 */
@Slf4j
@Repository
public class CigaretteDistributionInfoDAOImpl implements CigaretteDistributionInfoDAO {
    
    private static final String TABLE_NAME = "cigarette_distribution_info";
    
    private final CigaretteDistributionInfoMapper mapper;
    private final PartitionTableManager partitionTableManager;
    
    public CigaretteDistributionInfoDAOImpl(CigaretteDistributionInfoMapper mapper,
                                            PartitionTableManager partitionTableManager) {
        this.mapper = mapper;
        this.partitionTableManager = partitionTableManager;
    }
    
    @Override
    public List<Map<String, Object>> findAll(Integer year, Integer month, Integer weekSeq) {
        partitionTableManager.ensurePartitionExists(TABLE_NAME, year, month, weekSeq);
        QueryWrapper<CigaretteDistributionInfoData> qw = new QueryWrapper<>();
        qw.eq("YEAR", year).eq("MONTH", month).eq("WEEK_SEQ", weekSeq)
          .orderByAsc("CIG_CODE", "CIG_NAME");
        return mapper.selectMaps(qw);
    }
    
    @Override
    public List<Map<String, Object>> findByCigCodeAndName(Integer year, Integer month, Integer weekSeq, 
                                                          String cigCode, String cigName) {
        partitionTableManager.ensurePartitionExists(TABLE_NAME, year, month, weekSeq);
        QueryWrapper<CigaretteDistributionInfoData> qw = new QueryWrapper<>();
        qw.eq("YEAR", year).eq("MONTH", month).eq("WEEK_SEQ", weekSeq)
          .eq("CIG_CODE", cigCode).eq("CIG_NAME", cigName);
        return mapper.selectMaps(qw);
    }
    
    @Override
    public List<Map<String, Object>> findDistinctCombinations(Integer year, Integer month, Integer weekSeq) {
        partitionTableManager.ensurePartitionExists(TABLE_NAME, year, month, weekSeq);
        QueryWrapper<CigaretteDistributionInfoData> qw = new QueryWrapper<>();
        qw.select("DISTINCT DELIVERY_METHOD", "DELIVERY_ETYPE", "TAG")
          .eq("YEAR", year).eq("MONTH", month).eq("WEEK_SEQ", weekSeq);
        return mapper.selectMaps(qw);
    }
    
    @Override
    public int insert(Integer year, Integer month, Integer weekSeq, CigaretteDistributionInfoData data) {
        partitionTableManager.ensurePartitionExists(TABLE_NAME, year, month, weekSeq);
        data.setYear(year);
        data.setMonth(month);
        data.setWeekSeq(weekSeq);
        return mapper.upsert(data);
    }
    
    @Override
    public int batchInsert(Integer year, Integer month, Integer weekSeq, List<CigaretteDistributionInfoData> dataList) {
        if (dataList == null || dataList.isEmpty()) {
            return 0;
        }
        
        partitionTableManager.ensurePartitionExists(TABLE_NAME, year, month, weekSeq);
        dataList.forEach(d -> {
            d.setYear(year);
            d.setMonth(month);
            d.setWeekSeq(weekSeq);
        });
        return mapper.batchUpsert(dataList);
    }
    
    @Override
    public int update(Integer year, Integer month, Integer weekSeq, CigaretteDistributionInfoData data) {
        partitionTableManager.ensurePartitionExists(TABLE_NAME, year, month, weekSeq);
        data.setYear(year);
        data.setMonth(month);
        data.setWeekSeq(weekSeq);
        return mapper.updateOne(data);
    }
    
    @Override
    public int deleteByCigCodeAndName(Integer year, Integer month, Integer weekSeq, 
                                      String cigCode, String cigName) {
        QueryWrapper<CigaretteDistributionInfoData> qw = new QueryWrapper<>();
        qw.eq("YEAR", year).eq("MONTH", month).eq("WEEK_SEQ", weekSeq)
          .eq("CIG_CODE", cigCode).eq("CIG_NAME", cigName);
        return mapper.delete(qw);
    }
    
    @Override
    public int deleteByYearMonthWeekSeq(Integer year, Integer month, Integer weekSeq) {
        QueryWrapper<CigaretteDistributionInfoData> qw = new QueryWrapper<>();
        qw.eq("YEAR", year).eq("MONTH", month).eq("WEEK_SEQ", weekSeq);
        return mapper.delete(qw);
    }
    
    @Override
    public long count(Integer year, Integer month, Integer weekSeq) {
        QueryWrapper<CigaretteDistributionInfoData> qw = new QueryWrapper<>();
        qw.eq("YEAR", year).eq("MONTH", month).eq("WEEK_SEQ", weekSeq);
        Long c = mapper.selectCount(qw);
        return c != null ? c : 0L;
    }
    
    @Override
    public boolean exists(Integer year, Integer month, Integer weekSeq) {
        return count(year, month, weekSeq) > 0;
    }
}

