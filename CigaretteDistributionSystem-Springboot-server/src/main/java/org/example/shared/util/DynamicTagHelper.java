package org.example.shared.util;

import lombok.extern.slf4j.Slf4j;
import org.example.infrastructure.persistence.po.BaseCustomerInfoPO;

import java.util.HashMap;
import java.util.Map;

/**
 * 动态标签辅助工具类
 * <p>
 * 业务规则：
 * 1. QUALITY_DATA_SHARE等固定标签字段继续使用固定字段，不存储在JSON字段中
 * 2. 其他动态标签使用JSON字段存储，使用中文键值对，如{"优质客户":"是"}
 * </p>
 * <p>
 * 此工具类提供从JSON字段读取动态标签的辅助方法。
 * </p>
 *
 * @author Robin
 * @version 1.0
 * @since 2025-12-20
 */
@Slf4j
public class DynamicTagHelper {

    /**
     * 从JSON字段中读取动态标签值
     * <p>
     * 注意：此方法只读取JSON字段中的动态标签，不读取固定字段（如QUALITY_DATA_SHARE）
     * </p>
     *
     * @param customer 客户信息PO
     * @param tagKey 标签键名（中文，如 "优质客户"）
     * @return 标签值，如果不存在则返回null
     */
    public static String getDynamicTagValue(BaseCustomerInfoPO customer, String tagKey) {
        if (customer == null || tagKey == null) {
            return null;
        }

        if (customer.getDynamicTags() != null) {
            Object value = customer.getDynamicTags().get(tagKey);
            if (value != null && !value.toString().trim().isEmpty()) {
                return value.toString();
            }
        }

        return null;
    }

    /**
     * 检查动态标签值是否存在且非空
     *
     * @param customer 客户信息PO
     * @param tagKey 标签键名（中文）
     * @return true表示标签值存在且非空
     */
    public static boolean hasDynamicTagValue(BaseCustomerInfoPO customer, String tagKey) {
        String value = getDynamicTagValue(customer, tagKey);
        return value != null && !value.trim().isEmpty();
    }

    /**
     * 获取所有动态标签的Map（只包含JSON字段中的标签，不包含固定字段）
     *
     * @param customer 客户信息PO
     * @return 标签Map，键为中文（如 "优质客户"）
     */
    public static Map<String, String> getAllDynamicTags(BaseCustomerInfoPO customer) {
        Map<String, String> tags = new HashMap<>();

        if (customer == null) {
            return tags;
        }

        if (customer.getDynamicTags() != null) {
            customer.getDynamicTags().forEach((key, value) -> {
                if (value != null && !value.toString().trim().isEmpty()) {
                    tags.put(key, value.toString());
                }
            });
        }

        return tags;
    }

    /**
     * 从固定字段读取标签值（用于QUALITY_DATA_SHARE等固定标签字段）
     * <p>
     * 注意：固定标签字段不存储在JSON字段中，需要单独读取
     * </p>
     *
     * @param customer 客户信息PO
     * @param fixedFieldName 固定字段名（如 "QUALITY_DATA_SHARE"）
     * @return 标签值，如果不存在则返回null
     */
    public static String getFixedTagValue(BaseCustomerInfoPO customer, String fixedFieldName) {
        if (customer == null || fixedFieldName == null) {
            return null;
        }

        switch (fixedFieldName) {
            case "QUALITY_DATA_SHARE":
                return customer.getQualityDataShare();
            default:
                log.warn("未知的固定标签字段: {}", fixedFieldName);
                return null;
        }
    }

    /**
     * 检查固定标签值是否存在且非空
     *
     * @param customer 客户信息PO
     * @param fixedFieldName 固定字段名
     * @return true表示标签值存在且非空
     */
    public static boolean hasFixedTagValue(BaseCustomerInfoPO customer, String fixedFieldName) {
        String value = getFixedTagValue(customer, fixedFieldName);
        return value != null && !value.trim().isEmpty();
    }
}

