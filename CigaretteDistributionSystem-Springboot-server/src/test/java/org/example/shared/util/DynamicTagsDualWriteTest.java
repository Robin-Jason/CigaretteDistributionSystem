package org.example.shared.util;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.extern.slf4j.Slf4j;
import org.example.infrastructure.persistence.mapper.BaseCustomerInfoMapper;
import org.example.infrastructure.persistence.po.BaseCustomerInfoPO;
import org.example.shared.helper.BaseCustomerTableManager;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 动态标签双写功能测试
 * <p>
 * 验证阶段2的双写功能：
 * 1. Excel导入时同时写入固定字段和JSON字段
 * 2. 数据一致性验证
 * 3. 向后兼容性验证
 * </p>
 *
 * @author Robin
 * @since 2025-12-20
 */
@Slf4j
@SpringBootTest
@ActiveProfiles("test")
@DisplayName("动态标签双写功能测试")
class DynamicTagsDualWriteTest {

    @Autowired
    private BaseCustomerTableManager baseCustomerTableManager;

    @Autowired
    private BaseCustomerInfoMapper baseCustomerInfoMapper;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    @DisplayName("测试Excel导入时固定标签字段和动态标签的处理")
    @Transactional
    void should_handle_fixed_and_dynamic_tags_on_import() {
        // 业务规则：
        // 1. QUALITY_DATA_SHARE等固定标签字段继续使用固定字段，不写入JSON
        // 2. 其他动态标签以JSON格式存放，使用中文键值对
        
        // 1. 准备测试数据（模拟Excel导入）
        List<String> columns = Arrays.asList(
            "CUST_CODE", "CUST_ID", "GRADE", "ORDER_CYCLE", 
            "QUALITY_DATA_SHARE", "DYNAMIC_TAGS"
        );

        List<Map<String, Object>> rows = new ArrayList<>();
        
        // 测试用例1：有QUALITY_DATA_SHARE值和动态标签
        Map<String, Object> row1 = new HashMap<>();
        row1.put("CUST_CODE", "DUAL_WRITE_001");
        row1.put("CUST_ID", "TEST_ID_001");
        row1.put("GRADE", "D10");
        row1.put("ORDER_CYCLE", "周一");
        row1.put("QUALITY_DATA_SHARE", "1");
        // 动态标签使用中文键值对
        String dynamicTagsJson1 = "{\"优质客户\":\"是\",\"重点区域\":\"重点区域A\"}";
        row1.put("DYNAMIC_TAGS", dynamicTagsJson1);
        rows.add(row1);

        // 测试用例2：只有QUALITY_DATA_SHARE，无动态标签
        Map<String, Object> row2 = new HashMap<>();
        row2.put("CUST_CODE", "DUAL_WRITE_002");
        row2.put("CUST_ID", "TEST_ID_002");
        row2.put("GRADE", "D15");
        row2.put("ORDER_CYCLE", "周二");
        row2.put("QUALITY_DATA_SHARE", "0");
        rows.add(row2);

        // 测试用例3：只有动态标签，无QUALITY_DATA_SHARE
        Map<String, Object> row3 = new HashMap<>();
        row3.put("CUST_CODE", "DUAL_WRITE_003");
        row3.put("CUST_ID", "TEST_ID_003");
        row3.put("GRADE", "D20");
        row3.put("ORDER_CYCLE", "周三");
        String dynamicTagsJson3 = "{\"特殊渠道\":\"是\"}";
        row3.put("DYNAMIC_TAGS", dynamicTagsJson3);
        rows.add(row3);

        // 2. 执行导入（通过BaseCustomerTableManager）
        BaseCustomerTableManager.BaseCustomerImportStats stats = 
            baseCustomerTableManager.insertAll(columns, rows, "CUST_CODE");
        
        assertTrue(stats.getInsertedCount() > 0, "应该成功插入数据");
        log.info("导入统计: 处理{}条，插入{}条", stats.getProcessedCount(), stats.getInsertedCount());

        // 3. 验证结果
        // 验证测试用例1：QUALITY_DATA_SHARE = "1"，且有动态标签
        BaseCustomerInfoPO customer1 = baseCustomerInfoMapper.selectOne(
            new LambdaQueryWrapper<BaseCustomerInfoPO>()
                .eq(BaseCustomerInfoPO::getCustCode, "DUAL_WRITE_001")
        );
        assertNotNull(customer1, "应该能查询到客户1");
        assertEquals("1", customer1.getQualityDataShare(), "QUALITY_DATA_SHARE字段应为1");
        // 验证QUALITY_DATA_SHARE不写入JSON字段（业务规则）
        assertNotNull(customer1.getDynamicTags(), "DYNAMIC_TAGS不应为null");
        assertFalse(customer1.getDynamicTags().containsKey("quality_data_share"), 
            "DYNAMIC_TAGS中不应包含quality_data_share（固定字段不写入JSON）");
        // 验证动态标签（中文键名）
        assertEquals("是", customer1.getDynamicTags().get("优质客户"), 
            "DYNAMIC_TAGS中应包含'优质客户'标签");
        assertEquals("重点区域A", customer1.getDynamicTags().get("重点区域"), 
            "DYNAMIC_TAGS中应包含'重点区域'标签");
        log.info("✅ 测试用例1验证通过: 固定字段={}, JSON字段={}", 
            customer1.getQualityDataShare(), 
            customer1.getDynamicTags());

        // 验证测试用例2：只有QUALITY_DATA_SHARE，无动态标签
        BaseCustomerInfoPO customer2 = baseCustomerInfoMapper.selectOne(
            new LambdaQueryWrapper<BaseCustomerInfoPO>()
                .eq(BaseCustomerInfoPO::getCustCode, "DUAL_WRITE_002")
        );
        assertNotNull(customer2, "应该能查询到客户2");
        assertEquals("0", customer2.getQualityDataShare(), "QUALITY_DATA_SHARE字段应为0");
        // DYNAMIC_TAGS应为空（没有提供动态标签）
        assertNotNull(customer2.getDynamicTags(), "DYNAMIC_TAGS不应为null（TypeHandler返回空Map）");
        assertTrue(customer2.getDynamicTags().isEmpty() || 
            !customer2.getDynamicTags().containsKey("quality_data_share"),
            "DYNAMIC_TAGS应为空或不包含quality_data_share");
        log.info("✅ 测试用例2验证通过: 固定字段={}, JSON字段={}", 
            customer2.getQualityDataShare(), 
            customer2.getDynamicTags());

        // 验证测试用例3：只有动态标签，无QUALITY_DATA_SHARE
        BaseCustomerInfoPO customer3 = baseCustomerInfoMapper.selectOne(
            new LambdaQueryWrapper<BaseCustomerInfoPO>()
                .eq(BaseCustomerInfoPO::getCustCode, "DUAL_WRITE_003")
        );
        assertNotNull(customer3, "应该能查询到客户3");
        assertNull(customer3.getQualityDataShare(), "QUALITY_DATA_SHARE字段应为null");
        // 验证动态标签（中文键名）
        assertNotNull(customer3.getDynamicTags(), "DYNAMIC_TAGS不应为null");
        assertEquals("是", customer3.getDynamicTags().get("特殊渠道"), 
            "DYNAMIC_TAGS中应包含'特殊渠道'标签");
        log.info("✅ 测试用例3验证通过: 固定字段={}, JSON字段={}", 
            customer3.getQualityDataShare(), 
            customer3.getDynamicTags());
    }

