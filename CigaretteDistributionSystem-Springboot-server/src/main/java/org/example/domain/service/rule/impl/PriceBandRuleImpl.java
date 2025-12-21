package org.example.domain.service.rule.impl;

import org.example.domain.service.rule.PriceBandRule;
import org.example.infrastructure.config.price.PriceBandRuleRepository;

import java.math.BigDecimal;
import java.util.List;

/**
 * 价位段规则领域服务实现。
 *
 * <p>纯领域逻辑：基于 {@link PriceBandRuleRepository} 提供的价位段定义列表，
 * 对给定批发价执行“minInclusive &lt;= price &lt; maxExclusive” 匹配，返回段编号。</p>
 *
 * @author Robin
 * @since 2025-12-18
 */
public class PriceBandRuleImpl implements PriceBandRule {

    private final List<PriceBandRuleRepository.PriceBandDefinition> bands;

    public PriceBandRuleImpl(List<PriceBandRuleRepository.PriceBandDefinition> bands) {
        this.bands = bands;
    }

    @Override
    public int resolveBand(BigDecimal wholesalePrice) {
        if (wholesalePrice == null || bands == null || bands.isEmpty()) {
            return 0;
        }
        for (PriceBandRuleRepository.PriceBandDefinition band : bands) {
            if (band.getMinInclusive() != null
                    && wholesalePrice.compareTo(band.getMinInclusive()) < 0) {
                continue;
            }
            if (band.getMaxExclusive() != null
                    && wholesalePrice.compareTo(band.getMaxExclusive()) >= 0) {
                continue;
            }
            return band.getCode();
        }
        // 不在任一配置段内
        return 0;
    }
}


