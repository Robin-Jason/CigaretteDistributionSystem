package org.example.application.dto;

import lombok.Data;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 查询卷烟分配数据响应DTO
 */
@Data
public class QueryCigaretteDistributionResponseDto {
    private Boolean success;
    private String message;
    private List<QueryCigaretteDistributionRecordDto> data;
    private Integer total;
    
    /**
     * 转换为Map格式，用于HTTP响应
     */
    public Map<String, Object> toMap() {
        Map<String, Object> map = new HashMap<>();
        map.put("success", success);
        map.put("message", message);
        map.put("data", data != null ? data : new ArrayList<>());
        map.put("total", total);
        return map;
    }
    
    public boolean isSuccess() {
        return Boolean.TRUE.equals(success);
    }
}

