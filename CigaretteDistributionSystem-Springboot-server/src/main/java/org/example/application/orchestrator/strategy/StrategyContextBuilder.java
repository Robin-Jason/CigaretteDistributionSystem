package org.example.application.orchestrator.strategy;

import org.example.application.orchestrator.allocation.RegionCustomerMatrix;
import org.example.domain.model.valueobject.DeliveryCombination;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * 策略上下文构建器。
 *
 * <p>将 {@link StrategyExecutionRequest} 的原始输入整理为 {@link StrategyContext}：</p>
 * <ul>
 *   <li>抽取批次信息、目标量；</li>
 *   <li>合并 remark、deliveryArea、extraInfo 等信息进入 cigaretteInfo；</li>
 *   <li>绑定已预处理的区域客户矩阵（与区域列表顺序对齐）。</li>
 * </ul>
 *
 * @author Robin
 */
@Component
public class StrategyContextBuilder {

    /**
     * 构建策略执行上下文。
     *
     * @param combination     解析后的投放组合（投放方式/扩展类型/标签）
     * @param request         策略执行请求
     * @param customerMatrix  区域客户矩阵（已做必要的预处理）
     * @return 策略上下文
     *
     * @example
     * <pre>
     *     StrategyContext ctx = builder.build(combination, request, matrix);
     * </pre>
     */
    public StrategyContext build(DeliveryCombination combination,
                                 StrategyExecutionRequest request,
                                 RegionCustomerMatrix customerMatrix) {
        Map<String, Object> cigaretteInfo = new HashMap<>();
        cigaretteInfo.put("deliveryArea", request.getDeliveryArea());
        cigaretteInfo.put("remark", request.getRemark());
        cigaretteInfo.putAll(request.getExtraInfo());

        return new StrategyContext(
                combination,
                request.getTargetAmount(),
                request.getYear(),
                request.getMonth(),
                request.getWeekSeq(),
                cigaretteInfo,
                customerMatrix
        );
    }
}

