package org.example.application.service.writeback;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * 价位段自选投放分配结果写回服务接口。
 * <p>
 * 负责价位段自选投放算法的分配结果批量写回 {@code cigarette_distribution_prediction_price} 分区表。
 * </p>
 *
 * @author Robin
 * @since 2025-12-22
 */
public interface PriceBandDistributionWriteBackService {

    /**
     * 批量写回价位段自选投放分配结果。
     * <p>
     * 将价位段自选投放算法的分配结果批量写回 {@code cigarette_distribution_prediction_price} 分区表。
     * </p>
     *
     * @param candidates      候选卷烟列表（已包含分配结果 GRADES 字段）
     * @param year            年份
     * @param month           月份
     * @param weekSeq         周序号
     * @param cityCustomerRow 全市客户数数组（30个档位，用于计算实际投放量）
     * @example writeBackPriceBandAllocations(candidates, 2025, 9, 4, cityCustomerRow)
     */
    void writeBackPriceBandAllocations(List<Map<String, Object>> candidates,
                                       Integer year,
                                       Integer month,
                                       Integer weekSeq,
                                       BigDecimal[] cityCustomerRow);
}
