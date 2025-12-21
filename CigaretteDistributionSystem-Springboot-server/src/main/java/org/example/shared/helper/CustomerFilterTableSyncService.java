package org.example.shared.helper;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.example.infrastructure.persistence.mapper.AdminMapper;

import java.util.*;

/**
 * customer_filter 分区表结构同步服务
 * 
 * 功能：
 * 1. 检测 base_customer_info 和 customer_filter 的字段差异
 * 2. 自动添加缺失的字段到 customer_filter 表
 * 3. 确保两个表的结构保持一致（除了分区键字段）
 * 
 * @author System
 * @version 1.0
 * @since 2025-12-20
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CustomerFilterTableSyncService {

    private final AdminMapper adminMapper;
    
    private static final String BASE_TABLE = "base_customer_info";
    private static final String FILTER_TABLE = "customer_filter";
    
    // customer_filter 表的固定字段（分区键和元数据字段）
    private static final Set<String> FILTER_TABLE_FIXED_COLUMNS = new HashSet<>(Arrays.asList(
            "YEAR", "MONTH", "WEEK_SEQ", "CREATED_AT", "UPDATED_AT"
    ));

    /**
     * 同步 customer_filter 表结构，确保包含 base_customer_info 的所有字段
     * 
     * @return 同步结果：添加的字段列表
     */
    public List<String> syncTableStructure() {
        log.info("开始同步 customer_filter 表结构...");
        
        // 1. 获取 base_customer_info 的所有字段
        List<ColumnInfo> baseColumns = getTableColumns(BASE_TABLE);
        log.debug("base_customer_info 表共有 {} 个字段", baseColumns.size());
        
        // 2. 获取 customer_filter 的所有字段
        List<ColumnInfo> filterColumns = getTableColumns(FILTER_TABLE);
        log.debug("customer_filter 表共有 {} 个字段", filterColumns.size());
        
        // 3. 找出缺失的字段
        Set<String> filterColumnNames = new HashSet<>();
        for (ColumnInfo col : filterColumns) {
            filterColumnNames.add(col.getName().toUpperCase());
        }
        
        List<String> addedColumns = new ArrayList<>();
        for (ColumnInfo baseCol : baseColumns) {
            String colName = baseCol.getName().toUpperCase();
            
            // 跳过固定字段和 ID 字段
            if (FILTER_TABLE_FIXED_COLUMNS.contains(colName) || "ID".equalsIgnoreCase(colName)) {
                continue;
            }
            
            // 如果字段不存在，添加它
            if (!filterColumnNames.contains(colName)) {
                try {
                    addColumnToFilterTable(baseCol);
                    addedColumns.add(colName);
                    log.info("添加字段到 customer_filter: {} ({})", colName, baseCol.getDefinition());
                } catch (Exception e) {
                    log.error("添加字段失败: {}", colName, e);
                    throw new RuntimeException("同步表结构失败: " + e.getMessage(), e);
                }
            }
        }
        
        if (addedColumns.isEmpty()) {
            log.info("customer_filter 表结构已是最新，无需同步");
        } else {
            log.info("同步完成，共添加 {} 个字段: {}", addedColumns.size(), addedColumns);
        }
        
        return addedColumns;
    }

    /**
     * 获取表的所有字段信息
     * 
     * @param tableName 表名
     * @return 字段信息列表
     */
    private List<ColumnInfo> getTableColumns(String tableName) {
        List<Map<String, Object>> rows = adminMapper.listTableColumns(tableName);
        List<ColumnInfo> result = new ArrayList<>();
        for (Map<String, Object> row : rows) {
            ColumnInfo info = new ColumnInfo();
            info.setName((String) row.get("COLUMN_NAME"));
            info.setType((String) row.get("COLUMN_TYPE"));
            info.setNullable("YES".equalsIgnoreCase(String.valueOf(row.get("IS_NULLABLE"))));
            Object def = row.get("COLUMN_DEFAULT");
            info.setDefaultValue(def == null ? null : String.valueOf(def));
            Object comment = row.get("COLUMN_COMMENT");
            info.setComment(comment == null ? null : String.valueOf(comment));
            result.add(info);
        }
        return result;
    }

    /**
     * 添加字段到 customer_filter 表
     * 
     * @param columnInfo 字段信息
     */
    private void addColumnToFilterTable(ColumnInfo columnInfo) {
        // 构建 ALTER TABLE 语句
        // 注意：分区表添加字段需要在所有分区上生效，MySQL 会自动处理
        StringBuilder sql = new StringBuilder();
        sql.append("ALTER TABLE `").append(FILTER_TABLE).append("` ");
        sql.append("ADD COLUMN `").append(columnInfo.getName()).append("` ");
        sql.append(columnInfo.getType());
        
        if (!columnInfo.isNullable() && columnInfo.getDefaultValue() == null) {
            // 如果原字段是 NOT NULL 且没有默认值，改为允许 NULL（避免数据迁移问题）
            sql.append(" DEFAULT NULL");
        } else if (columnInfo.getDefaultValue() != null) {
            String defaultValue = columnInfo.getDefaultValue();
            // 处理默认值
            if (defaultValue.equals("CURRENT_TIMESTAMP") || defaultValue.startsWith("CURRENT_TIMESTAMP")) {
                sql.append(" DEFAULT ").append(defaultValue);
            } else {
                sql.append(" DEFAULT '").append(defaultValue).append("'");
            }
        }
        
        if (columnInfo.getComment() != null && !columnInfo.getComment().isEmpty()) {
            sql.append(" COMMENT '").append(columnInfo.getComment().replace("'", "''")).append("'");
        }
        
        log.debug("执行 SQL: {}", sql.toString());
        adminMapper.executeSql(sql.toString());
    }

    /**
     * 获取 customer_filter 表中存在的字段列表（用于动态插入）
     * 
     * @return 字段名列表（排除固定字段）
     */
    public List<String> getFilterTableColumns() {
        List<ColumnInfo> columns = getTableColumns(FILTER_TABLE);
        List<String> columnNames = new ArrayList<>();
        
        for (ColumnInfo col : columns) {
            String colName = col.getName().toUpperCase();
            // 排除固定字段
            if (!FILTER_TABLE_FIXED_COLUMNS.contains(colName) && !"ID".equalsIgnoreCase(colName)) {
                columnNames.add(col.getName());
            }
        }
        
        return columnNames;
    }

    /**
     * 字段信息内部类
     */
    private static class ColumnInfo {
        private String name;
        private String type;
        private boolean nullable;
        private String defaultValue;
        private String comment;

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getType() {
            return type;
        }

        public void setType(String type) {
            this.type = type;
        }

        public boolean isNullable() {
            return nullable;
        }

        public void setNullable(boolean nullable) {
            this.nullable = nullable;
        }

        public String getDefaultValue() {
            return defaultValue;
        }

        public void setDefaultValue(String defaultValue) {
            this.defaultValue = defaultValue;
        }

        public String getComment() {
            return comment;
        }

        public void setComment(String comment) {
            this.comment = comment;
        }

        /**
         * 获取完整的字段定义（用于 ALTER TABLE）
         */
        public String getDefinition() {
            return type;
        }
    }
}

