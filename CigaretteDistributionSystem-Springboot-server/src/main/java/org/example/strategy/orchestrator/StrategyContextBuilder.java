package org.example.strategy.orchestrator;

import org.example.service.delivery.DeliveryCombination;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * Builds StrategyContext instances from raw request data.
 */
@Component
public class StrategyContextBuilder {

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

