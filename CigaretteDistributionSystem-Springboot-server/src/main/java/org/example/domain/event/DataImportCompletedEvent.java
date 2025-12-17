package org.example.domain.event;

import lombok.Data;
import java.io.Serializable;
import java.util.Map;

/**
 * 数据导入完成事件
 */
@Data
public class DataImportCompletedEvent implements Serializable {
    private static final long serialVersionUID = 1L;
    
    private Integer year;
    private Integer month;
    private Integer weekSeq;
    /**
     * 批次号：用于关联同一批次内的导入、分配等操作
     * 约定格式：year-month-weekSeq，例如 2025-09-3
     */
    private String batchId;
    private Boolean success;
    private String message;
    private Map<String, Object> baseCustomerInfoResult;
    private Map<String, Object> cigaretteDistributionInfoResult;
    private Long startTime;
    private Long endTime;
    
    public DataImportCompletedEvent(Integer year, Integer month, Integer weekSeq,
                                   Boolean success, String message) {
        this.year = year;
        this.month = month;
        this.weekSeq = weekSeq;
        this.batchId = buildBatchId(year, month, weekSeq);
        this.success = success;
        this.message = message;
    }

    private String buildBatchId(Integer year, Integer month, Integer weekSeq) {
        if (year == null || month == null || weekSeq == null) {
            return null;
        }
        return year + "-" + month + "-" + weekSeq;
    }
}

