package org.example.domain.service.rule.impl;

import org.example.domain.service.rule.EncodingRule;

import java.math.BigDecimal;

/**
 * 编码规则领域服务实现。
 * <p>
 * 纯领域逻辑，不含Spring依赖，可独立测试。
 * 该实现复制自 {@link org.example.application.service.encode.EncodeServiceImpl} 的核心业务规则方法，
 * 移除了Spring注解和基础设施依赖，保持业务逻辑完全一致。
 * </p>
 *
 * @author Robin
 * @version 1.0
 * @since 2025-12-14
 */
public class EncodingRuleImpl implements EncodingRule {

    @Override
    public String encodeGradeSequences(BigDecimal[] grades) {
        if (grades == null || grades.length == 0) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        int start = 0;
        while (start < grades.length) {
            BigDecimal current = grades[start] == null ? BigDecimal.ZERO : grades[start];
            int end = start;
            while (end < grades.length) {
                BigDecimal val = grades[end] == null ? BigDecimal.ZERO : grades[end];
                if (val.compareTo(current) == 0) {
                    end++;
                } else {
                    break;
                }
            }
            sb.append(end - start).append("×").append(formatAsInteger(current));
            if (end < grades.length) {
                sb.append("+");
            }
            start = end;
        }
        return sb.toString();
    }

    @Override
    public String formatAsInteger(BigDecimal value) {
        if (value == null) {
            return "0";
        }
        return value.stripTrailingZeros().toPlainString();
    }
}

