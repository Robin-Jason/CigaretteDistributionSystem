package org.example.api.web.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.api.web.converter.PredictionConverter;
import org.example.api.web.vo.request.AddRegionAllocationRequestVo;
import org.example.api.web.vo.request.DeleteCigaretteRequestVo;
import org.example.api.web.vo.request.DeleteRegionAllocationRequestVo;
import org.example.api.web.vo.request.UpdateRegionGradesRequestVo;
import org.example.api.web.vo.response.ApiResponseVo;
import org.example.api.web.vo.response.PredictionQueryResponseVo;
import org.example.application.service.encode.AggregatedEncodingQueryService;
import org.example.application.service.prediction.PredictionAddService;
import org.example.application.service.prediction.PredictionDeleteService;
import org.example.application.service.prediction.PredictionQueryService;
import org.example.application.service.prediction.PredictionUpdateService;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 预测分配数据控制器
 * <p>
 * 提供对 prediction 和 prediction_price 分区表的增删改查接口。
 * </p>
 *
 * @author Robin
 * @since 2025-12-22
 */
@Slf4j
@RestController
@RequestMapping("/api/prediction")
@CrossOrigin
@RequiredArgsConstructor
public class PredictionController {

    private final PredictionAddService predictionAddService;
    private final PredictionDeleteService predictionDeleteService;
    private final PredictionUpdateService predictionUpdateService;
    private final PredictionQueryService predictionQueryService;
    private final PredictionConverter converter;
    private final AggregatedEncodingQueryService aggregatedEncodingQueryService;

    // ==================== 查询接口 ====================

    /**
     * 按时间分区查询预测数据。
     */
    @GetMapping("/list-by-time")
    public ResponseEntity<ApiResponseVo<PredictionQueryResponseVo>> listByTime(
            @RequestParam Integer year,
            @RequestParam Integer month,
            @RequestParam Integer weekSeq) {
        try {
            log.info("查询预测分区数据，year={}, month={}, weekSeq={}", year, month, weekSeq);
            List<Map<String, Object>> dataList = predictionQueryService.listByTime(year, month, weekSeq);
            PredictionQueryResponseVo responseVo = converter.toQueryResponseVo(dataList);
            return ResponseEntity.ok(ApiResponseVo.success(responseVo, "查询成功"));
        } catch (Exception e) {
            log.error("查询预测分区数据失败", e);
            return ResponseEntity.ok(ApiResponseVo.error("查询预测分区数据失败: " + e.getMessage(), "INTERNAL_ERROR"));
        }
    }

    /**
     * 按时间分区查询价位段预测数据。
     */
    @GetMapping("/list-price-by-time")
    public ResponseEntity<ApiResponseVo<PredictionQueryResponseVo>> listPriceByTime(
            @RequestParam Integer year,
            @RequestParam Integer month,
            @RequestParam Integer weekSeq) {
        try {
            log.info("查询价位段预测分区数据，year={}, month={}, weekSeq={}", year, month, weekSeq);
            List<Map<String, Object>> dataList = predictionQueryService.listPriceByTime(year, month, weekSeq);
            PredictionQueryResponseVo responseVo = converter.toQueryResponseVo(dataList);
            return ResponseEntity.ok(ApiResponseVo.success(responseVo, "查询成功"));
        } catch (Exception e) {
            log.error("查询价位段预测分区数据失败", e);
            return ResponseEntity.ok(ApiResponseVo.error("查询价位段预测分区数据失败: " + e.getMessage(), "INTERNAL_ERROR"));
        }
    }

    /**
     * 懒加载：按批次 + 卷烟代码查询"多区域聚合编码表达式"。
     */
    @GetMapping("/aggregated-encodings")
    public ResponseEntity<ApiResponseVo<List<String>>> aggregatedEncodings(
            @RequestParam Integer year,
            @RequestParam Integer month,
            @RequestParam Integer weekSeq,
            @RequestParam String cigCode) {
        try {
            log.info("查询聚合编码表达式，year={}, month={}, weekSeq={}, cigCode={}", year, month, weekSeq, cigCode);
            List<String> expressions = aggregatedEncodingQueryService.listAggregatedEncodings(year, month, weekSeq, cigCode);
            return ResponseEntity.ok(ApiResponseVo.success(expressions, "查询成功"));
        } catch (Exception e) {
            log.error("查询聚合编码表达式失败", e);
            return ResponseEntity.ok(ApiResponseVo.error("查询聚合编码表达式失败: " + e.getMessage(), "INTERNAL_ERROR"));
        }
    }

    // ==================== 新增接口 ====================

    /**
     * 新增投放区域分配记录。
     */
    @PostMapping("/add-region-allocation")
    public ResponseEntity<ApiResponseVo<Void>> addRegionAllocation(
            @Validated @RequestBody AddRegionAllocationRequestVo request) {
        try {
            log.info("新增投放区域分配记录请求: year={}, month={}, weekSeq={}, cigCode={}, primaryRegion={}, secondaryRegion={}",
                    request.getYear(), request.getMonth(), request.getWeekSeq(),
                    request.getCigCode(), request.getPrimaryRegion(), request.getSecondaryRegion());
            predictionAddService.addRegionAllocation(converter.toAddDto(request));
            return ResponseEntity.ok(ApiResponseVo.success(null, "新增投放区域分配记录成功"));
        } catch (IllegalArgumentException e) {
            log.warn("新增投放区域分配记录参数校验失败: {}", e.getMessage());
            return ResponseEntity.ok(ApiResponseVo.error(e.getMessage(), "VALIDATION_ERROR"));
        } catch (IllegalStateException e) {
            log.warn("新增投放区域分配记录业务校验失败: {}", e.getMessage());
            return ResponseEntity.ok(ApiResponseVo.error(e.getMessage(), "BUSINESS_ERROR"));
        } catch (Exception e) {
            log.error("新增投放区域分配记录失败", e);
            return ResponseEntity.ok(ApiResponseVo.error("新增投放区域分配记录失败: " + e.getMessage(), "INTERNAL_ERROR"));
        }
    }

