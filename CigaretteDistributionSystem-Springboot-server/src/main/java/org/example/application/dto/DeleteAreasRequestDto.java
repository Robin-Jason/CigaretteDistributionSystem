package org.example.application.dto;

import lombok.Data;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import java.util.List;

@Data
public class DeleteAreasRequestDto {
    
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
    
    @NotEmpty(message = "要删除的投放区域列表不能为空")
    private List<String> areasToDelete;
}
