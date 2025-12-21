package org.example.infrastructure.persistence.po;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import org.example.infrastructure.persistence.typehandler.JsonMapTypeHandler;

import java.util.Map;

/**
 * 客户基础信息持久化对象（PO）
 * <p>
 * 对应表：base_customer_info
 * </p>
 *
 * @author Robin
 * @version 1.0
 * @since 2025-12-14
 */
@Data
@TableName("base_customer_info")
public class BaseCustomerInfoPO {

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

    /**
     * 优质数据共享客户
     * <p>
     * 业务规则：此字段为固定标签字段，继续使用固定字段存储和查询，不写入JSON字段。
     * 其他动态标签使用 {@link #dynamicTags} JSON字段存储，使用中文键值对。
     * </p>
     */
    @TableField("QUALITY_DATA_SHARE")
    private String qualityDataShare;

    /**
     * 动态标签（JSON格式）
     * <p>
     * 业务规则：
     * 1. QUALITY_DATA_SHARE等固定标签字段继续使用固定字段，不存储在此JSON字段中
     * 2. 其他动态标签使用中文键值对格式，例如：
     * {
     *   "优质客户": "是",
     *   "重点区域": "重点区域A"
     * }
     * </p>
     */
    @TableField(value = "DYNAMIC_TAGS", typeHandler = JsonMapTypeHandler.class)
    private Map<String, Object> dynamicTags;
}

