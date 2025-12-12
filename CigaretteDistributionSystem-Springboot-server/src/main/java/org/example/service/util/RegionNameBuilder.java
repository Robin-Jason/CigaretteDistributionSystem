package org.example.service.util;

import org.example.service.delivery.DeliveryExtensionType;

import java.util.List;

/**
 * 区域名称构建器
 * <p>
 * 负责根据扩展类型和值构建区域名称。
 * 主要用于将投放扩展类型（如区县公司、市场类型等）组合成完整的区域名称字符串。
 * </p>
 *
 * @author Robin
 * @version 1.0
 * @since 2025-12-11
 */
public final class RegionNameBuilder {

    private RegionNameBuilder() {
        // 工具类，禁止实例化
    }

    /**
     * 构建区域名称。
     * <p>
     * 根据主扩展类型和值，以及子扩展值列表，构建完整的区域名称。
     * 如果主扩展类型是区县公司，则提取前2个字符作为前缀。
     * </p>
     *
     * @param primaryType   主扩展类型
     * @param primaryValue  主扩展值（如："江汉区"）
     * @param subExtensions 子扩展值列表（如：["城区", "大型"]）
     * @return 区域名称（如："江汉（城区，大型）"）
     * @example
     * <pre>
     *     String name = RegionNameBuilder.buildRegionName(
     *         DeliveryExtensionType.COUNTY, "江汉区", Arrays.asList("城区", "大型")
     *     );
     *     // 返回: "江汉（城区，大型）"
     *     String name2 = RegionNameBuilder.buildRegionName(
     *         DeliveryExtensionType.MARKET_TYPE, "城区", Collections.emptyList()
     *     );
     *     // 返回: "城区"
     * </pre>
     */
    public static String buildRegionName(DeliveryExtensionType primaryType, String primaryValue, List<String> subExtensions) {
        String baseName = primaryType == DeliveryExtensionType.COUNTY
                ? extractCountyPrefix(primaryValue)
                : primaryValue;
        if (subExtensions == null || subExtensions.isEmpty()) {
            return baseName;
        }
        String subPart = String.join("，", subExtensions);
        return baseName + "（" + subPart + "）";
    }

    /**
     * 提取区县名称前缀。
     * <p>
     * 提取区县名称的前2个字符作为前缀（用于区域名称构建）。
     * </p>
     *
     * @param county 区县名称（如："江汉区"、"武昌区"）
     * @return 区县前缀（如："江汉"、"武昌"），如果长度不足2则返回原字符串
     * @example
     * <pre>
     *     String prefix = RegionNameBuilder.extractCountyPrefix("江汉区");
     *     // 返回: "江汉"
     *     String prefix2 = RegionNameBuilder.extractCountyPrefix("武昌区");
     *     // 返回: "武昌"
     *     String prefix3 = RegionNameBuilder.extractCountyPrefix("区");
     *     // 返回: "区"（长度不足2）
     * </pre>
     */
    public static String extractCountyPrefix(String county) {
        if (county == null || county.trim().isEmpty()) {
            return "";
        }
        String trimmed = county.trim();
        if (trimmed.length() >= 2) {
            return trimmed.substring(0, 2);
        }
        return trimmed;
    }
}

