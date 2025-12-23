package org.example.api.web.vo.request;

import lombok.Data;
import org.springframework.web.multipart.MultipartFile;

import javax.validation.constraints.NotNull;

/**
 * 客户基础信息表导入请求VO
 */
@Data
public class BaseCustomerInfoImportRequestVo {
    
    /**
     * 客户基础信息表Excel文件
     */
    @NotNull(message = "客户基础信息表Excel文件不能为空")
    private MultipartFile file;
}
