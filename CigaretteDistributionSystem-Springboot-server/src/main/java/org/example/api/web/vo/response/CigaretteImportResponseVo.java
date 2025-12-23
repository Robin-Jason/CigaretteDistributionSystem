package org.example.api.web.vo.response;

import lombok.Data;
import java.io.Serializable;

/**
 * 卷烟投放基础信息表导入响应VO
 */
@Data
public class CigaretteImportResponseVo implements Serializable {
    private static final long serialVersionUID = 1L;
    
    private Boolean success;
    private String message;
    private Integer insertedCount;
    private Integer totalRows;
}
