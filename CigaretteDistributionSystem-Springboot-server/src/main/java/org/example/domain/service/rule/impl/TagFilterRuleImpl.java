package org.example.domain.service.rule.impl;

import org.example.domain.service.rule.TagFilterRule;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 标签过滤规则领域服务实现。
 * <p>
 * 纯领域逻辑，不含Spring依赖，可独立测试。
 * 该实现复制自 {@link org.example.application.service.impl.TagExtractionServiceImpl} 的核心业务规则方法，
 * 移除了Spring注解和基础设施依赖，保持业务逻辑完全一致。
 * </p>
 *
 * @author Robin
 * @version 1.0
 * @since 2025-12-14
 */
public class TagFilterRuleImpl implements TagFilterRule {

    @Override
    public List<String> extractTags(Map<String, Object> cigaretteInfo) {
        if (cigaretteInfo == null) {
            return new ArrayList<>();
        }

        Object tagObj = cigaretteInfo.get("TAG");
        if (tagObj == null) {
            // 尝试不区分大小写查找
            for (Map.Entry<String, Object> entry : cigaretteInfo.entrySet()) {
                if (entry.getKey().equalsIgnoreCase("TAG")) {
                    tagObj = entry.getValue();
                    break;
                }
            }
        }

        if (tagObj == null) {
            return new ArrayList<>();
        }

        String tagStr = tagObj.toString().trim();
        if (tagStr.isEmpty()) {
            return new ArrayList<>();
        }

        // 支持多种分隔符：逗号、分号、空格等
        return Arrays.stream(tagStr.split("[,，;；\\s]+"))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .collect(Collectors.toList());
    }

    @Override
    public String combineRegionWithTag(String regionName, String tag) {
        if (regionName == null || regionName.trim().isEmpty()) {
            return tag != null ? tag : "";
        }
        if (tag == null || tag.trim().isEmpty()) {
            return regionName;
        }
        return regionName + "+" + tag.trim();
    }

    @Override
    public List<String> combineRegionWithTags(String regionName, List<String> tags) {
        List<String> result = new ArrayList<>();

        if (tags == null || tags.isEmpty()) {
            result.add(regionName);
            return result;
        }

        for (String tag : tags) {
            result.add(combineRegionWithTag(regionName, tag));
        }

        return result;
    }
}

