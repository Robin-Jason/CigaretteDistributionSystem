package org.example.config.encoding;

import lombok.Data;
import org.example.service.delivery.DeliveryExtensionType;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * 编码规则配置属性
 *
 * 作用：
 * - 从 encoding-rules.yml（或外部覆盖配置）绑定投放方式、扩展类型、区域类型的编码与别名。
 * - 为编码解析/回写提供规则元数据。
 *
 * 配置结构：
 * - deliveryMethods：投放方式编码 + 多个别名。
 * - extensionTypes：扩展投放类型编码 + 多个别名（首个可视为主标签）。
 * - regionTypes：按扩展类型组织的区域编码 + 多个别名。
 *
 * @author Robin
 * @version 1.0
 * @since 2025-11-30
 */
@Data
@Component
@ConfigurationProperties(prefix = "distribution.encoding")
public class EncodingRuleProperties {

    private List<DeliveryMethodRule> deliveryMethods = new ArrayList<>();
    private List<ExtensionTypeRule> extensionTypes = new ArrayList<>();
    private List<RegionTypeRule> regionTypes = new ArrayList<>();

    @Data
    public static class DeliveryMethodRule {
        private String code;
        private List<String> labels = new ArrayList<>();
    }

    @Data
    public static class ExtensionTypeRule {
        private String code;
        private List<String> labels = new ArrayList<>();

        public String getPrimaryLabel() {
            if (CollectionUtils.isEmpty(labels)) {
                return null;
            }
            return labels.get(0);
        }
    }

    @Data
    public static class RegionTypeRule {
        private DeliveryExtensionType type;
        private List<RegionEntry> entries = new ArrayList<>();
    }

    @Data
    public static class RegionEntry {
        private String code;
        private List<String> labels = new ArrayList<>();
    }
}

