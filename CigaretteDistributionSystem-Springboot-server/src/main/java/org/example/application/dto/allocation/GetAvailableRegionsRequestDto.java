package org.example.application.dto.allocation;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

/**
 * 获取可用投放区域列表请求 DTO
 *
 * @author Robin
 * @since 2025-12-23
 */
@Data
public class GetAvailableRegionsRequestDto {

    @NotNull(message = "年份不能为空")
    private Integer year;

    @NotNull(message = "月份不能为空")
    private Integer month;

    @NotNull(message = "周序号不能为空")
    private Integer weekSeq;

    @NotBlank(message = "投放类型不能为空")
    private String deliveryMethod;

    /**
     * 扩展投放类型（可选）
     * <p>
     * 例如："市场类型"、"区县"、"区县+市场类型"
     * </p>
     */
    private String deliveryEtype;

    /**
     * 标签（可选）
     * <p>
     * 如果指定标签，返回的区域列表会考虑标签过滤
     * </p>
     */
    private String tag;
}
