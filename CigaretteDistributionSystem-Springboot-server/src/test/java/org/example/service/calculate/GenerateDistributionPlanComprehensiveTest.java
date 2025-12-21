package org.example.service.calculate;

import lombok.extern.slf4j.Slf4j;
import org.example.application.dto.GenerateDistributionPlanRequestDto;
import org.example.application.dto.GenerateDistributionPlanResponseDto;
import org.example.application.service.calculate.DistributionCalculateService;
import org.example.domain.repository.RegionCustomerStatisticsRepository;
import org.example.domain.repository.CigaretteDistributionInfoRepository;
import org.example.infrastructure.persistence.po.CigaretteDistributionInfoPO;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 一键生成分配方案综合测试（高覆盖率版本）。
 * <p>
 * 测试策略：
 * 1. 预投放量分层：按1000为阶层递增，每个阶层随机生成，最终达到150000级别
 * 2. 组合覆盖：使用Pairwise组合测试设计，覆盖投放类型+扩展类型+标签的多种组合
 * 3. 区域动态选择：小投放量选择1-2个高客户数区域，大投放量选择接近全部区域
 * 4. 覆盖率目标：95%以上
 * </p>
 *
 * @author Robin
 * @since 2025-12-20
 */
@Slf4j
@SpringBootTest
@ActiveProfiles("test")
@DisplayName("一键生成分配方案综合测试")
public class GenerateDistributionPlanComprehensiveTest {

    @Autowired
    private DistributionCalculateService distributionCalculateService;

    @Autowired
    private RegionCustomerStatisticsRepository regionCustomerStatisticsRepository;

    @Autowired
    private CigaretteDistributionInfoRepository cigaretteDistributionInfoRepository;

    private static final int TEST_YEAR = 2025;
    private static final int TEST_MONTH = 9;
    private static final int TEST_WEEK_SEQ = 3;

    private List<String> availableRegions;

    @BeforeEach
    void setUp() {
        loadAvailableRegions();
    }

    @AfterEach
    void tearDown() {
        log.info("------------------------------------------------------------");
    }

    /**
     * 从数据库加载可用区域列表，并按客户总数降序排序
     */
    private void loadAvailableRegions() {
        try {
            List<Map<String, Object>> allStats = regionCustomerStatisticsRepository.findAll(
                    TEST_YEAR, TEST_MONTH, TEST_WEEK_SEQ);
            if (allStats == null || allStats.isEmpty()) {
                log.warn("未找到区域客户统计数据，使用默认区域列表");
                availableRegions = Arrays.asList("城区", "丹江", "房县", "郧阳", "竹山", "郧西", "竹溪");
                return;
            }

            // 按TOTAL降序排序，优先选择客户数高的区域
            availableRegions = allStats.stream()
                    .sorted((a, b) -> {
                        BigDecimal totalA = getBigDecimal(a.get("TOTAL"));
                        BigDecimal totalB = getBigDecimal(b.get("TOTAL"));
                        return totalB.compareTo(totalA);
                    })
                    .map(row -> (String) row.get("REGION"))
                    .filter(Objects::nonNull)
                    .collect(Collectors.toList());

            log.info("加载了 {} 个可用区域，前5个: {}", availableRegions.size(),
                    availableRegions.stream().limit(5).collect(Collectors.toList()));
        } catch (Exception e) {
            log.error("加载区域列表失败", e);
            availableRegions = Arrays.asList("城区", "丹江", "房县", "郧阳", "竹山", "郧西", "竹溪");
        }
    }

