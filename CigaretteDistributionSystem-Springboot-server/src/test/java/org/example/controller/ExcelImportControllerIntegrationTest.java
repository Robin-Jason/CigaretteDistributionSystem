package org.example.controller;

import org.example.dto.DataImportRequestDto;
import org.example.service.ExcelImportService;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockMultipartFile;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

/**
 * Excel 导入统一接口集成测试
 *
 * 覆盖两种场景：
 * 1) 客户基础信息表 + 卷烟投放基础信息表同时导入（全量模式）
 * 2) 仅导入卷烟投放基础信息表（客户表缺省）
 */
@SpringBootTest
public class ExcelImportControllerIntegrationTest {

    @Autowired
    private ExcelImportService excelImportService;

    private MockMultipartFile loadFile(String name) throws Exception {
        Path path = Path.of("excel", name);
        byte[] bytes = Files.readAllBytes(path);
        return new MockMultipartFile("file", name, "application/vnd.ms-excel", bytes);
    }

    @Test
    void importWithBothFiles_shouldSucceed() throws Exception {
        DataImportRequestDto req = new DataImportRequestDto();
        req.setBaseCustomerInfoFile(loadFile("base_customer_info.xlsx"));
        req.setCigaretteDistributionInfoFile(loadFile("cigarette_distribution_info.xlsx"));
        req.setYear(2025);
        req.setMonth(9);
        req.setWeekSeq(3);

        Map<String, Object> result = excelImportService.importData(req);
        Assertions.assertTrue(Boolean.TRUE.equals(result.get("success")), "Import should succeed with both files");
        Assertions.assertTrue(result.containsKey("baseCustomerInfoResult"), "Base customer result should exist");
        Assertions.assertTrue(result.containsKey("cigaretteDistributionInfoResult"), "Cigarette result should exist");
    }

    @Test
    void importWithOnlyCigaretteFile_shouldSkipBaseAndSucceed() throws Exception {
        DataImportRequestDto req = new DataImportRequestDto();
        req.setCigaretteDistributionInfoFile(loadFile("cigarette_distribution_info.xlsx"));
        req.setYear(2025);
        req.setMonth(9);
        req.setWeekSeq(3);

        Map<String, Object> result = excelImportService.importData(req);
        Assertions.assertTrue(Boolean.TRUE.equals(result.get("success")), "Import should succeed without base file");
        Assertions.assertTrue(result.containsKey("baseCustomerInfoResult"), "Base customer result should be present");
        Object baseMsg = result.get("baseCustomerInfoResult");
        Assertions.assertNotNull(baseMsg, "Base customer result should not be null even when skipped");
    }
}


