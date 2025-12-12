package org.example.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

/**
 * 客户基础信息实体，表名固定 base_customer_info。
 */
@Data
@TableName("base_customer_info")
public class BaseCustomerInfo {

    @TableId(value = "ID", type = IdType.AUTO)
    private Integer id;

    @TableField("CUST_CODE")
    private String custCode;

    @TableField("CUST_ID")
    private String custId;

    @TableField("GRADE")
    private String grade;

    @TableField("ORDER_CYCLE")
    private String orderCycle;

    @TableField("CREDIT_LEVEL")
    private String creditLevel;

    @TableField("MARKET_TYPE")
    private String marketType;

    @TableField("CLASSIFICATION_CODE")
    private String classificationCode;

    @TableField("CUST_FORMAT")
    private String custFormat;

    @TableField("BUSINESS_DISTRICT_TYPE")
    private String businessDistrictType;

    @TableField("COMPANY_BRANCH")
    private String companyBranch;

    @TableField("COMPANY_DISTRICT")
    private String companyDistrict;

    @TableField("MARKET_DEPARTMENT")
    private String marketDepartment;

    @TableField("BUSINESS_STATUS")
    private String businessStatus;

    @TableField("IS_MUTUAL_AID_GROUP")
    private String isMutualAidGroup;

    @TableField("GROUP_NAME")
    private String groupName;

    @TableField("QUALITY_DATA_SHARE")
    private String qualityDataShare;

    @TableField("PREMIUM_CUSTOMER")
    private String premiumCustomer;

    @TableField("CRITICAL_TIME_AREA")
    private String criticalTimeArea;
}

