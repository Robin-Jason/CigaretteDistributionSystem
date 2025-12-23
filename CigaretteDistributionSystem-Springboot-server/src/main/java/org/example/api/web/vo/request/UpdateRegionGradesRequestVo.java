package org.example.api.web.vo.request;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import java.math.BigDecimal;
import java.util.List;

/**
 * 修改区域档位值请求 VO
 *
 * @author Robin
 * @since 2025-12-22
 */
@Data
public class UpdateRegionGradesRequestVo {

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

    @NotNull(message = "档位值不能为空")
    @Size(min = 30, max = 30, message = "档位值必须为30个")
    private List<BigDecimal> grades;

    /**
     * 备注（可选）
     */
    private String remark;
}
