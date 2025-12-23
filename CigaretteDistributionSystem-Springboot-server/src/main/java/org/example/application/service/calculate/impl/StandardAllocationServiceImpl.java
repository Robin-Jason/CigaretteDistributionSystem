package org.example.application.service.calculate.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import lombok.extern.slf4j.Slf4j;
import org.example.application.dto.allocation.GenerateDistributionPlanRequestDto;
import org.example.application.dto.allocation.GenerateDistributionPlanResponseDto;
import org.example.application.dto.allocation.TotalActualDeliveryResponseDto;
import org.example.infrastructure.persistence.po.CigaretteDistributionInfoPO;
import org.example.infrastructure.persistence.po.CigaretteDistributionPredictionPO;
import org.example.domain.model.valueobject.RegionCustomerMatrix;
import org.example.application.service.calculate.StandardAllocationService;
import org.example.application.service.coordinator.CustomerMatrixBuilder;
import org.example.application.service.coordinator.AllocationAlgorithmSelector;
import org.example.application.service.prediction.PartitionPredictionQueryService;
import org.example.application.service.writeback.StandardDistributionWriteBackService;
import org.example.domain.model.valueobject.DeliveryMethodType;
import org.example.domain.repository.CigaretteDistributionInfoRepository;
import org.example.domain.repository.RegionCustomerStatisticsRepository;
import org.example.application.converter.DistributionDataConverter;
import org.example.shared.util.ActualDeliveryCalculator;
import org.example.shared.util.MapValueExtractor;
import org.example.shared.util.PartitionTableManager;
import org.example.domain.event.DistributionPlanGenerationStartedEvent;
import org.example.domain.event.DistributionPlanGenerationCompletedEvent;
import org.example.domain.event.ExistingDataDeletedEvent;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.*;
import java.util.Optional;

/**
 * 标准分配服务实现类（按档位投放、按档位扩展投放）。
 * <p>
 * 负责算法计算和分配矩阵写回等核心计算功能。
 * </p>
 *
 * @author Robin
 * @version 3.0
 * @since 2025-10-10
 */
@Slf4j
@Service
public class StandardAllocationServiceImpl implements StandardAllocationService {

    @Autowired
    private CigaretteDistributionInfoRepository cigaretteDistributionInfoRepository;

    @Autowired
    private RegionCustomerStatisticsRepository regionCustomerStatisticsRepository;

    @Autowired
    private PartitionTableManager partitionTableManager;

    @Autowired
    private PartitionPredictionQueryService partitionPredictionQueryService;

    @Autowired
    private StandardDistributionWriteBackService distributionWriteBackService;

    @Autowired
    private CustomerMatrixBuilder customerMatrixBuilder;

    @Autowired
    private AllocationAlgorithmSelector allocationAlgorithmSelector;

    @Autowired
    private ApplicationEventPublisher eventPublisher;

    // ==================== 主流程方法 ====================

    /**
     * 一键生成分配方案（完整流程，内部查询 info 表）。
     * @deprecated 建议使用 {@link #generateDistributionPlan(GenerateDistributionPlanRequestDto, List)} 接收已过滤的卷烟列表
     */
    @Override
    @Deprecated
    public GenerateDistributionPlanResponseDto generateDistributionPlan(GenerateDistributionPlanRequestDto request) {
        eventPublisher.publishEvent(new DistributionPlanGenerationStartedEvent(
            request.getYear(), request.getMonth(), request.getWeekSeq()));
        
        GenerateDistributionPlanResponseDto response = initResponse(request);
        
        try {
            // 1. 删除现有数据
            if (!deleteExistingDataIfPresent(request, response)) {
                return response;
            }
            
            // 2. 执行分配计算
            Map<String, BigDecimal> marketRatios = buildMarketRatios(request);
            Map<String, Object> allocationResult = executeAllocation(request, marketRatios);
            
            // 3. 构建响应
            buildResponse(request, response, allocationResult);
            
        } catch (Exception e) {
            handleException(request, response, e);
        }
        
        // 4. 发布完成事件
        publishCompletedEvent(response);
        return response;
    }

