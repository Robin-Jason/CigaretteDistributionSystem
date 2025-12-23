package org.example.shared.util;

import java.math.BigDecimal;
import java.util.List;
import java.util.Set;

/**
 * 通用参数校验工具类。
 * <p>
 * 提供无状态的参数格式校验方法，不涉及业务规则判断。
 * 适用于 Controller 层或 Service 层入口处的基础参数校验。
 * </p>
 *
 * @author Robin
 * @since 2025-12-22
 */
public final class ParamValidators {

    private ParamValidators() {
    }

    // ==================== 时间参数校验 ====================

    /**
     * 校验时间参数（年、月、周序号）。
     *
     * @param year    年份，应为 2020-2100 之间的整数
     * @param month   月份，应为 1-12 之间的整数
     * @param weekSeq 周序号，应为 1-5 之间的整数
     * @throws IllegalArgumentException 参数无效时抛出
     * @example validateTimeParams(2025, 9, 3) -> 校验通过
     * @example validateTimeParams(null, 9, 3) -> 抛出异常
     */
    public static void validateTimeParams(Integer year, Integer month, Integer weekSeq) {
        if (year == null || year < 2020 || year > 2100) {
            throw new IllegalArgumentException("[参数错误] 年份(year)无效，应为2020-2100之间的整数");
        }
        if (month == null || month < 1 || month > 12) {
            throw new IllegalArgumentException("[参数错误] 月份(month)无效，应为1-12之间的整数");
        }
        if (weekSeq == null || weekSeq < 1 || weekSeq > 5) {
            throw new IllegalArgumentException("[参数错误] 周序号(weekSeq)无效，应为1-5之间的整数");
        }
    }

    // ==================== 卷烟参数校验 ====================

    /**
     * 校验卷烟参数（代码、名称）。
     *
     * @param cigCode 卷烟代码，不能为空
     * @param cigName 卷烟名称，不能为空
     * @throws IllegalArgumentException 参数无效时抛出
     * @example validateCigaretteParams("001", "中华") -> 校验通过
     * @example validateCigaretteParams("", "中华") -> 抛出异常
     */
    public static void validateCigaretteParams(String cigCode, String cigName) {
        if (cigCode == null || cigCode.trim().isEmpty()) {
            throw new IllegalArgumentException("[参数错误] 卷烟代码(cigCode)不能为空");
        }
        if (cigName == null || cigName.trim().isEmpty()) {
            throw new IllegalArgumentException("[参数错误] 卷烟名称(cigName)不能为空");
        }
    }

    // ==================== 区域参数校验 ====================

    /**
     * 校验区域参数（主区域必填）。
     *
     * @param primaryRegion 主区域，不能为空
     * @throws IllegalArgumentException 参数无效时抛出
     * @example validateRegionParams("城区") -> 校验通过
     * @example validateRegionParams(null) -> 抛出异常
     */
    public static void validateRegionParams(String primaryRegion) {
        if (primaryRegion == null || primaryRegion.trim().isEmpty()) {
            throw new IllegalArgumentException("[参数错误] 主投放区域(primaryRegion)不能为空");
        }
    }

    // ==================== 档位参数校验 ====================

    /**
     * 校验档位参数（30个档位值，非负）。
     *
     * @param grades 档位值列表，必须包含30个非负数值
     * @throws IllegalArgumentException 参数无效时抛出
     * @example validateGradesParams([100, 90, 80, ...]) -> 校验通过（30个非负值）
     * @example validateGradesParams([100, -1, ...]) -> 抛出异常（含负数）
     */
    public static void validateGradesParams(List<BigDecimal> grades) {
        if (grades == null || grades.isEmpty()) {
            throw new IllegalArgumentException("[参数错误] 档位投放量(grades)不能为空");
        }
        if (grades.size() != 30) {
            throw new IllegalArgumentException(String.format(
                    "[参数错误] 档位投放量(grades)数量错误，期望30个，实际%d个", grades.size()));
        }
        for (int i = 0; i < grades.size(); i++) {
            BigDecimal grade = grades.get(i);
            if (grade != null && grade.compareTo(BigDecimal.ZERO) < 0) {
                throw new IllegalArgumentException(String.format(
                        "[参数错误] 档位D%d的投放量不能为负数，当前值=%s", 30 - i, grade));
            }
        }
    }

