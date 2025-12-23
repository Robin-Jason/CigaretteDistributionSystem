package org.example.domain.service.algorithm.impl;

import org.example.domain.model.valueobject.GradeRange;
import org.example.domain.service.algorithm.PriceBandTruncationService;
import org.example.shared.constants.GradeConstants;
import org.example.shared.util.ActualDeliveryCalculator;
import org.example.shared.util.WriteBackHelper;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 价位段截断与微调算法实现。
 * <p>
 * 纯领域逻辑，不含 Spring 依赖，可独立测试。
 * </p>
 *
 * @author Robin
 * @since 2025-12-22
 */
public class PriceBandTruncationServiceImpl implements PriceBandTruncationService {

    private static final int MAX_ITERATIONS = 10_000_000;

    @Override
    public void truncateAndAdjust(Map<Integer, List<Map<String, Object>>> bandsNeedingTruncation,
                                  BigDecimal[] cityCustomerRow,
                                  GradeRange gradeRange,
                                  Integer year, Integer month, Integer weekSeq) {
        // 委托给支持两周一访的重载方法（不传入上浮客户数）
        truncateAndAdjust(bandsNeedingTruncation, cityCustomerRow, null, gradeRange, year, month, weekSeq);
    }

    @Override
    public void truncateAndAdjust(Map<Integer, List<Map<String, Object>>> bandsNeedingTruncation,
                                  BigDecimal[] baseCustomerRow,
                                  BigDecimal[] boostedCustomerRow,
                                  GradeRange gradeRange,
                                  Integer year, Integer month, Integer weekSeq) {
        if (bandsNeedingTruncation == null || bandsNeedingTruncation.isEmpty()) {
            return;
        }

        final int maxIndex = gradeRange != null ? gradeRange.getMaxIndex() : GradeConstants.HIGHEST_GRADE_INDEX;
        final int minIndex = gradeRange != null ? gradeRange.getMinIndex() : GradeConstants.LOWEST_GRADE_INDEX;
        
        // 收集所有全0异常信息
        List<String> zeroAllocationErrors = new ArrayList<>();

        for (Map.Entry<Integer, List<Map<String, Object>>> entry : bandsNeedingTruncation.entrySet()) {
            Integer band = entry.getKey();
            List<Map<String, Object>> group = entry.getValue();
            if (group == null || group.size() <= 1) {
                continue;
            }

            // 保存截断前的分配结果（用于异常时恢复）
            List<BigDecimal[]> preTruncationGrades = savePreTruncationGrades(group);

            // 清零范围外的档位
            enforceGradeRangeBounds(group, maxIndex, minIndex);

            // 从 minIndex 向 maxIndex 扫描，找到首个"该列至少有两支卷烟分配量 > 0"的索引
            int cutoffIndex = findCutoffIndex(group, maxIndex, minIndex);

            if (cutoffIndex < 0) {
                continue;
            }

            // 将低于 cutoffIndex 的所有档位列清零
            truncateLowerGrades(group, cutoffIndex, minIndex);

            // 误差微调：每支卷烟独立执行（根据备注选择客户数数组）
            adjustErrorsForBandWithBoost(group, baseCustomerRow, boostedCustomerRow, maxIndex, cutoffIndex);

            // 再次确保范围外的档位为0
            enforceGradeRangeBounds(group, maxIndex, minIndex);

            // 全0检测（收集异常信息，不立即抛出）
            checkZeroAllocation(group, band, preTruncationGrades, year, month, weekSeq, zeroAllocationErrors);
        }
        
        // 所有价位段处理完成后，如果有全0异常，统一抛出
        if (!zeroAllocationErrors.isEmpty()) {
            String errorMessage = String.join("\n", zeroAllocationErrors);
            throw new IllegalStateException("价位段截断异常（共" + zeroAllocationErrors.size() + "支卷烟）：\n" + errorMessage);
        }
    }

    /**
     * 确保分配矩阵在GradeRange范围外的档位为0。
     *
     * @param group    同一价位段内的卷烟列表
     * @param maxIndex 最高档位索引
     * @param minIndex 最低档位索引
     */
    private void enforceGradeRangeBounds(List<Map<String, Object>> group, int maxIndex, int minIndex) {
        if (group == null) {
            return;
        }

        for (Map<String, Object> row : group) {
            BigDecimal[] grades = (BigDecimal[]) row.get("GRADES");
            if (grades == null) {
                continue;
            }

            // 清零范围外的档位
            for (int i = 0; i < maxIndex && i < grades.length; i++) {
                grades[i] = BigDecimal.ZERO;
            }
            for (int i = minIndex + 1; i < grades.length; i++) {
                grades[i] = BigDecimal.ZERO;
            }
        }
    }

