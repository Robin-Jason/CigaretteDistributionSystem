package org.example.application.service.calculate.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import lombok.extern.slf4j.Slf4j;
import org.example.application.dto.GenerateDistributionPlanRequestDto;
import org.example.application.dto.GenerateDistributionPlanResponseDto;
import org.example.application.dto.TotalActualDeliveryResponseDto;
import org.example.infrastructure.persistence.po.CigaretteDistributionInfoPO;
import org.example.infrastructure.persistence.po.CigaretteDistributionPredictionPO;
import org.example.application.service.calculate.DistributionCalculateService;
import org.example.application.service.calculate.PriceBandAllocationService;
import org.example.application.service.query.PartitionPredictionQueryService;
import org.example.application.service.writeback.DistributionWriteBackService;
import org.example.domain.repository.CigaretteDistributionInfoRepository;
import org.example.domain.repository.RegionCustomerStatisticsRepository;
import org.example.application.converter.DistributionDataConverter;
import org.example.application.orchestrator.allocation.AllocationCalculationResult;
import org.example.application.orchestrator.allocation.DistributionAllocationOrchestrator;
import org.example.shared.util.ActualDeliveryCalculator;
import org.example.shared.util.PartitionTableManager;
import org.example.domain.event.DistributionPlanGenerationStartedEvent;
import org.example.domain.event.DistributionPlanGenerationCompletedEvent;
import org.example.domain.event.ExistingDataDeletedEvent;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.*;

/**
 * 分配计算服务实现类
 * 负责算法计算和分配矩阵写回等核心计算功能。
 *
 * @author Robin
 * @version 3.0
 * @since 2025-10-10
 */
@Slf4j
@Service
public class DistributionCalculateServiceImpl implements DistributionCalculateService {


    @Autowired
    private CigaretteDistributionInfoRepository cigaretteDistributionInfoRepository;

    @Autowired
    private RegionCustomerStatisticsRepository regionCustomerStatisticsRepository;

    @Autowired
    private PartitionTableManager partitionTableManager;

    @Autowired
    private PartitionPredictionQueryService partitionPredictionQueryService;

    @Autowired
    @Qualifier("standardDistributionWriteBackServiceImpl")
    private DistributionWriteBackService distributionWriteBackService;

    @Autowired
    private DistributionAllocationOrchestrator distributionAllocationOrchestrator;

    @Autowired
    private PriceBandAllocationService priceBandAllocationService;

    @Autowired
    private ApplicationEventPublisher eventPublisher;

    
    /**
     * 计算区域实际投放量（使用分区表 region_customer_statistics）
     *
     * @param target         目标区域
     * @param allocationRow  分配矩阵的一行（30个档位，D30-D1）
     * @param deliveryMethod 投放方法
     * @param deliveryEtype  扩展投放类型
     * @param year           年份
     * @param month          月份
     * @param weekSeq        周序号
     * @return 计算得到的实际投放量
     */
    public BigDecimal calculateActualDeliveryForRegionDynamic(String target, BigDecimal[] allocationRow, 
                                                              String deliveryMethod, String deliveryEtype,
                                                              Integer year, Integer month, Integer weekSeq) {
        if (target == null || target.trim().isEmpty()) {
            throw new IllegalArgumentException("目标区域不能为空");
        }
        
        if (allocationRow == null || allocationRow.length != 30) {
            throw new IllegalArgumentException("档位分配数组必须包含30个档位(D30-D1)");
        }
        
        if (year == null || month == null || weekSeq == null) {
            throw new IllegalArgumentException("年份、月份、周序号不能为空");
        }
        
        try {
            // 统一使用分区表获取客户数
            BigDecimal[] customerCounts = getCustomerCountsForTargetDynamic(target, deliveryMethod, deliveryEtype, year, month, weekSeq);
                log.debug("成功从分区表获取区域 '{}' 的客户数数据 ({}年{}月第{}周)", target, year, month, weekSeq);
            
            if (customerCounts == null || customerCounts.length != 30) {
                throw new RuntimeException(String.format("无法获取区域 '%s' 的客户数档位数据", target));
            }
            
            // 使用公共工具类计算实际投放量
            BigDecimal actualDelivery = ActualDeliveryCalculator.calculateFixed30(allocationRow, customerCounts);
            
            log.debug("区域 '{}' 实际投放量计算完成: {}", target, actualDelivery);
            return actualDelivery;
            
        } catch (Exception e) {
            String errorMessage = String.format("计算区域 '%s' 的实际投放量时发生错误: %s", target, e.getMessage());
            log.error(errorMessage, e);
            throw new RuntimeException(errorMessage, e);
        }
    }

