package org.example.api.web.vo.request;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import java.math.BigDecimal;
import java.util.List;

/**
 * 新增投放区域分配记录请求 VO
 *
 * @author Robin
 * @since 2025-12-22
 */
@Data
public class AddRegionAllocationRequestVo {

    /**
     * 年份
     */
    @NotNull(message = "年份不能为空")
    private Integer year;

    /**
     * 月份
     */
    @NotNull(message = "月份不能为空")
    private Integer month;

    /**
     * 周序号
     */
    @NotNull(message = "周序号不能为空")
    private Integer weekSeq;

    /**
     * 卷烟代码
     */
    @NotBlank(message = "卷烟代码不能为空")
    private String cigCode;

    /**
     * 卷烟名称
     */
    @NotBlank(message = "卷烟名称不能为空")
    private String cigName;

    /**
     * 主投放区域（单扩展时为完整区域名，双扩展时为主扩展区域如"丹江"）
     */
    @NotBlank(message = "主投放区域不能为空")
    private String primaryRegion;

    /**
     * 子投放区域（双扩展时必填，如"城网"；单扩展时传空或不传）
     */
    private String secondaryRegion;

    /**
     * 30个档位的投放量值（D30-D1，索引0对应D30，索引29对应D1）
     */
    @NotNull(message = "档位投放量不能为空")
    @Size(min = 30, max = 30, message = "档位投放量必须为30个值")
    private List<BigDecimal> grades;

    /**
     * 备注（可选）
     */
    private String remark;
}
