package org.example.application.dto;

import lombok.Data;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

/**
 * 总实际投放量计算响应DTO
 */
@Data
public class TotalActualDeliveryResponseDto {
    private Boolean success;
    private String message;
    private Map<String, BigDecimal> data;
    private Integer year;
    private Integer month;
    private Integer weekSeq;
    private Integer totalRecords;
    private Integer cigaretteCount;
    
    /**
     * 转换为Map格式，用于HTTP响应
     */
    public Map<String, Object> toMap() {
        Map<String, Object> map = new HashMap<>();
        map.put("success", success);
        map.put("message", message);
        map.put("data", data != null ? data : new HashMap<>());
        map.put("year", year);
        map.put("month", month);
        map.put("weekSeq", weekSeq);
        map.put("totalRecords", totalRecords);
        map.put("cigaretteCount", cigaretteCount);
        return map;
    }
    
    public boolean isSuccess() {
        return Boolean.TRUE.equals(success);
    }
}

