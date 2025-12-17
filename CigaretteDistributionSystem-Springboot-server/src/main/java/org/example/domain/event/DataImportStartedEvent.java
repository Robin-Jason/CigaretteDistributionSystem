package org.example.domain.event;

import lombok.Data;
import java.io.Serializable;

/**
 * 数据导入开始事件
 */
@Data
public class DataImportStartedEvent implements Serializable {
    private static final long serialVersionUID = 1L;
    
    private Integer year;
    private Integer month;
    private Integer weekSeq;
    /**
     * 批次号：用于关联同一批次内的导入、分配等操作
     * 约定格式：year-month-weekSeq，例如 2025-09-3
     */
    private String batchId;
    private Boolean hasBaseCustomerFile;
    private Boolean hasCigaretteFile;
    private Long startTime;
    
    public DataImportStartedEvent(Integer year, Integer month, Integer weekSeq,
                                 Boolean hasBaseCustomerFile, Boolean hasCigaretteFile) {
        this.year = year;
        this.month = month;
        this.weekSeq = weekSeq;
        this.batchId = buildBatchId(year, month, weekSeq);
        this.hasBaseCustomerFile = hasBaseCustomerFile;
        this.hasCigaretteFile = hasCigaretteFile;
        this.startTime = System.currentTimeMillis();
    }

    private String buildBatchId(Integer year, Integer month, Integer weekSeq) {
        if (year == null || month == null || weekSeq == null) {
            return null;
        }
        return year + "-" + month + "-" + weekSeq;
    }
}

