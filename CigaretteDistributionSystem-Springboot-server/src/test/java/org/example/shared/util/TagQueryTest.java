package org.example.shared.util;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.extern.slf4j.Slf4j;
import org.example.domain.repository.FilterCustomerTableRepository;
import org.example.infrastructure.persistence.mapper.BaseCustomerInfoMapper;
import org.example.infrastructure.persistence.po.BaseCustomerInfoPO;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 标签查询功能测试
 * <p>
 * 验证固定标签字段和动态标签字段的查询功能：
 * 1. 固定标签字段（QUALITY_DATA_SHARE）的查询
 * 2. 动态标签字段（JSON字段，中文键名）的查询
 * 3. statGradesPartition查询逻辑
 * </p>
 *
 * @author Robin
 * @since 2025-12-20
 */
@Slf4j
@SpringBootTest
@ActiveProfiles("test")
@DisplayName("标签查询功能测试")
class TagQueryTest {

    @Autowired
    private BaseCustomerInfoMapper baseCustomerInfoMapper;

    @Autowired
    private FilterCustomerTableRepository filterCustomerTableRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    @DisplayName("测试固定标签字段查询（QUALITY_DATA_SHARE）")
    @Transactional
    void should_query_fixed_tag_field() {
        // 1. 创建测试数据
        BaseCustomerInfoPO customer1 = new BaseCustomerInfoPO();
        customer1.setCustCode("TAG_QUERY_001");
        customer1.setCustId("TEST_ID_001");
        customer1.setGrade("D10");
        customer1.setOrderCycle("周一");
        customer1.setQualityDataShare("1"); // 固定标签字段
        baseCustomerInfoMapper.insert(customer1);

        BaseCustomerInfoPO customer2 = new BaseCustomerInfoPO();
        customer2.setCustCode("TAG_QUERY_002");
        customer2.setCustId("TEST_ID_002");
        customer2.setGrade("D15");
        customer2.setOrderCycle("周二");
        customer2.setQualityDataShare("0"); // 固定标签字段
        baseCustomerInfoMapper.insert(customer2);

        // 2. 验证固定标签字段查询
        BaseCustomerInfoPO retrieved1 = baseCustomerInfoMapper.selectOne(
            new LambdaQueryWrapper<BaseCustomerInfoPO>()
                .eq(BaseCustomerInfoPO::getCustCode, "TAG_QUERY_001")
        );
        assertNotNull(retrieved1, "应该能查询到客户1");
        assertEquals("1", retrieved1.getQualityDataShare(), "QUALITY_DATA_SHARE应为1");

        BaseCustomerInfoPO retrieved2 = baseCustomerInfoMapper.selectOne(
            new LambdaQueryWrapper<BaseCustomerInfoPO>()
                .eq(BaseCustomerInfoPO::getCustCode, "TAG_QUERY_002")
        );
        assertNotNull(retrieved2, "应该能查询到客户2");
        assertEquals("0", retrieved2.getQualityDataShare(), "QUALITY_DATA_SHARE应为0");

        // 3. 使用DynamicTagHelper读取固定标签
        String value1 = DynamicTagHelper.getFixedTagValue(retrieved1, "QUALITY_DATA_SHARE");
        assertEquals("1", value1, "DynamicTagHelper应能读取固定标签");

        log.info("✅ 固定标签字段查询测试通过");
    }

