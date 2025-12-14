package org.example.application.dto;

import lombok.Data;

import java.util.List;
import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;

/**
 * 请求重新计算区域客户统计矩阵的 DTO。
 */
@Data
public class RegionCustomerStatisticsRequestDto {

    @NotNull(message = "年份不能为空")
    @Min(value = 2020, message = "年份不能小于2020")
    @Max(value = 2099, message = "年份不能大于2099")
    private Integer year;

    @NotNull(message = "月份不能为空")
    @Min(value = 1, message = "月份不能小于1")
    @Max(value = 12, message = "月份不能大于12")
    private Integer month;

    @NotNull(message = "周序号不能为空")
    @Min(value = 1, message = "周序号不能小于1")
    @Max(value = 5, message = "周序号不能大于5")
    private Integer weekSeq;

    /**
     * 当统计表已存在时是否覆盖重建，默认 true。
     */
    private Boolean overwriteExisting = Boolean.TRUE;

    /**
     * 客户类型筛选（单周客户、双周客户、正常客户），为 null 或空时默认使用全部。
     */
    private List<String> customerTypes;

    /**
     * 工作日筛选（周一~周五），为 null 或空时默认使用全部。
     */
    private List<String> workdays;
}