    /**
     * 从分区表获取区域客户数数组
     * 使用分区表：region_customer_statistics
     * 
     * @param target 目标区域
     * @param deliveryMethod 投放方法
     * @param deliveryEtype 扩展投放类型
     * @param year 年份
     * @param month 月份
     * @param weekSeq 周序号
     * @return 客户数数组（30个档位）
     */
    private BigDecimal[] getCustomerCountsForTargetDynamic(String target, String deliveryMethod, String deliveryEtype,
                                                           Integer year, Integer month, Integer weekSeq) {
        if (target == null || target.trim().isEmpty()) {
            throw new IllegalArgumentException("目标区域不能为空");
        }
        
        if (deliveryMethod == null) {
            throw new IllegalArgumentException("投放方法不能为空");
        }
        
        if (year == null || month == null || weekSeq == null) {
            throw new IllegalArgumentException("年份、月份、周序号不能为空");
        }
        
        try {
            partitionTableManager.ensurePartitionExists("region_customer_statistics", year, month, weekSeq);
            log.debug("查询区域客户统计分区表: {}-{}-{}, 目标区域: {}", year, month, weekSeq, target);

            Map<String, Object> row = regionCustomerStatisticsRepository.findByRegion(year, month, weekSeq, target);
            if (row == null) {
                String errorMessage = String.format(
                        "在分区表中未找到目标区域 '%s' (投放方法: %s, 投放类型: %s, 时间: %d-%d-%d) 的客户数数据",
                        target, deliveryMethod, deliveryEtype, year, month, weekSeq);
                log.error(errorMessage);
                throw new RuntimeException(errorMessage);
            }

            BigDecimal[] customerCounts = DistributionDataConverter.extractCustomerCounts(row);
            
            log.debug("成功从分区表获取目标区域 '{}' 的客户数数据", target);
            return customerCounts;
            
        } catch (Exception e) {
            String errorMessage = String.format(
                "从分区表获取目标区域 '%s' 的客户数数据时发生错误 (year=%d, month=%d, weekSeq=%d): %s", 
                target, year, month, weekSeq, e.getMessage());
            log.error(errorMessage, e);
            throw new RuntimeException(errorMessage, e);
        }
    }
    
