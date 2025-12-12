package org.example.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import javax.persistence.*;
import java.math.BigDecimal;

/**
 * 卷烟分配预测数据模型（分区表映射）
 * <p>
 * 对应表：cigarette_distribution_prediction（分区表，按 YEAR/MONTH/WEEK_SEQ），无需动态表名。
 * YEAR/MONTH/WEEK_SEQ 为分区键，所有读写必须带时间参数以命中正确分区。
 * 使用 MyBatis-Plus 注解保证列名与数据库一致；@Table 仅用于兼容 JPA。
 */
@Data
@Entity
@Table(name = "cigarette_distribution_prediction_dynamic") // 占位表名（JPA兼容），实际表名见 @TableName
@TableName("cigarette_distribution_prediction")
public class CigaretteDistributionPredictionData {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @TableId(value = "ID", type = IdType.AUTO)
    private Integer id;
    
    @Column(name = "CIG_CODE")
    @TableField("CIG_CODE")
    private String cigCode;
    
    @Column(name = "CIG_NAME")
    @TableField("CIG_NAME")
    private String cigName;
    
    @Column(name = "YEAR")
    @TableField("YEAR")
    private Integer year;
    
    @Column(name = "MONTH")
    @TableField("MONTH")
    private Integer month;
    
    @Column(name = "WEEK_SEQ")
    @TableField("WEEK_SEQ")
    private Integer weekSeq;
    
    @Column(name = "DELIVERY_AREA")
    @TableField("DELIVERY_AREA")
    private String deliveryArea;
    
    @Column(name = "DELIVERY_METHOD")
    @TableField("DELIVERY_METHOD")
    private String deliveryMethod;
    
    @Column(name = "DELIVERY_ETYPE")
    @TableField("DELIVERY_ETYPE")
    private String deliveryEtype;
    
    @Column(name = "TAG")
    @TableField("TAG")
    private String tag;

    @Column(name = "TAG_FILTER_CONFIG")
    @TableField("TAG_FILTER_CONFIG")
    private String tagFilterConfig;
    
    // 30个档位字段（D30-D1）
    @Column(name = "D30")
    @TableField("D30")
    private BigDecimal d30;
    @Column(name = "D29")
    @TableField("D29")
    private BigDecimal d29;
    @Column(name = "D28")
    @TableField("D28")
    private BigDecimal d28;
    @Column(name = "D27")
    @TableField("D27")
    private BigDecimal d27;
    @Column(name = "D26")
    @TableField("D26")
    private BigDecimal d26;
    @Column(name = "D25")
    @TableField("D25")
    private BigDecimal d25;
    @Column(name = "D24")
    @TableField("D24")
    private BigDecimal d24;
    @Column(name = "D23")
    @TableField("D23")
    private BigDecimal d23;
    @Column(name = "D22")
    @TableField("D22")
    private BigDecimal d22;
    @Column(name = "D21")
    @TableField("D21")
    private BigDecimal d21;
    @Column(name = "D20")
    @TableField("D20")
    private BigDecimal d20;
    @Column(name = "D19")
    @TableField("D19")
    private BigDecimal d19;
    @Column(name = "D18")
    @TableField("D18")
    private BigDecimal d18;
    @Column(name = "D17")
    @TableField("D17")
    private BigDecimal d17;
    @Column(name = "D16")
    @TableField("D16")
    private BigDecimal d16;
    @Column(name = "D15")
    @TableField("D15")
    private BigDecimal d15;
    @Column(name = "D14")
    @TableField("D14")
    private BigDecimal d14;
    @Column(name = "D13")
    @TableField("D13")
    private BigDecimal d13;
    @Column(name = "D12")
    @TableField("D12")
    private BigDecimal d12;
    @Column(name = "D11")
    @TableField("D11")
    private BigDecimal d11;
    @Column(name = "D10")
    @TableField("D10")
    private BigDecimal d10;
    @Column(name = "D9")
    @TableField("D9")
    private BigDecimal d9;
    @Column(name = "D8")
    @TableField("D8")
    private BigDecimal d8;
    @Column(name = "D7")
    @TableField("D7")
    private BigDecimal d7;
    @Column(name = "D6")
    @TableField("D6")
    private BigDecimal d6;
    @Column(name = "D5")
    @TableField("D5")
    private BigDecimal d5;
    @Column(name = "D4")
    @TableField("D4")
    private BigDecimal d4;
    @Column(name = "D3")
    @TableField("D3")
    private BigDecimal d3;
    @Column(name = "D2")
    @TableField("D2")
    private BigDecimal d2;
    @Column(name = "D1")
    @TableField("D1")
    private BigDecimal d1;
    
    @Column(name = "bz")
    @TableField("BZ")
    private String bz; // 备注
    
    @Column(name = "ACTUAL_DELIVERY")
    @TableField("ACTUAL_DELIVERY")
    private BigDecimal actualDelivery; // 实际投放量
    
    @Column(name = "DEPLOYINFO_CODE")
    @TableField("DEPLOYINFO_CODE")
    private String deployinfoCode; // 投放信息编码
}
