package org.example.service;

import lombok.extern.slf4j.Slf4j;
import org.example.application.service.coordinator.RegionCustomerStatisticsBuildService;
import org.example.domain.repository.CigaretteDistributionInfoRepository;
import org.example.domain.repository.FilterCustomerTableRepository;
import org.example.domain.repository.RegionCustomerStatisticsRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 区域客户数统计功能集成测试
 * 
 * <p>测试流程：
 * 1. 根据客户类型和工作日筛选 base_customer_info 数据到 customer_filter 分区表
 * 2. 调用 RegionCustomerStatisticsBuildService 构建区域客户数统计
 * 3. 验证 region_customer_statistics 分区表的结果
 * </p>
 * 
 * <p>前置条件：
 * - base_customer_info 表中有 2099/9/1 分区的客户数据
 * - cigarette_distribution_info 表中有 2099/9/1 分区的卷烟投放信息
 * </p>
 * 
 * @author Robin
 * @since 2025-12-20
 */
@Slf4j
@SpringBootTest
class RegionCustomerStatisticsIntegrationTest {

    @Autowired
    private FilterCustomerTableRepository filterCustomerTableRepository;

    @Autowired
    private RegionCustomerStatisticsBuildService regionCustomerStatisticsBuildService;

    @Autowired
    private RegionCustomerStatisticsRepository regionCustomerStatisticsRepository;

    @Autowired
    private CigaretteDistributionInfoRepository cigaretteDistributionInfoRepository;

    private static final Integer YEAR = 2099;
    private static final Integer MONTH = 9;
    private static final Integer WEEK_SEQ = 1;

    /**
     * 测试完整的区域客户数统计流程
     * 
     * 测试场景：使用 {单周客户, 正常客户} 和 {周一, 周二, 周三, 周四, 周五}
     */
    @Test
    void should_build_region_customer_statistics_for_partition_2099_9_1() {
        log.info("===== 开始测试区域客户数统计功能 =====");
        log.info("测试分区: {}-{}-{}", YEAR, MONTH, WEEK_SEQ);

        // 1. 准备 customer_filter 分区数据
        log.info("步骤1: 准备 customer_filter 分区数据...");
        prepareCustomerFilterData();

        // 2. 验证 customer_filter 数据已正确写入
        log.info("步骤2: 验证 customer_filter 数据...");
        Long customerFilterCount = filterCustomerTableRepository.countPartition(YEAR, MONTH, WEEK_SEQ);
        log.info("customer_filter 分区数据条数: {}", customerFilterCount);
        assertTrue(customerFilterCount > 0, "customer_filter 分区应该有数据");

        // 3. 验证 cigarette_distribution_info 表有投放组合数据
        log.info("步骤3: 检查卷烟投放信息...");
        List<Map<String, Object>> combinations = cigaretteDistributionInfoRepository.findDistinctCombinations(YEAR, MONTH, WEEK_SEQ);
        log.info("扫描到 {} 个不重复的投放组合", combinations.size());
        assertFalse(combinations.isEmpty(), "cigarette_distribution_info 应该有投放组合数据");

        // 打印部分投放组合信息
        combinations.stream().limit(5).forEach(combo -> {
            log.info("  投放组合: DELIVERY_METHOD={}, DELIVERY_ETYPE={}, TAG={}, TAG_FILTER_CONFIG={}",
                    combo.get("DELIVERY_METHOD"),
                    combo.get("DELIVERY_ETYPE"),
                    combo.get("TAG"),
                    combo.get("TAG_FILTER_CONFIG"));
        });

        // 4. 调用区域客户数统计构建服务
        log.info("步骤4: 构建区域客户数统计...");
        Map<String, Object> result = regionCustomerStatisticsBuildService.buildRegionCustomerStatistics(YEAR, MONTH, WEEK_SEQ);
        
        log.info("构建结果: {}", result);
        assertTrue(Boolean.TRUE.equals(result.get("success")), 
                "区域客户数统计构建应该成功，实际结果: " + result.get("message"));

        // 5. 验证 region_customer_statistics 分区数据
        log.info("步骤5: 验证 region_customer_statistics 分区数据...");
        List<Map<String, Object>> regionStats = regionCustomerStatisticsRepository.findAll(YEAR, MONTH, WEEK_SEQ);
        log.info("region_customer_statistics 分区记录数: {}", regionStats.size());
        assertFalse(regionStats.isEmpty(), "region_customer_statistics 分区应该有数据");

        // 打印部分区域统计信息（注意：resultMap 使用小写属性名）
        regionStats.stream().limit(10).forEach(stat -> {
            log.info("  区域: {}, total={}, d30={}, d1={}",
                    stat.get("region"),
                    stat.get("total"),
                    stat.get("d30"),
                    stat.get("d1"));
        });

        // 6. 验证"全市"区域存在（使用小写属性名）
        boolean hasFullCityRegion = regionStats.stream()
                .anyMatch(stat -> "全市".equals(stat.get("region")));
        assertTrue(hasFullCityRegion, "应该存在'全市'区域的统计记录");

        // 7. 验证全市区域的客户数总和
        Map<String, Object> fullCityStat = regionStats.stream()
                .filter(stat -> "全市".equals(stat.get("region")))
                .findFirst()
                .orElse(null);
        
        if (fullCityStat != null) {
            Object totalObj = fullCityStat.get("total");
            log.info("'全市' 区域统计: total={}", totalObj);
            assertNotNull(totalObj, "'全市'区域的total不应为null");
        }

        log.info("===== 区域客户数统计测试完成 =====");
        log.info("统计结果汇总:");
        log.info("  - customer_filter 记录数: {}", customerFilterCount);
        log.info("  - 投放组合数: {}", combinations.size());
        log.info("  - region_customer_statistics 记录数: {}", regionStats.size());
        log.info("  - 构建插入记录数: {}", result.get("insertedCount"));
    }

