package org.example.dao.impl;

import lombok.extern.slf4j.Slf4j;
import org.example.dao.BaseCustomerInfoDAO;
import org.example.mapper.BaseCustomerInfoMapper;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 客户基础信息表DAO实现类
 * 
 * @author System
 * @version 1.0
 * @since 2025-11-30
 */
@Slf4j
@Repository
public class BaseCustomerInfoDAOImpl implements BaseCustomerInfoDAO {
    
    private static final String NON_VISIT_ORDER_CYCLE = "不访销";
    private static final String DEFAULT_DYNAMIC_COLUMN_TYPE = "varchar(255) DEFAULT NULL";
    
    private final BaseCustomerInfoMapper mapper;
    
    public BaseCustomerInfoDAOImpl(BaseCustomerInfoMapper mapper) {
        this.mapper = mapper;
    }
    
    @Override
    public List<Map<String, Object>> findAll() {
        return mapper.selectAll();
    }
    
    @Override
    public List<Map<String, Object>> findByOrderCycle(List<String> orderCycles) {
        if (orderCycles == null || orderCycles.isEmpty()) {
            return findAll();
        }
        // 固定排除“不访销”
        return mapper.selectByOrderCycle(orderCycles, NON_VISIT_ORDER_CYCLE);
    }
    
    @Override
    public void dropTable() {
        mapper.dropTable();
    }
    
    @Override
    public void createTable(List<String> columnOrder,
                            Map<String, String> columnDefinitions,
                            String defaultColumnDefinition) {
        LinkedHashSet<String> orderedColumns = new LinkedHashSet<>();
        if (columnOrder != null) {
            for (String column : columnOrder) {
                if (column == null || column.isEmpty() || "ID".equalsIgnoreCase(column)) {
                    continue;
                }
                orderedColumns.add(column);
            }
        }
        
        String defaultDefinition = defaultColumnDefinition != null
                ? defaultColumnDefinition
                : DEFAULT_DYNAMIC_COLUMN_TYPE;
        
        mapper.createTable(new ArrayList<>(orderedColumns), columnDefinitions, defaultDefinition);
    }
    
    @Override
    public int insertRow(List<String> columns, Map<String, Object> row) {
        if (columns == null || columns.isEmpty()) {
            return 0;
        }
        List<String> sanitizedColumns = columns.stream()
                .filter(col -> col != null && !col.isEmpty() && !"ID".equalsIgnoreCase(col))
                .collect(Collectors.toList());
        if (sanitizedColumns.isEmpty()) {
            return 0;
        }
        return mapper.insertRow(sanitizedColumns, row);
    }
    
    @Override
    public List<Map<String, Object>> findGroupNameStatistics() {
        return mapper.selectGroupNameStatistics();
    }
    
    @Override
    public long count() {
        Long count = mapper.countAll();
        return count != null ? count : 0L;
    }
    
    @Override
    public Set<String> getColumnNames() {
        return mapper.listColumns().stream()
                .map(row -> row.get("Field"))
                .filter(Objects::nonNull)
                .map(Object::toString)
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }
    
    @Override
    public boolean tableExists() {
        Integer exists = mapper.existsTable();
        return exists != null && exists > 0;
    }
}

