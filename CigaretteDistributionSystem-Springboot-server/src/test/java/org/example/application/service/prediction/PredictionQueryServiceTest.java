package org.example.application.service.prediction;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 预测查询服务测试类
 *
 * @author Robin
 * @since 2025-12-22
 */
@Slf4j
@SpringBootTest
@ActiveProfiles("test")
@DisplayName("预测查询服务测试")
class PredictionQueryServiceTest {

    @Autowired
    private PredictionQueryService predictionQueryService;

    @Autowired
    private PartitionPredictionQueryService partitionPredictionQueryService;

    private static final Integer TEST_YEAR = 2025;
    private static final Integer TEST_MONTH = 9;
    private static final Integer TEST_WEEK_SEQ = 3;

    @Test
    @DisplayName("测试 listByTime - 查询预测数据")
    void testListByTime() {
        log.info("========== 测试 listByTime ==========");
        
        List<Map<String, Object>> result = predictionQueryService.listByTime(TEST_YEAR, TEST_MONTH, TEST_WEEK_SEQ);
        
        assertNotNull(result, "查询结果不应为null");
        log.info("查询到 {} 条预测记录", result.size());
        
        if (!result.isEmpty()) {
            Map<String, Object> first = result.get(0);
            log.info("第一条记录: cig_code={}, cig_name={}, delivery_area={}", 
                    first.get("cig_code"), first.get("cig_name"), first.get("delivery_area"));
            
            // 验证字段存在
            assertTrue(first.containsKey("cig_code"), "应包含 cig_code 字段");
            assertTrue(first.containsKey("cig_name"), "应包含 cig_name 字段");
            assertTrue(first.containsKey("delivery_area"), "应包含 delivery_area 字段");
            assertTrue(first.containsKey("D30"), "应包含 D30 字段");
            assertTrue(first.containsKey("D1"), "应包含 D1 字段");
        }
        
        log.info("✅ listByTime 测试通过");
    }

    @Test
    @DisplayName("测试 listPriceByTime - 查询价位段预测数据")
    void testListPriceByTime() {
        log.info("========== 测试 listPriceByTime ==========");
        
        List<Map<String, Object>> result = predictionQueryService.listPriceByTime(TEST_YEAR, TEST_MONTH, TEST_WEEK_SEQ);
        
        assertNotNull(result, "查询结果不应为null");
        log.info("查询到 {} 条价位段预测记录", result.size());
        
        if (!result.isEmpty()) {
            Map<String, Object> first = result.get(0);
            log.info("第一条记录: cig_code={}, cig_name={}, delivery_area={}", 
                    first.get("cig_code"), first.get("cig_name"), first.get("delivery_area"));
        }
        
        log.info("✅ listPriceByTime 测试通过");
    }

    @Test
    @DisplayName("测试 queryPredictionByTime - 查询预测实体列表")
    void testQueryPredictionByTime() {
        log.info("========== 测试 queryPredictionByTime ==========");
        
        List<org.example.infrastructure.persistence.po.CigaretteDistributionPredictionPO> result = 
                partitionPredictionQueryService.queryPredictionByTime(TEST_YEAR, TEST_MONTH, TEST_WEEK_SEQ);
        
        assertNotNull(result, "查询结果不应为null");
        log.info("查询到 {} 条预测实体记录", result.size());
        
        if (!result.isEmpty()) {
            org.example.infrastructure.persistence.po.CigaretteDistributionPredictionPO first = result.get(0);
            log.info("第一条记录: cigCode={}, cigName={}, deliveryArea={}", 
                    first.getCigCode(), first.getCigName(), first.getDeliveryArea());
            
            assertNotNull(first.getCigCode(), "cigCode 不应为null");
            assertNotNull(first.getCigName(), "cigName 不应为null");
        }
        
        log.info("✅ queryPredictionByTime 测试通过");
    }

    @Test
    @DisplayName("测试空分区查询")
    void testQueryEmptyPartition() {
        log.info("========== 测试空分区查询 ==========");
        
        // 查询一个不存在的分区
        List<Map<String, Object>> result = predictionQueryService.listByTime(2099, 1, 1);
        
        assertNotNull(result, "查询结果不应为null");
        assertTrue(result.isEmpty(), "不存在的分区应返回空列表");
        
        log.info("✅ 空分区查询测试通过");
    }
}
