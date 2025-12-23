package org.example.application.dto.importing;

import lombok.Data;
import org.springframework.web.multipart.MultipartFile;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Min;
import javax.validation.constraints.Max;

/**
 * 数据导入请求DTO
 * 用于同时导入客户基础信息表和卷烟投放基础信息表
 */
@Data
public class DataImportRequestDto {
    
    /**
     * 客户基础信息表Excel文件（可选）。
     * 未提供时跳过客户表导入，仅导入卷烟投放基础信息表。
     */
    private MultipartFile baseCustomerInfoFile;
    
    /**
     * 卷烟投放基础信息表Excel文件（必填）。
     */
    @NotNull(message = "卷烟投放基础信息表Excel文件不能为空")
    private MultipartFile cigaretteDistributionInfoFile;
    
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

