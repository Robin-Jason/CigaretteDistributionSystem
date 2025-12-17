package org.example.infrastructure.algorithm;

import java.math.BigDecimal;
import java.util.List;

/**
 * 单层区域分配算法抽象。（历史版本最新算法，实现已迁移到domain，仅用于对比与回归验证）：
 * <ol>
 *     <li>粗调阶段：从最高档位（HG）到最低档位（LG），多轮逐档位+1，直到刚好超出目标。</li>
 *     <li>高档位微调阶段：撤销粗调方案一次+1操作，基于此进行迭代微调，生成多个候选方案。</li>
 *     <li>方案选择：从4个候选方案中选择误差最小的方案（误差相同时选择编号较大的方案）。</li>
 *     <li>非递增约束：确保每一区域的档位为非递增序列（D30 >= ... >= D1）。</li>
 * </ol>    
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

