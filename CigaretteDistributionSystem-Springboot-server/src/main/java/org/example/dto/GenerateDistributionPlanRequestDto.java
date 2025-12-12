package org.example.dto;

import lombok.Data;
import javax.validation.constraints.NotNull;
import java.math.BigDecimal;

/**
 * 一键生成分配方案请求DTO
 */
@Data
public class GenerateDistributionPlanRequestDto {
    @NotNull(message = "年份不能为空")
    private Integer year;
    
    @NotNull(message = "月份不能为空")
    private Integer month;
    
    @NotNull(message = "周序号不能为空")
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

