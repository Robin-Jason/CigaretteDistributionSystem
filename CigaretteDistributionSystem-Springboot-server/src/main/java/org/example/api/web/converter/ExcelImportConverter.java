package org.example.api.web.converter;

import org.example.api.web.vo.request.DataImportRequestVo;
import org.example.api.web.vo.response.DataImportResponseVo;
import org.example.application.dto.importing.DataImportRequestDto;
import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;

/**
 * Excel导入转换器
 * MapStruct 会在编译时自动生成实现类
 */
@Mapper(
    componentModel = "spring",
    unmappedTargetPolicy = ReportingPolicy.IGNORE
)
public interface ExcelImportConverter {
    
    /**
     * VO 转 DTO - 数据导入请求
     */
    DataImportRequestDto toDto(DataImportRequestVo vo);
    
    /**
     * Map 转 VO - 数据导入响应
     * 注意：由于Service层返回的是Map，这里需要手动转换
     * 或者让Service层返回DTO，然后使用MapStruct转换
     */
    @SuppressWarnings("unchecked")
    default DataImportResponseVo toVo(java.util.Map<String, Object> resultMap) {
        if (resultMap == null) {
            return null;
        }
        
        DataImportResponseVo vo = new DataImportResponseVo();
        vo.setSuccess((Boolean) resultMap.get("success"));
        vo.setMessage((String) resultMap.get("message"));
        vo.setBaseCustomerInfoResult((java.util.Map<String, Object>) resultMap.get("baseCustomerInfoResult"));
        vo.setCigaretteDistributionInfoResult((java.util.Map<String, Object>) resultMap.get("cigaretteDistributionInfoResult"));
        vo.setBaseCustomerInfoNotice((String) resultMap.get("baseCustomerInfoNotice"));
        vo.setIntegrityGroupMapping((java.util.Map<String, Object>) resultMap.get("integrityGroupMapping"));
        return vo;
    }
}

