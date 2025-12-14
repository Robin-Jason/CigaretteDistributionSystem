package org.example.api.web.controller;

import lombok.extern.slf4j.Slf4j;
import org.example.application.dto.GenerateDistributionPlanRequestDto;
import org.example.application.dto.GenerateDistributionPlanResponseDto;
import org.example.application.dto.TotalActualDeliveryResponseDto;
import org.example.application.service.DistributionCalculateService;
import org.example.shared.util.ApiResponses;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.Map;

/**
 * 分配计算控制器
 *
 * 作用：
 * - 提供一键生成分配方案的入口，触发算法计算并写回预测表。
 * - 提供分区级总实际投放量汇总的查询入口，用于对账/看板。
 *
 * 特性：
 * - 仅暴露计算/汇总接口，不承载增删改查的细粒度操作。
 * - 接口遵循 REST 风格，返回统一的 result map（包含 success/message/code 等）。
 *
 * @author Robin
 * @version 1.0
 * @since 2025-11-30
 */
@Slf4j
@RestController
@RequestMapping("/api/calculate")
@CrossOrigin(origins = "*")
public class DistributionCalculateController {
    
    @Autowired
    private DistributionCalculateService distributionService;
    
    /**
     * 一键生成分配方案
     *
     * 功能：全量重建指定分区（year/month/weekSeq）的卷烟分配记录，并写回预测表。
     * 适用：运营一键重跑、定时任务补数、全量回灌。
     *
     * 注意：
     * - 仅做"全量重建"，不提供增删改查的局部操作。
     * - 市场类型比例仅在"档位+市场类型"扩展投放时生效，其他组合忽略该参数。
     *
     * @param year        年份（必填）
     * @param month       月份（必填）
     * @param weekSeq     周序号（必填）
     * @param urbanRatio  城网比例（可选，仅档位+市场类型生效）
     * @param ruralRatio  农网比例（可选，仅档位+市场类型生效）
     * @return 成功时返回 success=true 的结果映射；失败返回 500 且包含错误码
     *
     * @example POST /api/calculate/generate-distribution-plan?year=2025&month=9&weekSeq=3
     */
    @PostMapping("/generate-distribution-plan")
    public ResponseEntity<Map<String, Object>> generateDistributionPlan(
            @RequestParam Integer year,
            @RequestParam Integer month,
            @RequestParam Integer weekSeq,
            @RequestParam(required = false) BigDecimal urbanRatio,
            @RequestParam(required = false) BigDecimal ruralRatio) {
        
        log.info("接收一键生成分配方案请求，年份: {}, 月份: {}, 周序号: {}", year, month, weekSeq);
        if (urbanRatio != null && ruralRatio != null) {
            log.info("接收市场类型比例参数 - 城网: {}, 农网: {}", urbanRatio, ruralRatio);
        }
        
        try {
            // 构建请求DTO
            GenerateDistributionPlanRequestDto request = new GenerateDistributionPlanRequestDto();
            request.setYear(year);
            request.setMonth(month);
            request.setWeekSeq(weekSeq);
            request.setUrbanRatio(urbanRatio);
            request.setRuralRatio(ruralRatio);
            
            // 调用Service层，所有业务逻辑都在Service层
            GenerateDistributionPlanResponseDto result = distributionService.generateDistributionPlan(request);
            
            // 根据Service返回的结果构建HTTP响应
            if (result.isSuccess()) {
                return ResponseEntity.ok(result.toMap());
            } else {
                return ResponseEntity.internalServerError().body(result.toMap());
            }
            
        } catch (Exception e) {
            log.error("一键生成分配方案失败", e);
            return ApiResponses.internalError("一键生成分配方案失败: " + e.getMessage(), "GENERATION_FAILED");
        }
    }

    /**
     * 计算指定分区（year/month/weekSeq）内所有卷烟的总实际投放量。
     *
     * 用途：对账/看板汇总，验证分配写回结果。
     *
     * @param year    年份（必填）
     * @param month   月份（必填）
     * @param weekSeq 周序号（必填）
     * @return 汇总结果映射，包含 success 标识与总量；失败仍返回 200 但 success=false
     *
     * @example POST /api/calculate/total-actual-delivery?year=2025&month=9&weekSeq=3
     */
    @PostMapping("/total-actual-delivery")
    public ResponseEntity<Map<String, Object>> calculateTotalActualDelivery(@RequestParam Integer year,
                                                                           @RequestParam Integer month,
                                                                           @RequestParam Integer weekSeq) {
        log.info("接收总实际投放量计算请求，年份: {}, 月份: {}, 周序号: {}", year, month, weekSeq);
        
        try {
            // 调用Service层，所有业务逻辑都在Service层
            TotalActualDeliveryResponseDto result = distributionService.calculateTotalActualDelivery(year, month, weekSeq);
            
            // 根据Service返回的结果构建HTTP响应
            if (result.isSuccess()) {
                return ResponseEntity.ok(result.toMap());
            } else {
                return ResponseEntity.ok(result.toMap()); // 即使失败也返回200，但success=false
            }
            
        } catch (Exception e) {
            log.error("总实际投放量计算失败", e);
            return ApiResponses.internalError("总实际投放量计算失败: " + e.getMessage(), "INTERNAL_ERROR");
        }
    }
}