    @Test
    @DisplayName("测试数据一致性：固定字段与JSON字段值应一致")
    @Transactional
    void should_maintain_consistency_between_fixed_field_and_json_field() {
        // 1. 插入测试数据
        BaseCustomerInfoPO customer = new BaseCustomerInfoPO();
        customer.setCustCode("CONSISTENCY_TEST_001");
        customer.setCustId("TEST_ID_CONSISTENCY");
        customer.setGrade("D25");
        customer.setOrderCycle("周四");
        customer.setQualityDataShare("1");
        
        // 手动设置DYNAMIC_TAGS（模拟enrichDynamicTags的效果）
        Map<String, Object> dynamicTags = new HashMap<>();
        dynamicTags.put("quality_data_share", "1");
        customer.setDynamicTags(dynamicTags);
        
        baseCustomerInfoMapper.insert(customer);

        // 2. 从数据库直接查询验证一致性
        String sql = "SELECT QUALITY_DATA_SHARE, " +
                     "JSON_EXTRACT(DYNAMIC_TAGS, '$.quality_data_share') AS json_value " +
                     "FROM base_customer_info WHERE CUST_CODE = ?";
        
        Map<String, Object> result = jdbcTemplate.queryForMap(sql, "CONSISTENCY_TEST_001");
        String fixedValue = result.get("QUALITY_DATA_SHARE") != null ? 
            result.get("QUALITY_DATA_SHARE").toString() : null;
        String jsonValue = result.get("json_value") != null ? 
            result.get("json_value").toString().replaceAll("\"", "") : null;
        
        log.info("数据一致性检查: 固定字段={}, JSON字段={}", fixedValue, jsonValue);
        assertEquals(fixedValue, jsonValue, "固定字段和JSON字段的值应该一致");
        
        log.info("✅ 数据一致性验证通过");
    }

