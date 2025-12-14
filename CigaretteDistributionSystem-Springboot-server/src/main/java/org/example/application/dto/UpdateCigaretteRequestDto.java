package org.example.application.dto;

import lombok.Data;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.util.List;

@Data
public class UpdateCigaretteRequestDto {
    
    @NotBlank(message = "卷烟代码不能为空")
    private String cigCode;
    
    @NotBlank(message = "卷烟名称不能为空")
    private String cigName;
    
    @NotNull(message = "年份不能为空")
    private Integer year;
    
    @NotNull(message = "月份不能为空")
    private Integer month;
    
    @NotNull(message = "周序号不能为空")
    private Integer weekSeq;
    
    @NotBlank(message = "投放方式不能为空")
    private String deliveryMethod;
    
    @NotBlank(message = "扩展投放方式不能为空")
    private String deliveryEtype;
    
    @NotBlank(message = "投放区域不能为空")
    private String deliveryArea;
    
    @NotNull(message = "档位分配不能为空")
    private List<BigDecimal> distribution;
    
    private String bz;
}
