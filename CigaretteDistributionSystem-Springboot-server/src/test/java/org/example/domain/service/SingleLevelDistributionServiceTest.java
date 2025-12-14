package org.example.domain.service;

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
 * 单层区域分配领域服务单元测试
 * <p>
 * 验证领域服务的功能正确性，不依赖Spring容器。
 * </p>
 *
 * @author Robin
 * @version 1.0
 * @since 2025-12-14
 */
class SingleLevelDistributionServiceTest {

    private SingleLevelDistributionService service;

    @BeforeEach
    void setUp() {
        service = new SingleLevelDistributionServiceImpl();
    }

    @Test
    void testDistribute_SimpleCase() {
        // 准备测试数据
        List<String> targetRegions = Collections.singletonList("区域1");
        BigDecimal[][] regionCustomerMatrix = new BigDecimal[1][30];
        Arrays.fill(regionCustomerMatrix[0], BigDecimal.ZERO);
        regionCustomerMatrix[0][0] = new BigDecimal("100"); // HG档位有100个客户
        regionCustomerMatrix[0][1] = new BigDecimal("50");  // D29档位有50个客户
        
        BigDecimal targetAmount = new BigDecimal("150");

        // 执行分配
        BigDecimal[][] result = service.distribute(targetRegions, regionCustomerMatrix, targetAmount);

        // 验证结果
        assertNotNull(result);
        assertEquals(1, result.length, "应该只有一行（单区域）");
        assertEquals(30, result[0].length, "应该有30个档位");
        
        // 验证分配结果：应该分配HG档位1次（100）+ D29档位1次（50）= 150
        BigDecimal actualAmount = result[0][0].multiply(regionCustomerMatrix[0][0])
                .add(result[0][1].multiply(regionCustomerMatrix[0][1]));
        assertTrue(actualAmount.compareTo(targetAmount) >= 0, 
                "实际分配量应该大于等于目标量");
    }

    @Test
    void testDistribute_ExactMatch() {
        // 准备测试数据：目标量恰好等于HG档位客户数
        List<String> targetRegions = Collections.singletonList("区域1");
        BigDecimal[][] regionCustomerMatrix = new BigDecimal[1][30];
        Arrays.fill(regionCustomerMatrix[0], BigDecimal.ZERO);
        regionCustomerMatrix[0][0] = new BigDecimal("100"); // HG档位有100个客户
        
        BigDecimal targetAmount = new BigDecimal("100");

        // 执行分配
        BigDecimal[][] result = service.distribute(targetRegions, regionCustomerMatrix, targetAmount);

        // 验证结果
        assertNotNull(result);
        assertEquals(1, result[0][0].intValue(), "HG档位应该分配1");
        assertEquals(0, result[0][1].intValue(), "D29档位应该分配0");
        
        // 验证实际分配量
        BigDecimal actualAmount = result[0][0].multiply(regionCustomerMatrix[0][0]);
        assertEquals(0, actualAmount.compareTo(targetAmount), 
                "实际分配量应该等于目标量");
    }

    @Test
    void testDistribute_InvalidInput_EmptyRegions() {
        // 测试空区域列表
        BigDecimal[][] regionCustomerMatrix = new BigDecimal[1][30];
        BigDecimal targetAmount = new BigDecimal("100");

        BigDecimal[][] result = service.distribute(Collections.emptyList(), regionCustomerMatrix, targetAmount);

        assertNotNull(result);
        assertEquals(0, result.length, "应该返回空矩阵");
    }

    @Test
    void testDistribute_InvalidInput_NullMatrix() {
        // 测试空矩阵
        List<String> targetRegions = Collections.singletonList("区域1");
        BigDecimal targetAmount = new BigDecimal("100");

        BigDecimal[][] result = service.distribute(targetRegions, null, targetAmount);

        assertNotNull(result);
        assertEquals(0, result.length, "应该返回空矩阵");
    }

