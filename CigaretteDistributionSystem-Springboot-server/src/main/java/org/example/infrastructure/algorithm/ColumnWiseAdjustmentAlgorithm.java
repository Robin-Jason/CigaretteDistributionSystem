package org.example.infrastructure.algorithm;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.List;

/**
 * 多区域无权重分配算法
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

