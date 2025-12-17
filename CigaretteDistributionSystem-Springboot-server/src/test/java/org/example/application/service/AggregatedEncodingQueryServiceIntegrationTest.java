package org.example.application.service;

import lombok.extern.slf4j.Slf4j;
import org.example.CigaretteDistributionApplication;
import org.example.application.service.encode.AggregatedEncodingQueryService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

/**
 * “单卷烟多区域聚合编码（懒加载）”集成测试。
 *
 * <p>该测试用于验证：</p>
 * <ul>
 *   <li>新增的仓储查询（按 cigCode 查询 prediction 分区）可用；</li>
 *   <li>聚合编码服务可在真实数据下运行并返回结果。</li>
 * </ul>
 */
@Slf4j
@SpringBootTest(classes = CigaretteDistributionApplication.class)
public class AggregatedEncodingQueryServiceIntegrationTest {

    @Autowired
    private AggregatedEncodingQueryService aggregatedEncodingQueryService;

    @Test
    void listAggregatedEncodings_in2025_9_3_shouldNotThrow() {
        assertDoesNotThrow(() -> {
            Integer year = 2025;
            Integer month = 9;
            Integer weekSeq = 3;

            // 选取一支多区域卷烟，便于观察聚合效果（如存在）
            String cigCode = "42010020";

            List<String> expressions = aggregatedEncodingQueryService.listAggregatedEncodings(year, month, weekSeq, cigCode);
            log.info("聚合编码表达式 - year={}, month={}, weekSeq={}, cigCode={}, size={}",
                    year, month, weekSeq, cigCode, expressions == null ? 0 : expressions.size());
            if (expressions != null) {
                expressions.forEach(expr -> log.info("  {}", expr));
            }
        });
    }
}


