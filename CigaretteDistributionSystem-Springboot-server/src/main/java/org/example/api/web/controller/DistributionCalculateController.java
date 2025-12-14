package org.example.api.web.controller;

import lombok.extern.slf4j.Slf4j;
import org.example.api.web.converter.DistributionCalculateConverter;
import org.example.api.web.vo.request.GenerateDistributionPlanRequestVo;
import org.example.api.web.vo.response.ApiResponseVo;
import org.example.api.web.vo.response.GenerateDistributionPlanResponseVo;
import org.example.api.web.vo.response.TotalActualDeliveryResponseVo;
import org.example.application.dto.GenerateDistributionPlanRequestDto;
import org.example.application.dto.GenerateDistributionPlanResponseDto;
import org.example.application.dto.TotalActualDeliveryResponseDto;
import org.example.application.service.DistributionCalculateService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;

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
    
    @Autowired
    private DistributionCalculateConverter converter;
    
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
     * @param requestVo 生成分配计划请求VO
     * @return 统一格式的API响应
     *
     * @example POST /api/calculate/generate-distribution-plan
     * {
     *   "year": 2025,
     *   "month": 9,
     *   "weekSeq": 3,
     *   "urbanRatio": 0.6,
     *   "ruralRatio": 0.4
     * }
     */
    @PostMapping("/generate-distribution-plan")
    public ResponseEntity<ApiResponseVo<GenerateDistributionPlanResponseVo>> generateDistributionPlan(
            @Valid @RequestBody GenerateDistributionPlanRequestVo requestVo) {
        
        log.info("接收一键生成分配方案请求，年份: {}, 月份: {}, 周序号: {}", 
                requestVo.getYear(), requestVo.getMonth(), requestVo.getWeekSeq());
        if (requestVo.getUrbanRatio() != null && requestVo.getRuralRatio() != null) {
            log.info("接收市场类型比例参数 - 城网: {}, 农网: {}", 
                    requestVo.getUrbanRatio(), requestVo.getRuralRatio());
        }
        
        try {
            // VO 转 DTO
            GenerateDistributionPlanRequestDto requestDto = converter.toDto(requestVo);
            
            // 调用Service层
            GenerateDistributionPlanResponseDto responseDto = distributionService.generateDistributionPlan(requestDto);
            
            // DTO 转 VO
            GenerateDistributionPlanResponseVo responseVo = converter.toVo(responseDto);
            
            // 返回统一格式的响应
            if (responseDto.isSuccess()) {
                return ResponseEntity.ok(ApiResponseVo.success(responseVo, responseDto.getMessage()));
            } else {
                return ResponseEntity.ok(ApiResponseVo.error(
                    responseDto.getMessage() != null ? responseDto.getMessage() : "生成分配计划失败",
                    responseDto.getError() != null ? responseDto.getError() : "GENERATION_FAILED"
                ));
            }
            
        } catch (Exception e) {
            log.error("一键生成分配方案失败", e);
            return ResponseEntity.ok(ApiResponseVo.error(
                "一键生成分配方案失败: " + e.getMessage(), 
                "GENERATION_FAILED"
            ));
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
     * @return 统一格式的API响应
     *
     * @example POST /api/calculate/total-actual-delivery?year=2025&month=9&weekSeq=3
     */
    @PostMapping("/total-actual-delivery")
    public ResponseEntity<ApiResponseVo<TotalActualDeliveryResponseVo>> calculateTotalActualDelivery(
            @RequestParam Integer year,
                                                                           @RequestParam Integer month,
                                                                           @RequestParam Integer weekSeq) {
        log.info("接收总实际投放量计算请求，年份: {}, 月份: {}, 周序号: {}", year, month, weekSeq);
        
        try {
            // 调用Service层
            TotalActualDeliveryResponseDto responseDto = distributionService.calculateTotalActualDelivery(year, month, weekSeq);
            
            // DTO 转 VO
            TotalActualDeliveryResponseVo responseVo = converter.toVo(responseDto);
            
            // 返回统一格式的响应（即使失败也返回200，但success=false）
            if (responseDto.isSuccess()) {
                return ResponseEntity.ok(ApiResponseVo.success(responseVo, responseDto.getMessage()));
            } else {
                return ResponseEntity.ok(ApiResponseVo.error(
                    responseDto.getMessage() != null ? responseDto.getMessage() : "计算总实际投放量失败",
                    "CALCULATION_FAILED"
                ));
            }
            
        } catch (Exception e) {
            log.error("总实际投放量计算失败", e);
            return ResponseEntity.ok(ApiResponseVo.error(
                "总实际投放量计算失败: " + e.getMessage(), 
                "INTERNAL_ERROR"
            ));
        }
    }
}

