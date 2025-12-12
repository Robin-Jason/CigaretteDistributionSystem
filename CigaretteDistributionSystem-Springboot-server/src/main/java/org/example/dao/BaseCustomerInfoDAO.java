package org.example.dao;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 客户基础信息表DAO接口
 * 
 * 封装对base_customer_info表的所有数据库操作
 * 
 * @author System
 * @version 1.0
 * @since 2025-11-30
 */
public interface BaseCustomerInfoDAO {
    
    /**
     * 查询所有客户信息
     * 
     * @return 客户信息列表
     */
    List<Map<String, Object>> findAll();
    
    /**
     * 按ORDER_CYCLE查询客户信息，固定排除“不访销”。
     * 
     * @param orderCycles ORDER_CYCLE值列表
     * @return 客户信息列表（不包含“不访销”）
     */
    List<Map<String, Object>> findByOrderCycle(List<String> orderCycles);
    
    /**
     * 删除整张表
     */
    void dropTable();
    
    /**
     * 根据给定列顺序及定义重建表结构
     *
     * @param columnOrder 列顺序（已去重）
     * @param columnDefinitions 列定义映射
     * @param defaultColumnDefinition 默认列定义
     */
    void createTable(List<String> columnOrder,
                     Map<String, String> columnDefinitions,
                     String defaultColumnDefinition);
    
    /**
     * 插入一行客户数据
     *
     * @param columns 列顺序
     * @param row 行数据
     * @return 影响行数
     */
    int insertRow(List<String> columns, Map<String, Object> row);
    
    /**
     * 查询诚信互助小组聚合数据
     *
     * @return 结果列表
     */
    List<Map<String, Object>> findGroupNameStatistics();
    
    /**
     * 统计客户数量
     * 
     * @return 客户数量
     */
    long count();
    
    /**
     * 获取表的所有列名
     * 
     * @return 列名集合
     */
    Set<String> getColumnNames();
    
    /**
     * 检查表是否存在
     * 
     * @return true如果表存在
     */
    boolean tableExists();
}

