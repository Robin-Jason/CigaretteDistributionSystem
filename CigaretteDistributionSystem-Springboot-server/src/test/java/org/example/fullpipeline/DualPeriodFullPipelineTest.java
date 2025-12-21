package org.example.fullpipeline;

import lombok.extern.slf4j.Slf4j;
import org.example.application.service.calculate.DistributionCalculateService;
import org.example.application.service.coordinator.RegionCustomerStatisticsBuildService;
import org.example.shared.util.PartitionTableManager;
import org.example.application.dto.GenerateDistributionPlanRequestDto;
import org.example.application.dto.GenerateDistributionPlanResponseDto;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

import java.util.*;

/**
 * 双时间段全链路测试
 * 测试2025/9/3和2025/9/4的完整分配流程
 * 
 * @author Robin
 * @since 2025-12-20
 */
@Slf4j
@SpringBootTest
@ActiveProfiles("test")
public class DualPeriodFullPipelineTest {

    @Autowired
    private RegionCustomerStatisticsBuildService regionCustomerStatisticsBuildService;

    @Autowired
    private DistributionCalculateService distributionCalculateService;

    @Autowired
    private PartitionTableManager partitionTableManager;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    public void testBothPeriods() {
        log.info("╔════════════════════════════════════════════════════════════════╗");
        log.info("║         双时间段全链路测试                                      ║");
        log.info("╚════════════════════════════════════════════════════════════════╝");

        // 测试2025/9/3
        testSinglePeriod(2025, 9, 3);

        log.info("\n\n");

        // 测试2025/9/4
        testSinglePeriod(2025, 9, 4);

        log.info("\n╔════════════════════════════════════════════════════════════════╗");
        log.info("║         双时间段测试完成                                        ║");
        log.info("╚════════════════════════════════════════════════════════════════╝");
    }

