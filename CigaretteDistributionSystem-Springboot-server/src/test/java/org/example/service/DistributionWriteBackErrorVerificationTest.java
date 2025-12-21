package org.example.service;

import lombok.extern.slf4j.Slf4j;
import org.example.application.dto.GenerateDistributionPlanRequestDto;
import org.example.application.dto.GenerateDistributionPlanResponseDto;
import org.example.application.service.calculate.DistributionCalculateService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.List;
import java.util.Map;

/**
 * 分配写回误差验证测试
 * 
 * 测试2025年9月第3周的分配写回，并验证误差（包括普通表和价位段表）
 * 
 * @author Robin
 * @version 1.0
 * @since 2025-12-18
 */
@Slf4j
@SpringBootTest
public class DistributionWriteBackErrorVerificationTest {

    @Autowired
    private DistributionCalculateService distributionCalculateService;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private static final int TEST_YEAR = 2025;
    private static final int TEST_MONTH = 9;
    private static final int TEST_WEEK_SEQ = 3;

    @Test
    public void testDistributionWriteBackAndVerifyErrors() {
        log.info("==========================================");
        log.info("开始测试：2025年9月第3周分配写回并验证误差");
        log.info("==========================================");

        // 步骤1: 生成分配方案
        log.info("");
        log.info("步骤1: 生成分配方案...");
        GenerateDistributionPlanRequestDto request = new GenerateDistributionPlanRequestDto();
        request.setYear(TEST_YEAR);
        request.setMonth(TEST_MONTH);
        request.setWeekSeq(TEST_WEEK_SEQ);

        GenerateDistributionPlanResponseDto response = distributionCalculateService.generateDistributionPlan(request);

        log.info("分配方案生成结果: success={}, message={}, totalCigarettes={}, successfulAllocations={}, processedCount={}",
                response.getSuccess(), response.getMessage(), response.getTotalCigarettes(),
                response.getSuccessfulAllocations(), response.getProcessedCount());

        if (!response.isSuccess()) {
            log.error("分配方案生成失败: {}", response.getMessage());
            return;
        }

        log.info("✅ 分配方案生成成功");
        
        // 等待数据写入完成
        try {
            Thread.sleep(2000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        // 步骤2: 验证普通表的误差
        log.info("");
        log.info("步骤2: 验证普通表（cigarette_distribution_prediction）的误差...");
        verifyPredictionTableErrors(TEST_YEAR, TEST_MONTH, TEST_WEEK_SEQ);

        // 步骤3: 验证价位段表的误差
        log.info("");
        log.info("步骤3: 验证价位段表（cigarette_distribution_prediction_price）的误差...");
        verifyPricePredictionTableErrors(TEST_YEAR, TEST_MONTH, TEST_WEEK_SEQ);

        log.info("");
        log.info("==========================================");
        log.info("测试完成！");
        log.info("==========================================");
    }

    /**
     * 验证普通预测表的误差
     */
    private void verifyPredictionTableErrors(int year, int month, int weekSeq) {
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
            log.warn("⚠️  普通表未找到任何卷烟数据");
            return;
        }

        log.info("========== 普通表误差统计（共{}条） ==========", rows.size());
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
            if (absErr > 0.01) {
                errorCount++;
            }
        }

        log.info("--------------------------------------------------------------------------------------------------------");
        log.info("普通表误差统计汇总:");
        log.info("  总卷烟数: {}", rows.size());
        log.info("  有误差的卷烟数: {}", errorCount);
        log.info("  总绝对误差: {}", String.format("%.2f", totalAbsError));
        double avgError = rows.size() > 0 ? totalAbsError / rows.size() : 0.0;
        log.info("  平均绝对误差: {}", String.format("%.2f", avgError));
        log.info("  最大绝对误差: {}", String.format("%.2f", maxAbsError));
        log.info("  最大相对误差: {}%", String.format("%.2f", maxRelativeError));
    }

    /**
     * 验证价位段预测表的误差
     */
    private void verifyPricePredictionTableErrors(int year, int month, int weekSeq) {
        String sql = "SELECT p.CIG_CODE AS cig_code, p.CIG_NAME AS cig_name, " +
                "SUM(IFNULL(p.ACTUAL_DELIVERY,0)) AS actual_total, " +
                "MAX(IFNULL(i.ADV,0)) AS adv_total, " +
                "ABS(SUM(IFNULL(p.ACTUAL_DELIVERY,0))-MAX(IFNULL(i.ADV,0))) AS abs_error, " +
                "CASE WHEN MAX(IFNULL(i.ADV,0)) > 0 " +
                "THEN ABS(SUM(IFNULL(p.ACTUAL_DELIVERY,0))-MAX(IFNULL(i.ADV,0))) / MAX(IFNULL(i.ADV,0)) * 100 " +
                "ELSE 0 END AS relative_error_percent " +
                "FROM cigarette_distribution_prediction_price p " +
                "JOIN cigarette_distribution_info i ON p.YEAR=i.YEAR AND p.MONTH=i.MONTH AND p.WEEK_SEQ=i.WEEK_SEQ " +
                "AND p.CIG_CODE=i.CIG_CODE AND p.CIG_NAME=i.CIG_NAME " +
                "WHERE p.YEAR=? AND p.MONTH=? AND p.WEEK_SEQ=? " +
                "GROUP BY p.CIG_CODE, p.CIG_NAME " +
                "ORDER BY abs_error DESC";

        List<Map<String, Object>> rows = jdbcTemplate.queryForList(sql, year, month, weekSeq);

        if (rows.isEmpty()) {
            log.warn("⚠️  价位段表未找到任何卷烟数据");
            return;
        }

        log.info("========== 价位段表误差统计（共{}条） ==========", rows.size());
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
            if (absErr > 0.01) {
                errorCount++;
            }
        }

        log.info("--------------------------------------------------------------------------------------------------------");
        log.info("价位段表误差统计汇总:");
        log.info("  总卷烟数: {}", rows.size());
        log.info("  有误差的卷烟数: {}", errorCount);
        log.info("  总绝对误差: {}", String.format("%.2f", totalAbsError));
        double avgError = rows.size() > 0 ? totalAbsError / rows.size() : 0.0;
        log.info("  平均绝对误差: {}", String.format("%.2f", avgError));
        log.info("  最大绝对误差: {}", String.format("%.2f", maxAbsError));
        log.info("  最大相对误差: {}%", String.format("%.2f", maxRelativeError));
    }
}

