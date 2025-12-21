package org.example.application.orchestrator.allocation.provider.impl;

import lombok.extern.slf4j.Slf4j;
import org.example.application.orchestrator.allocation.provider.GroupRatioProvider;
import org.example.application.orchestrator.strategy.StrategyExecutionRequest;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;

/**
 * 诚信互助小组比例分配提供者
 * <p>
 * 规则：根据客户数占比为每个区域拆分预投放量
 * 即：每个诚信互助小组的分配量 = 总预投放量 × (该小组客户数 / 所有小组客户数总和)
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
    public Map<String, BigDecimal> calculateGroupRatios(StrategyExecutionRequest request,
                                                         List<String> regions,
                                                         BigDecimal[][] customerMatrix) {
        if (!supports(request.getDeliveryEtype())) {
            return Collections.emptyMap();
        }

        if (regions == null || regions.isEmpty() || customerMatrix == null) {
            return Collections.emptyMap();
        }

        // 计算每个区域的客户数总和（所有档位客户数之和）
        Map<String, BigDecimal> regionCustomerTotals = new LinkedHashMap<>();
        BigDecimal totalCustomers = BigDecimal.ZERO;

        for (int i = 0; i < regions.size() && i < customerMatrix.length; i++) {
            String region = regions.get(i);
            BigDecimal[] customerRow = customerMatrix[i];
            BigDecimal regionTotal = BigDecimal.ZERO;

            if (customerRow != null) {
                for (BigDecimal customerCount : customerRow) {
                    if (customerCount != null) {
                        regionTotal = regionTotal.add(customerCount);
                    }
                }
            }

            regionCustomerTotals.put(region, regionTotal);
            totalCustomers = totalCustomers.add(regionTotal);
        }

        // 如果总客户数为0，无法计算比例，返回空Map（退化为无比例参数算法）
        if (totalCustomers.compareTo(BigDecimal.ZERO) <= 0) {
            log.warn("诚信互助小组：所有区域客户数总和为0，无法计算比例，退化为无比例参数算法");
            return Collections.emptyMap();
        }

        // 根据客户数占比计算每个区域的比例
        Map<String, BigDecimal> ratios = new LinkedHashMap<>();
        for (Map.Entry<String, BigDecimal> entry : regionCustomerTotals.entrySet()) {
            String region = entry.getKey();
            BigDecimal regionTotal = entry.getValue();
            // 比例 = 该区域客户数 / 总客户数
            BigDecimal ratio = regionTotal.divide(totalCustomers, 10, RoundingMode.HALF_UP);
            ratios.put(region, ratio);
            log.debug("诚信互助小组：区域 {} 客户数占比: {} (客户数: {}, 总客户数: {})",
                    region, ratio, regionTotal, totalCustomers);
        }

        return ratios;
    }

    @Override
    public Map<String, String> getRegionGroupMapping(StrategyExecutionRequest request,
                                                      List<String> regions) {
        if (!supports(request.getDeliveryEtype())) {
            return Collections.emptyMap();
        }

        // 诚信互助小组：每个区域就是一个独立的组
        Map<String, String> mapping = new LinkedHashMap<>();
        for (String region : regions) {
            mapping.put(region, region);
        }
        return mapping;
    }
}

