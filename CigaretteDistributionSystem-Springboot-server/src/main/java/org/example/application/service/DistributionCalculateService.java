package org.example.application.service;

import org.example.application.dto.GenerateDistributionPlanRequestDto;
import org.example.application.dto.GenerateDistributionPlanResponseDto;
import org.example.application.dto.TotalActualDeliveryResponseDto;
import org.example.infrastructure.persistence.po.CigaretteDistributionPredictionPO;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * 分配计算服务接口。
 *
 * <p>核心职责：查询分区数据、编排分配算法、写回分配矩阵并提供统计接口。</p>
 *
 * @author Robin
 * @version 3.0
 * @since 2025-10-10
 */
public interface DistributionCalculateService {
    
    /**
     * 卷烟总投放量统计计算
     * 
     * 对传入的卷烟分配数据列表按卷烟维度分组统计，计算每种卷烟的总实际投放量。
     * 支持跨区域的投放量汇总，为业务决策提供统计数据支持。
     * 
     * @param rawDataList 原始卷烟分配数据列表（必填，包含各区域的投放记录）
     * @return 卷烟统计 Map，key 为 "卷烟代码_卷烟名称"，value 为总实际投放量
     * 
     * @example
     * rawDataList包含多个区域的"黄鹤楼（1916中支）"投放记录
     * -> 按卷烟分组：{"42020181_黄鹤楼（1916中支）": 总投放量}
     * -> 汇总该卷烟在所有区域的实际投放量
     */
    Map<String, BigDecimal> calculateTotalActualDeliveryByTobacco(List<CigaretteDistributionPredictionPO> rawDataList);
    
    /**
     * 一键生成分配方案（完整流程）
     *
     * 包含以下步骤：
     *  * 1. 检查指定日期是否存在分配数据
     *  * 2. 如果存在，删除现有分配数据
     *  * 3. 执行算法分配并按卷烟写回数据库
     *  * 4. 查询生成的分配记录数
     *  * 5. 计算处理时间
     *  * 6. 组装响应数据
     *
     * @param request 一键生成分配方案请求 DTO
     * @return 一键生成分配方案响应 DTO
     */
    GenerateDistributionPlanResponseDto generateDistributionPlan(GenerateDistributionPlanRequestDto request);
    
    /**
     * 计算指定时间范围内所有卷烟的总实际投放量
     * 
     * @param year 年份
     * @param month 月份
     * @param weekSeq 周序号
     * @return 总实际投放量计算响应 DTO
     */
    TotalActualDeliveryResponseDto calculateTotalActualDelivery(Integer year, Integer month, Integer weekSeq);
}