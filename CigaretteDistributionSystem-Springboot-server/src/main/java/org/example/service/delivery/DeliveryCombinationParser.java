package org.example.service.delivery;

import org.example.entity.CigaretteDistributionInfoData;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 投放组合解析器（从原始字段解析为规范化的投放方式/扩展类型/标签集合）
 *
 * @author Robin
 * @version 1.0
 * @since 2025-12-10
 */
@Component
public class DeliveryCombinationParser {

    private static final Set<String> NULL_TOKENS = Collections.unmodifiableSet(
            new HashSet<>(Arrays.asList("", "NULL", "null")));

    private static final Set<String> GRADE_PREFIX_TOKENS;

    static {
        Set<String> tokens = new HashSet<>();
        tokens.add("档位");
        tokens.add("档位扩展");
        tokens.add("档位拓展");
        tokens.add("按档位");
        tokens.add("按档位扩展");
        tokens.add("按档位拓展");
        tokens.add("按档位扩展投放");
        tokens.add("按档位拓展投放");
        GRADE_PREFIX_TOKENS = Collections.unmodifiableSet(tokens);
    }

    /**
     * 从投放基础信息实体解析投放组合
     *
     * @param infoData 投放基础信息
     * @return 规范化投放组合
     */
    public DeliveryCombination parse(CigaretteDistributionInfoData infoData) {
        if (infoData == null) {
            throw new IllegalArgumentException("CigaretteDistributionInfoData 不能为空");
        }
        return parse(infoData.getDeliveryMethod(), infoData.getDeliveryEtype(), infoData.getTag());
    }

    /**
     * 从原始字段解析投放组合
     *
     * @param methodRaw    投放方式原始值
     * @param extensionRaw 扩展类型原始串（+分隔）
     * @param tagRaw       标签原始串（+分隔）
     * @return 规范化投放组合
     */
    public DeliveryCombination parse(String methodRaw, String extensionRaw, String tagRaw) {
        DeliveryMethodType methodType = DeliveryMethodType.from(methodRaw)
                .orElseThrow(() -> new IllegalArgumentException("未知的投放类型: " + methodRaw));

        List<DeliveryExtensionType> extensions = parseExtensions(extensionRaw);
        methodType.validateExtensions(extensions.size());

        List<String> tags = parseTags(tagRaw);

        return new DeliveryCombination(methodType, extensions, tags);
    }

    private List<DeliveryExtensionType> parseExtensions(String raw) {
        if (isNullLike(raw)) {
            return Collections.emptyList();
        }

        String normalized = normalizeDelimiter(raw);
        String[] tokens = normalized.split("\\+");
        LinkedHashSet<DeliveryExtensionType> result = new LinkedHashSet<>();
        for (String token : tokens) {
            String trimmed = token.trim();
            if (trimmed.isEmpty() || isNullLike(trimmed)) {
                continue;
            }
            if (isGradePrefixToken(trimmed)) {
                // 兼容“档位+区县(+市场类型)”这类写法，直接忽略“档位”前缀
                continue;
            }
            Optional<DeliveryExtensionType> type = DeliveryExtensionType.from(trimmed);
            if (!type.isPresent()) {
                throw new IllegalArgumentException("未知的扩展投放类型: " + trimmed);
            }
            result.add(type.get());
        }
        return new ArrayList<>(result);
    }

    private List<String> parseTags(String raw) {
        if (isNullLike(raw)) {
            return Collections.emptyList();
        }
        String normalized = normalizeDelimiter(raw);
        String[] tokens = normalized.split("\\+");
        LinkedHashSet<String> tags = new LinkedHashSet<>();
        for (String token : tokens) {
            String value = token.trim();
            if (value.isEmpty() || isNullLike(value)) {
                continue;
            }
            tags.add(value);
        }
        return new ArrayList<>(tags);
    }

    private boolean isNullLike(String raw) {
        if (!StringUtils.hasText(raw)) {
            return true;
        }
        return NULL_TOKENS.contains(raw.trim());
    }

    private String normalizeDelimiter(String raw) {
        if (raw == null) {
            return "";
        }
        return raw.replace("，", "+")
                .replace(",", "+")
                .replace("；", "+")
                .replace(";", "+");
    }

    /**
     * 将扩展类型集合还原为数据库存储格式（+分隔）
     *
     * @param extensionTypes 扩展类型列表
     * @return 序列化字符串
     */
    public String toExtensionString(List<DeliveryExtensionType> extensionTypes) {
        if (extensionTypes == null || extensionTypes.isEmpty()) {
            return null;
        }
        return extensionTypes.stream()
                .map(DeliveryExtensionType::getDisplayName)
                .collect(Collectors.joining("+"));
    }

    private boolean isGradePrefixToken(String token) {
        if (!StringUtils.hasText(token)) {
            return false;
        }
        String normalized = token.replaceAll("\\s+", "").trim();
        return GRADE_PREFIX_TOKENS.contains(normalized);
    }
}

