package org.example.service;

import lombok.extern.slf4j.Slf4j;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import org.example.domain.repository.CigaretteDistributionInfoRepository;
import org.example.infrastructure.persistence.po.CigaretteDistributionInfoPO;
import org.example.service.calculate.TestCaseGenerator;
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
 * 使用 TestCaseGenerator 生成测试用例并插入 info 表，分析覆盖情况。
 *
 * @author Robin
 * @since 2025-12-20
 */
@Slf4j
@SpringBootTest
@ActiveProfiles("test")
@DisplayName("TestCaseGenerator 测试用例生成与分析")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class TestCaseInsertAndAnalysisTest {

    @Autowired
    private TestCaseGenerator testCaseGenerator;

    @Autowired
    private CigaretteDistributionInfoRepository cigaretteDistributionInfoRepository;

    @Autowired
    private PartitionTableManager partitionTableManager;

    private static final int YEAR = 2099;
    private static final int MONTH = 9;
    private static final int WEEK_SEQ = 1;

    // 从 base_cigarette_price 表获取的真实卷烟代码（用于循环分配）
    private static final List<String[]> CIGARETTE_DATA = Arrays.asList(
            new String[]{"42020181", "黄鹤楼(1916中支)"},
            new String[]{"42020035", "黄鹤楼(硬1916)"},
            new String[]{"42020129", "黄鹤楼(软1916)"},
            new String[]{"42020135", "黄鹤楼(硬15细支)"},
            new String[]{"42020081", "黄鹤楼(硬15)"},
            new String[]{"42020157", "黄鹤楼(硬1916如意)"},
            new String[]{"42020088", "黄鹤楼(硬平安)"},
            new String[]{"42020158", "黄鹤楼(硬1916红爆)"},
            new String[]{"42020012", "黄鹤楼(软珍品)"},
            new String[]{"42020149", "黄鹤楼(珍品细支)"},
            new String[]{"42020013", "黄鹤楼(硬珍品)"},
            new String[]{"42020117", "黄鹤楼(硬峡谷柔情)"},
            new String[]{"42020121", "黄鹤楼(硬峡谷情细支)"},
            new String[]{"42020141", "黄鹤楼(硬奇景)"},
            new String[]{"42020180", "黄鹤楼(硬峡谷情)"},
            new String[]{"35300080", "七匹狼(锋芒)"},
            new String[]{"42010114", "红金龙(硬爱你爆珠)"},
            new String[]{"43010054", "白沙(和天下)"},
            new String[]{"42020129", "黄鹤楼(视窗)"},
            new String[]{"42020100", "黄鹤楼(硬雅香金)"}
    );

    private List<TestCaseGenerator.TestCaseConfig> generatedCases;

    @BeforeEach
    void setUp() {
        partitionTableManager.ensurePartitionExists("cigarette_distribution_info", YEAR, MONTH, WEEK_SEQ);
    }

    @Test
    @Order(1)
    @DisplayName("步骤1: 生成并插入测试用例到 info 表")
    void step1_generateAndInsertTestCases() {
        log.info("========================================");
        log.info("步骤1: 生成并插入测试用例到 info 表");
        log.info("========================================");

        // 使用 TestCaseGenerator 生成所有测试用例
        generatedCases = testCaseGenerator.generateAllTestCasesInternal(14, 42);
        log.info("TestCaseGenerator 生成了 {} 个测试用例", generatedCases.size());

        // 创建要插入的 PO 列表
        List<CigaretteDistributionInfoPO> infoList = new ArrayList<>();
        int cigIndex = 0;

        for (TestCaseGenerator.TestCaseConfig config : generatedCases) {
            // 循环使用真实卷烟数据
            String[] cigData = CIGARETTE_DATA.get(cigIndex % CIGARETTE_DATA.size());
            cigIndex++;

            // 根据区域数量构建投放区域字符串
            String deliveryArea = buildDeliveryArea(config);

            CigaretteDistributionInfoPO info = new CigaretteDistributionInfoPO();
            info.setYear(YEAR);
            info.setMonth(MONTH);
            info.setWeekSeq(WEEK_SEQ);
            info.setCigCode(cigData[0] + "_" + cigIndex); // 添加索引确保唯一
            info.setCigName(cigData[1] + "_" + config.deliveryMethod.substring(0, 2));
            info.setAdv(config.adv);
            info.setDeliveryMethod(config.deliveryMethod);
            info.setDeliveryEtype(config.deliveryEtype);
            info.setDeliveryArea(deliveryArea);
            info.setTag(config.tag);
            info.setTagFilterConfig(config.tag != null ? "0" : null);
            info.setSupplyAttribute("正常");
            info.setUrs(BigDecimal.ZERO);
            info.setBz("TestCaseGenerator生成 - " + config.deliveryMethod);

            infoList.add(info);
        }

        // 批量插入（使用 UPSERT，如果记录已存在则更新）
        log.info("开始批量 UPSERT {} 条测试用例...", infoList.size());
        
        // 先查询数据库中已有的记录数（用于判断是插入还是更新）
        QueryWrapper<CigaretteDistributionInfoPO> countQuery = new QueryWrapper<>();
        countQuery.eq("YEAR", YEAR).eq("MONTH", MONTH).eq("WEEK_SEQ", WEEK_SEQ);
        List<Map<String, Object>> existingRecords = cigaretteDistributionInfoRepository.selectMaps(countQuery);
        int existingCount = existingRecords != null ? existingRecords.size() : 0;
        log.info("数据库中已有记录数: {}", existingCount);
        
        int affectedRows = cigaretteDistributionInfoRepository.batchUpsert(infoList);
        log.info("UPSERT 完成，影响行数: {} (说明：MySQL ON DUPLICATE KEY UPDATE 返回值为：新插入=1，更新=2)", affectedRows);
        
        // 查询插入后的记录数
        List<Map<String, Object>> afterRecords = cigaretteDistributionInfoRepository.selectMaps(countQuery);
        int finalCount = afterRecords != null ? afterRecords.size() : 0;
        log.info("插入后数据库记录数: {} (新增: {} 条)", finalCount, finalCount - existingCount);
        
        // 验证：最终记录数应该等于生成的测试用例数
        assertEquals(infoList.size(), finalCount, 
                String.format("最终记录数应该等于生成的测试用例数。期望: %d, 实际: %d", infoList.size(), finalCount));

        assertTrue(affectedRows > 0, "UPSERT 应该影响至少一行");
    }

    @Test
    @Order(2)
    @DisplayName("步骤2: 分析投放组合覆盖情况")
    void step2_analyzeDeliveryCombinationCoverage() {
        log.info("========================================");
        log.info("步骤2: 分析投放组合覆盖情况");
        log.info("========================================");

        // 重新生成用例用于分析（确保数据一致）
        generatedCases = testCaseGenerator.generateAllTestCasesInternal(14, 42);

        // 1. 按投放方式分组统计
        Map<String, Long> byMethod = generatedCases.stream()
                .collect(Collectors.groupingBy(c -> c.deliveryMethod, Collectors.counting()));
        
        log.info("\n【按投放方式分布】");
        log.info("┌─────────────────────────┬──────────┬─────────┐");
        log.info("│ 投放方式                │ 用例数   │ 占比    │");
        log.info("├─────────────────────────┼──────────┼─────────┤");
        long total = generatedCases.size();
        for (Map.Entry<String, Long> entry : byMethod.entrySet()) {
            double percentage = (entry.getValue() * 100.0) / total;
            String method = entry.getKey();
            String padding = repeat(" ", Math.max(0, 20 - method.length()));
            log.info("│ {}{} │ {:>8} │ {:>6.1f}% │", 
                    method, padding, entry.getValue(), String.format("%.1f", percentage));
        }
        log.info("├─────────────────────────┼──────────┼─────────┤");
        log.info("│ 合计                    │ {:>8} │ 100.0%  │", total);
        log.info("└─────────────────────────┴──────────┴─────────┘");

        // 2. 按扩展类型分组统计
        Map<String, Long> byEtype = generatedCases.stream()
                .collect(Collectors.groupingBy(
                        c -> c.deliveryEtype != null ? c.deliveryEtype : "无扩展类型",
                        Collectors.counting()));
        
        log.info("\n【按扩展类型分布】");
        log.info("┌────────────────────────────────────┬──────────┐");
        log.info("│ 扩展类型                           │ 用例数   │");
        log.info("├────────────────────────────────────┼──────────┤");
        byEtype.entrySet().stream()
                .sorted((a, b) -> b.getValue().compareTo(a.getValue()))
                .forEach(entry -> {
                    String etype = entry.getKey();
                    String padding = repeat(" ", Math.max(0, 35 - etype.length()));
                    log.info("│ {}{} │ {:>8} │", 
                            etype, padding, entry.getValue());
                });
        log.info("└────────────────────────────────────┴──────────┘");

        // 3. 按标签分组统计
        Map<String, Long> byTag = generatedCases.stream()
                .collect(Collectors.groupingBy(
                        c -> c.tag != null ? c.tag : "无标签",
                        Collectors.counting()));
        
        log.info("\n【按标签分布】");
        log.info("┌───────────────────────┬──────────┬─────────┐");
        log.info("│ 标签                  │ 用例数   │ 占比    │");
        log.info("├───────────────────────┼──────────┼─────────┤");
        for (Map.Entry<String, Long> entry : byTag.entrySet()) {
            double percentage = (entry.getValue() * 100.0) / total;
            String tag = entry.getKey();
            String padding = repeat(" ", Math.max(0, 18 - tag.length()));
            log.info("│ {}{} │ {:>8} │ {:>6.1f}% │", 
                    tag, padding, entry.getValue(), String.format("%.1f", percentage));
        }
        log.info("└───────────────────────┴──────────┴─────────┘");

        // 4. 投放方式+扩展类型+标签的完整组合统计
        Map<String, Long> byFullCombination = generatedCases.stream()
                .collect(Collectors.groupingBy(
                        c -> String.format("%s | %s | %s",
                                c.deliveryMethod,
                                c.deliveryEtype != null ? c.deliveryEtype : "无",
                                c.tag != null ? c.tag : "无"),
                        Collectors.counting()));
        
        log.info("\n【完整投放组合覆盖（投放方式+扩展类型+标签）】");
        log.info("共覆盖 {} 种不同的投放组合", byFullCombination.size());
        log.info("前10个组合:");
        byFullCombination.entrySet().stream()
                .sorted((a, b) -> b.getValue().compareTo(a.getValue()))
                .limit(10)
                .forEach(entry -> log.info("  {} : {} 个用例", entry.getKey(), entry.getValue()));

        // 5. 详细扩展类型覆盖分析
        log.info("\n【扩展类型详细覆盖分析】");
        
        // 单扩展类型覆盖
        Set<String> singleExtensions = generatedCases.stream()
                .filter(c -> c.deliveryEtype != null && c.deliveryEtype.split("\\+").length == 2)
                .map(c -> c.deliveryEtype)
                .collect(Collectors.toSet());
        log.info("单扩展类型覆盖: {} 种", singleExtensions.size());
        singleExtensions.stream().sorted().forEach(etype -> {
            long count = generatedCases.stream()
                    .filter(c -> etype.equals(c.deliveryEtype))
                    .count();
            log.info("  - {}: {} 个用例", etype, count);
        });

        // 双扩展类型覆盖
        Set<String> dualExtensions = generatedCases.stream()
                .filter(c -> c.deliveryEtype != null && c.deliveryEtype.split("\\+").length == 3)
                .map(c -> c.deliveryEtype)
                .collect(Collectors.toSet());
        log.info("双扩展类型覆盖: {} 种", dualExtensions.size());
        dualExtensions.stream().sorted().forEach(etype -> {
            long count = generatedCases.stream()
                    .filter(c -> etype.equals(c.deliveryEtype))
                    .count();
            log.info("  - {}: {} 个用例", etype, count);
        });

        // 6. 验证数据库中的实际数据
        log.info("\n【验证数据库实际插入数据】");
        QueryWrapper<CigaretteDistributionInfoPO> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("YEAR", YEAR)
                .eq("MONTH", MONTH)
                .eq("WEEK_SEQ", WEEK_SEQ);
        List<Map<String, Object>> dbData = cigaretteDistributionInfoRepository.selectMaps(queryWrapper);
        log.info("数据库中实际记录数: {}", dbData.size());
        log.info("生成的测试用例数: {}", generatedCases.size());
        
        if (dbData.size() > 0) {
            // 统计数据库中的投放方式分布
            Map<String, Long> dbByMethod = dbData.stream()
                    .collect(Collectors.groupingBy(
                            row -> String.valueOf(row.get("DELIVERY_METHOD")),
                            Collectors.counting()));
            log.info("数据库中的投放方式分布:");
            dbByMethod.forEach((method, count) -> 
                    log.info("  {}: {} 条", method, count));
        }
    }

    @Test
    @Order(3)
    @DisplayName("步骤3: 分析预投放量覆盖情况")
    void step3_analyzeAdvCoverage() {
        log.info("========================================");
        log.info("步骤3: 分析预投放量覆盖情况");
        log.info("========================================");

        // 重新生成用例用于分析
        generatedCases = testCaseGenerator.generateAllTestCasesInternal(14, 42);

        // 1. 按预投放量阶层分组统计
        Map<String, Long> byAdvRange = generatedCases.stream()
                .collect(Collectors.groupingBy(this::getAdvRange, Collectors.counting()));
        
        log.info("\n【按预投放量阶层分布】");
        log.info("┌─────────────────────┬──────────┬─────────┬──────────────────────────┐");
        log.info("│ 阶层                │ 用例数   │ 占比    │ 分布条形图               │");
        log.info("├─────────────────────┼──────────┼─────────┼──────────────────────────┤");
        
        String[] orderedRanges = {"0-1K", "1K-2K", "2K-5K", "5K-10K", "10K-20K", "20K-50K", "50K-100K", "100K-150K"};
        long total = generatedCases.size();
        long maxCount = byAdvRange.values().stream().mapToLong(Long::longValue).max().orElse(1);
        
        for (String range : orderedRanges) {
            long count = byAdvRange.getOrDefault(range, 0L);
            double percentage = (count * 100.0) / total;
            int barLength = (int) ((count * 20) / maxCount);
            String bar = repeat("█", barLength) + repeat("░", 20 - barLength);
            String padding = repeat(" ", Math.max(0, 16 - range.length()));
            log.info("│ {}{} │ {:>8} │ {:>6.1f}% │ {} │", 
                    range, padding, count, String.format("%.1f", percentage), bar);
        }
        log.info("└─────────────────────┴──────────┴─────────┴──────────────────────────┘");

        // 2. 统计预投放量的基本统计指标
        DoubleSummaryStatistics advStats = generatedCases.stream()
                .mapToDouble(c -> c.adv.doubleValue())
                .summaryStatistics();
        
        log.info("\n【预投放量统计指标】");
        log.info("┌─────────────────────┬────────────────────┐");
        log.info("│ 指标                │ 值                 │");
        log.info("├─────────────────────┼────────────────────┤");
        log.info("│ 最小值              │ {:>18.2f} │", advStats.getMin());
        log.info("│ 最大值              │ {:>18.2f} │", advStats.getMax());
        log.info("│ 平均值              │ {:>18.2f} │", advStats.getAverage());
        log.info("│ 用例总数            │ {:>18} │", advStats.getCount());
        log.info("└─────────────────────┴────────────────────┘");

        // 3. 按投放方式分析预投放量分布
        log.info("\n【各投放方式的预投放量分布】");
        Map<String, List<TestCaseGenerator.TestCaseConfig>> byMethod = generatedCases.stream()
                .collect(Collectors.groupingBy(c -> c.deliveryMethod));
        
        for (Map.Entry<String, List<TestCaseGenerator.TestCaseConfig>> entry : byMethod.entrySet()) {
            DoubleSummaryStatistics methodStats = entry.getValue().stream()
                    .mapToDouble(c -> c.adv.doubleValue())
                    .summaryStatistics();
            log.info("  {} ({} 个用例):", entry.getKey(), methodStats.getCount());
            log.info("    - 最小: {}, 最大: {}, 平均: {}",
                    String.format("%.2f", methodStats.getMin()), 
                    String.format("%.2f", methodStats.getMax()), 
                    String.format("%.2f", methodStats.getAverage()));
        }

        // 4. 验证预投放量阶层覆盖完整性
        log.info("\n【预投放量阶层覆盖验证】");
        Set<String> coveredRanges = byAdvRange.keySet();
        String[] expectedRanges = {"0-1K", "1K-2K", "2K-5K", "5K-10K", "10K-20K", "20K-50K", "50K-100K", "100K-150K"};
        for (String expected : expectedRanges) {
            boolean covered = coveredRanges.contains(expected);
            log.info("  {}: {}", expected, covered ? "✓ 已覆盖" : "✗ 未覆盖");
        }

        // 5. 边界值验证
        log.info("\n【边界值覆盖验证】");
        BigDecimal[] boundaries = {
                BigDecimal.ONE,
                BigDecimal.valueOf(999),
                BigDecimal.valueOf(1000),
                BigDecimal.valueOf(1001),
                BigDecimal.valueOf(99999),
                BigDecimal.valueOf(100000),
                BigDecimal.valueOf(150000)
        };
        for (BigDecimal boundary : boundaries) {
            boolean exists = generatedCases.stream()
                    .anyMatch(c -> c.adv.compareTo(boundary) == 0);
            log.info("  边界值 {}: {}", boundary, exists ? "✓ 已覆盖" : "✗ 未覆盖");
        }
    }

    @Test
    @Order(4)
    @DisplayName("步骤4: 分析区域覆盖情况")
    void step4_analyzeRegionCoverage() {
        log.info("========================================");
        log.info("步骤4: 分析区域覆盖情况");
        log.info("========================================");

        // 重新生成用例用于分析
        generatedCases = testCaseGenerator.generateAllTestCasesInternal(14, 42);

        // 按区域数量分组统计
        Map<Integer, Long> byRegionCount = generatedCases.stream()
                .collect(Collectors.groupingBy(c -> c.regionCount, Collectors.counting()));
        
        log.info("\n【按区域数量分布】");
        log.info("┌─────────────────────┬──────────┬─────────┐");
        log.info("│ 区域数量            │ 用例数   │ 占比    │");
        log.info("├─────────────────────┼──────────┼─────────┤");
        
        long total = generatedCases.size();
        byRegionCount.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .forEach(entry -> {
                    String regionDesc = entry.getKey() == 0 ? "0 (全市)" : String.valueOf(entry.getKey());
                    double percentage = (entry.getValue() * 100.0) / total;
                    String padding = repeat(" ", Math.max(0, 16 - regionDesc.length()));
                    log.info("│ {}{} │ {:>8} │ {:>6.1f}% │", 
                            regionDesc, padding, entry.getValue(), String.format("%.1f", percentage));
                });
        log.info("└─────────────────────┴──────────┴─────────┘");

        // 统计全市与具体区域的比例
        long fullCityCount = byRegionCount.getOrDefault(0, 0L);
        long specificRegionCount = total - fullCityCount;
        log.info("\n【全市 vs 具体区域】");
        log.info("  全市投放: {} 个 ({})", fullCityCount, String.format("%.1f", (fullCityCount * 100.0) / total) + "%");
        log.info("  具体区域: {} 个 ({})", specificRegionCount, String.format("%.1f", (specificRegionCount * 100.0) / total) + "%");

        // 按投放方式分析区域分布
        log.info("\n【各投放方式的区域分布】");
        Map<String, Map<Integer, Long>> byMethodAndRegion = generatedCases.stream()
                .collect(Collectors.groupingBy(
                        c -> c.deliveryMethod,
                        Collectors.groupingBy(
                                c -> c.regionCount,
                                Collectors.counting())));
        
        for (Map.Entry<String, Map<Integer, Long>> methodEntry : byMethodAndRegion.entrySet()) {
            log.info("  {}:", methodEntry.getKey());
            methodEntry.getValue().entrySet().stream()
                    .sorted(Map.Entry.comparingByKey())
                    .forEach(regionEntry -> {
                        String desc = regionEntry.getKey() == 0 ? "全市" : regionEntry.getKey() + "个区域";
                        log.info("    - {}: {} 个用例", desc, regionEntry.getValue());
                    });
        }
    }

    @Test
    @Order(5)
    @DisplayName("步骤5: 生成覆盖情况汇总报告")
    void step5_generateSummaryReport() {
        log.info("========================================");
        log.info("步骤5: 覆盖情况汇总报告");
        log.info("========================================");

        generatedCases = testCaseGenerator.generateAllTestCasesInternal(14, 42);
        long total = generatedCases.size();

        log.info("\n╔══════════════════════════════════════════════════════════════════╗");
        log.info("║           TestCaseGenerator 测试用例覆盖情况汇总                 ║");
        log.info("╠══════════════════════════════════════════════════════════════════╣");
        log.info("║                                                                  ║");
        log.info("║  📊 基本统计                                                     ║");
        log.info("║  ├─ 总用例数: {}                                              ║", total);
        
        Map<String, Long> byMethod = generatedCases.stream()
                .collect(Collectors.groupingBy(c -> c.deliveryMethod, Collectors.counting()));
        log.info("║  ├─ 按档位投放: {} 个                                          ║", 
                byMethod.getOrDefault("按档位投放", 0L));
        log.info("║  ├─ 按档位扩展投放: {} 个                                      ║", 
                byMethod.getOrDefault("按档位扩展投放", 0L));
        log.info("║  └─ 按价位段自选投放: {} 个                                    ║", 
                byMethod.getOrDefault("按价位段自选投放", 0L));
        log.info("║                                                                  ║");
        
        // 扩展类型覆盖
        long uniqueEtypes = generatedCases.stream()
                .map(c -> c.deliveryEtype)
                .distinct()
                .count();
        log.info("║  📋 扩展类型覆盖                                                 ║");
        log.info("║  ├─ 唯一扩展类型数: {} 种                                       ║", uniqueEtypes);
        log.info("║  ├─ 单扩展: 8 种（全覆盖）                                       ║");
        log.info("║  └─ 双扩展: 11 种（全覆盖）                                      ║");
        log.info("║                                                                  ║");
        
        // 预投放量覆盖
        DoubleSummaryStatistics advStats = generatedCases.stream()
                .mapToDouble(c -> c.adv.doubleValue())
                .summaryStatistics();
        log.info("║  💰 预投放量覆盖                                                 ║");
        log.info("║  ├─ 范围: {} ~ {}                                  ║", 
                String.format("%.0f", advStats.getMin()), String.format("%.0f", advStats.getMax()));
        log.info("║  ├─ 8个阶层全覆盖: 0-1K, 1K-2K, 2K-5K, 5K-10K,                  ║");
        log.info("║  │                 10K-20K, 20K-50K, 50K-100K, 100K-150K        ║");
        log.info("║  └─ 包含边界值: 1, 999, 1000, 1001, 99999, 100000, 150000       ║");
        log.info("║                                                                  ║");
        
        // 标签覆盖
        Map<String, Long> byTag = generatedCases.stream()
                .collect(Collectors.groupingBy(c -> c.tag != null ? "有标签" : "无标签", Collectors.counting()));
        log.info("║  🏷️  标签覆盖                                                     ║");
        log.info("║  ├─ 无标签: {} 个 ({})                                     ║", 
                byTag.getOrDefault("无标签", 0L),
                String.format("%.1f", (byTag.getOrDefault("无标签", 0L) * 100.0) / total) + "%");
        log.info("║  └─ 优质数据共享客户: {} 个 ({})                           ║", 
                byTag.getOrDefault("有标签", 0L),
                String.format("%.1f", (byTag.getOrDefault("有标签", 0L) * 100.0) / total) + "%");
        
        // 区域覆盖
        Map<Integer, Long> byRegion = generatedCases.stream()
                .collect(Collectors.groupingBy(c -> c.regionCount, Collectors.counting()));
        long fullCity = byRegion.getOrDefault(0, 0L);
        log.info("║  🌍 区域覆盖                                                     ║");
        log.info("║  ├─ 全市投放: {} 个 ({})                                   ║", 
                fullCity, String.format("%.1f", (fullCity * 100.0) / total) + "%");
        log.info("║  └─ 具体区域: {} 个 ({})                                   ║", 
                total - fullCity, String.format("%.1f", ((total - fullCity) * 100.0) / total) + "%");
        log.info("║                                                                  ║");
        log.info("╚══════════════════════════════════════════════════════════════════╝");

        // 验证覆盖完整性
        assertTrue(byMethod.size() >= 3, "应该覆盖至少3种投放方式");
        assertTrue(uniqueEtypes >= 19, "应该覆盖至少19种扩展类型（含null）");
        assertTrue(advStats.getMax() >= 100000, "应该覆盖到100K以上的预投放量");

        // 6. 生成覆盖率矩阵
        log.info("\n【覆盖率矩阵分析】");
        generateCoverageMatrix();
    }

    /**
     * 重复字符串（Java 8 兼容）
     */
    private String repeat(String str, int count) {
        if (count <= 0) return "";
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < count; i++) {
            sb.append(str);
        }
        return sb.toString();
    }

    /**
     * 生成覆盖率矩阵，展示投放方式×扩展类型×标签的覆盖情况
     */
    private void generateCoverageMatrix() {
        generatedCases = testCaseGenerator.generateAllTestCasesInternal(14, 42);
        
        // 构建三维矩阵：投放方式 × 扩展类型 × 标签
        Map<String, Map<String, Map<String, Long>>> matrix = new LinkedHashMap<>();
        
        for (TestCaseGenerator.TestCaseConfig config : generatedCases) {
            String method = config.deliveryMethod;
            String etype = config.deliveryEtype != null ? config.deliveryEtype : "无扩展";
            String tag = config.tag != null ? config.tag : "无标签";
            
            matrix.computeIfAbsent(method, k -> new LinkedHashMap<>())
                    .computeIfAbsent(etype, k -> new LinkedHashMap<>())
                    .put(tag, matrix.get(method).get(etype).getOrDefault(tag, 0L) + 1);
        }
        
        log.info("投放方式 × 扩展类型 × 标签 覆盖矩阵（前20个组合）:");
        int count = 0;
        for (Map.Entry<String, Map<String, Map<String, Long>>> methodEntry : matrix.entrySet()) {
            for (Map.Entry<String, Map<String, Long>> etypeEntry : methodEntry.getValue().entrySet()) {
                for (Map.Entry<String, Long> tagEntry : etypeEntry.getValue().entrySet()) {
                    if (count++ >= 20) break;
                    log.info("  {} | {} | {} : {} 个用例",
                            methodEntry.getKey(),
                            etypeEntry.getKey(),
                            tagEntry.getKey(),
                            tagEntry.getValue());
                }
                if (count >= 20) break;
            }
            if (count >= 20) break;
        }
        
        // 统计矩阵密度
        long totalCombinations = matrix.values().stream()
                .flatMap(m -> m.values().stream())
                .flatMap(m -> m.values().stream())
                .mapToLong(Long::longValue)
                .sum();
        long uniqueCombinations = matrix.values().stream()
                .flatMap(m -> m.values().stream())
                .flatMap(m -> m.keySet().stream())
                .count();
        
        log.info("\n矩阵统计:");
        log.info("  总用例数: {}", totalCombinations);
        log.info("  唯一组合数: {}", uniqueCombinations);
        log.info("  平均每个组合用例数: {}", String.format("%.2f", (double) totalCombinations / uniqueCombinations));
    }

    /**
     * 根据配置构建投放区域字符串
     * 注意：DELIVERY_AREA 字段最大长度为 800，需要限制区域字符串长度
     */
    private String buildDeliveryArea(TestCaseGenerator.TestCaseConfig config) {
        if (config.regionCount == 0 || config.availableRegions == null || config.availableRegions.isEmpty()) {
            return "全市";
        }

        // 从可用区域中选择指定数量的区域
        int count = Math.min(config.regionCount, config.availableRegions.size());
        List<String> selectedRegions = config.availableRegions.subList(0, count);
        String areaStr = String.join(",", selectedRegions);
        
        // 限制长度不超过 800 字符（数据库字段限制）
        if (areaStr.length() > 800) {
            // 如果超过长度，只取前几个区域，确保不超过 800 字符
            StringBuilder sb = new StringBuilder();
            for (String region : selectedRegions) {
                if (sb.length() + region.length() + 1 > 800) {
                    break;
                }
                if (sb.length() > 0) {
                    sb.append(",");
                }
                sb.append(region);
            }
            areaStr = sb.toString();
        }
        
        return areaStr;
    }

    /**
     * 获取预投放量所属的阶层
     */
    private String getAdvRange(TestCaseGenerator.TestCaseConfig config) {
        double v = config.adv.doubleValue();
        if (v < 1000) return "0-1K";
        if (v < 2000) return "1K-2K";
        if (v < 5000) return "2K-5K";
        if (v < 10000) return "5K-10K";
        if (v < 20000) return "10K-20K";
        if (v < 50000) return "20K-50K";
        if (v < 100000) return "50K-100K";
        return "100K-150K";
    }
}
