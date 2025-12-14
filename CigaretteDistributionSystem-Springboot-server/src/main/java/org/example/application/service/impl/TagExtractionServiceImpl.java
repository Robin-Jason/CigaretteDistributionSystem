package org.example.application.service.impl;

import lombok.extern.slf4j.Slf4j;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.application.service.TagExtractionService;
import org.example.domain.service.rule.TagFilterRule;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 标签提取服务实现类
 * 
 * 负责从卷烟投放基础信息中提取标签，并提供扩展接口支持不同的标签拼接规则
 * 
 * @author System
 * @version 1.0
 * @since 2025-11-30
 */
@Slf4j
@Service
public class TagExtractionServiceImpl implements TagExtractionService {
    
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private static final TagFilterRule TAG_FILTER_RULE = new org.example.domain.service.rule.impl.TagFilterRuleImpl();
    
    /**
     * 从卷烟投放信息中提取标签列表
     * 
     * 标签存储在TAG字段中，可能包含多个标签，用逗号或其他分隔符分隔
     * 
     * @param cigaretteInfo 卷烟投放信息Map，包含TAG等字段
     * @return 标签列表，如果没有标签则返回空列表
     */
    @Override
    public List<String> extractTags(Map<String, Object> cigaretteInfo) {
        List<String> tags = TAG_FILTER_RULE.extractTags(cigaretteInfo);
        log.debug("从卷烟信息中提取标签: {}", tags);
        return tags;
    }

    @Override
    public List<org.example.domain.model.tag.TagFilterRule> resolveTagFilters(Map<String, Object> cigaretteInfo) {
        List<String> tags = extractTags(cigaretteInfo);
        if (tags.isEmpty()) {
            return Collections.emptyList();
        }

        Map<String, org.example.domain.model.tag.TagFilterRule> configRules = parseTagFilterConfig(cigaretteInfo);
        List<org.example.domain.model.tag.TagFilterRule> result = new ArrayList<>();

        for (String tag : tags) {
            org.example.domain.model.tag.TagFilterRule rule = configRules.get(tag);
            if (rule != null) {
                result.add(rule);
            } else {
                // 没有在 TAG_FILTER_CONFIG 中定义过滤规则的标签：
                // 仅用于区域名称拼接，不参与SQL过滤
                result.add(new org.example.domain.model.tag.TagFilterRule(tag, null, "=", null, org.example.domain.model.tag.TagFilterRule.ValueType.STRING));
            }
        }

        return result;
    }
    
    /**
     * 将标签与区域名拼接
     * 
     * 默认规则：区域名 + "+" + 标签名
     * 例如：丹江（城网）+优质数据共享客户
     * 
     * 子类可以实现不同的拼接规则
     * 
     * @param regionName 区域名称
     * @param tag 标签名称
     * @return 拼接后的区域名称
     */
    @Override
    public String combineRegionWithTag(String regionName, String tag) {
        return TAG_FILTER_RULE.combineRegionWithTag(regionName, tag);
    }
    
    /**
     * 将多个标签与区域名拼接
     * 
     * 对每个标签分别进行拼接，返回所有可能的组合
     * 
     * @param regionName 区域名称
     * @param tags 标签列表
     * @return 拼接后的区域名称列表
     */
    @Override
    public List<String> combineRegionWithTags(String regionName, List<String> tags) {
        return TAG_FILTER_RULE.combineRegionWithTags(regionName, tags);
    }

    private Map<String, org.example.domain.model.tag.TagFilterRule> parseTagFilterConfig(Map<String, Object> cigaretteInfo) {
        Object configObject = cigaretteInfo != null ? cigaretteInfo.get("TAG_FILTER_CONFIG") : null;
        if (configObject == null) {
            // 尝试大小写不敏感匹配
            if (cigaretteInfo != null) {
                for (Map.Entry<String, Object> entry : cigaretteInfo.entrySet()) {
                    if ("TAG_FILTER_CONFIG".equalsIgnoreCase(entry.getKey())) {
                        configObject = entry.getValue();
                        break;
                    }
                }
            }
        }

        if (configObject == null) {
            return Collections.emptyMap();
        }

        String json = configObject.toString();
        if (json.trim().isEmpty()) {
            return Collections.emptyMap();
        }

        Map<String, org.example.domain.model.tag.TagFilterRule> rules = new HashMap<>();
        try {
            JsonNode root = OBJECT_MAPPER.readTree(json);
            if (root == null || !root.isObject()) {
                return Collections.emptyMap();
            }

            root.fields().forEachRemaining(entry -> {
                String tagName = entry.getKey();
                JsonNode node = entry.getValue();
                if (node == null || !node.isObject()) {
                    return;
                }

                String column = getText(node, "column");
                String operator = getText(node, "operator");
                String value = getText(node, "value");
                org.example.domain.model.tag.TagFilterRule.ValueType valueType = org.example.domain.model.tag.TagFilterRule.ValueType.from(getText(node, "valueType"));

                if (column != null && !column.trim().isEmpty()) {
                    rules.put(tagName, new org.example.domain.model.tag.TagFilterRule(tagName, column.trim(), operator, value, valueType));
                }
            });
        } catch (Exception e) {
            log.warn("解析TAG_FILTER_CONFIG失败: {}", json, e);
        }

        return rules;
    }

    private String getText(JsonNode node, String fieldName) {
        JsonNode target = node.get(fieldName);
        return target == null || target.isMissingNode() ? null : target.asText();
    }
}

