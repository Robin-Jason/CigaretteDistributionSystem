package org.example.infrastructure.config.price;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * 价位段规则内存仓库。
 *
 * <p>在应用启动时加载 {@link PriceBandRuleProperties}，构建不可变的价位段定义列表，供领域规则使用。</p>
 *
 * @author Robin
 * @since 2025-12-18
 */
@Slf4j
@Component
public class PriceBandRuleRepository {

    /**
     * 单个价位段的定义。
     */
    public static class PriceBandDefinition {
        private final int code;
        private final String label;
        private final BigDecimal minInclusive;
        private final BigDecimal maxExclusive;

        public PriceBandDefinition(int code, String label, BigDecimal minInclusive, BigDecimal maxExclusive) {
            this.code = code;
            this.label = label;
            this.minInclusive = minInclusive;
            this.maxExclusive = maxExclusive;
        }

        public int getCode() {
            return code;
        }

        public String getLabel() {
            return label;
        }

        public BigDecimal getMinInclusive() {
            return minInclusive;
        }

        public BigDecimal getMaxExclusive() {
            return maxExclusive;
        }
    }

    private final PriceBandRuleProperties properties;

    /**
     * 按 code 升序排列的只读价位段定义列表。
     */
    private List<PriceBandDefinition> bands = Collections.emptyList();

    public PriceBandRuleRepository(PriceBandRuleProperties properties) {
        this.properties = properties;
    }

    @PostConstruct
    public void initialize() {
        reload();
    }

    /**
     * 重新加载价位段配置。
     */
    public synchronized void reload() {
        List<PriceBandDefinition> list = new ArrayList<>();
        if (properties.getPriceBands() != null) {
            for (PriceBandRuleProperties.PriceBandConfig cfg : properties.getPriceBands()) {
                list.add(new PriceBandDefinition(
                        cfg.getCode(),
                        cfg.getLabel(),
                        cfg.getMinInclusive(),
                        cfg.getMaxExclusive()
                ));
            }
        }

        // 按 code 升序排序，保证段间顺序稳定（1 段 -> 9 段）
        list.sort(Comparator.comparingInt(PriceBandDefinition::getCode));
        this.bands = Collections.unmodifiableList(list);

        log.info("Price band rule repository initialized: {} bands", bands.size());
    }

    /**
     * 获取全部价位段定义（只读）。
     */
    public List<PriceBandDefinition> getBands() {
        return bands;
    }
}


