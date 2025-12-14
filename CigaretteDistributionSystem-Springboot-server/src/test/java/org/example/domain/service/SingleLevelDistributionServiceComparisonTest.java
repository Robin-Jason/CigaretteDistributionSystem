package org.example.domain.service;

import org.example.infrastructure.algorithm.impl.DefaultSingleLevelDistributionAlgorithm;
import org.example.domain.service.algorithm.SingleLevelDistributionService;
import org.example.domain.service.algorithm.impl.SingleLevelDistributionServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 领域服务与原始算法对比测试
 * <p>
 * 验证领域服务的计算结果与原始算法实现完全一致。
 * </p>
 *
 * @author Robin
 * @version 1.0
 * @since 2025-12-14
 */
class SingleLevelDistributionServiceComparisonTest {

    private SingleLevelDistributionService domainService;
    private DefaultSingleLevelDistributionAlgorithm originalAlgorithm;

    @BeforeEach
    void setUp() {
        domainService = new SingleLevelDistributionServiceImpl();
        originalAlgorithm = new DefaultSingleLevelDistributionAlgorithm();
    }

    @Test
    void testCompareResults_SimpleCase() {
        // 准备测试数据
        List<String> targetRegions = Collections.singletonList("区域1");
        BigDecimal[][] regionCustomerMatrix = new BigDecimal[1][30];
        Arrays.fill(regionCustomerMatrix[0], BigDecimal.ZERO);
        regionCustomerMatrix[0][0] = new BigDecimal("100"); // HG档位
        regionCustomerMatrix[0][1] = new BigDecimal("50");  // D29档位
        regionCustomerMatrix[0][2] = new BigDecimal("30");  // D28档位
        
        BigDecimal targetAmount = new BigDecimal("500");

        // 执行两个实现
        BigDecimal[][] domainResult = domainService.distribute(targetRegions, regionCustomerMatrix, targetAmount);
        BigDecimal[][] originalResult = originalAlgorithm.distribute(targetRegions, regionCustomerMatrix, targetAmount);

        // 验证结果维度一致
        assertEquals(originalResult.length, domainResult.length, "结果行数应该一致");
        assertEquals(originalResult[0].length, domainResult[0].length, "结果列数应该一致");

        // 验证分配值一致
        for (int i = 0; i < 30; i++) {
            assertEquals(0, originalResult[0][i].compareTo(domainResult[0][i]),
                    String.format("档位D%d的分配值应该一致", 30 - i));
        }
    }

    @Test
    void testCompareResults_ExactMatch() {
        // 测试恰好等于目标的情况
        List<String> targetRegions = Collections.singletonList("区域1");
        BigDecimal[][] regionCustomerMatrix = new BigDecimal[1][30];
        Arrays.fill(regionCustomerMatrix[0], BigDecimal.ZERO);
        regionCustomerMatrix[0][0] = new BigDecimal("1000");
        
        BigDecimal targetAmount = new BigDecimal("1000");

        BigDecimal[][] domainResult = domainService.distribute(targetRegions, regionCustomerMatrix, targetAmount);
        BigDecimal[][] originalResult = originalAlgorithm.distribute(targetRegions, regionCustomerMatrix, targetAmount);

        // 验证结果完全一致
        assertArrayEquals(originalResult[0], domainResult[0], "恰好等于目标时结果应该完全一致");
    }

    @Test
    void testCompareResults_LargeTarget() {
        // 测试大目标量
        List<String> targetRegions = Collections.singletonList("区域1");
        BigDecimal[][] regionCustomerMatrix = new BigDecimal[1][30];
        Arrays.fill(regionCustomerMatrix[0], BigDecimal.ZERO);
        for (int i = 0; i < 10; i++) {
            regionCustomerMatrix[0][i] = new BigDecimal(1000 - i * 50);
        }
        
        BigDecimal targetAmount = new BigDecimal("50000");

        BigDecimal[][] domainResult = domainService.distribute(targetRegions, regionCustomerMatrix, targetAmount);
        BigDecimal[][] originalResult = originalAlgorithm.distribute(targetRegions, regionCustomerMatrix, targetAmount);

        // 验证结果维度一致
        assertEquals(originalResult.length, domainResult.length);
        assertEquals(originalResult[0].length, domainResult[0].length);

        // 验证分配值一致（允许小的数值误差）
        for (int i = 0; i < 30; i++) {
            BigDecimal diff = originalResult[0][i].subtract(domainResult[0][i]).abs();
            assertTrue(diff.compareTo(new BigDecimal("0.01")) < 0,
                    String.format("档位D%d的分配值差异应该小于0.01", 30 - i));
        }
    }

    @Test
    void testCompareResults_ActualAmount() {
        // 验证实际分配量的计算一致性
        List<String> targetRegions = Collections.singletonList("区域1");
        BigDecimal[][] regionCustomerMatrix = new BigDecimal[1][30];
        Arrays.fill(regionCustomerMatrix[0], BigDecimal.ZERO);
        regionCustomerMatrix[0][0] = new BigDecimal("100");
        regionCustomerMatrix[0][1] = new BigDecimal("50");
        regionCustomerMatrix[0][2] = new BigDecimal("30");
        
        BigDecimal targetAmount = new BigDecimal("2000");

        BigDecimal[][] domainResult = domainService.distribute(targetRegions, regionCustomerMatrix, targetAmount);
        BigDecimal[][] originalResult = originalAlgorithm.distribute(targetRegions, regionCustomerMatrix, targetAmount);

        // 计算实际分配量
        BigDecimal domainActual = BigDecimal.ZERO;
        BigDecimal originalActual = BigDecimal.ZERO;
        
        for (int i = 0; i < 30; i++) {
            if (regionCustomerMatrix[0][i] != null) {
                domainActual = domainActual.add(domainResult[0][i].multiply(regionCustomerMatrix[0][i]));
                originalActual = originalActual.add(originalResult[0][i].multiply(regionCustomerMatrix[0][i]));
            }
        }

        // 验证实际分配量一致
        assertEquals(0, originalActual.compareTo(domainActual), 
                "实际分配量应该完全一致");
    }
}

