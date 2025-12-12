package org.example.dao.impl;

import lombok.extern.slf4j.Slf4j;
import org.example.dao.TemporaryCustomerTableDAO;
import org.example.mapper.TemporaryCustomerTableMapper;
import org.springframework.stereotype.Repository;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * 临时客户表 DAO 实现
 */
@Slf4j
@Repository
public class TemporaryCustomerTableDAOImpl implements TemporaryCustomerTableDAO {

    private final TemporaryCustomerTableMapper mapper;
    private static final Pattern SAFE_TABLE_NAME = Pattern.compile("^[a-zA-Z0-9_]+$");

    public TemporaryCustomerTableDAOImpl(TemporaryCustomerTableMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    public void createTableFromBase(String tableName, String whereClause) {
        validateTableName(tableName);
        mapper.createTableFromBase(tableName, whereClause);
    }

    @Override
    public void createEmptyTable(String tableName) {
        validateTableName(tableName);
        mapper.createEmptyTable(tableName);
    }

    @Override
    public boolean tableExists(String tableName) {
        validateTableName(tableName);
        Long count = mapper.tableExists(tableName);
        return count != null && count > 0;
    }

    @Override
    public void dropTable(String tableName) {
        validateTableName(tableName);
        mapper.dropTable(tableName);
    }

    @Override
    public long count(String tableName) {
        validateTableName(tableName);
        Long count = mapper.count(tableName);
        return count != null ? count : 0L;
    }

    @Override
    public void setTableComment(String tableName, String comment) {
        validateTableName(tableName);
        mapper.setTableComment(tableName, comment == null ? "" : comment.replace("'", "''"));
    }

    @Override
    public String getTableComment(String tableName) {
        validateTableName(tableName);
        return mapper.getTableComment(tableName);
    }

    @Override
    public List<String> listTemporaryTables(String prefix) {
        List<String> rows = mapper.listTemporaryTables(prefix);
        if (rows == null || rows.isEmpty()) {
            return Collections.emptyList();
        }
        return rows;
    }

    @Override
    public List<Map<String, Object>> query(String tableName, List<String> columns) {
        validateTableName(tableName);
        return mapper.query(tableName, columns);
    }

    private void validateTableName(String tableName) {
        if (tableName == null || !SAFE_TABLE_NAME.matcher(tableName).matches()) {
            throw new IllegalArgumentException("非法表名: " + tableName);
        }
    }
}

