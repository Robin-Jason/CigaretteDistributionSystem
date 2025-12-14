package org.example.infrastructure.algorithm.impl;

import lombok.extern.slf4j.Slf4j;
import org.example.infrastructure.algorithm.ColumnWiseAdjustmentAlgorithm;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

/**
 * 默认多区域无权重分配算法实现（新算法）：
 * <ol>
 *     <li>粗调阶段：从最高档位（HG）到最低档位（LG），多轮逐档位列+1（整列+1），直到刚好超出目标。</li>
 *     <li>高档位微调阶段：撤销粗调方案最后一次档位列+1操作，基于此进行迭代微调，生成多个候选方案。</li>
 *     <li>方案选择：从4个候选方案中选择误差最小的方案（误差相同时选择编号较大的方案）。</li>
 *     <li>非递增约束：确保每一区域的档位为非递增序列（D30 >= ... >= D1）。</li>
 * </ol>
 */
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class DefaultColumnWiseAdjustmentAlgorithm implements ColumnWiseAdjustmentAlgorithm {

    private static final int GRADE_COUNT = 30;

    @Override
    public BigDecimal[][] distribute(List<String> segments,
                                     BigDecimal[][] customerMatrix,
                                     BigDecimal targetAmount,
                                     Comparator<Integer> segmentComparator) {
        if (segments == null || segments.isEmpty()
                || customerMatrix == null
                || targetAmount == null
                || targetAmount.compareTo(BigDecimal.ZERO) <= 0) {
            log.warn("ColumnWise 输入非法，返回空矩阵");
            return new BigDecimal[0][0];
        }

        validateMatrixDimensions(segments.size(), customerMatrix);

        // 防御性检查：如果存在某个区域30个档位客户数全部为0，则该卷烟无法进行有效分配
        for (int r = 0; r < customerMatrix.length; r++) {
            BigDecimal[] row = customerMatrix[r];
            boolean allZero = true;
            for (int g = 0; g < row.length; g++) {
                if (row[g] != null && row[g].compareTo(BigDecimal.ZERO) > 0) {
                    allZero = false;
                    break;
                }
            }
            if (allZero) {
                throw new IllegalStateException(
                        "ColumnWise 分配失败：区域索引 " + r + " 的30个档位客户数全部为0，已停止本卷烟分配以避免死循环");
            }
        }

        try {
            // 执行新算法
            BigDecimal[][] result = distributeMultiRegion(segments.size(), customerMatrix, targetAmount, segmentComparator);
            
            // 强制执行单调性约束作为双重保险
            enforceMonotonicConstraint(result);
            
            BigDecimal finalAmount = calculateTotalAmount(result, customerMatrix);
            log.info("ColumnWise 分配完成，目标: {}, 实际: {}, 误差: {}",
                    targetAmount, finalAmount, targetAmount.subtract(finalAmount).abs());
            return result;
        } catch (Exception ex) {
            log.error("ColumnWise 分配异常，返回当前结果", ex);
            return initMatrix(segments.size());
        }
    }
    
    /**
     * 多区域分配算法主流程（新算法）
     * 
     * @param segmentCount 区域数量
     * @param customerMatrix 客户数矩阵
     * @param targetAmount 目标预投放量
     * @param segmentComparator 区域比较器（用于排序）
     * @return 分配结果矩阵
     */
    private BigDecimal[][] distributeMultiRegion(int segmentCount,
                                                  BigDecimal[][] customerMatrix,
                                                  BigDecimal targetAmount,
                                                  Comparator<Integer> segmentComparator) {
        // 1. 粗调阶段（候选方案1）
        BigDecimal[][] candidate1 = coarseAdjustment(segmentCount, customerMatrix, targetAmount);
        BigDecimal amount1 = calculateTotalAmount(candidate1, customerMatrix);
        BigDecimal error1 = targetAmount.subtract(amount1).abs();
        
        // 如果恰好等于目标，直接返回
        if (amount1.compareTo(targetAmount) == 0) {
            log.info("粗调恰好等于目标，直接返回候选方案1");
            return candidate1;
        }
        
        // 2. 高档位微调阶段
        // 撤销粗调方案最后一次档位列+1操作作为候选方案2
        BigDecimal[][] candidate2 = rollbackLastColumnIncrement(candidate1, customerMatrix, targetAmount);
        BigDecimal amount2 = calculateTotalAmount(candidate2, customerMatrix);
        BigDecimal error2 = targetAmount.subtract(amount2).abs();
        
        BigDecimal[][] candidate3 = null;
        BigDecimal[][] candidate4 = null;
        BigDecimal[][] candidate5 = null;
        BigDecimal error3 = null;
        BigDecimal error4 = null;
        BigDecimal error5 = null;
        
        // 迭代微调：每轮基于当前余量做一次完整的HG→LG填充，终止条件为余量 < HG档位列客户总数
        int refineIterations = 0;
        BigDecimal currentAmount = amount2;
        final BigDecimal hgColumnIncrement = calculateColumnIncrement(customerMatrix, 0);
        boolean loggedZeroHgColumn = false;

        while (true) {
            if (++refineIterations > 10_000_000) {
                log.warn("ColumnWise 高档位微调达到最大迭代上限1e7，提前终止微调，使用当前候选方案2/后续方案进行选择");
                break;
            }

            BigDecimal remainder = targetAmount.subtract(currentAmount);
            if (hgColumnIncrement.compareTo(BigDecimal.ZERO) > 0) {
                if (remainder.compareTo(hgColumnIncrement) < 0) {
                    candidate3 = deepCopy(candidate2);
                    BigDecimal amount3 = currentAmount;
                    error3 = targetAmount.subtract(amount3).abs();

                    // 旧候选方案4：HG整列+1
                    if (canIncrementColumn(candidate2, 0)) {
                        candidate4 = deepCopy(candidate2);
                        addFullColumn(candidate4, 0);
                        BigDecimal amount4 = currentAmount.add(hgColumnIncrement);
                        error4 = targetAmount.subtract(amount4).abs();
                    }

                    // 新候选方案5：基于候选3在HG列选择部分区域+1，使误差尽量缩小（0-1背包 / 子集和）
                    HgSubsetCandidate4Result subsetResult =
                            generateHgSubsetCandidate4(candidate3, customerMatrix, remainder, currentAmount, targetAmount);
                    if (subsetResult != null && subsetResult.matrix != null && subsetResult.error != null) {
                        candidate5 = subsetResult.matrix;
                        error5 = subsetResult.error;
                    }
                    break;
                }
            } else if (!loggedZeroHgColumn) {
                log.warn("ColumnWise 微调：HG档位列客户数为0，无法依据余量阈值提前终止，将依赖精确命中或迭代上限");
                loggedZeroHgColumn = true;
            }

            ColumnFillResult iterationResult = runColumnFillIteration(
                    candidate2, customerMatrix, targetAmount, currentAmount);

            if (!iterationResult.progressMade) {
                log.warn("ColumnWise 微调无法继续推进，提前结束");
                break;
            }

            candidate2 = iterationResult.matrix;

            if (iterationResult.hitExactTarget) {
                currentAmount = iterationResult.amount;
                candidate3 = deepCopy(candidate2);
                BigDecimal amount3 = currentAmount;
                error3 = targetAmount.subtract(amount3).abs();
                break;
            }

            if (iterationResult.exceeded) {
                subtractFullColumn(candidate2, iterationResult.lastGrade);
                currentAmount = iterationResult.amount.subtract(iterationResult.lastIncrement);
            } else {
                currentAmount = iterationResult.amount;
            }
        }

        amount2 = currentAmount;
        error2 = targetAmount.subtract(amount2).abs();
        
        // 3. 方案选择：选择误差最小的方案
        // 如果误差相同，优先选择编号较大的方案（候选方案5 > 4 > 3 > 2 > 1）
        BigDecimal[][] bestCandidate = candidate1;
        BigDecimal bestError = error1;
        int bestCandidateIndex = 1;
        
        // 候选方案2
        if (error2.compareTo(bestError) < 0 || 
            (error2.compareTo(bestError) == 0 && 2 > bestCandidateIndex)) {
            bestCandidate = candidate2;
            bestError = error2;
            bestCandidateIndex = 2;
        }
        
        // 候选方案3
        if (candidate3 != null && error3 != null) {
            if (error3.compareTo(bestError) < 0 || 
                (error3.compareTo(bestError) == 0 && 3 > bestCandidateIndex)) {
                bestCandidate = candidate3;
                bestError = error3;
                bestCandidateIndex = 3;
            }
        }
        
        // 候选方案4（整列+1）
        if (candidate4 != null && error4 != null) {
            if (error4.compareTo(bestError) < 0 || 
                (error4.compareTo(bestError) == 0 && 4 > bestCandidateIndex)) {
                bestCandidate = candidate4;
                bestError = error4;
                bestCandidateIndex = 4;
            }
        }
        
        // 候选方案5（DP子集+1）
        if (candidate5 != null && error5 != null) {
            if (error5.compareTo(bestError) < 0 || 
                (error5.compareTo(bestError) == 0 && 5 > bestCandidateIndex)) {
                bestCandidate = candidate5;
                bestError = error5;
                bestCandidateIndex = 5;
            }
        }

        log.info("ColumnWise 候选方案误差：e1={}, e2={}, e3={}, e4(整列+1)={}, e5(DP子集)={}",
                error1, error2, error3, error4, error5);
        log.info("多区域分配完成，目标: {}, 选择方案{}，误差: {}", 
                 targetAmount, bestCandidateIndex, bestError);
        return bestCandidate;
    }

    /**
     * 粗调阶段：从HG到LG，多轮逐档位列+1（整列+1），直到刚好超出目标
     * 
     * @param segmentCount 区域数量
     * @param customerMatrix 客户数矩阵
     * @param targetAmount 目标预投放量
     * @return 粗调后的分配矩阵（候选方案1）
     */
    private BigDecimal[][] coarseAdjustment(int segmentCount,
                                            BigDecimal[][] customerMatrix,
                                            BigDecimal targetAmount) {
        log.debug("ColumnWise 粗调开始，目标: {}", targetAmount);
        BigDecimal[][] allocationMatrix = initMatrix(segmentCount);
        BigDecimal currentAmount = BigDecimal.ZERO;

        while (true) {
            boolean hasProgress = false;
            for (int grade = 0; grade < GRADE_COUNT; grade++) {
                // 计算该档位列+1的增量（所有区域该档位客户数总和）
                BigDecimal gradeIncrement = BigDecimal.ZERO;
                for (int segment = 0; segment < segmentCount; segment++) {
                    BigDecimal customerCount = customerMatrix[segment][grade];
                    if (customerCount != null) {
                        gradeIncrement = gradeIncrement.add(customerCount);
                    }
                }
                
                // 如果恰好等于目标，直接返回
                if (currentAmount.add(gradeIncrement).compareTo(targetAmount) == 0) {
                    addFullColumn(allocationMatrix, grade);
                    log.debug("粗调恰好等于目标，直接返回");
                    return allocationMatrix;
                }
                
                // 如果再加该档位列会超出目标，停止
                if (currentAmount.add(gradeIncrement).compareTo(targetAmount) > 0) {
                    log.debug("ColumnWise 粗调停止：再加 D{} 会超出目标，当前 {} 剩余 {}",
                            30 - grade, currentAmount, targetAmount.subtract(currentAmount));
                    return allocationMatrix;
                }
                
                // 整列+1（所有区域该档位都+1）
                addFullColumn(allocationMatrix, grade);
                currentAmount = currentAmount.add(gradeIncrement);
                hasProgress = true;
            }
            if (!hasProgress) {
                break;
            }
        }
        return allocationMatrix;
    }

    /**
     * 撤销导致超出目标的最后一次档位列+1操作（整列+1）
     * 
     * @param allocationMatrix 当前分配矩阵
     * @param customerMatrix 客户数矩阵
     * @param targetAmount 目标预投放量
     * @return 撤销后的分配矩阵
     */
    private BigDecimal[][] rollbackLastColumnIncrement(BigDecimal[][] allocationMatrix,
                                          BigDecimal[][] customerMatrix,
                                                       BigDecimal targetAmount) {
        BigDecimal[][] result = deepCopy(allocationMatrix);
        int segmentCount = result.length;
        // 先整体计算一次当前实际投放量，后续用增量方式更新，避免每次尝试都整矩阵重算
        BigDecimal currentAmount = calculateTotalAmount(result, customerMatrix);

        // 从低档位到高档位查找最后一次整列+1操作
        for (int grade = GRADE_COUNT - 1; grade >= 0; grade--) {
            // 检查该档位是否所有区域都有分配值
            boolean allHaveValue = true;
            BigDecimal gradeIncrement = BigDecimal.ZERO;
            for (int segment = 0; segment < segmentCount; segment++) {
                if (result[segment][grade].compareTo(BigDecimal.ZERO) <= 0) {
                    allHaveValue = false;
                    break;
                }
                BigDecimal customerCount = customerMatrix[segment][grade];
                if (customerCount != null) {
                    gradeIncrement = gradeIncrement.add(customerCount);
                }
            }

            if (allHaveValue) {
                // 撤销整列+1后的实际投放量
                BigDecimal newAmount = currentAmount.subtract(gradeIncrement);

                if (newAmount.compareTo(targetAmount) <= 0) {
                    // 撤销后不超出，正式更新分配与当前量
                    for (int segment = 0; segment < segmentCount; segment++) {
                        result[segment][grade] = result[segment][grade].subtract(BigDecimal.ONE);
                    }
                    currentAmount = newAmount;
                    log.debug("撤销档位 {} 的整列+1操作，当前量: {}, 目标: {}",
                            30 - grade, newAmount, targetAmount);
                    return result;
        }
                // 否则跳过该档位，尝试更高档位的回撤
            }
        }

        return result;
    }

    /**
     * 从当前候选方案出发执行一轮 HG→LG 整列填充，直到刚好超过或等于目标。
     */
    private ColumnFillResult runColumnFillIteration(BigDecimal[][] baseMatrix,
                                                   BigDecimal[][] customerMatrix,
                                                   BigDecimal targetAmount,
                                                    BigDecimal startAmount) {
        ColumnFillResult result = new ColumnFillResult();
        BigDecimal[][] working = deepCopy(baseMatrix);
        BigDecimal currentAmount = startAmount;
        boolean hasProgress = false;

        while (true) {
            boolean roundProgress = false;
            for (int grade = 0; grade < GRADE_COUNT; grade++) {
                if (!canIncrementColumn(working, grade)) {
                    continue;
                }

                BigDecimal gradeIncrement = calculateColumnIncrement(customerMatrix, grade);
                addFullColumn(working, grade);
                hasProgress = true;
                roundProgress = true;

                BigDecimal newAmount = currentAmount.add(gradeIncrement);
                if (newAmount.compareTo(targetAmount) > 0) {
                    result.matrix = working;
                    result.amount = newAmount;
                    result.exceeded = true;
                    result.progressMade = true;
                    result.lastGrade = grade;
                    result.lastIncrement = gradeIncrement;
                    return result;
                }

                currentAmount = newAmount;
                result.matrix = working;
                result.amount = currentAmount;
                result.progressMade = true;

                if (newAmount.compareTo(targetAmount) == 0) {
                    result.hitExactTarget = true;
                    return result;
                }
            }

            if (!roundProgress) {
                break;
            }
        }

        result.matrix = working;
        result.amount = currentAmount;
        result.progressMade = hasProgress;
        return result;
    }
    
    /**
     * 强制执行单调性约束作为双重保险
     * 
     * @param matrix 分配矩阵
     */
    private void enforceMonotonicConstraint(BigDecimal[][] matrix) {
        for (BigDecimal[] row : matrix) {
            if (row == null) {
                    continue;
                }
            for (int grade = 1; grade < GRADE_COUNT && grade < row.length; grade++) {
                if (row[grade].compareTo(row[grade - 1]) > 0) {
                    row[grade] = row[grade - 1];  // 强制调整
                }
            }
        }
    }



    private void addFullColumn(BigDecimal[][] matrix, int grade) {
        for (BigDecimal[] row : matrix) {
            row[grade] = row[grade].add(BigDecimal.ONE);
        }
    }

    private void subtractFullColumn(BigDecimal[][] matrix, int grade) {
            for (BigDecimal[] row : matrix) {
            row[grade] = row[grade].subtract(BigDecimal.ONE);
                }
    }

    private BigDecimal calculateColumnIncrement(BigDecimal[][] customerMatrix, int grade) {
        BigDecimal increment = BigDecimal.ZERO;
        for (BigDecimal[] row : customerMatrix) {
            BigDecimal customerCount = row[grade];
            if (customerCount != null) {
                increment = increment.add(customerCount);
            }
        }
        return increment;
    }

    private boolean canIncrementColumn(BigDecimal[][] matrix, int grade) {
        if (grade == 0) {
            return true;
        }
        for (BigDecimal[] row : matrix) {
            if (row[grade - 1].compareTo(row[grade].add(BigDecimal.ONE)) < 0) {
                return false;
            }
        }
        return true;
    }



    private BigDecimal calculateTotalAmount(BigDecimal[][] allocationMatrix,
                                            BigDecimal[][] customerMatrix) {
        BigDecimal total = BigDecimal.ZERO;
        for (int segment = 0; segment < allocationMatrix.length; segment++) {
            for (int grade = 0; grade < GRADE_COUNT; grade++) {
                total = total.add(allocationMatrix[segment][grade].multiply(customerMatrix[segment][grade]));
            }
        }
        return total;
    }

    private BigDecimal[][] initMatrix(int segmentCount) {
        BigDecimal[][] matrix = new BigDecimal[segmentCount][GRADE_COUNT];
        for (int i = 0; i < segmentCount; i++) {
            Arrays.fill(matrix[i] = new BigDecimal[GRADE_COUNT], BigDecimal.ZERO);
        }
        return matrix;
    }

    /**
     * 基于候选方案3，在HG列（grade=0）选择部分区域+1，使得总增量尽量贴近当前余量 remainder。
     * 这是一个典型的 0-1 背包 / 子集和问题：每个区域的“重量”为该区域 HG 档客户数。
     */
    private HgSubsetCandidate4Result generateHgSubsetCandidate4(BigDecimal[][] baseMatrix,
                                                                BigDecimal[][] customerMatrix,
                                                                BigDecimal remainder,
                                                                BigDecimal currentAmount,
                                                                BigDecimal targetAmount) {
        if (remainder == null || remainder.compareTo(BigDecimal.ZERO) <= 0) {
            return null;
        }

        int segmentCount = baseMatrix.length;

        // 将余量视为整数进行处理（多区域无权重场景下，客户数与目标量本身就是整数）
        long remainderLong = remainder.longValue();
        if (remainderLong <= 0) {
            return null;
        }

        // 预处理每个可选区域的“重量” = HG档客户数，同时统计总重量与最大重量
        long[] weights = new long[segmentCount];
        int[] indexMap = new int[segmentCount]; // 有效区域索引 -> 实际segment索引
        int itemCount = 0;
        long totalWeight = 0L;
        long maxWeight = 0L;
        boolean[] zeroWeightEligible = new boolean[segmentCount]; // HG客户数为0但可+1且不影响单调性的区域
        for (int s = 0; s < segmentCount; s++) {
            BigDecimal customerCount = customerMatrix[s][0];

            // HG 客户数为 0 的区域：本轮在 DP 中视为“零权重物品”，不参与子集和搜索，
            // 但在构造最终候选方案时会无成本地一起 +1。
            if (customerCount == null || customerCount.compareTo(BigDecimal.ZERO) <= 0) {
                zeroWeightEligible[s] = true;
                continue;
            }

            long w = customerCount.longValue(); // 客户数保证为自然数
            if (w <= 0) {
                zeroWeightEligible[s] = true;
                continue;
            }

            weights[itemCount] = w;
            indexMap[itemCount] = s;
            itemCount++;
            totalWeight += w;
            if (w > maxWeight) {
                maxWeight = w;
            }
        }

        if (itemCount == 0) {
            return null;
        }

        // DP 容量：覆盖所有正权重区域的总和，确保“整列+1”的组合必然在状态空间内
        if (totalWeight <= 0 || totalWeight > 10_000_000L) {
            // 极端情况下放弃DP以避免内存过大，但这种场景下仍有候选4兜底
            return null;
        }
        int capacity = (int) totalWeight;

        boolean[] dp = new boolean[capacity + 1];
        int[] prevSum = new int[capacity + 1];
        int[] prevItem = new int[capacity + 1];
        Arrays.fill(prevItem, -1);

        dp[0] = true;
        prevSum[0] = -1;

        // 0-1 背包：选出总增量尽量接近（且不超过）remainder 的区域集合
        for (int i = 0; i < itemCount; i++) {
            int w = (int) weights[i];
            for (int s = capacity; s >= w; s--) {
                if (!dp[s] && dp[s - w]) {
                    dp[s] = true;
                    prevSum[s] = s - w;
                    prevItem[s] = i;
                }
            }
        }

        int bestSum = 0;
        long bestDiff = Long.MAX_VALUE;
        for (int s = 1; s <= capacity; s++) {
            if (dp[s]) {
                long diff = Math.abs(remainderLong - s);
                if (diff < bestDiff || (diff == bestDiff && s > bestSum)) {
                    bestDiff = diff;
                    bestSum = s;
                }
            }
        }
        if (bestSum == 0) {
            return null;
        }

        // 回溯选中的区域集合
        boolean[] chosen = new boolean[segmentCount];
        int cur = bestSum;
        while (cur > 0) {
            int item = prevItem[cur];
            if (item < 0) {
                break;
            }
            int segIndex = indexMap[item];
            chosen[segIndex] = true;
            cur = prevSum[cur];
        }

        BigDecimal[][] candidate4 = deepCopy(baseMatrix);
        for (int s = 0; s < segmentCount; s++) {
            if (chosen[s]) {
                candidate4[s][0] = candidate4[s][0].add(BigDecimal.ONE);
            }
        }
        // 对所有 HG客户数为0 且可+1的区域，同样无成本地+1，以尽可能多覆盖区域
        for (int s = 0; s < segmentCount; s++) {
            if (zeroWeightEligible[s]) {
                candidate4[s][0] = candidate4[s][0].add(BigDecimal.ONE);
            }
        }

        BigDecimal amount4 = currentAmount.add(BigDecimal.valueOf(bestSum));
        BigDecimal error4 = targetAmount.subtract(amount4).abs();

        HgSubsetCandidate4Result result = new HgSubsetCandidate4Result();
        result.matrix = candidate4;
        result.amount = amount4;
        result.error = error4;
        return result;
    }

    private static final class HgSubsetCandidate4Result {
        private BigDecimal[][] matrix;
        private BigDecimal amount;
        private BigDecimal error;
    }

    private static final class ColumnFillResult {
        private BigDecimal[][] matrix;
        private BigDecimal amount = BigDecimal.ZERO;
        private boolean exceeded;
        private boolean hitExactTarget;
        private boolean progressMade;
        private int lastGrade = -1;
        private BigDecimal lastIncrement = BigDecimal.ZERO;
    }

    private BigDecimal[][] deepCopy(BigDecimal[][] original) {
        BigDecimal[][] copy = new BigDecimal[original.length][GRADE_COUNT];
        for (int i = 0; i < original.length; i++) {
            copy[i] = Arrays.copyOf(original[i], GRADE_COUNT);
        }
        return copy;
    }

    private void validateMatrixDimensions(int count, BigDecimal[][] matrix) {
        if (matrix.length != count) {
            throw new IllegalArgumentException("客户矩阵行数与目标列表不一致");
        }
        for (BigDecimal[] row : matrix) {
            if (row == null || row.length != GRADE_COUNT) {
                throw new IllegalArgumentException("客户矩阵列数必须为 30");
            }
            for (int i = 0; i < row.length; i++) {
                if (row[i] == null) {
                    row[i] = BigDecimal.ZERO;
                }
            }
        }
    }
}



