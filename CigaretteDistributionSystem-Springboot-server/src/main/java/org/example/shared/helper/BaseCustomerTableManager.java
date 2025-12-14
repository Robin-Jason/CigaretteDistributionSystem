package org.example.shared.helper;

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

    /**
     * 按导入列重建 base_customer_info。
     */
    public void recreateTable(List<String> excelColumns,
                              Map<String, String> baseColumnDefinitions,
                              String defaultColumnType,
                              String tableName) {
        baseCustomerInfoRepository.dropTable();

        LinkedHashSet<String> columnOrder = new LinkedHashSet<>(baseColumnDefinitions.keySet());
        for (String column : excelColumns) {
            if (column == null || column.isEmpty() || "ID".equalsIgnoreCase(column)) {
                continue;
            }
            columnOrder.add(column);
        }

        baseCustomerInfoRepository.createTable(new ArrayList<>(columnOrder),
                baseColumnDefinitions,
                defaultColumnType);
        log.info("重建客户基础信息表: {}", tableName);
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

        for (Map<String, Object> row : rows) {
            Object codeObj = row.get(mandatoryColumn);
            String custCode = codeObj != null ? codeObj.toString() : null;
            if (custCode == null || custCode.trim().isEmpty()) {
                log.warn("跳过缺少 {} 的记录: {}", mandatoryColumn, row);
                continue;
            }
            int affected = baseCustomerInfoRepository.insertRow(sanitizedCols, row);
            if (affected > 0) {
                stats.insertedCount++;
            }
            stats.processedCount++;
        }
        return stats;
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

