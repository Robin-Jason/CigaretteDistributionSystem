package org.example.infrastructure.config.importing;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * 导入合法性校验规则配置属性。
 *
 * 作用：
 * - 从 import-validation-rules.yml（或外部覆盖配置）绑定导入批次级/单行级校验规则。
 * - 为领域层导入校验规则提供可运营化的参数来源。
 *
 * 配置结构（见 resources/config/import-validation-rules.yml）：
 * - cityWide.minRatio：全市投放卷烟数量占比阈值。
 * - supplyRules：货源属性 + 投放类型 + 是否允许叠加标签的合法组合。
 *
 * @author Robin
 * @since 2025-12-18
 */
@Data
@Component
@ConfigurationProperties(prefix = "distribution.import-validation")
public class ImportValidationRuleProperties {

    /**
     * 全市投放占比相关配置。
     */
    private CityWideConfig cityWide = new CityWideConfig();

    /**
     * 货源属性 + 投放类型 + 标签 的合法性规则集合。
     */
    private List<SupplyRuleConfig> supplyRules = new ArrayList<>();

    @Data
    public static class CityWideConfig {
        /**
         * 全市投放卷烟数量 / 本期投放所有卷烟数量 的最小占比。
         * 例如：0.4 表示至少 40% 的卷烟需要是“全市投放”。
         */
        private double minRatio = 0.4D;
    }

    @Data
    public static class SupplyRuleConfig {
        /**
         * 货源属性编码（如 TIGHT / BALANCED / FULL / NEW）。
         */
        private String sourceCode;

        /**
         * 货源属性中文名称/别名映射（如 “紧俏货源”）。
         */
        private List<String> labels = new ArrayList<>();

        /**
         * 允许的投放类型编码集合，对应 encoding-rules.yml 中的 A/B/C/D/E。
         */
        private List<String> allowedDeliveryMethods = new ArrayList<>();

        /**
         * 是否允许在该货源属性下叠加标签投放。
         */
        private boolean allowTag;
    }
}


