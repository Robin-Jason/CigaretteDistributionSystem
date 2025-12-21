package org.example.domain.service.rule.impl;

import org.example.domain.service.rule.CityWideRatioRule;

/**
 * 全市投放占比校验规则领域服务实现。
 *
 * <p>纯领域逻辑，不依赖 Spring 或持久化层。</p>
 *
 * @author Robin
 * @since 2025-12-18
 */
public class CityWideRatioRuleImpl implements CityWideRatioRule {

    @Override
    /**
     * @param totalCount       本期投放卷烟总数
     * @param cityWideCount    投放区域为“全市”的卷烟数量
     * @param minRequiredRatio 最小占比阈值
     *
     * @example totalCount=50, cityWideCount=25, minRequiredRatio=0.4 -> 不抛异常
     */
    public void validate(long totalCount, long cityWideCount, double minRequiredRatio) {
        if (totalCount <= 0) {
            // 无数据时不做比例限制，由上层决定是否单独拦截“空导入”
            return;
        }
        if (minRequiredRatio <= 0D) {
            // 阈值为 0 或负数时视为未开启约束
            return;
        }
        double ratio = cityWideCount / (double) totalCount;
        if (ratio + 1e-8 < minRequiredRatio) {
            throw new IllegalArgumentException(
                    String.format("全市投放卷烟占比不足：当前=%.4f, 要求至少=%.4f", ratio, minRequiredRatio));
        }
    }
}


