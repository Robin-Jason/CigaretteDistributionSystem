package org.example.shared.constants;

/**
 * 业务相关常量定义。
 * <p>
 * 统一管理项目中与业务逻辑相关的常量，包括业务关键字、业务规则等。
 * </p>
 *
 * @author Robin
 * @version 1.0
 * @since 2025-12-14
 */
public final class BusinessConstants {

    /**
     * 双周访销上浮关键字
     */
    public static final String BI_WEEKLY_VISIT_BOOST_PHRASE = "两周一访上浮100%";

    /**
     * 单周订单周期关键字
     */
    public static final String SINGLE_WEEK_KEYWORD = "单周";

    /**
     * 双周订单周期关键字
     */
    public static final String DOUBLE_WEEK_KEYWORD = "双周";

    /**
     * 区域与标签拼接分隔符
     */
    public static final String REGION_TAG_SEPARATOR = "+";

    /**
     * 档位序列编码分隔符（不同档位值之间）
     */
    public static final String GRADE_SEQUENCE_SEPARATOR = "+";

    /**
     * 档位序列编码连接符（数量×值）
     */
    public static final String GRADE_SEQUENCE_CONNECTOR = "×";

    private BusinessConstants() {
        // 工具类，禁止实例化
    }
}

