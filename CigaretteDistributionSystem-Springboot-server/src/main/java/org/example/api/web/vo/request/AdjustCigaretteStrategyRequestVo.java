package org.example.api.web.vo.request;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Positive;
import java.math.BigDecimal;

/**
 * 卷烟投放策略调整请求 VO
 *
 * @author Robin
 * @since 2025-12-22
 */
@Data
public class AdjustCigaretteStrategyRequestVo {

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
}
