package org.example.service;

import lombok.extern.slf4j.Slf4j;
import org.example.application.service.calculate.PriceBandAllocationService;
import org.example.domain.repository.CigaretteDistributionInfoRepository;
import org.example.domain.repository.CigaretteDistributionPredictionPriceRepository;
import org.example.domain.repository.FilterCustomerTableRepository;
import org.example.application.service.coordinator.RegionCustomerStatisticsBuildService;
import org.example.infrastructure.persistence.po.CigaretteDistributionInfoPO;
import org.example.shared.util.PartitionTableManager;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 按价位段自选投放分配写回集成测试。
 * <p>
 * 测试流程：
 * 1. 生成按价位段自选投放的测试用例
 * 2. 将测试用例插入到 cigarette_distribution_info 表的 2099/9/1 分区
 * 3. 调用 PriceBandAllocationService 执行分配
 * 4. 验证结果写回到 cigarette_distribution_prediction_price 表
 * </p>
 *
 * @author Robin
 * @since 2025-12-20
 */
@Slf4j
@SpringBootTest
@ActiveProfiles("test")
@DisplayName("按价位段自选投放分配写回集成测试")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class PriceBandAllocationIntegrationTest {

    @Autowired
    private PriceBandAllocationService priceBandAllocationService;

    @Autowired
    private CigaretteDistributionInfoRepository cigaretteDistributionInfoRepository;

    @Autowired
    private CigaretteDistributionPredictionPriceRepository predictionPriceRepository;

    @Autowired
    private FilterCustomerTableRepository filterCustomerTableRepository;

    @Autowired
    private RegionCustomerStatisticsBuildService regionCustomerStatisticsBuildService;

    @Autowired
    private PartitionTableManager partitionTableManager;

    private static final int YEAR = 2099;
    private static final int MONTH = 9;
    private static final int WEEK_SEQ = 1;

    // 从 base_cigarette_price 表获取的真实卷烟代码和批发价
    // 这些卷烟必须在价目表中存在，才能正确执行价位段分配
    private static final List<String[]> CIGARETTE_PRICES = Arrays.asList(
            new String[]{"42020181", "黄鹤楼(1916中支)", "1000.00"},
            new String[]{"42020035", "黄鹤楼(硬1916)", "848.00"},
            new String[]{"42020129", "黄鹤楼(软1916)", "848.00"},
            new String[]{"42020135", "黄鹤楼(硬15细支)", "848.00"},
            new String[]{"42020081", "黄鹤楼(硬15)", "848.00"},
            new String[]{"42020157", "黄鹤楼(硬1916如意)", "763.20"},
            new String[]{"42020088", "黄鹤楼(硬平安)", "720.80"},
            new String[]{"42020158", "黄鹤楼(硬1916红爆)", "720.80"},
            new String[]{"42020012", "黄鹤楼(软珍品)", "530.00"},
            new String[]{"42020149", "黄鹤楼(珍品细支)", "381.60"}
    );

    @BeforeEach
    void setUp() {
        log.info("===== 准备测试环境 =====");
        // 确保分区存在
        partitionTableManager.ensurePartitionExists("cigarette_distribution_info", YEAR, MONTH, WEEK_SEQ);
        partitionTableManager.ensurePartitionExists("cigarette_distribution_prediction_price", YEAR, MONTH, WEEK_SEQ);
    }

    /**
     * 测试步骤1：插入按价位段自选投放测试用例到 info 表
     */
    /**
     * 生成按价位段自选投放的测试用例
     * 直接构造测试数据，不依赖 TestCaseGenerator
     */
    private List<PriceBandTestCase> generatePriceBandTestCases() {
        List<PriceBandTestCase> cases = new ArrayList<>();
        
        // 预投放量阶层：从小到大覆盖各种场景
        BigDecimal[] advValues = {
                new BigDecimal("500"),     // 小投放量
                new BigDecimal("1500"),    // 中小投放量
                new BigDecimal("3000"),    // 中投放量
                new BigDecimal("8000"),    // 中大投放量
                new BigDecimal("15000"),   // 大投放量
                new BigDecimal("30000"),   // 超大投放量
                new BigDecimal("50000"),   // 特大投放量
                new BigDecimal("80000"),   // 巨大投放量
                new BigDecimal("100000"),  // 边界值
                new BigDecimal("120000")   // 最大投放量
        };
        
        // 标签：无标签和有标签
        String[] tags = {null, "优质数据共享客户"};
        
        int index = 0;
        for (BigDecimal adv : advValues) {
            for (String tag : tags) {
                cases.add(new PriceBandTestCase(
                        CIGARETTE_PRICES.get(index % CIGARETTE_PRICES.size()),
                        adv,
                        tag
                ));
                index++;
            }
        }
        
        return cases;
    }
    
    /**
     * 按价位段自选投放测试用例
     */
    private static class PriceBandTestCase {
        final String cigCode;
        final String cigName;
        final String wholesalePrice;
        final BigDecimal adv;
        final String tag;
        
        PriceBandTestCase(String[] cigData, BigDecimal adv, String tag) {
            this.cigCode = cigData[0];
            this.cigName = cigData[1];
            this.wholesalePrice = cigData[2];
            this.adv = adv;
            this.tag = tag;
        }
    }

    @Test
    @Order(1)
    @DisplayName("步骤1: 插入按价位段自选投放测试用例到 info 表")
    void step1_insertPriceBandTestCasesToInfoTable() {
        log.info("===== 开始插入测试用例 =====");

        // 先清理旧的按价位段自选投放数据
        try {
            int deletedCount = predictionPriceRepository.deleteByDeliveryMethod(YEAR, MONTH, WEEK_SEQ, "按价位段自选投放");
            log.info("清理旧的 prediction_price 数据: {} 条", deletedCount);
        } catch (Exception e) {
            log.warn("清理旧数据失败（可能不存在）: {}", e.getMessage());
        }

        // 生成测试用例
        List<PriceBandTestCase> priceBandCases = generatePriceBandTestCases();
        log.info("生成了 {} 个按价位段自选投放测试用例", priceBandCases.size());

        // 创建要插入的 PO 列表
        List<CigaretteDistributionInfoPO> infoList = new ArrayList<>();
        
        for (PriceBandTestCase testCase : priceBandCases) {
            CigaretteDistributionInfoPO info = new CigaretteDistributionInfoPO();
            info.setYear(YEAR);
            info.setMonth(MONTH);
            info.setWeekSeq(WEEK_SEQ);
            info.setCigCode(testCase.cigCode);
            info.setCigName(testCase.cigName);
            info.setAdv(testCase.adv);
            info.setDeliveryMethod("按价位段自选投放");
            info.setDeliveryEtype(null); // 按价位段自选投放无扩展类型
            info.setDeliveryArea("全市"); // 按价位段自选投放固定为全市
            info.setTag(testCase.tag);
            info.setTagFilterConfig(testCase.tag != null ? "0" : null);
            info.setSupplyAttribute("正常");
            info.setUrs(BigDecimal.ZERO);
            info.setBz("测试用例 - 按价位段自选投放 - ADV=" + testCase.adv);

            infoList.add(info);
        }

        // 批量插入
        if (!infoList.isEmpty()) {
            int insertedCount = cigaretteDistributionInfoRepository.batchUpsert(infoList);
            log.info("成功插入 {} 条按价位段自选投放测试用例", insertedCount);
        }

        // 验证插入结果
        List<Map<String, Object>> inserted = cigaretteDistributionInfoRepository.findPriceBandCandidates(YEAR, MONTH, WEEK_SEQ);
        log.info("验证: info 表中有 {} 条按价位段自选投放记录", inserted.size());
        
        assertFalse(inserted.isEmpty(), "应该有按价位段自选投放的测试数据");
        
        // 打印前5条
        inserted.stream().limit(5).forEach(row -> {
            log.info("  卷烟: {}({}), ADV={}, 批发价={}", 
                    row.get("CIG_NAME"), row.get("CIG_CODE"), 
                    row.get("ADV"), row.get("WHOLESALE_PRICE"));
        });
    }

    /**
     * 测试步骤2：验证 customer_filter 和 region_customer_statistics 数据准备
     */
    @Test
    @Order(2)
    @DisplayName("步骤2: 验证客户数据准备")
    void step2_verifyCustomerDataPreparation() {
        log.info("===== 验证客户数据准备 =====");

        // 检查 customer_filter 数据
        Long customerFilterCount = filterCustomerTableRepository.countPartition(YEAR, MONTH, WEEK_SEQ);
        log.info("customer_filter 表 {}/{}/{} 分区记录数: {}", YEAR, MONTH, WEEK_SEQ, customerFilterCount);
        
        if (customerFilterCount == null || customerFilterCount == 0) {
            log.warn("customer_filter 数据为空，需要先执行数据准备");
            
            // 准备 customer_filter 数据（筛选单周客户和正常客户，周一到周五）
            // ORDER_CYCLE 包含：周一、周二、周三、周四、周五、单周周一、单周周二、单周周三、单周周四、单周周五
            String whereClause = "WHERE ORDER_CYCLE IN ('周一', '周二', '周三', '周四', '周五', " +
                    "'单周周一', '单周周二', '单周周三', '单周周四', '单周周五') " +
                    "AND ORDER_CYCLE != '不访销'";
            
            filterCustomerTableRepository.ensurePartitionAndInsertData(
                    YEAR, MONTH, WEEK_SEQ, whereClause);
            
            customerFilterCount = filterCustomerTableRepository.countPartition(YEAR, MONTH, WEEK_SEQ);
            log.info("准备后 customer_filter 记录数: {}", customerFilterCount);
        }
        
        assertTrue(customerFilterCount != null && customerFilterCount > 0, 
                "customer_filter 表应该有数据");

        // 构建区域客户数统计
        log.info("构建区域客户数统计...");
        regionCustomerStatisticsBuildService.buildRegionCustomerStatistics(YEAR, MONTH, WEEK_SEQ);
        log.info("区域客户数统计构建完成");
    }

    /**
     * 测试步骤3：执行按价位段自选投放分配
     */
    @Test
    @Order(3)
    @DisplayName("步骤3: 执行按价位段自选投放分配")
    void step3_executePriceBandAllocation() {
        log.info("===== 执行按价位段自选投放分配 =====");

        // 执行分配
        priceBandAllocationService.allocateForPriceBand(YEAR, MONTH, WEEK_SEQ);
        log.info("分配执行完成");
    }

    /**
     * 测试步骤4：验证分配结果写回到 prediction_price 表
     */
    @Test
    @Order(4)
    @DisplayName("步骤4: 验证分配结果写回")
    void step4_verifyWriteBackResults() {
        log.info("===== 验证分配结果写回 =====");

        // 查询 prediction_price 表
        List<Map<String, Object>> results = predictionPriceRepository.findAll(YEAR, MONTH, WEEK_SEQ);
        log.info("prediction_price 表 {}/{}/{} 分区记录数: {}", YEAR, MONTH, WEEK_SEQ, results.size());

        assertFalse(results.isEmpty(), "prediction_price 表应该有分配结果");

        // 统计各投放方式的记录数
        Map<String, Long> byMethod = results.stream()
                .collect(Collectors.groupingBy(
                        r -> String.valueOf(r.get("DELIVERY_METHOD")),
                        Collectors.counting()));
        log.info("按投放方式分布: {}", byMethod);

        // 过滤按价位段自选投放的结果
        List<Map<String, Object>> priceBandResults = results.stream()
                .filter(r -> "按价位段自选投放".equals(r.get("DELIVERY_METHOD")))
                .collect(Collectors.toList());

        log.info("按价位段自选投放分配结果: {} 条", priceBandResults.size());
        
        // 打印前10条结果
        log.info("前10条分配结果:");
        priceBandResults.stream().limit(10).forEach(row -> {
            log.info("  卷烟: {}({}), 区域={}, 实际投放量={}, D30={}, D29={}, D1={}",
                    row.get("CIG_NAME"), row.get("CIG_CODE"),
                    row.get("DELIVERY_AREA"),
                    row.get("ACTUAL_DELIVERY"),
                    row.get("D30"), row.get("D29"), row.get("D1"));
        });

        // 验证每条记录的档位数据
        for (Map<String, Object> row : priceBandResults) {
            String cigCode = String.valueOf(row.get("CIG_CODE"));
            BigDecimal actualDelivery = toBigDecimal(row.get("ACTUAL_DELIVERY"));
            
            // 计算档位总和
            BigDecimal gradeSum = BigDecimal.ZERO;
            for (int i = 30; i >= 1; i--) {
                BigDecimal gradeValue = toBigDecimal(row.get("D" + i));
                if (gradeValue != null) {
                    gradeSum = gradeSum.add(gradeValue);
                }
            }
            
            log.debug("  卷烟 {}: 实际投放量={}, 档位总和={}", cigCode, actualDelivery, gradeSum);
            
            // 验证实际投放量不为空
            assertNotNull(actualDelivery, "实际投放量不应为null: " + cigCode);
        }

        assertTrue(priceBandResults.size() > 0, "应该有按价位段自选投放的分配结果");
        log.info("===== 验证完成 =====");
    }

    /**
     * 完整流程测试：从插入到验证的完整流程
     */
    @Test
    @Order(5)
    @DisplayName("完整流程测试: 插入 -> 分配 -> 验证")
    void testFullWorkflow() {
        log.info("===== 完整流程测试 =====");

        // 1. 准备数据
        step1_insertPriceBandTestCasesToInfoTable();
        
        // 2. 验证客户数据
        step2_verifyCustomerDataPreparation();
        
        // 3. 执行分配
        step3_executePriceBandAllocation();
        
        // 4. 验证结果
        step4_verifyWriteBackResults();

        log.info("===== 完整流程测试完成 =====");
    }

    /**
     * 单独测试：直接插入并验证分配结果
     */
    @Test
    @Order(6)
    @DisplayName("单独测试: 直接使用现有数据进行分配")
    void testWithExistingData() {
        log.info("===== 使用现有数据测试分配 =====");

        // 检查是否有候选卷烟
        List<Map<String, Object>> candidates = cigaretteDistributionInfoRepository.findPriceBandCandidates(YEAR, MONTH, WEEK_SEQ);
        
        if (candidates.isEmpty()) {
            log.info("没有现有的按价位段自选投放数据，先插入测试数据");
            step1_insertPriceBandTestCasesToInfoTable();
            candidates = cigaretteDistributionInfoRepository.findPriceBandCandidates(YEAR, MONTH, WEEK_SEQ);
        }

        log.info("候选卷烟数量: {}", candidates.size());
        
        if (!candidates.isEmpty()) {
            // 确保客户数据准备好
            step2_verifyCustomerDataPreparation();
            
            // 执行分配
            priceBandAllocationService.allocateForPriceBand(YEAR, MONTH, WEEK_SEQ);
            
            // 验证结果
            List<Map<String, Object>> results = predictionPriceRepository.findAll(YEAR, MONTH, WEEK_SEQ);
            log.info("分配后 prediction_price 记录数: {}", results.size());
            
            long priceBandCount = results.stream()
                    .filter(r -> "按价位段自选投放".equals(r.get("DELIVERY_METHOD")))
                    .count();
            log.info("其中按价位段自选投放: {} 条", priceBandCount);
            
            assertTrue(priceBandCount > 0, "应该有按价位段自选投放的分配结果");
        }

        log.info("===== 测试完成 =====");
    }

    private BigDecimal toBigDecimal(Object value) {
        if (value == null) {
            return BigDecimal.ZERO;
        }
        if (value instanceof BigDecimal) {
            return (BigDecimal) value;
        }
        try {
            return new BigDecimal(value.toString());
        } catch (NumberFormatException e) {
            return BigDecimal.ZERO;
        }
    }
}

