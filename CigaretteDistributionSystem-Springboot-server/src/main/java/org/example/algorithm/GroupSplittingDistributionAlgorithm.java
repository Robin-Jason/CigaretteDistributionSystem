package org.example.algorithm;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

/**
 * 多区域带权重的分配算法
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

