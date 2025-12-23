package org.example.application.service.calculate.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import lombok.extern.slf4j.Slf4j;
import org.example.application.dto.allocation.GenerateDistributionPlanRequestDto;
import org.example.application.dto.allocation.GenerateDistributionPlanResponseDto;
import org.example.application.service.calculate.PriceBandAllocationService;
import org.example.application.service.calculate.StandardAllocationService;
import org.example.application.service.calculate.UnifiedAllocationService;
import org.example.domain.model.valueobject.DeliveryMethodType;
import org.example.domain.repository.CigaretteDistributionInfoRepository;
import org.example.infrastructure.persistence.po.CigaretteDistributionInfoPO;
import org.example.shared.util.PartitionTableManager;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * 统一分配服务实现类。
 * <p>
 * 职责：
 * <ul>
 *   <li>查询 info 表获取本次需要投放的所有卷烟</li>
 *   <li>按 delivery_method 分成标准分配和价位段分配两个集合</li>
 *   <li>分别调用对应的服务处理</li>
 * </ul>
 * </p>
 *
 * @author Robin
 * @since 2025-12-22
 */
@Slf4j
@Service
public class UnifiedAllocationServiceImpl implements UnifiedAllocationService {

    private final StandardAllocationService standardAllocationService;
    private final PriceBandAllocationService priceBandAllocationService;
    private final CigaretteDistributionInfoRepository infoRepository;
    private final PartitionTableManager partitionTableManager;

    public UnifiedAllocationServiceImpl(StandardAllocationService standardAllocationService,
                                        PriceBandAllocationService priceBandAllocationService,
                                        CigaretteDistributionInfoRepository infoRepository,
                                        PartitionTableManager partitionTableManager) {
        this.standardAllocationService = standardAllocationService;
        this.priceBandAllocationService = priceBandAllocationService;
        this.infoRepository = infoRepository;
        this.partitionTableManager = partitionTableManager;
    }

