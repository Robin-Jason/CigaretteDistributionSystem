package org.example.application.service.calculate;

/**
 * 按价位段自选投放（全市默认策略）分配服务接口。
 *
 * <p>职责：基于价位段候选卷烟列表，完成按价位段自选投放场景下的完整分配流程
 *（初分配、价位段矩阵截断与微调、写回 prediction_price 分区表）。</p>
 *
 * @author Robin
 * @since 2025-12-18
 */
public interface PriceBandAllocationService {

    /**
     * 对指定时间分区内“按价位段自选投放（全市）”的卷烟执行默认分配策略。
     *
     * @param year    年份
     * @param month   月份
     * @param weekSeq 周序号
     *
     * @example allocateForPriceBand(2025, 9, 3) -> 生成/覆盖当期的价位段自选投放预测结果
     */
    void allocateForPriceBand(Integer year, Integer month, Integer weekSeq);
}


