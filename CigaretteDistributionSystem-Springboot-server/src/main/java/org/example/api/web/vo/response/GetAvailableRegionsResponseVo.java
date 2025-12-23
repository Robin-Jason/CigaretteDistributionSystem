package org.example.api.web.vo.response;

import lombok.Data;

import java.util.List;

/**
 * 获取可用投放区域列表响应VO
 * 
 * @author System
 * @version 1.0
 * @since 2025-12-23
 */
@Data
public class GetAvailableRegionsResponseVo {
    
    /**
     * 是否成功
     */
    private Boolean success;
    
    /**
     * 消息
     */
    private String message;
    
    /**
     * 可用区域列表
     */
    private List<String> availableRegions;
    
    /**
     * 是否构建了新数据
     */
    private Boolean hasBuiltNewData;
    
    /**
     * 构建的区域列表
     */
    private List<String> builtRegions;
}
