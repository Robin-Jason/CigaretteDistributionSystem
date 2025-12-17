package org.example.infrastructure.algorithm;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.List;

/**
 * 多区域无权重分配算法（历史版本最新算法，实现已迁移到domain，仅用于对比与回归验证）：
 * <ol>
 *     <li>粗调阶段：从最高档位（HG）到最低档位（LG），多轮逐档位列+1（整列+1），直到刚好超出目标。</li>
 *     <li>高档位微调阶段：撤销粗调方案最后一次档位列+1操作，基于此进行迭代微调，生成多个候选方案。</li>
 *     <li>方案选择：从4个候选方案中选择误差最小的方案（误差相同时选择编号较大的方案）。</li>
 *     <li>非递增约束：确保每一区域的档位为非递增序列（D30 >= ... >= D1）。</li>
 * </ol>    
 */
public interface ColumnWiseAdjustmentAlgorithm {

    /**
     * 执行整列粗调 + 排序微调。
     *
     * @param segments              目标段列表（区域、业态、标签等）
     * @param customerMatrix        段对应的客户矩阵 [count][30]
     * @param targetAmount          预投放量
     * @param segmentComparator     段排序器，用于微调阶段（可为 null，默认按客户总量）
     * @return 分配矩阵
     */
    BigDecimal[][] distribute(List<String> segments,
                              BigDecimal[][] customerMatrix,
                              BigDecimal targetAmount,
                              Comparator<Integer> segmentComparator);
}

