package org.example.application.dto.prediction;

import lombok.Data;

/**
 * 删除特定区域分配记录 DTO
 *
 * @author Robin
 * @since 2025-12-22
 */
@Data
public class DeleteRegionAllocationDto {

    private Integer year;
    private Integer month;
    private Integer weekSeq;
    private String cigCode;
    private String cigName;
    private String primaryRegion;
    private String secondaryRegion;
}
