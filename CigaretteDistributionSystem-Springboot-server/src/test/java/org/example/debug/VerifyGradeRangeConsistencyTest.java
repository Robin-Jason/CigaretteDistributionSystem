package org.example.debug;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * 验证写入数据库的档位范围是否与info表的HIGHEST_GRADE和LOWEST_GRADE一致
 * 
 * 问题：
 * - 用户发现写入数据有些卷烟写入的范围和info表HG/LG不一致
 * - 这可能导致实际投放量计算错误
 */
@Slf4j
@SpringBootTest
public class VerifyGradeRangeConsistencyTest {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    public void verifyAllCigarettesGradeRange() {
        int year = 2025;
        int month = 9;
        int weekSeq = 3;
        
        log.info("==================== 验证所有卷烟的档位范围一致性 ====================");
        
        // 1. 查询所有标准分配卷烟的配置
        String infoSql = "SELECT CIG_CODE, CIG_NAME, DELIVERY_METHOD, ADV, " +
                "HIGHEST_GRADE, LOWEST_GRADE " +
                "FROM cigarette_distribution_info " +
                "WHERE YEAR = ? AND MONTH = ? AND WEEK_SEQ = ? " +
                "AND DELIVERY_METHOD != '按价位段自选投放' " +
                "ORDER BY CIG_NAME";
        
        List<Map<String, Object>> infoList = jdbcTemplate.queryForList(infoSql, year, month, weekSeq);
        
        log.info("共 {} 种标准分配卷烟", infoList.size());
        
        int inconsistentCount = 0;
        
        for (Map<String, Object> info : infoList) {
            String cigCode = (String) info.get("CIG_CODE");
            String cigName = (String) info.get("CIG_NAME");
            String highestGrade = (String) info.get("HIGHEST_GRADE");
            String lowestGrade = (String) info.get("LOWEST_GRADE");
            BigDecimal adv = (BigDecimal) info.get("ADV");
            
            // 2. 查询该卷烟在prediction表中的分配数据
            String predictionSql = "SELECT D30, D29, D28, D27, D26, D25, D24, D23, D22, D21, " +
                    "D20, D19, D18, D17, D16, D15, D14, D13, D12, D11, " +
                    "D10, D9, D8, D7, D6, D5, D4, D3, D2, D1, ACTUAL_DELIVERY " +
                    "FROM cigarette_distribution_prediction " +
                    "WHERE YEAR = ? AND MONTH = ? AND WEEK_SEQ = ? AND CIG_CODE = ?";
            
            List<Map<String, Object>> predictionList = jdbcTemplate.queryForList(
                    predictionSql, year, month, weekSeq, cigCode);
            
            if (predictionList.isEmpty()) {
                log.warn("⚠️  {} - 未找到分配数据", cigName);
                continue;
            }
            
            // 3. 分析实际写入的档位范围
            int actualHighestIndex = -1;  // 最高档位索引（D30=0, D29=1, ..., D1=29）
            int actualLowestIndex = -1;   // 最低档位索引
            BigDecimal totalActualDelivery = BigDecimal.ZERO;
            
            for (Map<String, Object> prediction : predictionList) {
                BigDecimal actualDelivery = (BigDecimal) prediction.get("ACTUAL_DELIVERY");
                if (actualDelivery != null) {
                    totalActualDelivery = totalActualDelivery.add(actualDelivery);
                }
                
                for (int i = 0; i < 30; i++) {
                    String gradeColumn = "D" + (30 - i);
                    BigDecimal allocation = (BigDecimal) prediction.get(gradeColumn);
                    
                    if (allocation != null && allocation.compareTo(BigDecimal.ZERO) > 0) {
                        if (actualHighestIndex == -1) {
                            actualHighestIndex = i;
                        }
                        actualLowestIndex = i;
                    }
                }
            }
            
            // 4. 转换为档位名称
            String actualHighestGrade = actualHighestIndex >= 0 ? "D" + (30 - actualHighestIndex) : "无";
            String actualLowestGrade = actualLowestIndex >= 0 ? "D" + (30 - actualLowestIndex) : "无";
            
            // 5. 比较是否一致
            boolean isConsistent = actualHighestGrade.equals(highestGrade) && 
                                   actualLowestGrade.equals(lowestGrade);
            
            if (!isConsistent) {
                inconsistentCount++;
                log.error("❌ {} - 档位范围不一致", cigName);
                log.error("   Info表配置: {} ~ {}", highestGrade, lowestGrade);
                log.error("   实际写入:   {} ~ {}", actualHighestGrade, actualLowestGrade);
                log.error("   目标量(ADV): {}", adv);
                log.error("   实际投放:    {}", totalActualDelivery);
                log.error("   误差:        {}", totalActualDelivery.subtract(adv));
                log.error("");
            } else {
                log.info("✅ {} - 档位范围一致: {} ~ {}, 目标={}, 实际={}, 误差={}", 
                        cigName, highestGrade, lowestGrade, adv, totalActualDelivery, 
                        totalActualDelivery.subtract(adv));
            }
        }
        
        log.info("\n==================== 验证结果 ====================");
        log.info("总卷烟数: {}", infoList.size());
        log.info("档位范围不一致: {} 种", inconsistentCount);
        log.info("档位范围一致: {} 种", infoList.size() - inconsistentCount);
        
