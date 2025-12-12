package org.example.strategy.orchestrator.impl;

import lombok.extern.slf4j.Slf4j;
import org.example.strategy.orchestrator.DistributionAlgorithmEngine;
import org.example.strategy.orchestrator.GroupRatioProvider;
import org.example.strategy.orchestrator.StrategyExecutionRequest;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.*;

/**
 * 市场类型比例分配提供者
 * <p>
 * 规则：
 * 1. 默认比例：城网:农网 = 4:6（用户未传入比例参数时使用）
 * 2. 如果用户传入了比例参数，使用用户参数
 * 3. 比例参数生效条件：当前区域既包括城网也包括农网
 * 4. 如果为单扩展单区域，退化为单区域分配算法（不设置比例）
 * 5. 如果为双扩展且市场类型扩展类型的区域仅包含一种，退化为无比例参数的算法（不设置比例）
 */
@Slf4j
@Component
public class MarketTypeRatioProvider implements GroupRatioProvider {

    private static final String MARKET_TYPE_EXTENSION = "档位+市场类型";
    private static final String URBAN_GROUP = "城网";
    private static final String RURAL_GROUP = "农网";
    private static final BigDecimal DEFAULT_URBAN_RATIO = new BigDecimal("0.4"); // 4:6
    private static final BigDecimal DEFAULT_RURAL_RATIO = new BigDecimal("0.6");

    @Override
    public boolean supports(String deliveryEtype) {
        return MARKET_TYPE_EXTENSION.equals(deliveryEtype);
    }

    @Override
    public Map<String, BigDecimal> calculateGroupRatios(StrategyExecutionRequest request,
                                                         List<String> regions,
                                                         BigDecimal[][] customerMatrix) {
        if (!supports(request.getDeliveryEtype())) {
            return Collections.emptyMap();
        }

        // 检查区域中是否同时包含城网和农网
        boolean hasUrban = regions.contains(URBAN_GROUP);
        boolean hasRural = regions.contains(RURAL_GROUP);

        // 如果区域仅包含一种市场类型，不设置比例（退化为无比例参数算法）
        if (!hasUrban || !hasRural) {
            log.debug("市场类型扩展类型：区域仅包含一种市场类型（城网: {}, 农网: {}），不设置比例", hasUrban, hasRural);
            return Collections.emptyMap();
        }

        // 如果为单扩展单区域（只有一个区域），不设置比例（退化为单区域分配算法）
        if (regions.size() == 1) {
            log.debug("市场类型扩展类型：单扩展单区域，不设置比例");
            return Collections.emptyMap();
        }

        // 从请求中提取用户传入的比例参数
        Map<String, BigDecimal> userRatios = extractUserRatios(request);

        Map<String, BigDecimal> ratios = new LinkedHashMap<>();
        if (!userRatios.isEmpty()) {
            // 使用用户传入的比例
            BigDecimal urbanRatio = userRatios.get(URBAN_GROUP);
            BigDecimal ruralRatio = userRatios.get(RURAL_GROUP);
            if (urbanRatio != null && ruralRatio != null) {
                ratios.put(URBAN_GROUP, urbanRatio);
                ratios.put(RURAL_GROUP, ruralRatio);
                log.debug("市场类型扩展类型：使用用户传入的比例 - 城网: {}, 农网: {}", urbanRatio, ruralRatio);
            } else {
                // 用户传入的比例不完整，使用默认比例
                ratios.put(URBAN_GROUP, DEFAULT_URBAN_RATIO);
                ratios.put(RURAL_GROUP, DEFAULT_RURAL_RATIO);
                log.debug("市场类型扩展类型：用户传入的比例不完整，使用默认比例 - 城网: {}, 农网: {}",
                        DEFAULT_URBAN_RATIO, DEFAULT_RURAL_RATIO);
            }
        } else {
            // 使用默认比例
            ratios.put(URBAN_GROUP, DEFAULT_URBAN_RATIO);
            ratios.put(RURAL_GROUP, DEFAULT_RURAL_RATIO);
            log.debug("市场类型扩展类型：使用默认比例 - 城网: {}, 农网: {}",
                    DEFAULT_URBAN_RATIO, DEFAULT_RURAL_RATIO);
        }

        return ratios;
    }

    @Override
    public Map<String, String> getRegionGroupMapping(StrategyExecutionRequest request,
                                                      List<String> regions) {
        if (!supports(request.getDeliveryEtype())) {
            return Collections.emptyMap();
        }

        Map<String, String> mapping = new LinkedHashMap<>();
        for (String region : regions) {
            if (URBAN_GROUP.equals(region)) {
                mapping.put(region, URBAN_GROUP);
            } else if (RURAL_GROUP.equals(region)) {
                mapping.put(region, RURAL_GROUP);
            } else {
                // 其他区域保持原样
                mapping.put(region, region);
            }
        }
        return mapping;
    }

    /**
     * 从请求的ExtraInfo中提取用户传入的比例参数
     */
    @SuppressWarnings("unchecked")
    private Map<String, BigDecimal> extractUserRatios(StrategyExecutionRequest request) {
        if (request.getExtraInfo() == null) {
            return Collections.emptyMap();
        }

        Object raw = request.getExtraInfo().get(DistributionAlgorithmEngine.EXTRA_GROUP_RATIOS);
        if (!(raw instanceof Map)) {
            return Collections.emptyMap();
        }

        Map<String, Object> source = (Map<String, Object>) raw;
        Map<String, BigDecimal> result = new LinkedHashMap<>();
        for (Map.Entry<String, Object> entry : source.entrySet()) {
            BigDecimal value = toBigDecimal(entry.getValue());
            if (value != null && value.compareTo(BigDecimal.ZERO) > 0) {
                result.put(entry.getKey(), value);
            }
        }
        return result;
    }

    private BigDecimal toBigDecimal(Object raw) {
        if (raw == null) {
            return null;
        }
        if (raw instanceof BigDecimal) {
            return (BigDecimal) raw;
        }
        if (raw instanceof Number) {
            return new BigDecimal(((Number) raw).toString());
        }
        try {
            return new BigDecimal(raw.toString());
        } catch (NumberFormatException ex) {
            log.warn("无法解析比例值: {}", raw, ex);
            return null;
        }
    }
}

