package org.example.fullpipeline;

import lombok.extern.slf4j.Slf4j;
import org.example.application.dto.GenerateDistributionPlanRequestDto;
import org.example.application.dto.GenerateDistributionPlanResponseDto;
import org.example.application.dto.TotalActualDeliveryResponseDto;
import org.example.application.service.calculate.DistributionCalculateService;
import org.example.application.service.coordinator.RegionCustomerStatisticsBuildService;
import org.example.domain.repository.FilterCustomerTableRepository;
import org.example.shared.util.PartitionTableManager;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 一键生成分配方案全链路测试
 * 
 * 测试流程：
 * 1. 准备数据：创建分区，筛选客户
 * 2. 构建区域客户数统计
 * 3. 执行分配计算
 * 4. 验证结果
 * 
 * @author Robin
 * @since 2025-12-20
 */
@Slf4j
@SpringBootTest
@ActiveProfiles("test")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@DisplayName("一键生成分配方案全链路测试")
public class FullDIstributionPipelineTest {

    @Autowired
    private FilterCustomerTableRepository filterCustomerTableRepository;

    @Autowired
    private RegionCustomerStatisticsBuildService regionCustomerStatisticsBuildService;

    @Autowired
    private DistributionCalculateService distributionCalculateService;

    @Autowired
    private PartitionTableManager partitionTableManager;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    // 测试参数
    private static final int YEAR = 2025;
    private static final int MONTH = 9;
    private static final int WEEK_SEQ = 3;

    @BeforeEach
    void setUp() {
        log.info("========================================");
        log.info("开始全链路测试：{}-{}-{}", YEAR, MONTH, WEEK_SEQ);
        log.info("========================================");
    }

    @Test
    @Order(1)
    @DisplayName("步骤1: 准备数据 - 创建分区并筛选客户")
    void step1_prepareData() {
        log.info("\n===== 步骤1: 准备数据 =====");
        
        long startTime = System.currentTimeMillis();
        
        try {
            // 1.1 确保分区存在
            log.info("1.1 确保分区存在...");
            partitionTableManager.ensurePartitionExists("customer_filter", YEAR, MONTH, WEEK_SEQ);
            partitionTableManager.ensurePartitionExists("region_customer_statistics", YEAR, MONTH, WEEK_SEQ);
            partitionTableManager.ensurePartitionExists("cigarette_distribution_prediction", YEAR, MONTH, WEEK_SEQ);
            partitionTableManager.ensurePartitionExists("cigarette_distribution_prediction_price", YEAR, MONTH, WEEK_SEQ);
            log.info("✅ 分区创建成功");
            
            // 1.2 准备客户筛选参数
            log.info("\n1.2 筛选客户数据...");
            List<String> customerTypes = Arrays.asList("单周客户", "正常客户");
            List<String> workdays = Arrays.asList("周一", "周二", "周三", "周四", "周五");
            
            log.info("  客户类型: {}", customerTypes);
            log.info("  工作日: {}", workdays);
            
            // 1.3 构建 WHERE 子句并执行客户筛选
            String whereClause = buildWhereClause(customerTypes, workdays);
            log.info("  WHERE 子句: {}", whereClause);
            
            filterCustomerTableRepository.ensurePartitionAndInsertData(YEAR, MONTH, WEEK_SEQ, whereClause);
            
            // 1.4 验证筛选结果
            String countSql = "SELECT COUNT(*) FROM customer_filter WHERE YEAR = ? AND MONTH = ? AND WEEK_SEQ = ?";
            Integer customerCount = jdbcTemplate.queryForObject(countSql, Integer.class, YEAR, MONTH, WEEK_SEQ);
            
            log.info("✅ 客户筛选完成");
            log.info("  筛选出的客户数: {}", customerCount);
            
            if (customerCount == null || customerCount == 0) {
                log.error("❌ 警告: customer_filter 表中没有数据！");
                log.error("  请检查 base_customer_info 表是否有符合条件的客户数据");
            }
            
            long elapsedTime = System.currentTimeMillis() - startTime;
            log.info("⏱️  步骤1耗时: {} ms", elapsedTime);
            
        } catch (Exception e) {
            log.error("❌ 步骤1失败: {}", e.getMessage(), e);
            throw e;
        }
    }

