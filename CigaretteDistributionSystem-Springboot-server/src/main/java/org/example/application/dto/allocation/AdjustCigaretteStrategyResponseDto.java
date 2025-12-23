package org.example.application.dto.allocation;

import lombok.Data;

import java.util.List;
import java.util.Map;

/**
 * 卷烟投放策略调整响应 DTO
 *
 * @author Robin
 * @since 2025-12-22
 */
@Data
public class AdjustCigaretteStrategyResponseDto {

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

    public static AdjustCigaretteStrategyResponseDto success(List<Map<String, Object>> records, String message) {
        AdjustCigaretteStrategyResponseDto dto = new AdjustCigaretteStrategyResponseDto();
        dto.setSuccess(true);
        dto.setMessage(message);
        dto.setAllocationRecords(records);
        dto.setRecordCount(records != null ? records.size() : 0);
        return dto;
    }

    public static AdjustCigaretteStrategyResponseDto failure(String message) {
        AdjustCigaretteStrategyResponseDto dto = new AdjustCigaretteStrategyResponseDto();
        dto.setSuccess(false);
        dto.setMessage(message);
        return dto;
    }
}
