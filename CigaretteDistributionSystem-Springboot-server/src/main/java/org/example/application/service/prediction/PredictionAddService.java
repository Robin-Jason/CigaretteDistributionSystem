package org.example.application.service.prediction;

import org.example.application.dto.prediction.AddRegionAllocationDto;

/**
 * 预测分配数据新增服务接口
 * <p>
 * 提供对 prediction 和 prediction_price 分区表的新增操作。
 * </p>
 *
 * @author Robin
 * @since 2025-12-22
 */
public interface PredictionAddService {

    /**
     * 新增投放区域分配记录。
     * <p>
     * 根据卷烟的投放方式自动判断写入 prediction 表还是 prediction_price 表：
     * - "按价位段自选投放" → prediction_price
     * - 其他投放方式 → prediction
     * </p>
     *
     * @param dto 新增请求参数
     * @throws IllegalArgumentException 参数校验失败时抛出
     * @throws IllegalStateException    业务规则校验失败时抛出
     */
    void addRegionAllocation(AddRegionAllocationDto dto);
}
