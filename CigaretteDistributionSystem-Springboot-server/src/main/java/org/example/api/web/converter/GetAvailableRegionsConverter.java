package org.example.api.web.converter;

import org.example.api.web.vo.request.GetAvailableRegionsRequestVo;
import org.example.api.web.vo.response.GetAvailableRegionsResponseVo;
import org.example.application.dto.allocation.GetAvailableRegionsRequestDto;
import org.example.application.dto.allocation.GetAvailableRegionsResponseDto;
import org.mapstruct.Mapper;

/**
 * 获取可用投放区域列表转换器
 * 
 * @author System
 * @version 1.0
 * @since 2025-12-23
 */
@Mapper(componentModel = "spring")
public interface GetAvailableRegionsConverter {
    
    /**
     * VO 转 DTO
     */
    GetAvailableRegionsRequestDto toDto(GetAvailableRegionsRequestVo vo);
    
    /**
     * DTO 转 VO
     */
    GetAvailableRegionsResponseVo toVo(GetAvailableRegionsResponseDto dto);
}
