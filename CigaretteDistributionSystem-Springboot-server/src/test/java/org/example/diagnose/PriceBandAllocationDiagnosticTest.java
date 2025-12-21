package org.example.diagnose;

import lombok.extern.slf4j.Slf4j;
import org.example.application.service.calculate.PriceBandAllocationService;
import org.example.application.service.coordinator.PriceBandCandidateQueryService;
import org.example.application.service.coordinator.RegionCustomerStatisticsBuildService;
import org.example.domain.repository.CigaretteDistributionInfoRepository;
import org.example.domain.repository.CigaretteDistributionPredictionPriceRepository;
import org.example.domain.repository.RegionCustomerStatisticsRepository;
import org.example.infrastructure.persistence.po.CigaretteDistributionInfoPO;
import org.example.shared.util.PartitionTableManager;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 按价位段自选投放分配写回诊断测试。
 * <p>
 * 专门用于定位按价位段自选投放分配写回过程中的问题。
 * 测试流程：
 * 1. 检查 info 表中的按价位段自选投放数据
 * 2. 检查 base_cigarette_price 表中的批发价数据
 * 3. 检查 region_customer_statistics 表中的"全市"客户数据
 * 4. 检查候选卷烟查询结果
 * 5. 执行分配流程
 * 6. 检查写回结果并定位问题
 * </p>
 *
 * @author Robin
 * @since 2025-12-20
 */
@Slf4j
@SpringBootTest
@ActiveProfiles("test")
@DisplayName("按价位段自选投放分配写回诊断测试")
public class PriceBandAllocationDiagnosticTest {

    @Autowired
    private PriceBandAllocationService priceBandAllocationService;

    @Autowired
    private PriceBandCandidateQueryService priceBandCandidateQueryService;

    @Autowired
    private CigaretteDistributionInfoRepository cigaretteDistributionInfoRepository;

    @Autowired
    private CigaretteDistributionPredictionPriceRepository predictionPriceRepository;

    @Autowired
    private RegionCustomerStatisticsRepository regionCustomerStatisticsRepository;

    @Autowired
    private RegionCustomerStatisticsBuildService regionCustomerStatisticsBuildService;

    @Autowired
    private PartitionTableManager partitionTableManager;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private static final int YEAR = 2099;
    private static final int MONTH = 9;
    private static final int WEEK_SEQ = 1;

    @BeforeEach
    void setUp() {
        log.info("========================================");
        log.info("开始按价位段自选投放分配写回诊断测试");
        log.info("========================================");
        
        // 确保分区存在
        partitionTableManager.ensurePartitionExists("cigarette_distribution_info", YEAR, MONTH, WEEK_SEQ);
        partitionTableManager.ensurePartitionExists("cigarette_distribution_prediction_price", YEAR, MONTH, WEEK_SEQ);
        partitionTableManager.ensurePartitionExists("region_customer_statistics", YEAR, MONTH, WEEK_SEQ);
    }

    @Test
    @DisplayName("步骤1: 检查 info 表中的按价位段自选投放数据")
    void step1_checkInfoTableData() {
        log.info("\n===== 步骤1: 检查 info 表中的按价位段自选投放数据 =====");
        
        String sql = "SELECT CIG_CODE, CIG_NAME, DELIVERY_METHOD, DELIVERY_ETYPE, DELIVERY_AREA, " +
                     "ADV, TAG, TAG_FILTER_CONFIG " +
                     "FROM cigarette_distribution_info " +
                     "WHERE YEAR = ? AND MONTH = ? AND WEEK_SEQ = ? " +
                     "AND DELIVERY_METHOD = '按价位段自选投放' " +
                     "ORDER BY CIG_CODE " +
                     "LIMIT 20";
        
        List<Map<String, Object>> infoRecords = jdbcTemplate.queryForList(sql, YEAR, MONTH, WEEK_SEQ);
        
        log.info("info 表中按价位段自选投放记录数: {}", infoRecords.size());
        
        if (infoRecords.isEmpty()) {
            log.error("❌ 问题1: info 表中没有按价位段自选投放的数据！");
            log.info("   解决方案: 需要先插入按价位段自选投放的测试数据到 cigarette_distribution_info 表");
            return;
        }
        
        log.info("✅ info 表中有 {} 条按价位段自选投放记录", infoRecords.size());
        
        // 打印前10条记录
        log.info("前10条记录:");
        infoRecords.stream().limit(10).forEach(row -> {
            log.info("  卷烟: {}({}), ADV={}, DELIVERY_AREA={}, TAG={}",
                    row.get("CIG_NAME"), row.get("CIG_CODE"),
                    row.get("ADV"), row.get("DELIVERY_AREA"), row.get("TAG"));
        });
        
        // 检查是否有 ADV 为 0 或 null 的记录
        long zeroAdvCount = infoRecords.stream()
                .filter(row -> {
                    Object adv = row.get("ADV");
                    return adv == null || 
                           (adv instanceof BigDecimal && ((BigDecimal) adv).compareTo(BigDecimal.ZERO) <= 0);
                })
                .count();
        
        if (zeroAdvCount > 0) {
            log.warn("⚠️  警告: 有 {} 条记录的 ADV 为 0 或 null，这些记录可能无法参与分配", zeroAdvCount);
        }
    }

