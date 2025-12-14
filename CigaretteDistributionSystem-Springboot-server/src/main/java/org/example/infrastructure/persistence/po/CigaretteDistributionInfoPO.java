package org.example.infrastructure.persistence.po;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 卷烟投放基础信息持久化对象（PO）
 * <p>
 * 对应表：cigarette_distribution_info（分区表）
 * 分区键：year/month/weekSeq
 * </p>
 *
 * @author Robin
 * @version 1.0
 * @since 2025-12-14
 */
@Data
@TableName("cigarette_distribution_info")
public class CigaretteDistributionInfoPO {

    @TableId(value = "ID", type = IdType.AUTO)
    private Integer id;

    @TableField("CIG_CODE")
    private String cigCode;

    @TableField("CIG_NAME")
    private String cigName;

    @TableField("DELIVERY_AREA")
    private String deliveryArea;

    @TableField("DELIVERY_METHOD")
    private String deliveryMethod;

    @TableField("DELIVERY_ETYPE")
    private String deliveryEtype;

    @TableField("SUPPLY_ATTRIBUTE")
    private String supplyAttribute;

    @TableField("TAG")
    private String tag;

    @TableField("TAG_FILTER_CONFIG")
    private String tagFilterConfig; 

    @TableField("URS")
    private BigDecimal urs;

    @TableField("ADV")
    private BigDecimal adv;

    @TableField("YEAR")
    private Integer year;

    @TableField("MONTH")
    private Integer month;

    @TableField("WEEK_SEQ")
    private Integer weekSeq;

    @TableField("BZ")
    private String bz;
}

