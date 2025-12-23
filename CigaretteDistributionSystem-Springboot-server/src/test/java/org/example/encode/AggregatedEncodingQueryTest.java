package org.example.encode;

import lombok.extern.slf4j.Slf4j;
import org.example.application.service.encode.AggregatedEncodingQueryService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 聚合编码查询服务测试 - 基于 2025/9/3 Prediction 分区数据
 */
@Slf4j
@SpringBootTest
public class AggregatedEncodingQueryTest {

    @Autowired
    private AggregatedEncodingQueryService aggregatedEncodingQueryService;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private static final int YEAR = 2025;
    private static final int MONTH = 9;
    private static final int WEEK_SEQ = 3;

    @Test
    public void testAggregatedEncodings() {
        log.info("==================== 聚合编码表达式测试 (2025/9/3) ====================");

        // 1) 查询分区中的卷烟列表
        String sql = "SELECT DISTINCT CIG_CODE, CIG_NAME FROM cigarette_distribution_prediction " +
                "WHERE YEAR = ? AND MONTH = ? AND WEEK_SEQ = ? ORDER BY CIG_CODE";
        List<Map<String, Object>> cigarettes = jdbcTemplate.queryForList(sql, YEAR, MONTH, WEEK_SEQ);

        if (cigarettes.isEmpty()) {
            log.warn("分区 {}-{}-{} 无数据，跳过测试", YEAR, MONTH, WEEK_SEQ);
            return;
        }

        log.info("分区中共有 {} 种卷烟", cigarettes.size());
        log.info("");

        // 2) 对每种卷烟查询聚合编码
        int successCount = 0;
        int emptyCount = 0;

        for (Map<String, Object> cig : cigarettes) {
            String cigCode = (String) cig.get("CIG_CODE");
            String cigName = (String) cig.get("CIG_NAME");

            List<String> encodings = aggregatedEncodingQueryService.listAggregatedEncodings(YEAR, MONTH, WEEK_SEQ, cigCode);

            if (encodings == null || encodings.isEmpty()) {
                log.warn("  {} ({}): 无编码结果", cigCode, cigName);
                emptyCount++;
            } else {
                log.info("【{}】{}", cigCode, cigName);
                for (String encoding : encodings) {
                    log.info("    {}", encoding);
                }
                log.info("");
                successCount++;
            }
        }

        log.info("==================== 测试结果汇总 ====================");
        log.info("总卷烟数: {}", cigarettes.size());
        log.info("有编码结果: {}", successCount);
        log.info("无编码结果: {}", emptyCount);

        // 断言：至少有一些卷烟有编码结果
        assertTrue(successCount > 0, "应该至少有一些卷烟有编码结果");
    }

    @Test
    public void testSpecificCigarette() {
        log.info("==================== 单支卷烟聚合编码测试 ====================");

        // 查询一支有多区域投放的卷烟
        String sql = "SELECT CIG_CODE, CIG_NAME, COUNT(DISTINCT DELIVERY_AREA) as area_count " +
                "FROM cigarette_distribution_prediction " +
                "WHERE YEAR = ? AND MONTH = ? AND WEEK_SEQ = ? " +
                "GROUP BY CIG_CODE, CIG_NAME " +
                "HAVING COUNT(DISTINCT DELIVERY_AREA) > 1 " +
                "ORDER BY area_count DESC LIMIT 5";

        List<Map<String, Object>> multiAreaCigs = jdbcTemplate.queryForList(sql, YEAR, MONTH, WEEK_SEQ);

        if (multiAreaCigs.isEmpty()) {
            log.info("无多区域投放卷烟，跳过测试");
            return;
        }

        log.info("多区域投放卷烟 TOP 5:");
        for (Map<String, Object> cig : multiAreaCigs) {
            String cigCode = (String) cig.get("CIG_CODE");
            String cigName = (String) cig.get("CIG_NAME");
            Number areaCount = (Number) cig.get("area_count");

            log.info("");
            log.info("【{}】{} (投放区域数: {})", cigCode, cigName, areaCount);

            // 查询原始记录
            String detailSql = "SELECT DELIVERY_AREA, DELIVERY_METHOD, DELIVERY_ETYPE, TAG " +
                    "FROM cigarette_distribution_prediction " +
                    "WHERE YEAR = ? AND MONTH = ? AND WEEK_SEQ = ? AND CIG_CODE = ?";
            List<Map<String, Object>> details = jdbcTemplate.queryForList(detailSql, YEAR, MONTH, WEEK_SEQ, cigCode);
            log.info("  原始记录:");
            for (Map<String, Object> d : details) {
                log.info("    区域={}, 方式={}, 扩展类型={}, 标签={}",
                        d.get("DELIVERY_AREA"), d.get("DELIVERY_METHOD"),
                        d.get("DELIVERY_ETYPE"), d.get("TAG"));
            }

            // 查询聚合编码
            List<String> encodings = aggregatedEncodingQueryService.listAggregatedEncodings(YEAR, MONTH, WEEK_SEQ, cigCode);
            log.info("  聚合编码:");
            if (encodings != null && !encodings.isEmpty()) {
                for (String encoding : encodings) {
                    log.info("    {}", encoding);
                }
            } else {
                log.info("    (无)");
            }
        }
    }
}
