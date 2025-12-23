package org.example.application.service.statistics.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.application.service.statistics.DeliveryCombinationRegionService;
import org.example.domain.repository.CigaretteDistributionPredictionPriceRepository;
import org.example.domain.repository.CigaretteDistributionPredictionRepository;
import org.example.domain.repository.RegionCustomerStatisticsRepository;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 投放组合与区域映射查询服务实现。
 * <p>
 * 从分配结果表查询投放组合，从区域客户统计表查询已构建的区域，
 * 根据投放组合类型返回对应的区域列表。
 * </p>
 *
 * @author Robin
 * @since 2025-12-22
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DeliveryCombinationRegionServiceImpl implements DeliveryCombinationRegionService {

    private final CigaretteDistributionPredictionRepository predictionRepository;
    private final CigaretteDistributionPredictionPriceRepository predictionPriceRepository;
    private final RegionCustomerStatisticsRepository regionCustomerStatisticsRepository;

    @Override
    public DeliveryCombinationRegionResult queryCombinationRegions(Integer year, Integer month, Integer weekSeq) {
        log.info("查询投放组合与区域映射: {}-{}-{}", year, month, weekSeq);

        // 1. 从两个预测表查询不重复的投放组合
        List<Map<String, Object>> predictionCombinations = predictionRepository.findDistinctCombinations(year, month, weekSeq);
        List<Map<String, Object>> priceCombinations = predictionPriceRepository.findDistinctCombinations(year, month, weekSeq);

        // 2. 合并去重
        Set<String> combinationKeys = new HashSet<>();
        List<Map<String, Object>> allCombinations = new ArrayList<>();
        
        for (Map<String, Object> combo : predictionCombinations) {
            String key = buildCombinationKey(combo);
            if (combinationKeys.add(key)) {
                allCombinations.add(combo);
            }
        }
        for (Map<String, Object> combo : priceCombinations) {
            String key = buildCombinationKey(combo);
            if (combinationKeys.add(key)) {
                allCombinations.add(combo);
            }
        }

        log.debug("查询到 {} 个不重复投放组合", allCombinations.size());

        // 3. 查询所有已构建的区域
        List<Map<String, Object>> regionStats = regionCustomerStatisticsRepository.findAll(year, month, weekSeq);
        Set<String> allRegions = regionStats.stream()
                .map(r -> (String) r.get("region"))
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        log.debug("查询到 {} 个已构建区域", allRegions.size());

        // 4. 构建区域映射（按扩展类型分组）
        Map<String, Set<String>> extensionTypeRegions = buildExtensionTypeRegions(allRegions);

        // 5. 为每个投放组合匹配区域
        List<CombinationRegionMapping> mappings = new ArrayList<>();
        for (Map<String, Object> combo : allCombinations) {
            CombinationRegionMapping mapping = buildMapping(combo, allRegions, extensionTypeRegions);
            mappings.add(mapping);
        }

        DeliveryCombinationRegionResult result = new DeliveryCombinationRegionResult();
        result.setCombinations(mappings);

        log.info("投放组合与区域映射查询完成: {}-{}-{}, 返回 {} 个组合", year, month, weekSeq, mappings.size());
        return result;
    }

    /**
     * 构建组合唯一键
     */
    private String buildCombinationKey(Map<String, Object> combo) {
        String method = getStringValue(combo, "DELIVERY_METHOD");
        String etype = getStringValue(combo, "DELIVERY_ETYPE");
        String tag = getStringValue(combo, "TAG");
        return method + "|" + etype + "|" + tag;
    }

    /**
     * 按扩展类型分组区域
     * <p>
     * 区域名称格式：
     * - 基础区域：全市、城区、郊区、农村
     * - 带标签区域：全市+优质客户、城区+优质客户
     * - 带扩展区域：城区（A片区）、城区（Z1）
     * </p>
     */
    private Map<String, Set<String>> buildExtensionTypeRegions(Set<String> allRegions) {
        Map<String, Set<String>> result = new HashMap<>();
        
        // 基础区域（不含括号和加号后缀的）
        Set<String> baseRegions = new HashSet<>();
        // 市场部扩展区域（含括号，括号内为字母+数字如A1、B2）
        Set<String> marketRegions = new HashSet<>();
        // 诚信互助小组扩展区域（含括号，括号内为Z+数字如Z1、Z2）
        Set<String> integrityRegions = new HashSet<>();

        for (String region : allRegions) {
            // 去除标签后缀（如 +优质客户）
            String cleanRegion = stripTagSuffix(region);
            
            if (cleanRegion.contains("（") && cleanRegion.contains("）")) {
                // 提取括号内容
                int start = cleanRegion.indexOf("（");
                int end = cleanRegion.indexOf("）");
                String inner = cleanRegion.substring(start + 1, end);
                
                if (inner.matches("Z\\d+")) {
                    // 诚信互助小组区域
                    integrityRegions.add(cleanRegion);
                } else {
                    // 市场部区域
                    marketRegions.add(cleanRegion);
                }
            } else {
                // 基础区域
                baseRegions.add(cleanRegion);
            }
        }

        result.put("base", baseRegions);
        result.put("市场部", marketRegions);
        result.put("诚信互助小组", integrityRegions);

        log.debug("区域分组: 基础={}, 市场部={}, 诚信互助小组={}", 
                baseRegions.size(), marketRegions.size(), integrityRegions.size());

        return result;
    }

    /**
     * 去除区域名称中的标签后缀
     * <p>
     * 如：全市+优质数据共享客户 -> 全市
     * </p>
     */
    private String stripTagSuffix(String region) {
        if (region == null) {
            return null;
        }
        int plusIndex = region.indexOf("+");
        if (plusIndex > 0) {
            return region.substring(0, plusIndex);
        }
        return region;
    }

    /**
     * 为单个投放组合构建区域映射
     */
    private CombinationRegionMapping buildMapping(Map<String, Object> combo, 
                                                   Set<String> allRegions,
                                                   Map<String, Set<String>> extensionTypeRegions) {
        CombinationRegionMapping mapping = new CombinationRegionMapping();
        
        String deliveryMethod = getStringValue(combo, "DELIVERY_METHOD");
        String deliveryEtype = getStringValue(combo, "DELIVERY_ETYPE");
        String tag = getStringValue(combo, "TAG");

        mapping.setDeliveryMethod(deliveryMethod);
        mapping.setDeliveryEtype(deliveryEtype);
        mapping.setTag(tag);

        // 判断是否为双扩展
        if (isDualExtension(deliveryEtype)) {
            // 双扩展：返回 extensionRegions
            Map<String, List<String>> extRegions = new LinkedHashMap<>();
            
            // 解析扩展类型，如 "档位+市场部+诚信互助小组"
            String[] parts = deliveryEtype.split("\\+");
            for (int i = 1; i < parts.length; i++) {
                String extType = parts[i].trim();
                Set<String> regions = extensionTypeRegions.getOrDefault(extType, Collections.emptySet());
                extRegions.put(extType, new ArrayList<>(regions));
            }
            
            mapping.setExtensionRegions(extRegions);
        } else {
            // 单扩展或无扩展：返回 regions
            List<String> regions = determineRegions(deliveryMethod, deliveryEtype, tag, allRegions, extensionTypeRegions);
            mapping.setRegions(regions);
        }

        return mapping;
    }

    /**
     * 判断是否为双扩展
     * <p>
     * 双扩展格式：档位+市场部+诚信互助小组
     * </p>
     */
    private boolean isDualExtension(String deliveryEtype) {
        if (deliveryEtype == null || deliveryEtype.isEmpty()) {
            return false;
        }
        // 统计加号数量，双扩展有两个加号
        long plusCount = deliveryEtype.chars().filter(ch -> ch == '+').count();
        return plusCount >= 2;
    }

    /**
     * 确定单扩展或无扩展场景的区域列表
     */
    private List<String> determineRegions(String deliveryMethod, String deliveryEtype, String tag,
                                          Set<String> allRegions,
                                          Map<String, Set<String>> extensionTypeRegions) {
        Set<String> result = new LinkedHashSet<>();

        // 如果有标签，筛选带该标签的区域
        if (tag != null && !tag.isEmpty()) {
            for (String region : allRegions) {
                String cleanRegion = stripTagSuffix(region);
                // 只返回基础区域（不含括号的）
                if (!cleanRegion.contains("（")) {
                    result.add(cleanRegion);
                }
            }
        } else if (deliveryEtype != null && !deliveryEtype.isEmpty()) {
            // 有扩展类型，根据扩展类型返回对应区域
            String[] parts = deliveryEtype.split("\\+");
            if (parts.length > 1) {
                String extType = parts[1].trim();
                Set<String> extRegions = extensionTypeRegions.getOrDefault(extType, Collections.emptySet());
                result.addAll(extRegions);
            }
        } else {
            // 无扩展无标签，返回基础区域
            result.addAll(extensionTypeRegions.getOrDefault("base", Collections.emptySet()));
        }

        return new ArrayList<>(result);
    }

    /**
     * 安全获取字符串值
     */
    private String getStringValue(Map<String, Object> map, String key) {
        Object value = map.get(key);
        return value != null ? value.toString() : null;
    }
}
