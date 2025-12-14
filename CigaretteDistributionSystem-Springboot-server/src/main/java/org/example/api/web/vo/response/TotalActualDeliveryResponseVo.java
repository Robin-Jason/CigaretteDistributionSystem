package org.example.api.web.vo.response;

import lombok.Data;
import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Map;

/**
 * 总实际投放量响应VO
 */
@Data
public class TotalActualDeliveryResponseVo implements Serializable {
    private static final long serialVersionUID = 1L;
    
    private Boolean success;
    private String message;
    private Map<String, BigDecimal> data;
    private Integer year;
    private Integer month;
    private Integer weekSeq;
    private Integer totalRecords;
    private Integer cigaretteCount;
}

