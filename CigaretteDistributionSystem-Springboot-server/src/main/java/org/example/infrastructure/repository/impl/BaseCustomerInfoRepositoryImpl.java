package org.example.infrastructure.repository.impl;

import lombok.RequiredArgsConstructor;
import org.example.domain.repository.BaseCustomerInfoRepository;
import org.example.infrastructure.persistence.mapper.BaseCustomerInfoMapper;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;

/**
 * {@link BaseCustomerInfoRepository} 的 MyBatis-Plus 实现。
 * <p>
 * 适配 {@code BaseCustomerInfoMapper} 提供数据访问。
 * </p>
 *
 * @author Robin
 * @version 1.0
 * @since 2025-12-12
 */
@Repository
@RequiredArgsConstructor
public class BaseCustomerInfoRepositoryImpl implements BaseCustomerInfoRepository {

    private final BaseCustomerInfoMapper baseCustomerInfoMapper;

    /**
     * 删除物理表（重建前使用）
     */
    @Override
    public void dropTable() {
        baseCustomerInfoMapper.dropTable();
    }

    /**
     * 动态建表，列定义与顺序由 Excel 解析后传入
     *
     * @param columnOrder           列顺序
     * @param columnDefinitions     列定义映射
     * @param defaultColumnDefinition 默认列定义
     */
    @Override
    public void createTable(List<String> columnOrder,
                            Map<String, String> columnDefinitions,
                            String defaultColumnDefinition) {
        baseCustomerInfoMapper.createTable(columnOrder, columnDefinitions, defaultColumnDefinition);
    }

    /**
     * 动态列插入一行数据
     *
     * @param columns 列名顺序
     * @param row     行数据
     * @return 影响行数
     */
    @Override
    public int insertRow(List<String> columns, Map<String, Object> row) {
        return baseCustomerInfoMapper.insertRow(columns, row);
    }

    /**
     * 诚信互助小组统计
     *
     * @return 统计结果
     */
    @Override
    public List<Map<String, Object>> selectGroupNameStatistics() {
        return baseCustomerInfoMapper.selectGroupNameStatistics();
    }
}

