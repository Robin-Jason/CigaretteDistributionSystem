package org.example.domain.service.rule;

import java.util.List;
import java.util.Map;

/**
 * 标签过滤规则领域服务接口。
 * <p>
 * 定义标签提取和过滤的核心业务规则，不依赖于Spring框架或持久化层。
 * 纯领域逻辑，可独立测试。
 * </p>
 *
 * @author Robin
 * @version 1.0
 * @since 2025-12-14
 */
public interface TagFilterRule {

    /**
     * 从卷烟投放信息中提取标签列表。
     * <p>
     * 标签存储在TAG字段中，可能包含多个标签，用逗号或其他分隔符分隔。
     * </p>
     *
     * @param cigaretteInfo 卷烟投放信息Map，包含TAG等字段
     * @return 标签列表，如果没有标签则返回空列表
     */
    List<String> extractTags(Map<String, Object> cigaretteInfo);

    /**
     * 将标签与区域名拼接。
     * <p>
     * 默认规则：区域名 + "+" + 标签名
     * 例如：丹江（城网）+优质数据共享客户
     * </p>
     *
     * @param regionName 区域名称
     * @param tag        标签名称
     * @return 拼接后的区域名称
     */
    String combineRegionWithTag(String regionName, String tag);

    /**
     * 将多个标签与区域名拼接。
     * <p>
     * 对每个标签分别进行拼接，返回所有可能的组合。
     * </p>
     *
     * @param regionName 区域名称
     * @param tags        标签列表
     * @return 拼接后的区域名称列表
     */
    List<String> combineRegionWithTags(String regionName, List<String> tags);
}

