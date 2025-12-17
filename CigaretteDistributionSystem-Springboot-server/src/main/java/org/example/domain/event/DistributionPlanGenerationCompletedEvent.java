package org.example.domain.event;

import lombok.Data;
import java.io.Serializable;

/**
 * 分配方案生成完成事件
 */
@Data
public class DistributionPlanGenerationCompletedEvent implements Serializable {
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
    private Long endTime;
    private Integer totalCount;
    private Integer successCount;
    private Integer failedCount;
    /**
     * 实际生成的分配记录条数（prediction 表中的记录数），用于监控/报表。
     */
    private Integer processedCount;
    private Boolean success;
    private String message;
    
    public DistributionPlanGenerationCompletedEvent(Integer year, Integer month, Integer weekSeq) {
        this.year = year;
        this.month = month;
        this.weekSeq = weekSeq;
        this.batchId = buildBatchId(year, month, weekSeq);
    }

    private String buildBatchId(Integer year, Integer month, Integer weekSeq) {
        if (year == null || month == null || weekSeq == null) {
            return null;
        }
        return year + "-" + month + "-" + weekSeq;
    }
}

