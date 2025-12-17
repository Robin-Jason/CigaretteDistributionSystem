package org.example.api.web.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.example.api.web.vo.request.GenerateDistributionPlanRequestVo;
import org.example.api.web.vo.response.ApiResponseVo;
import org.example.api.web.vo.response.DataImportResponseVo;
import org.example.api.web.vo.response.GenerateDistributionPlanResponseVo;
import org.example.api.web.vo.response.PredictionQueryResponseVo;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * API集成测试
 * 
 * 测试内容：
 * 1. Excel导入功能测试
 * 2. 一键生成分配方案功能测试（2025年9月第3周）
 * 3. 查询API功能测试
 * 
 * @author Robin
 * @version 1.0
 */
@Slf4j
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class ApiIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private static final int TEST_YEAR = 2025;
    private static final int TEST_MONTH = 9;
    private static final int TEST_WEEK_SEQ = 3;

    /**
     * 加载Excel文件
     */
    private MockMultipartFile loadExcelFile(String fileName) throws Exception {
        Path filePath = Paths.get("excel", fileName);
        if (!Files.exists(filePath)) {
            throw new RuntimeException("Excel文件不存在: " + filePath.toAbsolutePath());
        }
        byte[] bytes = Files.readAllBytes(filePath);
        // 根据文件名确定正确的参数名
        String paramName;
        if (fileName.contains("base_customer")) {
            paramName = "baseCustomerInfoFile";
        } else if (fileName.contains("cigarette_distribution")) {
            paramName = "cigaretteDistributionInfoFile";
        } else {
            paramName = "file";
        }
        
        return new MockMultipartFile(
            paramName,
            fileName,
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
            bytes
        );
    }

    @Test
    @Order(1)
    @DisplayName("测试1: Excel导入功能 - 同时导入客户表和卷烟表")
    public void testExcelImport_WithBothFiles() throws Exception {
        log.info("========== 开始测试：Excel导入功能（同时导入两个文件） ==========");

        // 准备测试数据
        MockMultipartFile baseCustomerFile = loadExcelFile("base_customer_info.xlsx");
        MockMultipartFile cigaretteFile = loadExcelFile("cigarette_distribution_info.xlsx");

        // 执行请求
        MvcResult result = mockMvc.perform(multipart("/api/import/data")
                .file(baseCustomerFile)
                .file(cigaretteFile)
                .param("year", String.valueOf(TEST_YEAR))
                .param("month", String.valueOf(TEST_MONTH))
                .param("weekSeq", String.valueOf(TEST_WEEK_SEQ))
                .contentType(MediaType.MULTIPART_FORM_DATA))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").exists())
                .andReturn();

        // 验证响应
        String responseContent = result.getResponse().getContentAsString();
        log.info("导入响应: {}", responseContent);

        ApiResponseVo<DataImportResponseVo> response = objectMapper.readValue(
            responseContent,
            objectMapper.getTypeFactory().constructParametricType(
                ApiResponseVo.class,
                DataImportResponseVo.class
            )
        );

        assertNotNull(response, "响应不应为空");
        assertTrue(response.getSuccess(), "导入应该成功");
        assertNotNull(response.getData(), "响应数据不应为空");
        
        DataImportResponseVo data = response.getData();
        assertNotNull(data.getBaseCustomerInfoResult(), "客户表导入结果不应为空");
        assertNotNull(data.getCigaretteDistributionInfoResult(), "卷烟表导入结果不应为空");

        log.info("✅ Excel导入测试通过");
    }

    @Test
    @Order(2)
    @DisplayName("测试2: Excel导入功能 - 仅导入卷烟表")
    public void testExcelImport_WithOnlyCigaretteFile() throws Exception {
        log.info("========== 开始测试：Excel导入功能（仅导入卷烟表） ==========");

        // 准备测试数据
        MockMultipartFile cigaretteFile = loadExcelFile("cigarette_distribution_info.xlsx");

        // 执行请求
        MvcResult result = mockMvc.perform(multipart("/api/import/data")
                .file(cigaretteFile)
                .param("year", String.valueOf(TEST_YEAR))
                .param("month", String.valueOf(TEST_MONTH))
                .param("weekSeq", String.valueOf(TEST_WEEK_SEQ))
                .contentType(MediaType.MULTIPART_FORM_DATA))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").exists())
                .andReturn();

        // 验证响应
        String responseContent = result.getResponse().getContentAsString();
        log.info("导入响应: {}", responseContent);

        ApiResponseVo<DataImportResponseVo> response = objectMapper.readValue(
            responseContent,
            objectMapper.getTypeFactory().constructParametricType(
                ApiResponseVo.class,
                DataImportResponseVo.class
            )
        );

        assertNotNull(response, "响应不应为空");
        assertTrue(response.getSuccess(), "导入应该成功");
        assertNotNull(response.getData(), "响应数据不应为空");
        
        DataImportResponseVo data = response.getData();
        // 当未提供客户表时，应该有提示信息
        if (data.getBaseCustomerInfoNotice() != null) {
            log.info("客户表提示: {}", data.getBaseCustomerInfoNotice());
        }

        log.info("✅ Excel导入测试（仅卷烟表）通过");
    }

    @Test
    @Order(3)
    @DisplayName("测试3: 一键生成分配方案功能 - 2025年9月第3周")
    public void testGenerateDistributionPlan() throws Exception {
        log.info("========== 开始测试：一键生成分配方案（2025年9月第3周） ==========");

        // 准备请求数据
        GenerateDistributionPlanRequestVo requestVo = new GenerateDistributionPlanRequestVo();
        requestVo.setYear(TEST_YEAR);
        requestVo.setMonth(TEST_MONTH);
        requestVo.setWeekSeq(TEST_WEEK_SEQ);
        // 可选：设置市场类型比例
        // requestVo.setUrbanRatio(new BigDecimal("0.6"));
        // requestVo.setRuralRatio(new BigDecimal("0.4"));

        String requestJson = objectMapper.writeValueAsString(requestVo);
        log.info("请求数据: {}", requestJson);

        // 执行请求
        MvcResult result = mockMvc.perform(post("/api/calculate/generate-distribution-plan")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestJson))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").exists())
                .andReturn();

        // 验证响应
        String responseContent = result.getResponse().getContentAsString();
        log.info("生成分配方案响应: {}", responseContent);

        ApiResponseVo<GenerateDistributionPlanResponseVo> response = objectMapper.readValue(
            responseContent,
            objectMapper.getTypeFactory().constructParametricType(
                ApiResponseVo.class,
                GenerateDistributionPlanResponseVo.class
            )
        );

        assertNotNull(response, "响应不应为空");
        assertNotNull(response.getData(), "响应数据不应为空");
        
        GenerateDistributionPlanResponseVo data = response.getData();
        log.info("生成结果 - 年份: {}, 月份: {}, 周序号: {}", 
            data.getYear(), data.getMonth(), data.getWeekSeq());
        log.info("处理数量: {}, 总卷烟数: {}, 成功分配数: {}", 
            data.getProcessedCount(), data.getTotalCigarettes(), data.getSuccessfulAllocations());
        
        if (data.getProcessingTime() != null) {
            log.info("处理时间: {}", data.getProcessingTime());
        }

        // 如果成功，验证关键字段
        if (Boolean.TRUE.equals(data.getSuccess())) {
            assertNotNull(data.getYear(), "年份不应为空");
            assertNotNull(data.getMonth(), "月份不应为空");
            assertNotNull(data.getWeekSeq(), "周序号不应为空");
            
            // 查询并输出所有卷烟的误差
            log.info("========== 开始查询所有卷烟的误差 ==========");
            queryAndPrintCigaretteErrors(data.getYear(), data.getMonth(), data.getWeekSeq());
        }

        log.info("✅ 一键生成分配方案测试通过");
    }

    /**
     * 查询并输出所有卷烟的误差信息
     */
    private void queryAndPrintCigaretteErrors(int year, int month, int weekSeq) {
        try {
            // 查询所有卷烟的绝对误差
            String sql = "SELECT p.CIG_CODE AS cig_code, p.CIG_NAME AS cig_name, " +
                    "SUM(IFNULL(p.ACTUAL_DELIVERY,0)) AS actual_total, " +
                    "MAX(IFNULL(i.ADV,0)) AS adv_total, " +
                    "ABS(SUM(IFNULL(p.ACTUAL_DELIVERY,0))-MAX(IFNULL(i.ADV,0))) AS abs_error, " +
                    "CASE WHEN MAX(IFNULL(i.ADV,0)) > 0 " +
                    "THEN ABS(SUM(IFNULL(p.ACTUAL_DELIVERY,0))-MAX(IFNULL(i.ADV,0))) / MAX(IFNULL(i.ADV,0)) * 100 " +
                    "ELSE 0 END AS relative_error_percent " +
                    "FROM cigarette_distribution_prediction p " +
                    "JOIN cigarette_distribution_info i ON p.YEAR=i.YEAR AND p.MONTH=i.MONTH AND p.WEEK_SEQ=i.WEEK_SEQ " +
                    "AND p.CIG_CODE=i.CIG_CODE AND p.CIG_NAME=i.CIG_NAME " +
                    "WHERE p.YEAR=? AND p.MONTH=? AND p.WEEK_SEQ=? " +
                    "GROUP BY p.CIG_CODE, p.CIG_NAME " +
                    "ORDER BY abs_error DESC";

            List<Map<String, Object>> rows = jdbcTemplate.queryForList(sql, year, month, weekSeq);

            if (rows.isEmpty()) {
                log.warn("未找到任何卷烟数据，请确保已导入数据并生成分配方案");
                return;
            }

            log.info("========== 所有卷烟误差统计（共{}条） ==========", rows.size());
            log.info(String.format("%-15s %-30s %15s %15s %15s %15s", 
                "卷烟代码", "卷烟名称", "ADV总量", "实际总量", "绝对误差", "相对误差(%)"));
            log.info("--------------------------------------------------------------------------------------------------------");

            double totalAbsError = 0;
            double maxAbsError = 0;
            double maxRelativeError = 0;
            int errorCount = 0;

            for (Map<String, Object> row : rows) {
                String cigCode = (String) row.get("cig_code");
                String cigName = (String) row.get("cig_name");
                Number advTotal = (Number) row.get("adv_total");
                Number actualTotal = (Number) row.get("actual_total");
                Number absError = (Number) row.get("abs_error");
                Number relativeError = (Number) row.get("relative_error_percent");

                double adv = advTotal != null ? advTotal.doubleValue() : 0;
                double actual = actualTotal != null ? actualTotal.doubleValue() : 0;
                double absErr = absError != null ? absError.doubleValue() : 0;
                double relErr = relativeError != null ? relativeError.doubleValue() : 0;

                log.info(String.format("%-15s %-30s %15.2f %15.2f %15.2f %15.2f%%",
                    cigCode != null ? cigCode : "",
                    cigName != null ? (cigName.length() > 30 ? cigName.substring(0, 27) + "..." : cigName) : "",
                    adv, actual, absErr, relErr));

                totalAbsError += absErr;
                if (absErr > maxAbsError) {
                    maxAbsError = absErr;
                }
                if (relErr > maxRelativeError) {
                    maxRelativeError = relErr;
                }
                if (absErr > 0.01) { // 误差大于0.01的视为有误差
                    errorCount++;
                }
            }

            log.info("--------------------------------------------------------------------------------------------------------");
            log.info("误差统计汇总:");
            log.info("  总卷烟数: {}", rows.size());
            log.info("  有误差的卷烟数: {}", errorCount);
            log.info("  总绝对误差: {}", String.format("%.2f", totalAbsError));
            double avgError = rows.size() > 0 ? totalAbsError / rows.size() : 0.0;
            log.info("  平均绝对误差: {}", String.format("%.2f", avgError));
            log.info("  最大绝对误差: {}", String.format("%.2f", maxAbsError));
            log.info("  最大相对误差: {}%", String.format("%.2f", maxRelativeError));
            log.info("========== 误差统计完成 ==========");

        } catch (Exception e) {
            log.error("查询卷烟误差失败", e);
        }
    }

    @Test
    @Order(4)
    @DisplayName("测试4: 查询预测数据 - 按时间查询")
    public void testQueryPrediction_ListByTime() throws Exception {
        log.info("========== 开始测试：查询预测数据（按时间查询） ==========");

        // 执行请求
        MvcResult result = mockMvc.perform(get("/api/prediction/list-by-time")
                .param("year", String.valueOf(TEST_YEAR))
                .param("month", String.valueOf(TEST_MONTH))
                .param("weekSeq", String.valueOf(TEST_WEEK_SEQ))
                .contentType(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").exists())
                .andReturn();

        // 验证响应
        String responseContent = result.getResponse().getContentAsString();
        log.info("查询响应: {}", responseContent);

        ApiResponseVo<PredictionQueryResponseVo> response = objectMapper.readValue(
            responseContent,
            objectMapper.getTypeFactory().constructParametricType(
                ApiResponseVo.class,
                PredictionQueryResponseVo.class
            )
        );

        assertNotNull(response, "响应不应为空");
        assertTrue(response.getSuccess(), "查询应该成功");
        assertNotNull(response.getData(), "响应数据不应为空");
        
        PredictionQueryResponseVo data = response.getData();
        assertNotNull(data.getData(), "预测数据列表不应为空");
        assertNotNull(data.getTotal(), "总数不应为空");
        
        log.info("查询结果 - 总记录数: {}", data.getTotal());
        if (data.getData() != null && !data.getData().isEmpty()) {
            log.info("第一条记录: {}", data.getData().get(0));
        }

        log.info("✅ 查询预测数据测试通过");
    }

    @Test
    @Order(5)
    @DisplayName("测试5: 查询价位段预测数据")
    public void testQueryPrediction_ListPriceByTime() throws Exception {
        log.info("========== 开始测试：查询价位段预测数据 ==========");

        // 执行请求
        MvcResult result = mockMvc.perform(get("/api/prediction/list-price-by-time")
                .param("year", String.valueOf(TEST_YEAR))
                .param("month", String.valueOf(TEST_MONTH))
                .param("weekSeq", String.valueOf(TEST_WEEK_SEQ))
                .contentType(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").exists())
                .andReturn();

        // 验证响应
        String responseContent = result.getResponse().getContentAsString();
        log.info("查询响应: {}", responseContent);

        ApiResponseVo<PredictionQueryResponseVo> response = objectMapper.readValue(
            responseContent,
            objectMapper.getTypeFactory().constructParametricType(
                ApiResponseVo.class,
                PredictionQueryResponseVo.class
            )
        );

        assertNotNull(response, "响应不应为空");
        assertTrue(response.getSuccess(), "查询应该成功");
        assertNotNull(response.getData(), "响应数据不应为空");
        
        PredictionQueryResponseVo data = response.getData();
        assertNotNull(data.getData(), "价位段预测数据列表不应为空");
        assertNotNull(data.getTotal(), "总数不应为空");
        
        log.info("查询结果 - 总记录数: {}", data.getTotal());
        if (data.getData() != null && !data.getData().isEmpty()) {
            log.info("第一条记录: {}", data.getData().get(0));
        }

        log.info("✅ 查询价位段预测数据测试通过");
    }

    @Test
    @Order(6)
    @DisplayName("测试6: 计算总实际投放量")
    public void testCalculateTotalActualDelivery() throws Exception {
        log.info("========== 开始测试：计算总实际投放量 ==========");

        // 执行请求
        MvcResult result = mockMvc.perform(post("/api/calculate/total-actual-delivery")
                .param("year", String.valueOf(TEST_YEAR))
                .param("month", String.valueOf(TEST_MONTH))
                .param("weekSeq", String.valueOf(TEST_WEEK_SEQ))
                .contentType(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").exists())
                .andReturn();

        // 验证响应
        String responseContent = result.getResponse().getContentAsString();
        log.info("计算总实际投放量响应: {}", responseContent);

        // 使用TypeReference来避免类型警告
        com.fasterxml.jackson.core.type.TypeReference<Map<String, Object>> typeRef = 
            new com.fasterxml.jackson.core.type.TypeReference<Map<String, Object>>() {};
        Map<String, Object> responseMap = objectMapper.readValue(responseContent, typeRef);
        
        assertNotNull(responseMap, "响应不应为空");
        assertTrue(responseMap.containsKey("success"), "响应应包含success字段");
        
        if (Boolean.TRUE.equals(responseMap.get("success"))) {
            Object dataObj = responseMap.get("data");
            if (dataObj instanceof Map) {
                @SuppressWarnings("unchecked")
                Map<String, Object> data = (Map<String, Object>) dataObj;
                log.info("总实际投放量数据: {}", data);
                if (data.containsKey("data")) {
                    log.info("投放量详情: {}", data.get("data"));
                }
            }
        }

        log.info("✅ 计算总实际投放量测试通过");
    }

    @Test
    @Order(7)
    @DisplayName("测试7: 完整流程测试 - 导入 -> 生成分配方案 -> 查询")
    public void testCompleteWorkflow() throws Exception {
        log.info("========== 开始测试：完整流程（导入 -> 生成分配方案 -> 查询） ==========");

        // 步骤1: 导入数据
        log.info("步骤1: 导入Excel数据...");
        MockMultipartFile cigaretteFile = loadExcelFile("cigarette_distribution_info.xlsx");

        MvcResult importResult = mockMvc.perform(multipart("/api/import/data")
                .file(cigaretteFile)
                .param("year", String.valueOf(TEST_YEAR))
                .param("month", String.valueOf(TEST_MONTH))
                .param("weekSeq", String.valueOf(TEST_WEEK_SEQ))
                .contentType(MediaType.MULTIPART_FORM_DATA))
                .andDo(print())
                .andExpect(status().isOk())
                .andReturn();

        String importResponse = importResult.getResponse().getContentAsString();
        ApiResponseVo<DataImportResponseVo> importResponseObj = objectMapper.readValue(
            importResponse,
            objectMapper.getTypeFactory().constructParametricType(
                ApiResponseVo.class,
                DataImportResponseVo.class
            )
        );
        
        assertTrue(importResponseObj.getSuccess(), "导入应该成功");
        log.info("✅ 步骤1完成：数据导入成功");

        // 等待一下，确保数据已写入
        Thread.sleep(1000);

        // 步骤2: 生成分配方案
        log.info("步骤2: 生成分配方案...");
        GenerateDistributionPlanRequestVo requestVo = new GenerateDistributionPlanRequestVo();
        requestVo.setYear(TEST_YEAR);
        requestVo.setMonth(TEST_MONTH);
        requestVo.setWeekSeq(TEST_WEEK_SEQ);

        String requestJson = objectMapper.writeValueAsString(requestVo);
        MvcResult generateResult = mockMvc.perform(post("/api/calculate/generate-distribution-plan")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestJson))
                .andDo(print())
                .andExpect(status().isOk())
                .andReturn();

        String generateResponse = generateResult.getResponse().getContentAsString();
        ApiResponseVo<GenerateDistributionPlanResponseVo> generateResponseObj = objectMapper.readValue(
            generateResponse,
            objectMapper.getTypeFactory().constructParametricType(
                ApiResponseVo.class,
                GenerateDistributionPlanResponseVo.class
            )
        );
        
        assertNotNull(generateResponseObj.getData(), "生成分配方案响应数据不应为空");
        log.info("✅ 步骤2完成：分配方案生成完成");

        // 等待一下，确保数据已写入
        Thread.sleep(1000);

        // 步骤3: 查询预测数据
        log.info("步骤3: 查询预测数据...");
        MvcResult queryResult = mockMvc.perform(get("/api/prediction/list-by-time")
                .param("year", String.valueOf(TEST_YEAR))
                .param("month", String.valueOf(TEST_MONTH))
                .param("weekSeq", String.valueOf(TEST_WEEK_SEQ))
                .contentType(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isOk())
                .andReturn();

        String queryResponse = queryResult.getResponse().getContentAsString();
        ApiResponseVo<PredictionQueryResponseVo> queryResponseObj = objectMapper.readValue(
            queryResponse,
            objectMapper.getTypeFactory().constructParametricType(
                ApiResponseVo.class,
                PredictionQueryResponseVo.class
            )
        );
        
        assertTrue(queryResponseObj.getSuccess(), "查询应该成功");
        assertNotNull(queryResponseObj.getData(), "查询响应数据不应为空");
        log.info("✅ 步骤3完成：查询到 {} 条记录", queryResponseObj.getData().getTotal());

        log.info("========== 完整流程测试通过 ==========");
    }
}