    @Test
    @Order(2)
    @DisplayName("步骤2: 构建区域客户数统计")
    void step2_buildRegionStatistics() {
        log.info("\n===== 步骤2: 构建区域客户数统计 =====");
        
        long startTime = System.currentTimeMillis();
        
        try {
            // 执行区域客户数统计
            Map<String, Object> result = regionCustomerStatisticsBuildService.buildRegionCustomerStatistics(YEAR, MONTH, WEEK_SEQ);
            
            // 验证结果
            Boolean success = (Boolean) result.get("success");
            String message = (String) result.get("message");
            Integer insertedCount = (Integer) result.get("insertedCount");
            
            if (Boolean.TRUE.equals(success)) {
                log.info("✅ 区域客户数统计构建成功");
                log.info("  插入记录数: {}", insertedCount);
                log.info("  消息: {}", message);
                
                // 查询统计详情
                String detailSql = "SELECT REGION, TOTAL FROM region_customer_statistics " +
                                  "WHERE YEAR = ? AND MONTH = ? AND WEEK_SEQ = ? " +
                                  "ORDER BY TOTAL DESC LIMIT 10";
                List<Map<String, Object>> topRegions = jdbcTemplate.queryForList(detailSql, YEAR, MONTH, WEEK_SEQ);
                
                log.info("\n  客户数最多的前10个区域:");
                for (Map<String, Object> region : topRegions) {
                    log.info("    {} - 客户数: {}", region.get("REGION"), region.get("TOTAL"));
                }
                
            } else {
                log.error("❌ 区域客户数统计构建失败");
                log.error("  错误信息: {}", message);
                throw new RuntimeException("区域客户数统计构建失败: " + message);
            }
            
            long elapsedTime = System.currentTimeMillis() - startTime;
            log.info("⏱️  步骤2耗时: {} ms", elapsedTime);
            
        } catch (Exception e) {
            log.error("❌ 步骤2失败: {}", e.getMessage(), e);
            throw e;
        }
    }

