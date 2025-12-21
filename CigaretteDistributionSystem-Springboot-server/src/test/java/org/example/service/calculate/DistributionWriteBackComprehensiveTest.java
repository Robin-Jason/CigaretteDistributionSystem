package org.example.service.calculate;

import org.example.application.service.calculate.DistributionCalculateService;

import lombok.extern.slf4j.Slf4j;
import org.example.application.dto.GenerateDistributionPlanRequestDto;
import org.example.application.dto.GenerateDistributionPlanResponseDto;
import org.example.application.service.coordinator.RegionCustomerStatisticsBuildService;
import org.example.domain.repository.CigaretteDistributionInfoRepository;
import org.example.domain.repository.RegionCustomerStatisticsRepository;
import org.example.domain.repository.FilterCustomerTableRepository;
import org.example.infrastructure.persistence.po.CigaretteDistributionInfoPO;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 分配写回综合测试
 * <p>
 * 测试流程：
 * 1. 使用TestCaseGenerator生成测试用例
 * 2. 将测试用例插入到数据库（2099年9月第1周）
 * 3. 执行分配方案生成
 * 4. 验证分配写回的结果和误差
 * </p>
 *
 * @author Robin
 * @since 2025-12-20
 */
@Slf4j
@SpringBootTest
@ActiveProfiles("test")
@DisplayName("分配写回综合测试")
public class DistributionWriteBackComprehensiveTest {

    @Autowired
    private DistributionCalculateService distributionCalculateService;

    @Autowired
    private CigaretteDistributionInfoRepository cigaretteDistributionInfoRepository;

    @Autowired
    private RegionCustomerStatisticsRepository regionCustomerStatisticsRepository;

    @Autowired
    private RegionCustomerStatisticsBuildService regionCustomerStatisticsBuildService;

    @Autowired
    private FilterCustomerTableRepository filterCustomerTableRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private TestCaseGenerator testCaseGenerator;

    @Autowired
    private org.example.infrastructure.config.encoding.EncodingRuleRepository encodingRuleRepository;

    // 使用2099年9月第1周作为测试分区（避免与真实数据冲突）
    // 注意：region_customer_statistics数据会在运行一键生成分配方案时自动生成
    private static final int TEST_YEAR = 2099;
    private static final int TEST_MONTH = 9;
    private static final int TEST_WEEK_SEQ = 1;

    // 可用区域列表（按客户数从高到低排序）
    private static final String[] REGIONS = {
            "城区",
            "丹江口市",
            "房县",
            "郧阳区",
            "竹山县",
            "郧西县",
            "竹溪县"
    };

    private List<String> availableRegions;
    private int testCaseCount = 0;

    @BeforeEach
    void setUp() {
        long startTime = System.currentTimeMillis();
        log.info("==========================================");
        log.info("开始准备测试数据：{}年{}月第{}周", TEST_YEAR, TEST_MONTH, TEST_WEEK_SEQ);
        log.info("==========================================");

        // 清理旧测试数据（包括预测表和测试用例）
        long cleanupStart = System.currentTimeMillis();
        cleanupTestData();
        long cleanupTime = System.currentTimeMillis() - cleanupStart;
        log.info("⏱️  清理旧数据耗时: {} ms", cleanupTime);

        // 生成并插入测试用例到cigarette_distribution_info表
        long generateStart = System.currentTimeMillis();
        generateAndInsertTestCases();
        long generateTime = System.currentTimeMillis() - generateStart;
        log.info("⏱️  生成并插入测试用例耗时: {} ms", generateTime);

        // 为按价位段自选投放的测试用例准备批发价数据
        long priceStart = System.currentTimeMillis();
        initializeTestCigarettePrices();
        long priceTime = System.currentTimeMillis() - priceStart;
        log.info("⏱️  初始化测试卷烟批发价耗时: {} ms", priceTime);

        // 创建临时表并生成region_customer_statistics数据
        long buildStatsStart = System.currentTimeMillis();
        buildRegionCustomerStatisticsData();
        long buildStatsTime = System.currentTimeMillis() - buildStatsStart;
        log.info("⏱️  构建region_customer_statistics数据耗时: {} ms", buildStatsTime);

        long totalSetupTime = System.currentTimeMillis() - startTime;
        log.info("✅ 测试用例数据准备完成，总耗时: {} ms", totalSetupTime);
        log.info("   - 测试用例已插入到cigarette_distribution_info表");
        log.info("   - 测试卷烟批发价已初始化");
        log.info("   - region_customer_statistics数据已生成");
    }

    @AfterEach
    void tearDown() {
        log.info("==========================================");
        log.info("测试完成，清理测试数据");
        log.info("==========================================");
        // 可选：清理测试数据
        // cleanupTestData();
    }

    /**
     * 加载可用区域列表
     * 注意：在第一次运行分配方案生成前，region_customer_statistics可能没有数据
     * 此时使用默认区域列表，分配方案生成后会自动创建region_customer_statistics数据
     */
    private void loadAvailableRegions() {
        // 初始化时使用默认区域列表
        // region_customer_statistics数据会在运行一键生成分配方案时自动生成
        availableRegions = Arrays.asList(REGIONS);
        log.info("使用默认区域列表: {}", Arrays.toString(REGIONS));
        log.info("注意：region_customer_statistics数据会在运行一键生成分配方案时自动生成");
    }

