package org.example.domain.service.rule;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 预测分配数据业务校验规则领域服务接口。
 * <p>
 * 提供预测分配数据增删改操作的业务规则校验，包括：
 * <ul>
 *   <li>卷烟存在性校验</li>
 *   <li>区域合法性校验</li>
 *   <li>档位单调性校验</li>
 *   <li>记录存在性校验</li>
 * </ul>
 * </p>
 * <p>
 * 纯领域逻辑，不依赖 Spring 或持久化层。校验结果通过 {@link ValidationResult} 返回。
 * </p>
 *
 * @author Robin
 * @since 2025-12-22
 */
public interface PredictionValidationRule {

    // ==================== 错误码常量 ====================

    String ERR_CIGARETTE_NOT_FOUND = "CIGARETTE_NOT_FOUND";
    String ERR_REGION_INVALID = "REGION_INVALID";
    String ERR_GRADES_MONOTONICITY = "GRADES_MONOTONICITY";
    String ERR_RECORD_EXISTS = "RECORD_EXISTS";
    String ERR_RECORD_NOT_FOUND = "RECORD_NOT_FOUND";
    String ERR_REGION_STATS_NOT_FOUND = "REGION_STATS_NOT_FOUND";
    String ERR_BATCH_NOT_FOUND = "BATCH_NOT_FOUND";

    // ==================== 卷烟校验 ====================

    /**
     * 校验卷烟是否存在于投放信息表。
     *
     * @param cigInfo 查询结果（可为 null 表示不存在）
     * @param cigCode 卷烟代码
     * @param cigName 卷烟名称
     * @param year    年份
     * @param month   月份
     * @param weekSeq 周序号
     * @return 校验结果
     * @example validateCigaretteExists(null, "001", "中华", 2025, 9, 3) -> 失败，卷烟不存在
     * @example validateCigaretteExists({...}, "001", "中华", 2025, 9, 3) -> 成功
     */
    ValidationResult validateCigaretteExists(Map<String, Object> cigInfo,
                                             String cigCode, String cigName,
                                             Integer year, Integer month, Integer weekSeq);

    // ==================== 批次校验 ====================

    /**
     * 校验批次区域统计数据是否存在。
     *
     * @param allStats 区域统计数据列表
     * @param year     年份
     * @param month    月份
     * @param weekSeq  周序号
     * @return 校验结果
     * @example validateBatchRegionStatsExists([], 2025, 9, 3) -> 失败，批次数据为空
     * @example validateBatchRegionStatsExists([{...}], 2025, 9, 3) -> 成功
     */
    ValidationResult validateBatchRegionStatsExists(List<Map<String, Object>> allStats,
                                                    Integer year, Integer month, Integer weekSeq);

    // ==================== 区域校验 ====================

    /**
     * 校验投放区域合法性（单扩展场景）。
     *
     * @param primaryRegion 主区域
     * @param validRegions  合法区域集合（完整区域名）
     * @param primarySet    主区域集合（从双扩展区域解析出的主区域）
     * @return 校验结果
     * @example validateSingleExtensionRegion("城区", {"城区", "郊区"}, {"城区"}) -> 成功
     * @example validateSingleExtensionRegion("无效区", {"城区"}, {"城区"}) -> 失败
     */
    ValidationResult validateSingleExtensionRegion(String primaryRegion,
                                                   Set<String> validRegions,
                                                   Set<String> primarySet);

    /**
     * 校验投放区域合法性（双扩展场景）。
     *
     * @param primaryRegion   主区域
     * @param secondaryRegion 子区域
     * @param deliveryArea    拼接后的完整区域名
     * @param validRegions    合法区域集合
     * @param primarySet      主区域集合
     * @param secondarySet    子区域集合
     * @return 校验结果
     * @example validateDualExtensionRegion("城区", "A片区", "城区（A片区）", {...}, {...}, {...}) -> 成功
     */
    ValidationResult validateDualExtensionRegion(String primaryRegion,
                                                 String secondaryRegion,
                                                 String deliveryArea,
                                                 Set<String> validRegions,
                                                 Set<String> primarySet,
                                                 Set<String> secondarySet);

    /**
     * 校验区域客户统计数据是否存在。
     *
     * @param stat       统计数据（可为 null）
     * @param regionName 区域名称
     * @return 校验结果
     * @example validateRegionStatsExists(null, "城区") -> 失败
     * @example validateRegionStatsExists({...}, "城区") -> 成功
     */
    ValidationResult validateRegionStatsExists(Map<String, Object> stat, String regionName);

    // ==================== 档位校验 ====================

    /**
     * 校验档位值单调性（D30 >= D29 >= ... >= D1）。
     *
     * @param grades 档位值列表（索引0对应D30，索引29对应D1）
     * @return 校验结果
     * @example validateGradesMonotonicity([100, 90, 80, ...]) -> 成功（单调递减）
     * @example validateGradesMonotonicity([100, 110, 80, ...]) -> 失败（D29 > D30）
     */
    ValidationResult validateGradesMonotonicity(List<BigDecimal> grades);