    private void testSinglePeriod(int year, int month, int weekSeq) {
        log.info("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        log.info("开始测试: {}年{}月第{}周", year, month, weekSeq);
        log.info("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");

        try {
            // 步骤1: 准备数据
            step1_prepareData(year, month, weekSeq);

            // 步骤2: 构建区域客户统计
            step2_buildRegionStatistics(year, month, weekSeq);

            // 步骤3: 执行分配
            step3_executeAllocation(year, month, weekSeq);

            // 步骤4: 验证编码表达式
            step4_verifyEncodingExpressions(year, month, weekSeq);

            // 步骤5: 验证误差
            step5_verifyAllocationError(year, month, weekSeq);

            log.info("✅ {}年{}月第{}周 测试完成\n", year, month, weekSeq);

        } catch (Exception e) {
            log.error("❌ {}年{}月第{}周 测试失败: {}", year, month, weekSeq, e.getMessage(), e);
        }
    }

    private void step1_prepareData(int year, int month, int weekSeq) {
        log.info("\n【步骤1】准备数据");

        // 确保分区存在
        partitionTableManager.ensurePartitionExists("region_customer_statistics", year, month, weekSeq);
        partitionTableManager.ensurePartitionExists("cigarette_distribution_prediction", year, month, weekSeq);
        partitionTableManager.ensurePartitionExists("cigarette_distribution_prediction_price", year, month, weekSeq);

        log.info("✓ 数据准备完成");
    }

    private void step2_buildRegionStatistics(int year, int month, int weekSeq) {
        log.info("\n【步骤2】构建区域客户统计");

        regionCustomerStatisticsBuildService.buildRegionCustomerStatistics(year, month, weekSeq);

        // 统计区域数量
        String sql = "SELECT COUNT(DISTINCT REGION) FROM region_customer_statistics " +
                "WHERE YEAR = ? AND MONTH = ? AND WEEK_SEQ = ?";
        Integer regionCount = jdbcTemplate.queryForObject(sql, Integer.class, year, month, weekSeq);

        log.info("✓ 区域客户统计构建完成，共 {} 个区域", regionCount);
    }

    private void step3_executeAllocation(int year, int month, int weekSeq) {
        log.info("\n【步骤3】执行分配算法");

        GenerateDistributionPlanRequestDto request = new GenerateDistributionPlanRequestDto();
        request.setYear(year);
        request.setMonth(month);
        request.setWeekSeq(weekSeq);

        GenerateDistributionPlanResponseDto response = distributionCalculateService.generateDistributionPlan(request);

        log.info("✓ 分配完成");
        log.info("  成功: {}", response.getSuccess());
        log.info("  处理数量: {}", response.getProcessedCount());
        log.info("  总卷烟数: {}", response.getTotalCigarettes());
        log.info("  成功分配数: {}", response.getSuccessfulAllocations());
        
        if (response.getAllocationResult() != null) {
            log.info("  总实际投放量: {}", response.getAllocationResult().get("totalActualDelivery"));
        }
    }

    private void step4_verifyEncodingExpressions(int year, int month, int weekSeq) {
        log.info("\n【步骤4】验证编码表达式");

        // 统计总记录数
        String totalSql = "SELECT COUNT(*) FROM cigarette_distribution_prediction " +
                "WHERE YEAR = ? AND MONTH = ? AND WEEK_SEQ = ?";
        Integer totalCount = jdbcTemplate.queryForObject(totalSql, Integer.class, year, month, weekSeq);

        // 统计带标签的记录
        String tagSql = "SELECT COUNT(*) FROM cigarette_distribution_prediction " +
                "WHERE YEAR = ? AND MONTH = ? AND WEEK_SEQ = ? " +
                "AND TAG IS NOT NULL AND TAG != ''";
        Integer tagCount = jdbcTemplate.queryForObject(tagSql, Integer.class, year, month, weekSeq);

        // 统计编码包含+a的记录
        String tagSuffixSql = "SELECT COUNT(*) FROM cigarette_distribution_prediction " +
                "WHERE YEAR = ? AND MONTH = ? AND WEEK_SEQ = ? " +
                "AND DEPLOYINFO_CODE LIKE '%+a%'";
        Integer tagSuffixCount = jdbcTemplate.queryForObject(tagSuffixSql, Integer.class, year, month, weekSeq);

        // 统计带标签但缺少+a的记录
        String missingSql = "SELECT COUNT(*) FROM cigarette_distribution_prediction " +
                "WHERE YEAR = ? AND MONTH = ? AND WEEK_SEQ = ? " +
                "AND TAG IS NOT NULL AND TAG != '' " +
                "AND (DEPLOYINFO_CODE IS NULL OR DEPLOYINFO_CODE NOT LIKE '%+a%')";
        Integer missingCount = jdbcTemplate.queryForObject(missingSql, Integer.class, year, month, weekSeq);

        log.info("  总记录数: {}", totalCount);
        log.info("  带标签记录数: {}", tagCount);
        log.info("  编码包含+a: {}", tagSuffixCount);
        log.info("  缺少+a后缀: {}", missingCount);

        if (tagCount != null && tagCount > 0) {
            double correctRate = (tagSuffixCount * 100.0) / tagCount;
            log.info("  标签编码正确率: {:.2f}%", correctRate);

            if (missingCount != null && missingCount > 0) {
                log.error("  ❌ 发现 {} 条记录缺少标签后缀！", missingCount);

                // 列出缺少标签后缀的卷烟
                String detailSql = "SELECT DISTINCT CIG_CODE, CIG_NAME, TAG, DEPLOYINFO_CODE " +
                        "FROM cigarette_distribution_prediction " +
                        "WHERE YEAR = ? AND MONTH = ? AND WEEK_SEQ = ? " +
                        "AND TAG IS NOT NULL AND TAG != '' " +
                        "AND (DEPLOYINFO_CODE IS NULL OR DEPLOYINFO_CODE NOT LIKE '%+a%') " +
                        "LIMIT 5";
                List<Map<String, Object>> missingRecords = jdbcTemplate.queryForList(
                        detailSql, year, month, weekSeq);

                for (Map<String, Object> record : missingRecords) {
                    log.error("    {} - {} | 标签: {} | 编码: {}",
                            record.get("CIG_CODE"),
                            record.get("CIG_NAME"),
                            record.get("TAG"),
                            record.get("DEPLOYINFO_CODE"));
                }
            } else {
                log.info("  ✅ 所有带标签的记录编码表达式都正确！");
            }
        } else {
            log.info("  ℹ️  无带标签的记录");
        }
    }

    private void step5_verifyAllocationError(int year, int month, int weekSeq) {
        log.info("\n【步骤5】验证分配误差");

        // 计算误差
        String sql = "WITH cigarette_summary AS ( " +
                "    SELECT " +
                "        i.CIG_CODE, " +
                "        i.CIG_NAME, " +
                "        i.DELIVERY_METHOD, " +
                "        i.ADV as expected_total, " +
                "        COALESCE(SUM(p.ACTUAL_DELIVERY), 0) as actual_total, " +
                "        ABS(i.ADV - COALESCE(SUM(p.ACTUAL_DELIVERY), 0)) as abs_error, " +
                "        CASE " +
                "            WHEN i.ADV > 0 THEN " +
                "                ABS(i.ADV - COALESCE(SUM(p.ACTUAL_DELIVERY), 0)) * 100.0 / i.ADV " +
                "            ELSE 0 " +
                "        END as relative_error_pct " +
                "    FROM cigarette_distribution_info i " +
                "    LEFT JOIN cigarette_distribution_prediction p " +
                "        ON i.CIG_CODE = p.CIG_CODE " +
                "        AND i.YEAR = p.YEAR " +
                "        AND i.MONTH = p.MONTH " +
                "        AND i.WEEK_SEQ = p.WEEK_SEQ " +
                "    WHERE i.YEAR = ? AND i.MONTH = ? AND i.WEEK_SEQ = ? " +
                "    GROUP BY i.CIG_CODE, i.CIG_NAME, i.DELIVERY_METHOD, i.ADV " +
                ") " +
                "SELECT " +
                "    COUNT(*) as total_cigarettes, " +
                "    SUM(expected_total) as sum_expected, " +
                "    SUM(actual_total) as sum_actual, " +
                "    MAX(abs_error) as max_abs_error, " +
                "    AVG(abs_error) as avg_abs_error, " +
                "    MAX(relative_error_pct) as max_relative_error_pct, " +
                "    AVG(relative_error_pct) as avg_relative_error_pct " +
                "FROM cigarette_summary";

        Map<String, Object> errorStats = jdbcTemplate.queryForMap(sql, year, month, weekSeq);

        log.info("  总卷烟数: {}", errorStats.get("total_cigarettes"));
        log.info("  预期投放总量: {}", errorStats.get("sum_expected"));
        log.info("  实际投放总量: {}", errorStats.get("sum_actual"));
        log.info("  最大绝对误差: {}", errorStats.get("max_abs_error"));
        log.info("  平均绝对误差: {:.2f}", errorStats.get("avg_abs_error"));
        log.info("  最大相对误差: {:.2f}%", errorStats.get("max_relative_error_pct"));
        log.info("  平均相对误差: {:.2f}%", errorStats.get("avg_relative_error_pct"));

        // 查询误差最大的卷烟
        String topErrorSql = "WITH cigarette_summary AS ( " +
                "    SELECT " +
                "        i.CIG_CODE, " +
                "        i.CIG_NAME, " +
                "        i.DELIVERY_METHOD, " +
                "        i.ADV as expected_total, " +
                "        COALESCE(SUM(p.ACTUAL_DELIVERY), 0) as actual_total, " +
                "        ABS(i.ADV - COALESCE(SUM(p.ACTUAL_DELIVERY), 0)) as abs_error, " +
                "        CASE " +
                "            WHEN i.ADV > 0 THEN " +
                "                ABS(i.ADV - COALESCE(SUM(p.ACTUAL_DELIVERY), 0)) * 100.0 / i.ADV " +
                "            ELSE 0 " +
                "        END as relative_error_pct " +
                "    FROM cigarette_distribution_info i " +
                "    LEFT JOIN cigarette_distribution_prediction p " +
                "        ON i.CIG_CODE = p.CIG_CODE " +
                "        AND i.YEAR = p.YEAR " +
                "        AND i.MONTH = p.MONTH " +
                "        AND i.WEEK_SEQ = p.WEEK_SEQ " +
                "    WHERE i.YEAR = ? AND i.MONTH = ? AND i.WEEK_SEQ = ? " +
                "    GROUP BY i.CIG_CODE, i.CIG_NAME, i.DELIVERY_METHOD, i.ADV " +
                ") " +
                "SELECT " +
                "    CIG_CODE, " +
                "    CIG_NAME, " +
                "    DELIVERY_METHOD, " +
                "    expected_total, " +
                "    actual_total, " +
                "    abs_error, " +
                "    ROUND(relative_error_pct, 2) as relative_error_pct " +
                "FROM cigarette_summary " +
                "ORDER BY abs_error DESC " +
                "LIMIT 3";

        List<Map<String, Object>> topErrors = jdbcTemplate.queryForList(topErrorSql, year, month, weekSeq);

        if (!topErrors.isEmpty()) {
            log.info("\n  误差最大的TOP 3卷烟:");
            int rank = 1;
            for (Map<String, Object> record : topErrors) {
                log.info("    {}. {} - {} ({})",
                        rank++,
                        record.get("CIG_CODE"),
                        record.get("CIG_NAME"),
                        record.get("DELIVERY_METHOD"));
                log.info("       预期: {}, 实际: {}, 误差: {} ({:.2f}%)",
                        record.get("expected_total"),
                        record.get("actual_total"),
                        record.get("abs_error"),
                        record.get("relative_error_pct"));
            }
        }

        // 判断误差是否在可接受范围内
        Object maxRelativeError = errorStats.get("max_relative_error_pct");
        Object avgRelativeError = errorStats.get("avg_relative_error_pct");

        if (maxRelativeError != null && avgRelativeError != null) {
            double maxError = ((Number) maxRelativeError).doubleValue();
            double avgError = ((Number) avgRelativeError).doubleValue();

            if (avgError < 1.0) {
                log.info("\n  ✅ 平均误差 {:.2f}% < 1%，精度优秀！", avgError);
            } else if (avgError < 5.0) {
                log.info("\n  ✅ 平均误差 {:.2f}% < 5%，精度良好！", avgError);
            } else {
                log.warn("\n  ⚠️  平均误差 {:.2f}% >= 5%，需要优化", avgError);
            }

            if (maxError < 15.0) {
                log.info("  ✅ 最大误差 {:.2f}% < 15%，可接受", maxError);
            } else {
                log.warn("  ⚠️  最大误差 {:.2f}% >= 15%，建议检查", maxError);
            }
        }
    }
}