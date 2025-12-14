package org.example.api.web.vo.response;

import lombok.Data;
import java.io.Serializable;
import java.util.List;
import java.util.Map;

/**
 * 预测查询响应VO
 */
@Data
public class PredictionQueryResponseVo implements Serializable {
    private static final long serialVersionUID = 1L;
    
    private List<Map<String, Object>> data;
    private Integer total;
}