    /**
     * 保存截断前的分配结果，用于异常时恢复。
     *
     * @param group 同一价位段内的卷烟列表，每个 Map 需包含 "GRADES" 字段
     * @return 分配结果副本列表，与输入列表一一对应
     *
     * @example
     * <pre>{@code
     * List<Map<String, Object>> group = ...;
     * List<BigDecimal[]> backup = savePreTruncationGrades(group);
     * // 执行截断操作...
     * // 如需恢复：group.get(i).put("GRADES", backup.get(i));
     * }</pre>
     */
    private List<BigDecimal[]> savePreTruncationGrades(List<Map<String, Object>> group) {
        List<BigDecimal[]> result = new ArrayList<>();
        for (Map<String, Object> row : group) {
            BigDecimal[] grades = (BigDecimal[]) row.get("GRADES");
            if (grades != null) {
                result.add(grades.clone());
            } else {
                result.add(new BigDecimal[GradeConstants.GRADE_COUNT]);
            }
        }
        return result;
    }

    /**
     * 从 minIndex（低档位）向 maxIndex（高档位）扫描，找到首个"该列至少有两支卷烟分配量 > 0"的索引。
     * <p>
     * 该索引作为截断点，低于此索引的档位将被清零。
     * </p>
     *
     * @param group        同一价位段内的卷烟列表
     * @param maxIndex     最高档位索引（如 D30=0）
     * @param minIndex     最低档位索引（如 D1=29）
     * @return 截断点索引；如果未找到满足条件的列，返回 -1
     *
     * @example
     * <pre>{@code
     * // 假设 D10（索引20）是首个有两支卷烟分配量>0的列
     * int cutoff = findCutoffIndex(group, 0, 29);
     * // cutoff = 20，表示 D11~D1（索引21~29）将被清零
     * }</pre>
     */
    private int findCutoffIndex(List<Map<String, Object>> group, int maxIndex, int minIndex) {
        for (int col = minIndex; col >= maxIndex; col--) {
            int nonZeroCount = 0;
            for (Map<String, Object> row : group) {
                BigDecimal[] grades = (BigDecimal[]) row.get("GRADES");
                if (grades == null || grades.length <= col) {
                    continue;
                }
                BigDecimal val = grades[col];
                if (val != null && val.compareTo(BigDecimal.ZERO) > 0) {
                    nonZeroCount++;
                    if (nonZeroCount >= 2) {
                        return col;
                    }
                }
            }
        }
        return -1;
    }

    /**
     * 将低于 cutoffIndex 的所有档位列清零（截断操作）。
     * <p>
     * 截断后，只保留 maxIndex~cutoffIndex 的分配值，其余档位置为0。
     * </p>
     *
     * @param group       同一价位段内的卷烟列表，每个 Map 的 "GRADES" 字段会被原地修改
     * @param cutoffIndex 截断点索引（该索引对应的档位保留）
     * @param minIndex    最低档位索引（如 D1=29）
     *
     * @example
     * <pre>{@code
     * // cutoffIndex=20 表示保留 D30~D10，清零 D9~D1
     * truncateLowerGrades(group, 20, 29);
     * // 执行后，grades[21]~grades[29] 全部为 0
     * }</pre>
     */
    private void truncateLowerGrades(List<Map<String, Object>> group, int cutoffIndex, int minIndex) {
        for (Map<String, Object> row : group) {
            BigDecimal[] grades = (BigDecimal[]) row.get("GRADES");
            if (grades == null || grades.length == 0) {
                continue;
            }
            for (int col = cutoffIndex + 1; col <= minIndex && col < grades.length; col++) {
                grades[col] = BigDecimal.ZERO;
            }
        }
    }


