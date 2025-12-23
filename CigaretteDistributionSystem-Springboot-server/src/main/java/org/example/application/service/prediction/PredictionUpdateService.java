package org.example.application.service.prediction;

import org.example.application.dto.prediction.UpdateRegionGradesDto;

/**
 * 预测分配数据修改服务接口
 * <p>
 * 提供对 prediction 和 prediction_price 分区表的修改操作。
 * </p>
 *
 * @author Robin
 * @since 2025-12-22
 */
public interface PredictionUpdateService {

    /**
     * 修改指定卷烟特定区域的档位值。
     * <p>
     * 根据卷烟的投放方式自动判断修改 prediction 表还是 prediction_price 表：
     * - "按价位段自选投放" → prediction_price
     * - 其他投放方式 → prediction
     * </p>
     * <p>
     * 修改后会重新计算实际投放量和编码表达式，并更新对应区域记录备注。
     * </p>
     *
     * @param dto 修改请求参数
     * @throws IllegalArgumentException 参数校验失败时抛出
     * @throws IllegalStateException    业务规则校验失败时抛出（如记录不存在）
     */
    void updateRegionGrades(UpdateRegionGradesDto dto);
}
