package org.example.application.dto.prediction;

import lombok.Data;
import javax.validation.constraints.NotNull;

@Data
public class QueryRequestDto {
    @NotNull(message = "年份不能为空")
    private Integer year;
    
    @NotNull(message = "月份不能为空")
    private Integer month;
    
    @NotNull(message = "周序号不能为空")
    private Integer weekSeq;
}
