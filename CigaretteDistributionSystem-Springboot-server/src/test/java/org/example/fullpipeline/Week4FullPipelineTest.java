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
 * 2025年9月第4周全链路测试
 * 
 * 测试参数：
 * - 时间：2025年9月第4周
 * - 客户类型：{双周客户, 正常客户}
 * - 工作日：{周一, 周二, 周三, 周四, 周五}
 */
@Slf4j
@SpringBootTest
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class Week4FullPipelineTest {

    @Autowired
    private FilterCustomerTableRepository filterCustomerTableRepository;

    @Autowired
    private RegionCustomerStatisticsBuildService regionCustomerStatisticsBuildService;

    @Autowired
    private StandardAllocationService standardAllocationService;

    @Autowired
    private UnifiedAllocationService unifiedAllocationService;

    @Autowired
    private PartitionTableManager partitionTableManager;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    // 测试参数
    private static final int YEAR = 2025;
    private static final int MONTH = 9;
    private static final int WEEK_SEQ = 4;
    private static final Set<String> CUSTOMER_TYPES = new HashSet<>(Arrays.asList("双周客户", "正常客户"));
    private static final Set<String> WORKDAYS = new HashSet<>(Arrays.asList("周一", "周二", "周三", "周四", "周五"));

    @Test
    @Order(1)
    public void step1_prepareData() {
        log.info("==================== 步骤1: 准备数据 ====================");
        
        // 1.1 确保分区存在
        log.info("创建必要的分区...");
        partitionTableManager.ensurePartitionExists("customer_filter", YEAR, MONTH, WEEK_SEQ);
        partitionTableManager.ensurePartitionExists("region_customer_statistics", YEAR, MONTH, WEEK_SEQ);
        partitionTableManager.ensurePartitionExists("cigarette_distribution_prediction", YEAR, MONTH, WEEK_SEQ);
        partitionTableManager.ensurePartitionExists("cigarette_distribution_prediction_price", YEAR, MONTH, WEEK_SEQ);
        log.info("✅ 分区创建完成");

        // 1.2 清空旧数据
        log.info("清空旧数据...");
        String deleteCustomerFilter = String.format(
                "DELETE FROM customer_filter WHERE YEAR = %d AND MONTH = %d AND WEEK_SEQ = %d",
                YEAR, MONTH, WEEK_SEQ);
        String deleteRegionStats = String.format(
                "DELETE FROM region_customer_statistics WHERE YEAR = %d AND MONTH = %d AND WEEK_SEQ = %d",
                YEAR, MONTH, WEEK_SEQ);
        String deletePrediction = String.format(
                "DELETE FROM cigarette_distribution_prediction WHERE YEAR = %d AND MONTH = %d AND WEEK_SEQ = %d",
                YEAR, MONTH, WEEK_SEQ);
        String deletePredictionPrice = String.format(
                "DELETE FROM cigarette_distribution_prediction_price WHERE YEAR = %d AND MONTH = %d AND WEEK_SEQ = %d",
                YEAR, MONTH, WEEK_SEQ);

        jdbcTemplate.update(deleteCustomerFilter);
        jdbcTemplate.update(deleteRegionStats);
        jdbcTemplate.update(deletePrediction);
        jdbcTemplate.update(deletePredictionPrice);
        log.info("✅ 旧数据清空完成");

        // 1.3 筛选客户数据到customer_filter表
        log.info("筛选客户数据...");
        log.info("客户类型: {}", CUSTOMER_TYPES);
        log.info("工作日: {}", WORKDAYS);
        
        String whereClause = buildWhereClause(CUSTOMER_TYPES, WORKDAYS);
        log.info("筛选条件: {}", whereClause);
        
        filterCustomerTableRepository.ensurePartitionAndInsertData(YEAR, MONTH, WEEK_SEQ, whereClause);
        
        // 验证筛选结果
        String countSql = String.format(
                "SELECT COUNT(*) FROM customer_filter WHERE YEAR = %d AND MONTH = %d AND WEEK_SEQ = %d",
                YEAR, MONTH, WEEK_SEQ);
        Integer customerCount = jdbcTemplate.queryForObject(countSql, Integer.class);
        log.info("✅ 客户筛选完成，共 {} 名客户", customerCount);
        
        if (customerCount == null || customerCount == 0) {
            throw new RuntimeException("客户筛选失败：未找到符合条件的客户");
        }
    }

    @Test
    @Order(2)
    public void step2_buildRegionStatistics() {
        log.info("==================== 步骤2: 构建区域客户统计 ====================");
        
        Map<String, Object> result = regionCustomerStatisticsBuildService.buildRegionCustomerStatistics(YEAR, MONTH, WEEK_SEQ);
        
        // 验证结果
        Boolean success = (Boolean) result.get("success");
        String message = (String) result.get("message");
        Integer insertedCount = (Integer) result.get("insertedCount");
        
        if (!Boolean.TRUE.equals(success)) {
            throw new RuntimeException("区域统计失败：" + message);
        }
        
        log.info("✅ 区域统计完成，共 {} 条统计记录", insertedCount);
        
        // 检查客户数是否为0
        String checkZeroSql = String.format(
                "SELECT REGION, TOTAL FROM region_customer_statistics " +
                "WHERE YEAR = %d AND MONTH = %d AND WEEK_SEQ = %d",
                YEAR, MONTH, WEEK_SEQ);
        
        List<Map<String, Object>> stats = jdbcTemplate.queryForList(checkZeroSql);
        log.info("区域客户统计详情：");
        for (Map<String, Object> stat : stats) {
            String region = (String) stat.get("REGION");
            BigDecimal total = (BigDecimal) stat.get("TOTAL");
            log.info("  区域: {}, 总客户数: {}", region, total);
        }
    }

    @Test
    @Order(3)
    public void step3_executeAllocation() {
        log.info("==================== 步骤3: 执行分配 ====================");
        
        // 检查info表数据
        String checkInfoSql = String.format(
                "SELECT COUNT(*) FROM cigarette_distribution_info " +
                "WHERE YEAR = %d AND MONTH = %d AND WEEK_SEQ = %d",
                YEAR, MONTH, WEEK_SEQ);
        Integer infoCount = jdbcTemplate.queryForObject(checkInfoSql, Integer.class);
        log.info("Info表中待分配卷烟数: {}", infoCount);
        
        if (infoCount == null || infoCount == 0) {
            throw new RuntimeException("Info表中无待分配数据");
        }
        
        // 3.1 执行分配方案生成（这会触发所有分配算法的执行）
        log.info("执行分配方案生成...");
        GenerateDistributionPlanRequestDto planRequest = new GenerateDistributionPlanRequestDto();
        planRequest.setYear(YEAR);
        planRequest.setMonth(MONTH);
        planRequest.setWeekSeq(WEEK_SEQ);
        
        GenerateDistributionPlanResponseDto planResponse = unifiedAllocationService.generateDistributionPlan(planRequest);
        
        if (planResponse == null || !Boolean.TRUE.equals(planResponse.getSuccess())) {
            String errorMsg = planResponse != null ? planResponse.getMessage() : "返回结果为空";
            throw new RuntimeException("分配方案生成失败: " + errorMsg);
        }
        
        log.info("✅ 分配方案生成完成");
        if (planResponse.getSuccessfulAllocations() != null) {
            log.info("   成功分配: {} 种卷烟", planResponse.getSuccessfulAllocations());
        }
        if (planResponse.getTotalCigarettes() != null) {
            log.info("   总卷烟数: {} 种", planResponse.getTotalCigarettes());
        }
        
        // 3.2 计算总实际投放量（验证分配结果）
        log.info("计算总实际投放量...");
        TotalActualDeliveryResponseDto totalResponse = standardAllocationService.calculateTotalActualDelivery(YEAR, MONTH, WEEK_SEQ);
        
        if (totalResponse == null || !Boolean.TRUE.equals(totalResponse.getSuccess())) {
            String errorMsg = totalResponse != null ? totalResponse.getMessage() : "返回结果为空";
            log.warn("总实际投放量计算失败: {}", errorMsg);
            // 注意：价位段分配的数据在 prediction_price 表，不在 prediction 表
            // 所以如果全部是价位段卷烟，这里可能会报"未找到数据"
        } else {
            log.info("✅ 总实际投放量计算完成: {} 种卷烟", totalResponse.getCigaretteCount());
        }
        
        log.info("✅ 分配执行完成");
    }

    @Test
    @Order(4)
    public void step4_verifyResults() {
        log.info("==================== 步骤4: 验证结果 ====================");
        
        // 4.1 统计标准分配结果
        String countPredictionSql = String.format(
                "SELECT COUNT(*) FROM cigarette_distribution_prediction " +
                "WHERE YEAR = %d AND MONTH = %d AND WEEK_SEQ = %d",
                YEAR, MONTH, WEEK_SEQ);
        Integer predictionCount = jdbcTemplate.queryForObject(countPredictionSql, Integer.class);
        log.info("标准分配结果记录数: {}", predictionCount);
        
        // 4.2 统计价位段分配结果
        String countPredictionPriceSql = String.format(
                "SELECT COUNT(*) FROM cigarette_distribution_prediction_price " +
                "WHERE YEAR = %d AND MONTH = %d AND WEEK_SEQ = %d",
                YEAR, MONTH, WEEK_SEQ);
        Integer predictionPriceCount = jdbcTemplate.queryForObject(countPredictionPriceSql, Integer.class);
        log.info("价位段分配结果记录数: {}", predictionPriceCount);
        
        // 4.3 查看标准分配样例
        if (predictionCount != null && predictionCount > 0) {
            String sampleSql = String.format(
                    "SELECT CIG_CODE, CIG_NAME, DELIVERY_METHOD, DELIVERY_ETYPE, DELIVERY_AREA, " +
                    "ACTUAL_DELIVERY, TAG " +
                    "FROM cigarette_distribution_prediction " +
                    "WHERE YEAR = %d AND MONTH = %d AND WEEK_SEQ = %d " +
                    "LIMIT 5",
                    YEAR, MONTH, WEEK_SEQ);
            
            List<Map<String, Object>> samples = jdbcTemplate.queryForList(sampleSql);
            log.info("\n标准分配样例数据：");
            for (Map<String, Object> sample : samples) {
                log.info("  {}: {}, 方法: {}, 类型: {}, 区域: {}, 实际投放: {}, 标签: {}",
                        sample.get("CIG_CODE"),
                        sample.get("CIG_NAME"),
                        sample.get("DELIVERY_METHOD"),
                        sample.get("DELIVERY_ETYPE"),
                        sample.get("DELIVERY_AREA"),
                        sample.get("ACTUAL_DELIVERY"),
                        sample.get("TAG"));
            }
        }
        
        // 4.4 查看价位段分配样例
        if (predictionPriceCount != null && predictionPriceCount > 0) {
            String samplePriceSql = String.format(
                    "SELECT CIG_CODE, CIG_NAME, DELIVERY_METHOD, ACTUAL_DELIVERY " +
                    "FROM cigarette_distribution_prediction_price " +
                    "WHERE YEAR = %d AND MONTH = %d AND WEEK_SEQ = %d " +
                    "LIMIT 5",
                    YEAR, MONTH, WEEK_SEQ);
            
            List<Map<String, Object>> samplesPrices = jdbcTemplate.queryForList(samplePriceSql);
            log.info("\n价位段分配样例数据：");
            for (Map<String, Object> sample : samplesPrices) {
                log.info("  {}: {}, 方法: {}, 实际投放: {}",
                        sample.get("CIG_CODE"),
                        sample.get("CIG_NAME"),
                        sample.get("DELIVERY_METHOD"),
                        sample.get("ACTUAL_DELIVERY"));
            }
        }
        
        log.info("✅ 结果验证完成");
    }

    @Test
    @Order(5)
    public void step5_analyzeErrors() {
        log.info("==================== 步骤5: 分析误差 ====================");
        
        // 5.1 标准分配误差分析
        String standardErrorSql = String.format(
                "SELECT " +
                "  i.CIG_CODE, " +
                "  i.CIG_NAME, " +
                "  i.DELIVERY_METHOD, " +
                "  i.ADV as expected_delivery, " +
                "  COALESCE(SUM(p.ACTUAL_DELIVERY), 0) as actual_delivery, " +
                "  (COALESCE(SUM(p.ACTUAL_DELIVERY), 0) - i.ADV) as absolute_error, " +
                "  CASE WHEN i.ADV > 0 THEN ABS((COALESCE(SUM(p.ACTUAL_DELIVERY), 0) - i.ADV) / i.ADV * 100) ELSE 0 END as error_percent " +
                "FROM cigarette_distribution_info i " +
                "LEFT JOIN cigarette_distribution_prediction p " +
                "  ON i.YEAR = p.YEAR AND i.MONTH = p.MONTH AND i.WEEK_SEQ = p.WEEK_SEQ " +
                "  AND i.CIG_CODE = p.CIG_CODE " +
                "WHERE i.YEAR = %d AND i.MONTH = %d AND i.WEEK_SEQ = %d " +
                "  AND i.DELIVERY_METHOD != '按价位段自选投放' " +
                "GROUP BY i.CIG_CODE, i.CIG_NAME, i.DELIVERY_METHOD, i.ADV " +
                "ORDER BY ABS(absolute_error) DESC",
                YEAR, MONTH, WEEK_SEQ);
        
        List<Map<String, Object>> standardErrors = jdbcTemplate.queryForList(standardErrorSql);
        
        if (!standardErrors.isEmpty()) {
            log.info("\n【标准分配误差分析】");
            log.info("总卷烟数: {}", standardErrors.size());
            
            BigDecimal maxAbsError = BigDecimal.ZERO;
            BigDecimal sumError = BigDecimal.ZERO;
            BigDecimal sumExpected = BigDecimal.ZERO;
            
            for (Map<String, Object> error : standardErrors) {
                BigDecimal absError = ((BigDecimal) error.get("absolute_error")).abs();
                BigDecimal expected = (BigDecimal) error.get("expected_delivery");
                
                if (absError.compareTo(maxAbsError) > 0) {
                    maxAbsError = absError;
                }
                sumError = sumError.add(absError);
                sumExpected = sumExpected.add(expected);
            }
            
            BigDecimal avgErrorPercent = sumExpected.compareTo(BigDecimal.ZERO) > 0 
                    ? sumError.divide(sumExpected, 6, RoundingMode.HALF_UP).multiply(new BigDecimal("100"))
                    : BigDecimal.ZERO;
            
            log.info("最大绝对误差: {} 条", maxAbsError);
            log.info("平均误差率: {}%", avgErrorPercent.setScale(2, RoundingMode.HALF_UP));
            
            log.info("\n误差TOP 5:");
            for (int i = 0; i < Math.min(5, standardErrors.size()); i++) {
                Map<String, Object> error = standardErrors.get(i);
                log.info("  {}. {}: 预期 {} → 实际 {}, 误差 {} ({}%)",
                        i + 1,
                        error.get("CIG_NAME"),
                        error.get("expected_delivery"),
                        error.get("actual_delivery"),
                        error.get("absolute_error"),
                        ((BigDecimal) error.get("error_percent")).setScale(2, RoundingMode.HALF_UP));
            }
        } else {
            log.info("无标准分配数据");
        }
        
        // 5.2 价位段分配误差分析
        String priceErrorSql = String.format(
                "SELECT " +
                "  i.CIG_CODE, " +
                "  i.CIG_NAME, " +
                "  i.DELIVERY_METHOD, " +
                "  i.ADV as expected_delivery, " +
                "  COALESCE(p.ACTUAL_DELIVERY, 0) as actual_delivery, " +
                "  (COALESCE(p.ACTUAL_DELIVERY, 0) - i.ADV) as absolute_error, " +
                "  CASE WHEN i.ADV > 0 THEN ABS((COALESCE(p.ACTUAL_DELIVERY, 0) - i.ADV) / i.ADV * 100) ELSE 0 END as error_percent " +
                "FROM cigarette_distribution_info i " +
                "LEFT JOIN cigarette_distribution_prediction_price p " +
                "  ON i.YEAR = p.YEAR AND i.MONTH = p.MONTH AND i.WEEK_SEQ = p.WEEK_SEQ " +
                "  AND i.CIG_CODE = p.CIG_CODE " +
                "WHERE i.YEAR = %d AND i.MONTH = %d AND i.WEEK_SEQ = %d " +
                "  AND i.DELIVERY_METHOD = '按价位段自选投放' " +
                "ORDER BY ABS(absolute_error) DESC",
                YEAR, MONTH, WEEK_SEQ);
        
        List<Map<String, Object>> priceErrors = jdbcTemplate.queryForList(priceErrorSql);
        
        if (!priceErrors.isEmpty()) {
            log.info("\n【价位段分配误差分析】");
            log.info("总卷烟数: {}", priceErrors.size());
            
            BigDecimal maxAbsError = BigDecimal.ZERO;
            BigDecimal sumError = BigDecimal.ZERO;
            BigDecimal sumExpected = BigDecimal.ZERO;
            
            for (Map<String, Object> error : priceErrors) {
                BigDecimal absError = ((BigDecimal) error.get("absolute_error")).abs();
                BigDecimal expected = (BigDecimal) error.get("expected_delivery");
                
                if (absError.compareTo(maxAbsError) > 0) {
                    maxAbsError = absError;
                }
                sumError = sumError.add(absError);
                sumExpected = sumExpected.add(expected);
            }
            
            BigDecimal avgErrorPercent = sumExpected.compareTo(BigDecimal.ZERO) > 0 
                    ? sumError.divide(sumExpected, 6, RoundingMode.HALF_UP).multiply(new BigDecimal("100"))
                    : BigDecimal.ZERO;
            
            log.info("最大绝对误差: {} 条", maxAbsError);
            log.info("平均误差率: {}%", avgErrorPercent.setScale(2, RoundingMode.HALF_UP));
            
            log.info("\n误差TOP 5:");
            for (int i = 0; i < Math.min(5, priceErrors.size()); i++) {
                Map<String, Object> error = priceErrors.get(i);
                log.info("  {}. {}: 预期 {} → 实际 {}, 误差 {} ({}%)",
                        i + 1,
                        error.get("CIG_NAME"),
                        error.get("expected_delivery"),
                        error.get("actual_delivery"),
                        error.get("absolute_error"),
                        ((BigDecimal) error.get("error_percent")).setScale(2, RoundingMode.HALF_UP));
            }
        } else {
            log.info("无价位段分配数据");
        }
        
        log.info("✅ 误差分析完成");
    }

    @Test
    @Order(6)
    public void step6_verifyEncodingExpression() {
        log.info("==================== 步骤6: 验证编码表达式 ====================");
        
        String checkTagSql = String.format(
                "SELECT " +
                "  CIG_CODE, " +
                "  CIG_NAME, " +
                "  DELIVERY_AREA, " +
                "  TAG, " +
                "  DEPLOYINFO_CODE " +
                "FROM cigarette_distribution_prediction " +
                "WHERE YEAR = %d AND MONTH = %d AND WEEK_SEQ = %d " +
                "  AND TAG IS NOT NULL AND TAG != ''",
                YEAR, MONTH, WEEK_SEQ);
        
        List<Map<String, Object>> taggedRecords = jdbcTemplate.queryForList(checkTagSql);
        
        log.info("带标签的记录数: {}", taggedRecords.size());
        
        if (!taggedRecords.isEmpty()) {
            int correctCount = 0;
            int missingCount = 0;
            
            for (Map<String, Object> record : taggedRecords) {
                String deployinfoCode = (String) record.get("DEPLOYINFO_CODE");
                String tag = (String) record.get("TAG");
                
                if (deployinfoCode != null && deployinfoCode.contains("+a")) {
                    correctCount++;
                } else {
                    missingCount++;
                    log.warn("❌ 编码缺少标签后缀: {} - {}, 标签: {}, 编码: {}",
                            record.get("CIG_CODE"),
                            record.get("CIG_NAME"),
                            tag,
                            deployinfoCode);
                }
            }
            
            double correctRate = taggedRecords.size() > 0 
                    ? (correctCount * 100.0 / taggedRecords.size()) 
                    : 0;
            
            log.info("\n编码表达式验证结果:");
            log.info("  总记录数: {}", taggedRecords.size());
            log.info("  包含+a后缀: {}", correctCount);
            log.info("  缺少+a后缀: {}", missingCount);
            log.info("  正确率: {}%", String.format("%.2f", correctRate));
            
            if (correctRate == 100.0) {
                log.info("✅ 编码表达式验证通过");
            } else {
                log.warn("⚠️  编码表达式存在问题");
            }
        } else {
            log.info("无带标签的分配记录");
        }
    }

    /**
     * 构建客户筛选WHERE子句
     */
    private String buildWhereClause(Set<String> customerTypes, Set<String> workdays) {
        // 构建 ORDER_CYCLE 条件
        List<String> orderCycleConditions = new ArrayList<>();

        for (String customerType : customerTypes) {
            for (String workday : workdays) {
                switch (customerType) {
                    case "单周客户":
                        // 单周客户的 ORDER_CYCLE 格式: "单周周一", "单周周二", ...
                        orderCycleConditions.add("ORDER_CYCLE = '单周" + workday + "'");
                        break;
                    case "双周客户":
                        // 双周客户的 ORDER_CYCLE 格式: "双周周一", "双周周二", ...
                        orderCycleConditions.add("ORDER_CYCLE = '双周" + workday + "'");
                        break;
                    case "正常客户":
                        // 正常客户的 ORDER_CYCLE 格式: "周一", "周二", ...
                        orderCycleConditions.add("ORDER_CYCLE = '" + workday + "'");
                        break;
                    default:
                        log.warn("未知的客户类型: {}", customerType);
                }
            }
        }

        // 组合条件
        String orderCycleClause = orderCycleConditions.stream()
                .reduce((a, b) -> a + " OR " + b)
                .map(clause -> "(" + clause + ")")
                .orElse("(1=1)");

        // 排除 "不访销" 和 NULL
        return "WHERE " + orderCycleClause + " AND ORDER_CYCLE != '不访销' AND ORDER_CYCLE IS NOT NULL";
    }
}