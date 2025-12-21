package org.example.shared.helper;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.domain.repository.BaseCustomerInfoRepository;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * 客户基础信息表的重建与写入管理。
 *
 * <p>职责：重建 base_customer_info 表结构，批量写入导入数据，返回导入统计。</p>
 *
 * @author Robin
 * @version 1.0
 * @since 2025-12-11
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class BaseCustomerTableManager {

    private final BaseCustomerInfoRepository baseCustomerInfoRepository;
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    
    // 业务规则：
    // 1. QUALITY_DATA_SHARE等固定标签字段继续使用固定字段，不写入JSON
    // 2. 其他动态标签以JSON格式存放，使用中文键值对，如{"优质客户":"是"}
    // 3. Excel中新增的动态标签列（不在BASE_CUSTOMER_COLUMN_DEFINITIONS中，且列名为中文）应写入DYNAMIC_TAGS JSON字段
    
    // 固定字段列表（这些字段不会写入JSON）
    private static final Set<String> FIXED_FIELDS = new HashSet<String>() {{
        add("CUST_CODE");
        add("CUST_ID");
        add("GRADE");
        add("ORDER_CYCLE");
        add("CREDIT_LEVEL");
        add("MARKET_TYPE");
        add("CLASSIFICATION_CODE");
        add("CUST_FORMAT");
        add("BUSINESS_DISTRICT_TYPE");
        add("COMPANY_BRANCH");
        add("COMPANY_DISTRICT");
        add("MARKET_DEPARTMENT");
        add("BUSINESS_STATUS");
        add("IS_MUTUAL_AID_GROUP");
        add("GROUP_NAME");
        add("QUALITY_DATA_SHARE");
        add("DYNAMIC_TAGS"); // DYNAMIC_TAGS本身不写入JSON
        add("ID");
    }};

    /**
     * 按导入列重建 base_customer_info。
     */
    public void recreateTable(List<String> excelColumns,
                              Map<String, String> baseColumnDefinitions,
                              String defaultColumnType,
                              String tableName) {
        baseCustomerInfoRepository.dropTable();

        LinkedHashSet<String> columnOrder = new LinkedHashSet<>(baseColumnDefinitions.keySet());
        // 确保DYNAMIC_TAGS列存在（系统内部使用，Excel中不应有此列）
        columnOrder.add("DYNAMIC_TAGS");
        
        // 只添加固定字段和DYNAMIC_TAGS，过滤掉动态标签列（中文列名）和Excel中的DYNAMIC_TAGS列
        for (String column : excelColumns) {
            if (column == null || column.isEmpty() || "ID".equalsIgnoreCase(column)) {
                continue;
            }
            // 忽略Excel中的DYNAMIC_TAGS列（如果存在）
            if ("DYNAMIC_TAGS".equalsIgnoreCase(column)) {
                log.warn("Excel中不应包含DYNAMIC_TAGS列，已忽略: {}", column);
                continue;
            }
            // 如果是固定字段，添加到列列表中
            if (FIXED_FIELDS.contains(column)) {
                columnOrder.add(column);
            } else {
                // 如果是中文列名（动态标签列），不创建数据库列，这些列的值将写入DYNAMIC_TAGS JSON字段
                if (column.matches(".*[\\u4e00-\\u9fa5].*")) {
                    log.debug("跳过动态标签列，不创建数据库列: {}", column);
                    continue;
                }
                // 其他非中文列名（可能是其他业务字段）保留
                columnOrder.add(column);
            }
        }

        baseCustomerInfoRepository.createTable(new ArrayList<>(columnOrder),
                baseColumnDefinitions,
                defaultColumnType);
        log.info("重建客户基础信息表: {}, 动态标签列将写入DYNAMIC_TAGS JSON字段", tableName);
    }

    /**
     * 整表插入导入数据。
     */
    /**
     * 整表插入导入数据。
     *
     * @param columns           列顺序
     * @param rows              行数据
     * @param mandatoryColumn   必填列（如 CUST_CODE）
     * @return 导入统计
     * @example 传入包含 CUST_CODE 的列与行 -> 返回插入/处理计数
     */
    public BaseCustomerImportStats insertAll(List<String> columns,
                                             List<Map<String, Object>> rows,
                                             String mandatoryColumn) {
        BaseCustomerImportStats stats = new BaseCustomerImportStats();
        List<String> sanitizedCols = new ArrayList<>(columns);
        sanitizedCols.removeIf(col -> col == null || col.isEmpty() || "ID".equalsIgnoreCase(col));

        if (!sanitizedCols.contains(mandatoryColumn)) {
            throw new IllegalStateException("缺少必填列 " + mandatoryColumn);
        }

        // 阶段2：确保DYNAMIC_TAGS列在列列表中（如果不存在则添加）
        if (!sanitizedCols.contains("DYNAMIC_TAGS")) {
            sanitizedCols.add("DYNAMIC_TAGS");
        }

        for (Map<String, Object> row : rows) {
            Object codeObj = row.get(mandatoryColumn);
            String custCode = codeObj != null ? codeObj.toString() : null;
            if (custCode == null || custCode.trim().isEmpty()) {
                log.warn("跳过缺少 {} 的记录: {}", mandatoryColumn, row);
                continue;
            }
            
            // 处理动态标签：将Excel中新增的动态标签列（中文列名）写入DYNAMIC_TAGS JSON字段
            enrichDynamicTags(row);
            
            // 过滤掉动态标签列（中文列名）和Excel中的DYNAMIC_TAGS列，避免在数据库中创建这些列
            List<String> filteredCols = new ArrayList<>();
            for (String col : sanitizedCols) {
                // 忽略Excel中的DYNAMIC_TAGS列（如果存在）
                if ("DYNAMIC_TAGS".equalsIgnoreCase(col)) {
                    // 系统会自动添加DYNAMIC_TAGS列，这里跳过Excel中的该列
                    continue;
                }
                // 保留固定字段
                if (FIXED_FIELDS.contains(col)) {
                    filteredCols.add(col);
                } else {
                    // 如果是中文列名（动态标签列），不添加到插入列列表中
                    // 这些列的值已经写入DYNAMIC_TAGS JSON字段了
                    if (col != null && col.matches(".*[\\u4e00-\\u9fa5].*")) {
                        log.debug("过滤动态标签列，不创建数据库列: {}", col);
                        continue;
                    }
                    // 其他非中文列名（可能是其他业务字段）保留
                    filteredCols.add(col);
                }
            }
            // 确保DYNAMIC_TAGS列在插入列列表中（系统内部使用）
            if (!filteredCols.contains("DYNAMIC_TAGS")) {
                filteredCols.add("DYNAMIC_TAGS");
            }
            
            int affected = baseCustomerInfoRepository.insertRow(filteredCols, row);
            if (affected > 0) {
                stats.insertedCount++;
            }
            stats.processedCount++;
        }
        return stats;
    }

    /**
     * 丰富动态标签JSON字段
     * <p>
     * 业务规则：
     * 1. QUALITY_DATA_SHARE等固定标签字段继续使用固定字段，不写入JSON
     * 2. 其他动态标签以JSON格式存放，使用中文键值对，如{"优质客户":"是"}
     * 3. Excel中新增的动态标签列（不在固定字段列表中，且列名为中文）应写入DYNAMIC_TAGS JSON字段
     * 4. Excel中不应包含DYNAMIC_TAGS列，如果存在则忽略
     * </p>
     *
     * @param row 行数据Map
     */
    private void enrichDynamicTags(Map<String, Object> row) {
        try {
            // 1. 忽略Excel中的DYNAMIC_TAGS列（如果存在）
            // Excel中不应包含DYNAMIC_TAGS列，动态标签应通过中文列名提供
            if (row.containsKey("DYNAMIC_TAGS")) {
                log.warn("Excel中不应包含DYNAMIC_TAGS列，已忽略。客户代码: {}", row.get("CUST_CODE"));
                row.remove("DYNAMIC_TAGS"); // 移除，避免影响后续处理
            }
            
            // 2. 创建新的动态标签Map（不从Excel读取，只从中文列名构建）
            Map<String, Object> dynamicTags = new HashMap<>();
            
            // 3. 将Excel中新增的动态标签列（不在固定字段列表中，且列名为中文）写入JSON字段
            for (Map.Entry<String, Object> entry : row.entrySet()) {
                String columnName = entry.getKey();
                Object columnValue = entry.getValue();
                
                // 跳过固定字段、DYNAMIC_TAGS和空值
                if (FIXED_FIELDS.contains(columnName) || 
                    "DYNAMIC_TAGS".equalsIgnoreCase(columnName) ||
                    columnValue == null || 
                    columnValue.toString().trim().isEmpty()) {
                    continue;
                }
                
                // 判断是否为中文列名（动态标签列）
                if (columnName != null && columnName.matches(".*[\\u4e00-\\u9fa5].*")) {
                    // 将动态标签列的值写入JSON字段，键名为列名本身
                    dynamicTags.put(columnName, columnValue.toString());
                    log.debug("检测到动态标签列: {} = {}, 将写入DYNAMIC_TAGS JSON字段", columnName, columnValue);
                }
            }
            
            // 4. 将构建好的JSON对象写回row
            if (!dynamicTags.isEmpty()) {
                row.put("DYNAMIC_TAGS", OBJECT_MAPPER.writeValueAsString(dynamicTags));
            }
        } catch (Exception e) {
            log.warn("处理DYNAMIC_TAGS JSON字段失败，跳过: {}", row.get("CUST_CODE"), e);
            // 失败时不影响主流程
        }
    }

    public static class BaseCustomerImportStats {
        private int insertedCount;
        private int processedCount;

        public int getInsertedCount() {
            return insertedCount;
        }

        public int getProcessedCount() {
            return processedCount;
        }

        public void addInserted(int delta) {
            this.insertedCount += delta;
        }

        public void addProcessed(int delta) {
            this.processedCount += delta;
        }
    }
}

