package org.example.application.service.prediction;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@Slf4j
@SpringBootTest
@ActiveProfiles("test")
class PriceQueryTest {

    @Autowired
    private PredictionQueryService predictionQueryService;

    @Test
    void testListPriceByTime_2025_9_4() {
        List<Map<String, Object>> result = predictionQueryService.listPriceByTime(2025, 9, 4);
        
        log.info("========== 2025/9/4 价位段预测查询结果 ==========");
        log.info("查询到 {} 条记录", result.size());
        
        assertNotNull(result);
        assertEquals(37, result.size(), "应该有37条记录");
        
        if (!result.isEmpty()) {
            Map<String, Object> first = result.get(0);
            log.info("第一条: cig_code={}, cig_name={}, delivery_area={}", 
                    first.get("cig_code"), first.get("cig_name"), first.get("delivery_area"));
        }
    }
}
