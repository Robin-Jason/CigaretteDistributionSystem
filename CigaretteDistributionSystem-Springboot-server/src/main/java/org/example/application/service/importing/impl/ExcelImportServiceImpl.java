package org.example.application.service.importing.impl;

import lombok.extern.slf4j.Slf4j;
import lombok.RequiredArgsConstructor;
import org.example.application.dto.importing.BaseCustomerInfoImportRequestDto;
import org.example.application.dto.importing.CigaretteImportRequestDto;
import org.example.application.service.importing.CigaretteImportValidator;
import org.example.application.service.importing.ExcelImportService;
import org.example.shared.constants.TableConstants;
import org.example.shared.helper.BaseCustomerTableManager;
import org.example.shared.helper.CigaretteInfoWriter;
import org.example.shared.helper.ExcelParseHelper;
import org.example.shared.helper.ImportValidationHelper;
import org.example.shared.helper.IntegrityGroupMappingService;
import org.example.domain.event.DataImportStartedEvent;
import org.example.domain.event.DataImportCompletedEvent;
import org.example.domain.event.DataImportFailedEvent;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Excel导入服务实现类
 * <p>职责：提供客户基础信息表和卷烟投放基础信息表的独立导入功能。</p>
 *
 * @author Robin
 * @version 2.0
 * @since 2025-12-11
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ExcelImportServiceImpl implements ExcelImportService {

    private final BaseCustomerTableManager baseCustomerTableManager;
    private final CigaretteInfoWriter cigaretteInfoWriter;
    private final IntegrityGroupMappingService integrityGroupMappingService;
    private final ApplicationEventPublisher eventPublisher;
    private final CigaretteImportValidator cigaretteImportValidator;

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
    }


    /**
     * 导入卷烟投放基础信息Excel
     */
    @Override
    @Transactional(rollbackFor = Exception.class, timeout = 120)
    public Map<String, Object> importCigaretteDistributionInfo(CigaretteImportRequestDto request) {
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
            
            // 3. 验证数据结构（必需列）
            if (!validateCigaretteInfoStructure(excelData.get(0))) {
                result.put("success", false);
                result.put("message", "Excel文件结构不符合要求，请检查列名是否与cigarette_distribution_info表结构完全一致");
                return result;
            }

            // 4. 业务合法性校验（全市占比 + 货源属性规则）
            cigaretteImportValidator.validate(excelData);
            
            // 5. 插入数据到分区表
            int insertedCount = cigaretteInfoWriter.writeToPartition(
                    excelData, request.getYear(), request.getMonth(), request.getWeekSeq());
            
            result.put("success", true);
            result.put("message", "导入成功");
            result.put("insertedCount", insertedCount);
            result.put("totalRows", excelData.size());
            
            log.info("卷烟投放基础信息导入完成: {}-{}-{}, 插入记录数: {}", 
                    request.getYear(), request.getMonth(), request.getWeekSeq(), insertedCount);
            
        } catch (IllegalArgumentException e) {
            // 业务合法性校验失败（全市占比 / 货源属性规则等）
            log.warn("卷烟投放基础信息导入校验未通过: {}", e.getMessage());
            result.put("success", false);
            result.put("message", "导入校验失败: " + e.getMessage());
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
     * @return 导入结果，包含 integrityGroupMapping（诚信互助小组编码映射）
     * @example 上传 base_customer_info.xlsx -> 重建表并插入数据，返回 success=true 与行数统计及诚信互助小组映射
     */
    @Override
    @Transactional(rollbackFor = Exception.class, timeout = 120)
    public Map<String, Object> importBaseCustomerInfo(BaseCustomerInfoImportRequestDto request) {
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
            
            // 重要：必须在导入 base_customer_info 后立即同步生成 integrity_group_code_mapping
            // 确保两个表保持同步，这是使用诚信互助小组扩展类型的前提条件
            log.info("开始同步生成 integrity_group_code_mapping 表...");
            integrityGroupMappingService.refreshFromBaseCustomer();
            List<Map<String, Object>> integrityGroupMappings = integrityGroupMappingService.fetchAll();
            log.info("integrity_group_code_mapping 表同步完成，共 {} 条记录", integrityGroupMappings.size());
            
            result.put("success", true);
            result.put("message", "导入成功");
            result.put("insertedCount", stats.getInsertedCount());
            result.put("processedCount", stats.getProcessedCount());
            result.put("tableName", TableConstants.BASE_CUSTOMER_INFO);
            
            // 诚信互助小组编码映射返回结构
            Map<String, Object> integrityGroupInfo = new LinkedHashMap<>();
            integrityGroupInfo.put("total", integrityGroupMappings.size());
            integrityGroupInfo.put("updated", true);  // 标注已更新
            // 根据是否有数据调整提示信息
            if (integrityGroupMappings.isEmpty()) {
                integrityGroupInfo.put("notice", "已根据本次导入的客户表重新生成映射，但客户表中无诚信互助小组数据");
            } else {
                integrityGroupInfo.put("notice", "已根据本次导入的客户表重新生成映射");
            }
            // 简洁映射：groupName -> groupCode，用于前端下拉选择或编码转换
            Map<String, String> codeMapping = new LinkedHashMap<>();
            for (Map<String, Object> mapping : integrityGroupMappings) {
                codeMapping.put((String) mapping.get("groupName"), (String) mapping.get("groupCode"));
            }
            integrityGroupInfo.put("codeMapping", codeMapping);
            // 详细列表：包含客户数等统计信息，用于展示
            integrityGroupInfo.put("details", integrityGroupMappings);
            result.put("integrityGroupMapping", integrityGroupInfo);
            
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
}
