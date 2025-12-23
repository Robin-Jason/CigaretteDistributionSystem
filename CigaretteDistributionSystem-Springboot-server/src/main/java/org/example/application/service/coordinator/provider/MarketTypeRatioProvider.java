package org.example.application.service.coordinator.provider;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.*;

/**
 * 市场类型比例提供者（档位+市场类型）。
 * <p>
 * 处理"档位+市场类型"扩展投放类型的分组比例计算。
 * </p>
 *
 * @author Robin
 */
@Slf4j
@Component
public class MarketTypeRatioProvider implements GroupRatioProvider {

    private static final String MARKET_TYPE_EXTENSION = "档位+市场类型";
    private static final String EXTRA_GROUP_RATIOS = "groupRatios";

    @Override
    public boolean supports(String deliveryEtype) {
        return MARKET_TYPE_EXTENSION.equals(deliveryEtype);
    }

    // 默认比例：城网 40%，农网 60%
    private static final BigDecimal DEFAULT_URBAN_RATIO = new BigDecimal("0.4");
    private static final BigDecimal DEFAULT_RURAL_RATIO = new BigDecimal("0.6");

    @Override
    public Map<String, BigDecimal> calculateGroupRatios(String deliveryEtype,
                                                         List<String> regions,
                                                         BigDecimal[][] customerMatrix,
                                                         Map<String, Object> extraInfo) {
        if (!supports(deliveryEtype)) {
            return Collections.emptyMap();
        }

        // 优先使用用户传入的比例
        Map<String, BigDecimal> userRatios = extractUserRatios(extraInfo);
        if (!userRatios.isEmpty()) {
            log.debug("使用用户传入的市场类型比例: {}", userRatios);
            return userRatios;
        }

        // 使用默认比例：城网 40%，农网 60%
        Map<String, BigDecimal> defaultRatios = new LinkedHashMap<>();
        defaultRatios.put("城网", DEFAULT_URBAN_RATIO);
        defaultRatios.put("农网", DEFAULT_RURAL_RATIO);
        log.debug("使用默认市场类型比例: 城网={}, 农网={}", DEFAULT_URBAN_RATIO, DEFAULT_RURAL_RATIO);
        return defaultRatios;
    }

    @Override
    public Map<String, String> getRegionGroupMapping(String deliveryEtype, List<String> regions) {
        if (!supports(deliveryEtype)) {
            return Collections.emptyMap();
        }

        Map<String, String> mapping = new LinkedHashMap<>();
        for (String region : regions) {
            if (region == null) continue;
            
            if (region.contains("城网") || region.contains("城区")) {
                mapping.put(region, "城网");
            } else if (region.contains("农网") || region.contains("农村")) {
                mapping.put(region, "农网");
            } else {
                // 默认归为城网
                mapping.put(region, "城网");
            }
        }
        return mapping;
    }

    @SuppressWarnings("unchecked")
    private Map<String, BigDecimal> extractUserRatios(Map<String, Object> extraInfo) {
        if (extraInfo == null) {
            return Collections.emptyMap();
        }

        Object raw = extraInfo.get(EXTRA_GROUP_RATIOS);
        if (!(raw instanceof Map)) {
            // 尝试直接获取 urbanRatio/ruralRatio
            BigDecimal urbanRatio = toBigDecimal(extraInfo.get("urbanRatio"));
            BigDecimal ruralRatio = toBigDecimal(extraInfo.get("ruralRatio"));
            if (urbanRatio != null && ruralRatio != null) {
                Map<String, BigDecimal> result = new LinkedHashMap<>();
                result.put("城网", urbanRatio);
                result.put("农网", ruralRatio);
                return result;
            }
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

    private Map<String, BigDecimal> calculateRatiosFromMatrix(List<String> regions, BigDecimal[][] customerMatrix) {
        BigDecimal urbanTotal = BigDecimal.ZERO;
        BigDecimal ruralTotal = BigDecimal.ZERO;

        for (int i = 0; i < regions.size() && i < customerMatrix.length; i++) {
            String region = regions.get(i);
            BigDecimal regionTotal = sumRow(customerMatrix[i]);

            if (region.contains("城网") || region.contains("城区")) {
                urbanTotal = urbanTotal.add(regionTotal);
            } else if (region.contains("农网") || region.contains("农村")) {
                ruralTotal = ruralTotal.add(regionTotal);
            } else {
                urbanTotal = urbanTotal.add(regionTotal);
            }
        }

        BigDecimal total = urbanTotal.add(ruralTotal);
        if (total.compareTo(BigDecimal.ZERO) == 0) {
            return Collections.emptyMap();
        }

        Map<String, BigDecimal> ratios = new LinkedHashMap<>();
        ratios.put("城网", urbanTotal.divide(total, 4, BigDecimal.ROUND_HALF_UP));
        ratios.put("农网", ruralTotal.divide(total, 4, BigDecimal.ROUND_HALF_UP));
        
        log.debug("根据客户矩阵计算市场类型比例: 城网={}, 农网={}", ratios.get("城网"), ratios.get("农网"));
        return ratios;
    }

    private BigDecimal sumRow(BigDecimal[] row) {
        if (row == null) return BigDecimal.ZERO;
        BigDecimal sum = BigDecimal.ZERO;
        for (BigDecimal v : row) {
            if (v != null) sum = sum.add(v);
        }
        return sum;
    }

    private BigDecimal toBigDecimal(Object value) {
        if (value == null) return null;
        if (value instanceof BigDecimal) return (BigDecimal) value;
        if (value instanceof Number) return BigDecimal.valueOf(((Number) value).doubleValue());
        try {
            return new BigDecimal(value.toString());
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
