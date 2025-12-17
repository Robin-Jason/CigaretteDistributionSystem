package org.example.infrastructure.config.encoding;

import javax.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.example.domain.model.valueobject.DeliveryExtensionType;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.util.*;

/**
 * 编码规则内存仓库
 *
 * 作用：
 * - 启动时加载 {@link EncodingRuleProperties}，构建投放方式/扩展类型/区域编码与别名的双向映射。
 * - 对外提供 label->code 及 code->label 查询，避免反复解析配置文件。
 *
 * 生命周期：
 * - @PostConstruct 自动初始化，支持 reload() 动态刷新。
 *
 * @author Robin
 * @version 1.0
 * @since 2025-11-30
 */
@Slf4j
@Component
public class EncodingRuleRepository {

    private final EncodingRuleProperties properties;

    private Map<String, String> deliveryMethodCodes = Collections.emptyMap();
    private Map<String, String> reverseDeliveryMethodCodes = Collections.emptyMap();

    private Map<String, String> extensionTypeCodes = Collections.emptyMap();
    private Map<String, String> reverseExtensionTypeCodes = Collections.emptyMap();

    private final Map<DeliveryExtensionType, Map<String, String>> regionCodeMaps = new EnumMap<>(DeliveryExtensionType.class);
    private final Map<DeliveryExtensionType, Map<String, String>> reverseRegionCodeMaps = new EnumMap<>(DeliveryExtensionType.class);

    public EncodingRuleRepository(EncodingRuleProperties properties) {
        this.properties = properties;
    }

    @PostConstruct
    public void initialize() {
        reload();
    }

    /**
     * 重新加载全部编码映射。
     *
     * <p>流程：</p>
     * <ol>
     *   <li>构建投放方式编码正/反向表；</li>
     *   <li>构建扩展投放类型编码正/反向表；</li>
     *   <li>构建区域编码及别名的正/反向表。</li>
     * </ol>
     *
     * @since 1.0
     */
    public synchronized void reload() {
        deliveryMethodCodes = buildDeliveryMethodMap();
        reverseDeliveryMethodCodes = buildReverseMap(deliveryMethodCodes);

        extensionTypeCodes = buildExtensionTypeMap();
        reverseExtensionTypeCodes = buildReverseMap(extensionTypeCodes);

        buildRegionMaps();

        log.info("Encoding rule repository initialized: {} delivery methods, {} extension types, {} region groups",
                deliveryMethodCodes.size(), extensionTypeCodes.size(), regionCodeMaps.size());
    }

    /**
     * 根据投放方式标签查编码，支持宽松匹配。
     *
     * <p>规则：优先按原样精确匹配，未命中则去掉首尾/中间空白后匹配。</p>
     *
     * @param label 投放方式名称/别名（如“按档位统一投放”）
     * @return 对应编码，未命中返回 null
     *
     * @example label="按档位  统一投放" -> 编码 "A1"
     */
    public String findDeliveryMethodCode(String label) {
        if (!StringUtils.hasText(label)) {
            return null;
        }
        String direct = deliveryMethodCodes.get(label.trim());
        if (direct != null) {
            return direct;
        }
        return deliveryMethodCodes.get(normalize(label));
    }

    /**
     * 根据投放方式编码查标签（返回首个注册的标签）。
     *
     * @param code 投放方式编码（如 "A1"）
     * @return 标准标签或 null
     */
    public String findDeliveryMethodLabel(String code) {
        return reverseDeliveryMethodCodes.get(code);
    }

    /**
     * 根据扩展投放类型标签查编码，支持空白归一化匹配。
     *
     * @param label 扩展投放类型名称/别名（如“档位+区县”）
     * @return 对应编码，未命中返回 null
     *
     * @example label="档位 + 区县" -> 编码 "B1"
     */
    public String findExtensionTypeCode(String label) {
        if (!StringUtils.hasText(label)) {
            return null;
        }
        String trimmed = label.trim();
        // 1) 直接匹配原始/归一化标签
        String direct = extensionTypeCodes.get(trimmed);
        if (direct != null) {
            return direct;
        }
        String normalized = normalize(trimmed);
        direct = extensionTypeCodes.get(normalized);
        if (direct != null) {
            return direct;
        }

        // 2) 兼容别名：通过 DeliveryExtensionType 解析出规范显示名再查一次
        //    例如：label="档位+区县+市场类型" -> DeliveryExtensionType.COUNTY -> 使用显示名 "区县" 查询编码
        java.util.Optional<org.example.domain.model.valueobject.DeliveryExtensionType> typeOpt =
                org.example.domain.model.valueobject.DeliveryExtensionType.from(label);
        if (typeOpt.isPresent()) {
            String displayName = typeOpt.get().getDisplayName();
            String byDisplay = extensionTypeCodes.get(displayName);
            if (byDisplay != null) {
                return byDisplay;
            }
            return extensionTypeCodes.get(normalize(displayName));
        }

        return null;
    }

