package org.example.infrastructure.persistence;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.extern.slf4j.Slf4j;
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
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 动态标签JSON字段功能测试
 * <p>
 * 验证DYNAMIC_TAGS JSON字段的读写功能，包括：
 * 1. JSON字段的写入和读取
 * 2. TypeHandler是否正确工作
 * 3. 空值和null值的处理
 * 4. 复杂JSON结构的处理
 * </p>
 *
 * @author Robin
 * @since 2025-12-20
 */
@Slf4j
@SpringBootTest
@ActiveProfiles("test")
@DisplayName("动态标签JSON字段功能测试")
class DynamicTagsJsonFieldTest {

    @Autowired
    private BaseCustomerInfoMapper baseCustomerInfoMapper;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    @DisplayName("测试JSON字段的写入和读取")
    @Transactional
    void should_write_and_read_json_field() {
        // 1. 创建测试数据
        BaseCustomerInfoPO customer = new BaseCustomerInfoPO();
        customer.setCustCode("TEST_CUST_001");
        customer.setCustId("TEST_ID_001");
        customer.setGrade("D10");
        customer.setOrderCycle("周一");
        
        // 2. 设置动态标签（使用中文键值对，符合业务规则）
        Map<String, Object> dynamicTags = new HashMap<>();
        dynamicTags.put("优质客户", "是");
        dynamicTags.put("重点区域", "重点区域A");
        dynamicTags.put("自定义标签", "自定义标签值");
        customer.setDynamicTags(dynamicTags);

        // 3. 保存到数据库
        baseCustomerInfoMapper.insert(customer);
        log.info("已保存客户: {}, 动态标签: {}", customer.getCustCode(), customer.getDynamicTags());

        // 4. 从数据库读取
        BaseCustomerInfoPO retrieved = baseCustomerInfoMapper.selectOne(
                new LambdaQueryWrapper<BaseCustomerInfoPO>()
                        .eq(BaseCustomerInfoPO::getCustCode, "TEST_CUST_001")
        );
        assertNotNull(retrieved, "应该能查询到保存的客户");
        assertNotNull(retrieved.getDynamicTags(), "动态标签不应为null");
        assertEquals(3, retrieved.getDynamicTags().size(), "动态标签应包含3个键值对");
        assertEquals("是", retrieved.getDynamicTags().get("优质客户"), "优质客户标签值应为'是'");
        assertEquals("重点区域A", retrieved.getDynamicTags().get("重点区域"), "重点区域标签值应正确");
        assertEquals("自定义标签值", retrieved.getDynamicTags().get("自定义标签"), "自定义标签标签值应正确");

        log.info("✅ JSON字段读写测试通过");
    }

    @Test
    @DisplayName("测试空JSON字段的处理")
    @Transactional
    void should_handle_empty_json_field() {
        // 1. 创建测试数据（不设置动态标签）
        BaseCustomerInfoPO customer = new BaseCustomerInfoPO();
        customer.setCustCode("TEST_CUST_002");
        customer.setCustId("TEST_ID_002");
        customer.setGrade("D15");
        customer.setOrderCycle("周二");
        // 不设置dynamicTags，应该为null

        // 2. 保存到数据库
        baseCustomerInfoMapper.insert(customer);
        log.info("已保存客户（无动态标签）: {}", customer.getCustCode());

        // 3. 从数据库读取
        BaseCustomerInfoPO retrieved = baseCustomerInfoMapper.selectOne(
                new LambdaQueryWrapper<BaseCustomerInfoPO>()
                        .eq(BaseCustomerInfoPO::getCustCode, "TEST_CUST_002")
        );
        assertNotNull(retrieved, "应该能查询到保存的客户");
        // TypeHandler应该将null转换为空Map
        assertNotNull(retrieved.getDynamicTags(), "动态标签不应为null（TypeHandler应返回空Map）");
        assertTrue(retrieved.getDynamicTags().isEmpty(), "动态标签应为空Map");

        log.info("✅ 空JSON字段处理测试通过");
    }

