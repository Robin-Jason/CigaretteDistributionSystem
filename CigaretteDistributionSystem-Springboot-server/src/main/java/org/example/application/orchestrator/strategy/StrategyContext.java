package org.example.application.orchestrator.strategy;

import org.example.application.orchestrator.allocation.RegionCustomerMatrix;
import org.example.domain.model.valueobject.DeliveryCombination;

import java.math.BigDecimal;
import java.util.Map;

/**
 * 策略执行上下文（编排阶段的“只读快照”）。
 *
 * <p>作用：</p>
 * <ul>
 *   <li>将组合解析结果（{@link DeliveryCombination}）、目标投放量与批次信息聚合到一个上下文对象中；</li>
 *   <li>承载本次算法执行所需的区域客户矩阵（已叠加两周一访上浮等预处理）；</li>
 *   <li>为算法引擎提供统一输入，避免在算法层依赖大量散乱参数。</li>
 * </ul>
 *
 * @author Robin
 */
public class StrategyContext {

    private final DeliveryCombination combination;
    private final BigDecimal targetAmount;
    private final Integer year;
    private final Integer month;
    private final Integer weekSeq;
    private final Map<String, Object> cigaretteInfo;
    private final RegionCustomerMatrix customerMatrix;

    public StrategyContext(DeliveryCombination combination,
                           BigDecimal targetAmount,
                           Integer year,
                           Integer month,
                           Integer weekSeq,
                           Map<String, Object> cigaretteInfo,
                           RegionCustomerMatrix customerMatrix) {
        this.combination = combination;
        this.targetAmount = targetAmount;
        this.year = year;
        this.month = month;
        this.weekSeq = weekSeq;
        this.cigaretteInfo = cigaretteInfo;
        this.customerMatrix = customerMatrix;
    }

    public DeliveryCombination getCombination() {
        return combination;
    }

    public BigDecimal getTargetAmount() {
        return targetAmount;
    }

    public Integer getYear() {
        return year;
    }

    public Integer getMonth() {
        return month;
    }

    public Integer getWeekSeq() {
        return weekSeq;
    }

    public Map<String, Object> getCigaretteInfo() {
        return cigaretteInfo;
    }

    public RegionCustomerMatrix getCustomerMatrix() {
        return customerMatrix;
    }
}