    /**
     * 综合分配方案生成测试（参数化）
     * 
     * 注意：此测试需要预先准备测试数据（cigarette_distribution_info表），
     * 实际执行时会使用数据库中已有的数据，而不是根据参数创建新数据。
     * 如需完全控制测试数据，请使用 prepareTestData 方法创建数据后再执行。
     */
    @ParameterizedTest(name = "用例#{index}: adv={0}, method={1}, etype={2}, tag={3}, regions={4}")
    @MethodSource("generateTestCases")
    @DisplayName("综合分配方案生成测试")
    @Transactional
    void testGenerateDistributionPlan(
            BigDecimal adv,
            String deliveryMethod,
            String deliveryEtype,
            String tag,
            int regionCount) {

        // 准备测试数据：创建一条测试卷烟记录
        String cigCode = "TEST" + System.currentTimeMillis() + "_" + Math.abs(adv.hashCode());
        String cigName = "测试卷烟_" + deliveryMethod;
        
        prepareTestData(cigCode, cigName, adv, deliveryMethod, deliveryEtype, tag, regionCount);

        // 执行分配
        GenerateDistributionPlanRequestDto request = new GenerateDistributionPlanRequestDto();
        request.setYear(TEST_YEAR);
        request.setMonth(TEST_MONTH);
        request.setWeekSeq(TEST_WEEK_SEQ);
        request.setUrbanRatio(new BigDecimal("0.6"));
        request.setRuralRatio(new BigDecimal("0.4"));

        GenerateDistributionPlanResponseDto response = distributionCalculateService.generateDistributionPlan(request);

        // 验证结果
        assertNotNull(response, "响应不应为null");
        assertTrue(response.isSuccess(), 
                String.format("分配应成功: cigCode=%s, adv=%s, method=%s, etype=%s, regions=%d", 
                        cigCode, adv, deliveryMethod, deliveryEtype, regionCount));

        log.info("✓ 测试通过: cigCode={}, adv={}, method={}, etype={}, tag={}, regions={}, processed={}",
                cigCode, adv, deliveryMethod, deliveryEtype, tag, regionCount, response.getProcessedCount());
    }

    /**
     * 准备测试数据：插入一条卷烟投放信息记录
     */
    private void prepareTestData(String cigCode, String cigName, BigDecimal adv,
                                String deliveryMethod, String deliveryEtype, String tag, int regionCount) {
        List<String> selectedRegions = selectRegionsByAdvAndCount(adv, regionCount);
        String deliveryArea = String.join(",", selectedRegions);

        CigaretteDistributionInfoPO info = new CigaretteDistributionInfoPO();
        info.setCigCode(cigCode);
        info.setCigName(cigName);
        info.setYear(TEST_YEAR);
        info.setMonth(TEST_MONTH);
        info.setWeekSeq(TEST_WEEK_SEQ);
        info.setAdv(adv);
        info.setDeliveryMethod(deliveryMethod);
        info.setDeliveryEtype(deliveryEtype);
        info.setDeliveryArea(deliveryArea);
        info.setTag(tag);
        info.setSupplyAttribute("正常"); // 默认值
        info.setUrs(BigDecimal.ZERO); // 默认值

        // 使用batchUpsert插入数据
        try {
            cigaretteDistributionInfoRepository.batchUpsert(Collections.singletonList(info));
            log.debug("已创建测试数据: cigCode={}, adv={}, method={}, etype={}, regions={}",
                    cigCode, adv, deliveryMethod, deliveryEtype, regionCount);
        } catch (Exception e) {
            log.warn("创建测试数据失败（可能已存在）: {}", e.getMessage());
        }
    }

    /**
     * 根据预投放量和区域数量选择区域
     * 策略：
     * - 小投放量（<5000）：选择1-2个客户数最高的区域
     * - 中投放量（5000-50000）：选择30%-50%的区域
     * - 大投放量（>50000）：选择80%-100%的区域（可能排除1个最小区域）
     */
    private List<String> selectRegionsByAdvAndCount(BigDecimal adv, int targetCount) {
        if (availableRegions == null || availableRegions.isEmpty()) {
            return Collections.singletonList("城区");
        }

        int actualCount = Math.min(targetCount, availableRegions.size());
        if (actualCount <= 0) {
            actualCount = 1;
        }

        // 始终从客户数高的区域开始选择
        return availableRegions.stream()
                .limit(actualCount)
                .collect(Collectors.toList());
    }

    /**
     * 生成测试用例流
     * 使用TestCaseGenerator生成所有测试用例
     */
    static Stream<org.junit.jupiter.params.provider.Arguments> generateTestCases() {
        // 使用默认7个区域（实际测试时会动态加载）
        List<TestCaseGenerator.TestCaseConfig> configs = TestCaseGenerator.generateAllTestCases(7, 42);
        
        return configs.stream().map(config -> 
            org.junit.jupiter.params.provider.Arguments.of(
                config.adv,
                config.deliveryMethod,
                config.deliveryEtype,
                config.tag,
                config.regionCount
            )
        );
    }

    private static BigDecimal getBigDecimal(Object value) {
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