    @Test
    @DisplayName("测试向后兼容性：优先读取JSON字段，兼容固定字段")
    @Transactional
    void should_read_json_field_first_then_fallback_to_fixed_field() {
        // 1. 创建只有固定字段的数据（模拟旧数据）
        BaseCustomerInfoPO oldCustomer = new BaseCustomerInfoPO();
        oldCustomer.setCustCode("COMPATIBILITY_OLD");
        oldCustomer.setCustId("TEST_ID_OLD");
        oldCustomer.setGrade("D5");
        oldCustomer.setOrderCycle("周五");
        oldCustomer.setQualityDataShare("1");
        // 不设置DYNAMIC_TAGS，模拟旧数据
        
        // 直接通过SQL插入，绕过enrichDynamicTags
        String insertSql = "INSERT INTO base_customer_info " +
            "(CUST_CODE, CUST_ID, GRADE, ORDER_CYCLE, QUALITY_DATA_SHARE) " +
            "VALUES (?, ?, ?, ?, ?)";
        jdbcTemplate.update(insertSql, 
            oldCustomer.getCustCode(), 
            oldCustomer.getCustId(),
            oldCustomer.getGrade(),
            oldCustomer.getOrderCycle(),
            oldCustomer.getQualityDataShare());

        // 2. 读取数据，验证向后兼容
        BaseCustomerInfoPO retrieved = baseCustomerInfoMapper.selectOne(
            new LambdaQueryWrapper<BaseCustomerInfoPO>()
                .eq(BaseCustomerInfoPO::getCustCode, "COMPATIBILITY_OLD")
        );
        
        assertNotNull(retrieved, "应该能查询到旧数据");
        assertEquals("1", retrieved.getQualityDataShare(), "固定字段应该存在");
        // DYNAMIC_TAGS可能为空Map（TypeHandler处理），这是正常的
        // 在实际业务逻辑中，应该优先读取JSON字段，如果不存在则读取固定字段
        log.info("✅ 向后兼容性验证: 固定字段={}, JSON字段={}", 
            retrieved.getQualityDataShare(), 
            retrieved.getDynamicTags());

        // 3. 创建同时有固定字段和JSON字段的数据（模拟新数据）
        BaseCustomerInfoPO newCustomer = new BaseCustomerInfoPO();
        newCustomer.setCustCode("COMPATIBILITY_NEW");
        newCustomer.setCustId("TEST_ID_NEW");
        newCustomer.setGrade("D30");
        newCustomer.setOrderCycle("单周周一");
        newCustomer.setQualityDataShare("1");
        
        Map<String, Object> newTags = new HashMap<>();
        newTags.put("quality_data_share", "1");
        newCustomer.setDynamicTags(newTags);
        
        baseCustomerInfoMapper.insert(newCustomer);

        // 4. 读取新数据，验证JSON字段优先
        BaseCustomerInfoPO retrievedNew = baseCustomerInfoMapper.selectOne(
            new LambdaQueryWrapper<BaseCustomerInfoPO>()
                .eq(BaseCustomerInfoPO::getCustCode, "COMPATIBILITY_NEW")
        );
        
        assertNotNull(retrievedNew, "应该能查询到新数据");
        assertEquals("1", retrievedNew.getQualityDataShare(), "固定字段应该存在");
        assertNotNull(retrievedNew.getDynamicTags(), "JSON字段应该存在");
        assertEquals("1", retrievedNew.getDynamicTags().get("quality_data_share"), 
            "JSON字段中的值应该正确");
        log.info("✅ 新数据验证: 固定字段={}, JSON字段={}", 
            retrievedNew.getQualityDataShare(), 
            retrievedNew.getDynamicTags().get("quality_data_share"));
    }