    @Test
    @DisplayName("步骤2: 检查 base_cigarette_price 表中的批发价数据")
    void step2_checkPriceTableData() {
        log.info("\n===== 步骤2: 检查 base_cigarette_price 表中的批发价数据 =====");
        
        // 先查询 info 表中的卷烟代码
        String infoSql = "SELECT DISTINCT CIG_CODE, CIG_NAME " +
                         "FROM cigarette_distribution_info " +
                         "WHERE YEAR = ? AND MONTH = ? AND WEEK_SEQ = ? " +
                         "AND DELIVERY_METHOD = '按价位段自选投放' " +
                         "LIMIT 50";
        
        List<Map<String, Object>> infoCigarettes = jdbcTemplate.queryForList(infoSql, YEAR, MONTH, WEEK_SEQ);
        
        if (infoCigarettes.isEmpty()) {
            log.warn("info 表中没有按价位段自选投放的数据，跳过批发价检查");
            return;
        }
        
        log.info("info 表中有 {} 种不同的卷烟", infoCigarettes.size());
        
        // 检查每种卷烟是否在 base_cigarette_price 表中有批发价
        List<String> missingPrices = new ArrayList<>();
        List<String> hasPrices = new ArrayList<>();
        
        for (Map<String, Object> cig : infoCigarettes) {
            String cigCode = (String) cig.get("CIG_CODE");
            String cigName = (String) cig.get("CIG_NAME");
            
            String priceSql = "SELECT CIG_CODE, CIG_NAME, WHOLESALE_PRICE " +
                              "FROM base_cigarette_price " +
                              "WHERE CIG_CODE = ? " +
                              "LIMIT 1";
            
            List<Map<String, Object>> priceRecords = jdbcTemplate.queryForList(priceSql, cigCode);
            
            if (priceRecords.isEmpty()) {
                missingPrices.add(cigCode + "(" + cigName + ")");
            } else {
                BigDecimal wholesalePrice = (BigDecimal) priceRecords.get(0).get("WHOLESALE_PRICE");
                hasPrices.add(cigCode + "(" + cigName + ") - 批发价: " + wholesalePrice);
            }
        }
        
        log.info("✅ 有批发价的卷烟数: {}", hasPrices.size());
        if (!hasPrices.isEmpty()) {
            log.info("前10条有批发价的卷烟:");
            hasPrices.stream().limit(10).forEach(log::info);
        }
        
        if (!missingPrices.isEmpty()) {
            log.error("❌ 问题2: 以下 {} 种卷烟在 base_cigarette_price 表中没有批发价数据:", missingPrices.size());
            missingPrices.forEach(cig -> log.error("   - {}", cig));
            log.error("   解决方案: 需要在 base_cigarette_price 表中为这些卷烟添加批发价数据");
        } else {
            log.info("✅ 所有卷烟都有批发价数据");
        }
    }

