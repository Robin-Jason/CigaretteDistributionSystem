package org.example.api.web.vo.response;

import lombok.Data;
import java.io.Serializable;
import java.util.Map;

/**
 * 数据导入响应VO
 */
@Data
public class DataImportResponseVo implements Serializable {
    private static final long serialVersionUID = 1L;
    
    private Boolean success;
    private String message;
    private Map<String, Object> baseCustomerInfoResult;
    private Map<String, Object> cigaretteDistributionInfoResult;
    private String baseCustomerInfoNotice;
    
    /**
     * 诚信互助小组编码映射信息
     * <p>
     * 结构：
     * - total: 小组总数
     * - codeMapping: Map<小组名称, 编码>，用于前端下拉选择或编码转换
     * - details: List<Map>，包含 groupName/groupCode/customerCount/sortOrder
     * </p>
     */
    private Map<String, Object> integrityGroupMapping;
}

