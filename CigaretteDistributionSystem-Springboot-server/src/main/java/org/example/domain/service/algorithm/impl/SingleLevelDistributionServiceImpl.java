package org.example.domain.service.algorithm.impl;

import org.example.domain.service.algorithm.SingleLevelDistributionService;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;

/**
 * 单层区域分配领域服务实现
 * <p>
 * 纯领域逻辑，不含Spring依赖，可独立测试。
 * 该实现复制自 {@link org.example.infrastructure.algorithm.impl.DefaultSingleLevelDistributionAlgorithm}，
 * 移除了Spring注解和日志依赖，保持算法逻辑完全一致。
 * </p>
 *
 * @author Robin
 * @version 1.0
 * @since 2025-12-14
 */
public class SingleLevelDistributionServiceImpl implements SingleLevelDistributionService {

    private static final BigDecimal INCREMENT = BigDecimal.ONE;

    @Override
    public BigDecimal[][] distribute(List<String> targetRegions,
                                     BigDecimal[][] regionCustomerMatrix,
                                     BigDecimal targetAmount) {
        if (targetRegions == null || targetRegions.isEmpty()
                || regionCustomerMatrix == null
                || targetAmount == null
                || targetAmount.compareTo(BigDecimal.ZERO) <= 0) {
            return new BigDecimal[0][0];
        }

        int gradeCount = resolveGradeCount(regionCustomerMatrix);
        if (gradeCount <= 0) {
            return new BigDecimal[0][0];
        }

        validateMatrixDimensions(targetRegions.size(), regionCustomerMatrix, gradeCount);

        // 单区域算法，regionCustomerMatrix只有一行
        BigDecimal[] customerRow = regionCustomerMatrix[0];

        BigDecimal[][] allocationMatrix = initMatrix(targetRegions.size(), gradeCount);
        try {
            // 执行新算法
            BigDecimal[] result = distributeSingleRegion(customerRow, targetAmount, gradeCount);
            
            // 将结果复制到分配矩阵（单区域，只有一行）
            allocationMatrix[0] = result;
            
            // 强制单调性约束（确保 D30 >= ... >= D1）
            enforceMonotonicConstraint(allocationMatrix, gradeCount);
        } catch (Exception ex) {
            // 领域服务不依赖日志，静默处理异常
        }
        return allocationMatrix;
    }
    
