package org.example.strategy.orchestrator;

import org.example.service.delivery.DeliveryCombination;

import java.math.BigDecimal;
import java.util.Map;

/**
 * Canonical strategy execution context.
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

