package org.example.domain.event;

import lombok.Data;
import java.io.Serializable;

/**
 * 分配方案生成开始事件
 */
@Data
public class DistributionPlanGenerationStartedEvent implements Serializable {
    private static final long serialVersionUID = 1L;
    
    private Integer year;
    private Integer month;
    private Integer weekSeq;
    /**
     * 批次号：用于关联同一批次内的导入、分配等操作
     * 约定格式：year-month-weekSeq，例如 2025-09-3
     */
    private String batchId;
    private Long startTime;
    private String requestId; // 可选：用于追踪（如一次HTTP请求的ID）
    
    public DistributionPlanGenerationStartedEvent(Integer year, Integer month, Integer weekSeq) {
        this.year = year;
        this.month = month;
        this.weekSeq = weekSeq;
        this.batchId = buildBatchId(year, month, weekSeq);
        this.startTime = System.currentTimeMillis();
    }

    private String buildBatchId(Integer year, Integer month, Integer weekSeq) {
        if (year == null || month == null || weekSeq == null) {
            return null;
        }
        return year + "-" + month + "-" + weekSeq;
    }
}