    /**
     * 对某个价位段内的所有卷烟执行误差微调（支持两周一访上浮）。
     * <p>
     * 根据每支卷烟的备注（BZ字段）判断使用原始或上浮后的客户数数组。
     * 微调策略：从 maxIndex→cutoffIndex 逐档位+1，直到实际投放量逼近目标值。
     * </p>
     *
     * @param group              同一价位段内的卷烟列表
     * @param baseCustomerRow    原始客户数数组
     * @param boostedCustomerRow 上浮后客户数数组（可能为 null）
     * @param maxIndex           最高档位索引（如 D30=0）
     * @param cutoffIndex        截断点索引
     */
    private void adjustErrorsForBandWithBoost(List<Map<String, Object>> group,
                                              BigDecimal[] baseCustomerRow,
                                              BigDecimal[] boostedCustomerRow,
                                              int maxIndex,
                                              int cutoffIndex) {
        if (group == null || group.isEmpty() || baseCustomerRow == null) {
            return;
        }

        for (Map<String, Object> row : group) {
            BigDecimal[] grades = (BigDecimal[]) row.get("GRADES");
            if (grades == null || grades.length == 0) {
                continue;
            }

            BigDecimal target = WriteBackHelper.toBigDecimal(row.get("ADV"));
            if (target == null || target.compareTo(BigDecimal.ZERO) <= 0) {
                continue;
            }

            // 根据备注选择客户数数组
            String remark = WriteBackHelper.getString(row, "BZ");
            boolean needsBoost = remark != null && remark.contains("两周一访上浮100%");
            BigDecimal[] cityCustomerRow = (needsBoost && boostedCustomerRow != null) ? boostedCustomerRow : baseCustomerRow;

            BigDecimal currentAmount = ActualDeliveryCalculator.calculateFixed30(grades, cityCustomerRow);
            if (currentAmount.compareTo(target) >= 0) {
                continue;
            }

            BigDecimal maxGradeCustomerCount = cityCustomerRow[maxIndex] != null
                    ? cityCustomerRow[maxIndex]
                    : BigDecimal.ZERO;

            // 迭代微调
            int refineIterations = 0;
            while (true) {
                if (++refineIterations > MAX_ITERATIONS) {
                    break;
                }

                BigDecimal remainder = target.subtract(currentAmount);

                // 终止条件：余量 < maxIndex客户数
                if (remainder.compareTo(maxGradeCustomerCount) < 0) {
                    handleFinalAdjustment(grades, cityCustomerRow, target, currentAmount, maxIndex, maxGradeCustomerCount);
                    break;
                }

                // 执行一轮 maxIndex→cutoffIndex 填充
                FillResult result = runFillIteration(grades, cityCustomerRow, target, maxIndex, cutoffIndex, currentAmount);

                if (!result.progressMade) {
                    break;
                }

                if (result.hitExactTarget) {
                    break;
                }

                if (result.exceeded) {
                    grades[result.lastGrade] = grades[result.lastGrade].subtract(BigDecimal.ONE);
                    currentAmount = result.amount.subtract(result.lastIncrement);
                } else {
                    currentAmount = result.amount;
                }
            }
        }
    }


    /**
     * 处理最终调整：比较不对maxIndex+1和对maxIndex+1两种方案，选择误差较小者。
     * <p>
     * 当余量 < maxIndex客户数时调用此方法，决定是否对最高档位再+1。
     * </p>
     *
     * @param grades          分配方案数组（会被原地修改）
     * @param cityCustomerRow 全市客户数数组
     * @param target          目标投放量
     * @param currentAmount   当前实际投放量
     * @param maxIndex        最高档位索引（如 D30=0）
     * @param maxGradeCustomerCount 最高档位客户数
     *
     * @example
     * <pre>{@code
     * // 目标 3000，当前 2850，maxIndex客户数 149
     * // 方案1：不+1，误差 = |3000-2850| = 150
     * // 方案2：maxIndex+1，新投放量 = 2850+149 = 2999，误差 = |3000-2999| = 1
     * // 选择方案2
     * handleFinalAdjustment(grades, cityCustomerRow, 3000, 2850, 0, 149);
     * }</pre>
     */
    private void handleFinalAdjustment(BigDecimal[] grades, BigDecimal[] cityCustomerRow,
                                       BigDecimal target, BigDecimal currentAmount,
                                       int maxIndex, BigDecimal maxGradeCustomerCount) {
        BigDecimal error3 = target.subtract(currentAmount).abs();

        // 尝试对最高档位+1，比较误差
        BigDecimal maxGradeOriginalValue = grades[maxIndex];
        grades[maxIndex] = maxGradeOriginalValue.add(BigDecimal.ONE);
        BigDecimal amount4 = ActualDeliveryCalculator.calculateFixed30(grades, cityCustomerRow);
        BigDecimal error4 = target.subtract(amount4).abs();

        if (error3.compareTo(error4) <= 0) {
            // 不+1的误差更小，恢复原值
            grades[maxIndex] = maxGradeOriginalValue;
        }
    }

