package org.example.domain.service.rule.impl;

import org.example.domain.service.rule.SupplySourceValidationRule;

import java.util.Locale;

/**
 * 货源属性合法性校验规则领域服务实现。
 *
 * <p>纯领域逻辑，不依赖 Spring 或持久化层。</p>
 *
 * @author Robin
 * @since 2025-12-18
 */
public class SupplySourceValidationRuleImpl implements SupplySourceValidationRule {

    @Override
    /**
     * @param sourceCode         货源属性编码
     * @param deliveryMethodCode 投放类型编码
     * @param hasTag             是否叠加标签
     * @param ruleDefinition     该货源属性对应的合法规则定义
     *
     * @example sourceCode="TIGHT", deliveryMethodCode="A", hasTag=false -> 合法
     */
    public void validate(String sourceCode,
                         String deliveryMethodCode,
                         boolean hasTag,
                         SupplyRuleDefinition ruleDefinition) {
        if (ruleDefinition == null) {
            // 未配置规则时由上层决定是否拦截，这里默认放行
            return;
        }

        String normalizedSource = normalize(sourceCode);
        String normalizedMethod = normalize(deliveryMethodCode);

        // 1. 校验投放类型是否在允许集合中
        if (normalizedMethod != null
                && ruleDefinition.getAllowedDeliveryMethods() != null
                && !ruleDefinition.getAllowedDeliveryMethods().isEmpty()
                && !ruleDefinition.getAllowedDeliveryMethods().contains(normalizedMethod)) {
            throw new IllegalArgumentException(String.format(
                    "货源属性[%s]的投放类型[%s]不在允许集合内",
                    safe(sourceCode), safe(deliveryMethodCode)));
        }

        // 2. 校验是否允许叠加标签
        if (hasTag && !ruleDefinition.isAllowTag()) {
            throw new IllegalArgumentException(String.format(
                    "货源属性[%s]不允许叠加标签投放",
                    safe(sourceCode)));
        }
    }

    private String normalize(String code) {
        if (code == null) {
            return null;
        }
        return code.trim().toUpperCase(Locale.ROOT);
    }

    private String safe(String val) {
        return val == null ? "" : val;
    }
}


