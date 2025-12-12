package org.example.service;

import lombok.extern.slf4j.Slf4j;
import org.example.dto.GenerateDistributionPlanRequestDto;
import org.example.dto.GenerateDistributionPlanResponseDto;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 一键生成分配方案的集成测试。
 *
 * 固定使用 2025 年 9 月第 3 周作为测试输入，便于比对分配日志和误差分布。
 */
@Slf4j
@SpringBootTest
public class GenerateDistributionPlanIntegrationTest {

    @Autowired
    private DistributionCalculateService distributionCalculateService;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @AfterEach
    public void separator() {
        log.info("------------------------------------------------------------");
    }

    @Test
    public void shouldGenerateDistributionPlan() {
        GenerateDistributionPlanRequestDto req = new GenerateDistributionPlanRequestDto();
        req.setYear(2025);
        req.setMonth(9);
        req.setWeekSeq(3);

        GenerateDistributionPlanResponseDto resp = distributionCalculateService.generateDistributionPlan(req);

        log.info("一键生成分配方案响应: success={}, message={}, totalCigarettes={}, successfulAllocations={}, processedCount={}",
                resp.getSuccess(), resp.getMessage(), resp.getTotalCigarettes(),
                resp.getSuccessfulAllocations(), resp.getProcessedCount());

        Assertions.assertTrue(resp.isSuccess(), "分配方案生成应当成功");
    }

    @Test
    public void shouldReportAbsoluteErrors() {
        int year = 2025;
        int month = 9;
        int week = 3;

        String sql = "SELECT p.CIG_CODE AS cig_code, p.CIG_NAME AS cig_name, " +
                "SUM(IFNULL(p.ACTUAL_DELIVERY,0)) AS actual_total, " +
                "SUM(IFNULL(i.ADV,0)) AS adv_total, " +
                "ABS(SUM(IFNULL(p.ACTUAL_DELIVERY,0))-SUM(IFNULL(i.ADV,0))) AS abs_error " +
                "FROM cigarette_distribution_prediction p " +
                "JOIN cigarette_distribution_info i ON p.YEAR=i.YEAR AND p.MONTH=i.MONTH AND p.WEEK_SEQ=i.WEEK_SEQ " +
                "AND p.CIG_CODE=i.CIG_CODE AND p.CIG_NAME=i.CIG_NAME " +
                "WHERE p.YEAR=? AND p.MONTH=? AND p.WEEK_SEQ=? " +
                "GROUP BY p.CIG_CODE,p.CIG_NAME";

        List<Map<String, Object>> rows = jdbcTemplate.queryForList(sql, year, month, week);
        List<Map<String, Object>> sorted = rows.stream()
                .sorted(Comparator.comparing(r -> ((Number) r.get("abs_error")).doubleValue(), Comparator.reverseOrder()))
                .collect(Collectors.toList());

        log.info("卷烟绝对误差（按降序，前20条）");
        sorted.stream().limit(20).forEach(r -> log.info("cig_code={}, cig_name={}, adv_total={}, actual_total={}, abs_error={}",
                r.get("cig_code"), r.get("cig_name"), r.get("adv_total"), r.get("actual_total"), r.get("abs_error")));

        double maxError = sorted.stream()
                .mapToDouble(r -> ((Number) r.get("abs_error")).doubleValue())
                .max().orElse(0d);

        // 仅记录，不强制失败；如需阈值校验可打开断言
        log.warn("本次分配最大绝对误差: {}", maxError);
    }
}

