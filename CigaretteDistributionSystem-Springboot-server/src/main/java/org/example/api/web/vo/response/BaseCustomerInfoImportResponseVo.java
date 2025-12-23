package org.example.api.web.vo.response;

import lombok.Data;
import java.io.Serializable;
import java.util.Map;

/**
 * 客户基础信息表导入响应VO
 */
@Data
public class BaseCustomerInfoImportResponseVo implements Serializable {
    private static final long serialVersionUID = 1L;
    
    private Boolean success;
    private String message;
    private Integer insertedCount;
    private Integer processedCount;
    private String tableName;
    
    /**
     * 诚信互助小组编码映射信息
     * <p>
     * 结构：
     * - total: 小组总数
     * - updated: 是否已更新（true=本次导入已更新）
     * - notice: 提示信息
     * - codeMapping: Map<小组名称, 编码>，用于前端下拉选择或编码转换
     * - details: List<Map>，包含 groupName/groupCode/customerCount/sortOrder
     * </p>
     */
    private Map<String, Object> integrityGroupMapping;
}
