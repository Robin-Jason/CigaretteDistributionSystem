package org.example.application.dto.allocation;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Positive;
import java.math.BigDecimal;
import java.util.List;

/**
 * 卷烟投放策略调整请求 DTO
 *
 * @author Robin
 * @since 2025-12-22
 */
@Data
public class AdjustCigaretteStrategyRequestDto {

    @NotNull(message = "年份不能为空")
    private Integer year;

    @NotNull(message = "月份不能为空")
    private Integer month;

    @NotNull(message = "周序号不能为空")
    private Integer weekSeq;

    @NotBlank(message = "卷烟代码不能为空")
    private String cigCode;

    @NotBlank(message = "卷烟名称不能为空")
    private String cigName;

    @NotBlank(message = "新投放类型不能为空")
    private String newDeliveryMethod;

    /**
     * 新扩展投放类型（可选）
     */
    private String newDeliveryEtype;

    /**
     * 新标签（可选，最多1个）
     */
    private String newTag;

    /**
     * 新标签过滤值（与 newTag 配套使用）
     */
    private String newTagFilterValue;

    @NotNull(message = "新建议投放量不能为空")
    @Positive(message = "新建议投放量必须大于0")
    private BigDecimal newAdvAmount;

    /**
     * 新投放区域列表（必填）
     * <p>
     * 用户需要传入笛卡尔积后的完整区域列表，每个区域对应分配矩阵的一行。
     * </p>
     * <p>
     * 格式示例：
     * - 单扩展单区域：["全市"]
     * - 单扩展多区域：["丹江", "郧西", "竹山"]
     * - 双扩展（区县+市场类型）：["丹江（城网）", "丹江（农网）", "郧西（城网）", "郧西（农网）"]
     * - 双扩展（市场类型+区县）：["城网（丹江）", "城网（郧西）", "农网（丹江）", "农网（郧西）"]
     * </p>
     * <p>
     * 注意：
     * 1. 区域顺序会影响分配结果的顺序
     * 2. 如果是双扩展，建议主扩展写在括号外，子扩展写在括号内
     * 3. 系统会根据这个列表构建客户矩阵并执行分配算法
     * </p>
     */
    @NotNull(message = "新投放区域列表不能为空")
    @NotEmpty(message = "新投放区域列表不能为空")
    private List<String> newDeliveryAreas;

    /**
     * 新最高档位（可选，默认 D30）
     * <p>
     * 指定分配算法的最高档位，例如 "D30"、"D25" 等。
     * 如果不指定，默认为 D30。
     * </p>
     */
    private String newHighestGrade;

    /**
     * 新最低档位（可选，默认 D1）
     * <p>
     * 指定分配算法的最低档位，例如 "D1"、"D15" 等。
     * 如果不指定，默认为 D1。
     * </p>
     */
    private String newLowestGrade;

    /**
     * 城网比例（可选，仅用于市场类型扩展）
     * <p>
     * 当投放类型包含"市场类型"扩展时，可以指定城网的分配比例。
     * 如果不指定，使用默认比例 0.4（城网）: 0.6（农网）。
     * </p>
     * <p>
     * 注意：urbanRatio 和 ruralRatio 必须同时指定或同时不指定。
     * </p>
     */
    private BigDecimal urbanRatio;

    /**
     * 农网比例（可选，仅用于市场类型扩展）
     * <p>
     * 当投放类型包含"市场类型"扩展时，可以指定农网的分配比例。
     * 如果不指定，使用默认比例 0.4（城网）: 0.6（农网）。
     * </p>
     * <p>
     * 注意：urbanRatio 和 ruralRatio 必须同时指定或同时不指定。
     * </p>
     */
    private BigDecimal ruralRatio;

    /**
     * 新备注（可选）
     * <p>
     * 用于更新 Info 表的 BZ 字段。
     * 如果不指定，系统会自动生成备注："已人工调整策略{投放类型}（标签）"。
     * </p>
     * <p>
     * 注意：预测表（prediction/prediction_price）的备注固定为"人工已确认投放组合与投放区域修改"，不受此字段影响。
     * </p>
     */
    private String newRemark;
}