    @Test
    @DisplayName("步骤3: 检查 region_customer_statistics 表中的'全市'客户数据")
    void step3_checkRegionCustomerStatistics() {
        log.info("\n===== 步骤3: 检查 region_customer_statistics 表中的'全市'客户数据 =====");
        
        String sql = "SELECT REGION, TOTAL, D30, D29, D28, D27, D26, D25, D24, D23, D22, D21, " +
                     "D20, D19, D18, D17, D16, D15, D14, D13, D12, D11, " +
                     "D10, D9, D8, D7, D6, D5, D4, D3, D2, D1 " +
                     "FROM region_customer_statistics " +
                     "WHERE YEAR = ? AND MONTH = ? AND WEEK_SEQ = ? " +
                     "AND REGION = '全市' " +
                     "LIMIT 1";
        
        List<Map<String, Object>> cityStats = jdbcTemplate.queryForList(sql, YEAR, MONTH, WEEK_SEQ);
        
        if (cityStats.isEmpty()) {
            log.error("❌ 问题3: region_customer_statistics 表中没有'全市'的客户数据！");
            log.error("   解决方案: 需要先构建区域客户数统计表");
            log.info("   正在尝试构建区域客户数统计表...");
            
            try {
                regionCustomerStatisticsBuildService.buildRegionCustomerStatistics(YEAR, MONTH, WEEK_SEQ);
                log.info("✅ 区域客户数统计表构建完成，重新查询...");
                
                cityStats = jdbcTemplate.queryForList(sql, YEAR, MONTH, WEEK_SEQ);
                if (cityStats.isEmpty()) {
                    log.error("❌ 构建后仍然没有'全市'的客户数据！");
                    return;
                }
            } catch (Exception e) {
                log.error("❌ 构建区域客户数统计表失败: {}", e.getMessage(), e);
                return;
            }
        }
        
        Map<String, Object> cityRow = cityStats.get(0);
        BigDecimal total = (BigDecimal) cityRow.get("TOTAL");
        
        log.info("✅ region_customer_statistics 表中有'全市'的客户数据");
        log.info("   总客户数: {}", total);
        
        // 检查30个档位是否全为0
        boolean allZero = true;
        for (int i = 30; i >= 1; i--) {
            BigDecimal gradeValue = (BigDecimal) cityRow.get("D" + i);
            if (gradeValue != null && gradeValue.compareTo(BigDecimal.ZERO) > 0) {
                allZero = false;
                break;
            }
        }
        
        if (allZero) {
            log.error("❌ 问题3-1: '全市'区域的30个档位客户数全为0！");
            log.error("   这会导致按价位段自选投放分配失败");
            log.error("   解决方案: 检查 customer_filter 表是否有数据，并重新构建 region_customer_statistics 表");
        } else {
            log.info("✅ '全市'区域有有效的客户数据（30个档位不全为0）");
            
            // 打印前5个非零档位
            log.info("前5个非零档位:");
            int count = 0;
            for (int i = 30; i >= 1 && count < 5; i--) {
                BigDecimal gradeValue = (BigDecimal) cityRow.get("D" + i);
                if (gradeValue != null && gradeValue.compareTo(BigDecimal.ZERO) > 0) {
                    log.info("   D{}: {}", i, gradeValue);
                    count++;
                }
            }
        }
    }

    @Test
    @DisplayName("步骤4: 检查候选卷烟查询结果")
    void step4_checkCandidateQuery() {
        log.info("\n===== 步骤4: 检查候选卷烟查询结果 =====");
        
        try {
            List<Map<String, Object>> candidates = priceBandCandidateQueryService.listOrderedPriceBandCandidates(YEAR, MONTH, WEEK_SEQ);
            
            if (candidates == null || candidates.isEmpty()) {
                log.error("❌ 问题4: 候选卷烟查询结果为空！");
                log.error("   可能的原因:");
                log.error("   1. info 表中没有按价位段自选投放的数据");
                log.error("   2. base_cigarette_price 表中没有对应的批发价数据");
                log.error("   3. 卷烟的批发价不在任何价位段范围内");
                return;
            }
            
            log.info("✅ 候选卷烟查询成功，共 {} 条", candidates.size());
            
            // 按价位段分组统计
            Map<Integer, Long> bandCounts = new TreeMap<>();
            for (Map<String, Object> candidate : candidates) {
                Object bandObj = candidate.get("PRICE_BAND");
                if (bandObj instanceof Number) {
                    int band = ((Number) bandObj).intValue();
                    bandCounts.put(band, bandCounts.getOrDefault(band, 0L) + 1);
                }
            }
            
            log.info("按价位段分布: {}", bandCounts);
            
            // 打印前10条候选卷烟
            log.info("前10条候选卷烟:");
            candidates.stream().limit(10).forEach(candidate -> {
                log.info("  卷烟: {}({}), 价位段={}, 批发价={}, ADV={}",
                        candidate.get("CIG_NAME"), candidate.get("CIG_CODE"),
                        candidate.get("PRICE_BAND"), candidate.get("WHOLESALE_PRICE"),
                        candidate.get("ADV"));
            });
            
        } catch (Exception e) {
            log.error("❌ 候选卷烟查询失败: {}", e.getMessage(), e);
        }
    }

