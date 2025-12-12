package org.example.service.importer;

import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 导入校验工具。
 *
 * <p>职责：基础文件校验、必需列校验。</p>
 *
 * @author Robin
 * @version 1.0
 * @since 2025-12-11
 */
public final class ImportValidationHelper {

    private ImportValidationHelper() {}

    /**
     * 校验文件是否为 Excel。
     *
     * @param file 上传文件
     * @return 是否通过
     * @example 文件名以 .xlsx/.xls 结尾且非空 -> true
     */
    public static boolean validateExcelFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            return false;
        }
        String fileName = file.getOriginalFilename();
        if (fileName == null) {
            return false;
        }
        String lower = fileName.toLowerCase();
        return lower.endsWith(".xlsx") || lower.endsWith(".xls");
    }

    /**
     * 校验样例行是否包含必需列。
     *
     * @param sampleRow 样例行
     * @param requiredColumns 必需列
     * @return 是否全部存在
     * @example sampleRow 含 DELIVERY_METHOD 列 -> true，否则 false
     */
    public static boolean validateRequiredColumns(Map<String, Object> sampleRow, List<String> requiredColumns) {
        Set<String> actual = sampleRow.keySet();
        for (String col : requiredColumns) {
            if (!actual.contains(col)) {
                return false;
            }
        }
        return true;
    }
}

