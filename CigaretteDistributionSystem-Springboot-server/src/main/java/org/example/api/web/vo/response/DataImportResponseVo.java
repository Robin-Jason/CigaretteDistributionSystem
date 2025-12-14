package org.example.api.web.vo.response;

import lombok.Data;
import java.io.Serializable;
import java.util.Map;

/**
 * 数据导入响应VO
 */
@Data
public class DataImportResponseVo implements Serializable {
    private static final long serialVersionUID = 1L;
    
    private Boolean success;
    private String message;
    private Map<String, Object> baseCustomerInfoResult;
    private Map<String, Object> cigaretteDistributionInfoResult;
    private String baseCustomerInfoNotice;
}

