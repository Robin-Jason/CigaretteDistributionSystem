package org.example.application.service.calculate;

import org.example.application.dto.allocation.GenerateDistributionPlanRequestDto;
import org.example.application.dto.allocation.GenerateDistributionPlanResponseDto;
import org.example.application.dto.allocation.TotalActualDeliveryResponseDto;
import org.example.infrastructure.persistence.po.CigaretteDistributionPredictionPO;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * 标准分配服务接口（按档位投放、按档位扩展投放）。
 * <p>
 * 核心职责：编排分配算法、写回分配矩阵并提供统计接口。
 * </p>
 *
 * @author Robin
 * @version 3.0
 * @since 2025-10-10
 */
public interface StandardAllocationService {

    /**
     * 卷烟总投放量统计计算。
     * <p>
     * 对传入的卷烟分配数据列表按卷烟维度分组统计，计算每种卷烟的总实际投放量。
     * 支持跨区域的投放量汇总，为业务决策提供统计数据支持。
     * </p>
     *
     * @param rawDataList 原始卷烟分配数据列表（必填，包含各区域的投放记录）
     * @return 卷烟统计 Map，key 为 "卷烟代码_卷烟名称"，value 为总实际投放量
     */
    Map<String, BigDecimal> calculateTotalActualDeliveryByTobacco(List<CigaretteDistributionPredictionPO> rawDataList);

    /**
     * 一键生成分配方案（完整流程，内部查询 info 表）。
     *
     * @param request 一键生成分配方案请求 DTO
     * @return 一键生成分配方案响应 DTO
     * @deprecated 建议使用 {@link #generateDistributionPlan(GenerateDistributionPlanRequestDto, List)} 接收已过滤的卷烟列表
     */
    @Deprecated
    GenerateDistributionPlanResponseDto generateDistributionPlan(GenerateDistributionPlanRequestDto request);

    /**
     * 生成分配方案（接收已过滤的标准分配卷烟列表）。
     * <p>
     * 由 UnifiedAllocationService 调用，传入已按 delivery_method 过滤的标准分配卷烟列表。
     * </p>
     *
     * @param request        一键生成分配方案请求 DTO
     * @param cigaretteList  已过滤的标准分配卷烟列表（按档位投放、按档位扩展投放）
     * @return 一键生成分配方案响应 DTO
     */
    GenerateDistributionPlanResponseDto generateDistributionPlan(GenerateDistributionPlanRequestDto request, 
                                                                  List<Map<String, Object>> cigaretteList);

    /**
     * 计算指定时间范围内所有卷烟的总实际投放量。
     *
     * @param year    年份
     * @param month   月份
     * @param weekSeq 周序号
     * @return 总实际投放量计算响应 DTO
     */
    TotalActualDeliveryResponseDto calculateTotalActualDelivery(Integer year, Integer month, Integer weekSeq);
}
