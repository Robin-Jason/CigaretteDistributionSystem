package org.example.service.util;

import lombok.RequiredArgsConstructor;
import org.example.service.delivery.DeliveryExtensionType;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.*;

/**
 * 组合策略分析器
 * <p>
 * 负责分析投放组合策略，确定组合模式（城市级、扩展维度、或跳过）。
 * 根据投放方式和扩展类型，判断应该采用哪种区域组合策略来构建客户数统计。
 * </p>
 *
 * @author Robin
 * @version 1.0
 * @since 2025-12-11
 */
@Component
@RequiredArgsConstructor
public class CombinationStrategyAnalyzer {

    /**
     * "按档位投放"的同义词集合：
     * - 按档位投放
     * - 按档位统一投放
     *
     * 这两种方式都视为"全市单区域"投放，区域只有一个：全市。
     */
    private static final Set<String> CITY_WIDE_METHODS =
            Collections.unmodifiableSet(new HashSet<>(Arrays.asList("按档位投放", "按档位统一投放")));
    private static final Set<String> UNSUPPORTED_METHODS =
            Collections.unmodifiableSet(new HashSet<>(Arrays.asList("选点投放", "按需投放")));

    /**
     * 分析投放组合策略。
     * <p>
     * 根据投放方式和扩展类型，确定组合模式（城市级、扩展维度、或跳过）。
     * </p>
     *
     * @param deliveryMethod 投放方式（如："按档位投放"、"按价位段自选投放"）
     * @param deliveryEtype  扩展投放类型（如："区县公司+市场类型"）
     * @return 组合策略对象，包含模式（CITY/EXTENSION/SKIPPED）和扩展类型列表
     * @example
     * <pre>
     *     CombinationStrategy strategy = analyzer.analyzeCombination("按档位投放", null);
     *     // strategy.mode = CombinationMode.CITY
     *     CombinationStrategy strategy2 = analyzer.analyzeCombination("按价位段自选投放", "区县公司+市场类型");
     *     // strategy2.mode = CombinationMode.EXTENSION
     *     // strategy2.extensionTypes = [COUNTY, MARKET_TYPE]
     * </pre>
     */
    public CombinationStrategy analyzeCombination(String deliveryMethod, String deliveryEtype) {
        CombinationStrategy strategy = new CombinationStrategy();
        strategy.extensionTypes = parseExtensionTypes(deliveryEtype);

        String method = deliveryMethod == null ? "" : deliveryMethod.trim();
        if (UNSUPPORTED_METHODS.contains(method)) {
            strategy.mode = CombinationMode.SKIPPED;
            return strategy;
        }

        if (CITY_WIDE_METHODS.contains(method)) {
            strategy.mode = CombinationMode.CITY;
            return strategy;
        }

        if ("按价位段自选投放".equals(method) || "按档位扩展投放".equals(method)) {
            strategy.mode = strategy.extensionTypes.isEmpty() ? CombinationMode.CITY : CombinationMode.EXTENSION;
            return strategy;
        }

        strategy.mode = strategy.extensionTypes.isEmpty() ? CombinationMode.CITY : CombinationMode.EXTENSION;
        return strategy;
    }

    /**
     * 解析扩展投放类型字符串。
     * <p>
     * 将扩展类型字符串（支持"+"、"＋"、"，"分隔）解析为扩展类型列表。
     * </p>
     *
     * @param deliveryEtype 扩展投放类型字符串（如："区县公司+市场类型"、"区县公司＋市场类型"）
     * @return 扩展类型列表（去重），如果输入为空则返回空列表
     * @example
     * <pre>
     *     List<DeliveryExtensionType> types = analyzer.parseExtensionTypes("区县公司+市场类型");
     *     // 返回: [COUNTY, MARKET_TYPE]
     *     List<DeliveryExtensionType> types2 = analyzer.parseExtensionTypes("区县公司＋市场类型");
     *     // 返回: [COUNTY, MARKET_TYPE]（支持全角加号）
     * </pre>
     */
    public List<DeliveryExtensionType> parseExtensionTypes(String deliveryEtype) {
        if (!StringUtils.hasText(deliveryEtype)) {
            return Collections.emptyList();
        }
        String normalized = deliveryEtype.replace('＋', '+')
                .replace('，', '+')
                .trim();
        String[] segments = normalized.split("\\+");
        List<DeliveryExtensionType> types = new ArrayList<>();
        for (String segment : segments) {
            DeliveryExtensionType.from(segment)
                    .filter(type -> type != DeliveryExtensionType.UNKNOWN)
                    .ifPresent(type -> {
                        if (!types.contains(type)) {
                            types.add(type);
                        }
                    });
        }
        return types;
    }

    /**
     * 确定主扩展类型。
     * <p>
     * 优先选择区县公司（COUNTY）作为主扩展类型，如果不存在则选择第一个。
     * </p>
     *
     * @param extensionTypes 扩展类型列表
     * @return 主扩展类型，如果列表为空则返回 COUNTY
     * @example
     * <pre>
     *     List<DeliveryExtensionType> types = Arrays.asList(
     *         DeliveryExtensionType.COUNTY, DeliveryExtensionType.MARKET_TYPE
     *     );
     *     DeliveryExtensionType primary = analyzer.determinePrimaryExtension(types);
     *     // 返回: COUNTY（优先选择）
     *     List<DeliveryExtensionType> types2 = Arrays.asList(
     *         DeliveryExtensionType.MARKET_TYPE, DeliveryExtensionType.BUSINESS_FORMAT
     *     );
     *     DeliveryExtensionType primary2 = analyzer.determinePrimaryExtension(types2);
     *     // 返回: MARKET_TYPE（第一个）
     * </pre>
     */
    public DeliveryExtensionType determinePrimaryExtension(List<DeliveryExtensionType> extensionTypes) {
        if (extensionTypes == null || extensionTypes.isEmpty()) {
            return DeliveryExtensionType.COUNTY;
        }
        if (extensionTypes.contains(DeliveryExtensionType.COUNTY)) {
            return DeliveryExtensionType.COUNTY;
        }
        return extensionTypes.get(0);
    }

    /**
     * 组合策略
     * <p>
     * 包含组合模式和扩展类型列表。
     * </p>
     */
    public static class CombinationStrategy {
        /** 组合模式 */
        public CombinationMode mode = CombinationMode.EXTENSION;
        /** 扩展类型列表 */
        public List<DeliveryExtensionType> extensionTypes = new ArrayList<>();
    }

    /**
     * 组合模式枚举
     * <p>
     * 定义投放组合的处理模式。
     * </p>
     */
    public enum CombinationMode {
        /** 城市级：全市单区域 */
        CITY,
        /** 扩展维度：按扩展类型组合多个区域 */
        EXTENSION,
        /** 跳过：不支持的投放方式 */
        SKIPPED
    }
}

