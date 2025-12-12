package org.example.strategy.orchestrator;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;

/**
 * Standard result model for strategy execution.
 */
public class StrategyExecutionResult {

    private final boolean success;
    private final String message;
    private final List<String> targetRegions;
    /**
     * 算法使用的区域客户数矩阵（已应用上浮等处理，顺序与 targetRegions 对齐）。
     */
    private final BigDecimal[][] customerMatrix;
    /**
     * 算法输出的分配矩阵（最终扩展到 30 档位的结果，顺序与 targetRegions 对齐）。
     */
    private final BigDecimal[][] distributionMatrix;

    private StrategyExecutionResult(boolean success,
                                    String message,
                                    List<String> targetRegions,
                                    BigDecimal[][] customerMatrix,
                                    BigDecimal[][] distributionMatrix) {
        this.success = success;
        this.message = message;
        this.targetRegions = targetRegions == null ? Collections.emptyList() : targetRegions;
        this.customerMatrix = customerMatrix;
        this.distributionMatrix = distributionMatrix;
    }

    public static StrategyExecutionResult success(List<String> targetRegions,
                                                  BigDecimal[][] customerMatrix,
                                                  BigDecimal[][] distributionMatrix) {
        return new StrategyExecutionResult(true, "OK", targetRegions, customerMatrix, distributionMatrix);
    }

    public static StrategyExecutionResult failure(String message) {
        return new StrategyExecutionResult(false, message, Collections.emptyList(), null, null);
    }

    public boolean isSuccess() {
        return success;
    }

    public String getMessage() {
        return message;
    }

    public List<String> getTargetRegions() {
        return targetRegions;
    }

    public BigDecimal[][] getCustomerMatrix() {
        return customerMatrix;
    }

    public BigDecimal[][] getDistributionMatrix() {
        return distributionMatrix;
    }
}

