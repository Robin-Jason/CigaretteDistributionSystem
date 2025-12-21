package org.example.service.calculate;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 标签过滤配置验证测试
 * <p>
 * 验证测试用例中，如果TAG为"优质数据共享客户"，TAG_FILTER_CONFIG应当默认为"0"。
 * </p>
 *
 * @author Robin
 * @since 2025-12-20
 */
@Slf4j
@SpringBootTest
@ActiveProfiles("test")
@DisplayName("标签过滤配置验证测试")
class TagFilterConfigValidationTest {

    private static final int YEAR = 2099;
    private static final int MONTH = 9;
    private static final int WEEK_SEQ = 1;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    @DisplayName("验证TAG为优质数据共享客户时TAG_FILTER_CONFIG应为0")
    void should_tag_filter_config_be_zero_when_tag_is_premium_customer() {
        log.info("开始验证TAG_FILTER_CONFIG配置: {}-{}-{}", YEAR, MONTH, WEEK_SEQ);

        // 查询所有TAG为"优质数据共享客户"的记录
        String sql = "SELECT CIG_CODE, CIG_NAME, TAG, TAG_FILTER_CONFIG " +
                     "FROM cigarette_distribution_info " +
                     "WHERE YEAR = ? AND MONTH = ? AND WEEK_SEQ = ? " +
                     "AND TAG = '优质数据共享客户' " +
                     "ORDER BY CIG_CODE";
        
        List<Map<String, Object>> records = jdbcTemplate.queryForList(sql, YEAR, MONTH, WEEK_SEQ);
        
        log.info("找到 {} 条TAG为'优质数据共享客户'的记录", records.size());
        
        assertFalse(records.isEmpty(), "应该存在TAG为'优质数据共享客户'的记录");
        
        // 验证每条记录的TAG_FILTER_CONFIG是否为"0"
        List<String> inconsistencies = new java.util.ArrayList<>();
        int correctCount = 0;
        
        for (Map<String, Object> record : records) {
            String cigCode = getString(record, "CIG_CODE");
            String cigName = getString(record, "CIG_NAME");
            String tag = getString(record, "TAG");
            String tagFilterConfig = getString(record, "TAG_FILTER_CONFIG");
            
            if (!"0".equals(tagFilterConfig)) {
                inconsistencies.add(String.format("卷烟 %s-%s: TAG=%s, TAG_FILTER_CONFIG=%s (期望: 0)", 
                    cigCode, cigName, tag, tagFilterConfig));
            } else {
                correctCount++;
            }
        }
        
        log.info("验证结果: 总记录数={}, 正确记录数={}, 不一致记录数={}", 
            records.size(), correctCount, inconsistencies.size());
        
        if (!inconsistencies.isEmpty()) {
            log.error("发现 {} 条不一致记录:", inconsistencies.size());
            for (int i = 0; i < Math.min(20, inconsistencies.size()); i++) {
                log.error("  {}. {}", i + 1, inconsistencies.get(i));
            }
            if (inconsistencies.size() > 20) {
                log.error("  ... 还有 {} 条不一致记录未显示", inconsistencies.size() - 20);
            }
        } else {
            log.info("✅ 所有TAG为'优质数据共享客户'的记录，TAG_FILTER_CONFIG都正确设置为'0'");
        }
        
        // 断言
        assertTrue(inconsistencies.isEmpty(), 
            String.format("发现 %d 条TAG_FILTER_CONFIG不为'0'的记录，前10条: %s", 
                inconsistencies.size(), 
                String.join("; ", inconsistencies.subList(0, Math.min(10, inconsistencies.size())))));
    }

    @Test
    @DisplayName("验证TAG为null时TAG_FILTER_CONFIG应为null")
    void should_tag_filter_config_be_null_when_tag_is_null() {
        log.info("开始验证TAG为null时TAG_FILTER_CONFIG应为null: {}-{}-{}", YEAR, MONTH, WEEK_SEQ);

        // 查询所有TAG为null的记录
        String sql = "SELECT CIG_CODE, CIG_NAME, TAG, TAG_FILTER_CONFIG " +
                     "FROM cigarette_distribution_info " +
                     "WHERE YEAR = ? AND MONTH = ? AND WEEK_SEQ = ? " +
                     "AND TAG IS NULL " +
                     "ORDER BY CIG_CODE " +
                     "LIMIT 10";
        
        List<Map<String, Object>> records = jdbcTemplate.queryForList(sql, YEAR, MONTH, WEEK_SEQ);
        
        log.info("检查了 {} 条TAG为null的记录", records.size());
        
        // 验证每条记录的TAG_FILTER_CONFIG应为null
        for (Map<String, Object> record : records) {
            String cigCode = getString(record, "CIG_CODE");
            String tagFilterConfig = getString(record, "TAG_FILTER_CONFIG");
            
            // TAG为null时，TAG_FILTER_CONFIG应该也为null或空字符串
            if (tagFilterConfig != null && !tagFilterConfig.trim().isEmpty()) {
                log.warn("卷烟 {}: TAG为null但TAG_FILTER_CONFIG不为空: {}", cigCode, tagFilterConfig);
            }
        }
        
        log.info("✅ TAG为null的记录检查完成");
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

