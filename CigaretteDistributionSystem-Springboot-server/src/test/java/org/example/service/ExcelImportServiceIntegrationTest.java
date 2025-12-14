package org.example.service;

import org.example.application.dto.DataImportRequestDto;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockMultipartFile;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;

/**
 * Excel 导入服务集成测试。
 *
 * <p>使用项目根目录下 excel/ 目录的样例文件，直接调用 service 导入。</p>
 */
@SpringBootTest
class ExcelImportServiceIntegrationTest {

    @Autowired
    private org.example.application.service.ExcelImportService excelImportService;

    @Test
    void importFullData_withBaseAndCigaretteFiles_shouldSucceed() throws Exception {
        Path basePath = Paths.get("excel/base_customer_info.xlsx");
        Path cigPath = Paths.get("excel/cigarette_distribution_info.xlsx");

        MockMultipartFile baseFile = new MockMultipartFile(
                "baseCustomerInfoFile",
                "base_customer_info.xlsx",
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                Files.readAllBytes(basePath)
        );

        MockMultipartFile cigFile = new MockMultipartFile(
                "cigaretteDistributionInfoFile",
                "cigarette_distribution_info.xlsx",
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                Files.readAllBytes(cigPath)
        );

        DataImportRequestDto request = new DataImportRequestDto();
        request.setBaseCustomerInfoFile(baseFile);
        request.setCigaretteDistributionInfoFile(cigFile);
        request.setYear(2025);
        request.setMonth(9);
        request.setWeekSeq(3);

        Map<String, Object> result = excelImportService.importData(request);
        Assertions.assertTrue(Boolean.TRUE.equals(result.get("success")),
                "Import should succeed but got: " + result);
    }

    @Test
    void importOnlyCigaretteFile_shouldSucceed() throws Exception {
        Path cigPath = Paths.get("excel/cigarette_distribution_info.xlsx");

        MockMultipartFile cigFile = new MockMultipartFile(
                "cigaretteDistributionInfoFile",
                "cigarette_distribution_info.xlsx",
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                Files.readAllBytes(cigPath)
        );

        DataImportRequestDto request = new DataImportRequestDto();
        request.setCigaretteDistributionInfoFile(cigFile);
        request.setYear(2025);
        request.setMonth(9);
        request.setWeekSeq(3);

        Map<String, Object> result = excelImportService.importData(request);
        Assertions.assertTrue(Boolean.TRUE.equals(result.get("success")),
                "Import should succeed but got: " + result);
        Assertions.assertTrue(result.containsKey("baseCustomerInfoResult"),
                "baseCustomerInfoResult should be present even when base file is skipped");
    }
}

