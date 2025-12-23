package org.example.application.service.calculate;

import java.util.List;
import java.util.Map;

/**
 * 按价位段自选投放分配服务接口。
 *
 * <p>职责：基于价位段候选卷烟列表，完成按价位段自选投放场景下的完整分配流程
 *（初分配、价位段矩阵截断与微调、写回 prediction_price 分区表）。</p>
 *
 * @author Robin
 * @since 2025-12-18
 */
public interface PriceBandAllocationService {

    /**
     * 对指定时间分区内"按价位段自选投放"的卷烟执行分配策略（内部查询候选卷烟）。
     *
     * @param year    年份
     * @param month   月份
     * @param weekSeq 周序号
     * @deprecated 建议使用 {@link #allocateForPriceBand(List, Integer, Integer, Integer)} 接收已过滤的卷烟列表
     */
    @Deprecated
    void allocateForPriceBand(Integer year, Integer month, Integer weekSeq);

    /**
     * 对传入的价位段卷烟列表执行分配策略。
     * <p>
     * 由 UnifiedAllocationService 调用，传入已按 delivery_method 过滤的价位段卷烟列表。
     * </p>
     *
     * @param cigaretteList 已过滤的价位段卷烟列表（按价位段自选投放）
     * @param year          年份
     * @param month         月份
     * @param weekSeq       周序号
     * @return 成功处理的卷烟数量（用于统计）
     */
    int allocateForPriceBand(List<Map<String, Object>> cigaretteList, Integer year, Integer month, Integer weekSeq);
}
