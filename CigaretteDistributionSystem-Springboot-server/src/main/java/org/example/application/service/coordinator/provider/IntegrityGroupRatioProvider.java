package org.example.application.service.coordinator.provider;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.*;

/**
 * 诚信互助小组比例提供者（档位+诚信互助小组）。
 * <p>
 * 处理"档位+诚信互助小组"扩展投放类型的分组比例计算。
 * </p>
 *
 * @author Robin
 */
@Slf4j
@Component
public class IntegrityGroupRatioProvider implements GroupRatioProvider {

    private static final String INTEGRITY_GROUP_EXTENSION = "档位+诚信互助小组";

    @Override
    public boolean supports(String deliveryEtype) {
        return INTEGRITY_GROUP_EXTENSION.equals(deliveryEtype);
    }

    @Override
    public Map<String, BigDecimal> calculateGroupRatios(String deliveryEtype,
                                                         List<String> regions,
                                                         BigDecimal[][] customerMatrix,
                                                         Map<String, Object> extraInfo) {
        if (!supports(deliveryEtype)) {
            return Collections.emptyMap();
        }

        // 诚信互助小组按区域客户数比例分配
        return calculateRatiosFromMatrix(regions, customerMatrix);
    }

    @Override
    public Map<String, String> getRegionGroupMapping(String deliveryEtype, List<String> regions) {
        if (!supports(deliveryEtype)) {
            return Collections.emptyMap();
        }

        // 每个区域作为独立分组
        Map<String, String> mapping = new LinkedHashMap<>();
        for (String region : regions) {
            if (region != null) {
                mapping.put(region, region);
            }
        }
        return mapping;
    }

    private Map<String, BigDecimal> calculateRatiosFromMatrix(List<String> regions, BigDecimal[][] customerMatrix) {
        Map<String, BigDecimal> regionTotals = new LinkedHashMap<>();
        BigDecimal grandTotal = BigDecimal.ZERO;

        for (int i = 0; i < regions.size() && i < customerMatrix.length; i++) {
            String region = regions.get(i);
            BigDecimal regionTotal = sumRow(customerMatrix[i]);
            regionTotals.put(region, regionTotal);
            grandTotal = grandTotal.add(regionTotal);
        }

        if (grandTotal.compareTo(BigDecimal.ZERO) == 0) {
            return Collections.emptyMap();
        }

        Map<String, BigDecimal> ratios = new LinkedHashMap<>();
        for (Map.Entry<String, BigDecimal> entry : regionTotals.entrySet()) {
            BigDecimal ratio = entry.getValue().divide(grandTotal, 4, BigDecimal.ROUND_HALF_UP);
            ratios.put(entry.getKey(), ratio);
        }

        log.debug("根据客户矩阵计算诚信互助小组比例: {}", ratios);
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
}
