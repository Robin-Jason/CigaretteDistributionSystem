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
 * 分配写回字段一致性验证测试
 * <p>
 * 验证写回prediction分区表的投放组合信息（DELIVERY_METHOD、DELIVERY_ETYPE、TAG、TAG_FILTER_CONFIG）
 * 是否与info表对应记录一致。
 * </p>
 *
 * @author Robin
 * @since 2025-12-20
 */
@Slf4j
@SpringBootTest
@ActiveProfiles("test")
@DisplayName("分配写回字段一致性验证测试")
class DistributionWriteBackFieldConsistencyTest {

    private static final int YEAR = 2099;
    private static final int MONTH = 9;
    private static final int WEEK_SEQ = 1;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    @DisplayName("验证prediction表与info表的投放组合信息一致性")
    void should_prediction_fields_match_info_fields() {
        log.info("开始验证prediction表与info表的字段一致性: {}-{}-{}", YEAR, MONTH, WEEK_SEQ);

        // 1. 查询info表的所有记录
        List<Map<String, Object>> infoRecords = queryInfoRecords(YEAR, MONTH, WEEK_SEQ);
        assertFalse(infoRecords.isEmpty(), "info表应该有数据");

        log.info("info表记录数: {}", infoRecords.size());

        // 2. 查询prediction表的所有记录
        List<Map<String, Object>> predictionRecords = queryPredictionRecords(YEAR, MONTH, WEEK_SEQ);
        assertFalse(predictionRecords.isEmpty(), "prediction表应该有数据");

        log.info("prediction表记录数: {}", predictionRecords.size());

        // 3. 构建info表的索引（按CIG_CODE + CIG_NAME + DELIVERY_METHOD + DELIVERY_ETYPE + TAG + TAG_FILTER_CONFIG）
        Map<String, Map<String, Object>> infoIndex = buildInfoIndex(infoRecords);

        // 4. 验证每条prediction记录是否能在info表中找到对应的记录
        List<String> inconsistencies = new ArrayList<>();
        int matchedCount = 0;
        int totalPredictionRecords = predictionRecords.size();

        for (Map<String, Object> predRecord : predictionRecords) {
            String cigCode = getString(predRecord, "CIG_CODE");
            String cigName = getString(predRecord, "CIG_NAME");
            String predDeliveryMethod = getString(predRecord, "DELIVERY_METHOD");
            String predDeliveryEtype = getString(predRecord, "DELIVERY_ETYPE");
            String predTag = getString(predRecord, "TAG");
            String predTagFilterConfig = getString(predRecord, "TAG_FILTER_CONFIG");

            // 构建查找键（注意：prediction表是按区域拆分的，所以需要匹配info表的投放组合）
            String lookupKey = buildLookupKey(cigCode, cigName, predDeliveryMethod, predDeliveryEtype, predTag, predTagFilterConfig);

            Map<String, Object> matchingInfoRecord = infoIndex.get(lookupKey);

            if (matchingInfoRecord == null) {
                // 尝试模糊匹配（可能info表中的字段值略有不同）
                matchingInfoRecord = findMatchingInfoRecord(infoRecords, cigCode, cigName, predDeliveryMethod, predDeliveryEtype, predTag, predTagFilterConfig);
            }

            if (matchingInfoRecord != null) {
                matchedCount++;
                // 验证字段值是否一致
                String infoDeliveryMethod = getString(matchingInfoRecord, "DELIVERY_METHOD");
                String infoDeliveryEtype = getString(matchingInfoRecord, "DELIVERY_ETYPE");
                String infoTag = getString(matchingInfoRecord, "TAG");
                String infoTagFilterConfig = getString(matchingInfoRecord, "TAG_FILTER_CONFIG");

                boolean isConsistent = true;
                StringBuilder inconsistencyMsg = new StringBuilder();
                inconsistencyMsg.append(String.format("卷烟 %s-%s (区域: %s): ", 
                    cigCode, cigName, getString(predRecord, "DELIVERY_AREA")));

                if (!equalsIgnoreCaseAndNull(predDeliveryMethod, infoDeliveryMethod)) {
                    isConsistent = false;
                    inconsistencyMsg.append(String.format("DELIVERY_METHOD不一致 (prediction: %s, info: %s); ", 
                        predDeliveryMethod, infoDeliveryMethod));
                }

                if (!equalsIgnoreCaseAndNull(predDeliveryEtype, infoDeliveryEtype)) {
                    isConsistent = false;
                    inconsistencyMsg.append(String.format("DELIVERY_ETYPE不一致 (prediction: %s, info: %s); ", 
                        predDeliveryEtype, infoDeliveryEtype));
                }

                if (!equalsIgnoreCaseAndNull(predTag, infoTag)) {
                    isConsistent = false;
                    inconsistencyMsg.append(String.format("TAG不一致 (prediction: %s, info: %s); ", 
                        predTag, infoTag));
                }

                if (!equalsIgnoreCaseAndNull(predTagFilterConfig, infoTagFilterConfig)) {
                    isConsistent = false;
                    inconsistencyMsg.append(String.format("TAG_FILTER_CONFIG不一致 (prediction: %s, info: %s); ", 
                        predTagFilterConfig, infoTagFilterConfig));
                }

                if (!isConsistent) {
                    inconsistencies.add(inconsistencyMsg.toString());
                }
            } else {
                inconsistencies.add(String.format("卷烟 %s-%s (区域: %s): 在info表中未找到匹配的记录 (组合: %s/%s/%s/%s)", 
                    cigCode, cigName, getString(predRecord, "DELIVERY_AREA"),
                    predDeliveryMethod, predDeliveryEtype, predTag, predTagFilterConfig));
            }
        }

        // 5. 输出验证结果
        log.info("验证完成: 总prediction记录数={}, 匹配记录数={}, 不一致记录数={}", 
            totalPredictionRecords, matchedCount, inconsistencies.size());

        if (!inconsistencies.isEmpty()) {
            log.error("发现 {} 条不一致记录:", inconsistencies.size());
            for (int i = 0; i < Math.min(20, inconsistencies.size()); i++) {
                log.error("  {}. {}", i + 1, inconsistencies.get(i));
            }
            if (inconsistencies.size() > 20) {
                log.error("  ... 还有 {} 条不一致记录未显示", inconsistencies.size() - 20);
            }
        }

        // 6. 断言
        assertTrue(inconsistencies.isEmpty(), 
            String.format("发现 %d 条不一致记录，前10条: %s", 
                inconsistencies.size(), 
                String.join("; ", inconsistencies.subList(0, Math.min(10, inconsistencies.size())))));
    }

