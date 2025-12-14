package org.example.domain.repository;

import java.util.List;
import java.util.Map;

/**
 * 客户基础信息仓储接口
 * <p>
 * 定义对 {@code base_customer_info} 数据的抽象访问方式。
 * </p>
 *
 * @author Robin
 * @version 1.0
 * @since 2025-12-12
 */
public interface BaseCustomerInfoRepository {

    /**
     * 删除物理表（重建前使用）
     */
    void dropTable();

    /**
     * 动态建表，列定义与顺序由 Excel 解析后传入
     *
     * @param columnOrder           列顺序
     * @param columnDefinitions     列定义映射
     * @param defaultColumnDefinition 默认列定义
     */
    void createTable(List<String> columnOrder,
                     Map<String, String> columnDefinitions,
                     String defaultColumnDefinition);

    /**
     * 动态列插入一行数据
     *
     * @param columns 列名顺序
     * @param row     行数据
     * @return 影响行数
     */
    int insertRow(List<String> columns, Map<String, Object> row);

    /**
     * 诚信互助小组统计
     *
     * @return 统计结果
     */
    List<Map<String, Object>> selectGroupNameStatistics();
}

