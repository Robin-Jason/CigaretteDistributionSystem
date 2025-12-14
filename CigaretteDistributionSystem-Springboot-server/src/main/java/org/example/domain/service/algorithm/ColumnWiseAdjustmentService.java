package org.example.domain.service.algorithm;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.List;

/**
 * 多区域无权重分配领域服务接口。
 * <p>
 * 定义了多区域无权重分配的核心业务逻辑，不依赖于Spring框架或持久化层。
 * </p>
 *
 * @author Robin
 * @version 1.0
 * @since 2025-12-14
 */
public interface ColumnWiseAdjustmentService {

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