    /**
     * 校验档位值单调性（基于 HG/LG 范围）。
     * <p>
     * 验证规则：
     * <ul>
     *   <li>HG 之前的档位（更高档位）必须全为 0</li>
     *   <li>HG~LG 范围内的档位必须满足单调递减（D_HG >= D_HG+1 >= ... >= D_LG）</li>
     *   <li>LG 之后的档位（更低档位）必须全为 0</li>
     * </ul>
     * </p>
     *
     * @param grades 档位值列表（索引0对应D30，索引29对应D1）
     * @param hg     最高档位（如 "D25"）
     * @param lg     最低档位（如 "D15"）
     * @return 校验结果
     * @example validateGradesMonotonicityWithRange([0,0,0,0,0,100,90,80,...,0,0], "D25", "D15") -> 成功
     * @example validateGradesMonotonicityWithRange([10,0,0,0,0,100,90,80,...], "D25", "D15") -> 失败（D30 不为 0）
     * @example validateGradesMonotonicityWithRange([0,0,0,0,0,100,110,80,...], "D25", "D15") -> 失败（D24 > D25）
     */
    ValidationResult validateGradesMonotonicityWithRange(List<BigDecimal> grades, String hg, String lg);

    // ==================== 记录存在性校验 ====================

    /**
     * 校验记录是否已存在（用于新增场景，存在则失败）。
     *
     * @param exists       是否存在
     * @param cigCode      卷烟代码
     * @param cigName      卷烟名称
     * @param deliveryArea 投放区域
     * @return 校验结果
     * @example validateRecordNotExists(true, "001", "中华", "城区") -> 失败，记录已存在
     * @example validateRecordNotExists(false, "001", "中华", "城区") -> 成功
     */
    ValidationResult validateRecordNotExists(boolean exists,
                                             String cigCode, String cigName,
                                             String deliveryArea);

    /**
     * 校验记录是否存在（用于修改/删除场景，不存在则失败）。
     *
     * @param exists       是否存在
     * @param cigCode      卷烟代码
     * @param cigName      卷烟名称
     * @param deliveryArea 投放区域
     * @return 校验结果
     * @example validateRecordExists(false, "001", "中华", "城区") -> 失败，记录不存在
     * @example validateRecordExists(true, "001", "中华", "城区") -> 成功
     */
    ValidationResult validateRecordExists(boolean exists,
                                          String cigCode, String cigName,
                                          String deliveryArea);

    // ==================== 区域解析 ====================

    /**
     * 从区域统计数据中解析区域集合。
     *
     * @param allStats 区域统计数据列表
     * @return RegionSets 包含完整区域集合、主区域集合、子区域集合
     * @example parseRegionSets([{region: "城区（A片区）"}, {region: "郊区"}])
     *          -> {validRegions: ["城区（A片区）", "郊区"], primaryRegions: ["城区", "郊区"], secondaryRegions: ["A片区"]}
     */
    RegionSets parseRegionSets(List<Map<String, Object>> allStats);

    // ==================== 内部类 ====================

    /**
     * 校验结果封装类。
     */
    class ValidationResult {
        private final boolean valid;
        private final String errorCode;
        private final String errorMessage;

        private ValidationResult(boolean valid, String errorCode, String errorMessage) {
            this.valid = valid;
            this.errorCode = errorCode;
            this.errorMessage = errorMessage;
        }

        /**
         * 创建成功结果。
         *
         * @return 成功的校验结果
         */
        public static ValidationResult success() {
            return new ValidationResult(true, null, null);
        }

        /**
         * 创建失败结果。
         *
         * @param errorCode    错误码
         * @param errorMessage 错误信息
         * @return 失败的校验结果
         */
        public static ValidationResult fail(String errorCode, String errorMessage) {
            return new ValidationResult(false, errorCode, errorMessage);
        }

        public boolean isValid() {
            return valid;
        }

        public String getErrorCode() {
            return errorCode;
        }

        public String getErrorMessage() {
            return errorMessage;
        }

        /**
         * 校验失败时抛出 IllegalArgumentException。
         *
         * @throws IllegalArgumentException 校验失败时抛出
         */
        public void throwIfInvalid() {
            if (!valid) {
                throw new IllegalArgumentException(errorMessage);
            }
        }
    }

    /**
     * 区域集合封装类。
     */
    class RegionSets {
        private final Set<String> validRegions;
        private final Set<String> primaryRegions;
        private final Set<String> secondaryRegions;

        public RegionSets(Set<String> validRegions, Set<String> primaryRegions, Set<String> secondaryRegions) {
            this.validRegions = validRegions;
            this.primaryRegions = primaryRegions;
            this.secondaryRegions = secondaryRegions;
        }

        public Set<String> getValidRegions() {
            return validRegions;
        }

        public Set<String> getPrimaryRegions() {
            return primaryRegions;
        }

        public Set<String> getSecondaryRegions() {
            return secondaryRegions;
        }
    }
}
