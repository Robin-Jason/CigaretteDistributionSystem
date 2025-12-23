package org.example.api.web.controller;

import lombok.extern.slf4j.Slf4j;
import org.example.api.web.converter.CigaretteStrategyAdjustConverter;
import org.example.api.web.converter.DistributionCalculateConverter;
import org.example.api.web.converter.GetAvailableRegionsConverter;
import org.example.api.web.vo.request.AdjustCigaretteStrategyRequestVo;
import org.example.api.web.vo.request.GenerateDistributionPlanRequestVo;
import org.example.api.web.vo.request.GetAvailableRegionsRequestVo;
import org.example.api.web.vo.response.AdjustCigaretteStrategyResponseVo;
import org.example.api.web.vo.response.ApiResponseVo;
import org.example.api.web.vo.response.GenerateDistributionPlanResponseVo;
import org.example.api.web.vo.response.GetAvailableRegionsResponseVo;
import org.example.api.web.vo.response.TotalActualDeliveryResponseVo;
import org.example.application.dto.allocation.AdjustCigaretteStrategyRequestDto;
import org.example.application.dto.allocation.AdjustCigaretteStrategyResponseDto;
import org.example.application.dto.allocation.GenerateDistributionPlanRequestDto;
import org.example.application.dto.allocation.GenerateDistributionPlanResponseDto;
import org.example.application.dto.allocation.GetAvailableRegionsRequestDto;
import org.example.application.dto.allocation.GetAvailableRegionsResponseDto;
import org.example.application.dto.allocation.TotalActualDeliveryResponseDto;
import org.example.application.service.adjust.CigaretteStrategyAdjustService;
import org.example.application.service.calculate.StandardAllocationService;
import org.example.application.service.calculate.UnifiedAllocationService;
import org.example.application.service.region.GetAvailableRegionsService;
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
    private StandardAllocationService standardAllocationService;
    
    @Autowired
    private UnifiedAllocationService unifiedAllocationService;
    
    @Autowired
    private CigaretteStrategyAdjustService adjustService;
    
    @Autowired
    private GetAvailableRegionsService getAvailableRegionsService;
    
    @Autowired
    private DistributionCalculateConverter converter;
    
    @Autowired
    private CigaretteStrategyAdjustConverter adjustConverter;
    
    @Autowired
    private GetAvailableRegionsConverter regionsConverter;
    
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
            
            // 调用统一分配服务（协调标准分配和价位段分配）
            GenerateDistributionPlanResponseDto responseDto = unifiedAllocationService.generateDistributionPlan(requestDto);
            
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
            // 调用标准分配服务计算总实际投放量
            TotalActualDeliveryResponseDto responseDto = standardAllocationService.calculateTotalActualDelivery(year, month, weekSeq);
            
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

    /**
     * 调整卷烟投放策略并重新生成分配方案
     *
     * 功能：
     * 1. 删除指定卷烟的旧分配记录
     * 2. 根据新投放组合重新执行分配算法
     * 3. 将新分配结果写回数据库
     * 4. 更新 Info 表备注为"已人工调整策略?"
     *
     * @param requestVo 调整请求VO
     * @return 统一格式的API响应，成功返回新分配记录，失败返回错误信息
     *
     * @example POST /api/calculate/adjust-strategy
     * {
     *   "year": 2025,
     *   "month": 9,
     *   "weekSeq": 3,
     *   "cigCode": "42010020",
     *   "cigName": "红金龙(硬神州腾龙)",
     *   "newDeliveryMethod": "按档位投放",
     *   "newDeliveryEtype": null,
     *   "newTag": "优质数据共享客户",
     *   "newTagFilterValue": "是",
     *   "newAdvAmount": 1000
     * }
     */
    @PostMapping("/adjust-strategy")
    public ResponseEntity<ApiResponseVo<AdjustCigaretteStrategyResponseVo>> adjustCigaretteStrategy(
            @Valid @RequestBody AdjustCigaretteStrategyRequestVo requestVo) {
        
        log.info("接收卷烟投放策略调整请求，年份: {}, 月份: {}, 周序号: {}, 卷烟: {}-{}, 新投放类型: {}", 
                requestVo.getYear(), requestVo.getMonth(), requestVo.getWeekSeq(),
                requestVo.getCigCode(), requestVo.getCigName(), requestVo.getNewDeliveryMethod());
        
        try {
            // VO 转 DTO
            AdjustCigaretteStrategyRequestDto requestDto = adjustConverter.toDto(requestVo);
            
            // 调用Service层
            AdjustCigaretteStrategyResponseDto responseDto = adjustService.adjustStrategy(requestDto);
            
            // DTO 转 VO
            AdjustCigaretteStrategyResponseVo responseVo = adjustConverter.toVo(responseDto);
            
            // 返回统一格式的响应
            if (responseDto.getSuccess()) {
                return ResponseEntity.ok(ApiResponseVo.success(responseVo, responseDto.getMessage()));
            } else {
                return ResponseEntity.ok(ApiResponseVo.error(
                    responseDto.getMessage() != null ? responseDto.getMessage() : "调整卷烟投放策略失败",
                    "ADJUST_FAILED"
                ));
            }
            
        } catch (Exception e) {
            log.error("调整卷烟投放策略失败", e);
            return ResponseEntity.ok(ApiResponseVo.error(
                "调整卷烟投放策略失败: " + e.getMessage(), 
                "INTERNAL_ERROR"
            ));
        }
    }

    /**
     * 获取可用投放区域列表
     *
     * 功能：
     * 1. 根据投放类型和扩展类型解析出所有可能的区域
     * 2. 检查 region_customer_statistics 表中哪些区域已存在
     * 3. 对不存在的区域，追加构建区域客户数据
     * 4. 返回完整的区域列表和构建信息
     *
     * 用途：
     * - 为策略调整功能提供可选的区域列表
     * - 确保所有理论区域的客户数据都已构建
     *
     * @param requestVo 请求VO，包含年份、月份、周序号、投放类型、扩展类型
     * @return 统一格式的API响应，包含区域列表、是否构建了新数据、构建的区域列表
     *
     * @example POST /api/calculate/available-regions
     * {
     *   "year": 2025,
     *   "month": 9,
     *   "weekSeq": 3,
     *   "deliveryMethod": "按档位投放",
     *   "deliveryEtype": "区县公司+市场类型"
     * }
     */
    @PostMapping("/available-regions")
    public ResponseEntity<ApiResponseVo<GetAvailableRegionsResponseVo>> getAvailableRegions(
            @Valid @RequestBody GetAvailableRegionsRequestVo requestVo) {
        
        log.info("接收获取可用投放区域列表请求，年份: {}, 月份: {}, 周序号: {}, 投放类型: {}, 扩展类型: {}", 
                requestVo.getYear(), requestVo.getMonth(), requestVo.getWeekSeq(),
                requestVo.getDeliveryMethod(), requestVo.getDeliveryEtype());
        
        try {
            // VO 转 DTO
            GetAvailableRegionsRequestDto requestDto = regionsConverter.toDto(requestVo);
            
            // 调用Service层
            GetAvailableRegionsResponseDto responseDto = getAvailableRegionsService.getAvailableRegions(requestDto);
            
            // DTO 转 VO
            GetAvailableRegionsResponseVo responseVo = regionsConverter.toVo(responseDto);
            
            // 返回统一格式的响应
            if (responseDto.getSuccess()) {
                return ResponseEntity.ok(ApiResponseVo.success(responseVo, responseDto.getMessage()));
            } else {
                return ResponseEntity.ok(ApiResponseVo.error(
                    responseDto.getMessage() != null ? responseDto.getMessage() : "获取可用投放区域列表失败",
                    "GET_REGIONS_FAILED"
                ));
            }
            
        } catch (Exception e) {
            log.error("获取可用投放区域列表失败", e);
            return ResponseEntity.ok(ApiResponseVo.error(
                "获取可用投放区域列表失败: " + e.getMessage(), 
                "INTERNAL_ERROR"
            ));
        }
    }
}

