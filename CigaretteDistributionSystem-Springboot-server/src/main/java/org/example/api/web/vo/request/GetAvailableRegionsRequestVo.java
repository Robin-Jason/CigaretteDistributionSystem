package org.example.api.web.vo.request;

import lombok.Data;

import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;

/**
 * 获取可用投放区域列表请求VO
 * 
 * @author System
 * @version 1.0
 * @since 2025-12-23
 */
@Data
public class GetAvailableRegionsRequestVo {
    
    /**
     * 年份（必填）
     */
    @NotNull(message = "年份不能为空")
    @Min(value = 2020, message = "年份不能小于2020")
    @Max(value = 2100, message = "年份不能大于2100")
    private Integer year;
    
    /**
     * 月份（必填，1-12）
     */
    @NotNull(message = "月份不能为空")
    @Min(value = 1, message = "月份必须在1-12之间")
    @Max(value = 12, message = "月份必须在1-12之间")
    private Integer month;
    
    /**
     * 周序号（必填，1-5）
     */
    @NotNull(message = "周序号不能为空")
    @Min(value = 1, message = "周序号必须在1-5之间")
    @Max(value = 5, message = "周序号必须在1-5之间")
    private Integer weekSeq;
    
    /**
     * 投放类型（必填）
     * 例如："按档位投放"、"按价位段自选投放"
     */
    @NotNull(message = "投放类型不能为空")
    private String deliveryMethod;
    
    /**
     * 扩展类型（可选）
     * 例如："区县公司"、"市场类型"、"区县公司+市场类型"
     */
    private String deliveryEtype;
}
