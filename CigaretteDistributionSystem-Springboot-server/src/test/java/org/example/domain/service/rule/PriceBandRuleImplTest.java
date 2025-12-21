package org.example.domain.service.rule;

import org.example.domain.service.rule.impl.PriceBandRuleImpl;
import org.example.infrastructure.config.price.PriceBandRuleRepository;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;

/**
 * {@link PriceBandRuleImpl} 的单元测试。
 */
class PriceBandRuleImplTest {

    @Test
    void resolveBand_shouldReturnCorrectBand_forBoundaryValues() {
        List<PriceBandRuleRepository.PriceBandDefinition> bands = Arrays.asList(
                new PriceBandRuleRepository.PriceBandDefinition(1, "第1段",
                        bd(600), null),
                new PriceBandRuleRepository.PriceBandDefinition(2, "第2段",
                        bd(400), bd(600)),
                new PriceBandRuleRepository.PriceBandDefinition(3, "第3段",
                        bd(290), bd(400))
        );

        PriceBandRuleImpl rule = new PriceBandRuleImpl(bands);

        // 1 段：>=600
        Assertions.assertEquals(1, rule.resolveBand(bd(600)));
        Assertions.assertEquals(1, rule.resolveBand(bd(800)));

        // 2 段：[400,600)
        Assertions.assertEquals(2, rule.resolveBand(bd(400)));
        Assertions.assertEquals(2, rule.resolveBand(bd(599.99)));

        // 3 段：[290,400)
        Assertions.assertEquals(3, rule.resolveBand(bd(290)));
        Assertions.assertEquals(3, rule.resolveBand(bd(399.99)));

        // 不在任何段内
        Assertions.assertEquals(0, rule.resolveBand(bd(100)));
        Assertions.assertEquals(0, rule.resolveBand(null));
    }

    private BigDecimal bd(double v) {
        return BigDecimal.valueOf(v);
    }
}


