package org.example.api.web.vo.request;

import lombok.Data;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Min;
import javax.validation.constraints.Max;
import java.math.BigDecimal;

/**
 * 生成分配计划请求VO
 * 用于API层接收HTTP请求参数
 */
@Data
public class GenerateDistributionPlanRequestVo {
    
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
     * 城网比例（可选，仅用于档位+市场类型）
     */
    private BigDecimal urbanRatio;
    
    /**
     * 农网比例（可选，仅用于档位+市场类型）
     */
    private BigDecimal ruralRatio;
}

