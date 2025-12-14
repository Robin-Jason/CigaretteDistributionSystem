package org.example.application.service.impl;

import lombok.extern.slf4j.Slf4j;
import lombok.RequiredArgsConstructor;
import org.example.application.dto.DataImportRequestDto;
import org.example.application.service.ExcelImportService;
import org.example.shared.constants.TableConstants;
import org.example.shared.helper.BaseCustomerTableManager;
import org.example.shared.helper.ExcelParseHelper;
import org.example.shared.helper.ImportValidationHelper;
import org.example.shared.helper.IntegrityGroupMappingService;
import org.example.shared.helper.CigaretteInfoWriter;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Excel导入服务实现类
 * <p>职责：编排统一导入流程，委托解析/校验/写入/映射刷新组件执行。</p>
 *
 * @author Robin
 * @version 1.1
 * @since 2025-12-11
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ExcelImportServiceImpl implements ExcelImportService {

    private final BaseCustomerTableManager baseCustomerTableManager;
    private final CigaretteInfoWriter cigaretteInfoWriter;
    private final IntegrityGroupMappingService integrityGroupMappingService;

    private static final Map<String, String> BASE_CUSTOMER_COLUMN_DEFINITIONS = new LinkedHashMap<>();

    static {
        BASE_CUSTOMER_COLUMN_DEFINITIONS.put("CUST_CODE", "varchar(50) DEFAULT NULL COMMENT '许可证编号'");
        BASE_CUSTOMER_COLUMN_DEFINITIONS.put("CUST_ID", "varchar(50) DEFAULT NULL COMMENT '客户编码'");
        BASE_CUSTOMER_COLUMN_DEFINITIONS.put("GRADE", "varchar(20) DEFAULT NULL COMMENT '客户档位'");
        BASE_CUSTOMER_COLUMN_DEFINITIONS.put("ORDER_CYCLE", "varchar(20) DEFAULT NULL COMMENT '订货周期'");
        BASE_CUSTOMER_COLUMN_DEFINITIONS.put("CREDIT_LEVEL", "varchar(20) DEFAULT NULL COMMENT '信用等级'");
        BASE_CUSTOMER_COLUMN_DEFINITIONS.put("MARKET_TYPE", "varchar(20) DEFAULT NULL COMMENT '市场类型'");
        BASE_CUSTOMER_COLUMN_DEFINITIONS.put("CLASSIFICATION_CODE", "varchar(50) DEFAULT NULL COMMENT '城乡分类代码'");
        BASE_CUSTOMER_COLUMN_DEFINITIONS.put("CUST_FORMAT", "varchar(50) DEFAULT NULL COMMENT '经营业态'");
        BASE_CUSTOMER_COLUMN_DEFINITIONS.put("BUSINESS_DISTRICT_TYPE", "varchar(50) DEFAULT NULL COMMENT '商圈类型'");
        BASE_CUSTOMER_COLUMN_DEFINITIONS.put("COMPANY_BRANCH", "varchar(50) DEFAULT NULL COMMENT '分公司'");
        BASE_CUSTOMER_COLUMN_DEFINITIONS.put("COMPANY_DISTRICT", "varchar(50) DEFAULT NULL COMMENT '区县公司'");
        BASE_CUSTOMER_COLUMN_DEFINITIONS.put("MARKET_DEPARTMENT", "varchar(50) DEFAULT NULL COMMENT '市场部'");
        BASE_CUSTOMER_COLUMN_DEFINITIONS.put("BUSINESS_STATUS", "varchar(20) DEFAULT NULL COMMENT '经营状态'");
        BASE_CUSTOMER_COLUMN_DEFINITIONS.put("IS_MUTUAL_AID_GROUP", "varchar(20) DEFAULT NULL COMMENT '是否加入诚信互助小组'");
        BASE_CUSTOMER_COLUMN_DEFINITIONS.put("GROUP_NAME", "varchar(100) DEFAULT NULL COMMENT '小组名称'");
        BASE_CUSTOMER_COLUMN_DEFINITIONS.put("QUALITY_DATA_SHARE", "varchar(20) DEFAULT NULL COMMENT '优质数据共享客户'");
        BASE_CUSTOMER_COLUMN_DEFINITIONS.put("PREMIUM_CUSTOMER", "varchar(20) DEFAULT NULL COMMENT '优质客户'");
        BASE_CUSTOMER_COLUMN_DEFINITIONS.put("CRITICAL_TIME_AREA", "varchar(20) DEFAULT NULL COMMENT '重点时段+地段'");
    }


    /**
     * 导入卷烟投放基础信息Excel
     */
    @Transactional(rollbackFor = Exception.class, timeout = 120)
    private Map<String, Object> importCigaretteDistributionInfo(org.example.application.dto.CigaretteImportRequestDto request) {
        Map<String, Object> result = new HashMap<>();
        
        try {
            log.info("开始导入卷烟投放基础信息，年份: {}, 月份: {}, 周序号: {}", 
                    request.getYear(), request.getMonth(), request.getWeekSeq());
            
            // 1. 验证文件
            if (!ImportValidationHelper.validateExcelFile(request.getFile())) {
                result.put("success", false);
                result.put("message", "文件格式不正确，请上传Excel文件");
                return result;
            }
            
            // 2. 读取Excel数据
            List<Map<String, Object>> excelData = ExcelParseHelper.readCigaretteInfo(request.getFile());
            if (excelData.isEmpty()) {
                result.put("success", false);
                result.put("message", "Excel文件为空或格式不正确");
                return result;
            }
            
            // 4. 验证数据结构
            if (!validateCigaretteInfoStructure(excelData.get(0))) {
                result.put("success", false);
                result.put("message", "Excel文件结构不符合要求，请检查列名是否与cigarette_distribution_info表结构完全一致");
                return result;
            }
            
            // 5. 插入数据到分区表
            int insertedCount = cigaretteInfoWriter.writeToPartition(
                    excelData, request.getYear(), request.getMonth(), request.getWeekSeq());
            
            result.put("success", true);
            result.put("message", "导入成功");
            result.put("insertedCount", insertedCount);
            result.put("totalRows", excelData.size());
            
            log.info("卷烟投放基础信息导入完成: {}-{}-{}, 插入记录数: {}", 
                    request.getYear(), request.getMonth(), request.getWeekSeq(), insertedCount);
            
        } catch (Exception e) {
            log.error("导入卷烟投放基础信息失败", e);
            result.put("success", false);
            result.put("message", "导入失败: " + e.getMessage());
        }
        
        return result;
    }

    /**
     * 导入客户基础信息表 Excel。
     *
     * @param request 客户基础信息导入请求，包含 Excel 文件
     * @return 导入结果
     * @example 上传 base_customer_info.xlsx -> 重建表并插入数据，返回 success=true 与行数统计
     */
    @Transactional(rollbackFor = Exception.class, timeout = 120)
    private Map<String, Object> importBaseCustomerInfo(org.example.application.dto.BaseCustomerInfoImportRequestDto request) {
        Map<String, Object> result = new HashMap<>();
        
        try {
            log.info("开始导入客户基础信息数据");
            
            if (!ImportValidationHelper.validateExcelFile(request.getFile())) {
                result.put("success", false);
                result.put("message", "文件格式不正确，请上传Excel文件");
                return result;
            }
            
            ExcelParseHelper.BaseCustomerExcelData excelData = ExcelParseHelper.readBaseCustomerInfo(request.getFile());
            if (excelData.getRows().isEmpty()) {
                result.put("success", false);
                result.put("message", "Excel文件为空或格式不正确");
                return result;
            }
            
            if (!excelData.getColumns().contains("CUST_CODE")) {
                result.put("success", false);
                result.put("message", "Excel文件中缺少必填列：CUST_CODE");
                return result;
            }
            
            baseCustomerTableManager.recreateTable(excelData.getColumns(),
                    BASE_CUSTOMER_COLUMN_DEFINITIONS,
                    TableConstants.DEFAULT_DYNAMIC_COLUMN_TYPE,
                    TableConstants.BASE_CUSTOMER_INFO);
            BaseCustomerTableManager.BaseCustomerImportStats stats = baseCustomerTableManager.insertAll(
                    excelData.getColumns(),
                    excelData.getRows(),
                    TableConstants.MANDATORY_CUSTOMER_COLUMN);
            integrityGroupMappingService.refreshFromBaseCustomer();
            List<Map<String, Object>> integrityGroupMappings = integrityGroupMappingService.fetchAll();
            
            result.put("success", true);
            result.put("message", "导入成功");
            result.put("insertedCount", stats.getInsertedCount());
            result.put("processedCount", stats.getProcessedCount());
            result.put("tableName", TableConstants.BASE_CUSTOMER_INFO);
            result.put("integrityGroupMappings", integrityGroupMappings);
            
            log.info("客户基础信息导入完成，新增 {} 条，总计 {}", 
                    stats.getInsertedCount(), stats.getProcessedCount());
            
        } catch (Exception e) {
            log.error("导入客户基础信息失败", e);
            result.put("success", false);
            result.put("message", "导入失败: " + e.getMessage());
        }
        
        return result;
    }

    // ==================== 私有辅助方法 ====================

    /**
     * 验证卷烟投放基础信息数据结构。
     *
     * @param sampleRow Excel 首行样例
     * @return 是否包含必需列
     * @example 缺少 DELIVERY_METHOD 时返回 false
     */
    private boolean validateCigaretteInfoStructure(Map<String, Object> sampleRow) {
        List<String> requiredCoreColumns = Arrays.asList(
                "CIG_CODE", "CIG_NAME", "YEAR", "MONTH", "WEEK_SEQ",
                "URS", "ADV", "DELIVERY_METHOD", "DELIVERY_ETYPE", "DELIVERY_AREA",
                "SUPPLY_ATTRIBUTE", "TAG"
        );
        boolean valid = ImportValidationHelper.validateRequiredColumns(sampleRow, requiredCoreColumns);
        if (!valid) {
            log.warn("缺少必需的核心列，实际列名: {}", sampleRow.keySet());
        }
        return valid;
    }


    /**
     * 统一数据导入接口（客户表可选，卷烟表必传）。
     *
     * 场景：
     * - 同时导入客户基础信息表与卷烟投放基础信息表（全量模式）。
     * - 仅导入卷烟投放基础信息表（当客户表未提供时，客户表步骤跳过）。
     *
     * 流程：
     * 1. 如提供客户基础信息表：覆盖 base_customer_info。
     * 2. 必须提供卷烟投放基础信息表：覆盖 cigarette_distribution_info 对应分区。
     *
     * @param request 导入请求
     * @return 导入结果
     * @example 传入 base_customer_info.xlsx 与 cigarette_distribution_info.xlsx 且 year=2025, month=9, weekSeq=3 时，返回 success=true，包含两张表的导入统计
     */
    @Override
    @Transactional(rollbackFor = Exception.class, timeout = 300)
    public Map<String, Object> importData(DataImportRequestDto request) {
        Map<String, Object> result = new HashMap<>();
        
        try {
            log.info("开始统一数据导入，年份: {}, 月份: {}, 周序号: {}", 
                    request.getYear(), request.getMonth(), request.getWeekSeq());
            
            boolean hasBaseFile = request.getBaseCustomerInfoFile() != null && !request.getBaseCustomerInfoFile().isEmpty();
            Map<String, Object> baseCustomerResult = new HashMap<>();
            if (hasBaseFile) {
            log.info("步骤1：导入客户基础信息表");
            org.example.application.dto.BaseCustomerInfoImportRequestDto baseCustomerRequest = new org.example.application.dto.BaseCustomerInfoImportRequestDto();
            baseCustomerRequest.setFile(request.getBaseCustomerInfoFile());
            
                baseCustomerResult = importBaseCustomerInfo(baseCustomerRequest);
            if (!Boolean.TRUE.equals(baseCustomerResult.get("success"))) {
                result.put("success", false);
                result.put("message", "客户基础信息表导入失败: " + baseCustomerResult.get("message"));
                result.put("baseCustomerInfoResult", baseCustomerResult);
                return result;
                }
            } else {
                baseCustomerResult.put("success", true);
                baseCustomerResult.put("message", "未提供客户基础信息表，本次未更新");
                log.info("步骤1：跳过客户基础信息表导入（未提供文件）");
            }
            
            // 2. 导入卷烟投放基础信息表
            log.info("步骤2：导入卷烟投放基础信息表");
            org.example.application.dto.CigaretteImportRequestDto cigaretteRequest = new org.example.application.dto.CigaretteImportRequestDto();
            cigaretteRequest.setFile(request.getCigaretteDistributionInfoFile());
            cigaretteRequest.setYear(request.getYear());
            cigaretteRequest.setMonth(request.getMonth());
            cigaretteRequest.setWeekSeq(request.getWeekSeq());
            
            Map<String, Object> cigaretteResult = importCigaretteDistributionInfo(cigaretteRequest);
            if (!Boolean.TRUE.equals(cigaretteResult.get("success"))) {
                result.put("success", false);
                result.put("message", "卷烟投放基础信息表导入失败: " + cigaretteResult.get("message"));
                result.put("baseCustomerInfoResult", baseCustomerResult);
                result.put("cigaretteDistributionInfoResult", cigaretteResult);
                return result;
            }
            
            // 3. 汇总结果
            result.put("success", true);
            result.put("message", "数据导入成功");
            result.put("year", request.getYear());
            result.put("month", request.getMonth());
            result.put("weekSeq", request.getWeekSeq());
            result.put("baseCustomerInfoResult", baseCustomerResult);
            result.put("cigaretteDistributionInfoResult", cigaretteResult);
            
            log.info("统一数据导入完成: {}-{}-{}, 客户基础信息: {} 条, 卷烟投放信息: {} 条", 
                    request.getYear(), request.getMonth(), request.getWeekSeq(),
                    baseCustomerResult.get("insertedCount"), cigaretteResult.get("insertedCount"));
            
        } catch (Exception e) {
            log.error("统一数据导入失败: {}-{}-{}", 
                     request.getYear(), request.getMonth(), request.getWeekSeq(), e);
            result.put("success", false);
            result.put("message", "导入失败: " + e.getMessage());
        }
        
        return result;
    }

}