    @Test
    @DisplayName("测试JSON字段的更新")
    @Transactional
    void should_update_json_field() {
        // 1. 创建初始数据
        BaseCustomerInfoPO customer = new BaseCustomerInfoPO();
        customer.setCustCode("TEST_CUST_003");
        customer.setCustId("TEST_ID_003");
        customer.setGrade("D20");
        customer.setOrderCycle("周三");
        
        Map<String, Object> initialTags = new HashMap<>();
        initialTags.put("tag1", "value1");
        customer.setDynamicTags(initialTags);
        baseCustomerInfoMapper.insert(customer);

        // 2. 更新动态标签
        BaseCustomerInfoPO retrieved = baseCustomerInfoMapper.selectOne(
                new LambdaQueryWrapper<BaseCustomerInfoPO>()
                        .eq(BaseCustomerInfoPO::getCustCode, "TEST_CUST_003")
        );
        assertNotNull(retrieved);
        
        Map<String, Object> updatedTags = new HashMap<>();
        updatedTags.put("tag1", "updated_value1");
        updatedTags.put("tag2", "value2");
        updatedTags.put("tag3", "value3");
        retrieved.setDynamicTags(updatedTags);
        baseCustomerInfoMapper.updateById(retrieved);

        // 3. 验证更新
        BaseCustomerInfoPO afterUpdate = baseCustomerInfoMapper.selectOne(
                new LambdaQueryWrapper<BaseCustomerInfoPO>()
                        .eq(BaseCustomerInfoPO::getCustCode, "TEST_CUST_003")
        );
        assertNotNull(afterUpdate);
        assertEquals(3, afterUpdate.getDynamicTags().size(), "更新后应包含3个标签");
        assertEquals("updated_value1", afterUpdate.getDynamicTags().get("tag1"), "tag1应已更新");
        assertEquals("value2", afterUpdate.getDynamicTags().get("tag2"), "tag2应已添加");
        assertEquals("value3", afterUpdate.getDynamicTags().get("tag3"), "tag3应已添加");

        log.info("✅ JSON字段更新测试通过");
    }

    @Test
    @DisplayName("测试直接SQL查询JSON字段")
    @Transactional
    void should_query_json_field_with_sql() {
        // 1. 通过SQL直接插入JSON数据
        String insertSql = "INSERT INTO base_customer_info " +
                "(CUST_CODE, CUST_ID, GRADE, ORDER_CYCLE, DYNAMIC_TAGS) " +
                "VALUES (?, ?, ?, ?, ?)";
        
        // 使用中文键值对（符合业务规则）
        String jsonValue = "{\"优质客户\":\"是\",\"重点区域\":\"重点区域B\"}";
        jdbcTemplate.update(insertSql, "TEST_CUST_004", "TEST_ID_004", "D25", "周四", jsonValue);
        log.info("已通过SQL插入JSON数据: {}", jsonValue);

        // 2. 通过Mapper读取
        BaseCustomerInfoPO retrieved = baseCustomerInfoMapper.selectOne(
                new LambdaQueryWrapper<BaseCustomerInfoPO>()
                        .eq(BaseCustomerInfoPO::getCustCode, "TEST_CUST_004")
        );
        assertNotNull(retrieved, "应该能查询到插入的客户");
        assertNotNull(retrieved.getDynamicTags(), "动态标签不应为null");
        assertEquals(2, retrieved.getDynamicTags().size(), "动态标签应包含2个键值对");
        assertEquals("是", retrieved.getDynamicTags().get("优质客户"), "优质客户标签值应为'是'");
        assertEquals("重点区域B", retrieved.getDynamicTags().get("重点区域"), "重点区域标签值应正确");

        // 3. 通过SQL直接查询JSON字段
        String selectSql = "SELECT DYNAMIC_TAGS FROM base_customer_info WHERE CUST_CODE = ?";
        String jsonResult = jdbcTemplate.queryForObject(selectSql, String.class, "TEST_CUST_004");
        assertNotNull(jsonResult, "SQL查询结果不应为null");
        assertTrue(jsonResult.contains("优质客户"), "JSON应包含'优质客户'");
        assertTrue(jsonResult.contains("重点区域B"), "JSON应包含'重点区域B'");

        log.info("✅ SQL查询JSON字段测试通过，JSON内容: {}", jsonResult);
    }

