package org.example.domain.service.rule;

import java.util.Set;

/**
 * 货源属性 + 投放类型 + 标签 合法性校验规则领域服务接口。
 *
 * <p>只依赖抽象规则定义，不直接依赖配置文件或基础设施，便于单元测试和复用。</p>
 *
 * @author Robin
 * @since 2025-12-18
 */
public interface SupplySourceValidationRule {

    /**
     * @param sourceCode         货源属性编码（如 TIGHT / BALANCED / FULL / NEW）
     * @param deliveryMethodCode 投放类型编码（如 A/B/C/D/E）
     * @param hasTag             是否叠加标签投放
     * @param ruleDefinition     该货源属性对应的合法规则定义
     *
     * @example sourceCode="NEW", deliveryMethodCode="A", hasTag=true -> 仅当规则允许时通过
     */
    void validate(String sourceCode,
                  String deliveryMethodCode,
                  boolean hasTag,
                  SupplyRuleDefinition ruleDefinition);

    /**
     * 货源属性合法性规则定义。
     */
    class SupplyRuleDefinition {
        private final String sourceCode;
        private final Set<String> allowedDeliveryMethods;
        private final boolean allowTag;

        public SupplyRuleDefinition(String sourceCode,
                                    Set<String> allowedDeliveryMethods,
                                    boolean allowTag) {
            this.sourceCode = sourceCode;
            this.allowedDeliveryMethods = allowedDeliveryMethods;
            this.allowTag = allowTag;
        }

        public String getSourceCode() {
            return sourceCode;
        }

        public Set<String> getAllowedDeliveryMethods() {
            return allowedDeliveryMethods;
        }

        public boolean isAllowTag() {
            return allowTag;
        }
    }
}


