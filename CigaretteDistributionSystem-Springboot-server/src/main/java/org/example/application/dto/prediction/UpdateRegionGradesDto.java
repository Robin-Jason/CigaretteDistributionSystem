package org.example.application.dto.prediction;

import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

/**
 * 修改区域档位值 DTO
 *
 * @author Robin
 * @since 2025-12-22
 */
@Data
public class UpdateRegionGradesDto {

    private Integer year;
    private Integer month;
    private Integer weekSeq;
    private String cigCode;
    private String cigName;
    private String primaryRegion;
    private String secondaryRegion;
    
    /**
     * 30个档位值（D30-D1顺序）
     */
    private List<BigDecimal> grades;
    
    /**
     * 新的最高档位（可选，如果不传则使用 Info 表中的 HG）
     */
    private String newHg;
    
    /**
     * 新的最低档位（可选，如果不传则使用 Info 表中的 LG）
     */
    private String newLg;
    
    /**
     * 备注（可选）
     */
    private String remark;
}
