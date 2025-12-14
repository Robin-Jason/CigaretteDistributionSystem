package org.example.shared.constants;

/**
 * 数据库表名常量定义。
 * <p>
 * 统一管理项目中使用的数据库表名，避免硬编码字符串。
 * </p>
 *
 * @author Robin
 * @version 1.0
 * @since 2025-12-14
 */
public final class TableConstants {

    /**
     * 基础客户信息表名
     */
    public static final String BASE_CUSTOMER_INFO = "base_customer_info";

    /**
     * 卷烟分配信息表名（分区表）
     */
    public static final String CIGARETTE_DISTRIBUTION_INFO = "cigarette_distribution_info";

    /**
     * 卷烟分配预测表名（分区表）
     */
    public static final String CIGARETTE_DISTRIBUTION_PREDICTION = "cigarette_distribution_prediction";

    /**
     * 卷烟分配预测价格表名（分区表）
     */
    public static final String CIGARETTE_DISTRIBUTION_PREDICTION_PRICE = "cigarette_distribution_prediction_price";

    /**
     * 区域客户统计表名（分区表）
     */
    public static final String REGION_CUSTOMER_STATISTICS = "region_customer_statistics";

    /**
     * 诚信组映射表名
     */
    public static final String INTEGRITY_GROUP_MAPPING = "integrity_group_code_mapping";

    /**
     * 临时客户过滤表名前缀
     */
    public static final String TEMP_CUSTOMER_FILTER_PREFIX = "temp_customer_filter_";

    /**
     * 基础客户信息表必填字段
     */
    public static final String MANDATORY_CUSTOMER_COLUMN = "CUST_CODE";

    /**
     * 默认动态列类型
     */
    public static final String DEFAULT_DYNAMIC_COLUMN_TYPE = "varchar(255) DEFAULT NULL";

    private TableConstants() {
        // 工具类，禁止实例化
    }
}

