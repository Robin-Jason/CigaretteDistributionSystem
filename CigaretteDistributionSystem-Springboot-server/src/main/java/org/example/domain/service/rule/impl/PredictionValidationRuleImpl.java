package org.example.domain.service.rule.impl;

import org.example.domain.service.rule.PredictionValidationRule;
import org.example.shared.util.ParamValidators;

import java.math.BigDecimal;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 预测分配数据业务校验规则领域服务实现。
 * <p>
 * 纯领域逻辑，不依赖 Spring 或持久化层。
 * </p>
 *
 * @author Robin
 * @since 2025-12-22
 */
public class PredictionValidationRuleImpl implements PredictionValidationRule {

    @Override
    public ValidationResult validateCigaretteExists(Map<String, Object> cigInfo,
                                                    String cigCode, String cigName,
                                                    Integer year, Integer month, Integer weekSeq) {
        if (cigInfo == null) {
            String msg = String.format(
                    "卷烟不存在：代码[%s]、名称[%s]在批次[%d年%d月第%d周]的投放信息表中未找到。" +
                    "请确认卷烟代码和名称是否正确，或该卷烟是否已导入该批次。",
                    cigCode, cigName, year, month, weekSeq);
            return ValidationResult.fail(ERR_CIGARETTE_NOT_FOUND, msg);
        }
        return ValidationResult.success();
    }

    @Override
    public ValidationResult validateBatchRegionStatsExists(List<Map<String, Object>> allStats,
                                                           Integer year, Integer month, Integer weekSeq) {
        if (allStats == null || allStats.isEmpty()) {
            String msg = String.format(
                    "批次数据不存在：[%d年%d月第%d周]的区域客户统计数据为空。" +
                    "请先确保该批次的客户数据已导入。",
                    year, month, weekSeq);
            return ValidationResult.fail(ERR_BATCH_NOT_FOUND, msg);
        }
        return ValidationResult.success();
    }

    @Override
    public ValidationResult validateSingleExtensionRegion(String primaryRegion,
                                                          Set<String> validRegions,
                                                          Set<String> primarySet) {
        if (!validRegions.contains(primaryRegion) && !primarySet.contains(primaryRegion)) {
            String msg = String.format(
                    "投放区域无效：[%s]不在该批次的合法区域集合内。" +
                    "合法的区域包括：%s",
                    primaryRegion, ParamValidators.formatRegionSet(validRegions, 10));
            return ValidationResult.fail(ERR_REGION_INVALID, msg);
        }
        return ValidationResult.success();
    }

    @Override
    public ValidationResult validateDualExtensionRegion(String primaryRegion,
                                                        String secondaryRegion,
                                                        String deliveryArea,
                                                        Set<String> validRegions,
                                                        Set<String> primarySet,
                                                        Set<String> secondarySet) {
        // 校验主区域
        if (!primarySet.contains(primaryRegion)) {
            String msg = String.format(
                    "主投放区域无效：[%s]不在该批次的合法主区域集合内。" +
                    "合法的主区域包括：%s",
                    primaryRegion, ParamValidators.formatRegionSet(primarySet, 10));
            return ValidationResult.fail(ERR_REGION_INVALID, msg);
        }

        // 校验子区域
        if (!secondarySet.contains(secondaryRegion)) {
            String msg = String.format(
                    "子投放区域无效：[%s]不在该批次的合法子区域集合内。" +
                    "合法的子区域包括：%s",
                    secondaryRegion, ParamValidators.formatRegionSet(secondarySet, 10));
            return ValidationResult.fail(ERR_REGION_INVALID, msg);
        }

        // 校验拼接后的区域是否存在
        if (!validRegions.contains(deliveryArea)) {
            String msg = String.format(
                    "投放区域组合无效：[%s]不在该批次的合法区域笛卡尔积内。" +
                    "主区域[%s]和子区域[%s]的组合在该批次中不存在。",
                    deliveryArea, primaryRegion, secondaryRegion);
            return ValidationResult.fail(ERR_REGION_INVALID, msg);
        }

        return ValidationResult.success();
    }

    @Override
    public ValidationResult validateRegionStatsExists(Map<String, Object> stat, String regionName) {
        if (stat == null) {
            String msg = String.format(
                    "区域客户数据不存在：区域[%s]的客户统计数据未找到，无法计算实际投放量。",
                    regionName);
            return ValidationResult.fail(ERR_REGION_STATS_NOT_FOUND, msg);
        }
        return ValidationResult.success();
    }

    @Override
    public ValidationResult validateGradesMonotonicity(List<BigDecimal> grades) {
        if (grades == null || grades.size() != 30) {
            return ValidationResult.fail(ERR_GRADES_MONOTONICITY,
                    "档位数据无效：必须提供30个档位值（D30-D1）。");
        }

        for (int i = 1; i < grades.size(); i++) {
            BigDecimal prev = grades.get(i - 1) != null ? grades.get(i - 1) : BigDecimal.ZERO;
            BigDecimal curr = grades.get(i) != null ? grades.get(i) : BigDecimal.ZERO;
            if (curr.compareTo(prev) > 0) {
                int prevGrade = 31 - i;  // D30, D29, ...
                int currGrade = 30 - i;  // D29, D28, ...
                String msg = String.format(
                        "档位值不满足单调递减约束：D%d的值[%s]大于D%d的值[%s]。" +
                        "档位值必须满足 D30 >= D29 >= ... >= D1。",
                        currGrade, curr, prevGrade, prev);
                return ValidationResult.fail(ERR_GRADES_MONOTONICITY, msg);
            }
        }
        return ValidationResult.success();
    }