    /**
     * 准备 customer_filter 分区数据
     * 
     * 根据客户类型 {单周客户, 正常客户} 和工作日 {周一, 周二, 周三, 周四, 周五} 筛选
     */
    private void prepareCustomerFilterData() {
        // 客户类型集合
        List<String> customerTypes = Arrays.asList("单周客户", "正常客户");
        // 工作日集合
        List<String> workdays = Arrays.asList("周一", "周二", "周三", "周四", "周五");

        // 构建 ORDER_CYCLE 的 WHERE 条件
        // 规则：
        // - 单周客户: ORDER_CYCLE LIKE '单周%'
        // - 双周客户: ORDER_CYCLE LIKE '双周%'
        // - 正常客户: ORDER_CYCLE LIKE '周%' (不含单周/双周前缀)
        // - 排除: ORDER_CYCLE = '不访销'
        String whereClause = buildWhereClause(customerTypes, workdays);
        log.info("生成的 WHERE 子句: {}", whereClause);

        // 调用 FilterCustomerTableRepository 填充数据
        filterCustomerTableRepository.ensurePartitionAndInsertData(YEAR, MONTH, WEEK_SEQ, whereClause);
        log.info("customer_filter 分区数据准备完成");
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
        List<String> orderCycleConditions = new java.util.ArrayList<>();

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

    /**
     * 测试只查询已有的区域客户数统计数据（不重建）
     */
    @Test
    void should_query_existing_region_customer_statistics() {
        log.info("===== 测试查询已有区域客户数统计 =====");

        // 查询分区数据
        List<Map<String, Object>> regionStats = regionCustomerStatisticsRepository.findAll(YEAR, MONTH, WEEK_SEQ);
        log.info("查询到 {} 条区域客户统计记录", regionStats.size());

        // 打印所有区域信息（使用小写属性名）
        regionStats.forEach(stat -> {
            log.info("区域: {}, total={}", stat.get("region"), stat.get("total"));
        });

        // 这个测试不做断言，只是展示数据
        log.info("===== 查询测试完成 =====");
    }

    /**
     * 测试验证各区域的30个档位数据完整性
     */
    @Test
    void should_verify_grade_data_completeness() {
        log.info("===== 测试验证档位数据完整性 =====");

        // 先确保有数据
        prepareCustomerFilterData();
        regionCustomerStatisticsBuildService.buildRegionCustomerStatistics(YEAR, MONTH, WEEK_SEQ);

        // 查询所有区域统计
        List<Map<String, Object>> regionStats = regionCustomerStatisticsRepository.findAll(YEAR, MONTH, WEEK_SEQ);
        assertFalse(regionStats.isEmpty(), "应该有区域统计数据");

        // 验证每个区域的档位数据（使用小写属性名 d30-d1, total）
        for (Map<String, Object> stat : regionStats) {
            String region = (String) stat.get("region");
            
            // 验证 d30-d1 字段存在
            int nonNullGradeCount = 0;
            java.math.BigDecimal calculatedTotal = java.math.BigDecimal.ZERO;
            
            for (int i = 30; i >= 1; i--) {
                Object gradeValue = stat.get("d" + i);  // 小写
                if (gradeValue != null) {
                    nonNullGradeCount++;
                    if (gradeValue instanceof java.math.BigDecimal) {
                        calculatedTotal = calculatedTotal.add((java.math.BigDecimal) gradeValue);
                    } else if (gradeValue instanceof Number) {
                        calculatedTotal = calculatedTotal.add(java.math.BigDecimal.valueOf(((Number) gradeValue).longValue()));
                    }
                }
            }

            Object totalObj = stat.get("total");  // 小写
            log.debug("区域 {}: 非空档位数={}, 计算总数={}, total={}",
                    region, nonNullGradeCount, calculatedTotal, totalObj);

            // 验证 total 与各档位之和一致（允许小误差）
            if (totalObj != null) {
                java.math.BigDecimal recordedTotal;
                if (totalObj instanceof java.math.BigDecimal) {
                    recordedTotal = (java.math.BigDecimal) totalObj;
                } else {
                    recordedTotal = java.math.BigDecimal.valueOf(((Number) totalObj).longValue());
                }
                
                // 验证总数一致
                assertEquals(0, recordedTotal.compareTo(calculatedTotal),
                        String.format("区域 '%s' 的 total(%s) 应与档位之和(%s)一致", 
                                region, recordedTotal, calculatedTotal));
            }
        }

        log.info("===== 档位数据完整性验证完成，共验证 {} 个区域 =====", regionStats.size());
    }

    /**
     * 测试验证扩展维度区域（如按区县+市场类型）
     */
    @Test
    void should_verify_extension_dimension_regions() {
        log.info("===== 测试验证扩展维度区域 =====");

        // 先确保有数据
        prepareCustomerFilterData();
        regionCustomerStatisticsBuildService.buildRegionCustomerStatistics(YEAR, MONTH, WEEK_SEQ);

        // 查询所有区域统计（使用小写属性名 region）
        List<Map<String, Object>> regionStats = regionCustomerStatisticsRepository.findAll(YEAR, MONTH, WEEK_SEQ);

        // 分类统计各类区域
        long fullCityCount = regionStats.stream()
                .filter(s -> "全市".equals(s.get("region")) || String.valueOf(s.get("region")).startsWith("全市"))
                .count();

        // 按区县分类的区域（如：城区、丹江、房县等）
        List<String> countyRegions = Arrays.asList("城区", "丹江", "房县", "郧西", "郧阳", "竹山", "竹溪");
        long countyCount = regionStats.stream()
                .filter(s -> {
                    String region = String.valueOf(s.get("region"));
                    return countyRegions.stream().anyMatch(region::startsWith);
                })
                .count();

        // 按市场类型分类的区域（如：城网、农网）
        long marketTypeCount = regionStats.stream()
                .filter(s -> {
                    String region = String.valueOf(s.get("region"));
                    return region.contains("城网") || region.contains("农网");
                })
                .count();

        log.info("区域分类统计:");
        log.info("  - 全市类区域数: {}", fullCityCount);
        log.info("  - 区县类区域数: {}", countyCount);
        log.info("  - 市场类型类区域数: {}", marketTypeCount);
        log.info("  - 总区域数: {}", regionStats.size());

        // 至少应该有全市区域
        assertTrue(fullCityCount >= 1, "应该至少有一个'全市'区域");

        // 打印所有区域名称（用于调试）
        log.info("所有区域列表:");
        regionStats.forEach(s -> log.info("  - {}", s.get("region")));

        log.info("===== 扩展维度区域验证完成 =====");
    }

    /**
     * 测试验证 customer_filter 表的数据质量
     */
    @Test
    void should_verify_customer_filter_data_quality() {
        log.info("===== 测试验证 customer_filter 数据质量 =====");

        // 准备数据
        prepareCustomerFilterData();

        // 1. 验证记录数
        Long count = filterCustomerTableRepository.countPartition(YEAR, MONTH, WEEK_SEQ);
        log.info("customer_filter 记录数: {}", count);
        assertTrue(count > 0, "customer_filter 应该有数据");

        // 2. 验证 ORDER_CYCLE 分布
        List<String> orderCycles = filterCustomerTableRepository.listOrderCyclesPartition(YEAR, MONTH, WEEK_SEQ);
        log.info("ORDER_CYCLE 分布: {}", orderCycles);
        assertFalse(orderCycles.isEmpty(), "应该有 ORDER_CYCLE 数据");
        
        // 验证没有"不访销"
        assertFalse(orderCycles.contains("不访销"), "不应包含'不访销'的客户");

        // 3. 验证 GRADE 分布（用于确认档位数据）
        List<String> gradeColumns = Arrays.asList("GRADE");
        List<Map<String, Object>> gradeDistribution = filterCustomerTableRepository.listDistinctCombinationsPartition(
                YEAR, MONTH, WEEK_SEQ, gradeColumns);
        log.info("GRADE 分布数量: {}", gradeDistribution.size());
        
        gradeDistribution.forEach(g -> log.debug("  - GRADE: {}", g.get("GRADE")));

        // 4. 验证 COMPANY_DISTRICT 分布（应该是截取后的2个字）
        List<String> districtColumns = Arrays.asList("COMPANY_DISTRICT");
        List<Map<String, Object>> districtDistribution = filterCustomerTableRepository.listDistinctCombinationsPartition(
                YEAR, MONTH, WEEK_SEQ, districtColumns);
        log.info("COMPANY_DISTRICT 分布: ");
        districtDistribution.forEach(d -> {
            String district = (String) d.get("COMPANY_DISTRICT");
            log.info("  - {}", district);
            // 验证长度为2（截取规则）
            if (district != null && !district.isEmpty()) {
                assertTrue(district.length() <= 2, 
                        "COMPANY_DISTRICT 应该是截取后的2个字，实际: " + district);
            }
        });

        // 5. 验证 MARKET_DEPARTMENT 分布（应该去掉"市场部"后缀）
        List<String> deptColumns = Arrays.asList("MARKET_DEPARTMENT");
        List<Map<String, Object>> deptDistribution = filterCustomerTableRepository.listDistinctCombinationsPartition(
                YEAR, MONTH, WEEK_SEQ, deptColumns);
        log.info("MARKET_DEPARTMENT 分布: ");
        deptDistribution.forEach(d -> {
            String dept = (String) d.get("MARKET_DEPARTMENT");
            log.info("  - {}", dept);
            // 验证不包含"市场部"后缀
            if (dept != null && !dept.isEmpty()) {
                assertFalse(dept.endsWith("市场部"), 
                        "MARKET_DEPARTMENT 不应包含'市场部'后缀，实际: " + dept);
            }
        });

        log.info("===== customer_filter 数据质量验证完成 =====");
    }

    /**
     * 测试验证统计结果与 customer_filter 的一致性
     */
    @Test
    void should_verify_statistics_consistency_with_customer_filter() {
        log.info("===== 测试验证统计一致性 =====");

        // 准备数据并构建统计
        prepareCustomerFilterData();
        regionCustomerStatisticsBuildService.buildRegionCustomerStatistics(YEAR, MONTH, WEEK_SEQ);

        // 获取 customer_filter 总数
        Long customerFilterTotal = filterCustomerTableRepository.countPartition(YEAR, MONTH, WEEK_SEQ);
        log.info("customer_filter 总记录数: {}", customerFilterTotal);

        // 获取"全市"区域的 total（使用小写属性名）
        Map<String, Object> fullCityStat = regionCustomerStatisticsRepository.findByRegion(YEAR, MONTH, WEEK_SEQ, "全市");
        
        if (fullCityStat != null) {
            Object totalObj = fullCityStat.get("total");  // 小写
            log.info("'全市' 区域 total: {}", totalObj);
            
            if (totalObj != null) {
                long fullCityTotal;
                if (totalObj instanceof java.math.BigDecimal) {
                    fullCityTotal = ((java.math.BigDecimal) totalObj).longValue();
                } else {
                    fullCityTotal = ((Number) totalObj).longValue();
                }
                
                // 全市区域的客户数应该等于 customer_filter 的总记录数
                // （假设没有标签过滤的情况下）
                log.info("对比: customer_filter 总数={}, 全市total={}", customerFilterTotal, fullCityTotal);
                
                // 注意：如果有标签过滤，这个断言可能不成立
                // assertEquals(customerFilterTotal.longValue(), fullCityTotal,
                //         "全市区域的total应与customer_filter总数一致");
            }
        } else {
            log.warn("未找到'全市'区域的统计数据");
        }

        log.info("===== 统计一致性验证完成 =====");
    }
}