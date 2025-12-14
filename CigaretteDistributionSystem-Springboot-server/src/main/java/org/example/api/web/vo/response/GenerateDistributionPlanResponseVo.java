package org.example.api.web.vo.response;

import lombok.Data;
import java.io.Serializable;

/**
 * 生成分配计划响应VO
 * 用于API层返回HTTP响应
 */
@Data
public class GenerateDistributionPlanResponseVo implements Serializable {
    private static final long serialVersionUID = 1L;
    
    private Boolean success;
    private String message;
    private String errorCode;
    private Integer year;
    private Integer month;
    private Integer weekSeq;
    private Boolean deletedExistingData;
    private Integer deletedRecords;
    private Integer processedCount;
    private String processingTime;
    private Integer totalCigarettes;
    private Integer successfulAllocations;
}

