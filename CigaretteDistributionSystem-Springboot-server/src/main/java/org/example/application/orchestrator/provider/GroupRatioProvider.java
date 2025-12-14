package org.example.application.orchestrator.provider;

import org.example.application.orchestrator.StrategyExecutionRequest;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * 可扩展的比例分配提供者接口
 * <p>
 * 用于为不同的扩展类型提供比例分配逻辑，支持：
 * <ul>
 *     <li>市场类型：默认城网:农网 = 4:6，或使用用户传入的比例</li>
 *     <li>诚信自律小组：根据客户数占比自动计算比例</li>
 *     <li>其他扩展类型：可后续扩展</li>
 * </ul>
 */
public interface GroupRatioProvider {

    /**
     * 判断该提供者是否支持指定的扩展类型
     *
     * @param deliveryEtype 扩展投放类型，如"档位+市场类型"、"档位+诚信自律小组"
     * @return 如果支持则返回true
     */
    boolean supports(String deliveryEtype);

    /**
     * 计算分组比例
     *
     * @param request 策略执行请求
     * @param regions 区域列表
     * @param customerMatrix 区域客户数矩阵（用于计算客户数占比）
     * @return 分组比例Map，key为组名（如"城网"、"农网"），value为比例（0-1之间）
     */
    Map<String, BigDecimal> calculateGroupRatios(StrategyExecutionRequest request,
                                                   List<String> regions,
                                                   BigDecimal[][] customerMatrix);

    /**
     * 获取区域到分组的映射函数
     * <p>
     * 例如：市场类型扩展类型，区域"城网"映射到"城网"组，区域"农网"映射到"农网"组
     *
     * @param request 策略执行请求
     * @param regions 区域列表
     * @return 区域到分组的映射Map，key为区域名，value为组名
     */
    Map<String, String> getRegionGroupMapping(StrategyExecutionRequest request,
                                              List<String> regions);
}

