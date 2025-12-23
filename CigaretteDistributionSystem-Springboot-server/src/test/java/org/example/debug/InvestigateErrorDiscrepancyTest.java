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
 * 调查误差分析不一致的原因
 * 
 * 问题：
 * - 直接算法测试：target=195313, actual=195326, error=13 ✅
 * - 全链路测试：expected=195313, actual=131136, error=-64177 ❌
 * 
 * 假设：
 * 1. 可能是"两周一访上浮100%"逻辑影响了客户矩阵
 * 2. 可能是写回数据库时使用了不同的客户矩阵
 */
@Slf4j
@SpringBootTest
public class InvestigateErrorDiscrepancyTest {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    public void investigateJiaoziGediaoXizhi() {
        int year = 2025;
        int month = 9;
        int weekSeq = 3;
        String cigName = "娇子(格调细支)";
        
        log.info("==================== 调查 {} 的误差不一致问题 ====================", cigName);
        
        // 1. 查询 cigarette_distribution_info 表中的配置
        String infoSql = "SELECT CIG_CODE, CIG_NAME, DELIVERY_METHOD, DELIVERY_AREA, ADV, " +
                "HIGHEST_GRADE, LOWEST_GRADE, REMARK, TAG, DELIVERY_ETYPE " +
                "FROM cigarette_distribution_info " +
                "WHERE YEAR = ? AND MONTH = ? AND WEEK_SEQ = ? AND CIG_NAME = ?";
        
        List<Map<String, Object>> infoList = jdbcTemplate.queryForList(infoSql, year, month, weekSeq, cigName);
        
        if (infoList.isEmpty()) {
            log.error("❌ 未找到 {} 的配置信息", cigName);
            return;
        }
        
        Map<String, Object> info = infoList.get(0);
        log.info("\n【配置信息】");
        log.info("  卷烟代码: {}", info.get("CIG_CODE"));
        log.info("  卷烟名称: {}", info.get("CIG_NAME"));
        log.info("  投放方式: {}", info.get("DELIVERY_METHOD"));
        log.info("  投放区域: {}", info.get("DELIVERY_AREA"));
        log.info("  目标量(ADV): {}", info.get("ADV"));
        log.info("  最高档位: {}", info.get("HIGHEST_GRADE"));
        log.info("  最低档位: {}", info.get("LOWEST_GRADE"));
        log.info("  备注: {}", info.get("REMARK"));
        log.info("  标签: {}", info.get("TAG"));
        log.info("  扩展类型: {}", info.get("DELIVERY_ETYPE"));
        
        // 2. 检查是否有"两周一访上浮100%"关键字
        String remark = (String) info.get("REMARK");
        boolean hasBoostKeyword = remark != null && remark.contains("两周一访上浮100%");
        log.info("\n【两周一访上浮检查】");
        log.info("  是否包含'两周一访上浮100%': {}", hasBoostKeyword ? "是 ✅" : "否");
        
        // 3. 查询 region_customer_statistics 表中的客户数
        String deliveryArea = (String) info.get("DELIVERY_AREA");
        String statsSql = "SELECT REGION, D30, D29, D28, D27, D26, D25, D24, D23, D22, D21, " +
                "D20, D19, D18, D17, D16, D15, D14, D13, D12, D11, " +
                "D10, D9, D8, D7, D6, D5, D4, D3, D2, D1, TOTAL " +
                "FROM region_customer_statistics " +
                "WHERE YEAR = ? AND MONTH = ? AND WEEK_SEQ = ? AND REGION = ?";
        
        List<Map<String, Object>> statsList = jdbcTemplate.queryForList(statsSql, year, month, weekSeq, deliveryArea);
        
        if (statsList.isEmpty()) {
            log.warn("⚠️  未找到区域 {} 的客户统计数据", deliveryArea);
        } else {
            Map<String, Object> stats = statsList.get(0);
            log.info("\n【区域客户统计】区域: {}", stats.get("REGION"));
            log.info("  总客户数: {}", stats.get("TOTAL"));
            
            // 计算 D25-D15 范围内的客户数
            BigDecimal totalInRange = BigDecimal.ZERO;
            for (int i = 25; i >= 15; i--) {
                BigDecimal count = (BigDecimal) stats.get("D" + i);
                if (count != null) {
                    totalInRange = totalInRange.add(count);
                }
            }
            log.info("  D25-D15 范围内客户数: {}", totalInRange);
        }
        
