package org.example.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.mapper.CigaretteDistributionInfoMapper;
import org.example.mapper.RegionCustomerStatisticsMapper;
import org.example.service.RegionCustomerStatisticsBuildService;
import org.example.service.TagExtractionService;
import org.example.service.util.RegionRecordBuilder;
import org.example.service.model.tag.TagFilterRule;
import org.example.util.PartitionTableManager;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

import org.example.dao.RegionCustomerStatisticsDAO.RegionCustomerRecord;

import static org.example.service.util.MapValueExtractor.getStringValue;

/**
 * 区域客户数统计表构建服务实现类
 * 
 * 负责扫描卷烟投放基础信息表中的投放组合，构建全量区域客户数表
 * 
 * @author System
 * @version 1.0
 * @since 2025-11-30
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RegionCustomerStatisticsBuildServiceImpl implements RegionCustomerStatisticsBuildService {

    private static final String REGION_CUSTOMER_STATISTICS_TABLE = "region_customer_statistics";

    private final CigaretteDistributionInfoMapper cigaretteDistributionInfoMapper;
    private final RegionCustomerStatisticsMapper regionCustomerStatisticsMapper;
    private final TagExtractionService tagExtractionService;
    private final PartitionTableManager partitionTableManager;
    private final RegionRecordBuilder regionRecordBuilder;
    
    /**
     * 构建全量区域客户数表
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> buildRegionCustomerStatistics(Integer year, Integer month, Integer weekSeq, 
                                                               String temporaryTableName) {
        log.info("开始构建区域客户数统计表: {}-{}-{}, 临时表: {}", year, month, weekSeq, temporaryTableName);
        
        Map<String, Object> result = new HashMap<>();
        
        try {
            // 1. 确保分区存在
            partitionTableManager.ensurePartitionExists(REGION_CUSTOMER_STATISTICS_TABLE, year, month, weekSeq);
            
            // 2. 清空对应分区的旧数据
            int deletedCount = regionCustomerStatisticsMapper.deleteByYearMonthWeekSeq(year, month, weekSeq);
            log.info("清空区域客户数统计表分区: {}-{}-{}, 删除 {} 条旧记录", year, month, weekSeq, deletedCount);
            
            // 3. 扫描cigarette_distribution_info表，获取所有不重复的投放组合
            List<Map<String, Object>> distinctCombinations = cigaretteDistributionInfoMapper.findDistinctCombinations(year, month, weekSeq);
            log.info("扫描到 {} 个不重复的投放组合", distinctCombinations.size());
            
            if (distinctCombinations.isEmpty()) {
                result.put("success", false);
                result.put("message", "未找到需要统计的投放组合，请先导入卷烟投放信息");
                return result;
            }
            
            // 4. 对每个投放组合，构建区域客户数统计
            Map<String, RegionCustomerRecord> regionRecordMap = new LinkedHashMap<>();

            for (Map<String, Object> combination : distinctCombinations) {
                String deliveryMethod = getStringValue(combination, "DELIVERY_METHOD");
                String deliveryEtype = getStringValue(combination, "DELIVERY_ETYPE");
                log.debug("处理投放组合: DELIVERY_METHOD={}, DELIVERY_ETYPE={}", deliveryMethod, deliveryEtype);

                List<TagFilterRule> tagRules = tagExtractionService.resolveTagFilters(combination);

                List<RegionCustomerRecord> records =
                        regionRecordBuilder.buildRecordsForCombination(deliveryMethod, deliveryEtype, tagRules, temporaryTableName);

                // 去重：如果区域已存在，仅保留首次结果
                for (RegionCustomerRecord record : records) {
                    RegionCustomerRecord previous =
                            regionRecordMap.putIfAbsent(record.getRegion(), record);
                    if (previous != null) {
                        log.debug("区域 {} 已统计过，跳过重复结果", record.getRegion());
                    }
                }
            }
            
            List<RegionCustomerRecord> allRecords =
                new ArrayList<>(regionRecordMap.values());
            
            // 5. 批量插入到region_customer_statistics分区表
            if (!allRecords.isEmpty()) {
                int insertedCount = regionCustomerStatisticsMapper.batchUpsert(year, month, weekSeq, allRecords);
                log.info("区域客户数统计表构建完成: {}-{}-{}, 插入 {} 条记录", year, month, weekSeq, insertedCount);
                
                result.put("success", true);
                result.put("message", "构建成功");
                result.put("insertedCount", insertedCount);
                result.put("combinationCount", distinctCombinations.size());
                result.put("regionCount", allRecords.size());
            } else {
                result.put("success", false);
                result.put("message", "未生成任何区域客户数统计记录");
            }
            
        } catch (Exception e) {
            log.error("构建区域客户数统计表失败: {}-{}-{}", year, month, weekSeq, e);
            result.put("success", false);
            result.put("message", "构建失败: " + e.getMessage());
            throw e;
        }
        
        return result;
    }
    
}

