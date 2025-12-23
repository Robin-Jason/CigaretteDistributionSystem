package org.example.application.service.region;

import org.example.application.dto.allocation.GetAvailableRegionsRequestDto;
import org.example.application.dto.allocation.GetAvailableRegionsResponseDto;

/**
 * 获取可用投放区域列表服务接口
 * 
 * 功能：
 * 1. 根据投放类型和扩展类型解析出所有可能的区域
 * 2. 检查 region_customer_statistics 表中哪些区域已存在
 * 3. 对不存在的区域，追加构建区域客户数据
 * 4. 返回完整的区域列表和构建信息
 * 
 * @author System
 * @version 1.0
 * @since 2025-12-23
 */
public interface GetAvailableRegionsService {
    
    /**
     * 获取可用投放区域列表
     * 
     * @param request 请求DTO，包含年份、月份、周序号、投放类型、扩展类型
     * @return 响应DTO，包含区域列表、是否构建了新数据、构建的区域列表
     */
    GetAvailableRegionsResponseDto getAvailableRegions(GetAvailableRegionsRequestDto request);
}