    /**
     * 执行一轮 maxIndex→cutoffIndex 填充迭代。
     * <p>
     * 从最高档位开始，逐档位尝试+1，直到超过目标值或完成一轮扫描。
     * </p>
     *
     * @param grades          分配方案数组（会被原地修改）
     * @param cityCustomerRow 全市客户数数组
     * @param target          目标投放量
     * @param maxIndex        最高档位索引（如 D30=0）
     * @param cutoffIndex     截断点索引，填充范围为 [maxIndex, cutoffIndex]
     * @param startAmount     本轮开始时的实际投放量
     * @return 填充结果，包含是否有进展、是否超过目标、最终投放量等信息
     *
     * @example
     * <pre>{@code
     * FillResult result = runFillIteration(grades, cityCustomerRow, 3000, 0, 20, 2500);
     * if (result.exceeded) {
     *     // 超过目标，需要回退最后一次+1
     *     grades[result.lastGrade] -= 1;
     * }
     * }</pre>
     */
    private FillResult runFillIteration(BigDecimal[] grades, BigDecimal[] cityCustomerRow,
                                        BigDecimal target, int maxIndex, int cutoffIndex,
                                        BigDecimal startAmount) {
        FillResult result = new FillResult();
        BigDecimal currentAmount = startAmount;

        for (int grade = maxIndex; grade <= cutoffIndex && grade < grades.length; grade++) {
            BigDecimal customerCount = cityCustomerRow[grade] != null ? cityCustomerRow[grade] : BigDecimal.ZERO;

            grades[grade] = grades[grade].add(BigDecimal.ONE);
            result.progressMade = true;

            BigDecimal newAmount = currentAmount.add(customerCount);

            if (newAmount.compareTo(target) > 0) {
                result.amount = newAmount;
                result.exceeded = true;
                result.lastGrade = grade;
                result.lastIncrement = customerCount;
                return result;
            }

            currentAmount = newAmount;

            if (newAmount.compareTo(target) == 0) {
                result.amount = currentAmount;
                result.hitExactTarget = true;
                return result;
            }
        }

        result.amount = currentAmount;
        return result;
    }

    /**
     * 检查某个价位段内是否有卷烟经过截断和微调后所有档位都是0。
     * <p>
     * 如果发现全0分配，恢复截断前的分配结果并收集异常信息。
     * </p>
     *
     * @param group                同一价位段内的卷烟列表
     * @param band                 价位段编号
     * @param preTruncationGrades  截断前的分配结果备份
     * @param year                 年份，用于异常信息
     * @param month                月份，用于异常信息
     * @param weekSeq              周序号，用于异常信息
     * @param errorCollector       异常信息收集器
     *
     * @example
     * <pre>{@code
     * List<String> errors = new ArrayList<>();
     * checkZeroAllocation(group, 100, backup, 2025, 9, 4, errors);
     * if (!errors.isEmpty()) {
     *     // 处理收集到的异常信息
     * }
     * }</pre>
     */
    private void checkZeroAllocation(List<Map<String, Object>> group, Integer band,
                                     List<BigDecimal[]> preTruncationGrades,
                                     Integer year, Integer month, Integer weekSeq,
                                     List<String> errorCollector) {
        if (group == null || group.isEmpty()) {
            return;
        }

        for (int i = 0; i < group.size(); i++) {
            Map<String, Object> row = group.get(i);
            BigDecimal[] grades = (BigDecimal[]) row.get("GRADES");
            if (grades == null || grades.length == 0) {
                continue;
            }

            boolean allZero = true;
            for (BigDecimal grade : grades) {
                if (grade != null && grade.compareTo(BigDecimal.ZERO) > 0) {
                    allZero = false;
                    break;
                }
            }

            if (allZero) {
                // 恢复截断前的分配结果
                String cigName = row.get("CIG_NAME") != null ? row.get("CIG_NAME").toString() : "未知";
                String cigCode = row.get("CIG_CODE") != null ? row.get("CIG_CODE").toString() : "未知";
                
                // 恢复截断前的分配
                if (i < preTruncationGrades.size()) {
                    BigDecimal[] restored = preTruncationGrades.get(i);
                    row.put("GRADES", restored);
                }
                
                // 收集异常信息（不立即抛出，允许其他价位段继续处理）
                String errorMsg = String.format(
                    "卷烟【%s(%s)】在价位段%d经过截断和微调后分配结果全0，已恢复初分配。时间：%d-%d-%d",
                    cigName, cigCode, band, year, month, weekSeq
                );
                errorCollector.add(errorMsg);
            }
        }
    }

    /**
     * 填充迭代结果内部类。
     * <p>
     * 用于记录一轮填充迭代的状态，包括：
     * <ul>
     *   <li>amount: 迭代结束时的实际投放量</li>
     *   <li>exceeded: 是否超过目标值</li>
     *   <li>hitExactTarget: 是否精确命中目标值</li>
     *   <li>progressMade: 本轮是否有任何档位+1</li>
     *   <li>lastGrade: 最后一次+1的档位索引（用于回退）</li>
     *   <li>lastIncrement: 最后一次+1带来的投放量增量（用于回退计算）</li>
     * </ul>
     * </p>
     */
    private static final class FillResult {
        private BigDecimal amount = BigDecimal.ZERO;
        private boolean exceeded = false;
        private boolean hitExactTarget = false;
        private boolean progressMade = false;
        private int lastGrade = -1;
        private BigDecimal lastIncrement = BigDecimal.ZERO;
    }
}
