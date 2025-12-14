package org.example.domain.service.algorithm;

import java.math.BigDecimal;
import java.util.List;

/**
 * 单层区域分配领域服务接口。
 * <p>
 * 定义了单层区域分配的核心业务逻辑，不依赖于Spring框架或持久化层。
 * </p>
 *
 * @author Robin
 * @version 1.0
 * @since 2025-12-14
 */
public interface SingleLevelDistributionService {

    /**
     * 根据目标区域、客户矩阵与预投放量计算分配矩阵。
     *
     * @param targetRegions        区域列表（顺序与结果矩阵行对应）
     * @param regionCustomerMatrix 区域客户数矩阵，维度 [regionCount][30]
     * @param targetAmount         预投放量
     * @return 分配矩阵，维度与输入一致
     */
    BigDecimal[][] distribute(List<String> targetRegions,
                              BigDecimal[][] regionCustomerMatrix,
                              BigDecimal targetAmount);
}

