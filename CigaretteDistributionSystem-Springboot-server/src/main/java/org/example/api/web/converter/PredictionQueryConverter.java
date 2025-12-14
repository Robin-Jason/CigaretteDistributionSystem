package org.example.api.web.converter;

import org.example.api.web.vo.response.PredictionQueryResponseVo;
import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;

import java.util.List;
import java.util.Map;

/**
 * 预测查询转换器
 * MapStruct 会在编译时自动生成实现类
 */
@Mapper(
    componentModel = "spring",
    unmappedTargetPolicy = ReportingPolicy.IGNORE
)
public interface PredictionQueryConverter {
    
    /**
     * List<Map> 转 VO - 预测查询响应
     * 由于Service层返回的是List<Map<String, Object>>，这里需要手动转换
     */
    default PredictionQueryResponseVo toVo(List<Map<String, Object>> dataList) {
        if (dataList == null) {
            return null;
        }
        
        PredictionQueryResponseVo vo = new PredictionQueryResponseVo();
        vo.setData(dataList);
        vo.setTotal(dataList.size());
        return vo;
    }
}

