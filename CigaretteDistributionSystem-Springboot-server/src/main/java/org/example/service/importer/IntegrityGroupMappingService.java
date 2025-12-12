package org.example.service.importer;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.entity.IntegrityGroupMapping;
import org.example.mapper.BaseCustomerInfoMapper;
import org.example.mapper.IntegrityGroupMappingMapper;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 诚信自律小组映射刷新/查询服务。
 *
 * <p>职责：从 base_customer_info 统计小组，重建 integrity_group_code_mapping；提供查询。</p>
 *
 * @author Robin
 * @version 1.0
 * @since 2025-12-11
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class IntegrityGroupMappingService {

    private final BaseCustomerInfoMapper baseCustomerInfoMapper;
    private final IntegrityGroupMappingMapper integrityGroupMappingMapper;

    /**
     * 基于 base_customer_info 重新生成诚信小组映射。
     *
     * @example 客户表存在小组数据 -> 清空后批量写入映射表
     */
    public void refreshFromBaseCustomer() {
        ensureTable();
        integrityGroupMappingMapper.truncateTable();

        List<Map<String, Object>> rows = baseCustomerInfoMapper.selectGroupNameStatistics();
        if (rows.isEmpty()) {
            log.warn("base_customer_info 中无有效的诚信自律小组数据，已清空 integrity_group_code_mapping");
            return;
        }

        List<IntegrityGroupMapping> mappings = new ArrayList<>();
        int rank = 1;
        for (Map<String, Object> row : rows) {
            Object nameObj = row.getOrDefault("group_name", row.get("GROUP_NAME"));
            if (nameObj == null) {
                continue;
            }
            String groupName = nameObj.toString().trim();
            if (groupName.isEmpty()) {
                continue;
            }
            Object cntObj = row.getOrDefault("customer_cnt", row.get("CUSTOMER_CNT"));
            BigDecimal cnt = cntObj == null ? BigDecimal.ZERO : new BigDecimal(cntObj.toString());

            IntegrityGroupMapping mapping = new IntegrityGroupMapping();
            mapping.setGroupName(groupName);
            mapping.setGroupCode("Z" + rank);
            mapping.setCustomerCount(cnt);
            mapping.setSortOrder(rank);
            mappings.add(mapping);
            rank++;
        }

        if (mappings.isEmpty()) {
            log.warn("未找到可写入的诚信自律小组数据，integrity_group_code_mapping 仍为空");
            return;
        }
        integrityGroupMappingMapper.batchInsert(mappings);
        log.info("刷新诚信自律小组编码映射完成，共写入 {} 条记录", mappings.size());
    }

    /**
     * 查询全部诚信小组映射。
     *
     * @return 映射列表
     * @example 返回字段 groupName/groupCode/customerCount/sortOrder
     */
    public List<Map<String, Object>> fetchAll() {
        ensureTable();
        List<Map<String, Object>> result = new ArrayList<>();
        List<IntegrityGroupMapping> mappings = integrityGroupMappingMapper.selectAllOrderBySort();
        for (IntegrityGroupMapping mapping : mappings) {
            Map<String, Object> row = new HashMap<>();
            row.put("groupName", mapping.getGroupName());
            row.put("groupCode", mapping.getGroupCode());
            row.put("customerCount", mapping.getCustomerCount());
            row.put("sortOrder", mapping.getSortOrder());
            result.add(row);
        }
        return result;
    }

    private void ensureTable() {
        integrityGroupMappingMapper.createTableIfNotExists();
    }
}

