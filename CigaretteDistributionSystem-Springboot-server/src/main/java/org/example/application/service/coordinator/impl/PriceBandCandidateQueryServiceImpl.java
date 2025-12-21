package org.example.application.service.coordinator.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.application.service.coordinator.PriceBandCandidateQueryService;
import org.example.domain.repository.CigaretteDistributionInfoRepository;
import org.example.domain.service.rule.PriceBandRule;
import org.example.domain.service.rule.impl.PriceBandRuleImpl;
import org.example.infrastructure.config.price.PriceBandRuleRepository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.*;

/**
 * 价位段自选投放候选卷烟查询服务实现。
 *
 * <p>从投放信息分区表和价目表中获取"按价位段自选投放"的卷烟列表，
 * 通过价位段规则打上 PRICE_BAND 标签，并按"段间升序、段内批发价降序"排序。</p>
 *
 * @author Robin
 * @since 2025-12-19
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PriceBandCandidateQueryServiceImpl implements PriceBandCandidateQueryService {

    private final CigaretteDistributionInfoRepository cigaretteDistributionInfoRepository;
    private final PriceBandRuleRepository priceBandRuleRepository;

    @Override
    public List<Map<String, Object>> listOrderedPriceBandCandidates(Integer year, Integer month, Integer weekSeq) {
        List<Map<String, Object>> raw = cigaretteDistributionInfoRepository.findPriceBandCandidates(year, month, weekSeq);
        if (raw == null || raw.isEmpty()) {
            return Collections.emptyList();
        }

        PriceBandRule priceBandRule = new PriceBandRuleImpl(priceBandRuleRepository.getBands());

        // 按价位段分组：key = PRICE_BAND，value = 同一段内的卷烟列表
        Map<Integer, List<Map<String, Object>>> grouped = new TreeMap<>();
        for (Map<String, Object> row : raw) {
            BigDecimal wholesalePrice = toBigDecimal(row.get("WHOLESALE_PRICE"));
            int band = priceBandRule.resolveBand(wholesalePrice);
            if (band <= 0) {
                // 不在任何价位段内的卷烟不参与本次算法
                log.debug("卷烟不在任何价位段内，跳过: CIG_CODE={}, WHOLESALE_PRICE={}",
                        row.get("CIG_CODE"), wholesalePrice);
                continue;
            }
            row.put("PRICE_BAND", band);
            grouped.computeIfAbsent(band, k -> new ArrayList<>()).add(row);
        }

        // 构建最终有序列表：段间升序，段内按批发价降序
        List<Map<String, Object>> ordered = new ArrayList<>();
        for (Map.Entry<Integer, List<Map<String, Object>>> entry : grouped.entrySet()) {
            List<Map<String, Object>> group = entry.getValue();
            group.sort((a, b) -> {
                BigDecimal pa = toBigDecimal(a.get("WHOLESALE_PRICE"));
                BigDecimal pb = toBigDecimal(b.get("WHOLESALE_PRICE"));
                return pb.compareTo(pa); // 降序
            });
            ordered.addAll(group);
        }

        log.debug("价位段自选投放候选卷烟排序完成: {}-{}-{}, 最终候选数={}", year, month, weekSeq, ordered.size());
        return ordered;
    }

    private BigDecimal toBigDecimal(Object value) {
        if (value == null) {
            return BigDecimal.ZERO;
        }
        if (value instanceof BigDecimal) {
            return (BigDecimal) value;
        }
        try {
            return new BigDecimal(value.toString());
        } catch (NumberFormatException e) {
            return BigDecimal.ZERO;
        }
    }
}

