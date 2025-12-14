package org.example.domain.model.valueobject;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 投放组合实体（规范化的投放方式、扩展类型、标签集合）
 *
 * @author Robin
 * @version 1.0
 * @since 2025-12-10
 */
public class DeliveryCombination {

    private final DeliveryMethodType methodType;
    private final List<DeliveryExtensionType> extensionTypes;
    private final List<String> tags;

    /**
     * 构造函数
     *
     * @param methodType     规范化的投放方式
     * @param extensionTypes 扩展投放类型列表
     * @param tags           标签列表
     */
    public DeliveryCombination(DeliveryMethodType methodType,
                               List<DeliveryExtensionType> extensionTypes,
                               List<String> tags) {
        this.methodType = methodType;
        this.extensionTypes = Collections.unmodifiableList(extensionTypes);
        this.tags = Collections.unmodifiableList(tags);
    }

    /**
     * @return 投放方式
     */
    public DeliveryMethodType getMethodType() {
        return methodType;
    }

    /**
     * @return 扩展投放类型列表
     */
    public List<DeliveryExtensionType> getExtensionTypes() {
        return extensionTypes;
    }

    /**
     * @return 标签列表
     */
    public List<String> getTags() {
        return tags;
    }

    /**
     * 是否已实现业务逻辑（根据投放方式）
     *
     * @return true 表示已有实现
     */
    public boolean isImplemented() {
        return methodType != null && methodType.isImplemented();
    }

    /**
     * 是否需要区域客户统计（档位/档位扩展/价位段）
     *
     * @return true 需要统计
     */
    public boolean requiresStatistics() {
        return methodType == DeliveryMethodType.GRADE
                || methodType == DeliveryMethodType.GRADE_EXTEND
                || methodType == DeliveryMethodType.PRICE_SEGMENT;
    }

    /**
     * 返回稳定的缓存 Key（用于组合分组）
     *
     * @return 缓存键
     */
    public String toCacheKey() {
        String extPart = extensionTypes.stream()
                .map(Enum::name)
                .collect(Collectors.joining("+"));
        String tagPart = tags.isEmpty() ? "" : String.join("+", tags);
        return methodType.name() + "|" + extPart + "|" + tagPart;
    }

    @Override
    public String toString() {
        return "DeliveryCombination{" +
                "methodType=" + methodType +
                ", extensionTypes=" + extensionTypes +
                ", tags=" + tags +
                '}';
    }
}

