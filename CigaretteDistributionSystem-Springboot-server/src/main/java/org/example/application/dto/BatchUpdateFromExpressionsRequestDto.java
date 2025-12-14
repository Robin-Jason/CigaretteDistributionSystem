package org.example.application.dto;

import lombok.Data;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.util.List;

/**
 * 批量更新卷烟信息请求DTO（基于编码表达式）
 */
@Data
public class BatchUpdateFromExpressionsRequestDto {
    
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
    
    @NotNull(message = "编码表达式列表不能为空")
    private List<String> encodedExpressions;
    
    private String bz;
}
