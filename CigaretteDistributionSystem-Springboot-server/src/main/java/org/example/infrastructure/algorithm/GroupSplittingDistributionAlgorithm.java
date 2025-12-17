package org.example.infrastructure.algorithm;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

/**
 * 多区域带权重的分配算法（历史版本最新算法，实现已迁移到domain，仅用于对比与回归验证）：
 * <ol>
 *     <li>分组拆分阶段：根据分组与比例，将目标量拆分到各分组。</li>
 *     <li>分组独立分配阶段：对每个分组独立运行分配算法。</li>
 *     <li>方案选择：从多个候选方案中选择误差最小的方案（误差相同时选择编号较大的方案）。</li>
 *     <li>非递增约束：确保每一区域的档位为非递增序列（D30 >= ... >= D1）。</li>
 * </ol>    
 */
public interface GroupSplittingDistributionAlgorithm {

    /**
     * 根据分组与比例计算分配矩阵。
     *
     * @param regions               目标区域列表
     * @param customerMatrix        区域客户矩阵 [regionCount][30]
     * @param targetAmount          预投放量
     * @param groupingFunction      区域 -> 分组 ID 的映射函数
     * @param groupRatios           分组比例（总量可不为 1，算法会自动归一化；允许任意数量分组）
     * @return 分配矩阵
     */
    BigDecimal[][] distribute(List<String> regions,
                              BigDecimal[][] customerMatrix,
                              BigDecimal targetAmount,
                              Function<String, String> groupingFunction,
                              Map<String, BigDecimal> groupRatios);
}

