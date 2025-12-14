package org.example.domain.model.valueobject;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * 规范化投放方式定义与约束
 *
 * @author Robin
 * @version 1.0
 * @since 2025-12-10
 */
public enum DeliveryMethodType {

    GRADE("按档位投放", false, 0, 0, true,
            Arrays.asList("按档位投放", "按档位统一投放")),

    GRADE_EXTEND("按档位扩展投放", true, 1, 2, true,
            Arrays.asList("按档位扩展投放", "按档位拓展投放")),

    PRICE_SEGMENT("按价位段自选投放", true, 0, 2, true,
            Collections.singletonList("按价位段自选投放")),

    ON_DEMAND("按需投放", false, 0, 2, false,
            Collections.singletonList("按需投放")),

    POINT_SELECTION("选点投放", false, 0, 2, false,
            Arrays.asList("选点投放", "点位投放"));

    private static final Map<String, DeliveryMethodType> LOOKUP;

    static {
        Map<String, DeliveryMethodType> map = new HashMap<>();
        for (DeliveryMethodType type : values()) {
            for (String alias : type.aliases) {
                map.put(normalizeToken(alias), type);
            }
        }
        LOOKUP = Collections.unmodifiableMap(map);
    }

    private final String displayName;
    private final boolean allowsExtensions;
    private final int minExtensions;
    private final int maxExtensions;
    private final boolean implemented;
    private final Set<String> aliases;

    DeliveryMethodType(String displayName,
                       boolean allowsExtensions,
                       int minExtensions,
                       int maxExtensions,
                       boolean implemented,
                       Iterable<String> aliasIterable) {
        this.displayName = displayName;
        this.allowsExtensions = allowsExtensions;
        this.minExtensions = minExtensions;
        this.maxExtensions = maxExtensions;
        this.implemented = implemented;
        Set<String> aliasSet = new HashSet<>();
        for (String alias : aliasIterable) {
            aliasSet.add(alias);
        }
        this.aliases = Collections.unmodifiableSet(aliasSet);
    }

    /**
     * @return 显示名称
     */
    public String getDisplayName() {
        return displayName;
    }

    public boolean isImplemented() {
        return implemented;
    }

    public boolean allowsExtensions() {
        return allowsExtensions;
    }

    public int getMaxExtensions() {
        return maxExtensions;
    }

    public int getMinExtensions() {
        return minExtensions;
    }

    /**
     * 通过原始字符串解析投放方式（别名匹配，忽略空白）
     *
     * @param raw 原始值
     * @return 投放方式
     */
    public static Optional<DeliveryMethodType> from(String raw) {
        if (raw == null) {
            return Optional.empty();
        }
        String key = normalizeToken(raw);
        return Optional.ofNullable(LOOKUP.get(key));
    }

    /**
     * 校验扩展类型数量是否满足该投放方式约束
     *
     * @param extensionCount 扩展类型数量
     */
    public void validateExtensions(int extensionCount) {
        if (!allowsExtensions && extensionCount > 0) {
            throw new IllegalArgumentException(
                    String.format("%s 不允许扩展投放类型，但收到 %d 个", displayName, extensionCount));
        }
        if (allowsExtensions) {
            if (extensionCount < minExtensions) {
                throw new IllegalArgumentException(
                        String.format("%s 至少需要 %d 个扩展投放类型，当前为 %d 个",
                                displayName, minExtensions, extensionCount));
            }
            if (extensionCount > maxExtensions) {
                throw new IllegalArgumentException(
                        String.format("%s 最多支持 %d 个扩展投放类型，当前为 %d 个",
                                displayName, maxExtensions, extensionCount));
            }
        }
    }

    private static String normalizeToken(String raw) {
        return raw == null ? "" : raw.replaceAll("\\s+", "")
                .toUpperCase(Locale.ROOT);
    }
}