    @Test
    @Order(3)
    @DisplayName("步骤3: 执行分配计算并写回")
    void step3_executeAllocation() {
        log.info("\n===== 步骤3: 执行分配计算并写回 =====");
        
        long startTime = System.currentTimeMillis();
        
        try {
            // 3.1 检查 cigarette_distribution_info 表中是否有待分配的卷烟
            String infoCountSql = "SELECT COUNT(*) FROM cigarette_distribution_info WHERE YEAR = ? AND MONTH = ? AND WEEK_SEQ = ?";
            Integer infoCount = jdbcTemplate.queryForObject(infoCountSql, Integer.class, YEAR, MONTH, WEEK_SEQ);
            
            log.info("3.1 待分配卷烟数: {}", infoCount);
            
            if (infoCount == null || infoCount == 0) {
                log.warn("⚠️  警告: cigarette_distribution_info 表中没有待分配的卷烟数据");
                log.warn("  跳过分配计算步骤");
                return;
            }
            
            // 3.2 执行总实际投放量计算
            log.info("\n3.2 执行总实际投放量计算...");
            TotalActualDeliveryResponseDto totalResponse = distributionCalculateService.calculateTotalActualDelivery(YEAR, MONTH, WEEK_SEQ);
            
            if (totalResponse == null || !Boolean.TRUE.equals(totalResponse.getSuccess())) {
                String errorMsg = totalResponse != null ? totalResponse.getMessage() : "返回结果为空";
                log.error("❌ 总实际投放量计算失败: {}", errorMsg);
                throw new RuntimeException("总实际投放量计算失败: " + errorMsg);
            }
            log.info("✅ 总实际投放量计算完成");
            
            // 3.3 执行分配方案生成
            log.info("\n3.3 执行分配方案生成...");
            GenerateDistributionPlanRequestDto planRequest = new GenerateDistributionPlanRequestDto();
            planRequest.setYear(YEAR);
            planRequest.setMonth(MONTH);
            planRequest.setWeekSeq(WEEK_SEQ);
            
            GenerateDistributionPlanResponseDto planResponse = distributionCalculateService.generateDistributionPlan(planRequest);
            
            if (planResponse == null || !Boolean.TRUE.equals(planResponse.getSuccess())) {
                String errorMsg = planResponse != null ? planResponse.getMessage() : "返回结果为空";
                log.error("❌ 分配方案生成失败: {}", errorMsg);
                throw new RuntimeException("分配方案生成失败: " + errorMsg);
            }
            
                log.info("✅ 分配方案生成成功");
            log.info("  消息: {}", planResponse.getMessage());
            log.info("  处理卷烟数: {}", planResponse.getTotalCigarettes());
            log.info("  成功分配数: {}", planResponse.getSuccessfulAllocations());
            log.info("  处理时间: {}", planResponse.getProcessingTime());
            
            long elapsedTime = System.currentTimeMillis() - startTime;
            log.info("⏱️  步骤3耗时: {} ms", elapsedTime);
            
        } catch (Exception e) {
            log.error("❌ 步骤3失败: {}", e.getMessage(), e);
            throw e;
        }
    }

    @Test
    @Order(4)
    @DisplayName("步骤4: 验证分配结果")
    void step4_verifyResults() {
        log.info("\n===== 步骤4: 验证分配结果 =====");
        
        try {
            // 4.1 统计分配结果
            String predictionCountSql = "SELECT COUNT(*) FROM cigarette_distribution_prediction WHERE YEAR = ? AND MONTH = ? AND WEEK_SEQ = ?";
            Integer predictionCount = jdbcTemplate.queryForObject(predictionCountSql, Integer.class, YEAR, MONTH, WEEK_SEQ);
            
            String priceCountSql = "SELECT COUNT(*) FROM cigarette_distribution_prediction_price WHERE YEAR = ? AND MONTH = ? AND WEEK_SEQ = ?";
            Integer priceCount = jdbcTemplate.queryForObject(priceCountSql, Integer.class, YEAR, MONTH, WEEK_SEQ);
            
            log.info("4.1 分配结果统计:");
            log.info("  cigarette_distribution_prediction 记录数: {}", predictionCount);
            log.info("  cigarette_distribution_prediction_price 记录数: {}", priceCount);
            
            // 4.2 检查未分配的卷烟
            String infoCountSql = "SELECT COUNT(*) FROM cigarette_distribution_info WHERE YEAR = ? AND MONTH = ? AND WEEK_SEQ = ?";
            Integer infoCount = jdbcTemplate.queryForObject(infoCountSql, Integer.class, YEAR, MONTH, WEEK_SEQ);
            
            if (infoCount != null && infoCount > 0) {
                String unallocatedSql = 
                    "SELECT i.CIG_CODE, i.CIG_NAME, i.DELIVERY_METHOD, i.DELIVERY_ETYPE, i.DELIVERY_AREA " +
                    "FROM cigarette_distribution_info i " +
                    "LEFT JOIN cigarette_distribution_prediction p " +
                    "  ON i.CIG_CODE = p.CIG_CODE AND i.YEAR = p.YEAR AND i.MONTH = p.MONTH AND i.WEEK_SEQ = p.WEEK_SEQ " +
                    "WHERE i.YEAR = ? AND i.MONTH = ? AND i.WEEK_SEQ = ? " +
                    "  AND p.CIG_CODE IS NULL " +
                    "LIMIT 20";
                
                List<Map<String, Object>> unallocated = jdbcTemplate.queryForList(unallocatedSql, YEAR, MONTH, WEEK_SEQ);
                
                if (!unallocated.isEmpty()) {
                    log.warn("\n⚠️  发现 {} 条未分配的卷烟（显示前20条）:", unallocated.size());
                    for (Map<String, Object> cig : unallocated) {
                        log.warn("  {} ({}) - {} / {}, 投放区域={}", 
                                cig.get("CIG_NAME"), cig.get("CIG_CODE"),
                                cig.get("DELIVERY_METHOD"), cig.get("DELIVERY_ETYPE"),
                                cig.get("DELIVERY_AREA"));
                    }
                } else {
                    log.info("✅ 所有卷烟都已完成分配");
                }
            }
            
            // 4.3 显示分配结果样例
            if (predictionCount != null && predictionCount > 0) {
                String sampleSql = 
                    "SELECT CIG_CODE, CIG_NAME, DELIVERY_AREA, ACTUAL_DELIVERY " +
                    "FROM cigarette_distribution_prediction " +
                    "WHERE YEAR = ? AND MONTH = ? AND WEEK_SEQ = ? " +
                    "ORDER BY ACTUAL_DELIVERY DESC " +
                    "LIMIT 10";
                
                List<Map<String, Object>> samples = jdbcTemplate.queryForList(sampleSql, YEAR, MONTH, WEEK_SEQ);
                
                log.info("\n4.3 分配结果样例（实际投放量最大的前10条）:");
                for (Map<String, Object> sample : samples) {
                    log.info("  {} ({}) - {} -> 实际={}", 
                            sample.get("CIG_NAME"), sample.get("CIG_CODE"),
                            sample.get("DELIVERY_AREA"),
                            sample.get("ACTUAL_DELIVERY"));
                }
            }
            
            log.info("\n✅ 结果验证完成");
            
        } catch (Exception e) {
            log.error("❌ 步骤4失败: {}", e.getMessage(), e);
            throw e;
        }
    }

