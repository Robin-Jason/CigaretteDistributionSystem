package org.example.application.service.coordinator.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.application.service.coordinator.RegionCustomerStatisticsBuildService;
import org.example.application.service.coordinator.TagExtractionService;
import org.example.domain.model.tag.TagFilterRule;
import org.example.domain.repository.CigaretteDistributionInfoRepository;
import org.example.domain.repository.RegionCustomerStatisticsRepository;
import org.example.shared.exception.RegionNoCustomerException;
import org.example.shared.util.RegionRecordBuilder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.*;

import org.example.shared.dto.RegionCustomerRecord;

import static org.example.shared.util.MapValueExtractor.getStringValue;

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

    private final CigaretteDistributionInfoRepository cigaretteDistributionInfoRepository;
    private final RegionCustomerStatisticsRepository regionCustomerStatisticsRepository;
    private final TagExtractionService tagExtractionService;
    private final RegionRecordBuilder regionRecordBuilder;
    
    /**
     * 构建全量区域客户数表
     * 
     * 使用 REQUIRES_NEW 传播级别，确保在独立事务中运行，避免嵌套事务导致的表锁问题
     * 
     * @param year 年份
     * @param month 月份
     * @param weekSeq 周序号
     * @deprecated temporaryTableName 参数已删除，系统现在只使用分区表
     */
    @Override
    @Transactional(propagation = org.springframework.transaction.annotation.Propagation.REQUIRES_NEW, rollbackFor = Exception.class)
    public Map<String, Object> buildRegionCustomerStatistics(Integer year, Integer month, Integer weekSeq) {
        log.info("开始构建区域客户数统计表: {}-{}-{}, 使用分区表(customer_filter)", year, month, weekSeq);
        
        Map<String, Object> result = new HashMap<>();
        
        try {
            // 1. 清空对应分区的旧数据（Repository层会自动确保分区存在）
            int deletedCount = regionCustomerStatisticsRepository.deleteByYearMonthWeekSeq(year, month, weekSeq);
            log.info("清空区域客户数统计表分区: {}-{}-{}, 删除 {} 条旧记录", year, month, weekSeq, deletedCount);
            
            // 3. 扫描cigarette_distribution_info表，获取所有不重复的投放组合
            List<Map<String, Object>> distinctCombinations = cigaretteDistributionInfoRepository.findDistinctCombinations(year, month, weekSeq);
            log.info("扫描到 {} 个不重复的投放组合", distinctCombinations.size());
            
            if (distinctCombinations.isEmpty()) {
                result.put("success", false);
                result.put("message", "未找到需要统计的投放组合，请先导入卷烟投放信息");
                return result;
            }
            
            // 4. 对每个投放组合，构建区域客户数统计
            Map<String, RegionCustomerRecord> regionRecordMap = new LinkedHashMap<>();
            List<String> noCustomerRegions = new ArrayList<>(); // 记录无客户数据的区域

            for (Map<String, Object> combination : distinctCombinations) {
                String deliveryMethod = getStringValue(combination, "DELIVERY_METHOD");
                String deliveryEtype = getStringValue(combination, "DELIVERY_ETYPE");
                log.debug("处理投放组合: DELIVERY_METHOD={}, DELIVERY_ETYPE={}", deliveryMethod, deliveryEtype);

                List<TagFilterRule> tagRules = tagExtractionService.resolveTagFilters(combination);

                try {
                // 使用分区表
                List<RegionCustomerRecord> records = regionRecordBuilder.buildRecordsForCombination(
                        deliveryMethod, deliveryEtype, tagRules, year, month, weekSeq);

                // 合并：如果区域已存在，去重（保留第一次结果，因为同一区域应该使用同一份数据）
                // 规则：
                // 1. 对于"全市"区域：按档位投放和按价位段自选投放应该使用同一份客户数数据
                // 2. 对于"按档位扩展投放"：投放组合（DELIVERY_METHOD，DELIVERY_ETYPE，TAG，TAG_FILTER_CONFIG）相同的卷烟应当共享同一份区域客户数数据
                // 因此，如果区域已存在，直接跳过（去重），保留第一次的结果
                for (RegionCustomerRecord record : records) {
                    RegionCustomerRecord previous = regionRecordMap.get(record.getRegion());
                    if (previous != null) {
                        log.debug("区域 {} 已存在，跳过重复结果（使用同一份数据）: 已存在TOTAL={}, 新TOTAL={}, 当前投放组合=({}, {})", 
                                record.getRegion(), previous.getTotal(), record.getTotal(), deliveryMethod, deliveryEtype);
                    } else {
                        regionRecordMap.put(record.getRegion(), record);
                            // 检查是否30个档位全为0
                            if (isAllGradesZero(record)) {
                                noCustomerRegions.add(record.getRegion());
                    }
                }
                    }
                } catch (RegionNoCustomerException e) {
                    // 捕获区域无客户异常，记录警告但继续处理其他投放组合
                    log.warn("投放组合 ({}, {}) 存在无客户数据的区域: {}", 
                            deliveryMethod, deliveryEtype, e.getRegionName());
                    noCustomerRegions.add(e.getRegionName());
                    // 异常已经被处理，继续处理下一个投放组合
                }
            }
            
            // 如果有无客户数据的区域，记录警告
            if (!noCustomerRegions.isEmpty()) {
                log.warn("以下区域在 customer_filter 表中没有客户数据（30个档位全为0），已写入 region_customer_statistics 表: {}", 
                        noCustomerRegions);
            }
            
            List<RegionCustomerRecord> allRecords =
                new ArrayList<>(regionRecordMap.values());
            
            // 5. 批量插入到region_customer_statistics分区表
            if (!allRecords.isEmpty()) {
                int insertedCount = regionCustomerStatisticsRepository.batchUpsert(year, month, weekSeq, allRecords);
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

    /**
     * 检查区域记录的30个档位是否全为0
     */
    private boolean isAllGradesZero(RegionCustomerRecord record) {
        if (record == null || record.getGrades() == null) {
            return true;
        }
        BigDecimal[] grades = record.getGrades();
        for (BigDecimal count : grades) {
            if (count != null && count.compareTo(BigDecimal.ZERO) > 0) {
                return false;
            }
        }
        return true;
    }
    
}