    /**
     * 生成分配方案（接收已过滤的标准分配卷烟列表）。
     * <p>
     * 由 UnifiedAllocationService 调用，传入已按 delivery_method 过滤的标准分配卷烟列表。
     * </p>
     */
    @Override
    public GenerateDistributionPlanResponseDto generateDistributionPlan(GenerateDistributionPlanRequestDto request,
                                                                         List<Map<String, Object>> cigaretteList) {
        eventPublisher.publishEvent(new DistributionPlanGenerationStartedEvent(
            request.getYear(), request.getMonth(), request.getWeekSeq()));
        
        GenerateDistributionPlanResponseDto response = initResponse(request);
        
        try {
            // 1. 删除现有数据
            if (!deleteExistingDataIfPresent(request, response)) {
                return response;
            }
            
            // 2. 执行分配计算（使用传入的卷烟列表）
            Map<String, BigDecimal> marketRatios = buildMarketRatios(request);
            Map<String, Object> allocationResult = executeAllocationWithList(cigaretteList, request, marketRatios);
            
            // 3. 构建响应
            buildResponse(request, response, allocationResult);
            
        } catch (Exception e) {
            handleException(request, response, e);
        }
        
        // 4. 发布完成事件
        publishCompletedEvent(response);
        return response;
    }