    /**
     * 从当前候选方案出发执行一轮 HG→LG 填充，直到刚好超过或等于目标。
     */
    private SingleLevelFillResult runSingleLevelFillIteration(BigDecimal[] baseAllocation,
                                                              BigDecimal[] customerRow,
                                                              BigDecimal targetAmount,
                                                              int gradeCount,
                                                              BigDecimal startAmount) {
        SingleLevelFillResult result = new SingleLevelFillResult();
        BigDecimal[] working = Arrays.copyOf(baseAllocation, gradeCount);
        BigDecimal currentAmount = startAmount;

        boolean hasProgress = false;
        while (true) {
            boolean roundProgress = false;
            for (int grade = 0; grade < gradeCount; grade++) {
                if (!isValidIncrementSingle(working, grade)) {
                    continue;
                }

                BigDecimal customerCount = customerRow[grade];
                if (customerCount == null) {
                    customerCount = BigDecimal.ZERO;
                }

                working[grade] = working[grade].add(INCREMENT);
                hasProgress = true;
                roundProgress = true;

                BigDecimal newAmount = currentAmount.add(customerCount);
                if (newAmount.compareTo(targetAmount) > 0) {
                    result.allocation = working;
                    result.amount = newAmount;
                    result.exceeded = true;
                    result.progressMade = true;
                    result.lastGrade = grade;
                    result.lastIncrement = customerCount;
                    return result;
                }

                currentAmount = newAmount;
                result.allocation = working;
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

        result.allocation = working;
        result.amount = currentAmount;
        result.progressMade = hasProgress;
        return result;
    }
    
    /**
     * 单区域分配算法主流程（新算法）
     * 
     * @param customerRow 客户数数组（30个档位）
     * @param targetAmount 目标预投放量
     * @param gradeCount 档位数量（通常为30）
     * @return 分配结果数组（30个档位）
     */
    private BigDecimal[] distributeSingleRegion(BigDecimal[] customerRow,
                                                BigDecimal targetAmount,
                                                int gradeCount) {
        // 防御性检查：如果整行客户数全为0，则该卷烟无法进行有效分配，直接抛出异常
        boolean allZero = true;
        if (customerRow != null) {
            for (int i = 0; i < gradeCount && i < customerRow.length; i++) {
                BigDecimal c = customerRow[i];
                if (c != null && c.compareTo(BigDecimal.ZERO) > 0) {
                    allZero = false;
                    break;
                }
            }
        }
        if (allZero) {
            throw new IllegalStateException("SingleLevel 分配失败：该卷烟在当前区域30个档位客户数全部为0，已停止本卷烟分配以避免死循环");
        }

        // 1. 粗调阶段（候选方案1）
        BigDecimal[] candidate1 = coarseAdjustment(customerRow, targetAmount, gradeCount);
        BigDecimal amount1 = calculateAmount(candidate1, customerRow);
        BigDecimal error1 = targetAmount.subtract(amount1).abs();
        
        // 如果恰好等于目标，直接返回
        if (amount1.compareTo(targetAmount) == 0) {
            return candidate1;
        }
        
        // 2. 高档位微调阶段
        // 撤销粗调方案一次+1操作作为候选方案2
        BigDecimal[] candidate2 = rollbackLastIncrement(candidate1, customerRow, targetAmount, gradeCount);
        BigDecimal amount2 = calculateAmount(candidate2, customerRow);
        BigDecimal error2 = targetAmount.subtract(amount2).abs();
        
        BigDecimal[] candidate3 = null;
        BigDecimal[] candidate4 = null;
        BigDecimal error3 = null;
        BigDecimal error4 = null;
        
        // 迭代微调：每轮基于当前余量做一次完整的HG→LG填充，终止条件为余量<HG客户数
        int refineIterations = 0;
        BigDecimal currentAmount = amount2;
        final BigDecimal hgCustomerCount = customerRow[0] == null ? BigDecimal.ZERO : customerRow[0];
        BigDecimal lastRemainder = null;
        int stagnantRemainderRounds = 0;
        
        while (true) {
            if (++refineIterations > 10_000_000) {
                break;
            }

            BigDecimal remainder = targetAmount.subtract(currentAmount);
            if (hgCustomerCount.compareTo(BigDecimal.ZERO) > 0) {
                if (remainder.compareTo(hgCustomerCount) < 0) {
                    candidate3 = Arrays.copyOf(candidate2, gradeCount);
                    BigDecimal amount3 = currentAmount;
                    error3 = targetAmount.subtract(amount3).abs();

                    if (isValidIncrementSingle(candidate2, 0)) {
                        candidate4 = Arrays.copyOf(candidate2, gradeCount);
                        candidate4[0] = candidate4[0].add(INCREMENT);
                        BigDecimal amount4 = currentAmount.add(hgCustomerCount);
                        error4 = targetAmount.subtract(amount4).abs();
                    }
                    break;
                }
            } else {
                if (lastRemainder != null && remainder.compareTo(lastRemainder) == 0) {
                    stagnantRemainderRounds++;
                } else {
                    stagnantRemainderRounds = 1;
                }
                lastRemainder = remainder;
                if (stagnantRemainderRounds >= 100) {
                    candidate3 = Arrays.copyOf(candidate2, gradeCount);
                    BigDecimal amount3 = currentAmount;
                    error3 = targetAmount.subtract(amount3).abs();
                    break;
                }
            }

            SingleLevelFillResult iterationResult = runSingleLevelFillIteration(
                    candidate2, customerRow, targetAmount, gradeCount, currentAmount);

            if (!iterationResult.progressMade) {
                break;
            }

            candidate2 = iterationResult.allocation;

            if (iterationResult.hitExactTarget) {
                currentAmount = iterationResult.amount;
                candidate3 = Arrays.copyOf(candidate2, gradeCount);
                BigDecimal amount3 = currentAmount;
                error3 = targetAmount.subtract(amount3).abs();
                break;
            }

            if (iterationResult.exceeded) {
                candidate2[iterationResult.lastGrade] =
                        candidate2[iterationResult.lastGrade].subtract(INCREMENT);
                currentAmount = iterationResult.amount.subtract(iterationResult.lastIncrement);
            } else {
                currentAmount = iterationResult.amount;
            }
        }

        amount2 = currentAmount;
        error2 = targetAmount.subtract(amount2).abs();
        
        // 3. 方案选择：选择误差最小的方案
        // 如果误差相同，优先选择编号较大的方案（候选方案4 > 3 > 2 > 1）
        BigDecimal[] bestCandidate = candidate1;
        BigDecimal bestError = error1;
        int bestCandidateIndex = 1;  // 记录方案编号
        
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
        
        // 候选方案4
        if (candidate4 != null && error4 != null) {
            if (error4.compareTo(bestError) < 0 || 
                (error4.compareTo(bestError) == 0 && 4 > bestCandidateIndex)) {
                bestCandidate = candidate4;
                bestError = error4;
                bestCandidateIndex = 4;
            }
        }
        
        return bestCandidate;
    }

    private int resolveGradeCount(BigDecimal[][] regionCustomerMatrix) {
        if (regionCustomerMatrix == null || regionCustomerMatrix.length == 0) {
            return 0;
        }
        BigDecimal[] firstRow = regionCustomerMatrix[0];
        return firstRow == null ? 0 : firstRow.length;
    }

    /**
     * 粗调阶段：从HG到LG，多轮逐档位+1，直到刚好超出目标
     * 
     * @param customerRow 客户数数组（30个档位）
     * @param targetAmount 目标预投放量
     * @param gradeCount 档位数量
     * @return 粗调后的分配方案（候选方案1）
     */
    private BigDecimal[] coarseAdjustment(BigDecimal[] customerRow,
                            BigDecimal targetAmount,
                            int gradeCount) {
        BigDecimal[] allocation = new BigDecimal[gradeCount];
        Arrays.fill(allocation, BigDecimal.ZERO);
        BigDecimal currentAmount = BigDecimal.ZERO;
        int coarseIterations = 0;

        while (true) {
            if (++coarseIterations > 10_000_000) {
                break;
            }
            boolean hasProgress = false;
            for (int grade = 0; grade < gradeCount; grade++) {
                // 检查单调性约束
                if (!isValidIncrementSingle(allocation, grade)) {
                    continue;
                }
                
                BigDecimal customerCount = customerRow[grade];
                if (customerCount == null) {
                    customerCount = BigDecimal.ZERO;
                }
                
                BigDecimal newAmount = currentAmount.add(customerCount);
                
                // 如果恰好等于目标，直接返回
                if (newAmount.compareTo(targetAmount) == 0) {
                    allocation[grade] = allocation[grade].add(INCREMENT);
                    return allocation;
                }
                
                // 如果超出目标，停止
                if (newAmount.compareTo(targetAmount) > 0) {
                    return allocation;
                }
                
                // +1操作
                allocation[grade] = allocation[grade].add(INCREMENT);
                currentAmount = newAmount;
                hasProgress = true;
            }
            
            if (!hasProgress) {
                break;
            }
        }
        
        return allocation;
    }
    
    /**
     * 撤销导致超出目标的最后一次+1操作
     * 返回撤销后的分配方案
     * 
     * @param allocation 当前分配方案
     * @param customerRow 客户数数组
     * @param targetAmount 目标预投放量
     * @param gradeCount 档位数量
     * @return 撤销后的分配方案
     */
    private BigDecimal[] rollbackLastIncrement(BigDecimal[] allocation,
                                               BigDecimal[] customerRow,
                                     BigDecimal targetAmount,
                                     int gradeCount) {
        BigDecimal[] result = Arrays.copyOf(allocation, gradeCount);
        // 先整体计算一次当前实际投放量，后续用增量方式更新，避免每次尝试都整行重算
        BigDecimal currentAmount = calculateAmount(result, customerRow);

        // 从低档位到高档位查找（因为最后超出的是低档位）
        for (int grade = gradeCount - 1; grade >= 0; grade--) {
            if (result[grade].compareTo(BigDecimal.ZERO) > 0) {
                BigDecimal customerCount = customerRow[grade];
                if (customerCount == null) {
                    customerCount = BigDecimal.ZERO;
                }

                // 撤销该档位一次 +1 后的实际投放量
                BigDecimal newAmount = currentAmount.subtract(customerCount);

                if (newAmount.compareTo(targetAmount) <= 0) {
                    // 撤销后不超出，正式更新分配与当前量
                    result[grade] = result[grade].subtract(INCREMENT);
                    currentAmount = newAmount;
                    return result;
                }
                // 否则跳过该档位，尝试更高档位的回撤
            }
        }

        return result;
    }
    
    /**
     * 计算单区域分配方案的实际投放量
     * 
     * @param allocation 分配方案数组
     * @param customerRow 客户数数组
     * @return 实际投放量
     */
    private BigDecimal calculateAmount(BigDecimal[] allocation, BigDecimal[] customerRow) {
        BigDecimal total = BigDecimal.ZERO;
        for (int grade = 0; grade < allocation.length && grade < customerRow.length; grade++) {
            BigDecimal allocationValue = allocation[grade];
            BigDecimal customerCount = customerRow[grade];
            if (allocationValue != null && customerCount != null) {
                total = total.add(allocationValue.multiply(customerCount));
            }
        }
        return total;
    }
    
    /**
     * 检查单区域分配方案中，对指定档位+1是否违反单调性约束
     * 
     * @param allocation 分配方案数组
     * @param grade 档位索引（0=HG, gradeCount-1=LG）
     * @return 如果+1后不违反单调性约束，返回true
     */
    private boolean isValidIncrementSingle(BigDecimal[] allocation, int grade) {
        if (allocation == null || grade < 0 || grade >= allocation.length) {
            return false;
        }
        
        // 临时+1
        allocation[grade] = allocation[grade].add(INCREMENT);
        boolean valid = true;
        
        // 检查是否违反单调性约束（低档位不能大于高档位）
        if (grade > 0 && allocation[grade].compareTo(allocation[grade - 1]) > 0) {
            valid = false;
        }
        
        // 恢复原值
        allocation[grade] = allocation[grade].subtract(INCREMENT);
        return valid;
    }

    private void enforceMonotonicConstraint(BigDecimal[][] matrix, int gradeCount) {
        for (BigDecimal[] row : matrix) {
            if (row == null) {
                continue;
            }
            for (int grade = 1; grade < gradeCount && grade < row.length; grade++) {
                if (row[grade].compareTo(row[grade - 1]) > 0) {
                    row[grade] = row[grade - 1];
                }
            }
        }
    }

    // 保留接口兼容性，当前未使用
    @SuppressWarnings("unused")
    private BigDecimal calculateTotalAmount(BigDecimal[][] allocationMatrix,
                                            BigDecimal[][] regionCustomerMatrix,
                                            int gradeCount) {
        BigDecimal total = BigDecimal.ZERO;
        for (int region = 0; region < allocationMatrix.length; region++) {
            for (int grade = 0; grade < gradeCount; grade++) {
                BigDecimal allocation = allocationMatrix[region][grade];
                BigDecimal customer = regionCustomerMatrix[region][grade];
                if (allocation != null && customer != null) {
                    total = total.add(allocation.multiply(customer));
                }
            }
        }
        return total;
    }

    private BigDecimal[][] initMatrix(int regionCount, int gradeCount) {
        BigDecimal[][] matrix = new BigDecimal[regionCount][gradeCount];
        for (int i = 0; i < regionCount; i++) {
            Arrays.fill(matrix[i] = new BigDecimal[gradeCount], BigDecimal.ZERO);
        }
        return matrix;
    }

    private void validateMatrixDimensions(int regionCount, BigDecimal[][] matrix, int gradeCount) {
        if (matrix.length != regionCount) {
            throw new IllegalArgumentException("客户矩阵行数与区域数量不符");
        }
        for (BigDecimal[] row : matrix) {
            if (row == null || row.length != gradeCount) {
                throw new IllegalArgumentException("客户矩阵列数必须为 " + gradeCount);
            }
            for (int i = 0; i < row.length; i++) {
                if (row[i] == null) {
                    row[i] = BigDecimal.ZERO;
                }
            }
        }
    }

    private static final class SingleLevelFillResult {
        private BigDecimal[] allocation;
        private BigDecimal amount = BigDecimal.ZERO;
        private boolean exceeded;
        private boolean hitExactTarget;
        private boolean progressMade;
        private int lastGrade = -1;
        private BigDecimal lastIncrement = BigDecimal.ZERO;
    }
}

