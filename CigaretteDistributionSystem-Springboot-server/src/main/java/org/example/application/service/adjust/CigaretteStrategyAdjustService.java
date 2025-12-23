package org.example.application.service.adjust;

import org.example.application.dto.allocation.AdjustCigaretteStrategyRequestDto;
import org.example.application.dto.allocation.AdjustCigaretteStrategyResponseDto;

/**
 * 卷烟投放策略调整服务接口
 * <p>
 * 提供卷烟投放组合的调整与重新分配功能。
 * </p>
 *
 * @author Robin
 * @since 2025-12-22
 */
public interface CigaretteStrategyAdjustService {

    /**
     * 调整卷烟投放策略并重新生成分配方案
     * <p>
     * 处理流程：
     * 1. 参数校验（时间、卷烟是否存在）
     * 2. 标签校验（若有新标签，校验 customer_filter.Dynamic_tags 中是否存在）
     * 3. 删除旧记录（根据原投放类型删除 prediction 或 prediction_price 表对应卷烟的所有区域记录）
     * 4. 补充区域统计（若新策略对应的区域客户数据不存在，追加构建）
     * 5. 执行分配算法（根据新投放组合选择算法执行分配）
     * 6. 写回新记录（根据新投放类型写入 prediction 或 prediction_price 表）
     * 7. 更新 Info 表备注（格式：已人工调整策略?，?为新投放组合描述）
     * </p>
     *
     * @param request 调整请求
     * @return 调整结果，成功返回新分配记录，失败返回错误信息
     */
    AdjustCigaretteStrategyResponseDto adjustStrategy(AdjustCigaretteStrategyRequestDto request);
}
