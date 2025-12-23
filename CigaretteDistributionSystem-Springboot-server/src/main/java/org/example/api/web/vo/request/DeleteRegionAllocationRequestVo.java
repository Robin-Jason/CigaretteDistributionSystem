package org.example.api.web.vo.request;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

/**
 * 删除特定区域分配记录请求 VO
 *
 * @author Robin
 * @since 2025-12-22
 */
@Data
public class DeleteRegionAllocationRequestVo {

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

    @NotBlank(message = "主投放区域不能为空")
    private String primaryRegion;

    /**
     * 子投放区域（双扩展时必填，单扩展时传空或不传）
     */
    private String secondaryRegion;
}
