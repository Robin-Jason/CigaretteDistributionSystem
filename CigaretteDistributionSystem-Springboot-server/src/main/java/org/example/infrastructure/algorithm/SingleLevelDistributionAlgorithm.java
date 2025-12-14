package org.example.infrastructure.algorithm;

import java.math.BigDecimal;
import java.util.List;

/**
 * 单层区域分配算法抽象。
 * <p>
 * 典型场景：按档位投放 / 按价位段自选投放（默认“全市”）等，
 * 仅需要根据客户矩阵和目标量以及输出HG~LG列分配矩阵。
 */
public interface SingleLevelDistributionAlgorithm {

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

