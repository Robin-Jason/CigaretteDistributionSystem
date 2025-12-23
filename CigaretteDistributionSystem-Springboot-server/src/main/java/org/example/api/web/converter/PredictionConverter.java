package org.example.api.web.converter;

import org.example.api.web.vo.request.AddRegionAllocationRequestVo;
import org.example.api.web.vo.request.DeleteCigaretteRequestVo;
import org.example.api.web.vo.request.DeleteRegionAllocationRequestVo;
import org.example.api.web.vo.request.UpdateRegionGradesRequestVo;
import org.example.api.web.vo.response.PredictionQueryResponseVo;
import org.example.application.dto.prediction.AddRegionAllocationDto;
import org.example.application.dto.prediction.DeleteCigaretteDto;
import org.example.application.dto.prediction.DeleteRegionAllocationDto;
import org.example.application.dto.prediction.UpdateRegionGradesDto;
import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;

import java.util.List;
import java.util.Map;

/**
 * 预测分配数据转换器
 * <p>
 * 提供 VO ↔ DTO 的转换，包括查询响应和增删改请求。
 * MapStruct 会在编译时自动生成实现类。
 * </p>
 *
 * @author Robin
 * @since 2025-12-22
 */
@Mapper(
    componentModel = "spring",
    unmappedTargetPolicy = ReportingPolicy.IGNORE
)
public interface PredictionConverter {

    // ==================== 查询响应转换 ====================

    /**
     * List<Map> 转 VO - 预测查询响应
     */
    default PredictionQueryResponseVo toQueryResponseVo(List<Map<String, Object>> dataList) {
        if (dataList == null) {
            return null;
        }
        PredictionQueryResponseVo vo = new PredictionQueryResponseVo();
        vo.setData(dataList);
        vo.setTotal(dataList.size());
        return vo;
    }

    // ==================== 新增请求转换 ====================

    /**
     * 新增区域分配请求 VO → DTO
     */
    AddRegionAllocationDto toAddDto(AddRegionAllocationRequestVo request);

    // ==================== 删除请求转换 ====================

    /**
     * 删除区域分配请求 VO → DTO
     */
    DeleteRegionAllocationDto toDeleteRegionDto(DeleteRegionAllocationRequestVo request);

    /**
     * 删除卷烟请求 VO → DTO
     */
    DeleteCigaretteDto toDeleteCigaretteDto(DeleteCigaretteRequestVo request);

    // ==================== 修改请求转换 ====================

    /**
     * 修改区域档位请求 VO → DTO
     */
    UpdateRegionGradesDto toUpdateDto(UpdateRegionGradesRequestVo request);
}
