package org.example.application.dto.prediction;

import lombok.Data;

/**
 * 删除整个卷烟分配记录 DTO
 *
 * @author Robin
 * @since 2025-12-22
 */
@Data
public class DeleteCigaretteDto {

    private Integer year;
    private Integer month;
    private Integer weekSeq;
    private String cigCode;
    private String cigName;
}