        // 4. 检查 customer_filter 表中是否有单周/双周客户
        String filterSql = "SELECT DISTINCT ORDER_CYCLE FROM customer_filter " +
                "WHERE YEAR = ? AND MONTH = ? AND WEEK_SEQ = ?";
        
        List<Map<String, Object>> cycleList = jdbcTemplate.queryForList(filterSql, year, month, weekSeq);
        
        log.info("\n【订单周期类型】");
        boolean hasSingleWeek = false;
        boolean hasDoubleWeek = false;
        for (Map<String, Object> cycle : cycleList) {
            String orderCycle = (String) cycle.get("ORDER_CYCLE");
            log.info("  - {}", orderCycle);
            if (orderCycle != null) {
                if (orderCycle.contains("单周")) hasSingleWeek = true;
                if (orderCycle.contains("双周")) hasDoubleWeek = true;
            }
        }
        log.info("  存在单周客户: {}", hasSingleWeek ? "是" : "否");
        log.info("  存在双周客户: {}", hasDoubleWeek ? "是" : "否");
        
        // 5. 查询 cigarette_distribution_prediction 表中的分配结果
        String predictionSql = "SELECT DELIVERY_AREA, D30, D29, D28, D27, D26, D25, D24, D23, D22, D21, " +
                "D20, D19, D18, D17, D16, D15, D14, D13, D12, D11, " +
                "D10, D9, D8, D7, D6, D5, D4, D3, D2, D1, ACTUAL_DELIVERY " +
                "FROM cigarette_distribution_prediction " +
                "WHERE YEAR = ? AND MONTH = ? AND WEEK_SEQ = ? AND CIG_NAME = ?";
        
        List<Map<String, Object>> predictionList = jdbcTemplate.queryForList(predictionSql, year, month, weekSeq, cigName);
        
        if (predictionList.isEmpty()) {
            log.warn("⚠️  未找到 {} 的分配结果", cigName);
        } else {
            log.info("\n【分配结果】共 {} 条记录", predictionList.size());
            BigDecimal totalActualDelivery = BigDecimal.ZERO;
            
            for (Map<String, Object> prediction : predictionList) {
                String region = (String) prediction.get("DELIVERY_AREA");
                BigDecimal actualDelivery = (BigDecimal) prediction.get("ACTUAL_DELIVERY");
                totalActualDelivery = totalActualDelivery.add(actualDelivery != null ? actualDelivery : BigDecimal.ZERO);
                
                log.info("  区域: {}, 实际投放: {}", region, actualDelivery);
                
                // 检查 D25-D15 范围内的分配
                log.info("    D25-D15 档位分配:");
                for (int i = 25; i >= 15; i--) {
                    BigDecimal allocation = (BigDecimal) prediction.get("D" + i);
                    if (allocation != null && allocation.compareTo(BigDecimal.ZERO) > 0) {
                        log.info("      D{}: {}", i, allocation);
                    }
                }
            }
            
            log.info("\n【总计】");
            log.info("  目标量(ADV): {}", info.get("ADV"));
            log.info("  实际投放量: {}", totalActualDelivery);
            BigDecimal error = totalActualDelivery.subtract((BigDecimal) info.get("ADV"));
            log.info("  误差: {}", error);
            log.info("  误差率: {}%", error.divide((BigDecimal) info.get("ADV"), 4, BigDecimal.ROUND_HALF_UP).multiply(new BigDecimal("100")));
        }
        
        // 6. 分析结论
        log.info("\n==================== 分析结论 ====================");
        if (hasBoostKeyword && (hasSingleWeek || hasDoubleWeek)) {
            log.info("⚠️  该卷烟配置了'两周一访上浮100%'，且存在单周/双周客户");
            log.info("   这可能导致客户矩阵被修改，从而影响实际投放量计算");
            log.info("   建议：检查 BiWeeklyVisitBoostService 的逻辑");
        } else if (hasBoostKeyword) {
            log.info("⚠️  该卷烟配置了'两周一访上浮100%'，但不存在单周/双周客户");
            log.info("   上浮逻辑不会被触发");
        } else {
            log.info("✅ 该卷烟未配置'两周一访上浮100%'");
        }
    }
}
