package org.example.application.dto;

import lombok.Data;
import org.springframework.web.multipart.MultipartFile;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Min;
import javax.validation.constraints.Max;

@Data
public class CigaretteImportRequestDto {
    
    @NotNull(message = "Excel文件不能为空")
    private MultipartFile file;
    
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
}
