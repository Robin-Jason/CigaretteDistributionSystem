package org.example.service.importer;

import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.*;

/**
 * Excel 解析工具。
 *
 * <p>职责：读取 Excel 并转换为 Map/DTO 结构，供导入流程复用。</p>
 * <p>范围：卷烟投放基础信息、客户基础信息。</p>
 *
 * @author Robin
 * @version 1.0
 * @since 2025-12-11
 */
public final class ExcelParseHelper {

    private ExcelParseHelper() {}

    /**
     * 读取卷烟投放基础信息 Excel。
     *
     * @param file Excel 文件
     * @return 行数据列表（列名->值）
     * @throws IOException 读取失败
     * @example 传入 cigarette_distribution_info.xlsx -> 返回包含 CIG_CODE/CIG_NAME 等列的行列表
     */
    public static List<Map<String, Object>> readCigaretteInfo(MultipartFile file) throws IOException {
        List<Map<String, Object>> data = new ArrayList<>();

        try (Workbook workbook = createWorkbook(file)) {
            Sheet sheet = workbook.getSheetAt(0);
            Row headerRow = sheet.getRow(0);
            if (headerRow == null) {
                return data;
            }

            List<String> headers = new ArrayList<>();
            for (Cell cell : headerRow) {
                headers.add(getCellValueAsString(cell));
            }

            for (int i = 1; i <= sheet.getLastRowNum(); i++) {
                Row row = sheet.getRow(i);
                if (row == null) {
                    continue;
                }
                Map<String, Object> rowData = new HashMap<>();
                for (int j = 0; j < headers.size() && j < row.getLastCellNum(); j++) {
                    Cell cell = row.getCell(j);
                    rowData.put(headers.get(j), getCellValue(cell));
                }
                data.add(rowData);
            }
        }
        return data;
    }

    /**
     * 读取客户基础信息 Excel。
     *
     * @param file Excel 文件
     * @return 包含列顺序与行数据的封装
     * @throws IOException 读取失败
     * @example 传入 base_customer_info.xlsx -> 返回列集合与有效行列表
     */
    public static BaseCustomerExcelData readBaseCustomerInfo(MultipartFile file) throws IOException {
        List<Map<String, Object>> rows = new ArrayList<>();
        List<String> orderedColumns = new ArrayList<>();
        List<String> headerColumns = new ArrayList<>();
        Set<String> seenColumns = new LinkedHashSet<>();

        try (Workbook workbook = createWorkbook(file)) {
            Sheet sheet = workbook.getSheetAt(0);
            Row headerRow = sheet.getRow(0);
            if (headerRow == null) {
                return new BaseCustomerExcelData(orderedColumns, rows);
            }

            short lastCellNum = headerRow.getLastCellNum();
            for (int i = 0; i < lastCellNum; i++) {
                Cell cell = headerRow.getCell(i);
                String rawHeader = cell != null ? cell.toString() : "";
                String normalized = normalizeColumnName(rawHeader);
                if (normalized == null || normalized.isEmpty() || "ID".equals(normalized)) {
                    headerColumns.add(null);
                    continue;
                }
                headerColumns.add(normalized);
                if (seenColumns.add(normalized)) {
                    orderedColumns.add(normalized);
                }
            }

            for (int i = 1; i <= sheet.getLastRowNum(); i++) {
                Row row = sheet.getRow(i);
                if (row == null) {
                    continue;
                }

                Map<String, Object> rowData = new HashMap<>();
                boolean hasValue = false;
                for (int j = 0; j < headerColumns.size(); j++) {
                    String columnName = headerColumns.get(j);
                    if (columnName == null) {
                        continue;
                    }
                    Cell cell = row.getCell(j);
                    String cellValue = getCellValueAsString(cell);
                    if (cellValue != null) {
                        cellValue = cellValue.trim();
                    }
                    if (cellValue != null && cellValue.isEmpty()) {
                        cellValue = null;
                    }
                    if (cellValue != null) {
                        hasValue = true;
                    }
                    rowData.put(columnName, cellValue);
                }

                if (hasValue) {
                    rows.add(rowData);
                }
            }
        }

        return new BaseCustomerExcelData(orderedColumns, rows);
    }

    /**
     * 根据文件类型创建 Workbook。
     *
     * @param file Excel 文件
     * @return Workbook 实例
     * @throws IOException 流读取失败
     */
    public static Workbook createWorkbook(MultipartFile file) throws IOException {
        String fileName = file.getOriginalFilename();
        if (fileName != null && fileName.toLowerCase().endsWith(".xlsx")) {
            return new XSSFWorkbook(file.getInputStream());
        }
        return new HSSFWorkbook(file.getInputStream());
    }

    /**
     * 获取单元格值（保留原始类型）。
     */
    public static Object getCellValue(Cell cell) {
        if (cell == null) {
            return null;
        }
        switch (cell.getCellType()) {
            case STRING:
                return cell.getStringCellValue();
            case NUMERIC:
                if (DateUtil.isCellDateFormatted(cell)) {
                    return cell.getDateCellValue();
                }
                return BigDecimal.valueOf(cell.getNumericCellValue());
            case BOOLEAN:
                return cell.getBooleanCellValue();
            case FORMULA:
                return cell.getCellFormula();
            default:
                return null;
        }
    }

    /**
     * 获取单元格值的字符串表示。
     */
    public static String getCellValueAsString(Cell cell) {
        Object value = getCellValue(cell);
        return value != null ? value.toString() : "";
    }

    /**
     * 规范化列名（转大写、非字母数字替换为下划线、去首尾下划线）。
     */
    public static String normalizeColumnName(String header) {
        if (header == null) {
            return null;
        }
        String normalized = header.trim().toUpperCase().replaceAll("[^A-Z0-9]+", "_");
        normalized = normalized.replaceAll("_+", "_");
        normalized = normalized.replaceAll("^_|_$", "");
        if (normalized.isEmpty()) {
            return null;
        }
        if (Character.isDigit(normalized.charAt(0))) {
            normalized = "COL_" + normalized;
        }
        return normalized;
    }

    public static class BaseCustomerExcelData {
        private final List<String> columns;
        private final List<Map<String, Object>> rows;

        public BaseCustomerExcelData(List<String> columns, List<Map<String, Object>> rows) {
            this.columns = columns;
            this.rows = rows;
        }

        public List<String> getColumns() {
            return columns;
        }

        public List<Map<String, Object>> getRows() {
            return rows;
        }
    }
}