    /**
     * 按卷烟代码+名称分组计算总实际投放量。
     *
     * @param rawDataList 预测表记录列表（含 ACTUAL_DELIVERY 字段）
     * @return key 为 "cigCode_cigName" 的投放总量 Map
     */
    @Override
    public Map<String, BigDecimal> calculateTotalActualDeliveryByTobacco(List<CigaretteDistributionPredictionPO> rawDataList) {
        Map<String, BigDecimal> totalActualDeliveryMap = new HashMap<>();
        
        try {
            // 按卷烟代码+名称分组
            Map<String, List<CigaretteDistributionPredictionPO>> groupedByTobacco = new HashMap<>();
            for (CigaretteDistributionPredictionPO data : rawDataList) {
                String tobaccoKey = data.getCigCode() + "_" + data.getCigName();
                groupedByTobacco.computeIfAbsent(tobaccoKey, k -> new ArrayList<>()).add(data);
            }
            
            // 计算每个卷烟的总实际投放量
            for (Map.Entry<String, List<CigaretteDistributionPredictionPO>> entry : groupedByTobacco.entrySet()) {
                String tobaccoKey = entry.getKey();
                List<CigaretteDistributionPredictionPO> tobaccoRecords = entry.getValue();
                
                BigDecimal totalActualDelivery = BigDecimal.ZERO;
                
                // 直接从数据库记录中累加各区域的ACTUAL_DELIVERY字段
                for (CigaretteDistributionPredictionPO data : tobaccoRecords) {
                    if (data.getActualDelivery() != null) {
                        totalActualDelivery = totalActualDelivery.add(data.getActualDelivery());
                        log.debug("卷烟 {} 区域 {} 实际投放量: {}", data.getCigName(), data.getDeliveryArea(), data.getActualDelivery());
                    } else {
                        log.warn("卷烟 {} 区域 {} 的ACTUAL_DELIVERY字段为null", data.getCigName(), data.getDeliveryArea());
                    }
                }
                
                totalActualDeliveryMap.put(tobaccoKey, totalActualDelivery);
                log.debug("卷烟 {} 总实际投放量: {} (包含 {} 个区域)", 
                         tobaccoKey.split("_")[1], totalActualDelivery, tobaccoRecords.size());
            }
            
            log.debug("完成总实际投放量计算，共处理 {} 种卷烟", totalActualDeliveryMap.size());
            
        } catch (Exception e) {
            log.error("计算总实际投放量失败", e);
        }
        
        return totalActualDeliveryMap;
    }