    // ==================== 删除接口 ====================

    /**
     * 删除指定卷烟的特定区域分配记录。
     */
    @DeleteMapping("/delete-region-allocation")
    public ResponseEntity<ApiResponseVo<Void>> deleteRegionAllocation(
            @Validated @RequestBody DeleteRegionAllocationRequestVo request) {
        try {
            log.info("删除特定区域分配记录请求: year={}, month={}, weekSeq={}, cigCode={}, cigName={}, primaryRegion={}, secondaryRegion={}",
                    request.getYear(), request.getMonth(), request.getWeekSeq(),
                    request.getCigCode(), request.getCigName(),
                    request.getPrimaryRegion(), request.getSecondaryRegion());
            predictionDeleteService.deleteRegionAllocation(converter.toDeleteRegionDto(request));
            return ResponseEntity.ok(ApiResponseVo.success(null, "删除区域分配记录成功"));
        } catch (IllegalArgumentException e) {
            log.warn("删除区域分配记录参数校验失败: {}", e.getMessage());
            return ResponseEntity.ok(ApiResponseVo.error(e.getMessage(), "VALIDATION_ERROR"));
        } catch (IllegalStateException e) {
            log.warn("删除区域分配记录业务校验失败: {}", e.getMessage());
            return ResponseEntity.ok(ApiResponseVo.error(e.getMessage(), "BUSINESS_ERROR"));
        } catch (Exception e) {
            log.error("删除区域分配记录失败", e);
            return ResponseEntity.ok(ApiResponseVo.error("删除区域分配记录失败: " + e.getMessage(), "INTERNAL_ERROR"));
        }
    }

    /**
     * 删除指定卷烟的所有区域分配记录。
     */
    @DeleteMapping("/delete-cigarette")
    public ResponseEntity<ApiResponseVo<Void>> deleteCigarette(
            @Validated @RequestBody DeleteCigaretteRequestVo request) {
        try {
            log.info("删除整个卷烟分配记录请求: year={}, month={}, weekSeq={}, cigCode={}, cigName={}",
                    request.getYear(), request.getMonth(), request.getWeekSeq(),
                    request.getCigCode(), request.getCigName());
            predictionDeleteService.deleteCigarette(converter.toDeleteCigaretteDto(request));
            return ResponseEntity.ok(ApiResponseVo.success(null, "删除卷烟分配记录成功"));
        } catch (IllegalArgumentException e) {
            log.warn("删除卷烟分配记录参数校验失败: {}", e.getMessage());
            return ResponseEntity.ok(ApiResponseVo.error(e.getMessage(), "VALIDATION_ERROR"));
        } catch (IllegalStateException e) {
            log.warn("删除卷烟分配记录业务校验失败: {}", e.getMessage());
            return ResponseEntity.ok(ApiResponseVo.error(e.getMessage(), "BUSINESS_ERROR"));
        } catch (Exception e) {
            log.error("删除卷烟分配记录失败", e);
            return ResponseEntity.ok(ApiResponseVo.error("删除卷烟分配记录失败: " + e.getMessage(), "INTERNAL_ERROR"));
        }
    }

    // ==================== 修改接口 ====================

    /**
     * 修改指定卷烟特定区域的档位值。
     */
    @PutMapping("/update-region-grades")
    public ResponseEntity<ApiResponseVo<Void>> updateRegionGrades(
            @Validated @RequestBody UpdateRegionGradesRequestVo request) {
        try {
            log.info("修改区域档位值请求: year={}, month={}, weekSeq={}, cigCode={}, cigName={}, primaryRegion={}, secondaryRegion={}",
                    request.getYear(), request.getMonth(), request.getWeekSeq(),
                    request.getCigCode(), request.getCigName(),
                    request.getPrimaryRegion(), request.getSecondaryRegion());
            predictionUpdateService.updateRegionGrades(converter.toUpdateDto(request));
            return ResponseEntity.ok(ApiResponseVo.success(null, "修改区域档位值成功"));
        } catch (IllegalArgumentException e) {
            log.warn("修改区域档位值参数校验失败: {}", e.getMessage());
            return ResponseEntity.ok(ApiResponseVo.error(e.getMessage(), "VALIDATION_ERROR"));
        } catch (IllegalStateException e) {
            log.warn("修改区域档位值业务校验失败: {}", e.getMessage());
            return ResponseEntity.ok(ApiResponseVo.error(e.getMessage(), "BUSINESS_ERROR"));
        } catch (Exception e) {
            log.error("修改区域档位值失败", e);
            return ResponseEntity.ok(ApiResponseVo.error("修改区域档位值失败: " + e.getMessage(), "INTERNAL_ERROR"));
        }
    }

}
