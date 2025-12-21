package org.example.application.service.writeback;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * 分配结果写回服务接口。
 * <p>定义统一的写回服务接口，支持不同类型的分配结果写回操作。</p>
 *
 * @author Robin
 * @since 2025-12-18
 */
public interface DistributionWriteBackService {

    /**
     * 单条卷烟写回分配矩阵（标准场景：按档位投放、按档位扩展投放等）。
     * <p>支持多区域分配结果的写回，每条卷烟在独立事务中完成写回。</p>
     *
     * @param allocationMatrix 分配矩阵（行=区域，列=档位D30-D1）
     * @param customerMatrix 客户矩阵（可选，用于提升实际投放量计算精度）
     * @param targetList 目标区域列表
     * @param cigCode 卷烟代码
     * @param cigName 卷烟名称
     * @param year 年份
     * @param month 月份
     * @param weekSeq 周序号
     * @param deliveryMethod 投放方式
     * @param deliveryEtype 扩展投放类型
     * @param remark 备注
     * @param tag 标签
     * @param tagFilterConfig 标签过滤配置
     * @return 写回是否成功
     */
    boolean writeBackSingleCigarette(BigDecimal[][] allocationMatrix,
                                    BigDecimal[][] customerMatrix,
                                    List<String> targetList,
                                    String cigCode,
                                    String cigName,
                                    Integer year,
                                    Integer month,
                                    Integer weekSeq,
                                    String deliveryMethod,
                                    String deliveryEtype,
                                    String remark,
                                    String tag,
                                    String tagFilterConfig);

    /**
     * 批量写回价位段自选投放分配结果。
     * <p>将价位段自选投放算法的分配结果批量写回 {@code cigarette_distribution_prediction_price} 分区表。</p>
     *
     * @param candidates 候选卷烟列表（已包含分配结果 GRADES 字段）
     * @param year 年份
     * @param month 月份
     * @param weekSeq 周序号
     * @param cityCustomerRow 全市客户数数组（30个档位，用于计算实际投放量）
     */
    void writeBackPriceBandAllocations(List<Map<String, Object>> candidates,
                                       Integer year,
                                       Integer month,
                                       Integer weekSeq,
                                       BigDecimal[] cityCustomerRow);
}

