package org.example.fullpipeline;

import lombok.extern.slf4j.Slf4j;
import org.example.domain.service.rule.PriceBandRule;
import org.example.domain.service.rule.impl.PriceBandRuleImpl;
import org.example.infrastructure.config.price.PriceBandRuleRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.util.*;

/**
 * 价位段分配矩阵输出测试
 * 按价位段输出每个价位段的分配矩阵详情
 *
 * @author Robin
 * @since 2025-12-20
 */
@Slf4j
@SpringBootTest
@ActiveProfiles("test")
public class PriceBandMatrixOutputTest {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private PriceBandRuleRepository priceBandRuleRepository;
    
    private PriceBandRule priceBandRule;

    private static final int YEAR = 2025;
    private static final int MONTH = 9;
    private static final int WEEK_SEQ = 4;

    @Test
    public void outputPriceBandMatrices() {
        // 初始化价位段规则
        priceBandRule = new PriceBandRuleImpl(priceBandRuleRepository.getBands());
        
        log.info("==================== 价位段分配矩阵输出 ====================");
        log.info("时间：{}-{}-{}", YEAR, MONTH, WEEK_SEQ);

        // 1. 获取所有价位段卷烟及其价格
        String sql = String.format(
                "SELECT p.CIG_CODE, p.CIG_NAME, p.DELIVERY_METHOD, p.ACTUAL_DELIVERY, " +
                "       p.D30, p.D29, p.D28, p.D27, p.D26, p.D25, p.D24, p.D23, p.D22, p.D21, " +
                "       p.D20, p.D19, p.D18, p.D17, p.D16, p.D15, p.D14, p.D13, p.D12, p.D11, " +
                "       p.D10, p.D9, p.D8, p.D7, p.D6, p.D5, p.D4, p.D3, p.D2, p.D1, " +
                "       i.ADV, bp.WHOLESALE_PRICE " +
                "FROM cigarette_distribution_prediction_price p " +
                "LEFT JOIN cigarette_distribution_info i " +
                "  ON p.YEAR = i.YEAR AND p.MONTH = i.MONTH AND p.WEEK_SEQ = i.WEEK_SEQ AND p.CIG_CODE = i.CIG_CODE " +
                "LEFT JOIN base_cigarette_price bp " +
                "  ON p.CIG_CODE = bp.CIG_CODE " +
                "WHERE p.YEAR = %d AND p.MONTH = %d AND p.WEEK_SEQ = %d " +
                "ORDER BY bp.WHOLESALE_PRICE DESC, p.CIG_CODE",
                YEAR, MONTH, WEEK_SEQ);

        List<Map<String, Object>> records = jdbcTemplate.queryForList(sql);

        if (records.isEmpty()) {
            log.warn("未找到价位段分配数据");
            return;
        }

        // 2. 按价格分组（价位段）
        Map<Integer, List<Map<String, Object>>> priceBands = groupByPriceBand(records);

        // 3. 输出每个价位段的分配矩阵
        log.info("\n找到 {} 个价位段，共 {} 支卷烟", priceBands.size(), records.size());

        int bandIndex = 1;
        for (Map.Entry<Integer, List<Map<String, Object>>> entry : priceBands.entrySet()) {
            Integer avgPrice = entry.getKey();
            List<Map<String, Object>> cigarettes = entry.getValue();
            
            outputPriceBandMatrix(bandIndex++, avgPrice, cigarettes);
        }

        log.info("\n==================== 输出完成 ====================");
    }

    /**
     * 按价格分组到价位段（使用价位段规则）
     */
    private Map<Integer, List<Map<String, Object>>> groupByPriceBand(List<Map<String, Object>> records) {
        // 使用TreeMap并指定降序比较器，让价位段从高到低排列
        Map<Integer, List<Map<String, Object>>> priceBands = new TreeMap<>();

        for (Map<String, Object> record : records) {
            BigDecimal price = (BigDecimal) record.get("WHOLESALE_PRICE");
            if (price == null) {
                price = BigDecimal.ZERO;
            }
            
            // 使用价位段规则解析价位段编号
            int band = priceBandRule.resolveBand(price);
            
            // band=0表示不在任何价位段内，跳过
            if (band == 0) {
                log.warn("卷烟 {} ({}) 价格 {} 不在任何价位段内", 
                    record.get("CIG_CODE"), record.get("CIG_NAME"), price);
                continue;
            }
            
            priceBands.computeIfAbsent(band, k -> new ArrayList<>()).add(record);
        }

        return priceBands;
    }

