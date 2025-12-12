package org.example.service.delivery;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 扩展投放类型定义及别名映射
 *
 * @author Robin
 * @version 1.0
 * @since 2025-12-10
 */
public enum DeliveryExtensionType {
    COUNTY("区县", aliasSet("区县", "档位+区县", "区县区域", "档位+区县+市场类型")),
    MARKET_TYPE("市场类型", aliasSet("市场类型", "档位+市场类型")),
    URBAN_RURAL_CODE("城乡分类代码", aliasSet("城乡分类代码", "档位+城乡分类代码")),
    BUSINESS_FORMAT("业态", aliasSet("业态", "经营业态", "业态类型", "档位+业态")),
    MARKET_DEPARTMENT("市场部", aliasSet("市场部", "市场部门", "档位+市场部")),
    BUSINESS_DISTRICT("商圈类型", aliasSet("商圈类型", "商圈", "商圈类别", "档位+商圈类型")),
    INTEGRITY_GROUP("诚信自律小组", aliasSet("诚信自律小组", "诚信互助小组", "档位+诚信自律小组")),
    CREDIT_LEVEL("信用等级", aliasSet("信用等级", "客户信用等级", "档位+信用等级")),
    UNKNOWN("UNKNOWN", aliasSet("UNKNOWN"));

    private static final Map<String, DeliveryExtensionType> LOOKUP;

    static {
        Map<String, DeliveryExtensionType> map = new HashMap<>();
        for (DeliveryExtensionType type : values()) {
            for (String alias : type.aliases) {
                map.put(normalize(alias), type);
            }
        }
        LOOKUP = Collections.unmodifiableMap(map);
    }

    private final String displayName;
    private final Set<String> aliases;

    DeliveryExtensionType(String displayName, Set<String> aliases) {
        this.displayName = displayName;
        this.aliases = aliases;
    }

    /**
     * @return 显示名称
     */
    public String getDisplayName() {
        return displayName;
    }

    /**
     * 通过原始字符串解析扩展类型（别名匹配，忽略空白）
     *
     * @param raw 原始值
     * @return 解析结果（UNKNOWN 时返回 empty）
     */
    public static Optional<DeliveryExtensionType> from(String raw) {
        if (raw == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(LOOKUP.get(normalize(raw)))
                .filter(type -> type != UNKNOWN);
    }

    private static Set<String> aliasSet(String... values) {
        return Collections.unmodifiableSet(Arrays.stream(values)
                .map(DeliveryExtensionType::normalize)
                .collect(Collectors.toSet()));
    }

    private static String normalize(String raw) {
        return raw == null ? "" : raw.replaceAll("\\s+", "")
                .toUpperCase(Locale.ROOT);
    }
}