    @Test
    void testDistribute_InvalidInput_ZeroTarget() {
        // 测试零目标量
        List<String> targetRegions = Collections.singletonList("区域1");
        BigDecimal[][] regionCustomerMatrix = new BigDecimal[1][30];
        Arrays.fill(regionCustomerMatrix[0], BigDecimal.ZERO);
        regionCustomerMatrix[0][0] = new BigDecimal("100");

        BigDecimal[][] result = service.distribute(targetRegions, regionCustomerMatrix, BigDecimal.ZERO);

        assertNotNull(result);
        assertEquals(0, result.length, "应该返回空矩阵");
    }

    @Test
    void testDistribute_MonotonicConstraint() {
        // 验证单调性约束：D30 >= D29 >= ... >= D1
        List<String> targetRegions = Collections.singletonList("区域1");
        BigDecimal[][] regionCustomerMatrix = new BigDecimal[1][30];
        Arrays.fill(regionCustomerMatrix[0], BigDecimal.ZERO);
        regionCustomerMatrix[0][0] = new BigDecimal("100");
        regionCustomerMatrix[0][1] = new BigDecimal("50");
        regionCustomerMatrix[0][2] = new BigDecimal("30");
        
        BigDecimal targetAmount = new BigDecimal("500");

        // 执行分配
        BigDecimal[][] result = service.distribute(targetRegions, regionCustomerMatrix, targetAmount);

        // 验证单调性约束
        for (int i = 1; i < 30; i++) {
            assertTrue(result[0][i].compareTo(result[0][i - 1]) <= 0,
                    String.format("档位D%d应该小于等于D%d", 30 - i, 31 - i));
        }
    }

    @Test
    void testDistribute_AllZeroCustomers() {
        // 测试所有档位客户数都为0的情况
        // 注意：领域服务在distribute方法中捕获了异常，返回已初始化的矩阵（全0），而不是抛出异常
        List<String> targetRegions = Collections.singletonList("区域1");
        BigDecimal[][] regionCustomerMatrix = new BigDecimal[1][30];
        Arrays.fill(regionCustomerMatrix[0], BigDecimal.ZERO);
        
        BigDecimal targetAmount = new BigDecimal("100");

        // 领域服务捕获异常后返回已初始化的矩阵（与原始实现行为一致）
        BigDecimal[][] result = service.distribute(targetRegions, regionCustomerMatrix, targetAmount);
        assertNotNull(result);
        assertEquals(1, result.length, "应该返回1行（单区域）");
        assertEquals(30, result[0].length, "应该有30个档位");
        // 验证所有档位都是0（因为异常被捕获，矩阵保持初始状态）
        for (int i = 0; i < 30; i++) {
            assertEquals(0, result[0][i].intValue(), "档位" + i + "应该为0");
        }
    }

    @Test
    void testDistribute_LargeTarget() {
        // 测试大目标量
        List<String> targetRegions = Collections.singletonList("区域1");
        BigDecimal[][] regionCustomerMatrix = new BigDecimal[1][30];
        Arrays.fill(regionCustomerMatrix[0], BigDecimal.ZERO);
        regionCustomerMatrix[0][0] = new BigDecimal("1000");
        regionCustomerMatrix[0][1] = new BigDecimal("500");
        regionCustomerMatrix[0][2] = new BigDecimal("300");
        
        BigDecimal targetAmount = new BigDecimal("10000");

        // 执行分配
        BigDecimal[][] result = service.distribute(targetRegions, regionCustomerMatrix, targetAmount);

        // 验证结果不为空
        assertNotNull(result);
        assertEquals(1, result.length);
        assertEquals(30, result[0].length);
        
        // 验证实际分配量接近目标量
        BigDecimal actualAmount = BigDecimal.ZERO;
        for (int i = 0; i < 30; i++) {
            if (regionCustomerMatrix[0][i] != null && result[0][i] != null) {
                actualAmount = actualAmount.add(result[0][i].multiply(regionCustomerMatrix[0][i]));
            }
        }
        assertTrue(actualAmount.compareTo(targetAmount) >= 0, 
                "实际分配量应该大于等于目标量");
    }
}