    /**
     * 计算指定时间范围内所有卷烟的总实际投放量。
     *
     * @param year    年份
     * @param month   月份
     * @param weekSeq 周序号
     * @return 总实际投放量响应 DTO，包含按卷烟分组的投放量数据
     * @example
     * <pre>{@code
     * TotalActualDeliveryResponseDto response = service.calculateTotalActualDelivery(2025, 1, 1);
     * if (response.isSuccess()) {
     *     Map<String, BigDecimal> data = response.getData();
     *     // data: {"cigCode_cigName": totalDelivery, ...}
     * }
     * }</pre>
     */
    @Override
    public TotalActualDeliveryResponseDto calculateTotalActualDelivery(Integer year, Integer month, Integer weekSeq) {
        log.info("开始计算总实际投放量，年份: {}, 月份: {}, 周序号: {}", year, month, weekSeq);
        
        TotalActualDeliveryResponseDto response = new TotalActualDeliveryResponseDto();
        response.setYear(year);
        response.setMonth(month);
        response.setWeekSeq(weekSeq);
        
        try {
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


    /**
     * 按卷烟代码+名称分组计算总实际投放量。
     *
     * @param rawDataList 预测表记录列表（含 ACTUAL_DELIVERY 字段）
     * @return key 为 "cigCode_cigName" 的投放总量 Map；若输入为空则返回空 Map
     * @example
     * <pre>{@code
     * List<CigaretteDistributionPredictionPO> dataList = queryService.queryPredictionByTime(2025, 1, 1);
     * Map<String, BigDecimal> result = service.calculateTotalActualDeliveryByTobacco(dataList);
     * // result: {"001_红塔山": 1000.00, "002_云烟": 2000.00, ...}
     * }</pre>
     */
    @Override
    public Map<String, BigDecimal> calculateTotalActualDeliveryByTobacco(List<CigaretteDistributionPredictionPO> rawDataList) {
        if (rawDataList == null || rawDataList.isEmpty()) {
            return new HashMap<>();
        }
        
        Map<String, BigDecimal> result = rawDataList.stream()
                .filter(data -> data.getActualDelivery() != null)
                .collect(java.util.stream.Collectors.groupingBy(
                        data -> data.getCigCode() + "_" + data.getCigName(),
                        java.util.stream.Collectors.reducing(
                                BigDecimal.ZERO,
                                CigaretteDistributionPredictionPO::getActualDelivery,
                                BigDecimal::add
                        )
                ));
        
        log.debug("完成总实际投放量计算，共处理 {} 种卷烟", result.size());
        return result;
    }

    /**
     * 计算区域实际投放量（使用分区表 region_customer_statistics）。
     *
     * @param target         目标区域名称
     * @param allocationRow  分配矩阵的一行（30个档位，D30-D1）
     * @param deliveryMethod 投放方法
     * @param deliveryEtype  扩展投放类型
     * @param year           年份
     * @param month          月份
     * @param weekSeq        周序号
     * @return 计算得到的实际投放量
     * @throws IllegalArgumentException 当目标区域为空、档位数组不合法或时间参数为空时
     * @throws RuntimeException 当无法获取客户数数据或计算过程发生错误时
     * @example
     * <pre>{@code
     * BigDecimal[] allocationRow = new BigDecimal[30];
     * Arrays.fill(allocationRow, BigDecimal.ONE);
     * BigDecimal delivery = service.calculateActualDeliveryForRegionDynamic(
     *     "昆明市", allocationRow, "按档位投放", null, 2025, 1, 1);
     * }</pre>
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
            BigDecimal[] customerCounts = getCustomerCountsForTargetDynamic(target, deliveryMethod, deliveryEtype, year, month, weekSeq);
            log.debug("成功从分区表获取区域 '{}' 的客户数数据 ({}年{}月第{}周)", target, year, month, weekSeq);
            
            if (customerCounts == null || customerCounts.length != 30) {
                throw new RuntimeException(String.format("无法获取区域 '%s' 的客户数档位数据", target));
            }
            
            BigDecimal actualDelivery = ActualDeliveryCalculator.calculateFixed30(allocationRow, customerCounts);
            log.debug("区域 '{}' 实际投放量计算完成: {}", target, actualDelivery);
            return actualDelivery;
            
        } catch (Exception e) {
            String errorMessage = String.format("计算区域 '%s' 的实际投放量时发生错误: %s", target, e.getMessage());
            log.error(errorMessage, e);
            throw new RuntimeException(errorMessage, e);
        }
    }

    // ==================== 私有辅助方法 ====================

    /**
     * 初始化响应对象。
     *
     * @param request 请求 DTO
     * @return 初始化后的响应 DTO，包含年份、月份、周序号和开始时间
     */
    private GenerateDistributionPlanResponseDto initResponse(GenerateDistributionPlanRequestDto request) {
        GenerateDistributionPlanResponseDto response = new GenerateDistributionPlanResponseDto();
        response.setYear(request.getYear());
        response.setMonth(request.getMonth());
        response.setWeekSeq(request.getWeekSeq());
        response.setStartTime(System.currentTimeMillis());
        return response;
    }

    /**
     * 删除现有数据（如果存在）。
     *
     * @param request  请求 DTO
     * @param response 响应 DTO，用于记录删除结果
     * @return true 表示继续执行后续流程，false 表示删除失败需终止流程
     */
    private boolean deleteExistingDataIfPresent(GenerateDistributionPlanRequestDto request, 
                                                 GenerateDistributionPlanResponseDto response) {
        List<CigaretteDistributionPredictionPO> existingData = 
            partitionPredictionQueryService.queryPredictionByTime(request.getYear(), request.getMonth(), request.getWeekSeq());
        
        if (existingData.isEmpty()) {
            response.setDeletedExistingData(false);
            response.setDeletedRecords(0);
            return true;
        }
        
        Map<String, Object> deleteResult = partitionPredictionQueryService.deletePredictionByTime(
            request.getYear(), request.getMonth(), request.getWeekSeq());
        
        if (!Boolean.TRUE.equals(deleteResult.get("success"))) {
            response.setSuccess(false);
            response.setMessage("删除现有分配数据失败: " + deleteResult.get("message"));
            response.setError("DELETE_FAILED");
            return false;
        }
        
        Integer deletedCount = MapValueExtractor.toInteger(deleteResult.get("deletedCount"), 0);
        response.setDeletedExistingData(true);
        response.setDeletedRecords(deletedCount);
        
        eventPublisher.publishEvent(new ExistingDataDeletedEvent(
            request.getYear(), request.getMonth(), request.getWeekSeq(), deletedCount));
        return true;
    }

    /**
     * 构建市场类型比例参数。
     *
     * @param request 请求 DTO
     * @return 包含 urbanRatio 和 ruralRatio 的 Map；若请求中未设置比例则返回 null
     */
    private Map<String, BigDecimal> buildMarketRatios(GenerateDistributionPlanRequestDto request) {
        if (request.getUrbanRatio() == null || request.getRuralRatio() == null) {
            return null;
        }
        Map<String, BigDecimal> marketRatios = new HashMap<>();
        marketRatios.put("urbanRatio", request.getUrbanRatio());
        marketRatios.put("ruralRatio", request.getRuralRatio());
        return marketRatios;
    }


    /**
     * 执行分配计算主流程。
     * <p>只处理标准分配类型（按档位投放、按档位扩展投放）</p>
     *
     * @param request      请求 DTO
     * @param marketRatios 市场类型比例参数，可为 null
     * @return 分配结果 Map，包含 success、message、totalCount、successCount、results 等字段
     */
    private Map<String, Object> executeAllocation(GenerateDistributionPlanRequestDto request, 
                                                   Map<String, BigDecimal> marketRatios) {
        Map<String, Object> allocationResult = new HashMap<>();
        List<Map<String, Object>> writeBackResults = new ArrayList<>();
        
        try {
            partitionTableManager.ensurePartitionExists("cigarette_distribution_info",
                    request.getYear(), request.getMonth(), request.getWeekSeq());
            log.debug("查询卷烟投放基本信息分区表: {}-{}-{}", request.getYear(), request.getMonth(), request.getWeekSeq());

            List<Map<String, Object>> advDataList = queryDistributionInfo(request);
            
            // 只处理标准分配类型（按档位投放、按档位扩展投放）
            List<Map<String, Object>> standardList = filterStandardAllocationData(advDataList);
            int[] counts = processStandardAllocations(standardList, request, marketRatios, writeBackResults);
            
            allocationResult.put("success", true);
            allocationResult.put("message", String.format("标准分配完成，成功: %d/%d", counts[0], counts[1]));
            allocationResult.put("totalCount", counts[1]);
            allocationResult.put("successCount", counts[0]);
            allocationResult.put("results", writeBackResults);
            
        } catch (Exception e) {
            log.error("执行分配计算时发生错误", e);
            allocationResult.put("success", false);
            allocationResult.put("message", "执行分配计算失败: " + e.getMessage());
            allocationResult.put("totalCount", 0);
            allocationResult.put("successCount", 0);
            allocationResult.put("results", new ArrayList<>());
        }
        return allocationResult;
    }

    /**
     * 执行分配计算（接收已过滤的卷烟列表）。
     *
     * @param cigaretteList 已过滤的标准分配卷烟列表
     * @param request       请求 DTO
     * @param marketRatios  市场类型比例参数，可为 null
     * @return 分配结果 Map
     */
    private Map<String, Object> executeAllocationWithList(List<Map<String, Object>> cigaretteList,
                                                          GenerateDistributionPlanRequestDto request, 
                                                          Map<String, BigDecimal> marketRatios) {
        Map<String, Object> allocationResult = new HashMap<>();
        List<Map<String, Object>> writeBackResults = new ArrayList<>();
        
        try {
            int[] counts = processStandardAllocations(cigaretteList, request, marketRatios, writeBackResults);
            
            allocationResult.put("success", true);
            allocationResult.put("message", String.format("标准分配完成，成功: %d/%d", counts[0], counts[1]));
            allocationResult.put("totalCount", counts[1]);
            allocationResult.put("successCount", counts[0]);
            allocationResult.put("results", writeBackResults);
            
        } catch (Exception e) {
            log.error("执行分配计算时发生错误", e);
            allocationResult.put("success", false);
            allocationResult.put("message", "执行分配计算失败: " + e.getMessage());
            allocationResult.put("totalCount", 0);
            allocationResult.put("successCount", 0);
            allocationResult.put("results", new ArrayList<>());
        }
        return allocationResult;
    }

    /**
     * 查询卷烟投放基本信息。
     *
     * @param request 请求 DTO，包含年份、月份、周序号
     * @return 卷烟投放信息列表，按 CIG_CODE、CIG_NAME 升序排列
     */
    private List<Map<String, Object>> queryDistributionInfo(GenerateDistributionPlanRequestDto request) {
        QueryWrapper<CigaretteDistributionInfoPO> infoQuery = new QueryWrapper<>();
        infoQuery.eq("YEAR", request.getYear())
                .eq("MONTH", request.getMonth())
                .eq("WEEK_SEQ", request.getWeekSeq())
                .orderByAsc("CIG_CODE", "CIG_NAME");
        return cigaretteDistributionInfoRepository.selectMaps(infoQuery);
    }

    /**
     * 过滤出标准分配数据（使用 DeliveryMethodType 枚举判断）。
     *
     * @param advDataList 原始卷烟投放信息列表
     * @return 过滤后的列表，只包含标准分配类型的数据
     */
    private List<Map<String, Object>> filterStandardAllocationData(List<Map<String, Object>> advDataList) {
        List<Map<String, Object>> filtered = new ArrayList<>();
        for (Map<String, Object> advData : advDataList) {
            String deliveryMethod = DistributionDataConverter.getStringIgnoreCase(advData, "delivery_method");
            Optional<DeliveryMethodType> methodType = DeliveryMethodType.from(deliveryMethod);
            
            // 只保留标准分配类型：GRADE（按档位投放、按档位统一投放）、GRADE_EXTEND（按档位扩展投放）
            if (methodType.isPresent() && 
                    (methodType.get() == DeliveryMethodType.GRADE || methodType.get() == DeliveryMethodType.GRADE_EXTEND)) {
                filtered.add(advData);
            }
        }
        return filtered;
    }

    /**
     * 处理标准分配（按档位投放、按档位扩展投放等）。
     *
     * @param dataList         待处理的卷烟数据列表
     * @param request          请求 DTO
     * @param marketRatios     市场类型比例参数
     * @param writeBackResults 写回结果列表，用于收集每个卷烟的处理结果
     * @return int[]{successCount, totalCount}，分别为成功数和总数
     */
    private int[] processStandardAllocations(List<Map<String, Object>> dataList, 
                                              GenerateDistributionPlanRequestDto request,
                                              Map<String, BigDecimal> marketRatios,
                                              List<Map<String, Object>> writeBackResults) {
        int successCount = 0;
        int totalCount = 0;
        
        for (Map<String, Object> advData : dataList) {
            totalCount++;
            Map<String, Object> cigResult = processSingleCigarette(advData, request, marketRatios);
            if ("成功".equals(cigResult.get("writeBackStatus"))) {
                successCount++;
            }
            writeBackResults.add(cigResult);
        }
        return new int[]{successCount, totalCount};
    }


    /**
     * 处理单个卷烟的分配。
     * <p>流程：验证卷烟代码 → 提取字段 → 构建客户矩阵 → 执行分配算法 → 写回数据库</p>
     *
     * @param advData      卷烟投放信息 Map
     * @param request      请求 DTO
     * @param marketRatios 市场类型比例参数
     * @return 处理结果 Map，包含 cigCode、cigName、writeBackStatus、writeBackMessage 等字段
     */
    private Map<String, Object> processSingleCigarette(Map<String, Object> advData,
                                                        GenerateDistributionPlanRequestDto request,
                                                        Map<String, BigDecimal> marketRatios) {
        Map<String, Object> cigResult = new HashMap<>();
        
        // 提取并验证卷烟代码
        String rawCigCode = DistributionDataConverter.getStringIgnoreCase(advData, "cig_code");
        String cigName = DistributionDataConverter.getStringIgnoreCase(advData, "cig_name");
        cigResult.put("cigCode", rawCigCode);
        cigResult.put("cigName", cigName);
        
        String cigCode;
        try {
            cigCode = DistributionDataConverter.sanitizeAndValidateCigaretteCode(rawCigCode, cigName);
            advData.put("cig_code", cigCode);
            cigResult.put("cigCode", cigCode);
        } catch (IllegalArgumentException e) {
            log.error("卷烟数据验证失败: 代码[{}] 名称[{}], 错误: {}", rawCigCode, cigName, e.getMessage());
            cigResult.put("writeBackStatus", "跳过");
            cigResult.put("writeBackMessage", "卷烟数据格式错误: " + e.getMessage());
            return cigResult;
        }
        
        // 提取其他字段
        BigDecimal adv = DistributionDataConverter.getBigDecimalIgnoreCase(advData, "adv");
        String deliveryArea = DistributionDataConverter.getStringIgnoreCase(advData, "delivery_area");
        String deliveryMethod = DistributionDataConverter.getStringIgnoreCase(advData, "delivery_method");
        String deliveryEtype = DistributionDataConverter.getStringIgnoreCase(advData, "delivery_etype");
        String tag = DistributionDataConverter.getStringIgnoreCase(advData, "tag");
        String tagFilterConfig = DistributionDataConverter.normalizeTagFilterConfig(
                DistributionDataConverter.getObjectIgnoreCase(advData, "TAG_FILTER_CONFIG"));
        String remark = DistributionDataConverter.resolveRemark(advData);
        String maxGrade = resolveGrade(DistributionDataConverter.getObjectIgnoreCase(advData, "highest_grade"), "D30");
        String minGrade = resolveGrade(DistributionDataConverter.getObjectIgnoreCase(advData, "lowest_grade"), "D1");
        
        Integer cigYear = DistributionDataConverter.extractYearIgnoreCase(advData, "year");
        Integer cigMonth = DistributionDataConverter.extractIntegerIgnoreCase(advData, "month");
        Integer cigWeekSeq = DistributionDataConverter.extractIntegerIgnoreCase(advData, "week_seq");
        
        cigResult.put("adv", adv);
        cigResult.put("deliveryArea", deliveryArea);
        cigResult.put("deliveryEtype", deliveryEtype);
        cigResult.put("advYear", cigYear);
        cigResult.put("advMonth", cigMonth);
        cigResult.put("advWeekSeq", cigWeekSeq);
        
        log.debug("处理卷烟: {} - {}, delivery_etype: {}, tag: {}", cigCode, cigName, deliveryEtype, tag);
        
        // 执行分配
        if (deliveryArea == null || deliveryArea.trim().isEmpty()) {
            cigResult.put("writeBackStatus", "跳过");
            cigResult.put("writeBackMessage", "投放区域为空");
            return cigResult;
        }
        
        try {
            // 1. 构建客户矩阵（带两周一访上浮处理）
            Map<String, Object> extraInfo = buildExtraInfo(deliveryEtype, marketRatios);
            RegionCustomerMatrix customerMatrix = customerMatrixBuilder.buildWithBoost(
                    cigYear, cigMonth, cigWeekSeq, deliveryArea,
                    deliveryMethod, deliveryEtype, tag, remark, extraInfo);
            
            if (customerMatrix == null || customerMatrix.isEmpty()) {
                cigResult.put("writeBackStatus", "跳过");
                cigResult.put("writeBackMessage", "未找到匹配的投放区域");
                return cigResult;
            }
            
            // 2. 执行分配算法
            Map<String, BigDecimal> groupRatios = extractGroupRatios(deliveryEtype, marketRatios, customerMatrix);
            Map<String, String> regionGroupMapping = extractRegionGroupMapping(deliveryEtype, customerMatrix);
            
            AllocationAlgorithmSelector.AllocationResult allocResult = allocationAlgorithmSelector.execute(
                    customerMatrix, adv, deliveryMethod, deliveryEtype, tag,
                    maxGrade, minGrade, groupRatios, regionGroupMapping, extraInfo);
            
            if (allocResult.isSuccess()) {
                boolean writeBackSuccess = distributionWriteBackService.writeBackSingleCigarette(
                    allocResult.getAllocationMatrix(), allocResult.getCustomerMatrix(), allocResult.getRegions(),
                    cigCode, cigName, cigYear, cigMonth, cigWeekSeq,
                    deliveryMethod, deliveryEtype, remark, tag, tagFilterConfig);
                
                cigResult.put("writeBackStatus", writeBackSuccess ? "成功" : "失败");
                cigResult.put("writeBackMessage", writeBackSuccess ? "分配矩阵已成功写回数据库" : "分配矩阵写回数据库失败");
            } else {
                cigResult.put("writeBackStatus", "跳过");
                cigResult.put("writeBackMessage", allocResult.getMessage() != null 
                    ? allocResult.getMessage() : "未找到匹配的投放目标");
            }
        } catch (Exception e) {
            log.error("处理卷烟 {} 时发生错误", cigCode, e);
            cigResult.put("writeBackStatus", "错误");
            cigResult.put("writeBackMessage", "处理过程中发生错误: " + e.getMessage());
        }
        return cigResult;
    }

    /**
     * 构建额外信息 Map。
     */
    private Map<String, Object> buildExtraInfo(String deliveryEtype, Map<String, BigDecimal> marketRatios) {
        Map<String, Object> extraInfo = new HashMap<>();
        if ("档位+市场类型".equals(deliveryEtype) && marketRatios != null) {
            extraInfo.put("groupRatios", marketRatios);
        }
        return extraInfo;
    }

    /**
     * 提取分组比例。
     */
    private Map<String, BigDecimal> extractGroupRatios(String deliveryEtype, 
                                                        Map<String, BigDecimal> marketRatios,
                                                        RegionCustomerMatrix customerMatrix) {
        if (!"档位+市场类型".equals(deliveryEtype) || marketRatios == null) {
            return null;
        }
        Map<String, BigDecimal> groupRatios = new HashMap<>();
        BigDecimal urbanRatio = marketRatios.get("urbanRatio");
        BigDecimal ruralRatio = marketRatios.get("ruralRatio");
        if (urbanRatio != null) {
            groupRatios.put("城网", urbanRatio);
        }
        if (ruralRatio != null) {
            groupRatios.put("农网", ruralRatio);
        }
        return groupRatios.isEmpty() ? null : groupRatios;
    }

    /**
     * 提取区域分组映射。
     */
    private Map<String, String> extractRegionGroupMapping(String deliveryEtype, 
                                                           RegionCustomerMatrix customerMatrix) {
        if (!"档位+市场类型".equals(deliveryEtype) || customerMatrix == null) {
            return null;
        }
        Map<String, String> mapping = new HashMap<>();
        for (RegionCustomerMatrix.Row row : customerMatrix.getRows()) {
            String region = row.getRegion();
            if (region != null) {
                // 根据区域名称判断城网/农网
                if (region.contains("城网") || region.contains("城区")) {
                    mapping.put(region, "城网");
                } else if (region.contains("农网") || region.contains("农村")) {
                    mapping.put(region, "农网");
                } else {
                    mapping.put(region, region);
                }
            }
        }
        return mapping.isEmpty() ? null : mapping;
    }

    /**
     * 解析档位字符串。
     */
    private String resolveGrade(Object raw, String fallback) {
        if (raw == null) {
            return fallback;
        }
        String value = raw.toString().trim().toUpperCase();
        return value.isEmpty() ? fallback : value;
    }

    /**
     * 构建响应对象。
     *
     * @param request          请求 DTO
     * @param response         响应 DTO
     * @param allocationResult 分配结果 Map
     */
    private void buildResponse(GenerateDistributionPlanRequestDto request,
                               GenerateDistributionPlanResponseDto response,
                               Map<String, Object> allocationResult) {
        int processedCount = queryProcessedCount(request);
        
        if (Boolean.TRUE.equals(allocationResult.get("success"))) {
            response.setEndTime(System.currentTimeMillis());
            response.setProcessingTime((response.getEndTime() - response.getStartTime()) + "ms");
            response.setSuccess(true);
            response.setMessage("一键分配方案生成成功");
            response.setAllocationResult(allocationResult);
            response.setTotalCigarettes(MapValueExtractor.toInteger(allocationResult.get("totalCount"), 0));
            response.setSuccessfulAllocations(MapValueExtractor.toInteger(allocationResult.get("successCount"), 0));
            response.setProcessedCount(processedCount);
            response.setAllocationDetails(allocationResult.get("results"));
            
            log.info("一键分配方案生成完成，成功分配: {}/{} 种卷烟，生成 {} 条分配记录", 
                    allocationResult.get("successCount"), allocationResult.get("totalCount"), processedCount);
        } else {
            response.setSuccess(false);
            response.setMessage("算法分配失败: " + allocationResult.get("message"));
            response.setError("ALLOCATION_FAILED");
            response.setProcessedCount(processedCount);
            response.setAllocationResult(allocationResult);
            
            log.error("一键分配方案生成失败: {}，已生成 {} 条分配记录", 
                    allocationResult.get("message"), processedCount);
        }
    }


    /**
     * 查询已处理记录数。
     *
     * @param request 请求 DTO
     * @return 已生成的分配记录数；查询异常时返回 0
     */
    private int queryProcessedCount(GenerateDistributionPlanRequestDto request) {
        try {
            List<CigaretteDistributionPredictionPO> data = 
                partitionPredictionQueryService.queryPredictionByTime(
                    request.getYear(), request.getMonth(), request.getWeekSeq());
            return data.size();
        } catch (Exception e) {
            log.warn("统计已生成记录数时发生异常: {}", e.getMessage());
            return 0;
        }
    }

    /**
     * 处理异常情况。
     *
     * @param request  请求 DTO
     * @param response 响应 DTO
     * @param e        捕获的异常
     */
    private void handleException(GenerateDistributionPlanRequestDto request,
                                  GenerateDistributionPlanResponseDto response,
                                  Exception e) {
        log.error("一键生成分配方案失败", e);
        response.setSuccess(false);
        response.setMessage("一键生成分配方案失败: " + e.getMessage());
        response.setError("GENERATION_FAILED");
        response.setProcessedCount(queryProcessedCount(request));
        response.setException(e.getClass().getSimpleName());
    }

    /**
     * 发布分配完成事件。
     *
     * @param response 响应 DTO，用于构建事件数据
     */
    private void publishCompletedEvent(GenerateDistributionPlanResponseDto response) {
        response.setEndTime(response.getEndTime() != null ? response.getEndTime() : System.currentTimeMillis());
        
        DistributionPlanGenerationCompletedEvent event = new DistributionPlanGenerationCompletedEvent(
            response.getYear(), response.getMonth(), response.getWeekSeq());
        event.setStartTime(response.getStartTime());
        event.setEndTime(response.getEndTime());
        event.setTotalCount(response.getTotalCigarettes());
        event.setSuccessCount(response.getSuccessfulAllocations());
        event.setFailedCount(response.getTotalCigarettes() - response.getSuccessfulAllocations());
        event.setProcessedCount(response.getProcessedCount());
        event.setSuccess(response.isSuccess());
        event.setMessage(response.getMessage());
        
        eventPublisher.publishEvent(event);
    }

    /**
     * 从分区表获取区域客户数数组。
     *
     * @param target         目标区域
     * @param deliveryMethod 投放方法
     * @param deliveryEtype  扩展投放类型
     * @param year           年份
     * @param month          月份
     * @param weekSeq        周序号
     * @return 客户数数组（30个档位，D30-D1）
     * @throws RuntimeException 当未找到目标区域数据时
     */
    private BigDecimal[] getCustomerCountsForTargetDynamic(String target, String deliveryMethod, String deliveryEtype,
                                                           Integer year, Integer month, Integer weekSeq) {
        partitionTableManager.ensurePartitionExists("region_customer_statistics", year, month, weekSeq);
        log.debug("查询区域客户统计分区表: {}-{}-{}, 目标区域: {}", year, month, weekSeq, target);

        Map<String, Object> row = regionCustomerStatisticsRepository.findByRegion(year, month, weekSeq, target);
        if (row == null) {
            throw new RuntimeException(String.format(
                    "在分区表中未找到目标区域 '%s' (投放方法: %s, 投放类型: %s, 时间: %d-%d-%d) 的客户数数据",
                    target, deliveryMethod, deliveryEtype, year, month, weekSeq));
        }
        return DistributionDataConverter.extractCustomerCounts(row);
    }
}
