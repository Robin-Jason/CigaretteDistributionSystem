package org.example.api.web.converter;

import org.example.api.web.vo.request.GenerateDistributionPlanRequestVo;
import org.example.api.web.vo.response.GenerateDistributionPlanResponseVo;
import org.example.api.web.vo.response.TotalActualDeliveryResponseVo;
import org.example.application.dto.GenerateDistributionPlanRequestDto;
import org.example.application.dto.GenerateDistributionPlanResponseDto;
import org.example.application.dto.TotalActualDeliveryResponseDto;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

/**
 * 分配计算转换器
 * MapStruct 会在编译时自动生成实现类
 */
@Mapper(
    componentModel = "spring",
    unmappedTargetPolicy = ReportingPolicy.IGNORE
)
public interface DistributionCalculateConverter {
    
    /**
     * VO 转 DTO - 生成分配计划请求
     */
    GenerateDistributionPlanRequestDto toDto(GenerateDistributionPlanRequestVo vo);
    
    /**
     * DTO 转 VO - 生成分配计划响应
     * 忽略不需要暴露给客户端的字段
     */
    @Mapping(source = "error", target = "errorCode")
    GenerateDistributionPlanResponseVo toVo(GenerateDistributionPlanResponseDto dto);
    
    /**
     * DTO 转 VO - 总实际投放量响应
     */
    TotalActualDeliveryResponseVo toVo(TotalActualDeliveryResponseDto dto);
}

