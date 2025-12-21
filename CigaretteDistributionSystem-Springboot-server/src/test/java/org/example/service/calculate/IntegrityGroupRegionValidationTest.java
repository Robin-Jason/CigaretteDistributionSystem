package org.example.service.calculate;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 诚信互助小组区域抽取验证测试
 * <p>
 * 验证info表中投放扩展类型包含诚信互助小组的卷烟，其投放区域是否从integrity_group_code_mapping表的GROUP_CODE编码集合中正确抽取（如Z1, Z2等），
 * 而不是错误地从区县扩展类型的区域集合中抽取。
 * </p>
 *
 * @author Robin
 * @since 2025-12-20
 */
@Slf4j
@SpringBootTest
@ActiveProfiles("test")
@DisplayName("诚信互助小组区域抽取验证测试")
class IntegrityGroupRegionValidationTest {

    private static final int YEAR = 2099;
    private static final int MONTH = 9;
    private static final int WEEK_SEQ = 1;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    @DisplayName("验证诚信互助小组扩展类型的区域抽取是否正确")
    void should_integrity_group_regions_be_correctly_extracted() {
        log.info("开始验证诚信互助小组区域抽取: {}-{}-{}", YEAR, MONTH, WEEK_SEQ);

        // 1. 查询info表中包含"诚信互助小组"的扩展类型记录
        String infoSql = "SELECT CIG_CODE, CIG_NAME, DELIVERY_METHOD, DELIVERY_ETYPE, DELIVERY_AREA " +
                         "FROM cigarette_distribution_info " +
                         "WHERE YEAR = ? AND MONTH = ? AND WEEK_SEQ = ? " +
                         "AND DELIVERY_ETYPE LIKE '%诚信互助小组%' " +
                         "ORDER BY CIG_CODE, CIG_NAME " +
                         "LIMIT 50";
        
        List<Map<String, Object>> integrityGroupRecords = jdbcTemplate.queryForList(infoSql, YEAR, MONTH, WEEK_SEQ);
        
        log.info("找到 {} 条包含'诚信互助小组'的记录", integrityGroupRecords.size());
        
        if (integrityGroupRecords.isEmpty()) {
            log.warn("未找到包含'诚信互助小组'的记录，跳过验证");
            return;
        }

        // 2. 获取诚信互助小组的编码集合（从integrity_group_code_mapping表的GROUP_CODE列，如Z1, Z2等）
        Set<String> integrityGroupCodes = getIntegrityGroupCodes();
        log.info("integrity_group_code_mapping表中诚信互助小组的编码数: {}", integrityGroupCodes.size());
        if (integrityGroupCodes.size() > 0 && integrityGroupCodes.size() <= 20) {
            log.info("诚信互助小组编码: {}", integrityGroupCodes);
        } else if (integrityGroupCodes.size() > 20) {
            List<String> sample = new ArrayList<>(integrityGroupCodes).subList(0, 20);
            log.info("诚信互助小组编码（前20个）: {}", sample);
        }

        // 3. 获取区县的实际区域集合（从customer_filter表的COMPANY_DISTRICT列）
        Set<String> actualCountyRegions = getActualCountyRegions();
        log.info("customer_filter表中区县的实际区域数: {}", actualCountyRegions.size());
        if (actualCountyRegions.size() > 0 && actualCountyRegions.size() <= 20) {
            log.info("区县实际区域: {}", actualCountyRegions);
        }

        // 4. 验证每条记录的投放区域
        List<String> issues = new ArrayList<>();
        int correctCount = 0;
        int totalCount = integrityGroupRecords.size();

        for (Map<String, Object> record : integrityGroupRecords) {
            String cigCode = getString(record, "CIG_CODE");
            String cigName = getString(record, "CIG_NAME");
            String deliveryEtype = getString(record, "DELIVERY_ETYPE");
            String deliveryArea = getString(record, "DELIVERY_AREA");

            // 解析投放区域（可能是单个区域或多个区域用"、"分隔）
            List<String> regions = parseDeliveryArea(deliveryArea);
            
            // 判断扩展类型
            boolean isPureIntegrityGroup = deliveryEtype != null && 
                    deliveryEtype.equals("档位+诚信互助小组");
            boolean isCountyWithIntegrityGroup = deliveryEtype != null && 
                    deliveryEtype.contains("区县") && 
                    deliveryEtype.contains("诚信互助小组");

            if (isPureIntegrityGroup) {
                // 纯诚信互助小组扩展类型：区域应该来自诚信互助小组编码集合（Z1, Z2等），不应该来自区县
                for (String region : regions) {
                    // 检查区域是否是诚信互助小组编码（应该在编码集合中）
                    if (integrityGroupCodes.contains(region)) {
                        correctCount++;
                    } else if (actualCountyRegions.contains(region)) {
                        // 区域在区县集合中，这是错误的
                        issues.add(String.format("卷烟 %s-%s: 扩展类型=%s, 区域=%s 错误地来自区县区域集合，应该是诚信互助小组编码（如Z1, Z2等）", 
                            cigCode, cigName, deliveryEtype, region));
                    } else {
                        // 区域既不是诚信互助小组编码也不在区县集合中
                        issues.add(String.format("卷烟 %s-%s: 扩展类型=%s, 区域=%s 既不是诚信互助小组编码也不在区县区域集合中", 
                            cigCode, cigName, deliveryEtype, region));
                    }
                }
            } else if (isCountyWithIntegrityGroup) {
                // 区县+诚信互助小组双扩展类型：区域格式应该是"区县（编码）"，其中编码来自诚信互助小组编码集合
                for (String region : regions) {
                    if (region.contains("（") && region.contains("）")) {
                        // 双扩展格式，检查是否正确
                        String[] parts = region.split("（");
                        if (parts.length == 2) {
                            String mainRegion = parts[0];
                            String subRegion = parts[1].replace("）", "");
                            
                            // 主区域应该来自区县
                            if (!actualCountyRegions.contains(mainRegion)) {
                                issues.add(String.format("卷烟 %s-%s: 扩展类型=%s, 区域=%s 的主区域'%s'不在区县区域集合中", 
                                    cigCode, cigName, deliveryEtype, region, mainRegion));
                            }
                            
                            // 子区域应该是诚信互助小组编码（Z1, Z2等）
                            if (!integrityGroupCodes.contains(subRegion)) {
                                issues.add(String.format("卷烟 %s-%s: 扩展类型=%s, 区域=%s 的子区域'%s'不是诚信互助小组编码（应该是Z1, Z2等）", 
                                    cigCode, cigName, deliveryEtype, region, subRegion));
                            }
                            
                            if (actualCountyRegions.contains(mainRegion) && integrityGroupCodes.contains(subRegion)) {
                                correctCount++;
                            }
                        }
                    } else {
                        issues.add(String.format("卷烟 %s-%s: 扩展类型=%s, 区域=%s 格式不正确（应该是'区县（编码）'格式，编码来自诚信互助小组编码集合）", 
                            cigCode, cigName, deliveryEtype, region));
                    }
                }
            }
        }

        log.info("验证结果: 总记录数={}, 正确区域数={}, 问题数={}", totalCount, correctCount, issues.size());

        if (!issues.isEmpty()) {
            log.error("发现 {} 个区域抽取问题:", issues.size());
            for (int i = 0; i < Math.min(20, issues.size()); i++) {
                log.error("  {}. {}", i + 1, issues.get(i));
            }
            if (issues.size() > 20) {
                log.error("  ... 还有 {} 个问题未显示", issues.size() - 20);
            }
        } else {
            log.info("✅ 所有诚信互助小组扩展类型的区域抽取都正确");
        }

        // 断言
        assertTrue(issues.isEmpty(), 
            String.format("发现 %d 个区域抽取问题，前10个: %s", 
                issues.size(), 
                String.join("; ", issues.subList(0, Math.min(10, issues.size())))));
    }

