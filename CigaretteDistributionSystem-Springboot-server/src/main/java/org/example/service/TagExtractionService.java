package org.example.service;

import org.example.service.model.tag.TagFilterRule;

import java.util.List;
import java.util.Map;

/**
 * 标签提取与组合服务接口（原三层结构恢复用）
 */
public interface TagExtractionService {

    /**
     * 从卷烟投放信息中提取标签列表。
     *
     * @param cigaretteInfo 卷烟投放信息
     * @return 标签列表
     */
    List<String> extractTags(Map<String, Object> cigaretteInfo);

    /**
     * 解析标签过滤规则。
     *
     * @param cigaretteInfo 卷烟投放信息
     * @return 标签过滤规则列表
     */
    List<TagFilterRule> resolveTagFilters(Map<String, Object> cigaretteInfo);

    /**
     * 将标签与区域名拼接。
     *
     * @param regionName 区域名
     * @param tag        标签
     * @return 拼接后的区域名
     */
    String combineRegionWithTag(String regionName, String tag);

    /**
     * 将多个标签与区域名拼接。
     *
     * @param regionName 区域名
     * @param tags       标签列表
     * @return 拼接后的区域名列表
     */
    List<String> combineRegionWithTags(String regionName, List<String> tags);
}

