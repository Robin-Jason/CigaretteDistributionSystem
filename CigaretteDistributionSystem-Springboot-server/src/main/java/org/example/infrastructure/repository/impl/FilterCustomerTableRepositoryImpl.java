package org.example.infrastructure.repository.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.domain.repository.FilterCustomerTableRepository;
import org.example.infrastructure.persistence.mapper.FilterCustomerTableMapper;
import org.example.shared.helper.CustomerFilterTableSyncService;
import org.example.shared.util.PartitionTableManager;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;

/**
 * {@link FilterCustomerTableRepository} 的 MyBatis-Plus 实现。
 * <p>
 * 适配 {@code FilterCustomerTableMapper} 提供数据访问。
 * 只支持分区表模式。
 * </p>
 *
 * @author Robin
 * @version 2.0
 * @since 2025-12-12
 */
@Slf4j
@Repository
@RequiredArgsConstructor
public class FilterCustomerTableRepositoryImpl implements FilterCustomerTableRepository {

    private final FilterCustomerTableMapper filterCustomerTableMapper;
    private final PartitionTableManager partitionTableManager;
    private final CustomerFilterTableSyncService customerFilterTableSyncService;
    
    private static final String CUSTOMER_FILTER_TABLE = "customer_filter";

    @Override
    public void ensurePartitionAndInsertData(Integer year, Integer month, Integer weekSeq, String whereClause) {
        // 1. 同步表结构（确保 customer_filter 包含 base_customer_info 的所有字段）
        try {
            List<String> addedColumns = customerFilterTableSyncService.syncTableStructure();
            if (!addedColumns.isEmpty()) {
                log.info("检测到 base_customer_info 新增字段，已同步到 customer_filter: {}", addedColumns);
            }
        } catch (Exception e) {
            log.warn("同步表结构失败，继续执行（可能表结构已是最新）: {}", e.getMessage());
        }
        
        // 2. 确保分区存在
        partitionTableManager.ensurePartitionExists(CUSTOMER_FILTER_TABLE, year, month, weekSeq);
        
        // 3. 截断分区数据（清空旧数据）
        String partitionName = PartitionTableManager.generatePartitionName(year, month, weekSeq);
        try {
            filterCustomerTableMapper.truncatePartition(partitionName);
            log.debug("截断分区成功: {}.{}", CUSTOMER_FILTER_TABLE, partitionName);
        } catch (Exception e) {
            log.warn("截断分区失败，可能分区不存在: {}.{}", CUSTOMER_FILTER_TABLE, partitionName, e);
            // 如果截断失败，继续尝试插入（可能是新分区）
        }
        
        // 4. 获取 customer_filter 表的字段列表（用于动态插入）
        List<String> filterColumns = customerFilterTableSyncService.getFilterTableColumns();
        
        // 5. 插入数据（动态字段）
        filterCustomerTableMapper.ensurePartitionAndInsertDataDynamic(year, month, weekSeq, whereClause, filterColumns);
        log.info("分区数据插入成功: {}.{} (year={}, month={}, weekSeq={})", 
                CUSTOMER_FILTER_TABLE, partitionName, year, month, weekSeq);
    }

    @Override
    public void truncatePartition(Integer year, Integer month, Integer weekSeq) {
        String partitionName = PartitionTableManager.generatePartitionName(year, month, weekSeq);
        filterCustomerTableMapper.truncatePartition(partitionName);
        log.info("截断分区成功: {}.{}", CUSTOMER_FILTER_TABLE, partitionName);
    }

    @Override
    public Long countPartition(Integer year, Integer month, Integer weekSeq) {
        return filterCustomerTableMapper.countPartition(year, month, weekSeq);
    }

    @Override
    public List<Map<String, Object>> queryPartition(Integer year, Integer month, Integer weekSeq, List<String> columns) {
        return filterCustomerTableMapper.queryPartition(year, month, weekSeq, columns);
    }

    @Override
    public List<String> listOrderCyclesPartition(Integer year, Integer month, Integer weekSeq) {
        return filterCustomerTableMapper.listOrderCyclesPartition(year, month, weekSeq);
    }

    @Override
    public List<Map<String, Object>> listDistinctCombinationsPartition(Integer year, Integer month, Integer weekSeq, List<String> columns) {
        return filterCustomerTableMapper.listDistinctCombinationsPartition(year, month, weekSeq, columns);
    }

    @Override
    public List<Map<String, Object>> statGradesPartition(Integer year, Integer month, Integer weekSeq,
                                                          Map<String, String> filters,
                                                          String tagColumn,
                                                          String tagOperator,
                                                          Object tagValue,
                                                          String orderCyclePattern) {
        return filterCustomerTableMapper.statGradesPartition(year, month, weekSeq, filters, tagColumn, tagOperator, tagValue, orderCyclePattern);
    }
}

