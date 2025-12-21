package org.example.application.service.calculate.impl;

import lombok.extern.slf4j.Slf4j;
import org.example.application.service.calculate.PriceBandAllocationService;
import org.example.application.service.coordinator.PriceBandCandidateQueryService;
import org.example.application.orchestrator.allocation.RegionCustomerMatrix;
import org.example.application.service.writeback.DistributionWriteBackService;
import org.example.shared.util.ActualDeliveryCalculator;
import org.example.shared.util.WriteBackHelper;
import org.example.domain.repository.RegionCustomerStatisticsRepository;
import org.example.domain.service.algorithm.SingleLevelDistributionService;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

/**
 * 按价位段自选投放（全市默认策略）分配服务默认实现。
 *
 * <p>通过统一的分配编排引擎，基于价位段候选卷烟列表，完成按价位段自选投放场景下的
 * 单支初分配、价位段矩阵截断与微调，并将结果写回 {@code cigarette_distribution_prediction_price} 分区表。</p>
 *
 * <p>本类作为独立的应用服务，后续将由统一分配编排链路在识别到“按价位段自选投放（全市）”场景时进行委托调用。</p>
 *
 * @author Robin
 * @since 2025-12-18
 */
@Slf4j
@Service
public class PriceBandAllocationServiceImpl implements PriceBandAllocationService {

    private final PriceBandCandidateQueryService priceBandCandidateQueryService;
    private final SingleLevelDistributionService singleLevelDistributionService;
    private final RegionCustomerStatisticsRepository regionCustomerStatisticsRepository;
    private final DistributionWriteBackService writeBackService;

    public PriceBandAllocationServiceImpl(
            PriceBandCandidateQueryService priceBandCandidateQueryService,
            SingleLevelDistributionService singleLevelDistributionService,
            RegionCustomerStatisticsRepository regionCustomerStatisticsRepository,
            @Qualifier("priceBandDistributionWriteBackServiceImpl") DistributionWriteBackService writeBackService) {
        this.priceBandCandidateQueryService = priceBandCandidateQueryService;
        this.singleLevelDistributionService = singleLevelDistributionService;
        this.regionCustomerStatisticsRepository = regionCustomerStatisticsRepository;
        this.writeBackService = writeBackService;
    }

    @Override
    public void allocateForPriceBand(Integer year, Integer month, Integer weekSeq) {
        log.info("Start price-band allocation for year={}, month={}, weekSeq={}", year, month, weekSeq);

        // 1) 获取候选卷烟列表（已按价位段升序、组内批发价降序排序）
        List<Map<String, Object>> candidates =
                priceBandCandidateQueryService.listOrderedPriceBandCandidates(year, month, weekSeq);
        if (candidates == null || candidates.isEmpty()) {
            log.info("价位段自选投放在 {}-{}-{} 分区无候选卷烟，跳过分配", year, month, weekSeq);
            return;
        }

        // 2) 构建“全市”区域客户矩阵（单行矩阵）
        RegionCustomerMatrix cityMatrix = buildCityWideCustomerMatrix(year, month, weekSeq);
        if (cityMatrix.isEmpty()) {
            log.warn("region_customer_statistics 在 {}-{}-{} 分区无数据，无法执行价位段自选投放分配", year, month, weekSeq);
            return;
        }
        List<String> targetRegions = Collections.singletonList("全市");
        BigDecimal[][] regionCustomerMatrix = toMatrix(cityMatrix);

        // 3) 为每支候选卷烟执行 SingleLevel 初分配，结果挂载回候选记录
        for (Map<String, Object> row : candidates) {
            BigDecimal adv = WriteBackHelper.toBigDecimal(row.get("ADV"));
            if (adv == null || adv.compareTo(BigDecimal.ZERO) <= 0) {
                // 无有效目标量时，视为全 0 分配
                row.put("GRADES", new BigDecimal[30]);
                continue;
            }
            BigDecimal[][] allocation = singleLevelDistributionService.distribute(
                    targetRegions, regionCustomerMatrix, adv);
            if (allocation == null || allocation.length == 0) {
                row.put("GRADES", new BigDecimal[30]);
            } else {
                // 单区域矩阵，仅取第一行作为该卷烟的分配结果
                BigDecimal[] grades = allocation[0];
                if (grades == null || grades.length == 0) {
                    grades = new BigDecimal[30];
                }
                row.put("GRADES", grades);
            }
        }

        // 4) 按价位段分组，为后续矩阵截断与微调做准备
        //    显式优化：仅当某价位段内有至少两支卷烟时，才需要做“列上至少两个非0元素”的扫描与截断。
        Map<Integer, List<Map<String, Object>>> bands = new java.util.TreeMap<>();
        for (Map<String, Object> row : candidates) {
            Object bandObj = row.get("PRICE_BAND");
            if (bandObj == null) {
                continue;
            }
            Integer band;
            try {
                band = Integer.valueOf(bandObj.toString());
            } catch (NumberFormatException e) {
                continue;
            }
            bands.computeIfAbsent(band, k -> new ArrayList<>()).add(row);
        }

        Map<Integer, List<Map<String, Object>>> bandsNeedingTruncation = new java.util.TreeMap<>();
        for (Entry<Integer, List<Map<String, Object>>> entry : bands.entrySet()) {
            Integer band = entry.getKey();
            List<Map<String, Object>> group = entry.getValue();
            if (group.size() <= 1) {
                // 仅一支卷烟时，不需要做后续截断与微调，直接保留 SingleLevel 结果
                log.debug("价位段 {} 仅包含 {} 支卷烟，跳过截断与微调", band, group.size());
            } else {
                bandsNeedingTruncation.put(band, group);
            }
        }

        // 5) 对需要截断的价位段执行"LG→HG 扫描找到首个列上≥2非0元素并清零更低档位"的操作
        //    然后进行误差微调和全0检测
        BigDecimal[] cityCustomerRow = regionCustomerMatrix[0]; // 全市客户数数组（30个档位）
        truncateAndAdjustBands(bandsNeedingTruncation, cityCustomerRow, year, month, weekSeq);

        // 6) 将分配结果写回 cigarette_distribution_prediction_price 分区表
        writeBackService.writeBackPriceBandAllocations(candidates, year, month, weekSeq, cityCustomerRow);

        log.info("Price-band allocation finished for year={}, month={}, weekSeq={}", year, month, weekSeq);
    }

