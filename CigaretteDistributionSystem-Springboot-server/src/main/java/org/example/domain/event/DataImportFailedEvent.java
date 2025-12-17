package org.example.domain.event;

import lombok.Data;
import java.io.Serializable;

/**
 * 数据导入失败事件
 */
@Data
public class DataImportFailedEvent implements Serializable {
    private static final long serialVersionUID = 1L;
    
    private Integer year;
    private Integer month;
    private Integer weekSeq;
    /**
     * 批次号：用于关联同一批次内的导入、分配等操作
     * 约定格式：year-month-weekSeq，例如 2025-09-3
     */
    private String batchId;
    private String errorMessage;
    private Exception exception;
    
    public DataImportFailedEvent(Integer year, Integer month, Integer weekSeq,
                                String errorMessage, Exception exception) {
        this.year = year;
        this.month = month;
        this.weekSeq = weekSeq;
        this.batchId = buildBatchId(year, month, weekSeq);
        this.errorMessage = errorMessage;
        this.exception = exception;
    }

    private String buildBatchId(Integer year, Integer month, Integer weekSeq) {
        if (year == null || month == null || weekSeq == null) {
            return null;
        }
        return year + "-" + month + "-" + weekSeq;
    }
}

