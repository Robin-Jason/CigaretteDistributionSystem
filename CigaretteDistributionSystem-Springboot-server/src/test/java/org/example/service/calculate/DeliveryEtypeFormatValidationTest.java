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
 * DELIVERY_ETYPE格式验证测试
 * <p>
 * 验证：
 * 1. info表和prediction表中的DELIVERY_ETYPE是否一致
 * 2. DELIVERY_ETYPE是否符合"档位+"前缀格式要求
 * </p>
 *
 * @author Robin
 * @since 2025-12-20
 */
@Slf4j
@SpringBootTest
@ActiveProfiles("test")
@DisplayName("DELIVERY_ETYPE格式验证测试")
class DeliveryEtypeFormatValidationTest {

    private static final int YEAR = 2099;
    private static final int MONTH = 9;
    private static final int WEEK_SEQ = 1;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    @DisplayName("验证info表和prediction表的DELIVERY_ETYPE格式一致性")
    void should_delivery_etype_format_be_consistent() {
        log.info("开始验证DELIVERY_ETYPE格式: {}-{}-{}", YEAR, MONTH, WEEK_SEQ);

        // 1. 查询info表的所有不重复的DELIVERY_ETYPE值
        String infoSql = "SELECT DISTINCT DELIVERY_ETYPE, COUNT(*) AS cnt " +
                         "FROM cigarette_distribution_info " +
                         "WHERE YEAR = ? AND MONTH = ? AND WEEK_SEQ = ? " +
                         "AND DELIVERY_ETYPE IS NOT NULL " +
                         "GROUP BY DELIVERY_ETYPE " +
                         "ORDER BY DELIVERY_ETYPE";
        List<Map<String, Object>> infoEtypes = jdbcTemplate.queryForList(infoSql, YEAR, MONTH, WEEK_SEQ);
        
        log.info("info表中的DELIVERY_ETYPE值（共{}种）:", infoEtypes.size());
        for (Map<String, Object> row : infoEtypes) {
            String etype = getString(row, "DELIVERY_ETYPE");
            Long count = getLong(row, "cnt");
            log.info("  {}: {} 条记录", etype, count);
        }

        // 2. 查询prediction表的所有不重复的DELIVERY_ETYPE值
        String predSql = "SELECT DISTINCT DELIVERY_ETYPE, COUNT(*) AS cnt " +
                         "FROM cigarette_distribution_prediction " +
                         "WHERE YEAR = ? AND MONTH = ? AND WEEK_SEQ = ? " +
                         "AND DELIVERY_ETYPE IS NOT NULL " +
                         "GROUP BY DELIVERY_ETYPE " +
                         "ORDER BY DELIVERY_ETYPE";
        List<Map<String, Object>> predEtypes = jdbcTemplate.queryForList(predSql, YEAR, MONTH, WEEK_SEQ);
        
        log.info("prediction表中的DELIVERY_ETYPE值（共{}种）:", predEtypes.size());
        for (Map<String, Object> row : predEtypes) {
            String etype = getString(row, "DELIVERY_ETYPE");
            Long count = getLong(row, "cnt");
            log.info("  {}: {} 条记录", etype, count);
        }

        // 3. 构建集合用于比较
        Set<String> infoEtypeSet = new HashSet<>();
        for (Map<String, Object> row : infoEtypes) {
            String etype = getString(row, "DELIVERY_ETYPE");
            if (etype != null) {
                infoEtypeSet.add(etype);
            }
        }

        Set<String> predEtypeSet = new HashSet<>();
        for (Map<String, Object> row : predEtypes) {
            String etype = getString(row, "DELIVERY_ETYPE");
            if (etype != null) {
                predEtypeSet.add(etype);
            }
        }

        // 4. 检查一致性
        Set<String> onlyInInfo = new HashSet<>(infoEtypeSet);
        onlyInInfo.removeAll(predEtypeSet);
        
        Set<String> onlyInPred = new HashSet<>(predEtypeSet);
        onlyInPred.removeAll(infoEtypeSet);

        if (!onlyInInfo.isEmpty()) {
            log.warn("仅在info表中存在的DELIVERY_ETYPE值: {}", onlyInInfo);
        }
        if (!onlyInPred.isEmpty()) {
            log.warn("仅在prediction表中存在的DELIVERY_ETYPE值: {}", onlyInPred);
        }

        // 5. 验证格式是否符合"档位+"前缀要求
        List<String> invalidFormats = new ArrayList<>();
        Set<String> allEtypes = new HashSet<>();
        allEtypes.addAll(infoEtypeSet);
        allEtypes.addAll(predEtypeSet);

        for (String etype : allEtypes) {
            if (etype != null && !etype.trim().isEmpty()) {
                // 检查是否符合"档位+"前缀格式
                if (!etype.startsWith("档位+")) {
                    invalidFormats.add(etype);
                }
            }
        }

        log.info("格式验证结果:");
        if (invalidFormats.isEmpty()) {
            log.info("✅ 所有DELIVERY_ETYPE值都符合'档位+'前缀格式");
        } else {
            log.error("❌ 发现 {} 个不符合'档位+'前缀格式的DELIVERY_ETYPE值:", invalidFormats.size());
            for (String invalid : invalidFormats) {
                log.error("  - {}", invalid);
            }
        }

        // 6. 详细对比：检查每条prediction记录是否能在info表中找到匹配的DELIVERY_ETYPE
        String detailSql = "SELECT p.CIG_CODE, p.CIG_NAME, p.DELIVERY_METHOD, p.DELIVERY_ETYPE AS pred_etype, " +
                           "i.DELIVERY_ETYPE AS info_etype " +
                           "FROM cigarette_distribution_prediction p " +
                           "LEFT JOIN cigarette_distribution_info i ON " +
                           "  p.YEAR = i.YEAR AND p.MONTH = i.MONTH AND p.WEEK_SEQ = i.WEEK_SEQ AND " +
                           "  p.CIG_CODE = i.CIG_CODE AND p.CIG_NAME = i.CIG_NAME AND " +
                           "  p.DELIVERY_METHOD = i.DELIVERY_METHOD " +
                           "WHERE p.YEAR = ? AND p.MONTH = ? AND p.WEEK_SEQ = ? " +
                           "AND (p.DELIVERY_ETYPE IS NOT NULL OR i.DELIVERY_ETYPE IS NOT NULL) " +
                           "LIMIT 100";
        
        List<Map<String, Object>> detailRecords = jdbcTemplate.queryForList(detailSql, YEAR, MONTH, WEEK_SEQ);
        
        List<String> inconsistencies = new ArrayList<>();
        int matchedCount = 0;
        
        for (Map<String, Object> record : detailRecords) {
            String cigCode = getString(record, "CIG_CODE");
            String cigName = getString(record, "CIG_NAME");
            String predEtype = getString(record, "pred_etype");
            String infoEtype = getString(record, "info_etype");
            
            if (!equalsIgnoreCaseAndNull(predEtype, infoEtype)) {
                inconsistencies.add(String.format("卷烟 %s-%s: prediction=%s, info=%s", 
                    cigCode, cigName, predEtype, infoEtype));
            } else {
                matchedCount++;
            }
        }
        
        log.info("详细对比结果: 检查了{}条记录, 匹配{}条, 不一致{}条", 
            detailRecords.size(), matchedCount, inconsistencies.size());
        
        if (!inconsistencies.isEmpty() && inconsistencies.size() <= 20) {
            log.warn("不一致记录:");
            for (String inc : inconsistencies) {
                log.warn("  {}", inc);
            }
        }

        // 7. 断言
        assertTrue(onlyInInfo.isEmpty() && onlyInPred.isEmpty(), 
            String.format("DELIVERY_ETYPE值不一致: 仅在info表中存在=%s, 仅在prediction表中存在=%s", 
                onlyInInfo, onlyInPred));
        
        assertTrue(invalidFormats.isEmpty(), 
            String.format("发现 %d 个不符合'档位+'前缀格式的DELIVERY_ETYPE值: %s", 
                invalidFormats.size(), invalidFormats));
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

    /**
     * 获取Long值
     */
    private Long getLong(Map<String, Object> map, String key) {
        Object value = map.get(key);
        if (value == null) {
            return null;
        }
        if (value instanceof Long) {
            return (Long) value;
        }
        if (value instanceof Number) {
            return ((Number) value).longValue();
        }
        try {
            return Long.parseLong(value.toString());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    /**
     * 比较两个字符串是否相等（忽略大小写和null处理）
     */
    private boolean equalsIgnoreCaseAndNull(String str1, String str2) {
        if (str1 == null && str2 == null) {
            return true;
        }
        if (str1 == null || str2 == null) {
            return false;
        }
        return str1.trim().equalsIgnoreCase(str2.trim());
    }
}

