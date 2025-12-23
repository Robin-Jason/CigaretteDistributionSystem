package org.example.application.dto.prediction;

import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

/**
 * 新增投放区域分配记录 DTO
 *
 * @author Robin
 * @since 2025-12-22
 */
@Data
public class AddRegionAllocationDto {

    /**
     * 年份
     */
    private Integer year;

    /**
     * 月份
     */
    private Integer month;

    /**
     * 周序号
     */
    private Integer weekSeq;

    /**
     * 卷烟代码
     */
    private String cigCode;

    /**
     * 卷烟名称
     */
    private String cigName;

    /**
     * 主投放区域
     */
    private String primaryRegion;

    /**
     * 子投放区域（双扩展时使用）
     */
    private String secondaryRegion;

    /**
     * 30个档位的投放量值（D30-D1）
     */
    private List<BigDecimal> grades;

    /**
     * 备注
     */
    private String remark;
}