    // ==================== 组合参数校验 ====================

    /**
     * 校验删除区域操作的参数（时间 + 卷烟 + 区域）。
     *
     * @param year          年份
     * @param month         月份
     * @param weekSeq       周序号
     * @param cigCode       卷烟代码
     * @param cigName       卷烟名称
     * @param primaryRegion 主区域
     * @throws IllegalArgumentException 参数无效时抛出
     */
    public static void validateDeleteRegionParams(Integer year, Integer month, Integer weekSeq,
                                                   String cigCode, String cigName, String primaryRegion) {
        validateTimeParams(year, month, weekSeq);
        validateCigaretteParams(cigCode, cigName);
        validateRegionParams(primaryRegion);
    }

    /**
     * 校验删除卷烟操作的参数（时间 + 卷烟）。
     *
     * @param year    年份
     * @param month   月份
     * @param weekSeq 周序号
     * @param cigCode 卷烟代码
     * @param cigName 卷烟名称
     * @throws IllegalArgumentException 参数无效时抛出
     */
    public static void validateDeleteCigaretteParams(Integer year, Integer month, Integer weekSeq,
                                                      String cigCode, String cigName) {
        validateTimeParams(year, month, weekSeq);
        validateCigaretteParams(cigCode, cigName);
    }

    /**
     * 校验新增/修改区域操作的参数（时间 + 卷烟 + 区域 + 档位）。
     *
     * @param year          年份
     * @param month         月份
     * @param weekSeq       周序号
     * @param cigCode       卷烟代码
     * @param cigName       卷烟名称
     * @param primaryRegion 主区域
     * @param grades        档位值列表
     * @throws IllegalArgumentException 参数无效时抛出
     */
    public static void validateAddOrUpdateParams(Integer year, Integer month, Integer weekSeq,
                                                  String cigCode, String cigName, String primaryRegion,
                                                  List<BigDecimal> grades) {
        validateTimeParams(year, month, weekSeq);
        validateCigaretteParams(cigCode, cigName);
        validateRegionParams(primaryRegion);
        validateGradesParams(grades);
    }

    // ==================== 辅助方法 ====================

    /**
     * 拼接投放区域名称。
     *
     * @param primaryRegion   主区域
     * @param secondaryRegion 子区域（可为 null 或空）
     * @return 拼接后的区域名称，格式为 "主区域" 或 "主区域（子区域）"
     * @example buildDeliveryArea("城区", null) -> "城区"
     * @example buildDeliveryArea("城区", "A片区") -> "城区（A片区）"
     */
    public static String buildDeliveryArea(String primaryRegion, String secondaryRegion) {
        if (secondaryRegion == null || secondaryRegion.trim().isEmpty()) {
            return primaryRegion;
        }
        return primaryRegion + "（" + secondaryRegion + "）";
    }

    /**
     * 判断是否为双扩展场景。
     *
     * @param secondaryRegion 子区域
     * @return true 表示双扩展（子区域非空），false 表示单扩展
     * @example isDualExtension("A片区") -> true
     * @example isDualExtension(null) -> false
     */
    public static boolean isDualExtension(String secondaryRegion) {
        return secondaryRegion != null && !secondaryRegion.trim().isEmpty();
    }

    /**
     * 格式化区域集合用于错误信息展示。
     *
     * @param regions  区域集合
     * @param maxCount 最大展示数量
     * @return 格式化后的字符串，超出部分显示"等N个"
     * @example formatRegionSet({"城区", "郊区", "农村"}, 2) -> "城区、郊区等3个"
     */
    public static String formatRegionSet(Set<String> regions, int maxCount) {
        if (regions == null || regions.isEmpty()) {
            return "（无）";
        }
        StringBuilder sb = new StringBuilder();
        int count = 0;
        for (String region : regions) {
            if (count > 0) {
                sb.append("、");
            }
            sb.append(region);
            count++;
            if (count >= maxCount) {
                int remaining = regions.size() - maxCount;
                if (remaining > 0) {
                    sb.append("等").append(regions.size()).append("个");
                }
                break;
            }
        }
        return sb.toString();
    }
}