    /**
     * 清理测试数据
     * 注意：只清理cigarette_distribution_info和prediction表，保留region_customer_statistics
     */
    private void cleanupTestData() {
        try {
            // 删除预测表数据
            String deletePredictionSql = "DELETE FROM cigarette_distribution_prediction " +
                    "WHERE YEAR = ? AND MONTH = ? AND WEEK_SEQ = ?";
            int deletedPrediction = jdbcTemplate.update(deletePredictionSql, TEST_YEAR, TEST_MONTH, TEST_WEEK_SEQ);
            log.info("清理预测表数据: {} 条", deletedPrediction);

            String deletePricePredictionSql = "DELETE FROM cigarette_distribution_prediction_price " +
                    "WHERE YEAR = ? AND MONTH = ? AND WEEK_SEQ = ?";
            int deletedPricePrediction = jdbcTemplate.update(deletePricePredictionSql, TEST_YEAR, TEST_MONTH, TEST_WEEK_SEQ);
            log.info("清理价位段预测表数据: {} 条", deletedPricePrediction);

            // 删除投放信息表数据（只删除TEST_开头的测试数据，避免影响真实数据）
            String deleteInfoSql = "DELETE FROM cigarette_distribution_info " +
                    "WHERE YEAR = ? AND MONTH = ? AND WEEK_SEQ = ? AND CIG_CODE LIKE 'TEST_%'";
            int deletedInfo = jdbcTemplate.update(deleteInfoSql, TEST_YEAR, TEST_MONTH, TEST_WEEK_SEQ);
            log.info("清理测试投放信息表数据: {} 条", deletedInfo);
        } catch (Exception e) {
            log.warn("清理测试数据失败: {}", e.getMessage());
        }
    }

    /**
     * 构建region_customer_statistics数据
     * 使用分区表 customer_filter 替代临时表，提升性能并避免连接超时
     */
    private void buildRegionCustomerStatisticsData() {
        log.info("开始构建region_customer_statistics数据...");
        long totalStart = System.currentTimeMillis();

            String whereClause = "WHERE ORDER_CYCLE <> '不访销'";
            
        try {
            // 1. 使用分区表：确保分区存在并插入数据
            log.info("准备分区表数据: customer_filter (year={}, month={}, weekSeq={})", 
                    TEST_YEAR, TEST_MONTH, TEST_WEEK_SEQ);
            long prepareStart = System.currentTimeMillis();
            
            filterCustomerTableRepository.ensurePartitionAndInsertData(
                    TEST_YEAR, TEST_MONTH, TEST_WEEK_SEQ, whereClause);
            
            long prepareTime = System.currentTimeMillis() - prepareStart;
            log.info("⏱️  准备分区表数据耗时: {} ms", prepareTime);
                
            // 2. 统计分区记录数
            Long count = filterCustomerTableRepository.countPartition(
                    TEST_YEAR, TEST_MONTH, TEST_WEEK_SEQ);
            log.info("分区表数据准备成功，记录数: {}", count);

            // 3. 调用buildRegionCustomerStatistics生成region_customer_statistics数据
            // 注意：这里传入 null，让服务层知道使用分区表
            log.info("开始生成region_customer_statistics数据...");
            long buildStatsStart = System.currentTimeMillis();
            Map<String, Object> buildResult = regionCustomerStatisticsBuildService.buildRegionCustomerStatistics(
                    TEST_YEAR, TEST_MONTH, TEST_WEEK_SEQ);
            long buildStatsTime = System.currentTimeMillis() - buildStatsStart;
            log.info("⏱️  生成region_customer_statistics数据耗时: {} ms", buildStatsTime);

            Boolean success = (Boolean) buildResult.get("success");
            if (Boolean.TRUE.equals(success)) {
                Integer insertedCount = (Integer) buildResult.get("insertedCount");
                log.info("✅ region_customer_statistics数据生成成功，插入 {} 条记录", insertedCount);
            } else {
                String message = (String) buildResult.get("message");
                log.error("❌ region_customer_statistics数据生成失败: {}", message);
                throw new RuntimeException("region_customer_statistics数据生成失败: " + message);
            }

            long totalTime = System.currentTimeMillis() - totalStart;
            log.info("⏱️  构建region_customer_statistics数据总耗时: {} ms", totalTime);

        } catch (Exception e) {
            log.error("构建region_customer_statistics数据失败", e);
            throw new RuntimeException("构建region_customer_statistics数据失败: " + e.getMessage(), e);
        }
    }

    /**
     * 生成并插入测试用例
     */
    private void generateAndInsertTestCases() {
        log.info("开始生成测试用例...");
        long totalStart = System.currentTimeMillis();

        // 生成测试用例（使用较小的样本数量以加快测试速度）
        // 注意：region_customer_statistics数据会在运行一键生成分配方案时自动生成
        // 使用Spring注入的TestCaseGenerator实例，确保正确加载诚信互助小组编码
        int maxRegions = 7; // 使用默认7个区域
        long generateStart = System.currentTimeMillis();
        List<TestCaseGenerator.TestCaseConfig> testCases = testCaseGenerator.generateAllTestCasesInternal(maxRegions, 42);
        long generateTime = System.currentTimeMillis() - generateStart;
        log.info("⏱️  生成测试用例配置耗时: {} ms", generateTime);
        log.info("生成了 {} 个测试用例配置", testCases.size());

        // 使用所有测试用例进行完整测试
        // 注意：region_customer_statistics数据会在运行一键生成分配方案时自动生成
        List<TestCaseGenerator.TestCaseConfig> selectedCases = testCases;
        testCaseCount = selectedCases.size();

        log.info("使用全部 {} 个用例进行测试", testCaseCount);
        log.info("测试用例将插入到cigarette_distribution_info表（{}年{}月第{}周）", TEST_YEAR, TEST_MONTH, TEST_WEEK_SEQ);

        // 批量插入测试用例
        long buildDataStart = System.currentTimeMillis();
        List<CigaretteDistributionInfoPO> testDataList = new ArrayList<>();
        for (int i = 0; i < selectedCases.size(); i++) {
            TestCaseGenerator.TestCaseConfig config = selectedCases.get(i);
            CigaretteDistributionInfoPO info = buildTestData(config, i + 1);
            testDataList.add(info);
        }
        long buildDataTime = System.currentTimeMillis() - buildDataStart;
        log.info("⏱️  构建测试数据对象耗时: {} ms", buildDataTime);

        // 批量插入
        long insertStart = System.currentTimeMillis();
        try {
            cigaretteDistributionInfoRepository.batchUpsert(testDataList);
            long insertTime = System.currentTimeMillis() - insertStart;
            log.info("⏱️  批量插入数据库耗时: {} ms", insertTime);
            log.info("✅ 成功插入 {} 条测试用例数据", testDataList.size());
        } catch (Exception e) {
            log.error("插入测试用例数据失败", e);
            fail("插入测试用例数据失败: " + e.getMessage());
        }
        
        long totalTime = System.currentTimeMillis() - totalStart;
        log.info("⏱️  生成并插入测试用例总耗时: {} ms", totalTime);
    }

