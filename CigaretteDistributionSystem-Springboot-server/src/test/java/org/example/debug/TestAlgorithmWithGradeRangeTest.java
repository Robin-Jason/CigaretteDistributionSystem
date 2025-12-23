package org.example.debug;

import lombok.extern.slf4j.Slf4j;
import org.example.domain.model.valueobject.GradeRange;
import org.example.domain.service.algorithm.SingleLevelDistributionService;
import org.example.domain.service.algorithm.impl.SingleLevelDistributionServiceImpl;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * 测试算法对 GradeRange 的处理
 */
@Slf4j
public class TestAlgorithmWithGradeRangeTest {

    @Test
    public void testSingleLevelWithGradeRange() {
        SingleLevelDistributionService service = new SingleLevelDistributionServiceImpl();
        
        // 模拟娇子(格调细支)的情况
        // HG: D25 (索引5), LG: D15 (索引15)
        // ADV: 195313.00
        
        List<String> regions = Collections.singletonList("全市");
        BigDecimal[][] customerMatrix = new BigDecimal[1][30];
        Arrays.fill(customerMatrix[0], BigDecimal.ZERO);
        
        // 在范围内设置客户数（索引5-15）
        customerMatrix[0][5] = new BigDecimal("449");   // D25
        customerMatrix[0][6] = new BigDecimal("449");   // D24
        customerMatrix[0][7] = new BigDecimal("596");   // D23
        customerMatrix[0][8] = new BigDecimal("594");   // D22
        customerMatrix[0][9] = new BigDecimal("597");   // D21
        customerMatrix[0][10] = new BigDecimal("598");  // D20
        customerMatrix[0][11] = new BigDecimal("597");  // D19
        customerMatrix[0][12] = new BigDecimal("747");  // D18
        customerMatrix[0][13] = new BigDecimal("747");  // D17
        customerMatrix[0][14] = new BigDecimal("745");  // D16
        customerMatrix[0][15] = new BigDecimal("745");  // D15
        
        BigDecimal targetAmount = new BigDecimal("195313");
        GradeRange gradeRange = GradeRange.of("D25", "D15");
        
        log.info("测试参数:");
        log.info("  档位范围: {}", gradeRange);
        log.info("  目标量: {}", targetAmount);
        log.info("  范围内客户数总和: {}", calculateTotalCustomers(customerMatrix[0], 5, 15));
        
        // 执行分配
        BigDecimal[][] result = service.distribute(regions, customerMatrix, targetAmount, gradeRange);
        
        log.info("\n分配结果:");
        log.info("  结果矩阵维度: {}x{}", result.length, result.length > 0 ? result[0].length : 0);
        
        if (result.length > 0 && result[0].length > 0) {
            // 检查范围内的分配
            BigDecimal totalAllocation = BigDecimal.ZERO;
            for (int i = 5; i <= 15; i++) {
                if (result[0][i].compareTo(BigDecimal.ZERO) > 0) {
                    log.info("  档位索引 {} (D{}): 分配 {}", i, 30 - i, result[0][i]);
                    totalAllocation = totalAllocation.add(result[0][i].multiply(customerMatrix[0][i]));
                }
            }
            
            log.info("  范围内实际投放量: {}", totalAllocation);
            log.info("  误差: {}", targetAmount.subtract(totalAllocation).abs());
            
            // 检查范围外是否为0
            boolean rangeOutsideIsZero = true;
            for (int i = 0; i < 5; i++) {
                if (result[0][i].compareTo(BigDecimal.ZERO) != 0) {
                    rangeOutsideIsZero = false;
                    log.error("❌ 范围外档位 {} 不为0: {}", i, result[0][i]);
                }
            }
            for (int i = 16; i < 30; i++) {
                if (result[0][i].compareTo(BigDecimal.ZERO) != 0) {
                    rangeOutsideIsZero = false;
                    log.error("❌ 范围外档位 {} 不为0: {}", i, result[0][i]);
                }
            }
            
            if (rangeOutsideIsZero) {
                log.info("✅ 范围外档位全为0");
            }
            
            if (totalAllocation.compareTo(BigDecimal.ZERO) == 0) {
                log.error("❌ 范围内实际投放量为0！");
            } else {
                log.info("✅ 范围内有分配");
            }
        } else {
            log.error("❌ 返回空矩阵！");
        }
    }
    
    private BigDecimal calculateTotalCustomers(BigDecimal[] customerRow, int startIndex, int endIndex) {
        BigDecimal total = BigDecimal.ZERO;
        for (int i = startIndex; i <= endIndex && i < customerRow.length; i++) {
            if (customerRow[i] != null) {
                total = total.add(customerRow[i]);
            }
        }
        return total;
    }
}
