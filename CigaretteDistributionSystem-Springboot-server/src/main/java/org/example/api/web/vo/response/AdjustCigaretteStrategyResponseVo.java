package org.example.api.web.vo.response;

import lombok.Data;

import java.util.List;
import java.util.Map;

/**
 * 卷烟投放策略调整响应 VO
 *
 * @author Robin
 * @since 2025-12-22
 */
@Data
public class AdjustCigaretteStrategyResponseVo {

    private Boolean success;
    private String message;
    
    /**
     * 调整后的卷烟分配记录列表
     */
    private List<Map<String, Object>> allocationRecords;
    
    /**
     * 新生成的记录数
     */
    private Integer recordCount;
}
