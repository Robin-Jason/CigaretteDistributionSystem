package org.example.application.dto;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import java.math.BigDecimal;
import java.util.List;

/**
 * 更新预测分区表档位值请求 DTO。
 */
@Data
public class UpdatePredictionGradesRequestDto {

    @NotNull
    private Integer year;

    @NotNull
    private Integer month;

    @NotNull
    private Integer weekSeq;

    @NotBlank
    private String cigCode;

    @NotBlank
    private String cigName;

    @NotBlank
    private String deliveryArea;

    @NotNull
    @Size(min = 30, max = 30, message = "grades必须包含30个档位值(D30-D1)")
    private List<BigDecimal> grades;
}