    @Test
    @DisplayName("步骤5: 执行分配流程")
    void step5_executeAllocation() {
        log.info("\n===== 步骤5: 执行分配流程 =====");
        
        try {
            log.info("开始执行按价位段自选投放分配...");
            priceBandAllocationService.allocateForPriceBand(YEAR, MONTH, WEEK_SEQ);
            log.info("✅ 分配执行完成（未抛出异常）");
        } catch (Exception e) {
            log.error("❌ 问题5: 分配执行失败！");
            log.error("   错误信息: {}", e.getMessage(), e);
            log.error("   可能的原因:");
            log.error("   1. region_customer_statistics 表中没有'全市'的客户数据");
            log.error("   2. '全市'区域的30个档位客户数全为0");
            log.error("   3. 候选卷烟数据有问题");
            throw e;
        }
    }

    @Test
    @DisplayName("步骤6: 检查写回结果")
    void step6_checkWriteBackResults() {
        log.info("\n===== 步骤6: 检查写回结果 =====");
        
        String sql = "SELECT CIG_CODE, CIG_NAME, DELIVERY_AREA, DELIVERY_METHOD, " +
                     "ACTUAL_DELIVERY, D30, D29, D28, D27, D26, D25, D24, D23, D22, D21, " +
                     "D20, D19, D18, D17, D16, D15, D14, D13, D12, D11, " +
                     "D10, D9, D8, D7, D6, D5, D4, D3, D2, D1 " +
                     "FROM cigarette_distribution_prediction_price " +
                     "WHERE YEAR = ? AND MONTH = ? AND WEEK_SEQ = ? " +
                     "AND DELIVERY_METHOD = '按价位段自选投放' " +
                     "ORDER BY CIG_CODE " +
                     "LIMIT 50";
        
        List<Map<String, Object>> results = jdbcTemplate.queryForList(sql, YEAR, MONTH, WEEK_SEQ);
        
        log.info("prediction_price 表中按价位段自选投放记录数: {}", results.size());
        
        if (results.isEmpty()) {
            log.error("❌ 问题6: prediction_price 表中没有按价位段自选投放的写回结果！");
            log.error("   可能的原因:");
            log.error("   1. 分配流程未执行或执行失败");
            log.error("   2. 候选卷烟查询结果为空");
            log.error("   3. 写回过程中发生异常");
            log.error("   4. 所有卷烟的分配结果都被过滤掉了（如档位全为0）");
            
            // 对比 info 表和 prediction_price 表，找出未写回的卷烟
            compareInfoAndPredictionTables();
            return;
        }
        
        log.info("✅ prediction_price 表中有 {} 条按价位段自选投放的写回结果", results.size());
        
        // 检查每条记录的数据完整性
        int validCount = 0;
        int invalidCount = 0;
        List<String> invalidRecords = new ArrayList<>();
        
        for (Map<String, Object> row : results) {
            String cigCode = (String) row.get("CIG_CODE");
            String cigName = (String) row.get("CIG_NAME");
            BigDecimal actualDelivery = (BigDecimal) row.get("ACTUAL_DELIVERY");
            
            // 检查实际投放量
            if (actualDelivery == null || actualDelivery.compareTo(BigDecimal.ZERO) <= 0) {
                invalidCount++;
                invalidRecords.add(cigCode + "(" + cigName + ") - 实际投放量为0或null");
                continue;
            }
            
            // 检查档位数据
            boolean allGradesZero = true;
            BigDecimal gradeSum = BigDecimal.ZERO;
            for (int i = 30; i >= 1; i--) {
                BigDecimal gradeValue = (BigDecimal) row.get("D" + i);
                if (gradeValue != null && gradeValue.compareTo(BigDecimal.ZERO) > 0) {
                    allGradesZero = false;
                    gradeSum = gradeSum.add(gradeValue);
                }
            }
            
            if (allGradesZero) {
                invalidCount++;
                invalidRecords.add(cigCode + "(" + cigName + ") - 30个档位全为0");
            } else {
                validCount++;
            }
        }
        
        log.info("有效记录数: {}", validCount);
        log.info("无效记录数: {}", invalidCount);
        
        if (invalidCount > 0) {
            log.warn("⚠️  以下记录存在问题:");
            invalidRecords.stream().limit(10).forEach(log::warn);
        }
        
        // 打印前10条有效记录
        log.info("前10条有效记录:");
        results.stream()
                .filter(row -> {
                    BigDecimal actualDelivery = (BigDecimal) row.get("ACTUAL_DELIVERY");
                    return actualDelivery != null && actualDelivery.compareTo(BigDecimal.ZERO) > 0;
                })
                .limit(10)
                .forEach(row -> {
                    log.info("  卷烟: {}({}), 区域={}, 实际投放量={}, D30={}, D1={}",
                            row.get("CIG_NAME"), row.get("CIG_CODE"),
                            row.get("DELIVERY_AREA"), row.get("ACTUAL_DELIVERY"),
                            row.get("D30"), row.get("D1"));
                });
        
        // 对比 info 表和 prediction_price 表，找出未写回的卷烟
        compareInfoAndPredictionTables();
    }
    