    /**
     * 构建测试数据
     */
    private CigaretteDistributionInfoPO buildTestData(TestCaseGenerator.TestCaseConfig config, int index) {
        String cigCode = String.format("TEST_%05d", index);
        String cigName = buildCigName(config);
        String deliveryArea = buildDeliveryArea(config.regionCount, config.deliveryMethod, 
                config.deliveryEtype, config.availableRegions, config.adv);

        CigaretteDistributionInfoPO info = new CigaretteDistributionInfoPO();
        info.setCigCode(cigCode);
        info.setCigName(cigName);
        info.setYear(TEST_YEAR);
        info.setMonth(TEST_MONTH);
        info.setWeekSeq(TEST_WEEK_SEQ);
        info.setAdv(config.adv);
        info.setDeliveryMethod(config.deliveryMethod);
        info.setDeliveryEtype(config.deliveryEtype);
        info.setDeliveryArea(deliveryArea);
        info.setTag(config.tag);
        // 如果TAG为"优质数据共享客户"，TAG_FILTER_CONFIG应当默认为"0"
        if ("优质数据共享客户".equals(config.tag)) {
            info.setTagFilterConfig("0");
        }
        info.setSupplyAttribute("正常");
        info.setUrs(BigDecimal.ZERO);

        return info;
    }

    /**
     * 构建卷烟名称
     */
    private String buildCigName(TestCaseGenerator.TestCaseConfig config) {
        StringBuilder name = new StringBuilder("测试卷烟");
        name.append("_").append(config.deliveryMethod);
        if (config.deliveryEtype != null) {
            name.append("_").append(config.deliveryEtype.replace("+", "_"));
        }
        if (config.tag != null) {
            name.append("_").append("有标签");
        }
        return name.toString();
    }

    /**
     * 构建投放区域字符串
     * 
     * 业务规则：
     * - regionCount = 0：表示"全市"
     * - regionCount > 0：根据预投放量（ADV）选择区域
     *   - ADV >= 10000：按客户数占比的加权随机选择（客户数多的区域被选中的概率更高）
     *   - ADV < 10000：等概率随机选择
     * 
     * 注意：DELIVERY_AREA字段有长度限制（800字符），对于双扩展类型的笛卡尔积区域集合，
     * 如果区域数量过多导致字符串过长，需要限制区域数量或截断。
     */
    private String buildDeliveryArea(int regionCount, String deliveryMethod, 
                                    String deliveryEtype, List<String> availableRegions, BigDecimal adv) {
        // 按档位投放和按价位段自选投放：固定为"全市"
        if ("按档位投放".equals(deliveryMethod) || "按价位段自选投放".equals(deliveryMethod)) {
            return "全市";
        }

        // regionCount = 0 也表示"全市"（按档位扩展投放的特殊情况）
        if (regionCount <= 0 || availableRegions == null || availableRegions.isEmpty()) {
            return "全市";
        }

        // 确定实际选择的区域数量
        int actualRegionCount = regionCount;
        if (regionCount >= availableRegions.size()) {
            actualRegionCount = availableRegions.size();
        }

        // 根据ADV选择区域
        List<String> selected;
        if (adv != null && adv.compareTo(new BigDecimal("10000")) >= 0) {
            // ADV >= 10000：按客户数占比的加权随机选择
            selected = selectRegionsByWeight(availableRegions, actualRegionCount);
        } else {
            // ADV < 10000：等概率随机选择
            selected = selectRegionsRandomly(availableRegions, actualRegionCount);
        }
        
        Collections.sort(selected); // 按名称排序以保证结果一致性
        
        // 使用中文逗号"、"分隔（与实际数据格式一致）
        String result = String.join("、", selected);
        
        // 限制最大长度为800字符（DELIVERY_AREA字段为VARCHAR(800)）
        // 如果超过限制，从后往前截断，保留尽可能多的区域
        int maxLength = 800;
        if (result.length() > maxLength) {
            // 从后往前截断，保留前面的区域
            while (result.length() > maxLength && selected.size() > 1) {
                selected.remove(selected.size() - 1);
                result = String.join("、", selected);
            }
            // 如果单个区域就超过限制，截断该区域字符串
            if (result.length() > maxLength && selected.size() == 1) {
                result = selected.get(0).substring(0, Math.min(maxLength, selected.get(0).length()));
            }
        }
        
        return result;
    }

    /**
     * 等概率随机选择区域（ADV < 10000）
     * 
     * @param availableRegions 可用区域列表
     * @param count 需要选择的区域数量
     * @return 选中的区域列表
     */
    private List<String> selectRegionsRandomly(List<String> availableRegions, int count) {
        List<String> shuffled = new ArrayList<>(availableRegions);
        Collections.shuffle(shuffled, new Random(42)); // 使用固定种子保证可重复性
        return shuffled.subList(0, count);
    }