    /**
     * 根据扩展投放类型编码查标签（返回首个注册的标签）。
     *
     * @param code 扩展投放类型编码
     * @return 标准标签或 null
     */
    public String findExtensionTypeLabel(String code) {
        return reverseExtensionTypeCodes.get(code);
    }

    /**
     * 获取指定扩展类型下的区域别名->编码映射。
     *
     * @param type 扩展投放类型（如 DeliveryExtensionType.COUNTY）
     * @return 别名（含规范标签与变体）到编码的不可变 Map
     */
    public Map<String, String> getRegionCodeMap(DeliveryExtensionType type) {
        return regionCodeMaps.getOrDefault(type, Collections.emptyMap());
    }

    /**
     * 获取指定扩展类型下的区域编码->规范标签映射。
     *
     * @param type 扩展投放类型
     * @return 编码到规范标签的不可变 Map
     */
    public Map<String, String> getReverseRegionCodeMap(DeliveryExtensionType type) {
        return reverseRegionCodeMaps.getOrDefault(type, Collections.emptyMap());
    }

    /**
     * 构建投放方式别名->编码映射。
     *
     * @return 可变的别名映射（后续会封装为只读）
     */
    private Map<String, String> buildDeliveryMethodMap() {
        Map<String, String> map = new LinkedHashMap<>();
        if (CollectionUtils.isEmpty(properties.getDeliveryMethods())) {
            return map;
        }

        for (EncodingRuleProperties.DeliveryMethodRule rule : properties.getDeliveryMethods()) {
            if (!StringUtils.hasText(rule.getCode())) {
                continue;
            }
            for (String label : rule.getLabels()) {
                registerLabel(map, label, rule.getCode());
            }
        }
        return map;
    }

    /**
     * 构建扩展投放类型别名->编码映射。
     *
     * @return 可变的别名映射（后续会封装为只读）
     */
    private Map<String, String> buildExtensionTypeMap() {
        Map<String, String> map = new LinkedHashMap<>();
        if (CollectionUtils.isEmpty(properties.getExtensionTypes())) {
            return map;
        }

        for (EncodingRuleProperties.ExtensionTypeRule rule : properties.getExtensionTypes()) {
            if (!StringUtils.hasText(rule.getCode())) {
                continue;
            }
            for (String label : rule.getLabels()) {
                registerLabel(map, label, rule.getCode());
            }
        }
        return map;
    }

    /**
     * 构建区域别名/编码的正反向映射（按扩展类型分组）。
     */
    private void buildRegionMaps() {
        regionCodeMaps.clear();
        reverseRegionCodeMaps.clear();

        if (CollectionUtils.isEmpty(properties.getRegionTypes())) {
            return;
        }

        for (EncodingRuleProperties.RegionTypeRule regionTypeRule : properties.getRegionTypes()) {
            DeliveryExtensionType type = regionTypeRule.getType();
            if (type == null) {
                continue;
            }
            Map<String, String> aliasMap = new LinkedHashMap<>();
            Map<String, String> reverseMap = new LinkedHashMap<>();

            for (EncodingRuleProperties.RegionEntry entry : regionTypeRule.getEntries()) {
                if (!StringUtils.hasText(entry.getCode())) {
                    continue;
                }
                String canonicalLabel = null;
                for (String label : entry.getLabels()) {
                    String normalized = normalizeLabel(label);
                    if (normalized == null) {
                        continue;
                    }
                    aliasMap.put(normalized, entry.getCode());
                    if (canonicalLabel == null) {
                        canonicalLabel = normalized;
                    }
                }
                if (canonicalLabel == null) {
                    canonicalLabel = entry.getCode();
                }
                reverseMap.put(entry.getCode(), canonicalLabel);
            }

            regionCodeMaps.put(type, Collections.unmodifiableMap(aliasMap));
            reverseRegionCodeMaps.put(type, Collections.unmodifiableMap(reverseMap));
        }
    }

    /**
     * 注册单个标签的多种归一化写法到 map 中。
     */
    private void registerLabel(Map<String, String> map, String label, String code) {
        String normalized = normalizeLabel(label);
        if (normalized != null) {
            map.put(normalized, code);
            String compact = normalize(normalized);
            if (compact != null && !compact.equals(normalized)) {
                map.put(compact, code);
            }
        }
    }

    /**
     * 根据正向 map 生成编码->标签的反向只读映射。
     */
    private Map<String, String> buildReverseMap(Map<String, String> source) {
        if (source == null || source.isEmpty()) {
            return Collections.emptyMap();
        }
        Map<String, String> reversed = new LinkedHashMap<>();
        source.forEach((key, value) -> reversed.putIfAbsent(value, key));
        return Collections.unmodifiableMap(reversed);
    }

    /**
     * 去除首尾空白，保留原大小写。
     */
    private String normalizeLabel(String label) {
        if (!StringUtils.hasText(label)) {
            return null;
        }
        return label.trim();
    }

    /**
     * 去除全部空白（含中间空格），便于宽松匹配。
     */
    private String normalize(String label) {
        if (!StringUtils.hasText(label)) {
            return null;
        }
        return label.replaceAll("\\s+", "");
    }
}