    @Test
    @DisplayName("测试动态标签字段查询（JSON字段，中文键名）")
    @Transactional
    void should_query_dynamic_tag_field() {
        // 1. 创建测试数据（包含动态标签）
        BaseCustomerInfoPO customer = new BaseCustomerInfoPO();
        customer.setCustCode("TAG_QUERY_003");
        customer.setCustId("TEST_ID_003");
        customer.setGrade("D20");
        customer.setOrderCycle("周三");
        
        // 动态标签使用中文键值对
        Map<String, Object> dynamicTags = new HashMap<>();
        dynamicTags.put("优质客户", "是");
        dynamicTags.put("重点区域", "重点区域A");
        customer.setDynamicTags(dynamicTags);
        baseCustomerInfoMapper.insert(customer);

        // 2. 验证动态标签查询
        BaseCustomerInfoPO retrieved = baseCustomerInfoMapper.selectOne(
            new LambdaQueryWrapper<BaseCustomerInfoPO>()
                .eq(BaseCustomerInfoPO::getCustCode, "TAG_QUERY_003")
        );
        assertNotNull(retrieved, "应该能查询到客户");
        assertNotNull(retrieved.getDynamicTags(), "动态标签不应为null");
        assertEquals("是", retrieved.getDynamicTags().get("优质客户"), "优质客户标签值应为'是'");
        assertEquals("重点区域A", retrieved.getDynamicTags().get("重点区域"), "重点区域标签值应正确");

        // 3. 使用DynamicTagHelper读取动态标签
        String premiumCustomer = DynamicTagHelper.getDynamicTagValue(retrieved, "优质客户");
        assertEquals("是", premiumCustomer, "DynamicTagHelper应能读取动态标签");

        // 4. 使用SQL直接查询JSON字段（中文键名需要用引号包裹）
        String sql = "SELECT JSON_EXTRACT(DYNAMIC_TAGS, CONCAT('$.', '优质客户')) AS premium_customer " +
                     "FROM base_customer_info WHERE CUST_CODE = ?";
        String sqlResult = jdbcTemplate.queryForObject(sql, String.class, "TAG_QUERY_003");
        assertNotNull(sqlResult, "SQL查询结果不应为null");
        // JSON_EXTRACT返回的是带引号的JSON字符串，需要去掉引号
        String unquotedResult = sqlResult != null && sqlResult.startsWith("\"") && sqlResult.endsWith("\"") 
            ? sqlResult.substring(1, sqlResult.length() - 1) : sqlResult;
        assertEquals("是", unquotedResult, "SQL查询应返回'是'");

        log.info("✅ 动态标签字段查询测试通过");
    }

    @Test
    @DisplayName("测试statGradesPartition查询逻辑（固定标签字段）")
    @Transactional
    void should_query_stat_grades_partition_with_fixed_tag() {
        // 1. 准备测试数据并同步到customer_filter表
        int year = 2099;
        int month = 9;
        int weekSeq = 1;

        // 创建测试客户
        BaseCustomerInfoPO customer1 = new BaseCustomerInfoPO();
        customer1.setCustCode("STAT_QUERY_001");
        customer1.setCustId("TEST_ID_001");
        customer1.setGrade("D10");
        customer1.setOrderCycle("周一");
        customer1.setQualityDataShare("1"); // 固定标签字段
        customer1.setCompanyDistrict("江汉区");
        baseCustomerInfoMapper.insert(customer1);

        BaseCustomerInfoPO customer2 = new BaseCustomerInfoPO();
        customer2.setCustCode("STAT_QUERY_002");
        customer2.setCustId("TEST_ID_002");
        customer2.setGrade("D15");
        customer2.setOrderCycle("周二");
        customer2.setQualityDataShare("0"); // 固定标签字段
        customer2.setCompanyDistrict("江汉区");
        baseCustomerInfoMapper.insert(customer2);

        // 同步到customer_filter表
        String syncSql = "INSERT INTO customer_filter " +
            "(YEAR, MONTH, WEEK_SEQ, CUST_CODE, CUST_ID, GRADE, ORDER_CYCLE, " +
            "QUALITY_DATA_SHARE, COMPANY_DISTRICT) " +
            "SELECT ?, ?, ?, CUST_CODE, CUST_ID, GRADE, ORDER_CYCLE, " +
            "QUALITY_DATA_SHARE, COMPANY_DISTRICT " +
            "FROM base_customer_info WHERE CUST_CODE IN (?, ?)";
        jdbcTemplate.update(syncSql, year, month, weekSeq, 
            "STAT_QUERY_001", "STAT_QUERY_002");

        // 2. 测试固定标签字段查询（QUALITY_DATA_SHARE = '1'）
        Map<String, String> filters = new HashMap<>();
        filters.put("COMPANY_DISTRICT", "江汉区");
        
        List<Map<String, Object>> results = filterCustomerTableRepository.statGradesPartition(
            year, month, weekSeq,
            filters,
            "QUALITY_DATA_SHARE", // 固定标签字段
            "=",
            "1",
            null // orderCyclePattern
        );

        assertNotNull(results, "查询结果不应为null");
        assertFalse(results.isEmpty(), "应该查询到结果");
        
        // 验证结果：应该只有D10档位的客户（QUALITY_DATA_SHARE = '1'）
        boolean foundD10 = false;
        for (Map<String, Object> result : results) {
            String grade = (String) result.get("GRADE");
            Number count = (Number) result.get("CUSTOMER_COUNT");
            if ("D10".equals(grade)) {
                foundD10 = true;
                assertEquals(1, count.intValue(), "D10档位应该有1个客户");
            }
        }
        assertTrue(foundD10, "应该找到D10档位的客户");

        log.info("✅ statGradesPartition固定标签字段查询测试通过，结果: {}", results);
    }

