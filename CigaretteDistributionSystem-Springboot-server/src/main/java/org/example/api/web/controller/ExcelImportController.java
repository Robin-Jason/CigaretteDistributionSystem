package org.example.api.web.controller;

import lombok.extern.slf4j.Slf4j;
import org.example.api.web.vo.request.BaseCustomerInfoImportRequestVo;
import org.example.api.web.vo.request.CigaretteImportRequestVo;
import org.example.api.web.vo.response.ApiResponseVo;
import org.example.api.web.vo.response.BaseCustomerInfoImportResponseVo;
import org.example.api.web.vo.response.CigaretteImportResponseVo;
import org.example.application.dto.importing.BaseCustomerInfoImportRequestDto;
import org.example.application.dto.importing.CigaretteImportRequestDto;
import org.example.application.service.importing.ExcelImportService;
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
 * - 提供客户基础信息表和卷烟投放基础信息表的独立导入接口
 *
 * 接口：
 * - POST /api/import/base-customer：导入客户基础信息表，返回诚信互助小组编码映射
 * - POST /api/import/cigarette：导入卷烟投放基础信息表
 *
 * @author Robin
 * @version 2.0
 * @since 2025-12-22
 */
@Slf4j
@RestController
@RequestMapping("/api/import")
@Validated
@CrossOrigin(origins = "*")
public class ExcelImportController {
    
    @Autowired
    private ExcelImportService excelImportService;

    /**
     * 导入客户基础信息表。
     * <p>
     * 功能：
     * - 全量覆盖 base_customer_info 表
     * - 同步刷新诚信互助小组编码映射表
     * - 返回诚信互助小组编码映射信息
     * </p>
     *
     * @param requestVo 包含客户基础信息表Excel文件
     * @return 导入结果，包含 integrityGroupMapping
     */
    @PostMapping("/base-customer")
    public ResponseEntity<ApiResponseVo<BaseCustomerInfoImportResponseVo>> importBaseCustomerInfo(
            @Valid BaseCustomerInfoImportRequestVo requestVo) {
        log.info("接收客户基础信息表导入请求");

        try {
            // 文件校验
            ResponseEntity<Map<String, Object>> validation = UploadValidators.validateRequiredFile(
                    requestVo.getFile(),
                    "请选择客户基础信息表Excel文件",
                    "BASE_CUSTOMER_FILE_EMPTY",
                    10 * 1024 * 1024L,
                    "客户基础信息表文件大小超过限制（最大10MB）",
                    "BASE_CUSTOMER_FILE_TOO_LARGE"
            );
            if (validation != null) {
                Map<String, Object> errorBody = validation.getBody();
                if (errorBody != null) {
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
            BaseCustomerInfoImportRequestDto requestDto = new BaseCustomerInfoImportRequestDto();
            requestDto.setFile(requestVo.getFile());
            
            // 执行导入
            Map<String, Object> importResult = excelImportService.importBaseCustomerInfo(requestDto);

            // 构建响应VO
            BaseCustomerInfoImportResponseVo responseVo = new BaseCustomerInfoImportResponseVo();
            responseVo.setSuccess((Boolean) importResult.get("success"));
            responseVo.setMessage((String) importResult.get("message"));
            responseVo.setInsertedCount((Integer) importResult.get("insertedCount"));
            responseVo.setProcessedCount((Integer) importResult.get("processedCount"));
            responseVo.setTableName((String) importResult.get("tableName"));
            responseVo.setIntegrityGroupMapping((Map<String, Object>) importResult.get("integrityGroupMapping"));

            if (Boolean.TRUE.equals(importResult.get("success"))) {
                log.info("客户基础信息表导入成功");
                return ResponseEntity.ok(ApiResponseVo.success(responseVo, responseVo.getMessage()));
            } else {
                log.warn("客户基础信息表导入失败: {}", importResult.get("message"));
                return ResponseEntity.badRequest().body(ApiResponseVo.error(
                    responseVo.getMessage() != null ? responseVo.getMessage() : "导入失败",
                    "IMPORT_FAILED"
                ));
            }

        } catch (Exception e) {
            log.error("客户基础信息表导入失败", e);
            return ResponseEntity.ok(ApiResponseVo.error(
                "导入失败: " + e.getMessage(), 
                "IMPORT_FAILED"
            ));
        }
    }

    /**
     * 导入卷烟投放基础信息表。
     * <p>
     * 功能：
     * - 覆盖 cigarette_distribution_info 对应分区数据
     * - 执行业务合法性校验（全市占比、货源属性规则等）
     * </p>
     *
     * @param requestVo 包含年份、月份、周序号及Excel文件
     * @return 导入结果
     */
    @PostMapping("/cigarette")
    public ResponseEntity<ApiResponseVo<CigaretteImportResponseVo>> importCigaretteDistributionInfo(
            @Valid CigaretteImportRequestVo requestVo) {
        log.info("接收卷烟投放基础信息表导入请求，年份: {}, 月份: {}, 周序号: {}",
                requestVo.getYear(), requestVo.getMonth(), requestVo.getWeekSeq());

        try {
            // 文件校验
            ResponseEntity<Map<String, Object>> validation = UploadValidators.validateRequiredFile(
                    requestVo.getFile(),
                    "请选择卷烟投放基础信息表Excel文件",
                    "CIGARETTE_FILE_EMPTY",
                    10 * 1024 * 1024L,
                    "卷烟投放基础信息表文件大小超过限制（最大10MB）",
                    "CIGARETTE_FILE_TOO_LARGE"
            );
            if (validation != null) {
                Map<String, Object> errorBody = validation.getBody();
                if (errorBody != null) {
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
            CigaretteImportRequestDto requestDto = new CigaretteImportRequestDto();
            requestDto.setFile(requestVo.getFile());
            requestDto.setYear(requestVo.getYear());
            requestDto.setMonth(requestVo.getMonth());
            requestDto.setWeekSeq(requestVo.getWeekSeq());
            
            // 执行导入
            Map<String, Object> importResult = excelImportService.importCigaretteDistributionInfo(requestDto);

            // 构建响应VO
            CigaretteImportResponseVo responseVo = new CigaretteImportResponseVo();
            responseVo.setSuccess((Boolean) importResult.get("success"));
            responseVo.setMessage((String) importResult.get("message"));
            responseVo.setInsertedCount((Integer) importResult.get("insertedCount"));
            responseVo.setTotalRows((Integer) importResult.get("totalRows"));

            if (Boolean.TRUE.equals(importResult.get("success"))) {
                log.info("卷烟投放基础信息表导入成功，年份: {}, 月份: {}, 周序号: {}",
                        requestVo.getYear(), requestVo.getMonth(), requestVo.getWeekSeq());
                return ResponseEntity.ok(ApiResponseVo.success(responseVo, responseVo.getMessage()));
            } else {
                log.warn("卷烟投放基础信息表导入失败: {}", importResult.get("message"));
                return ResponseEntity.badRequest().body(ApiResponseVo.error(
                    responseVo.getMessage() != null ? responseVo.getMessage() : "导入失败",
                    "IMPORT_FAILED"
                ));
            }

        } catch (Exception e) {
            log.error("卷烟投放基础信息表导入失败", e);
            return ResponseEntity.ok(ApiResponseVo.error(
                "导入失败: " + e.getMessage(), 
                "IMPORT_FAILED"
            ));
        }
    }
}