    /**
     * 一键生成分配方案（完整流程）。
     * <p>删除旧数据由数据管理服务控制事务；写回动作按卷烟使用 REQUIRES_NEW 独立事务。</p>
     *
     * @param request 一键生成分配方案请求 DTO
     * @return 生成结果响应 DTO，包含处理统计与状态
     */
    @Override
    public GenerateDistributionPlanResponseDto generateDistributionPlan(GenerateDistributionPlanRequestDto request) {
        // 发布开始事件
        eventPublisher.publishEvent(new DistributionPlanGenerationStartedEvent(
            request.getYear(), request.getMonth(), request.getWeekSeq()
        ));
        
        GenerateDistributionPlanResponseDto response = new GenerateDistributionPlanResponseDto();
        response.setYear(request.getYear());
        response.setMonth(request.getMonth());
        response.setWeekSeq(request.getWeekSeq());
        response.setStartTime(System.currentTimeMillis());
        
        try {
            // 1. 检查指定日期是否存在分配数据
            List<CigaretteDistributionPredictionPO> existingData = 
                partitionPredictionQueryService.queryPredictionByTime(request.getYear(), request.getMonth(), request.getWeekSeq());
            
            if (!existingData.isEmpty()) {
                // 2. 删除现有分配数据
                Map<String, Object> deleteResult = partitionPredictionQueryService.deletePredictionByTime(
                    request.getYear(), request.getMonth(), request.getWeekSeq());
                
                if (!Boolean.TRUE.equals(deleteResult.get("success"))) {
                    response.setSuccess(false);
                    response.setMessage("删除现有分配数据失败: " + deleteResult.get("message"));
                    response.setError("DELETE_FAILED");
                    return response;
                }
                
                response.setDeletedExistingData(true);
                // 安全处理Long到Integer的转换
                Object deletedCountObj = deleteResult.get("deletedCount");
                Integer deletedCount;
                if (deletedCountObj instanceof Long) {
                    deletedCount = ((Long) deletedCountObj).intValue();
                } else if (deletedCountObj instanceof Integer) {
                    deletedCount = (Integer) deletedCountObj;
                } else {
                    deletedCount = deletedCountObj != null ? Integer.valueOf(deletedCountObj.toString()) : 0;
                }
                response.setDeletedRecords(deletedCount);
                
                // 发布删除完成事件
                eventPublisher.publishEvent(new ExistingDataDeletedEvent(
                    request.getYear(), request.getMonth(), request.getWeekSeq(), deletedCount
                ));
            } else {
                response.setDeletedExistingData(false);
                response.setDeletedRecords(0);
            }
            
            // 3. 执行算法分配并写回数据库
            // 构建市场类型比例参数
            Map<String, BigDecimal> marketRatios = null;
            if (request.getUrbanRatio() != null && request.getRuralRatio() != null) {
                marketRatios = new HashMap<>();
                marketRatios.put("urbanRatio", request.getUrbanRatio());
                marketRatios.put("ruralRatio", request.getRuralRatio());
            }
            
            // 执行分配计算和写回（原getAndwriteBackAllocationMatrix的逻辑）
            Map<String, Object> allocationResult = new HashMap<>();
            List<Map<String, Object>> writeBackResults = new ArrayList<>();
            
            try {
                partitionTableManager.ensurePartitionExists("cigarette_distribution_info",
                        request.getYear(), request.getMonth(), request.getWeekSeq());
                log.debug("查询卷烟投放基本信息分区表: {}-{}-{}", request.getYear(), request.getMonth(), request.getWeekSeq());

                QueryWrapper<CigaretteDistributionInfoPO> infoQuery = new QueryWrapper<>();
                infoQuery.eq("YEAR", request.getYear())
                        .eq("MONTH", request.getMonth())
                        .eq("WEEK_SEQ", request.getWeekSeq())
                        .orderByAsc("CIG_CODE", "CIG_NAME");
                List<Map<String, Object>> advDataList = cigaretteDistributionInfoRepository.selectMaps(infoQuery);
                
                // 先处理价位段自选投放的卷烟（批量处理）
                try {
                    log.info("开始处理价位段自选投放卷烟: {}-{}-{}", request.getYear(), request.getMonth(), request.getWeekSeq());
                    priceBandAllocationService.allocateForPriceBand(request.getYear(), request.getMonth(), request.getWeekSeq());
                    log.info("价位段自选投放卷烟处理完成: {}-{}-{}", request.getYear(), request.getMonth(), request.getWeekSeq());
                } catch (Exception e) {
                    log.error("价位段自选投放卷烟处理失败: {}-{}-{}", request.getYear(), request.getMonth(), request.getWeekSeq(), e);
                    // 不中断整个流程，继续处理其他卷烟
                }
                
                // 过滤掉已处理的价位段自选投放卷烟
                List<Map<String, Object>> filteredAdvDataList = new ArrayList<>();
                for (Map<String, Object> advData : advDataList) {
                    String deliveryMethod = DistributionDataConverter.getStringIgnoreCase(advData, "delivery_method");
                    if (!"按价位段自选投放".equals(deliveryMethod)) {
                        filteredAdvDataList.add(advData);
                    }
                }
                
                int successCount = 0;
                int totalCount = 0;
                
                // 处理其他类型的卷烟（按档位投放、按档位扩展投放等）
                for (Map<String, Object> advData : filteredAdvDataList) {
                    totalCount++;
                    // 初始化结果对象
                    Map<String, Object> cigResult = new HashMap<>();
                    
                    // 清洗和验证卷烟代码，处理格式不规范的数据（大小写不敏感）
                    String rawCigCode = DistributionDataConverter.getStringIgnoreCase(advData, "cig_code");
                    String cigName = DistributionDataConverter.getStringIgnoreCase(advData, "cig_name");
                    String cigCode;
                    
                    // 设置基本信息到结果中（先用原始数据）
                    cigResult.put("cigCode", rawCigCode);
                    cigResult.put("cigName", cigName);
                    
                    try {
                        cigCode = DistributionDataConverter.sanitizeAndValidateCigaretteCode(rawCigCode, cigName);
                        // 更新清洗后的代码到原数据中，确保后续使用的都是清洗后的代码
                        advData.put("cig_code", cigCode);
                        // 更新结果中的代码为清洗后的代码
                        cigResult.put("cigCode", cigCode);
                    } catch (IllegalArgumentException e) {
                        log.error("卷烟数据验证失败: 代码[{}] 名称[{}], 错误: {}", rawCigCode, cigName, e.getMessage());
                        cigResult.put("writeBackStatus", "跳过");
                        cigResult.put("writeBackMessage", "卷烟数据格式错误: " + e.getMessage());
                        writeBackResults.add(cigResult);
                        continue;
                    }
                    BigDecimal adv = DistributionDataConverter.getBigDecimalIgnoreCase(advData, "adv");
                    String deliveryArea = DistributionDataConverter.getStringIgnoreCase(advData, "delivery_area");
                    String deliveryEtype = DistributionDataConverter.getStringIgnoreCase(advData, "delivery_etype");
                    String tag = DistributionDataConverter.getStringIgnoreCase(advData, "tag");  // 获取标签字段
                    String tagFilterConfig = DistributionDataConverter.normalizeTagFilterConfig(
                            DistributionDataConverter.getObjectIgnoreCase(advData, "TAG_FILTER_CONFIG"));
                    
                    // 调试日志：检查从cigarette_distribution_info表读取的关键字段值
                    log.debug("处理卷烟: {} - {}, delivery_etype: {}, tag: {}", cigCode, cigName, deliveryEtype, tag);
                    // 从cigarette_distribution_info表中获取对应的日期信息
                    // 处理year字段可能是Date类型的情况
                    // 使用RowMapper工具类提取数据，避免重复的类型转换代码
                    Integer cigYear = DistributionDataConverter.extractYearIgnoreCase(advData, "year");
                    Integer cigMonth = DistributionDataConverter.extractIntegerIgnoreCase(advData, "month");
                    Integer cigWeekSeq = DistributionDataConverter.extractIntegerIgnoreCase(advData, "week_seq");
                    
                    // 设置其他信息到结果中
                    cigResult.put("adv", adv);
                    cigResult.put("deliveryArea", deliveryArea);
                    cigResult.put("deliveryEtype", deliveryEtype);
                    cigResult.put("advYear", cigYear);
                    cigResult.put("advMonth", cigMonth);
                    cigResult.put("advWeekSeq", cigWeekSeq);
                        
                    try {
                        if (deliveryArea != null && !deliveryArea.trim().isEmpty()) {
                            // 根据投放方式和扩展投放类型委托给对应的服务处理
                            String deliveryMethod = DistributionDataConverter.getStringIgnoreCase(advData, "delivery_method");
                            String remark = DistributionDataConverter.resolveRemark(advData);
                            
                            // 调试日志：检查关键字段值
                            log.debug("卷烟: {} - {}, deliveryMethod: {}, deliveryEtype: {}, tag: {}, remark: {}", 
                                     cigCode, cigName, deliveryMethod, deliveryEtype, tag, remark);
                            
                            // 步骤1: 执行算法分配计算（独立函数）
                            AllocationCalculationResult allocationCalcResult = distributionAllocationOrchestrator.calculateAllocationMatrix(
                                cigCode, cigName, deliveryMethod, deliveryEtype, tag, deliveryArea, adv,
                                cigYear, cigMonth, cigWeekSeq, advData, marketRatios, remark);
                            
                            // 步骤2: 写回数据库（按卷烟独立事务）
                            if (allocationCalcResult.isSuccess() && !allocationCalcResult.getTargetList().isEmpty() 
                                && allocationCalcResult.getAllocationMatrix() != null) {
                                boolean writeBackSuccess = distributionWriteBackService.writeBackSingleCigarette(
                                    allocationCalcResult.getAllocationMatrix(),
                                    allocationCalcResult.getCustomerMatrix(),
                                    allocationCalcResult.getTargetList(),
                                    cigCode, cigName, cigYear, cigMonth, cigWeekSeq,
                                    deliveryMethod, deliveryEtype, remark, tag, tagFilterConfig);
                                if (writeBackSuccess) {
                                    successCount++;
                                    cigResult.put("writeBackStatus", "成功");
                                    cigResult.put("writeBackMessage", "分配矩阵已成功写回数据库");
                                } else {
                                    cigResult.put("writeBackStatus", "失败");
                                    cigResult.put("writeBackMessage", "分配矩阵写回数据库失败");
                                }
                            } else {
                                cigResult.put("writeBackStatus", "跳过");
                                cigResult.put("writeBackMessage", allocationCalcResult.getErrorMessage() != null 
                                    ? allocationCalcResult.getErrorMessage() : "未找到匹配的投放目标");
                                cigResult.put("writeBackTimeMs", 0L);
                            }
                        } else {
                            cigResult.put("writeBackStatus", "跳过");
                            cigResult.put("writeBackMessage", "投放区域为空");
                            cigResult.put("calcTimeMs", 0L);
                            cigResult.put("writeBackTimeMs", 0L);
                        }
                        
                    } catch (Exception e) {
                        log.error("处理卷烟 {} 时发生错误", cigCode, e);
                        cigResult.put("writeBackStatus", "错误");
                        cigResult.put("writeBackMessage", "处理过程中发生错误: " + e.getMessage());
                    }
                    
                    writeBackResults.add(cigResult);
                }
                
                allocationResult.put("success", true);
                allocationResult.put("message", String.format("分配矩阵写回完成，成功: %d/%d", successCount, totalCount));
                allocationResult.put("totalCount", totalCount);
                allocationResult.put("successCount", successCount);
                allocationResult.put("results", writeBackResults);
            } catch (Exception e) {
                log.error("执行分配计算时发生错误", e);
                allocationResult.put("success", false);
                allocationResult.put("message", "执行分配计算失败: " + e.getMessage());
                allocationResult.put("totalCount", 0);
                allocationResult.put("successCount", 0);
                allocationResult.put("results", new ArrayList<>());
            }
            
            if (Boolean.TRUE.equals(allocationResult.get("success"))) {
                // 4. 分配成功，查询生成的分配记录数
                List<CigaretteDistributionPredictionPO> generatedData = 
                    partitionPredictionQueryService.queryPredictionByTime(
                        request.getYear(), request.getMonth(), request.getWeekSeq());
                int processedCount = generatedData.size();
                
                // 5. 计算处理时间
                response.setEndTime(System.currentTimeMillis());
                long processingTimeMs = response.getEndTime() - response.getStartTime();
                response.setProcessingTime(processingTimeMs + "ms");
                
                // 6. 组装响应数据
                response.setSuccess(true);
                response.setMessage("一键分配方案生成成功");
                response.setAllocationResult(allocationResult);
                // 安全处理类型转换
                Object totalCountObj = allocationResult.get("totalCount");
                if (totalCountObj instanceof Long) {
                    response.setTotalCigarettes(((Long) totalCountObj).intValue());
                } else if (totalCountObj instanceof Integer) {
                    response.setTotalCigarettes((Integer) totalCountObj);
                } else {
                    response.setTotalCigarettes(totalCountObj != null ? Integer.valueOf(totalCountObj.toString()) : 0);
                }
                Object successCountObj = allocationResult.get("successCount");
                if (successCountObj instanceof Long) {
                    response.setSuccessfulAllocations(((Long) successCountObj).intValue());
                } else if (successCountObj instanceof Integer) {
                    response.setSuccessfulAllocations((Integer) successCountObj);
                } else {
                    response.setSuccessfulAllocations(successCountObj != null ? Integer.valueOf(successCountObj.toString()) : 0);
                }
                response.setProcessedCount(processedCount);
                response.setAllocationDetails(allocationResult.get("results"));
                
                log.info("一键分配方案生成完成，成功分配: {}/{} 种卷烟，生成 {} 条分配记录", 
                        allocationResult.get("successCount"), allocationResult.get("totalCount"), processedCount);
            } else {
                // 分配失败，但仍需统计可能已生成的记录数
                List<CigaretteDistributionPredictionPO> partialData = 
                    partitionPredictionQueryService.queryPredictionByTime(
                        request.getYear(), request.getMonth(), request.getWeekSeq());
                int processedCount = partialData.size();
                
                response.setSuccess(false);
                response.setMessage("算法分配失败: " + allocationResult.get("message"));
                response.setError("ALLOCATION_FAILED");
                response.setProcessedCount(processedCount);
                response.setAllocationResult(allocationResult);
                
                log.error("一键分配方案生成失败: {}，已生成 {} 条分配记录", 
                        allocationResult.get("message"), processedCount);
            }
            
        } catch (Exception e) {
            log.error("一键生成分配方案失败", e);
            
            // 即使发生异常，也尝试统计已生成的记录数
            int processedCount = 0;
            try {
                List<CigaretteDistributionPredictionPO> existingRecords = 
                    partitionPredictionQueryService.queryPredictionByTime(
                        request.getYear(), request.getMonth(), request.getWeekSeq());
                processedCount = existingRecords.size();
            } catch (Exception countException) {
                log.warn("统计已生成记录数时发生异常: {}", countException.getMessage());
            }
            
            response.setSuccess(false);
            response.setMessage("一键生成分配方案失败: " + e.getMessage());
            response.setError("GENERATION_FAILED");
            response.setProcessedCount(processedCount);
            response.setException(e.getClass().getSimpleName());
        }
        
        // 发布完成事件
        response.setEndTime(response.getEndTime() != null ? response.getEndTime() : System.currentTimeMillis());
        DistributionPlanGenerationCompletedEvent completedEvent = new DistributionPlanGenerationCompletedEvent(
            request.getYear(), request.getMonth(), request.getWeekSeq()
        );
        completedEvent.setStartTime(response.getStartTime());
        completedEvent.setEndTime(response.getEndTime());
        completedEvent.setTotalCount(response.getTotalCigarettes());
        completedEvent.setSuccessCount(response.getSuccessfulAllocations());
        completedEvent.setFailedCount(response.getTotalCigarettes() - response.getSuccessfulAllocations());
        completedEvent.setProcessedCount(response.getProcessedCount());
        completedEvent.setSuccess(response.isSuccess());
        completedEvent.setMessage(response.getMessage());
        eventPublisher.publishEvent(completedEvent);
        
        return response;
    }

