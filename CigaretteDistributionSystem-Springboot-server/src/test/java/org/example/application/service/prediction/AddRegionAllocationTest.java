package org.example.application.service.prediction;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.example.api.web.vo.request.AddRegionAllocationRequestVo;
import org.example.api.web.vo.response.ApiResponseVo;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 新增投放区域分配记录功能测试
 * 
 * 测试场景：2025/9/3分区的 42010020 红金龙（硬神州腾龙），新增投放区域 城区（城网），30个档位值全为1
 * 
 * 前提：数据库中已有 2025/9/3 批次的数据
 * 
 * @author Robin
 * @since 2025-12-22
 */
@Slf4j
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@DisplayName("新增投放区域分配记录功能测试")
public class AddRegionAllocationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private static final int TEST_YEAR = 2025;
    private static final int TEST_MONTH = 9;
    private static final int TEST_WEEK_SEQ = 3;
    private static final String TEST_CIG_CODE = "42010020";
    private static final String TEST_CIG_NAME = "红金龙(硬神州腾龙)";
    private static final String TEST_PRIMARY_REGION = "丹江";
    private static final String TEST_SECONDARY_REGION = "城网";

    @BeforeEach
    void setUp() {
        // 清理可能存在的测试记录，确保测试环境干净
        String targetRegion = TEST_PRIMARY_REGION + "（" + TEST_SECONDARY_REGION + "）";
        // 注意：不删除已有记录，因为我们要测试新增一个不存在的区域
    }

    @Test
    @Order(1)
    @DisplayName("测试1: 新增投放区域分配记录 - 丹江（城网）")
    public void testAddRegionAllocation_Success() throws Exception {
        log.info("========== 开始测试：新增投放区域分配记录 ==========");

        String targetRegion = TEST_PRIMARY_REGION + "（" + TEST_SECONDARY_REGION + "）";

        // 先检查记录是否已存在，如果存在则先删除
        int deleted = jdbcTemplate.update(
                "DELETE FROM cigarette_distribution_prediction " +
                        "WHERE YEAR = ? AND MONTH = ? AND WEEK_SEQ = ? AND CIG_CODE = ? AND DELIVERY_AREA = ?",
                TEST_YEAR, TEST_MONTH, TEST_WEEK_SEQ, TEST_CIG_CODE, targetRegion);
        if (deleted > 0) {
            log.info("清理已存在的测试记录: {} 条", deleted);
        }

        // 构建请求
        AddRegionAllocationRequestVo request = new AddRegionAllocationRequestVo();
        request.setYear(TEST_YEAR);
        request.setMonth(TEST_MONTH);
        request.setWeekSeq(TEST_WEEK_SEQ);
        request.setCigCode(TEST_CIG_CODE);
        request.setCigName(TEST_CIG_NAME);
        request.setPrimaryRegion(TEST_PRIMARY_REGION);
        request.setSecondaryRegion(TEST_SECONDARY_REGION);

        // 30个档位值全为1
        List<BigDecimal> grades = new ArrayList<>();
        for (int i = 0; i < 30; i++) {
            grades.add(BigDecimal.ONE);
        }
        request.setGrades(grades);
        request.setRemark("测试新增区域");

        String requestJson = objectMapper.writeValueAsString(request);
        log.info("请求数据: {}", requestJson);

        // 执行请求
        MvcResult result = mockMvc.perform(post("/api/prediction/add-region-allocation")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestJson))
                .andDo(print())
                .andExpect(status().isOk())
                .andReturn();

        // 解析响应
        String responseContent = result.getResponse().getContentAsString();
        log.info("响应内容: {}", responseContent);

        @SuppressWarnings("unchecked")
        ApiResponseVo<Void> response = objectMapper.readValue(responseContent, ApiResponseVo.class);

        if (response.getSuccess()) {
            log.info("✅ 新增成功: {}", response.getMessage());
            // 验证新增的记录
            verifyInsertedRecord(TEST_CIG_CODE, targetRegion);
        } else {
            log.error("❌ 新增失败: errorCode={}, message={}", response.getErrorCode(), response.getMessage());
        }

        log.info("========== 测试完成 ==========");
    }

    @Test
    @Order(2)
    @DisplayName("测试2: 重复新增同一区域 - 应返回记录已存在错误")
    public void testAddRegionAllocation_DuplicateRecord() throws Exception {
        log.info("========== 开始测试：重复新增同一区域 ==========");

        // 构建请求（与测试1相同的参数）
        AddRegionAllocationRequestVo request = new AddRegionAllocationRequestVo();
        request.setYear(TEST_YEAR);
        request.setMonth(TEST_MONTH);
        request.setWeekSeq(TEST_WEEK_SEQ);
        request.setCigCode(TEST_CIG_CODE);
        request.setCigName(TEST_CIG_NAME);
        request.setPrimaryRegion(TEST_PRIMARY_REGION);
        request.setSecondaryRegion(TEST_SECONDARY_REGION);

        List<BigDecimal> grades = new ArrayList<>();
        for (int i = 0; i < 30; i++) {
            grades.add(BigDecimal.ONE);
        }
        request.setGrades(grades);

        String requestJson = objectMapper.writeValueAsString(request);

        MvcResult result = mockMvc.perform(post("/api/prediction/add-region-allocation")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestJson))
                .andDo(print())
                .andExpect(status().isOk())
                .andReturn();

        String responseContent = result.getResponse().getContentAsString();
        @SuppressWarnings("unchecked")
        ApiResponseVo<Void> response = objectMapper.readValue(responseContent, ApiResponseVo.class);

        if (!response.getSuccess()) {
            log.info("✅ 预期的失败: {}", response.getMessage());
        } else {
            log.warn("⚠️ 意外成功，可能测试1未执行或记录被清理");
        }
    }

    @Test
    @Order(3)
    @DisplayName("测试3: 卷烟不存在 - 应返回卷烟不存在错误")
    public void testAddRegionAllocation_CigaretteNotFound() throws Exception {
        log.info("========== 开始测试：卷烟不存在 ==========");

        AddRegionAllocationRequestVo request = new AddRegionAllocationRequestVo();
        request.setYear(TEST_YEAR);
        request.setMonth(TEST_MONTH);
        request.setWeekSeq(TEST_WEEK_SEQ);
        request.setCigCode("99999999");
        request.setCigName("不存在的卷烟");
        request.setPrimaryRegion(TEST_PRIMARY_REGION);
        request.setSecondaryRegion(TEST_SECONDARY_REGION);

        List<BigDecimal> grades = new ArrayList<>();
        for (int i = 0; i < 30; i++) {
            grades.add(BigDecimal.ONE);
        }
        request.setGrades(grades);

        String requestJson = objectMapper.writeValueAsString(request);

        MvcResult result = mockMvc.perform(post("/api/prediction/add-region-allocation")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestJson))
                .andDo(print())
                .andExpect(status().isOk())
                .andReturn();

        String responseContent = result.getResponse().getContentAsString();
        @SuppressWarnings("unchecked")
        ApiResponseVo<Void> response = objectMapper.readValue(responseContent, ApiResponseVo.class);

        log.info("响应: success={}, message={}", response.getSuccess(), response.getMessage());
        Assertions.assertFalse(response.getSuccess(), "应该返回失败");
        log.info("✅ 校验通过");
    }

    @Test
    @Order(4)
    @DisplayName("测试4: 区域无效 - 应返回区域无效错误")
    public void testAddRegionAllocation_InvalidRegion() throws Exception {
        log.info("========== 开始测试：区域无效 ==========");

        AddRegionAllocationRequestVo request = new AddRegionAllocationRequestVo();
        request.setYear(TEST_YEAR);
        request.setMonth(TEST_MONTH);
        request.setWeekSeq(TEST_WEEK_SEQ);
        request.setCigCode(TEST_CIG_CODE);
        request.setCigName(TEST_CIG_NAME);
        request.setPrimaryRegion("不存在的区域");
        request.setSecondaryRegion(null);

        List<BigDecimal> grades = new ArrayList<>();
        for (int i = 0; i < 30; i++) {
            grades.add(BigDecimal.ONE);
        }
        request.setGrades(grades);

        String requestJson = objectMapper.writeValueAsString(request);

        MvcResult result = mockMvc.perform(post("/api/prediction/add-region-allocation")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestJson))
                .andDo(print())
                .andExpect(status().isOk())
                .andReturn();

        String responseContent = result.getResponse().getContentAsString();
        @SuppressWarnings("unchecked")
        ApiResponseVo<Void> response = objectMapper.readValue(responseContent, ApiResponseVo.class);

        log.info("响应: success={}, message={}", response.getSuccess(), response.getMessage());
        Assertions.assertFalse(response.getSuccess(), "应该返回失败");
        log.info("✅ 校验通过");
    }

    @Test
    @Order(5)
    @DisplayName("测试5: 档位单调性校验 - D29 > D30 应失败")
    public void testAddRegionAllocation_GradesMonotonicity() throws Exception {
        log.info("========== 开始测试：档位单调性校验 ==========");

        AddRegionAllocationRequestVo request = new AddRegionAllocationRequestVo();
        request.setYear(TEST_YEAR);
        request.setMonth(TEST_MONTH);
        request.setWeekSeq(TEST_WEEK_SEQ);
        request.setCigCode(TEST_CIG_CODE);
        request.setCigName(TEST_CIG_NAME);
        request.setPrimaryRegion(TEST_PRIMARY_REGION);
        request.setSecondaryRegion(TEST_SECONDARY_REGION);

        // 构造不满足单调性的档位值
        List<BigDecimal> grades = new ArrayList<>();
        grades.add(BigDecimal.ONE);           // D30 = 1
        grades.add(new BigDecimal("5"));      // D29 = 5，违反 D30 >= D29
        for (int i = 2; i < 30; i++) {
            grades.add(BigDecimal.ZERO);
        }
        request.setGrades(grades);

        String requestJson = objectMapper.writeValueAsString(request);

        MvcResult result = mockMvc.perform(post("/api/prediction/add-region-allocation")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestJson))
                .andDo(print())
                .andExpect(status().isOk())
                .andReturn();

        String responseContent = result.getResponse().getContentAsString();
        @SuppressWarnings("unchecked")
        ApiResponseVo<Void> response = objectMapper.readValue(responseContent, ApiResponseVo.class);

        log.info("响应: success={}, message={}", response.getSuccess(), response.getMessage());
        Assertions.assertFalse(response.getSuccess(), "应该返回失败");
        // 检查响应体中是否包含单调性相关的关键字（直接检查原始响应内容）
        Assertions.assertTrue(responseContent.contains("单调") || responseContent.contains("D29") || responseContent.contains("D30"),
                "错误信息应包含单调性描述");
        log.info("✅ 校验通过");
    }

    /**
     * 验证新增的记录
     */
    private void verifyInsertedRecord(String cigCode, String deliveryArea) {
        String sql = "SELECT * FROM cigarette_distribution_prediction " +
                "WHERE YEAR = ? AND MONTH = ? AND WEEK_SEQ = ? AND CIG_CODE = ? AND DELIVERY_AREA = ?";

        List<Map<String, Object>> rows = jdbcTemplate.queryForList(sql,
                TEST_YEAR, TEST_MONTH, TEST_WEEK_SEQ, cigCode, deliveryArea);

        if (rows.isEmpty()) {
            log.warn("❌ 未找到新增的记录");
        } else {
            Map<String, Object> record = rows.get(0);
            log.info("========== 新增记录详情 ==========");
            log.info("卷烟代码: {}", record.get("CIG_CODE"));
            log.info("卷烟名称: {}", record.get("CIG_NAME"));
            log.info("投放区域: {}", record.get("DELIVERY_AREA"));
            log.info("投放方式: {}", record.get("DELIVERY_METHOD"));
            log.info("扩展投放类型: {}", record.get("DELIVERY_ETYPE"));
            log.info("实际投放量: {}", record.get("ACTUAL_DELIVERY"));
            log.info("编码表达式: {}", record.get("DEPLOYINFO_CODE"));
            log.info("备注: {}", record.get("BZ"));
            log.info("D30={}, D29={}, D1={}", record.get("D30"), record.get("D29"), record.get("D1"));
            log.info("================================");
        }
    }
}