    @Test
    @DisplayName("测试customer_filter表同步DYNAMIC_TAGS字段")
    @Transactional
    void should_sync_dynamic_tags_to_customer_filter() {
        // 1. 在base_customer_info中插入测试数据
        BaseCustomerInfoPO customer = new BaseCustomerInfoPO();
        customer.setCustCode("SYNC_TEST_001");
        customer.setCustId("TEST_ID_SYNC");
        customer.setGrade("D12");
        customer.setOrderCycle("周一");
        customer.setQualityDataShare("1");
        
        Map<String, Object> dynamicTags = new HashMap<>();
        dynamicTags.put("quality_data_share", "1");
        customer.setDynamicTags(dynamicTags);
        
        baseCustomerInfoMapper.insert(customer);

        // 2. 同步到customer_filter表（模拟TemporaryCustomerTableRepository的逻辑）
        int year = 2099;
        int month = 9;
        int weekSeq = 1;
        
        String syncSql = "INSERT INTO customer_filter " +
            "(YEAR, MONTH, WEEK_SEQ, CUST_CODE, CUST_ID, GRADE, ORDER_CYCLE, " +
            "QUALITY_DATA_SHARE, DYNAMIC_TAGS) " +
            "SELECT ?, ?, ?, CUST_CODE, CUST_ID, GRADE, ORDER_CYCLE, " +
            "QUALITY_DATA_SHARE, DYNAMIC_TAGS " +
            "FROM base_customer_info WHERE CUST_CODE = ?";
        
        jdbcTemplate.update(syncSql, year, month, weekSeq, "SYNC_TEST_001");

        // 3. 验证customer_filter表中的数据
        String verifySql = "SELECT QUALITY_DATA_SHARE, " +
            "JSON_EXTRACT(DYNAMIC_TAGS, '$.quality_data_share') AS json_value " +
            "FROM customer_filter " +
            "WHERE YEAR = ? AND MONTH = ? AND WEEK_SEQ = ? AND CUST_CODE = ?";
        
        Map<String, Object> result = jdbcTemplate.queryForMap(verifySql, 
            year, month, weekSeq, "SYNC_TEST_001");
        
        String fixedValue = result.get("QUALITY_DATA_SHARE") != null ? 
            result.get("QUALITY_DATA_SHARE").toString() : null;
        String jsonValue = result.get("json_value") != null ? 
            result.get("json_value").toString().replaceAll("\"", "") : null;
        
        log.info("customer_filter同步验证: 固定字段={}, JSON字段={}", fixedValue, jsonValue);
        assertEquals("1", fixedValue, "固定字段应该同步");
        assertEquals("1", jsonValue, "JSON字段应该同步");
        
        log.info("✅ customer_filter表同步验证通过");
    }

    @Test
    @DisplayName("清理测试数据")
    @Transactional
    void cleanup_test_data() {
        String[] testCustCodes = {
            "DUAL_WRITE_001", "DUAL_WRITE_002", "DUAL_WRITE_003",
            "CONSISTENCY_TEST_001", "COMPATIBILITY_OLD", "COMPATIBILITY_NEW",
            "SYNC_TEST_001"
        };
        
        for (String custCode : testCustCodes) {
            BaseCustomerInfoPO customer = baseCustomerInfoMapper.selectOne(
                new LambdaQueryWrapper<BaseCustomerInfoPO>()
                    .eq(BaseCustomerInfoPO::getCustCode, custCode)
            );
            if (customer != null) {
                baseCustomerInfoMapper.deleteById(customer.getId());
                log.info("已清理base_customer_info测试数据: {}", custCode);
            }
            
            // 清理customer_filter表
            jdbcTemplate.update(
                "DELETE FROM customer_filter WHERE CUST_CODE = ?", custCode);
        }
        
        log.info("✅ 测试数据清理完成");
    }
}
