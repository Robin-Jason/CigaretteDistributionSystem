package org.example.api.web.vo.request;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

/**
 * 删除整个卷烟分配记录请求 VO
 *
 * @author Robin
 * @since 2025-12-22
 */
@Data
public class DeleteCigaretteRequestVo {

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
}