    /**
     * 对比 info 表和 prediction_price 表，找出未写回的卷烟
     */
    private void compareInfoAndPredictionTables() {
        log.info("\n===== 对比 info 表和 prediction_price 表 =====");
        
        // 查询 info 表中的所有按价位段自选投放的卷烟
        String infoSql = "SELECT DISTINCT CIG_CODE, CIG_NAME, ADV " +
                         "FROM cigarette_distribution_info " +
                         "WHERE YEAR = ? AND MONTH = ? AND WEEK_SEQ = ? " +
                         "AND DELIVERY_METHOD = '按价位段自选投放' " +
                         "ORDER BY CIG_CODE";
        
        List<Map<String, Object>> infoCigarettes = jdbcTemplate.queryForList(infoSql, YEAR, MONTH, WEEK_SEQ);
        
        // 查询 prediction_price 表中的所有按价位段自选投放的卷烟
        String predSql = "SELECT DISTINCT CIG_CODE, CIG_NAME " +
                         "FROM cigarette_distribution_prediction_price " +
                         "WHERE YEAR = ? AND MONTH = ? AND WEEK_SEQ = ? " +
                         "AND DELIVERY_METHOD = '按价位段自选投放' " +
                         "ORDER BY CIG_CODE";
        
        List<Map<String, Object>> predCigarettes = jdbcTemplate.queryForList(predSql, YEAR, MONTH, WEEK_SEQ);
        
        Set<String> infoCigCodes = new HashSet<>();
        for (Map<String, Object> row : infoCigarettes) {
            String cigCode = (String) row.get("CIG_CODE");
            if (cigCode != null) {
                infoCigCodes.add(cigCode);
            }
        }
        
        Set<String> predCigCodes = new HashSet<>();
        for (Map<String, Object> row : predCigarettes) {
            String cigCode = (String) row.get("CIG_CODE");
            if (cigCode != null) {
                predCigCodes.add(cigCode);
            }
        }
        
        log.info("info 表中的卷烟数: {}", infoCigCodes.size());
        log.info("prediction_price 表中的卷烟数: {}", predCigCodes.size());
        
        // 找出未写回的卷烟
        Set<String> missingCigCodes = new HashSet<>(infoCigCodes);
        missingCigCodes.removeAll(predCigCodes);
        
        if (!missingCigCodes.isEmpty()) {
            log.error("❌ 问题6-1: 以下 {} 种卷烟在 info 表中存在，但在 prediction_price 表中未写回:", missingCigCodes.size());
            
            // 查询这些卷烟的详细信息（限制前20个）
            List<String> missingList = new ArrayList<>(missingCigCodes);
            if (missingList.size() > 20) {
                missingList = missingList.subList(0, 20);
            }
            
            StringBuilder sqlBuilder = new StringBuilder(
                    "SELECT CIG_CODE, CIG_NAME, ADV, TAG " +
                    "FROM cigarette_distribution_info " +
                    "WHERE YEAR = ? AND MONTH = ? AND WEEK_SEQ = ? " +
                    "AND DELIVERY_METHOD = '按价位段自选投放' " +
                    "AND CIG_CODE IN (");
            for (int i = 0; i < missingList.size(); i++) {
                if (i > 0) {
                    sqlBuilder.append(",");
                }
                sqlBuilder.append("?");
            }
            sqlBuilder.append(") GROUP BY CIG_CODE, CIG_NAME, ADV, TAG");
            
            List<Object> params = new ArrayList<>();
            params.add(YEAR);
            params.add(MONTH);
            params.add(WEEK_SEQ);
            params.addAll(missingList);
            
            List<Map<String, Object>> missingDetails = jdbcTemplate.queryForList(sqlBuilder.toString(), params.toArray());
            
            for (Map<String, Object> row : missingDetails) {
                log.error("  未写回: {}({}), ADV={}, TAG={}",
                        row.get("CIG_NAME"), row.get("CIG_CODE"),
                        row.get("ADV"), row.get("TAG"));
            }
            
             log.error("   可能的原因:");
             log.error("   1. 这些卷烟在 base_cigarette_price 表中没有批发价数据");
             log.error("   2. 这些卷烟的批发价不在任何价位段范围内");
             log.error("   3. 这些卷烟在分配过程中被过滤掉了（如档位全为0）");
             log.error("   4. 这些卷烟在候选查询时被过滤掉了");
             
             // 进一步检查这些卷烟是否有批发价
             checkMissingCigarettesDetails(missingCigCodes);
         } else {
             log.info("✅ 所有 info 表中的卷烟都已写回到 prediction_price 表");
         }
     }
     
