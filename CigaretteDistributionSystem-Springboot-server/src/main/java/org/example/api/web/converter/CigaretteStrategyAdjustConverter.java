package org.example.api.web.converter;

import org.example.api.web.vo.request.AdjustCigaretteStrategyRequestVo;
import org.example.api.web.vo.response.AdjustCigaretteStrategyResponseVo;
import org.example.application.dto.allocation.AdjustCigaretteStrategyRequestDto;
import org.example.application.dto.allocation.AdjustCigaretteStrategyResponseDto;
import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;

/**
 * 卷烟投放策略调整 VO/DTO 转换器
 *
 * @author Robin
 * @since 2025-12-22
 */
@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface CigaretteStrategyAdjustConverter {

    AdjustCigaretteStrategyRequestDto toDto(AdjustCigaretteStrategyRequestVo vo);

    AdjustCigaretteStrategyResponseVo toVo(AdjustCigaretteStrategyResponseDto dto);
}
