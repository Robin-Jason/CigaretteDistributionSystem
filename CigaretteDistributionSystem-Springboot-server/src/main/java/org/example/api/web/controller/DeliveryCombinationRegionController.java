package org.example.api.web.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.api.web.vo.response.ApiResponseVo;
import org.example.application.service.statistics.DeliveryCombinationRegionService;
import org.example.application.service.statistics.DeliveryCombinationRegionService.DeliveryCombinationRegionResult;
import org.example.shared.util.ParamValidators;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * 投放组合与区域映射查询控制器
 *
 * @author Robin
 * @since 2025-12-22
 */
@Slf4j
@RestController
@RequestMapping("/api/statistics")
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
public class DeliveryCombinationRegionController {

    private final DeliveryCombinationRegionService deliveryCombinationRegionService;

    /**
     * 查询投放组合与区域映射
     * <p>
     * 返回本次分配所包含的所有投放组合及其对应的区域全集。
     * 投放组合由 DELIVERY_METHOD（投放方式）、DELIVERY_ETYPE（扩展类型）、TAG（标签）组成。
     * </p>
     *
     * @param year    年份
     * @param month   月份
     * @param weekSeq 周序号
     * @return 投放组合与区域映射结果
     */
    @GetMapping("/delivery-combination-regions")
    public ResponseEntity<ApiResponseVo<DeliveryCombinationRegionResult>> queryCombinationRegions(
            @RequestParam Integer year,
            @RequestParam Integer month,
            @RequestParam Integer weekSeq) {
        
        log.info("查询投放组合与区域映射: year={}, month={}, weekSeq={}", year, month, weekSeq);
        
        try {
            // 使用工具类进行参数校验
            ParamValidators.validateTimeParams(year, month, weekSeq);
            
            DeliveryCombinationRegionResult result = deliveryCombinationRegionService.queryCombinationRegions(year, month, weekSeq);
            return ResponseEntity.ok(ApiResponseVo.success(result, "查询成功"));
        } catch (IllegalArgumentException e) {
            log.warn("参数校验失败: {}", e.getMessage());
            return ResponseEntity.badRequest().body(ApiResponseVo.error(e.getMessage(), "PARAM_INVALID"));
        } catch (Exception e) {
            log.error("查询投放组合与区域映射失败: {}-{}-{}", year, month, weekSeq, e);
            return ResponseEntity.ok(ApiResponseVo.error("查询失败: " + e.getMessage(), "QUERY_FAILED"));
        }
    }
}