    /**
     * 计算指定时间范围内所有卷烟的总实际投放量
     */
    @Override
    public TotalActualDeliveryResponseDto calculateTotalActualDelivery(Integer year, Integer month, Integer weekSeq) {
        log.info("开始计算总实际投放量，年份: {}, 月份: {}, 周序号: {}", year, month, weekSeq);
        
        TotalActualDeliveryResponseDto response = new TotalActualDeliveryResponseDto();
        response.setYear(year);
        response.setMonth(month);
        response.setWeekSeq(weekSeq);
        
        try {
            // 获取指定时间的数据
            List<CigaretteDistributionPredictionPO> rawDataList = 
                partitionPredictionQueryService.queryPredictionByTime(year, month, weekSeq);
            
            if (rawDataList.isEmpty()) {
                response.setSuccess(false);
                response.setMessage("未找到指定时间的数据");
                response.setData(new HashMap<>());
                response.setTotalRecords(0);
                response.setCigaretteCount(0);
                return response;
            }
            
            // 计算总实际投放量
            Map<String, BigDecimal> totalActualDeliveryMap = calculateTotalActualDeliveryByTobacco(rawDataList);
            
            response.setSuccess(true);
            response.setMessage("总实际投放量计算成功");
            response.setData(totalActualDeliveryMap);
            response.setTotalRecords(rawDataList.size());
            response.setCigaretteCount(totalActualDeliveryMap.size());
            
            log.info("总实际投放量计算成功，返回{}种卷烟的数据", totalActualDeliveryMap.size());
            
        } catch (Exception e) {
            log.error("总实际投放量计算失败", e);
            response.setSuccess(false);
            response.setMessage("总实际投放量计算失败: " + e.getMessage());
            response.setData(new HashMap<>());
        }
        
        return response;
    }
}