    /**
     * 查询info表记录
     */
    private List<Map<String, Object>> queryInfoRecords(Integer year, Integer month, Integer weekSeq) {
        String sql = "SELECT CIG_CODE, CIG_NAME, DELIVERY_METHOD, DELIVERY_ETYPE, TAG, TAG_FILTER_CONFIG " +
                     "FROM cigarette_distribution_info " +
                     "WHERE YEAR = ? AND MONTH = ? AND WEEK_SEQ = ? " +
                     "ORDER BY CIG_CODE, CIG_NAME";
        return jdbcTemplate.queryForList(sql, year, month, weekSeq);
    }

    /**
     * 查询prediction表记录
     */
    private List<Map<String, Object>> queryPredictionRecords(Integer year, Integer month, Integer weekSeq) {
        String sql = "SELECT CIG_CODE, CIG_NAME, DELIVERY_METHOD, DELIVERY_ETYPE, TAG, TAG_FILTER_CONFIG, DELIVERY_AREA " +
                     "FROM cigarette_distribution_prediction " +
                     "WHERE YEAR = ? AND MONTH = ? AND WEEK_SEQ = ? " +
                     "ORDER BY CIG_CODE, CIG_NAME, DELIVERY_AREA";
        return jdbcTemplate.queryForList(sql, year, month, weekSeq);
    }

    /**
     * 构建info表的索引
     */
    private Map<String, Map<String, Object>> buildInfoIndex(List<Map<String, Object>> infoRecords) {
        Map<String, Map<String, Object>> index = new HashMap<>();
        for (Map<String, Object> record : infoRecords) {
            String key = buildLookupKey(
                getString(record, "CIG_CODE"),
                getString(record, "CIG_NAME"),
                getString(record, "DELIVERY_METHOD"),
                getString(record, "DELIVERY_ETYPE"),
                getString(record, "TAG"),
                getString(record, "TAG_FILTER_CONFIG")
            );
            index.put(key, record);
        }
        return index;
    }

    /**
     * 构建查找键
     */
    private String buildLookupKey(String cigCode, String cigName, String deliveryMethod, 
                                  String deliveryEtype, String tag, String tagFilterConfig) {
        return String.format("%s|%s|%s|%s|%s|%s",
            normalize(cigCode),
            normalize(cigName),
            normalize(deliveryMethod),
            normalize(deliveryEtype),
            normalize(tag),
            normalize(tagFilterConfig)
        );
    }

    /**
     * 规范化字符串（处理null和大小写）
     */
    private String normalize(String value) {
        if (value == null) {
            return "";
        }
        return value.trim().toUpperCase();
    }

    /**
     * 查找匹配的info记录（模糊匹配）
     */
    private Map<String, Object> findMatchingInfoRecord(List<Map<String, Object>> infoRecords,
                                                       String cigCode, String cigName,
                                                       String deliveryMethod, String deliveryEtype,
                                                       String tag, String tagFilterConfig) {
        for (Map<String, Object> infoRecord : infoRecords) {
            if (equalsIgnoreCaseAndNull(getString(infoRecord, "CIG_CODE"), cigCode) &&
                equalsIgnoreCaseAndNull(getString(infoRecord, "CIG_NAME"), cigName) &&
                equalsIgnoreCaseAndNull(getString(infoRecord, "DELIVERY_METHOD"), deliveryMethod) &&
                equalsIgnoreCaseAndNull(getString(infoRecord, "DELIVERY_ETYPE"), deliveryEtype) &&
                equalsIgnoreCaseAndNull(getString(infoRecord, "TAG"), tag) &&
                equalsIgnoreCaseAndNull(getString(infoRecord, "TAG_FILTER_CONFIG"), tagFilterConfig)) {
                return infoRecord;
            }
        }
        return null;
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