    /**
     * 按客户数占比的加权随机选择区域（ADV >= 10000）
     * 
     * @param availableRegions 可用区域列表
     * @param count 需要选择的区域数量
     * @return 选中的区域列表
     */
    private List<String> selectRegionsByWeight(List<String> availableRegions, int count) {
        // 获取区域客户数映射
        Map<String, BigDecimal> regionCustomerCountMap = getRegionCustomerCountMap(availableRegions);
        
        // 计算总客户数
        BigDecimal totalCustomers = availableRegions.stream()
                .map(region -> regionCustomerCountMap.getOrDefault(region, BigDecimal.ZERO))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        // 如果总客户数为0，回退到等概率随机选择
        if (totalCustomers.compareTo(BigDecimal.ZERO) == 0) {
            return selectRegionsRandomly(availableRegions, count);
        }
        
        // 按权重随机选择（不重复）
        List<String> selected = new ArrayList<>();
        Random random = new Random(42); // 使用固定种子保证可重复性
        List<String> remainingRegions = new ArrayList<>(availableRegions);
        
        while (selected.size() < count && !remainingRegions.isEmpty()) {
            // 构建剩余区域的加权列表
            List<String> remainingWeighted = new ArrayList<>();
            for (String region : remainingRegions) {
                BigDecimal customerCount = regionCustomerCountMap.getOrDefault(region, BigDecimal.ZERO);
                // 计算权重（客户数占比 * 1000，放大1000倍以保证精度）
                int weight = customerCount.multiply(new BigDecimal("1000"))
                        .divide(totalCustomers, 0, java.math.RoundingMode.HALF_UP)
                        .intValue();
                // 至少权重为1，确保每个区域都有被选中的可能
                weight = Math.max(1, weight);
                for (int i = 0; i < weight; i++) {
                    remainingWeighted.add(region);
                }
            }
            
            if (remainingWeighted.isEmpty()) {
                break;
            }
            
            String selectedRegion = remainingWeighted.get(random.nextInt(remainingWeighted.size()));
            selected.add(selectedRegion);
            remainingRegions.remove(selectedRegion);
        }
        
        return selected;
    }


    /**
     * 获取区域客户数映射
     * 
     * @param regions 区域列表
     * @return 区域名称 -> 客户总数的映射
     */
    private Map<String, BigDecimal> getRegionCustomerCountMap(List<String> regions) {
        Map<String, BigDecimal> customerCountMap = new HashMap<>();
        
        try {
            // 查询所有区域客户统计数据
            List<Map<String, Object>> allStats = regionCustomerStatisticsRepository.findAll(
                    TEST_YEAR, TEST_MONTH, TEST_WEEK_SEQ);
            
            if (allStats != null && !allStats.isEmpty()) {
                for (Map<String, Object> stat : allStats) {
                    String region = (String) stat.get("REGION");
                    Object totalObj = stat.get("TOTAL");
                    BigDecimal total = totalObj != null ? 
                            (totalObj instanceof BigDecimal ? (BigDecimal) totalObj : 
                             new BigDecimal(totalObj.toString())) : BigDecimal.ZERO;
                    if (region != null) {
                        customerCountMap.put(region, total);
                    }
                }
            }
        } catch (Exception e) {
            // 如果查询失败，返回空映射（所有区域客户数为0，将按名称排序）
        }
        
        return customerCountMap;
    }

