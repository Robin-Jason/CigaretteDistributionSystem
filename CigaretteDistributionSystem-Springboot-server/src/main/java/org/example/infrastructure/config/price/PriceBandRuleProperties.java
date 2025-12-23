package org.example.infrastructure.config.price;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * 价位段规则配置属性。
 *
 * <p>从 {@code config/price-band-rules.yml} 绑定价位段区间配置，为领域价位段规则提供数据来源。</p>
 *
 * @author Robin
 * @since 2025-12-18
 */
@Data
@Component
@ConfigurationProperties(prefix = "price-bands")
public class PriceBandRuleProperties {

    /**
     * 订购量计算阈值，用于计算价位段订购量上限。
     * 默认值为 1.3
     */
    private BigDecimal orderLimitThreshold = new BigDecimal("1.3");

    /**
     * 价位段配置列表。
     */
    private List<PriceBandConfig> priceBands = new ArrayList<>();

    @Data
    public static class PriceBandConfig {
        /**
         * 价位段编号（如 1~9）。
         */
        private int code;

        /**
         * 价位段展示标签（如“第1段”）。
         */
        private String label;

        /**
         * 批发价下界（含）。
         */
        private BigDecimal minInclusive;

        /**
         * 批发价上界（不含），允许为 null 表示无上界。
         */
        private BigDecimal maxExclusive;
    }
}


