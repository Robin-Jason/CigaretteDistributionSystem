package org.example.application.dto.prediction;

import lombok.Data;
import java.math.BigDecimal;

/**
 * 查询卷烟分配数据记录DTO
 */
@Data
public class QueryCigaretteDistributionRecordDto {
    private Integer id;
    private String cigCode;
    private String cigName;
    private Integer year;
    private Integer month;
    private Integer weekSeq;
    private String deliveryArea;
    private String deliveryMethod;
    private String deliveryEtype;
    private BigDecimal advAmount;
    private BigDecimal actualDelivery;
    private String encodedExpression;
    private String decodedExpression;
    
    // 30个档位字段
    private BigDecimal d30;
    private BigDecimal d29;
    private BigDecimal d28;
    private BigDecimal d27;
    private BigDecimal d26;
    private BigDecimal d25;
    private BigDecimal d24;
    private BigDecimal d23;
    private BigDecimal d22;
    private BigDecimal d21;
    private BigDecimal d20;
    private BigDecimal d19;
    private BigDecimal d18;
    private BigDecimal d17;
    private BigDecimal d16;
    private BigDecimal d15;
    private BigDecimal d14;
    private BigDecimal d13;
    private BigDecimal d12;
    private BigDecimal d11;
    private BigDecimal d10;
    private BigDecimal d9;
    private BigDecimal d8;
    private BigDecimal d7;
    private BigDecimal d6;
    private BigDecimal d5;
    private BigDecimal d4;
    private BigDecimal d3;
    private BigDecimal d2;
    private BigDecimal d1;
}