    /**
     * 输出单个价位段的分配矩阵
     */
    private void outputPriceBandMatrix(int bandIndex, Integer bandCode, List<Map<String, Object>> cigarettes) {
        // 获取价位段定义
        PriceBandRuleRepository.PriceBandDefinition bandDef = null;
        for (PriceBandRuleRepository.PriceBandDefinition def : priceBandRuleRepository.getBands()) {
            if (def.getCode() == bandCode) {
                bandDef = def;
                break;
            }
        }
        
        String priceRange = "";
        if (bandDef != null) {
            BigDecimal min = bandDef.getMinInclusive();
            BigDecimal max = bandDef.getMaxExclusive();
            if (max == null) {
                priceRange = String.format(" (≥%s元)", min);
            } else {
                priceRange = String.format(" (%s ~ %s元)", min, max);
            }
        }
        
        log.info("\n");
        log.info("╔════════════════════════════════════════════════════════════════╗");
        log.info("║  价位段 #{} - 第{}段{}                                        ", 
            bandIndex, bandCode, priceRange);
        log.info("║  卷烟数量: {} 支                                                ", cigarettes.size());
        log.info("╚════════════════════════════════════════════════════════════════╝");

        // 1. 输出卷烟列表
        log.info("\n【卷烟列表】");
        for (int i = 0; i < cigarettes.size(); i++) {
            Map<String, Object> cig = cigarettes.get(i);
            String code = (String) cig.get("CIG_CODE");
            String name = (String) cig.get("CIG_NAME");
            BigDecimal price = (BigDecimal) cig.get("WHOLESALE_PRICE");
            BigDecimal adv = (BigDecimal) cig.get("ADV");
            BigDecimal actualDelivery = (BigDecimal) cig.get("ACTUAL_DELIVERY");
            
            BigDecimal error = actualDelivery != null && adv != null 
                ? actualDelivery.subtract(adv) 
                : BigDecimal.ZERO;
            
            double errorPercent = adv != null && adv.compareTo(BigDecimal.ZERO) > 0
                ? error.abs().divide(adv, 4, BigDecimal.ROUND_HALF_UP).multiply(new BigDecimal("100")).doubleValue()
                : 0.0;

            log.info("  {}. {} - {} ({}元)", i + 1, code, name, price);
            log.info("     预投: {}, 实际: {}, 误差: {} ({}%)",
                    adv, actualDelivery, error, String.format("%.2f", errorPercent));
        }

        // 2. 输出完整的30档位分配矩阵
        log.info("\n【30档位完整分配矩阵】(D30→D1)");
        log.info("注: 每行显示10个档位，共3行");
        
        for (int i = 0; i < cigarettes.size(); i++) {
            Map<String, Object> cig = cigarettes.get(i);
            String code = (String) cig.get("CIG_CODE");
            String name = (String) cig.get("CIG_NAME");
            
            log.info("  {}. {} ({})", i + 1, code, name);
            
            // 第1行: D30 ~ D21
            StringBuilder line1 = new StringBuilder("     D30-D21: ");
            for (int g = 30; g >= 21; g--) {
                BigDecimal value = (BigDecimal) cig.get("D" + g);
                if (value == null) value = BigDecimal.ZERO;
                line1.append(String.format("%4d", value.intValue()));
            }
            log.info(line1.toString());
            
            // 第2行: D20 ~ D11
            StringBuilder line2 = new StringBuilder("     D20-D11: ");
            for (int g = 20; g >= 11; g--) {
                BigDecimal value = (BigDecimal) cig.get("D" + g);
                if (value == null) value = BigDecimal.ZERO;
                line2.append(String.format("%4d", value.intValue()));
            }
            log.info(line2.toString());
            
            // 第3行: D10 ~ D1
            StringBuilder line3 = new StringBuilder("     D10-D1:  ");
            for (int g = 10; g >= 1; g--) {
                BigDecimal value = (BigDecimal) cig.get("D" + g);
                if (value == null) value = BigDecimal.ZERO;
                line3.append(String.format("%4d", value.intValue()));
            }
            log.info(line3.toString());
            log.info("");  // 空行分隔
        }

        // 3. 输出档位统计摘要
        log.info("\n【档位统计摘要】");
        
        // 找出非零档位范围
        int highestNonZeroGrade = -1;
        int lowestNonZeroGrade = -1;
        
        for (int g = 30; g >= 1; g--) {
            boolean hasNonZero = false;
            for (Map<String, Object> cig : cigarettes) {
                BigDecimal value = (BigDecimal) cig.get("D" + g);
                if (value != null && value.compareTo(BigDecimal.ZERO) > 0) {
                    hasNonZero = true;
                    break;
                }
            }
            if (hasNonZero) {
                if (highestNonZeroGrade == -1) {
                    highestNonZeroGrade = g;
                }
                lowestNonZeroGrade = g;
            }
        }

        log.info("  非零档位范围: D{} ~ D{}", highestNonZeroGrade, lowestNonZeroGrade);
        
        // 截断信息
        if (lowestNonZeroGrade > 1) {
            log.info("  已截断档位: D{} ~ D1 (共{}个档位)", lowestNonZeroGrade - 1, lowestNonZeroGrade - 1);
        } else {
            log.info("  未截断");
        }
    }
}