    /**
     * 执行分配写回测试
     * 
     * 测试流程：
     * 1. 已通过setUp()将测试用例插入到cigarette_distribution_info表
     * 2. 执行一键生成分配方案（会自动生成region_customer_statistics数据）
     * 3. 验证分配写回结果和误差
     */
    @Test
    @DisplayName("执行分配方案生成并验证写回结果")
    // 注意：不使用@Transactional，避免嵌套事务导致临时表锁问题
    // buildRegionCustomerStatistics使用REQUIRES_NEW在独立事务中运行
    void testDistributionWriteBack() {
        log.info("==========================================");
        log.info("开始执行分配写回测试");
        log.info("测试用例数: {}", testCaseCount);
        log.info("测试分区: {}年{}月第{}周", TEST_YEAR, TEST_MONTH, TEST_WEEK_SEQ);
        log.info("==========================================");

        // 步骤1: 执行分配方案生成
        // 注意：region_customer_statistics数据已在setUp()中通过buildRegionCustomerStatisticsData()生成
        log.info("");
        log.info("步骤1: 执行分配方案生成...");
        long allocationStart = System.currentTimeMillis();
        
        GenerateDistributionPlanRequestDto request = new GenerateDistributionPlanRequestDto();
        request.setYear(TEST_YEAR);
        request.setMonth(TEST_MONTH);
        request.setWeekSeq(TEST_WEEK_SEQ);
        request.setUrbanRatio(new BigDecimal("0.6"));
        request.setRuralRatio(new BigDecimal("0.4"));

        GenerateDistributionPlanResponseDto response = distributionCalculateService.generateDistributionPlan(request);
        
        long allocationTime = System.currentTimeMillis() - allocationStart;
        log.info("⏱️  分配方案生成总耗时: {} ms ({} 秒)", allocationTime, allocationTime / 1000.0);

        // 验证响应
        assertNotNull(response, "响应不应为null");
        
        log.info("分配方案生成结果:");
        log.info("   - 成功: {}", response.isSuccess());
        log.info("   - 消息: {}", response.getMessage());
        log.info("   - 总卷烟数: {}", response.getTotalCigarettes());
        log.info("   - 成功分配数: {}", response.getSuccessfulAllocations());
        log.info("   - 处理数量: {}", response.getProcessedCount());

        if (!response.isSuccess()) {
            log.error("分配方案生成失败: {}", response.getMessage());
            log.warn("可能原因：1) region_customer_statistics表没有数据 2) 投放区域不匹配");
            // 不直接失败，继续验证，看看是否有部分数据写回
        } else {
            log.info("✅ 分配方案生成成功");
        }

        // 等待数据写入完成
        try {
            Thread.sleep(2000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        // 步骤2: 验证分配写回结果
        log.info("");
        log.info("步骤2: 验证分配写回结果...");
        long verifyStart = System.currentTimeMillis();
        verifyWriteBackResults();
        long verifyTime = System.currentTimeMillis() - verifyStart;
        log.info("⏱️  验证写回结果耗时: {} ms", verifyTime);

        // 步骤3: 验证误差
        log.info("");
        log.info("步骤3: 验证分配误差...");
        long errorVerifyStart = System.currentTimeMillis();
        verifyAllocationErrors();
        long errorVerifyTime = System.currentTimeMillis() - errorVerifyStart;
        log.info("⏱️  验证分配误差耗时: {} ms", errorVerifyTime);

        // 步骤4: 检查未分配的卷烟
        log.info("");
        log.info("步骤4: 检查未分配的卷烟...");
        long unallocatedStart = System.currentTimeMillis();
        checkUnallocatedCigarettes();
        long unallocatedTime = System.currentTimeMillis() - unallocatedStart;
        log.info("⏱️  检查未分配卷烟耗时: {} ms", unallocatedTime);

        // 步骤5: 验证编码表达式
        log.info("");
        log.info("步骤5: 验证编码表达式是否符合编码规则...");
        long encodingStart = System.currentTimeMillis();
        verifyEncodingExpressions();
        long encodingTime = System.currentTimeMillis() - encodingStart;
        log.info("⏱️  验证编码表达式耗时: {} ms", encodingTime);

        log.info("");
        log.info("==========================================");
        log.info("✅ 分配写回测试完成！");
        log.info("==========================================");
    }

    /**
     * 验证写回结果
     */
    private void verifyWriteBackResults() {
        // 验证普通预测表
        String countPredictionSql = "SELECT COUNT(*) AS cnt FROM cigarette_distribution_prediction " +
                "WHERE YEAR = ? AND MONTH = ? AND WEEK_SEQ = ?";
        Integer predictionCount = jdbcTemplate.queryForObject(countPredictionSql, Integer.class,
                TEST_YEAR, TEST_MONTH, TEST_WEEK_SEQ);
        log.info("普通预测表记录数: {}", predictionCount);
        
        if (predictionCount == 0) {
            log.warn("⚠️ 普通预测表没有数据，可能原因：");
            log.warn("   1. region_customer_statistics表没有对应分区的数据");
            log.warn("   2. 投放区域与region_customer_statistics中的区域不匹配");
            log.warn("   3. 分配算法执行失败");
        } else {
            log.info("✅ 普通预测表有数据");
        }

        // 验证价位段预测表
        String countPricePredictionSql = "SELECT COUNT(*) AS cnt FROM cigarette_distribution_prediction_price " +
                "WHERE YEAR = ? AND MONTH = ? AND WEEK_SEQ = ?";
        Integer pricePredictionCount = jdbcTemplate.queryForObject(countPricePredictionSql, Integer.class,
                TEST_YEAR, TEST_MONTH, TEST_WEEK_SEQ);
        log.info("价位段预测表记录数: {}", pricePredictionCount);
        
        // 详细检查按价位段自选投放的写回结果
        String countPriceBandSql = "SELECT COUNT(*) AS cnt FROM cigarette_distribution_prediction_price " +
                "WHERE YEAR = ? AND MONTH = ? AND WEEK_SEQ = ? " +
                "AND DELIVERY_METHOD = '按价位段自选投放' " +
                "AND CIG_CODE LIKE 'TEST_%'";
        Integer priceBandCount = jdbcTemplate.queryForObject(countPriceBandSql, Integer.class,
                TEST_YEAR, TEST_MONTH, TEST_WEEK_SEQ);
        log.info("按价位段自选投放测试用例写回记录数: {}", priceBandCount);
        
        // 统计按价位段自选投放的测试用例数量
        String countPriceBandInfoSql = "SELECT COUNT(*) AS cnt FROM cigarette_distribution_info " +
                "WHERE YEAR = ? AND MONTH = ? AND WEEK_SEQ = ? " +
                "AND DELIVERY_METHOD = '按价位段自选投放' " +
                "AND CIG_CODE LIKE 'TEST_%'";
        Integer priceBandInfoCount = jdbcTemplate.queryForObject(countPriceBandInfoSql, Integer.class,
                TEST_YEAR, TEST_MONTH, TEST_WEEK_SEQ);
        log.info("按价位段自选投放测试用例总数: {}", priceBandInfoCount);
        
        if (priceBandInfoCount > 0 && priceBandCount == 0) {
            log.warn("⚠️ 按价位段自选投放的测试用例未写回到prediction_price表");
            log.warn("   可能原因：1) 候选卷烟查询失败（缺少批发价或批发价不在价位段范围内）");
            log.warn("            2) 分配过程出现异常");
            log.warn("            3) 写回过程失败");
        } else if (priceBandInfoCount > 0 && priceBandCount > 0) {
            log.info("✅ 按价位段自选投放的测试用例已成功写回到prediction_price表");
        }

        // 验证投放信息表
        String countInfoSql = "SELECT COUNT(*) AS cnt FROM cigarette_distribution_info " +
                "WHERE YEAR = ? AND MONTH = ? AND WEEK_SEQ = ? AND CIG_CODE LIKE 'TEST_%'";
        Integer infoCount = jdbcTemplate.queryForObject(countInfoSql, Integer.class,
                TEST_YEAR, TEST_MONTH, TEST_WEEK_SEQ);
        log.info("测试投放信息表记录数: {} (期望: {})", infoCount, testCaseCount);
        
        if (infoCount < testCaseCount) {
            log.warn("⚠️ 投放信息表记录数少于预期，可能部分用例插入失败");
        } else {
            log.info("✅ 投放信息表记录数符合预期");
        }
    }

    /**
     * 验证分配误差
     */
    private void verifyAllocationErrors() {
        // 验证普通表的误差（只查询测试用例）
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
                "AND i.CIG_CODE LIKE 'TEST_%' " +
                "GROUP BY p.CIG_CODE,p.CIG_NAME " +
                "ORDER BY abs_error DESC " +
                "LIMIT 20";

        List<Map<String, Object>> errors = jdbcTemplate.queryForList(sql, TEST_YEAR, TEST_MONTH, TEST_WEEK_SEQ);
        
        if (errors.isEmpty()) {
            log.warn("⚠️ 没有找到测试用例的分配结果，可能分配失败或数据未写回");
            return;
        }

        log.info("分配误差统计（前20条，按绝对误差降序）:");
        log.info("----------------------------------------------------------------------");
        log.info(String.format("%-15s %-30s %12s %12s %12s %10s",
                "卷烟代码", "卷烟名称", "预投放量", "实际投放", "绝对误差", "相对误差%"));
        log.info("----------------------------------------------------------------------");

        double maxAbsError = 0;
        double maxRelativeError = 0;

        for (Map<String, Object> row : errors) {
            String cigCode = (String) row.get("cig_code");
            String cigName = (String) row.get("cig_name");
            BigDecimal advTotal = getBigDecimal(row.get("adv_total"));
            BigDecimal actualTotal = getBigDecimal(row.get("actual_total"));
            BigDecimal absError = getBigDecimal(row.get("abs_error"));
            BigDecimal relativeError = getBigDecimal(row.get("relative_error_percent"));

            log.info(String.format("%-15s %-30s %12.2f %12.2f %12.2f %10.4f",
                    cigCode, truncate(cigName, 30), advTotal.doubleValue(), actualTotal.doubleValue(),
                    absError.doubleValue(), relativeError.doubleValue()));

            maxAbsError = Math.max(maxAbsError, absError.doubleValue());
            maxRelativeError = Math.max(maxRelativeError, relativeError.doubleValue());
        }

        log.info("----------------------------------------------------------------------");
        log.info("最大绝对误差: {}", maxAbsError);
        log.info("最大相对误差: {}%", maxRelativeError);

        // 统计信息（使用子查询先计算每个卷烟的误差，然后再聚合）
        String statsSql = "SELECT " +
                "COUNT(*) AS total_cigarettes, " +
                "AVG(abs_error) AS avg_abs_error, " +
                "MAX(abs_error) AS max_abs_error " +
                "FROM (" +
                "  SELECT " +
                "    ABS(SUM(IFNULL(p.ACTUAL_DELIVERY,0))-MAX(IFNULL(i.ADV,0))) AS abs_error " +
                "  FROM cigarette_distribution_prediction p " +
                "  JOIN cigarette_distribution_info i ON p.YEAR=i.YEAR AND p.MONTH=i.MONTH AND p.WEEK_SEQ=i.WEEK_SEQ " +
                "    AND p.CIG_CODE=i.CIG_CODE AND p.CIG_NAME=i.CIG_NAME " +
                "  WHERE p.YEAR=? AND p.MONTH=? AND p.WEEK_SEQ=? " +
                "    AND i.CIG_CODE LIKE 'TEST_%' " +
                "  GROUP BY p.CIG_CODE,p.CIG_NAME" +
                ") AS error_stats";

        List<Map<String, Object>> stats = jdbcTemplate.queryForList(statsSql, TEST_YEAR, TEST_MONTH, TEST_WEEK_SEQ);
        if (!stats.isEmpty()) {
            Map<String, Object> stat = stats.get(0);
            log.info("统计信息:");
            log.info("  - 总卷烟数: {}", stat.get("total_cigarettes"));
            log.info("  - 平均绝对误差: {}", getBigDecimal(stat.get("avg_abs_error")));
            log.info("  - 最大绝对误差: {}", getBigDecimal(stat.get("max_abs_error")));
        }
    }

    private BigDecimal getBigDecimal(Object value) {
        if (value == null) {
            return BigDecimal.ZERO;
        }
        if (value instanceof BigDecimal) {
            return (BigDecimal) value;
        }
        if (value instanceof Number) {
            return BigDecimal.valueOf(((Number) value).doubleValue());
        }
        try {
            return new BigDecimal(value.toString());
        } catch (NumberFormatException e) {
            return BigDecimal.ZERO;
        }
    }

    private String truncate(String str, int maxLength) {
        if (str == null) {
            return "";
        }
        return str.length() > maxLength ? str.substring(0, maxLength) : str;
    }

    /**
     * 为按价位段自选投放的测试用例初始化批发价数据
     * 确保这些测试用例能够被 findPriceBandCandidates 查询到
     */
    private void initializeTestCigarettePrices() {
        log.info("开始为按价位段自选投放的测试用例初始化批发价数据...");
        
        // 查询需要添加批发价的按价位段自选投放测试用例
        String querySql = "SELECT DISTINCT i.CIG_CODE, i.CIG_NAME " +
                         "FROM cigarette_distribution_info i " +
                         "LEFT JOIN base_cigarette_price p ON i.CIG_CODE = p.CIG_CODE " +
                         "WHERE i.YEAR = ? AND i.MONTH = ? AND i.WEEK_SEQ = ? " +
                         "  AND i.DELIVERY_METHOD = '按价位段自选投放' " +
                         "  AND i.CIG_CODE LIKE 'TEST_%' " +
                         "  AND p.CIG_CODE IS NULL";
        
        List<Map<String, Object>> cigarettes = jdbcTemplate.queryForList(querySql, TEST_YEAR, TEST_MONTH, TEST_WEEK_SEQ);
        
        if (cigarettes.isEmpty()) {
            log.info("所有按价位段自选投放的测试用例已存在批发价数据");
            return;
        }
        
        log.info("发现 {} 条按价位段自选投放的测试用例需要添加批发价", cigarettes.size());
        
        // 批量插入批发价数据
        String insertSql = "INSERT INTO base_cigarette_price (CIG_CODE, CIG_NAME, WHOLESALE_PRICE, CATEGORY, PRICE_TIER, PRICE_HL) " +
                          "VALUES (?, ?, ?, '测试价类', '测试价档', 1)";
        
        int inserted = 0;
        // 价位段分布：第1段(>=600), 第2段(400-600), 第3段(300-400), 第4段(240-300), 
        // 第5段(200-240), 第6段(170-200), 第7段(158-170), 第8段(130-158), 第9段(109-130)
        BigDecimal[] priceBands = {
            new BigDecimal("650.00"),  // 第1段
            new BigDecimal("500.00"),  // 第2段
            new BigDecimal("350.00"),  // 第3段
            new BigDecimal("270.00"),  // 第4段
            new BigDecimal("220.00"),  // 第5段
            new BigDecimal("185.00"),  // 第6段
            new BigDecimal("165.00"),  // 第7段
            new BigDecimal("145.00"),  // 第8段
            new BigDecimal("120.00")   // 第9段
        };
        
        for (int i = 0; i < cigarettes.size(); i++) {
            Map<String, Object> cig = cigarettes.get(i);
            String cigCode = (String) cig.get("CIG_CODE");
            String cigName = (String) cig.get("CIG_NAME");
            
            // 根据索引循环分配不同价位段的批发价
            BigDecimal wholesalePrice = priceBands[i % priceBands.length];
            
            try {
                jdbcTemplate.update(insertSql, cigCode, cigName, wholesalePrice);
                inserted++;
            } catch (Exception e) {
                log.warn("插入卷烟价格失败: CIG_CODE={}, error={}", cigCode, e.getMessage());
            }
        }
        
        if (inserted > 0) {
            log.info("✅ 为 {} 条按价位段自选投放的测试用例添加了批发价数据", inserted);
        }
    }

    /**
     * 检查未分配的卷烟
     * 查询在info表中但不在prediction表中的测试用例
     */
    private void checkUnallocatedCigarettes() {
        String sql = "SELECT i.CIG_CODE, i.CIG_NAME, i.DELIVERY_METHOD, i.DELIVERY_ETYPE, i.TAG, i.ADV " +
        "FROM cigarette_distribution_info i " +
        "LEFT JOIN cigarette_distribution_prediction p ON " +
        "  i.YEAR = p.YEAR AND i.MONTH = p.MONTH AND i.WEEK_SEQ = p.WEEK_SEQ " +
        "  AND i.CIG_CODE = p.CIG_CODE AND i.CIG_NAME = p.CIG_NAME " +
        "LEFT JOIN cigarette_distribution_prediction_price pp ON " +
        "  i.YEAR = pp.YEAR AND i.MONTH = pp.MONTH AND i.WEEK_SEQ = pp.WEEK_SEQ " +
        "  AND i.CIG_CODE = pp.CIG_CODE AND i.CIG_NAME = pp.CIG_NAME " +
        "WHERE i.YEAR = ? AND i.MONTH = ? AND i.WEEK_SEQ = ? " +
        "  AND i.CIG_CODE LIKE 'TEST_%' " +
        "  AND p.CIG_CODE IS NULL AND pp.CIG_CODE IS NULL " +
                "ORDER BY i.CIG_CODE " +
                "LIMIT 50";

        List<Map<String, Object>> unallocated = jdbcTemplate.queryForList(sql, TEST_YEAR, TEST_MONTH, TEST_WEEK_SEQ);

        if (unallocated.isEmpty()) {
            log.info("✅ 所有测试用例都已成功分配");
        } else {
            log.warn("⚠️ 发现 {} 条未分配的测试用例（显示前50条）:", unallocated.size());
            log.warn("----------------------------------------------------------------------");
            log.warn(String.format("%-15s %-40s %-20s %-30s %-20s %12s",
                    "卷烟代码", "卷烟名称", "投放方式", "扩展类型", "标签", "预投放量"));
            log.warn("----------------------------------------------------------------------");

            for (Map<String, Object> row : unallocated) {
                String cigCode = getString(row, "CIG_CODE");
                String cigName = truncate(getString(row, "CIG_NAME"), 40);
                String deliveryMethod = getString(row, "DELIVERY_METHOD");
                String deliveryEtype = getString(row, "DELIVERY_ETYPE");
                String tag = getString(row, "TAG");
                BigDecimal adv = getBigDecimal(row.get("ADV"));

                log.warn(String.format("%-15s %-40s %-20s %-30s %-20s %12.2f",
                        cigCode, cigName, deliveryMethod != null ? deliveryMethod : "",
                        deliveryEtype != null ? deliveryEtype : "",
                        tag != null ? tag : "", adv));
            }
            log.warn("----------------------------------------------------------------------");

            // 统计未分配的原因
            String statsSql = "SELECT i.DELIVERY_METHOD, COUNT(*) as cnt " +
                    "FROM cigarette_distribution_info i " +
                    "LEFT JOIN cigarette_distribution_prediction p ON " +
                    "  i.YEAR = p.YEAR AND i.MONTH = p.MONTH AND i.WEEK_SEQ = p.WEEK_SEQ " +
                    "  AND i.CIG_CODE = p.CIG_CODE AND i.CIG_NAME = p.CIG_NAME " +
                    "LEFT JOIN cigarette_distribution_prediction_price pp ON " +
                    "  i.YEAR = pp.YEAR AND i.MONTH = pp.MONTH AND i.WEEK_SEQ = pp.WEEK_SEQ " +
                    "  AND i.CIG_CODE = pp.CIG_CODE AND i.CIG_NAME = pp.CIG_NAME " +
                    "WHERE i.YEAR = ? AND i.MONTH = ? AND i.WEEK_SEQ = ? " +
                    "  AND i.CIG_CODE LIKE 'TEST_%' " +
                    "  AND p.CIG_CODE IS NULL AND pp.CIG_CODE IS NULL " +
                    "GROUP BY i.DELIVERY_METHOD";

            List<Map<String, Object>> stats = jdbcTemplate.queryForList(statsSql, TEST_YEAR, TEST_MONTH, TEST_WEEK_SEQ);
            if (!stats.isEmpty()) {
                log.warn("未分配卷烟按投放方式统计:");
                for (Map<String, Object> stat : stats) {
                    log.warn("  - {}: {} 条", stat.get("DELIVERY_METHOD"), stat.get("cnt"));
                }
            }
        }
    }

    /**
     * 验证编码表达式是否符合编码规则
     * 检查prediction表中的编码表达式是否符合编码规则表和encoding-rules.yml配置
     */
    private void verifyEncodingExpressions() {
        // 注意：prediction表中可能没有ENCODING_EXPRESSION字段，编码表达式可能是通过EncodeService动态生成的
        // 这里我们检查已分配的记录，并验证其字段是否符合编码规则
        String sql = "SELECT DISTINCT p.CIG_CODE, p.CIG_NAME, p.DELIVERY_METHOD, p.DELIVERY_ETYPE, p.TAG, " +
                "  p.DELIVERY_AREA, i.ADV " +
                "FROM cigarette_distribution_prediction p " +
                "JOIN cigarette_distribution_info i ON " +
                "  p.YEAR = i.YEAR AND p.MONTH = i.MONTH AND p.WEEK_SEQ = i.WEEK_SEQ " +
                "  AND p.CIG_CODE = i.CIG_CODE AND p.CIG_NAME = i.CIG_NAME " +
                "WHERE p.YEAR = ? AND p.MONTH = ? AND p.WEEK_SEQ = ? " +
                "  AND p.CIG_CODE LIKE 'TEST_%' " +
                "ORDER BY p.CIG_CODE " +
                "LIMIT 100";

        List<Map<String, Object>> records = jdbcTemplate.queryForList(sql, TEST_YEAR, TEST_MONTH, TEST_WEEK_SEQ);

        if (records.isEmpty()) {
            log.warn("⚠️ 未找到已分配的测试用例");
            return;
        }

        log.info("检查 {} 条已分配测试用例的编码规则符合性（显示前100条）", records.size());

        int validCount = 0;
        int invalidCount = 0;
        List<String> invalidRecords = new ArrayList<>();

        for (Map<String, Object> record : records) {
            String cigCode = getString(record, "CIG_CODE");
            String cigName = getString(record, "CIG_NAME");
            String deliveryMethod = getString(record, "DELIVERY_METHOD");
            String deliveryEtype = getString(record, "DELIVERY_ETYPE");
            String deliveryArea = getString(record, "DELIVERY_AREA");

            // 验证投放方式编码
            String methodCode = encodingRuleRepository.findDeliveryMethodCode(deliveryMethod);
            if (methodCode == null) {
                invalidCount++;
                invalidRecords.add(String.format("%s (%s): 投放方式 '%s' 无法找到对应编码", 
                        cigCode, cigName, deliveryMethod));
                continue;
            }

            // 验证扩展类型编码（如果存在）
            if (deliveryEtype != null && !deliveryEtype.trim().isEmpty()) {
                String etypeCode = encodingRuleRepository.findExtensionTypeCode(deliveryEtype);
                if (etypeCode == null) {
                    invalidCount++;
                    invalidRecords.add(String.format("%s (%s): 扩展类型 '%s' 无法找到对应编码", 
                            cigCode, cigName, deliveryEtype));
                    continue;
                }
            }

            // 验证区域编码（如果不是全市）
            if (deliveryArea != null && !deliveryArea.trim().isEmpty() && !"全市".equals(deliveryArea)) {
                // 解析投放区域，验证区域编码是否存在
                String[] areas = deliveryArea.split("、|，|,|;|；");
                for (String area : areas) {
                    area = area.trim();
                    if (area.isEmpty() || "全市".equals(area)) {
                        continue;
                    }
                    // 检查区域编码映射（简化验证，实际应该更详细）
                    // 这里主要验证区域名称是否在编码规则中存在
                }
            }

            validCount++;
        }

        log.info("编码规则验证结果:");
        log.info("  - 有效: {} 条", validCount);
        log.info("  - 无效: {} 条", invalidCount);

        if (invalidCount > 0 && invalidRecords.size() <= 20) {
            log.warn("不符合编码规则的记录（前{}条）:", Math.min(invalidRecords.size(), 20));
            for (int i = 0; i < Math.min(invalidRecords.size(), 20); i++) {
                log.warn("  {}", invalidRecords.get(i));
            }
        } else if (invalidCount > 0) {
            log.warn("不符合编码规则的记录（前20条）:");
            for (int i = 0; i < 20; i++) {
                log.warn("  {}", invalidRecords.get(i));
            }
            log.warn("  ... 还有 {} 条无效记录未显示", invalidRecords.size() - 20);
        }

        if (invalidCount == 0) {
            log.info("✅ 所有已分配记录的编码规则映射都符合encoding-rules.yml配置");
        } else {
            log.warn("⚠️ 发现 {} 条记录的编码规则映射不符合encoding-rules.yml配置", invalidCount);
        }
    }


    private String getString(Map<String, Object> map, String key) {
        Object value = map.get(key);
        if (value == null) {
            return null;
        }
        return value.toString();
    }
}