    @Override
    public ValidationResult validateGradesMonotonicityWithRange(List<BigDecimal> grades, String hg, String lg) {
        if (grades == null || grades.size() != 30) {
            return ValidationResult.fail(ERR_GRADES_MONOTONICITY,
                    "档位数据无效：必须提供30个档位值（D30-D1）。");
        }

        if (hg == null || lg == null) {
            return ValidationResult.fail(ERR_GRADES_MONOTONICITY,
                    "档位范围无效：HG 和 LG 不能为空。");
        }

        // 解析 HG/LG 为索引
        int maxIndex = parseGradeToIndex(hg);
        int minIndex = parseGradeToIndex(lg);

        if (maxIndex < 0 || minIndex < 0 || maxIndex > minIndex) {
            return ValidationResult.fail(ERR_GRADES_MONOTONICITY,
                    String.format("档位范围无效：HG[%s]必须高于或等于LG[%s]。", hg, lg));
        }

        // 1. 验证 HG 之前的档位（更高档位）必须全为 0
        for (int i = 0; i < maxIndex; i++) {
            BigDecimal value = grades.get(i) != null ? grades.get(i) : BigDecimal.ZERO;
            if (value.compareTo(BigDecimal.ZERO) != 0) {
                int gradeNum = 30 - i;
                return ValidationResult.fail(ERR_GRADES_MONOTONICITY,
                        String.format("档位范围外有非零值：D%d的值为[%s]，但有效范围是[%s-%s]，范围外档位必须为0。",
                                gradeNum, value, hg, lg));
            }
        }

        // 2. 验证 HG~LG 范围内的档位满足单调递减
        for (int i = maxIndex + 1; i <= minIndex && i < grades.size(); i++) {
            BigDecimal prev = grades.get(i - 1) != null ? grades.get(i - 1) : BigDecimal.ZERO;
            BigDecimal curr = grades.get(i) != null ? grades.get(i) : BigDecimal.ZERO;
            if (curr.compareTo(prev) > 0) {
                int prevGrade = 30 - (i - 1);
                int currGrade = 30 - i;
                return ValidationResult.fail(ERR_GRADES_MONOTONICITY,
                        String.format("档位值不满足单调递减约束：D%d的值[%s]大于D%d的值[%s]。" +
                                "在有效范围[%s-%s]内，档位值必须满足单调递减。",
                                currGrade, curr, prevGrade, prev, hg, lg));
            }
        }

        // 3. 验证 LG 之后的档位（更低档位）必须全为 0
        for (int i = minIndex + 1; i < grades.size(); i++) {
            BigDecimal value = grades.get(i) != null ? grades.get(i) : BigDecimal.ZERO;
            if (value.compareTo(BigDecimal.ZERO) != 0) {
                int gradeNum = 30 - i;
                return ValidationResult.fail(ERR_GRADES_MONOTONICITY,
                        String.format("档位范围外有非零值：D%d的值为[%s]，但有效范围是[%s-%s]，范围外档位必须为0。",
                                gradeNum, value, hg, lg));
            }
        }

        return ValidationResult.success();
    }

    /**
     * 将档位字符串（如 "D25"）解析为索引（0-29）。
     *
     * @param grade 档位字符串，如 "D30", "D25", "D1"
     * @return 索引值，D30=0, D29=1, ..., D1=29；解析失败返回 -1
     */
    private int parseGradeToIndex(String grade) {
        if (grade == null || !grade.startsWith("D")) {
            return -1;
        }
        try {
            int gradeNum = Integer.parseInt(grade.substring(1));
            if (gradeNum < 1 || gradeNum > 30) {
                return -1;
            }
            return 30 - gradeNum;  // D30=0, D29=1, ..., D1=29
        } catch (NumberFormatException e) {
            return -1;
        }
    }

    @Override
    public ValidationResult validateRecordNotExists(boolean exists,
                                                    String cigCode, String cigName,
                                                    String deliveryArea) {
        if (exists) {
            String msg = String.format(
                    "记录已存在：卷烟[%s-%s]在区域[%s]的分配记录已存在，不能重复添加。" +
                    "如需修改，请使用编辑功能。",
                    cigCode, cigName, deliveryArea);
            return ValidationResult.fail(ERR_RECORD_EXISTS, msg);
        }
        return ValidationResult.success();
    }

    @Override
    public ValidationResult validateRecordExists(boolean exists,
                                                 String cigCode, String cigName,
                                                 String deliveryArea) {
        if (!exists) {
            String msg = String.format(
                    "记录不存在：卷烟[%s-%s]在区域[%s]的分配记录不存在，无法进行操作。",
                    cigCode, cigName, deliveryArea);
            return ValidationResult.fail(ERR_RECORD_NOT_FOUND, msg);
        }
        return ValidationResult.success();
    }

    @Override
    public RegionSets parseRegionSets(List<Map<String, Object>> allStats) {
        Set<String> validRegions = new HashSet<>();
        Set<String> primaryRegions = new HashSet<>();
        Set<String> secondaryRegions = new HashSet<>();

        for (Map<String, Object> stat : allStats) {
            // 兼容不同的列名：region（小写，来自 resultMap）或 REGION（大写）
            Object regionObj = stat.get("region");
            if (regionObj == null) {
                regionObj = stat.get("REGION");
            }
            if (regionObj == null) continue;

            String regionName = regionObj.toString();
            validRegions.add(regionName);

            // 解析主区域和子区域
            int leftIdx = regionName.indexOf('（');
            int rightIdx = regionName.indexOf('）');
            if (leftIdx > 0 && rightIdx > leftIdx) {
                primaryRegions.add(regionName.substring(0, leftIdx));
                secondaryRegions.add(regionName.substring(leftIdx + 1, rightIdx));
            } else {
                primaryRegions.add(regionName);
            }
        }

        return new RegionSets(validRegions, primaryRegions, secondaryRegions);
    }
}
