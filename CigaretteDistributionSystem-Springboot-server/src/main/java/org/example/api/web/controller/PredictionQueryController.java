package org.example.api.web.controller;

import lombok.extern.slf4j.Slf4j;
import org.example.application.service.query.PredictionQueryService;
import org.example.shared.util.ApiResponses;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * 预测分区数据查询控制器
 *
 * 作用：
 * - 提供预测分区（普通/价位段）的按时间查询接口。
 *
 * 特性：
 * - 仅查询，不修改；路径前缀 /api/prediction。
 *
 * @author Robin
 * @version 1.0
 * @since 2025-11-30
 */
@Slf4j
@RestController
@RequestMapping("/api/prediction")
@CrossOrigin
public class PredictionQueryController {

    @Autowired
    private PredictionQueryService predictionQueryService;

    /**
     * 按时间分区查询预测数据。
     *
     * 返回字段顺序：卷烟代码，卷烟名称，投放信息编码，投放方式，扩展投放方式，投放区域，标签，标签过滤值，D30-D1，BZ。
     *
     * @param year    年份
     * @param month   月份
     * @param weekSeq 周序号
     * @return 预测数据列表
     *
     * @example GET /api/prediction/list-by-time?year=2025&month=9&weekSeq=3
     */
    @GetMapping("/list-by-time")
    public ResponseEntity<?> listByTime(@RequestParam Integer year,
                                        @RequestParam Integer month,
                                        @RequestParam Integer weekSeq) {
        try {
            log.info("查询预测分区数据，year={}, month={}, weekSeq={}", year, month, weekSeq);
            List<Map<String, Object>> data = predictionQueryService.listByTime(year, month, weekSeq);
            return ResponseEntity.ok(data);
        } catch (Exception e) {
            log.error("查询预测分区数据失败", e);
            return ApiResponses.internalError("查询预测分区数据失败: " + e.getMessage(), "INTERNAL_ERROR");
        }
    }

    /**
     * 按时间分区查询价位段预测数据（单独展示，不与普通分区表合并）。
     *
     * @param year    年份
     * @param month   月份
     * @param weekSeq 周序号
     * @return 价位段预测数据列表
     *
     * @example GET /api/prediction/list-price-by-time?year=2025&month=9&weekSeq=3
     */
    @GetMapping("/list-price-by-time")
    public ResponseEntity<?> listPriceByTime(@RequestParam Integer year,
                                             @RequestParam Integer month,
                                             @RequestParam Integer weekSeq) {
        try {
            log.info("查询价位段预测分区数据，year={}, month={}, weekSeq={}", year, month, weekSeq);
            List<Map<String, Object>> data = predictionQueryService.listPriceByTime(year, month, weekSeq);
            return ResponseEntity.ok(data);
        } catch (Exception e) {
            log.error("查询价位段预测分区数据失败", e);
            return ApiResponses.internalError("查询价位段预测分区数据失败: " + e.getMessage(), "INTERNAL_ERROR");
        }
    }
}

