package org.example.controller;

import org.example.application.dto.importing.BaseCustomerInfoImportRequestDto;
import org.example.application.dto.importing.CigaretteImportRequestDto;
import org.example.application.service.importing.ExcelImportService;
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
 * Excel 导入接口集成测试
 *
 * 覆盖两个独立导入场景：
 * 1) 客户基础信息表导入（返回诚信互助小组映射）
 * 2) 卷烟投放基础信息表导入
 */
@SpringBootTest
public class ExcelImportControllerIntegrationTest {

    @Autowired
    private ExcelImportService excelImportService;

    private MockMultipartFile loadFile(String name) throws Exception {
        Path path = Paths.get("excel", name);
        byte[] bytes = Files.readAllBytes(path);
        return new MockMultipartFile("file", name, "application/vnd.ms-excel", bytes);
    }

    @Test
    void importBaseCustomerInfo_shouldSucceedAndReturnIntegrityGroupMapping() throws Exception {
        BaseCustomerInfoImportRequestDto req = new BaseCustomerInfoImportRequestDto();
        req.setFile(loadFile("base_customer_info.xlsx"));

        Map<String, Object> result = excelImportService.importBaseCustomerInfo(req);
        Assertions.assertTrue(Boolean.TRUE.equals(result.get("success")), "Import should succeed");
        Assertions.assertTrue(result.containsKey("integrityGroupMapping"), "Should return integrityGroupMapping");
        
        @SuppressWarnings("unchecked")
        Map<String, Object> mapping = (Map<String, Object>) result.get("integrityGroupMapping");
        Assertions.assertTrue((Boolean) mapping.get("updated"), "Mapping should be marked as updated");
    }

    @Test
    void importCigaretteDistributionInfo_shouldSucceed() throws Exception {
        CigaretteImportRequestDto req = new CigaretteImportRequestDto();
        req.setFile(loadFile("cigarette_distribution_info.xlsx"));
        req.setYear(2025);
        req.setMonth(9);
        req.setWeekSeq(3);

        Map<String, Object> result = excelImportService.importCigaretteDistributionInfo(req);
        Assertions.assertTrue(Boolean.TRUE.equals(result.get("success")), "Import should succeed");
        Assertions.assertTrue(result.containsKey("insertedCount"), "Should return insertedCount");
    }
}
