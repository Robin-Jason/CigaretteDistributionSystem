package org.example.api.web.controller;

import lombok.extern.slf4j.Slf4j;
import org.example.api.web.converter.ExcelImportConverter;
import org.example.api.web.vo.request.DataImportRequestVo;
import org.example.api.web.vo.response.ApiResponseVo;
import org.example.api.web.vo.response.DataImportResponseVo;
import org.example.application.dto.DataImportRequestDto;
import org.example.application.service.ExcelImportService;
import org.example.shared.util.UploadValidators;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import javax.validation.Valid;
import java.util.Map;

/**
 * 数据导入控制器
 *
 * 作用：
 * - 提供统一导入入口：支持卷烟投放基础信息表必传，客户基础信息表可选的组合导入。
 * - 对上传文件做基础校验（是否存在、大小限制），并将结果交由服务层处理。
 *
 * 特性：
 * - 仅保留统一导入接口 /api/import/data，已移除分表单独导入接口。
 * - 当客户表未提供时，跳过客户表导入并在响应中给出提示。
 *
 * @author Robin
 * @version 1.0
 * @since 2025-11-30
 */
@Slf4j
@RestController
@RequestMapping("/api/import")
@Validated
@CrossOrigin(origins = "*")
public class ExcelImportController {
    
    @Autowired
    private ExcelImportService excelImportService;
    
    @Autowired
    private ExcelImportConverter converter;

    /**
     * 统一数据导入接口（客户表可选，卷烟表必传）。
     *
     * 支持场景：
     * - 全量导入：同时导入客户基础信息表 + 卷烟投放基础信息表。
     * - 快速导入：仅导入卷烟投放基础信息表（客户表缺省时自动跳过）。
     *
     * 参数与校验：
     * - 卷烟表（必传）：校验存在与大小限制。
     * - 客户表（可选）：存在则导入，不传则跳过并在响应中提示"未提供客户基础信息表，本次未更新"。
     * - year/month/weekSeq：用于卷烟表分区定位。
     *
     * 返回：
     * - success=true/false
     * - baseCustomerInfoResult：客户表导入结果或跳过提示
     * - cigaretteDistributionInfoResult：卷烟表导入结果
     */
    @PostMapping("/data")
    public ResponseEntity<ApiResponseVo<DataImportResponseVo>> importData(@Valid DataImportRequestVo requestVo) {
        log.info("接收统一数据导入请求，年份: {}, 月份: {}, 周序号: {}",
                requestVo.getYear(), requestVo.getMonth(), requestVo.getWeekSeq());

        try {
            // 文件校验
            ResponseEntity<Map<String, Object>> cigValidation = UploadValidators.validateRequiredFile(
                    requestVo.getCigaretteDistributionInfoFile(),
                    "请选择卷烟投放基础信息表Excel文件",
                    "CIGARETTE_DISTRIBUTION_FILE_EMPTY",
                    10 * 1024 * 1024L,
                    "卷烟投放基础信息表文件大小超过限制（最大10MB）",
                    "CIGARETTE_DISTRIBUTION_FILE_TOO_LARGE"
            );
            if (cigValidation != null) {
                // 校验失败，转换为统一响应格式
                Map<String, Object> errorBody = cigValidation.getBody();
                if (errorBody != null) {
                    DataImportResponseVo errorVo = new DataImportResponseVo();
                    errorVo.setSuccess(false);
                    errorVo.setMessage((String) errorBody.get("message"));
                    return ResponseEntity.badRequest().body(ApiResponseVo.error(
                        (String) errorBody.get("message"),
                        (String) errorBody.get("errorCode")
                    ));
                }
                return ResponseEntity.badRequest().body(ApiResponseVo.error(
                    "文件校验失败", 
                    "VALIDATION_FAILED"
                ));
            }

            // VO 转 DTO
            DataImportRequestDto requestDto = converter.toDto(requestVo);

            boolean hasBaseFile = requestVo.getBaseCustomerInfoFile() != null && !requestVo.getBaseCustomerInfoFile().isEmpty();
            
            // 执行导入
            Map<String, Object> importResult = excelImportService.importData(requestDto);

            if (!hasBaseFile) {
                importResult.put("baseCustomerInfoNotice", "未提供客户基础信息表，本次未更新");
            }

            // Map 转 VO
            DataImportResponseVo responseVo = converter.toVo(importResult);

            if ((Boolean) importResult.get("success")) {
                log.info("统一数据导入成功，年份: {}, 月份: {}, 周序号: {}",
                        requestVo.getYear(), requestVo.getMonth(), requestVo.getWeekSeq());
                return ResponseEntity.ok(ApiResponseVo.success(responseVo, responseVo.getMessage()));
            } else {
                log.warn("统一数据导入失败: {}", importResult.get("message"));
                return ResponseEntity.badRequest().body(ApiResponseVo.error(
                    responseVo.getMessage() != null ? responseVo.getMessage() : "数据导入失败",
                    "IMPORT_FAILED"
                ));
            }

        } catch (Exception e) {
            log.error("统一数据导入失败", e);
            return ResponseEntity.ok(ApiResponseVo.error(
                "导入失败: " + e.getMessage(), 
                "IMPORT_FAILED"
            ));
        }
    }
}