    @Test
    @DisplayName("测试statGradesPartition查询逻辑（动态标签字段）")
    @Transactional
    void should_query_stat_grades_partition_with_dynamic_tag() {
        // 1. 准备测试数据并同步到customer_filter表
        int year = 2099;
        int month = 9;
        int weekSeq = 1;

        // 创建测试客户（包含动态标签）
        BaseCustomerInfoPO customer1 = new BaseCustomerInfoPO();
        customer1.setCustCode("STAT_QUERY_003");
        customer1.setCustId("TEST_ID_003");
        customer1.setGrade("D20");
        customer1.setOrderCycle("周三");
        customer1.setCompanyDistrict("江汉区");
        // 动态标签使用中文键值对
        Map<String, Object> dynamicTags1 = new HashMap<>();
        dynamicTags1.put("优质客户", "是");
        customer1.setDynamicTags(dynamicTags1);
        baseCustomerInfoMapper.insert(customer1);

        BaseCustomerInfoPO customer2 = new BaseCustomerInfoPO();
        customer2.setCustCode("STAT_QUERY_004");
        customer2.setCustId("TEST_ID_004");
        customer2.setGrade("D25");
        customer2.setOrderCycle("周四");
        customer2.setCompanyDistrict("江汉区");
        // 动态标签使用中文键值对
        Map<String, Object> dynamicTags2 = new HashMap<>();
        dynamicTags2.put("优质客户", "否");
        customer2.setDynamicTags(dynamicTags2);
        baseCustomerInfoMapper.insert(customer2);

        // 同步到customer_filter表（包含DYNAMIC_TAGS）
        String syncSql = "INSERT INTO customer_filter " +
            "(YEAR, MONTH, WEEK_SEQ, CUST_CODE, CUST_ID, GRADE, ORDER_CYCLE, " +
            "COMPANY_DISTRICT, DYNAMIC_TAGS) " +
            "SELECT ?, ?, ?, CUST_CODE, CUST_ID, GRADE, ORDER_CYCLE, " +
            "COMPANY_DISTRICT, DYNAMIC_TAGS " +
            "FROM base_customer_info WHERE CUST_CODE IN (?, ?)";
        jdbcTemplate.update(syncSql, year, month, weekSeq, 
            "STAT_QUERY_003", "STAT_QUERY_004");

        // 2. 测试动态标签字段查询（优质客户 = '是'）
        Map<String, String> filters = new HashMap<>();
        filters.put("COMPANY_DISTRICT", "江汉区");
        
        List<Map<String, Object>> results = filterCustomerTableRepository.statGradesPartition(
            year, month, weekSeq,
            filters,
            "优质客户", // 动态标签字段（中文键名）
            "=",
            "是",
            null // orderCyclePattern
        );

        assertNotNull(results, "查询结果不应为null");
        assertFalse(results.isEmpty(), "应该查询到结果");
        
        // 验证结果：应该只有D20档位的客户（优质客户 = '是'）
        boolean foundD20 = false;
        for (Map<String, Object> result : results) {
            String grade = (String) result.get("GRADE");
            Number count = (Number) result.get("CUSTOMER_COUNT");
            if ("D20".equals(grade)) {
                foundD20 = true;
                assertEquals(1, count.intValue(), "D20档位应该有1个客户");
            }
        }
        assertTrue(foundD20, "应该找到D20档位的客户");

        log.info("✅ statGradesPartition动态标签字段查询测试通过，结果: {}", results);
    }
}

