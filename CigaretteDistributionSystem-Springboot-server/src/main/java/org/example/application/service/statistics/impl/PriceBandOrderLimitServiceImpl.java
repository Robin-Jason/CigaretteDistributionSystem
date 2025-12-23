package org.example.application.service.statistics.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.application.service.statistics.PriceBandOrderLimitService;
import org.example.domain.repository.CigaretteDistributionPredictionPriceRepository;
import org.example.infrastructure.config.price.PriceBandRuleProperties;
import org.example.infrastructure.config.price.PriceBandRuleRepository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;

/**
 * 价位段订购量上限统计服务实现。
 *
 * @author Robin
 * @since 2025-12-22
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PriceBandOrderLimitServiceImpl implements PriceBandOrderLimitService {

    private final CigaretteDistributionPredictionPriceRepository predictionPriceRepository;
    private final PriceBandRuleProperties priceBandRuleProperties;
    private final PriceBandRuleRepository priceBandRuleRepository;

    @Override
    public PriceBandOrderLimitResult queryOrderLimits(Integer year, Integer month, Integer weekSeq) {
        log.info("查询价位段订购量上限: {}-{}-{}", year, month, weekSeq);

        // 1. 获取阈值配置
        BigDecimal threshold = priceBandRuleProperties.getOrderLimitThreshold();
        if (threshold == null || threshold.compareTo(BigDecimal.ZERO) <= 0) {
            threshold = new BigDecimal("1.3");
        }

        // 2. 查询按价位段自选投放的分配结果
        List<Map<String, Object>> allocations = predictionPriceRepository.findPriceBandAllocations(year, month, weekSeq);
        
        // 3. 按价位段分组，计算每个档位的投放量之和
        Map<Integer, BigDecimal[]> bandGradeSums = new TreeMap<>();
        for (Map<String, Object> row : allocations) {
            Object bandObj = row.get("PRICE_BAND");
            if (bandObj == null) {
                continue;
            }
            
            Integer band;
            try {
                band = Integer.valueOf(bandObj.toString());
            } catch (NumberFormatException e) {
                continue;
            }

            // 获取或初始化该价位段的档位累加数组
            BigDecimal[] gradeSums = bandGradeSums.computeIfAbsent(band, k -> {
                BigDecimal[] arr = new BigDecimal[30];
                Arrays.fill(arr, BigDecimal.ZERO);
                return arr;
            });

            // 累加每个档位的值
            for (int i = 0; i < 30; i++) {
                String colName = "D" + (30 - i);
                Object val = row.get(colName);
                if (val != null) {
                    BigDecimal gradeVal = toBigDecimal(val);
                    gradeSums[i] = gradeSums[i].add(gradeVal);
                }
            }
        }

        // 4. 计算订购量上限 = 投放量之和 / 阈值（向下取整）
        Map<Integer, PriceBandLimitDetail> priceBandLimits = new LinkedHashMap<>();
        
        // 获取价位段标签映射
        Map<Integer, String> bandLabels = getBandLabels();

        for (Map.Entry<Integer, BigDecimal[]> entry : bandGradeSums.entrySet()) {
            Integer band = entry.getKey();
            BigDecimal[] gradeSums = entry.getValue();
            
            int[] limits = new int[30];
            for (int i = 0; i < 30; i++) {
                // 订购量上限 = 投放量之和 / 阈值，向下取整
                limits[i] = gradeSums[i].divide(threshold, 0, RoundingMode.FLOOR).intValue();
            }

            PriceBandLimitDetail detail = new PriceBandLimitDetail();
            detail.setLabel(bandLabels.getOrDefault(band, "第" + band + "段"));
            detail.setLimits(limits);
            priceBandLimits.put(band, detail);
        }

        // 5. 构建返回结果
        PriceBandOrderLimitResult result = new PriceBandOrderLimitResult();
        result.setThreshold(threshold);
        result.setPriceBandLimits(priceBandLimits);

        log.info("价位段订购量上限查询完成: {}-{}-{}, 共 {} 个价位段", 
                year, month, weekSeq, priceBandLimits.size());
        return result;
    }

    /**
     * 获取价位段编号到标签的映射
     */
    private Map<Integer, String> getBandLabels() {
        Map<Integer, String> labels = new HashMap<>();
        List<PriceBandRuleRepository.PriceBandDefinition> bands = priceBandRuleRepository.getBands();
        for (PriceBandRuleRepository.PriceBandDefinition band : bands) {
            labels.put(band.getCode(), band.getLabel());
        }
        return labels;
    }

    /**
     * 将对象转换为 BigDecimal
     */
    private BigDecimal toBigDecimal(Object val) {
        if (val == null) {
            return BigDecimal.ZERO;
        }
        if (val instanceof BigDecimal) {
            return (BigDecimal) val;
        }
        if (val instanceof Number) {
            return new BigDecimal(val.toString());
        }
        try {
            return new BigDecimal(val.toString());
        } catch (NumberFormatException e) {
            return BigDecimal.ZERO;
        }
    }
}
