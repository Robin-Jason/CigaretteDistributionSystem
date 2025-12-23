package org.example.api.web.vo.request;

import lombok.Data;
import org.springframework.web.multipart.MultipartFile;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Min;
import javax.validation.constraints.Max;

/**
 * 卷烟投放基础信息表导入请求VO
 */
@Data
public class CigaretteImportRequestVo {
    
    /**
     * 卷烟投放基础信息表Excel文件
     */
    @NotNull(message = "卷烟投放基础信息表Excel文件不能为空")
    private MultipartFile file;
    
    /**
     * 年份
     */
    @NotNull(message = "年份不能为空")
    @Min(value = 2020, message = "年份不能小于2020")
    @Max(value = 2099, message = "年份不能大于2099")
    private Integer year;
    
    /**
     * 月份
     */
    @NotNull(message = "月份不能为空")
    @Min(value = 1, message = "月份不能小于1")
    @Max(value = 12, message = "月份不能大于12")
    private Integer month;
    
    /**
     * 周序号
     */
    @NotNull(message = "周序号不能为空")
    @Min(value = 1, message = "周序号不能小于1")
    @Max(value = 5, message = "周序号不能大于5")
    private Integer weekSeq;
}
