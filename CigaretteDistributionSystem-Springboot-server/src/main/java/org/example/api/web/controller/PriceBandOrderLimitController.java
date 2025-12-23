package org.example.api.web.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.api.web.vo.response.ApiResponseVo;
import org.example.application.service.statistics.PriceBandOrderLimitService;
import org.example.application.service.statistics.PriceBandOrderLimitService.PriceBandOrderLimitResult;
import org.example.shared.util.ParamValidators;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * 价位段订购量上限查询控制器
 *
 * @author Robin
 * @since 2025-12-22
 */
@Slf4j
@RestController
@RequestMapping("/api/statistics")
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
public class PriceBandOrderLimitController {

    private final PriceBandOrderLimitService priceBandOrderLimitService;

    /**
     * 查询价位段订购量上限
     * <p>
     * 根据按价位段自选投放的分配结果，计算每个价位段各档位的订购量上限。
     * 计算规则：订购量上限 = 价位段单档位投放量之和 / 设定阈值（向下取整）
     * </p>
     *
     * @param year    年份
     * @param month   月份
     * @param weekSeq 周序号
     * @return 价位段订购量上限统计结果
     */
    @GetMapping("/price-band-order-limits")
    public ResponseEntity<ApiResponseVo<PriceBandOrderLimitResult>> queryOrderLimits(
            @RequestParam Integer year,
            @RequestParam Integer month,
            @RequestParam Integer weekSeq) {
        
        log.info("查询价位段订购量上限: year={}, month={}, weekSeq={}", year, month, weekSeq);
        
        try {
            // 使用工具类进行参数校验
            ParamValidators.validateTimeParams(year, month, weekSeq);
            
            PriceBandOrderLimitResult result = priceBandOrderLimitService.queryOrderLimits(year, month, weekSeq);
            return ResponseEntity.ok(ApiResponseVo.success(result, "查询成功"));
        } catch (IllegalArgumentException e) {
            log.warn("参数校验失败: {}", e.getMessage());
            return ResponseEntity.badRequest().body(ApiResponseVo.error(e.getMessage(), "PARAM_INVALID"));
        } catch (Exception e) {
            log.error("查询价位段订购量上限失败: {}-{}-{}", year, month, weekSeq, e);
            return ResponseEntity.ok(ApiResponseVo.error("查询失败: " + e.getMessage(), "QUERY_FAILED"));
        }
    }
}
