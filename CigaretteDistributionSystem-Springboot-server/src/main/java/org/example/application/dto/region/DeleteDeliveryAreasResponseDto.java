package org.example.application.dto.region;

import lombok.Data;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 删除投放区域响应DTO
 */
@Data
public class DeleteDeliveryAreasResponseDto {
    private Boolean success;
    private String message;
    private String errorCode;
    private Integer deletedCount;
    private List<String> deletedAreas;
    private List<String> remainingAreas;
    private Integer remainingCount;
    private List<String> currentAreas;
    
    /**
     * 转换为Map格式，用于HTTP响应
     */
    public Map<String, Object> toMap() {
        Map<String, Object> map = new HashMap<>();
        map.put("success", success);
        map.put("message", message);
        if (errorCode != null) {
            map.put("error", errorCode);
        }
        if (deletedCount != null) {
            map.put("deletedCount", deletedCount);
        }
        map.put("deletedAreas", deletedAreas != null ? deletedAreas : new ArrayList<>());
        map.put("remainingAreas", remainingAreas != null ? remainingAreas : new ArrayList<>());
        if (remainingCount != null) {
            map.put("remainingCount", remainingCount);
        }
        if (currentAreas != null) {
            map.put("currentAreas", currentAreas);
        }
        return map;
    }
    
    public boolean isSuccess() {
        return Boolean.TRUE.equals(success);
    }
}

