package org.example.service.model.tag;

import java.math.BigDecimal;
import java.util.Locale;
import java.util.Objects;

/**
 * 标签过滤规则类
 * <p>
 * 描述单个标签在临时表中的过滤规则。
 * 用于将标签配置转换为SQL查询条件，支持字符串、数字、布尔值等不同类型的值。
 * </p>
 * <p>
 * 主要功能：
 * - 定义标签名称、列名、操作符和原始值
 * - 将配置值转换为SQL参数类型
 * - 支持多种值类型的解析和转换
 * </p>
 *
 * @author Robin
 * @version 1.0
 * @since 2025-12-12
 */
public class TagFilterRule {

    public enum ValueType {
        STRING,
        NUMBER,
        BOOLEAN;

        public static ValueType from(String raw) {
            if (raw == null) {
                return STRING;
            }
            try {
                return ValueType.valueOf(raw.trim().toUpperCase(Locale.ROOT));
            } catch (IllegalArgumentException ex) {
                return STRING;
            }
        }
    }

    private final String tagName;
    private final String column;
    private final String operator;
    private final String rawValue;
    private final ValueType valueType;

    public TagFilterRule(String tagName, String column, String operator, String rawValue, ValueType valueType) {
        this.tagName = tagName;
        this.column = column;
        this.operator = operator == null || operator.isEmpty() ? "=" : operator;
        this.rawValue = rawValue;
        this.valueType = valueType == null ? ValueType.STRING : valueType;
    }

    public String getTagName() {
        return tagName;
    }

    public String getColumn() {
        return column;
    }

    public String getOperator() {
        return operator;
    }

    public String getRawValue() {
        return rawValue;
    }

    public ValueType getValueType() {
        return valueType;
    }

    public boolean hasColumn() {
        return column != null && !column.trim().isEmpty();
    }

    /**
     * 将配置值转换为可用于PreparedStatement的参数类型
     * <p>
     * 根据值类型（STRING、NUMBER、BOOLEAN）将原始值转换为对应的SQL参数类型。
     * - BOOLEAN类型：转换为0或1
     * - NUMBER类型：转换为BigDecimal
     * - STRING类型：保持原样
     * </p>
     *
     * @return SQL参数值，如果原始值为null则返回null
     * @example
     * <pre>
     *     TagFilterRule rule = new TagFilterRule("tag1", "COLUMN1", "=", "100", ValueType.NUMBER);
     *     Object sqlValue = rule.toSqlValue();
     *     // 返回: BigDecimal(100)
     *     TagFilterRule rule2 = new TagFilterRule("tag2", "COLUMN2", "=", "true", ValueType.BOOLEAN);
     *     Object sqlValue2 = rule2.toSqlValue();
     *     // 返回: 1（true转换为1）
     * </pre>
     */
    public Object toSqlValue() {
        if (rawValue == null) {
            return null;
        }
        switch (valueType) {
            case BOOLEAN:
                return parseBooleanValue(rawValue) ? 1 : 0;
            case NUMBER:
                try {
                    return new BigDecimal(rawValue);
                } catch (NumberFormatException ex) {
                    return rawValue;
                }
            case STRING:
            default:
                return rawValue;
        }
    }

    private boolean parseBooleanValue(String value) {
        String normalized = value.trim().toLowerCase(Locale.ROOT);
        if ("1".equals(normalized) || "true".equals(normalized) || "yes".equals(normalized) || "是".equals(normalized)) {
            return true;
        }
        return "0".equals(normalized) ? false : Boolean.parseBoolean(normalized);
    }

    @Override
    public String toString() {
        return "TagFilterRule{" +
            "tagName='" + tagName + '\'' +
            ", column='" + column + '\'' +
            ", operator='" + operator + '\'' +
            ", rawValue='" + rawValue + '\'' +
            ", valueType=" + valueType +
            '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        TagFilterRule that = (TagFilterRule) o;
        return Objects.equals(tagName, that.tagName) &&
            Objects.equals(column, that.column) &&
            Objects.equals(operator, that.operator) &&
            Objects.equals(rawValue, that.rawValue) &&
            valueType == that.valueType;
    }

    @Override
    public int hashCode() {
        return Objects.hash(tagName, column, operator, rawValue, valueType);
    }
}