    @Test
    @Order(5)
    @DisplayName("完整流程: 一键执行所有步骤")
    void fullPipeline() {
        log.info("\n========================================");
        log.info("开始执行完整流程");
        log.info("========================================");
        
        long totalStartTime = System.currentTimeMillis();
        
        try {
            // 步骤1: 准备数据
            step1_prepareData();
            
            // 步骤2: 构建区域客户数统计
            step2_buildRegionStatistics();
            
            // 步骤3: 执行分配计算
            step3_executeAllocation();
            
            // 步骤4: 验证结果
            step4_verifyResults();
            
            long totalElapsedTime = System.currentTimeMillis() - totalStartTime;
            
            log.info("\n========================================");
            log.info("✅ 全链路测试完成！");
            log.info("⏱️  总耗时: {} ms ({} 秒)", totalElapsedTime, totalElapsedTime / 1000.0);
            log.info("========================================");
            
        } catch (Exception e) {
            log.error("\n========================================");
            log.error("❌ 全链路测试失败！");
            log.error("错误信息: {}", e.getMessage(), e);
            log.error("========================================");
            throw e;
        }
    }
    
    @AfterEach
    void tearDown() {
        log.info("测试步骤完成\n");
    }
    
    /**
     * 根据客户类型和工作日构建 WHERE 子句
     * 
     * @param customerTypes 客户类型列表 (单周客户、双周客户、正常客户)
     * @param workdays 工作日列表 (周一、周二、周三、周四、周五)
     * @return WHERE 子句
     */
    private String buildWhereClause(List<String> customerTypes, List<String> workdays) {
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
                .collect(Collectors.joining(" OR ", "(", ")"));

        // 排除 "不访销" 和 NULL
        return "WHERE " + orderCycleClause + " AND ORDER_CYCLE != '不访销' AND ORDER_CYCLE IS NOT NULL";
    }
}