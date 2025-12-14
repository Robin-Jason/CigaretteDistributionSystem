package org.example.application.dto;

import lombok.Data;
import java.util.HashMap;
import java.util.Map;

/**
 * 一键生成分配方案响应DTO
 */
@Data
public class GenerateDistributionPlanResponseDto {
    private Boolean success;
    private String message;
    private String error;
    private Integer year;
    private Integer month;
    private Integer weekSeq;
    private Boolean deletedExistingData;
    private Integer deletedRecords;
    private Integer processedCount;
    private Long startTime;
    private Long endTime;
    private String processingTime;
    private Integer totalCigarettes;
    private Integer successfulAllocations;
    private Map<String, Object> allocationResult;
    private Object allocationDetails;
    private String exception;
    
    /**
     * 转换为Map格式，用于HTTP响应
     */
    public Map<String, Object> toMap() {
        Map<String, Object> map = new HashMap<>();
        map.put("success", success);
        map.put("message", message);
        if (error != null) {
            map.put("error", error);
        }
        map.put("year", year);
        map.put("month", month);
        map.put("weekSeq", weekSeq);
        if (deletedExistingData != null) {
            map.put("deletedExistingData", deletedExistingData);
        }
        if (deletedRecords != null) {
            map.put("deletedRecords", deletedRecords);
        }
        if (processedCount != null) {
            map.put("processedCount", processedCount);
        }
        if (startTime != null) {
            map.put("startTime", startTime);
        }
        if (endTime != null) {
            map.put("endTime", endTime);
        }
        if (processingTime != null) {
            map.put("processingTime", processingTime);
        }
        if (totalCigarettes != null) {
            map.put("totalCigarettes", totalCigarettes);
        }
        if (successfulAllocations != null) {
            map.put("successfulAllocations", successfulAllocations);
        }
        if (allocationResult != null) {
            map.put("allocationResult", allocationResult);
        }
        if (allocationDetails != null) {
            map.put("allocationDetails", allocationDetails);
        }
        if (exception != null) {
            map.put("exception", exception);
        }
        return map;
    }
    
    public boolean isSuccess() {
        return Boolean.TRUE.equals(success);
    }
}

