package org.example.dto;

import lombok.Data;
import org.springframework.web.multipart.MultipartFile;

import javax.validation.constraints.NotNull;

/**
 * 客户基础信息导入请求 DTO
 */
@Data
public class BaseCustomerInfoImportRequestDto {

    @NotNull(message = "Excel文件不能为空")
    private MultipartFile file;
}

