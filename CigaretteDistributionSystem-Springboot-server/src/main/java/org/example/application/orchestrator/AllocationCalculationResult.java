package org.example.application.orchestrator;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * 算法分配计算结果载体。
 *
 * @author Robin
 * @version 1.0
 * @since 2025-12-11
 */
public class AllocationCalculationResult {

    private final boolean success;
    private final List<String> targetList;
    private final BigDecimal[][] allocationMatrix;
    private final BigDecimal[][] customerMatrix;
    private final String algorithm;
    private final String errorMessage;

    public AllocationCalculationResult(boolean success,
                                       List<String> targetList,
                                       BigDecimal[][] allocationMatrix,
                                       BigDecimal[][] customerMatrix,
                                       String algorithm,
                                       String errorMessage) {
        this.success = success;
        this.targetList = targetList != null ? targetList : new ArrayList<>();
        this.allocationMatrix = allocationMatrix;
        this.customerMatrix = customerMatrix;
        this.algorithm = algorithm;
        this.errorMessage = errorMessage;
    }

    /**
     * 构建成功结果。
     *
     * @param targetList       目标区域列表
     * @param allocationMatrix 分配矩阵
     * @param customerMatrix   客户矩阵
     * @param algorithm        算法名称
     * @return 成功结果
     */
    public static AllocationCalculationResult success(List<String> targetList,
                                                       BigDecimal[][] allocationMatrix,
                                                       BigDecimal[][] customerMatrix,
                                                       String algorithm) {
        return new AllocationCalculationResult(true, targetList, allocationMatrix, customerMatrix, algorithm, null);
    }

    /**
     * 构建失败结果。
     *
     * @param errorMessage 失败原因
     * @return 失败结果
     */
    public static AllocationCalculationResult failure(String errorMessage) {
        return new AllocationCalculationResult(false, null, null, null, null, errorMessage);
    }

    public boolean isSuccess() {
        return success;
    }

    public List<String> getTargetList() {
        return targetList;
    }

    public BigDecimal[][] getAllocationMatrix() {
        return allocationMatrix;
    }

    public BigDecimal[][] getCustomerMatrix() {
        return customerMatrix;
    }

    public String getAlgorithm() {
        return algorithm;
    }

    public String getErrorMessage() {
        return errorMessage;
    }
}

