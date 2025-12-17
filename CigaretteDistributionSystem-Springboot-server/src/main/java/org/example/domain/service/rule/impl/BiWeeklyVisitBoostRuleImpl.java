package org.example.domain.service.rule.impl;

import org.example.domain.service.rule.BiWeeklyVisitBoostRule;
import org.example.shared.constants.BusinessConstants;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 双周访销上浮规则领域服务实现。
 * <p>
 * 纯领域逻辑，不含Spring依赖，可独立测试。
 * 该实现复制自 {@link org.example.application.service.coordinator.impl.BiWeeklyVisitBoostServiceImpl} 的核心业务规则方法，
 * 移除了Spring注解和基础设施依赖，保持业务逻辑完全一致。
 * </p>
 *
 * @author Robin
 * @version 1.0
 * @since 2025-12-14
 */
public class BiWeeklyVisitBoostRuleImpl implements BiWeeklyVisitBoostRule {

    @Override
    public boolean needsBoost(String remark) {
        if (remark == null) {
            return false;
        }
        String normalized = remark.replace(" ", "");
        return !normalized.isEmpty() && normalized.contains(BusinessConstants.BI_WEEKLY_VISIT_BOOST_PHRASE);
    }

    @Override
    public void applyBoost(Map<String, MatrixRow> rowIndex, Map<String, BigDecimal[]> increments) {
        if (increments == null || increments.isEmpty()) {
            return;
        }
        for (Map.Entry<String, BigDecimal[]> entry : increments.entrySet()) {
            MatrixRow row = rowIndex.get(entry.getKey());
            if (row == null) {
                continue;
            }
            BigDecimal[] grades = row.getGrades();
            BigDecimal[] addition = entry.getValue();
            ensureLength(grades);
            ensureLength(addition);
            for (int i = 0; i < grades.length && i < addition.length; i++) {
                BigDecimal base = grades[i] == null ? BigDecimal.ZERO : grades[i];
                BigDecimal delta = addition[i] == null ? BigDecimal.ZERO : addition[i];
                grades[i] = base.add(delta);
            }
        }
    }

    @Override
    public Map<String, MatrixRow> indexRows(List<MatrixRow> rows) {
        Map<String, MatrixRow> map = new LinkedHashMap<>();
        if (rows != null) {
            for (MatrixRow row : rows) {
                if (row != null && row.getRegion() != null) {
                    map.put(row.getRegion(), row);
                }
            }
        }
        return map;
    }

    @Override
    public void ensureLength(BigDecimal[] grades) {
        if (grades == null) {
            return;
        }
        if (grades.length < 30) {
            throw new IllegalStateException("档位数组长度不足30位");
        }
    }
}