        if (inconsistentCount > 0) {
            log.error("\n⚠️  发现 {} 种卷烟的档位范围不一致！", inconsistentCount);
            log.error("这可能导致实际投放量计算错误");
        } else {
            log.info("\n✅ 所有卷烟的档位范围都一致");
        }
    }
    
    @Test
    public void verifyJiaoziGediaoXizhi() {
        int year = 2025;
        int month = 9;
        int weekSeq = 3;
        String cigName = "娇子(格调细支)";
        
        log.info("==================== 验证 {} 的档位范围 ====================", cigName);
        
        // 1. 查询info表配置
        String infoSql = "SELECT CIG_CODE, CIG_NAME, ADV, HIGHEST_GRADE, LOWEST_GRADE " +
                "FROM cigarette_distribution_info " +
                "WHERE YEAR = ? AND MONTH = ? AND WEEK_SEQ = ? AND CIG_NAME = ?";
        
        List<Map<String, Object>> infoList = jdbcTemplate.queryForList(infoSql, year, month, weekSeq, cigName);
        
        if (infoList.isEmpty()) {
            log.error("❌ 未找到 {} 的配置", cigName);
            return;
        }
        
        Map<String, Object> info = infoList.get(0);
        String cigCode = (String) info.get("CIG_CODE");
        String highestGrade = (String) info.get("HIGHEST_GRADE");
        String lowestGrade = (String) info.get("LOWEST_GRADE");
        BigDecimal adv = (BigDecimal) info.get("ADV");
        
        log.info("【Info表配置】");
        log.info("  卷烟代码: {}", cigCode);
        log.info("  目标量(ADV): {}", adv);
        log.info("  最高档位(HG): {}", highestGrade);
        log.info("  最低档位(LG): {}", lowestGrade);
        
        // 2. 查询prediction表中的分配数据
        String predictionSql = "SELECT DELIVERY_AREA, " +
                "D30, D29, D28, D27, D26, D25, D24, D23, D22, D21, " +
                "D20, D19, D18, D17, D16, D15, D14, D13, D12, D11, " +
                "D10, D9, D8, D7, D6, D5, D4, D3, D2, D1, ACTUAL_DELIVERY " +
                "FROM cigarette_distribution_prediction " +
                "WHERE YEAR = ? AND MONTH = ? AND WEEK_SEQ = ? AND CIG_CODE = ?";
        
        List<Map<String, Object>> predictionList = jdbcTemplate.queryForList(
                predictionSql, year, month, weekSeq, cigCode);
        
        if (predictionList.isEmpty()) {
            log.error("❌ 未找到 {} 的分配数据", cigName);
            return;
        }
        
        log.info("\n【Prediction表数据】共 {} 条记录", predictionList.size());
        
        BigDecimal totalActualDelivery = BigDecimal.ZERO;
        int actualHighestIndex = -1;
        int actualLowestIndex = -1;
        
        for (Map<String, Object> prediction : predictionList) {
            String region = (String) prediction.get("DELIVERY_AREA");
            BigDecimal actualDelivery = (BigDecimal) prediction.get("ACTUAL_DELIVERY");
            totalActualDelivery = totalActualDelivery.add(actualDelivery != null ? actualDelivery : BigDecimal.ZERO);
            
            log.info("  区域: {}, 实际投放: {}", region, actualDelivery);
            
            // 找出有分配的档位
            StringBuilder allocatedGrades = new StringBuilder("    分配档位: ");
            for (int i = 0; i < 30; i++) {
                String gradeColumn = "D" + (30 - i);
                BigDecimal allocation = (BigDecimal) prediction.get(gradeColumn);
                
                if (allocation != null && allocation.compareTo(BigDecimal.ZERO) > 0) {
                    if (actualHighestIndex == -1) {
                        actualHighestIndex = i;
                    }
                    actualLowestIndex = i;
                    allocatedGrades.append(gradeColumn).append("=").append(allocation).append(", ");
                }
            }
            
            if (allocatedGrades.length() > 15) {
                log.info(allocatedGrades.substring(0, allocatedGrades.length() - 2));
            } else {
                log.info("    分配档位: 无");
            }
        }
        
        String actualHighestGrade = actualHighestIndex >= 0 ? "D" + (30 - actualHighestIndex) : "无";
        String actualLowestGrade = actualLowestIndex >= 0 ? "D" + (30 - actualLowestIndex) : "无";
        
        log.info("\n【对比结果】");
        log.info("  Info表配置范围: {} ~ {}", highestGrade, lowestGrade);
        log.info("  实际写入范围:   {} ~ {}", actualHighestGrade, actualLowestGrade);
        log.info("  目标量(ADV):    {}", adv);
        log.info("  实际投放量:     {}", totalActualDelivery);
        log.info("  误差:           {}", totalActualDelivery.subtract(adv));
        
        boolean isConsistent = actualHighestGrade.equals(highestGrade) && 
                               actualLowestGrade.equals(lowestGrade);
        
        if (isConsistent) {
            log.info("\n✅ 档位范围一致");
        } else {
            log.error("\n❌ 档位范围不一致！");
            log.error("   这可能是导致误差的原因");
        }
    }
}
