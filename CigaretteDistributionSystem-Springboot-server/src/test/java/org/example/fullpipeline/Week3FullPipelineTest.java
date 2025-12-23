package org.example.fullpipeline;

import lombok.extern.slf4j.Slf4j;
import org.example.application.dto.allocation.GenerateDistributionPlanRequestDto;
import org.example.application.dto.allocation.GenerateDistributionPlanResponseDto;
import org.example.application.dto.allocation.TotalActualDeliveryResponseDto;
import org.example.application.service.calculate.StandardAllocationService;
import org.example.application.service.calculate.UnifiedAllocationService;
import org.example.application.service.coordinator.RegionCustomerStatisticsBuildService;
import org.example.domain.repository.FilterCustomerTableRepository;
import org.example.shared.util.PartitionTableManager;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;

/**
 * 2025年9月第3周全链路测试
 */
@Slf4j
@SpringBootTest
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class Week3FullPipelineTest {

    @Autowired
    private FilterCustomerTableRepository filterCustomerTableRepository;

    @Autowired
    private RegionCustomerStatisticsBuildService regionCustomerStatisticsBuildService;

    @Autowired
    private StandardAllocationService distributionCalculateService;

    @Autowired
    private UnifiedAllocationService unifiedAllocationService;

    @Autowired
    private PartitionTableManager partitionTableManager;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    // 测试参数
    private static final int YEAR = 2025;
    private static final int MONTH = 9;
    private static final int WEEK_SEQ = 3;
    private static final Set<String> CUSTOMER_TYPES = new HashSet<>(Arrays.asList("单周客户", "正常客户"));
    private static final Set<String> WORKDAYS = new HashSet<>(Arrays.asList("周一", "周二", "周三", "周四", "周五"));

    @Test
    @Order(1)
    public void step1_prepareData() {
        log.info("==================== 步骤1: 准备数据 (2025/9/3) ====================");
        
        partitionTableManager.ensurePartitionExists("customer_filter", YEAR, MONTH, WEEK_SEQ);
        partitionTableManager.ensurePartitionExists("region_customer_statistics", YEAR, MONTH, WEEK_SEQ);
        partitionTableManager.ensurePartitionExists("cigarette_distribution_prediction", YEAR, MONTH, WEEK_SEQ);
        partitionTableManager.ensurePartitionExists("cigarette_distribution_prediction_price", YEAR, MONTH, WEEK_SEQ);
        log.info("✅ 分区创建完成");

        String deleteCustomerFilter = String.format(
                "DELETE FROM customer_filter WHERE YEAR = %d AND MONTH = %d AND WEEK_SEQ = %d", YEAR, MONTH, WEEK_SEQ);
        String deleteRegionStats = String.format(
                "DELETE FROM region_customer_statistics WHERE YEAR = %d AND MONTH = %d AND WEEK_SEQ = %d", YEAR, MONTH, WEEK_SEQ);
        String deletePrediction = String.format(
                "DELETE FROM cigarette_distribution_prediction WHERE YEAR = %d AND MONTH = %d AND WEEK_SEQ = %d", YEAR, MONTH, WEEK_SEQ);
        String deletePredictionPrice = String.format(
                "DELETE FROM cigarette_distribution_prediction_price WHERE YEAR = %d AND MONTH = %d AND WEEK_SEQ = %d", YEAR, MONTH, WEEK_SEQ);

        jdbcTemplate.update(deleteCustomerFilter);
        jdbcTemplate.update(deleteRegionStats);
        jdbcTemplate.update(deletePrediction);
        jdbcTemplate.update(deletePredictionPrice);
        log.info("✅ 旧数据清空完成");

        String whereClause = buildWhereClause(CUSTOMER_TYPES, WORKDAYS);
        log.info("筛选条件: {}", whereClause);
        
        filterCustomerTableRepository.ensurePartitionAndInsertData(YEAR, MONTH, WEEK_SEQ, whereClause);
        
        String countSql = String.format(
                "SELECT COUNT(*) FROM customer_filter WHERE YEAR = %d AND MONTH = %d AND WEEK_SEQ = %d", YEAR, MONTH, WEEK_SEQ);
        Integer customerCount = jdbcTemplate.queryForObject(countSql, Integer.class);
        log.info("✅ 客户筛选完成，共 {} 名客户", customerCount);
    }

    @Test
    @Order(2)
    public void step2_buildRegionStatistics() {
        log.info("==================== 步骤2: 构建区域客户统计 ====================");
        
        Map<String, Object> result = regionCustomerStatisticsBuildService.buildRegionCustomerStatistics(YEAR, MONTH, WEEK_SEQ);
        
        Boolean success = (Boolean) result.get("success");
        Integer insertedCount = (Integer) result.get("insertedCount");
        
        if (!Boolean.TRUE.equals(success)) {
            throw new RuntimeException("区域统计失败：" + result.get("message"));
        }
        
        log.info("✅ 区域统计完成，共 {} 条统计记录", insertedCount);
        
        String checkSql = String.format(
                "SELECT REGION, TOTAL FROM region_customer_statistics WHERE YEAR = %d AND MONTH = %d AND WEEK_SEQ = %d", YEAR, MONTH, WEEK_SEQ);
        
        List<Map<String, Object>> stats = jdbcTemplate.queryForList(checkSql);
        log.info("区域客户统计详情：");
        for (Map<String, Object> stat : stats) {
            log.info("  区域: {}, 总客户数: {}", stat.get("REGION"), stat.get("TOTAL"));
        }
    }

    @Test
    @Order(3)
    public void step3_executeAllocation() {
        log.info("==================== 步骤3: 执行分配 ====================");
        
        String checkInfoSql = String.format(
                "SELECT COUNT(*) FROM cigarette_distribution_info WHERE YEAR = %d AND MONTH = %d AND WEEK_SEQ = %d", YEAR, MONTH, WEEK_SEQ);
        Integer infoCount = jdbcTemplate.queryForObject(checkInfoSql, Integer.class);
        log.info("Info表中待分配卷烟数: {}", infoCount);
        
        if (infoCount == null || infoCount == 0) {
            log.warn("Info表中无待分配数据，跳过分配");
            return;
        }
        
        GenerateDistributionPlanRequestDto planRequest = new GenerateDistributionPlanRequestDto();
        planRequest.setYear(YEAR);
        planRequest.setMonth(MONTH);
        planRequest.setWeekSeq(WEEK_SEQ);
        
        GenerateDistributionPlanResponseDto planResponse = unifiedAllocationService.generateDistributionPlan(planRequest);
        
        log.info("✅ 分配方案生成完成");
        log.info("   成功: {}", planResponse.isSuccess());
        log.info("   成功分配: {} 种卷烟", planResponse.getSuccessfulAllocations());
        log.info("   总卷烟数: {} 种", planResponse.getTotalCigarettes());
        
        TotalActualDeliveryResponseDto totalResponse = distributionCalculateService.calculateTotalActualDelivery(YEAR, MONTH, WEEK_SEQ);
        if (totalResponse != null && Boolean.TRUE.equals(totalResponse.getSuccess())) {
            log.info("✅ 总实际投放量计算完成: {} 种卷烟", totalResponse.getCigaretteCount());
        }
    }

    @Test
    @Order(4)
    public void step4_verifyResults() {
        log.info("==================== 步骤4: 验证结果 ====================");
        
        String countPredictionSql = String.format(
                "SELECT COUNT(*) FROM cigarette_distribution_prediction WHERE YEAR = %d AND MONTH = %d AND WEEK_SEQ = %d", YEAR, MONTH, WEEK_SEQ);
        Integer predictionCount = jdbcTemplate.queryForObject(countPredictionSql, Integer.class);
        log.info("标准分配结果记录数: {}", predictionCount);
        
        String countPredictionPriceSql = String.format(
                "SELECT COUNT(*) FROM cigarette_distribution_prediction_price WHERE YEAR = %d AND MONTH = %d AND WEEK_SEQ = %d", YEAR, MONTH, WEEK_SEQ);
        Integer predictionPriceCount = jdbcTemplate.queryForObject(countPredictionPriceSql, Integer.class);
        log.info("价位段分配结果记录数: {}", predictionPriceCount);
        
        if (predictionCount != null && predictionCount > 0) {
            String sampleSql = String.format(
                    "SELECT CIG_CODE, CIG_NAME, DELIVERY_METHOD, DELIVERY_AREA, ACTUAL_DELIVERY " +
                    "FROM cigarette_distribution_prediction WHERE YEAR = %d AND MONTH = %d AND WEEK_SEQ = %d LIMIT 5", YEAR, MONTH, WEEK_SEQ);
            
            List<Map<String, Object>> samples = jdbcTemplate.queryForList(sampleSql);
            log.info("\n标准分配样例数据：");
            for (Map<String, Object> sample : samples) {
                log.info("  {}: {}, 方法: {}, 区域: {}, 实际投放: {}",
                        sample.get("CIG_CODE"), sample.get("CIG_NAME"), sample.get("DELIVERY_METHOD"),
                        sample.get("DELIVERY_AREA"), sample.get("ACTUAL_DELIVERY"));
            }
        }
        
        log.info("✅ 结果验证完成");
    }

    @Test
    @Order(5)
    public void step5_analyzeErrors() {
        log.info("==================== 步骤5: 分析误差 ====================");
        
        String errorSql = String.format(
                "SELECT i.CIG_CODE, i.CIG_NAME, i.DELIVERY_METHOD, i.ADV as expected_delivery, " +
                "COALESCE(SUM(p.ACTUAL_DELIVERY), 0) as actual_delivery, " +
                "(COALESCE(SUM(p.ACTUAL_DELIVERY), 0) - i.ADV) as absolute_error " +
                "FROM cigarette_distribution_info i " +
                "LEFT JOIN cigarette_distribution_prediction p ON i.YEAR = p.YEAR AND i.MONTH = p.MONTH AND i.WEEK_SEQ = p.WEEK_SEQ AND i.CIG_CODE = p.CIG_CODE " +
                "WHERE i.YEAR = %d AND i.MONTH = %d AND i.WEEK_SEQ = %d AND i.DELIVERY_METHOD != '按价位段自选投放' " +
                "GROUP BY i.CIG_CODE, i.CIG_NAME, i.DELIVERY_METHOD, i.ADV ORDER BY ABS(absolute_error) DESC", YEAR, MONTH, WEEK_SEQ);
        
        List<Map<String, Object>> errors = jdbcTemplate.queryForList(errorSql);
        
        if (!errors.isEmpty()) {
            log.info("【标准分配误差分析】总卷烟数: {}", errors.size());
            
            BigDecimal maxAbsError = BigDecimal.ZERO;
            BigDecimal sumError = BigDecimal.ZERO;
            BigDecimal sumExpected = BigDecimal.ZERO;
            
            for (Map<String, Object> error : errors) {
                BigDecimal absError = ((BigDecimal) error.get("absolute_error")).abs();
                BigDecimal expected = (BigDecimal) error.get("expected_delivery");
                if (absError.compareTo(maxAbsError) > 0) maxAbsError = absError;
                sumError = sumError.add(absError);
                sumExpected = sumExpected.add(expected);
            }
            
            BigDecimal avgErrorPercent = sumExpected.compareTo(BigDecimal.ZERO) > 0 
                    ? sumError.divide(sumExpected, 6, RoundingMode.HALF_UP).multiply(new BigDecimal("100"))
                    : BigDecimal.ZERO;
            
            log.info("最大绝对误差: {} 条", maxAbsError);
            log.info("平均误差率: {}%", avgErrorPercent.setScale(2, RoundingMode.HALF_UP));
            
            log.info("\n误差TOP 5:");
            for (int i = 0; i < Math.min(5, errors.size()); i++) {
                Map<String, Object> error = errors.get(i);
                log.info("  {}. {}: 预期 {} → 实际 {}, 误差 {}",
                        i + 1, error.get("CIG_NAME"), error.get("expected_delivery"),
                        error.get("actual_delivery"), error.get("absolute_error"));
            }
        } else {
            log.info("无标准分配数据");
        }
        
        log.info("✅ 误差分析完成");
    }

    private String buildWhereClause(Set<String> customerTypes, Set<String> workdays) {
        List<String> orderCycleConditions = new ArrayList<>();
        for (String customerType : customerTypes) {
            for (String workday : workdays) {
                switch (customerType) {
                    case "单周客户": orderCycleConditions.add("ORDER_CYCLE = '单周" + workday + "'"); break;
                    case "双周客户": orderCycleConditions.add("ORDER_CYCLE = '双周" + workday + "'"); break;
                    case "正常客户": orderCycleConditions.add("ORDER_CYCLE = '" + workday + "'"); break;
                }
            }
        }
        String orderCycleClause = orderCycleConditions.stream()
                .reduce((a, b) -> a + " OR " + b)
                .map(clause -> "(" + clause + ")")
                .orElse("(1=1)");
        return "WHERE " + orderCycleClause + " AND ORDER_CYCLE != '不访销' AND ORDER_CYCLE IS NOT NULL";
    }
}