     /**
      * 检查未写回卷烟的详细信息（批发价、价位段等）
      */
     private void checkMissingCigarettesDetails(Set<String> missingCigCodes) {
         log.info("\n===== 检查未写回卷烟的详细信息 =====");
         
         for (String cigCode : missingCigCodes) {
             // 检查是否有批发价
             String priceSql = "SELECT CIG_CODE, CIG_NAME, WHOLESALE_PRICE " +
                              "FROM base_cigarette_price " +
                              "WHERE CIG_CODE = ?";
             List<Map<String, Object>> priceRecords = jdbcTemplate.queryForList(priceSql, cigCode);
             
             if (priceRecords.isEmpty()) {
                 log.error("  卷烟 {} 在 base_cigarette_price 表中没有批发价数据", cigCode);
             } else {
                 BigDecimal wholesalePrice = (BigDecimal) priceRecords.get(0).get("WHOLESALE_PRICE");
                 log.warn("  卷烟 {} 有批发价: {}, 但未写回", cigCode, wholesalePrice);
                 
                 // 检查是否在候选查询结果中
                 try {
                     List<Map<String, Object>> candidates = priceBandCandidateQueryService.listOrderedPriceBandCandidates(YEAR, MONTH, WEEK_SEQ);
                     boolean inCandidates = candidates.stream()
                             .anyMatch(c -> cigCode.equals(c.get("CIG_CODE")));
                     if (!inCandidates) {
                         log.warn("    该卷烟不在候选查询结果中，可能原因：批发价不在任何价位段范围内");
                     } else {
                         log.warn("    该卷烟在候选查询结果中，但在分配或写回过程中丢失");
                     }
                 } catch (Exception e) {
                     log.warn("    无法检查候选查询结果: {}", e.getMessage());
                 }
             }
         }
    }

    @Test
    @DisplayName("完整诊断流程: 执行所有步骤并生成诊断报告")
    void fullDiagnosticWorkflow() {
        log.info("\n========================================");
        log.info("开始完整诊断流程");
        log.info("========================================\n");
        
        // 步骤1: 检查 info 表
        step1_checkInfoTableData();
        
        // 步骤2: 检查批发价表
        step2_checkPriceTableData();
        
        // 步骤3: 检查区域客户数统计表
        step3_checkRegionCustomerStatistics();
        
        // 步骤4: 检查候选卷烟查询
        step4_checkCandidateQuery();
        
        // 步骤5: 执行分配
        try {
            step5_executeAllocation();
        } catch (Exception e) {
            log.error("分配执行失败，跳过写回结果检查");
            return;
        }
        
        // 步骤6: 检查写回结果
        step6_checkWriteBackResults();
        
        log.info("\n========================================");
        log.info("完整诊断流程结束");
        log.info("========================================");
    }
}