    /**
     * 对需要截断的价位段执行"LG→HG 扫描找到首个列上≥2非0元素并清零更低档位"的截断操作，
     * 然后进行误差微调和全0检测。
     *
     * <p>说明：目前假设 HG=D30、LG=D1，与现有单层算法中的数组约定一致（D30 对应索引0，D1对应索引29）。</p>
     *
     * @param bandsNeedingTruncation 需要截断的价位段分组
     * @param cityCustomerRow 全市客户数数组（30个档位），用于计算实际投放量
     * @param year 年份
     * @param month 月份
     * @param weekSeq 周序号
     */
    private void truncateAndAdjustBands(Map<Integer, List<Map<String, Object>>> bandsNeedingTruncation,
                                         BigDecimal[] cityCustomerRow,
                                         Integer year, Integer month, Integer weekSeq) {
        if (bandsNeedingTruncation == null || bandsNeedingTruncation.isEmpty()) {
            return;
        }
        final int highestIndex = org.example.shared.constants.GradeConstants.HIGHEST_GRADE_INDEX;
        final int lowestIndex = org.example.shared.constants.GradeConstants.LOWEST_GRADE_INDEX;

        for (Entry<Integer, List<Map<String, Object>>> entry : bandsNeedingTruncation.entrySet()) {
            Integer band = entry.getKey();
            List<Map<String, Object>> group = entry.getValue();
            if (group == null || group.size() <= 1) {
                continue;
            }

            // 保存截断前的分配结果（用于异常时返回）
            List<BigDecimal[]> preTruncationGrades = new ArrayList<>();
            for (Map<String, Object> row : group) {
                BigDecimal[] grades = (BigDecimal[]) row.get("GRADES");
                if (grades != null) {
                    preTruncationGrades.add(grades.clone());
                } else {
                    preTruncationGrades.add(new BigDecimal[30]);
                }
            }

            // 从 LG 向 HG 扫描，找到首个"该列至少有两支卷烟分配量 > 0"的索引
            int cutoffIndex = -1;
            for (int col = lowestIndex; col >= highestIndex; col--) {
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
                            cutoffIndex = col;
                            break;
                        }
                    }
                }
                if (cutoffIndex >= 0) {
                    break;
                }
            }

            if (cutoffIndex < 0) {
                log.debug("价位段 {} 在 LG→HG 扫描中未找到列上≥2非0元素的档位，暂不做截断", band);
                continue;
            }

            // 将低于 cutoffIndex 的所有档位列（更低档位）统一清零
            for (Map<String, Object> row : group) {
                BigDecimal[] grades = (BigDecimal[]) row.get("GRADES");
                if (grades == null || grades.length == 0) {
                    continue;
                }
                for (int col = cutoffIndex + 1; col <= lowestIndex && col < grades.length; col++) {
                    grades[col] = BigDecimal.ZERO;
                }
            }

            log.debug("价位段 {} 截断完成，cutoffIndex={}, 保留高档位列，清零低档位列", band, cutoffIndex);

            // 误差微调：每支卷烟独立执行逐档位+1，直到误差最小化
            adjustErrorsForBand(group, cityCustomerRow, highestIndex, cutoffIndex);

            // 全0检测：如果某支卷烟经过截断和微调后所有档位都是0，抛出异常
            checkZeroAllocation(group, band, preTruncationGrades, year, month, weekSeq);
        }
    }

    /**
     * 对某个价位段内的所有卷烟执行误差微调。
     * <p>参考SingleLevel的微调逻辑，每支卷烟独立执行以下流程：
     * 1. 从截断后的状态开始（相当于candidate2）
     * 2. 多轮迭代：每轮从HG逐档位+1至cutoffIndex，直到超出预投放量
     * 3. 撤销导致超出的最后一次+1操作
     * 4. 终止条件：某一轮从HG开始+1时，HG档位+1就导致超出预投放量（余量 < HG客户数）
     * 5. 比较candidate3（不对HG+1）和candidate4（对HG+1），选误差较小者
     * </p>
     *
     * @param group 价位段内的卷烟列表
     * @param cityCustomerRow 全市客户数数组（30个档位）
     * @param highestIndex 最高档位索引（HG，通常为0）
     * @param cutoffIndex 截断档位索引（新的最低档位）
     */
    private void adjustErrorsForBand(List<Map<String, Object>> group,
                                     BigDecimal[] cityCustomerRow,
                                     int highestIndex,
                                     int cutoffIndex) {
        if (group == null || group.isEmpty() || cityCustomerRow == null) {
            return;
        }

        // 为每支卷烟独立执行微调
        for (Map<String, Object> row : group) {
            BigDecimal[] grades = (BigDecimal[]) row.get("GRADES");
            if (grades == null || grades.length == 0) {
                continue;
            }

            BigDecimal target = WriteBackHelper.toBigDecimal(row.get("ADV"));
            if (target == null || target.compareTo(BigDecimal.ZERO) <= 0) {
                continue;
            }

            // 从截断后的状态开始微调
            BigDecimal currentAmount = ActualDeliveryCalculator.calculateFixed30(grades, cityCustomerRow);
            
            // 如果已经达到或超过目标，跳过微调
            if (currentAmount.compareTo(target) >= 0) {
                continue;
            }

            // HG档位的客户数（用于终止条件判断）
            BigDecimal hgCustomerCount = cityCustomerRow[highestIndex] == null 
                ? BigDecimal.ZERO 
                : cityCustomerRow[highestIndex];

            // 迭代微调：每轮从HG→LG填充，直到余量 < HG客户数
            int refineIterations = 0;
            final int MAX_ITERATIONS = 10_000_000;

            while (true) {
                if (++refineIterations > MAX_ITERATIONS) {
                    log.warn("价位段微调达到最大迭代次数限制 {}", MAX_ITERATIONS);
                    break;
                }

                BigDecimal remainder = target.subtract(currentAmount);

                // 终止条件：余量 < HG客户数
                if (remainder.compareTo(hgCustomerCount) < 0) {
                    // candidate3: 当前状态（不对HG+1）
                    BigDecimal error3 = remainder.abs();

                    // candidate4: HG+1后的状态
                    BigDecimal error4 = null;
                    if (isValidIncrementPriceBand(grades, highestIndex)) {
                        BigDecimal hgOriginalValue = grades[highestIndex];
                        grades[highestIndex] = hgOriginalValue.add(BigDecimal.ONE);
                        BigDecimal amount4 = ActualDeliveryCalculator.calculateFixed30(grades, cityCustomerRow);
                        error4 = target.subtract(amount4).abs();

                        // 比较误差，选择较小者
                        if (error3.compareTo(error4) <= 0) {
                            // candidate3误差更小或相等，回退HG的+1操作
                            grades[highestIndex] = hgOriginalValue;
                        }
                        // 否则保持HG+1后的状态
                    }
                    // 如果HG不能+1，则直接使用candidate3
                    break;
                }

                // 执行一轮HG→LG填充
                PriceBandFillResult iterationResult = runPriceBandFillIteration(
                    grades, cityCustomerRow, target, highestIndex, cutoffIndex, currentAmount);

                if (!iterationResult.progressMade) {
                    // 无法继续填充，终止
                    break;
                }

                if (iterationResult.hitExactTarget) {
                    // 恰好达到目标，无需继续
                    currentAmount = iterationResult.amount;
                    break;
                }

                if (iterationResult.exceeded) {
                    // 撤销导致超出的最后一次+1
                    grades[iterationResult.lastGrade] = 
                        grades[iterationResult.lastGrade].subtract(BigDecimal.ONE);
                    currentAmount = iterationResult.amount.subtract(iterationResult.lastIncrement);
                } else {
                    currentAmount = iterationResult.amount;
                }
            }
        }
    }

    /**
     * 执行一轮HG→LG填充迭代（价位段版本）
     * 从HG开始逐档位+1，直到超出目标或达到cutoffIndex
     *
     * @param grades 分配方案数组
     * @param cityCustomerRow 全市客户数数组
     * @param target 目标预投放量
     * @param highestIndex HG索引
     * @param cutoffIndex 截断档位索引
     * @param startAmount 起始实际投放量
     * @return 填充结果
     */
    private PriceBandFillResult runPriceBandFillIteration(BigDecimal[] grades,
                                                          BigDecimal[] cityCustomerRow,
                                                          BigDecimal target,
                                                          int highestIndex,
                                                          int cutoffIndex,
                                                          BigDecimal startAmount) {
        PriceBandFillResult result = new PriceBandFillResult();
        BigDecimal currentAmount = startAmount;
        boolean hasProgress = false;

        // 从HG到cutoffIndex逐档位+1（不限制单调性，只要档位在范围内就+1）
        for (int grade = highestIndex; grade <= cutoffIndex && grade < grades.length; grade++) {
            // 检查是否满足单调性约束
            if (!isValidIncrementPriceBand(grades, grade)) {
                continue;
            }

            BigDecimal customerCount = cityCustomerRow[grade];
            if (customerCount == null) {
                customerCount = BigDecimal.ZERO;
            }

            // +1操作
            grades[grade] = grades[grade].add(BigDecimal.ONE);
            hasProgress = true;

            BigDecimal newAmount = currentAmount.add(customerCount);

            // 检查是否超出或达到目标
            if (newAmount.compareTo(target) > 0) {
                result.amount = newAmount;
                result.exceeded = true;
                result.progressMade = true;
                result.lastGrade = grade;
                result.lastIncrement = customerCount;
                return result;
            }

            currentAmount = newAmount;

            if (newAmount.compareTo(target) == 0) {
                result.amount = currentAmount;
                result.hitExactTarget = true;
                result.progressMade = true;
                return result;
            }
        }

        result.amount = currentAmount;
        result.progressMade = hasProgress;
        return result;
    }

    /**
     * 检查对指定档位+1是否违反单调性约束（非递增）
     * D30 >= D29 >= ... >= D1
     *
     * @param grades 分配方案数组
     * @param gradeIndex 档位索引
     * @return 如果+1后不违反约束，返回true
     */
    private boolean isValidIncrementPriceBand(BigDecimal[] grades, int gradeIndex) {
        if (grades == null || gradeIndex < 0 || gradeIndex >= grades.length) {
            return false;
        }

        // 临时+1
        BigDecimal originalValue = grades[gradeIndex];
        if (originalValue == null) {
            originalValue = BigDecimal.ZERO;
            grades[gradeIndex] = originalValue;
        }
        grades[gradeIndex] = grades[gradeIndex].add(BigDecimal.ONE);

        boolean valid = true;

        // 检查是否违反单调性约束：低档位不能大于高档位
        if (gradeIndex > 0 && grades[gradeIndex].compareTo(grades[gradeIndex - 1]) > 0) {
            valid = false;
        }

        // 恢复原值
        grades[gradeIndex] = originalValue;
        return valid;
    }

    /**
     * 价位段填充结果内部类
     */
    private static final class PriceBandFillResult {
        private BigDecimal amount = BigDecimal.ZERO;
        private boolean exceeded = false;
        private boolean hitExactTarget = false;
        private boolean progressMade = false;
        private int lastGrade = -1;
        private BigDecimal lastIncrement = BigDecimal.ZERO;
    }

    /**
     * 计算实际投放量：Σ(grades[i] * cityCustomerRow[i])
     *
     * @param grades 分配方案数组（30个档位）
     * @param cityCustomerRow 全市客户数数组（30个档位）
     * @return 实际投放量
     */

    /**
     * 检查某个价位段内是否有卷烟经过截断和微调后所有档位都是0。
     * 如果有，停止该价位段的分配，恢复截断前的结果，并抛出异常。
     *
     * @param group 价位段内的卷烟列表
     * @param band 价位段编号
     * @param preTruncationGrades 截断前的分配结果（用于异常时恢复）
     * @param year 年份
     * @param month 月份
     * @param weekSeq 周序号
     */
    private void checkZeroAllocation(List<Map<String, Object>> group,
                                    Integer band,
                                    List<BigDecimal[]> preTruncationGrades,
                                    Integer year, Integer month, Integer weekSeq) {
        if (group == null || group.isEmpty()) {
            return;
        }

        for (int i = 0; i < group.size(); i++) {
            Map<String, Object> row = group.get(i);
            BigDecimal[] grades = (BigDecimal[]) row.get("GRADES");
            if (grades == null || grades.length == 0) {
                continue;
            }

            // 检查是否所有档位都是0
            boolean allZero = true;
            for (BigDecimal grade : grades) {
                if (grade != null && grade.compareTo(BigDecimal.ZERO) > 0) {
                    allZero = false;
                    break;
                }
            }

            if (allZero) {
                // 恢复截断前的结果
                if (i < preTruncationGrades.size()) {
                    row.put("GRADES", preTruncationGrades.get(i));
                }

                // 构造错误信息
                String cigCode = row.get("CIG_CODE") != null ? row.get("CIG_CODE").toString() : "未知";
                String cigName = row.get("CIG_NAME") != null ? row.get("CIG_NAME").toString() : "未知";
                String errorMsg = String.format(
                        "价位段自选投放分配失败：批次 %d-%d-%d，价位段 %d，卷烟代码 %s（%s）经过截断和微调后所有档位分配量均为0",
                        year, month, weekSeq, band, cigCode, cigName);

                log.error(errorMsg);
                throw new IllegalStateException(errorMsg);
            }
        }
    }

    /**
     * 构建“全市”单行区域客户矩阵。
     * <p>实现方式：从 region_customer_statistics 分区表读取所有区域客户记录，
     * 将 30 档位客户数逐列求和，得到一个代表“全市”的矩阵行。</p>
     */
    private RegionCustomerMatrix buildCityWideCustomerMatrix(Integer year, Integer month, Integer weekSeq) {
        List<Map<String, Object>> allStats = regionCustomerStatisticsRepository.findAll(year, month, weekSeq);
        if (allStats == null || allStats.isEmpty()) {
            return new RegionCustomerMatrix(new ArrayList<>());
        }
        BigDecimal[] sumGrades = new BigDecimal[30];
        for (int i = 0; i < 30; i++) {
            sumGrades[i] = BigDecimal.ZERO;
        }
        for (Map<String, Object> stat : allStats) {
            BigDecimal[] grades = extractGrades(stat);
            for (int i = 0; i < 30; i++) {
                BigDecimal val = grades[i] == null ? BigDecimal.ZERO : grades[i];
                sumGrades[i] = sumGrades[i].add(val);
            }
        }
        List<RegionCustomerMatrix.Row> rows = new ArrayList<>();
        rows.add(new RegionCustomerMatrix.Row("全市", sumGrades));
        return new RegionCustomerMatrix(rows);
    }

    private BigDecimal[] extractGrades(Map<String, Object> stat) {
        return org.example.shared.util.GradeExtractor.extractFromMap(stat);
    }

    private BigDecimal[][] toMatrix(RegionCustomerMatrix matrix) {
        List<RegionCustomerMatrix.Row> rows = matrix.getRows();
        BigDecimal[][] result = new BigDecimal[rows.size()][30];
        for (int i = 0; i < rows.size(); i++) {
            BigDecimal[] src = rows.get(i).getGrades();
            if (src == null || src.length == 0) {
                result[i] = new BigDecimal[30];
            } else {
                result[i] = src;
            }
        }
        return result;
    }

}