    /**
     * 获取诚信互助小组的编码集合（从integrity_group_code_mapping表的GROUP_CODE列，如Z1, Z2等）
     */
    private Set<String> getIntegrityGroupCodes() {
        String sql = "SELECT GROUP_CODE " +
                     "FROM integrity_group_code_mapping " +
                     "WHERE GROUP_CODE IS NOT NULL AND GROUP_CODE != '' " +
                     "ORDER BY SORT_ORDER";
        
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(sql);
        Set<String> codes = new HashSet<>();
        for (Map<String, Object> row : rows) {
            String code = getString(row, "GROUP_CODE");
            if (code != null && !code.trim().isEmpty()) {
                codes.add(code.trim());
            }
        }
        return codes;
    }

    /**
     * 获取区县的实际区域集合（从customer_filter表的COMPANY_DISTRICT列）
     */
    private Set<String> getActualCountyRegions() {
        String sql = "SELECT DISTINCT COMPANY_DISTRICT " +
                     "FROM customer_filter " +
                     "WHERE YEAR = ? AND MONTH = ? AND WEEK_SEQ = ? " +
                     "AND COMPANY_DISTRICT IS NOT NULL AND COMPANY_DISTRICT != ''";
        
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(sql, YEAR, MONTH, WEEK_SEQ);
        Set<String> regions = new HashSet<>();
        for (Map<String, Object> row : rows) {
            String region = getString(row, "COMPANY_DISTRICT");
            if (region != null && !region.trim().isEmpty()) {
                // 区县可能只取前两个字
                String normalized = region.trim();
                if (normalized.length() > 2) {
                    normalized = normalized.substring(0, 2);
                }
                regions.add(normalized);
            }
        }
        return regions;
    }

    /**
     * 解析投放区域字符串（支持"、"分隔）
     */
    private List<String> parseDeliveryArea(String deliveryArea) {
        List<String> regions = new ArrayList<>();
        if (deliveryArea == null || deliveryArea.trim().isEmpty()) {
            return regions;
        }
        
        // 支持中文逗号"、"和英文逗号","
        String[] parts = deliveryArea.split("[、,]");
        for (String part : parts) {
            String trimmed = part.trim();
            if (!trimmed.isEmpty()) {
                regions.add(trimmed);
            }
        }
        return regions;
    }

    /**
     * 获取字符串值（忽略大小写）
     */
    private String getString(Map<String, Object> map, String key) {
        Object value = map.get(key);
        if (value != null) {
            return value.toString();
        }
        // 尝试忽略大小写查找
        for (Map.Entry<String, Object> entry : map.entrySet()) {
            if (entry.getKey().equalsIgnoreCase(key)) {
                return entry.getValue() != null ? entry.getValue().toString() : null;
            }
        }
        return null;
    }
}

