package org.example.application.service.coordinator.provider;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * 分组比例提供者接口。
 * <p>
 * 用于处理带权重扩展类型（如"档位+市场类型"、"档位+诚信互助小组"）的分组比例计算。
 * </p>
 *
 * @author Robin
 */
public interface GroupRatioProvider {

    /**
     * 判断是否支持指定的扩展类型。
     *
     * @param deliveryEtype 扩展投放类型
     * @return 是否支持
     */
    boolean supports(String deliveryEtype);

    /**
     * 计算分组比例。
     *
     * @param deliveryEtype  扩展投放类型
     * @param regions        区域列表
     * @param customerMatrix 客户矩阵
     * @param extraInfo      额外信息（可包含用户传入的比例）
     * @return 分组比例映射，如 {"城网": 0.6, "农网": 0.4}
     */
    Map<String, BigDecimal> calculateGroupRatios(String deliveryEtype,
                                                  List<String> regions,
                                                  BigDecimal[][] customerMatrix,
                                                  Map<String, Object> extraInfo);

    /**
     * 获取区域到分组的映射。
     *
     * @param deliveryEtype 扩展投放类型
     * @param regions       区域列表
     * @return 区域→分组映射，如 {"城区": "城网", "农村": "农网"}
     */
    Map<String, String> getRegionGroupMapping(String deliveryEtype, List<String> regions);
}
