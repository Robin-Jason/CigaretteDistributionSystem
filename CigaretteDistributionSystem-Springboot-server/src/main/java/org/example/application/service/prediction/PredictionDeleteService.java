package org.example.application.service.prediction;

import org.example.application.dto.prediction.DeleteCigaretteDto;
import org.example.application.dto.prediction.DeleteRegionAllocationDto;

/**
 * 预测分配数据删除服务接口
 * <p>
 * 提供对 prediction 和 prediction_price 分区表的删除操作。
 * </p>
 *
 * @author Robin
 * @since 2025-12-22
 */
public interface PredictionDeleteService {

    /**
     * 删除指定卷烟的特定区域分配记录。
     * <p>
     * 根据卷烟的投放方式自动判断从 prediction 表还是 prediction_price 表删除：
     * - "按价位段自选投放" → prediction_price
     * - 其他投放方式 → prediction
     * </p>
     * <p>
     * 删除后更新 info 表备注为"人工已删除{区域名}"。
     * 注意：至少保留一条区域记录，不能删除最后一条。
     * </p>
     *
     * @param dto 删除请求参数
     * @throws IllegalArgumentException 参数校验失败时抛出
     * @throws IllegalStateException    业务规则校验失败时抛出（如尝试删除最后一条区域记录）
     */
    void deleteRegionAllocation(DeleteRegionAllocationDto dto);

    /**
     * 删除指定卷烟的所有区域分配记录。
     * <p>
     * 根据卷烟的投放方式自动判断从 prediction 表还是 prediction_price 表删除：
     * - "按价位段自选投放" → prediction_price
     * - 其他投放方式 → prediction
     * </p>
     * <p>
     * 删除后更新 info 表备注为"人工确认删除"。
     * </p>
     *
     * @param dto 删除请求参数
     * @throws IllegalArgumentException 参数校验失败时抛出
     * @throws IllegalStateException    业务规则校验失败时抛出
     */
    void deleteCigarette(DeleteCigaretteDto dto);
}
