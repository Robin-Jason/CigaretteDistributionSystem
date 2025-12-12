package org.example.dto;

import lombok.Data;

import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;
import java.util.List;

/**
 * 计算区域客户数请求DTO
 * 用于接收前端传入的客户筛选条件和时间参数
 */
@Data
public class CalRegionCustomerNumRequestDto {
    
    /**
     * 年份 (2020-2099)
     */
    @NotNull(message = "年份不能为空")
    @Min(value = 2020, message = "年份不能小于2020")
    @Max(value = 2099, message = "年份不能大于2099")
    private Integer year;
    
    /**
     * 月份 (1-12)
     */
    @NotNull(message = "月份不能为空")
    @Min(value = 1, message = "月份必须在1-12之间")
    @Max(value = 12, message = "月份必须在1-12之间")
    private Integer month;
    
    /**
     * 周序号 (1-5)
     */
    @NotNull(message = "周序号不能为空")
    @Min(value = 1, message = "周序号必须在1-5之间")
    @Max(value = 5, message = "周序号必须在1-5之间")
    private Integer weekSeq;
    
    /**
     * 客户类型子集
     * 可选值：单周客户、双周客户、正常客户
     * 例如：["单周客户", "正常客户"]
     */
    @NotNull(message = "客户类型不能为空")
    private List<String> customerTypes;
    
    /**
     * 工作日子集
     * 可选值：周一、周二、周三、周四、周五
     * 例如：["周一", "周三", "周五"]
     */
    @NotNull(message = "工作日不能为空")
    private List<String> workdays;
}

