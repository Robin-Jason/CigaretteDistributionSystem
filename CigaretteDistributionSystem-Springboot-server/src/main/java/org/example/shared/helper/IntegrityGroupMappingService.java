package org.example.shared.helper;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.infrastructure.persistence.po.IntegrityGroupMappingPO;
import org.example.domain.repository.BaseCustomerInfoRepository;
import org.example.domain.repository.IntegrityGroupMappingRepository;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 诚信互助小组映射刷新/查询服务。
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

    private final BaseCustomerInfoRepository baseCustomerInfoRepository;
    private final IntegrityGroupMappingRepository integrityGroupMappingRepository;

    /**
     * 基于 base_customer_info 重新生成诚信小组映射。
     * <p>
     * 重要：此方法必须在每次导入或更新 base_customer_info 表后调用，确保 integrity_group_code_mapping 表与 base_customer_info 保持同步。
     * </p>
     * <p>
     * 业务规则：
     * - 如果 base_customer_info 中有诚信互助小组数据，则清空并重新生成 integrity_group_code_mapping 表
     * - 如果 base_customer_info 中没有诚信互助小组数据，则清空 integrity_group_code_mapping 表（表为空是正常状态）
     * - 如果后续需要使用诚信互助小组扩展类型，但 integrity_group_code_mapping 表为空，系统会抛出 IntegrityGroupMappingEmptyException 异常
     * </p>
     *
     * @example 客户表存在小组数据 -> 清空后批量写入映射表
     */
    public void refreshFromBaseCustomer() {
        ensureTable();
        integrityGroupMappingRepository.truncateTable();

        List<Map<String, Object>> rows = baseCustomerInfoRepository.selectGroupNameStatistics();
        if (rows.isEmpty()) {
            log.warn("base_customer_info 中无有效的诚信互助小组数据，已清空 integrity_group_code_mapping。" +
                    "如果后续需要使用诚信互助小组扩展类型，请确保 base_customer_info 中包含 GROUP_NAME 字段数据");
            return;
        }

        List<IntegrityGroupMappingPO> mappings = new ArrayList<>();
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

            IntegrityGroupMappingPO mapping = new IntegrityGroupMappingPO();
            mapping.setGroupName(groupName);
            mapping.setGroupCode("Z" + rank);
            mapping.setCustomerCount(cnt);
            mapping.setSortOrder(rank);
            mappings.add(mapping);
            rank++;
        }

        if (mappings.isEmpty()) {
            log.warn("未找到可写入的诚信互助小组数据，integrity_group_code_mapping 仍为空");
            return;
        }
        integrityGroupMappingRepository.batchInsert(mappings);
        log.info("刷新诚信互助小组编码映射完成，共写入 {} 条记录", mappings.size());
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
        List<IntegrityGroupMappingPO> mappings = integrityGroupMappingRepository.selectAllOrderBySort();
        for (IntegrityGroupMappingPO mapping : mappings) {
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
        integrityGroupMappingRepository.createTableIfNotExists();
    }
}