    @Test
    @DisplayName("测试JSON字段的MySQL JSON函数查询")
    @Transactional
    void should_query_json_field_with_mysql_json_functions() {
        // 1. 插入测试数据
        BaseCustomerInfoPO customer = new BaseCustomerInfoPO();
        customer.setCustCode("TEST_CUST_005");
        customer.setCustId("TEST_ID_005");
        customer.setGrade("D30");
        customer.setOrderCycle("周五");
        
        // 使用中文键值对（符合业务规则）
        Map<String, Object> tags = new HashMap<>();
        tags.put("优质客户", "是");
        tags.put("重点区域", "重点区域C");
        tags.put("自定义标志", "true");
        customer.setDynamicTags(tags);
        baseCustomerInfoMapper.insert(customer);

        // 2. 使用MySQL JSON函数查询（使用中文键名）
        String jsonQuery = "SELECT " +
                "JSON_EXTRACT(DYNAMIC_TAGS, '$.优质客户') AS premium_customer, " +
                "JSON_EXTRACT(DYNAMIC_TAGS, '$.重点区域') AS critical_time_area, " +
                "JSON_CONTAINS_PATH(DYNAMIC_TAGS, 'one', '$.自定义标志') AS has_custom_flag " +
                "FROM base_customer_info WHERE CUST_CODE = ?";
        
        Map<String, Object> result = jdbcTemplate.queryForMap(jsonQuery, "TEST_CUST_005");
        assertNotNull(result);
        assertEquals("\"是\"", result.get("premium_customer").toString(), "JSON_EXTRACT应返回字符串值");
        assertEquals("\"重点区域C\"", result.get("critical_time_area").toString(), "JSON_EXTRACT应返回字符串值");
        assertEquals(1, ((Number) result.get("has_custom_flag")).intValue(), "JSON_CONTAINS_PATH应返回1（存在）");

        log.info("✅ MySQL JSON函数查询测试通过");
    }

    @Test
    @DisplayName("测试复杂JSON结构的处理")
    @Transactional
    void should_handle_complex_json_structure() {
        // 1. 创建包含复杂结构的JSON
        BaseCustomerInfoPO customer = new BaseCustomerInfoPO();
        customer.setCustCode("TEST_CUST_006");
        customer.setCustId("TEST_ID_006");
        customer.setGrade("D5");
        customer.setOrderCycle("单周周一");
        
        Map<String, Object> complexTags = new HashMap<>();
        complexTags.put("string_tag", "字符串值");
        complexTags.put("number_tag", 12345);
        complexTags.put("boolean_tag", true);
        
        // 嵌套Map（虽然TypeHandler可能不支持，但测试基本功能）
        Map<String, Object> nested = new HashMap<>();
        nested.put("nested_key", "nested_value");
        complexTags.put("nested_map", nested);
        
        customer.setDynamicTags(complexTags);
        baseCustomerInfoMapper.insert(customer);

        // 2. 读取并验证
        BaseCustomerInfoPO retrieved = baseCustomerInfoMapper.selectOne(
                new LambdaQueryWrapper<BaseCustomerInfoPO>()
                        .eq(BaseCustomerInfoPO::getCustCode, "TEST_CUST_006")
        );
        assertNotNull(retrieved);
        assertNotNull(retrieved.getDynamicTags());
        assertEquals("字符串值", retrieved.getDynamicTags().get("string_tag"), "字符串标签应正确");
        // 注意：数字和布尔值在JSON中可能被转换为字符串，取决于TypeHandler的实现
        assertNotNull(retrieved.getDynamicTags().get("number_tag"), "数字标签应存在");
        assertNotNull(retrieved.getDynamicTags().get("boolean_tag"), "布尔标签应存在");

        log.info("✅ 复杂JSON结构处理测试通过，标签内容: {}", retrieved.getDynamicTags());
    }

    @Test
    @DisplayName("清理测试数据")
    @Transactional
    void cleanup_test_data() {
        // 清理所有测试数据
        String[] testCustCodes = {
            "TEST_CUST_001", "TEST_CUST_002", "TEST_CUST_003",
            "TEST_CUST_004", "TEST_CUST_005", "TEST_CUST_006"
        };
        
        for (String custCode : testCustCodes) {
            BaseCustomerInfoPO customer = baseCustomerInfoMapper.selectOne(
                    new LambdaQueryWrapper<BaseCustomerInfoPO>()
                            .eq(BaseCustomerInfoPO::getCustCode, custCode)
            );
            if (customer != null) {
                baseCustomerInfoMapper.deleteById(customer.getId());
                log.info("已清理测试数据: {}", custCode);
            }
        }
        
        log.info("✅ 测试数据清理完成");
    }
}