    /**
     * 生成分配方案（统一入口）。
     * <p>
     * 处理流程：
     * <ol>
     *   <li>查询 info 表获取所有待分配卷烟</li>
     *   <li>按 delivery_method 分成标准分配和价位段分配两个集合</li>
     *   <li>执行标准分配（GRADE、GRADE_EXTEND 类型）</li>
     *   <li>执行价位段分配（PRICE_SEGMENT 类型）</li>
     * </ol>
     * </p>
     *
     * @param request 分配请求，包含 year、month、weekSeq 等参数
     * @return 分配响应，包含成功标志、处理数量等信息
     * @example
     * <pre>{@code
     * GenerateDistributionPlanRequestDto request = new GenerateDistributionPlanRequestDto();
     * request.setYear(2025);
     * request.setMonth(12);
     * request.setWeekSeq(3);
     * 
     * GenerateDistributionPlanResponseDto response = unifiedAllocationService.generateDistributionPlan(request);
     * if (response.isSuccess()) {
     *     log.info("分配完成，共处理 {} 条卷烟", response.getTotalCigarettes());
     * }
     * }</pre>
     */
    @Override
    public GenerateDistributionPlanResponseDto generateDistributionPlan(GenerateDistributionPlanRequestDto request) {
        log.info("【统一分配】开始执行: {}-{}-{}", request.getYear(), request.getMonth(), request.getWeekSeq());

        // 1. 查询 info 表获取所有待分配卷烟
        partitionTableManager.ensurePartitionExists("cigarette_distribution_info",
                request.getYear(), request.getMonth(), request.getWeekSeq());
        
        List<Map<String, Object>> allCigarettes = queryAllCigarettes(request);
        log.info("【统一分配】查询到 {} 条卷烟数据", allCigarettes.size());

        // 2. 按 delivery_method 分成两个集合（使用 DeliveryMethodType 枚举判断）
        List<Map<String, Object>> standardList = new ArrayList<>();
        List<Map<String, Object>> priceBandList = new ArrayList<>();
        
        for (Map<String, Object> cig : allCigarettes) {
            String deliveryMethod = getDeliveryMethod(cig);
            Optional<DeliveryMethodType> methodType = DeliveryMethodType.from(deliveryMethod);
            
            if (!methodType.isPresent()) {
                log.warn("【统一分配】未知的投放方式: {}, 卷烟: {}", deliveryMethod, cig.get("CIG_CODE"));
                continue;
            }
            
            switch (methodType.get()) {
                case GRADE:
                case GRADE_EXTEND:
                    standardList.add(cig);
                    break;
                case PRICE_SEGMENT:
                    priceBandList.add(cig);
                    break;
                default:
                    log.debug("【统一分配】跳过未实现的投放方式: {}, 卷烟: {}", deliveryMethod, cig.get("CIG_CODE"));
            }
        }
        
        log.info("【统一分配】分流完成 - 标准分配: {} 条, 价位段分配: {} 条", 
                standardList.size(), priceBandList.size());

        // 3. 执行标准分配
        GenerateDistributionPlanResponseDto response = null;
        if (!standardList.isEmpty()) {
            log.info("【统一分配】执行标准分配...");
            response = standardAllocationService.generateDistributionPlan(request, standardList);
        } else {
            log.info("【统一分配】无标准分配数据，跳过");
            response = createEmptyResponse(request);
        }

        // 4. 执行价位段分配并合并统计信息
        int priceBandProcessed = 0;
        if (!priceBandList.isEmpty()) {
            log.info("【统一分配】执行价位段分配...");
            try {
                priceBandProcessed = priceBandAllocationService.allocateForPriceBand(priceBandList, 
                        request.getYear(), request.getMonth(), request.getWeekSeq());
                log.info("【统一分配】价位段分配完成，处理 {} 条", priceBandProcessed);
                
                // 合并统计信息
                if (response.getTotalCigarettes() != null) {
                    response.setTotalCigarettes(response.getTotalCigarettes() + priceBandProcessed);
                } else {
                    response.setTotalCigarettes(priceBandProcessed);
                }
                
                if (response.getSuccessfulAllocations() != null) {
                    response.setSuccessfulAllocations(response.getSuccessfulAllocations() + priceBandProcessed);
                } else {
                    response.setSuccessfulAllocations(priceBandProcessed);
                }
                
                // 更新消息
                if (response.getMessage() != null && !response.getMessage().equals("无标准分配数据")) {
                    response.setMessage(response.getMessage() + "; 价位段分配: " + priceBandProcessed + " 条");
                } else if (priceBandProcessed > 0) {
                    response.setMessage("价位段分配成功: " + priceBandProcessed + " 条");
                }
            } catch (Exception e) {
                log.error("【统一分配】价位段分配失败: {}", e.getMessage(), e);
                if (response.getMessage() != null) {
                    response.setMessage(response.getMessage() + "; 价位段分配失败: " + e.getMessage());
                }
            }
        } else {
            log.info("【统一分配】无价位段分配数据，跳过");
        }

        log.info("【统一分配】执行完成: {}-{}-{}, 标准: {}, 价位段: {}", 
                request.getYear(), request.getMonth(), request.getWeekSeq(),
                standardList.size(), priceBandProcessed);
        return response;
    }

    /**
     * 查询所有待分配卷烟。
     *
     * @param request 分配请求
     * @return 卷烟数据列表，每个 Map 包含 CIG_CODE、DELIVERY_METHOD 等字段
     */
    private List<Map<String, Object>> queryAllCigarettes(GenerateDistributionPlanRequestDto request) {
        QueryWrapper<CigaretteDistributionInfoPO> query = new QueryWrapper<>();
        query.eq("YEAR", request.getYear())
                .eq("MONTH", request.getMonth())
                .eq("WEEK_SEQ", request.getWeekSeq())
                .orderByAsc("CIG_CODE", "CIG_NAME");
        return infoRepository.selectMaps(query);
    }

    /**
     * 获取卷烟的投放方式。
     *
     * @param cig 卷烟数据 Map
     * @return 投放方式字符串，如 "按档位投放"、"按价位段自选投放"
     */
    private String getDeliveryMethod(Map<String, Object> cig) {
        Object value = cig.get("DELIVERY_METHOD");
        if (value == null) {
            value = cig.get("delivery_method");
        }
        return value != null ? value.toString() : null;
    }

    /**
     * 创建空响应（无标准分配数据时使用）。
     *
     * @param request 分配请求
     * @return 空响应对象
     */
    private GenerateDistributionPlanResponseDto createEmptyResponse(GenerateDistributionPlanRequestDto request) {
        GenerateDistributionPlanResponseDto response = new GenerateDistributionPlanResponseDto();
        response.setYear(request.getYear());
        response.setMonth(request.getMonth());
        response.setWeekSeq(request.getWeekSeq());
        response.setSuccess(true);
        response.setMessage("无标准分配数据");
        response.setTotalCigarettes(0);
        response.setSuccessfulAllocations(0);
        return response;
    }
}
