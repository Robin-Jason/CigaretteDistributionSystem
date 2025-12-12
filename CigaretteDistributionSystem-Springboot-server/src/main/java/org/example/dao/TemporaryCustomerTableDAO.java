package org.example.dao;

import java.util.List;
import java.util.Map;

/**
 * 临时客户表 DAO
 *
 * 统一封装所有对临时客户表的数据库访问操作，
 * 避免在 Service 层直接使用 JdbcTemplate。
 */
public interface TemporaryCustomerTableDAO {

    /**
     * 基于 base_customer_info 创建临时表
     *
     * @param tableName   临时表名
     * @param whereClause 查询条件（包含 WHERE 关键字）
     */
    void createTableFromBase(String tableName, String whereClause);

    /**
     * 创建一个结构与 base_customer_info 相同的空临时表
     *
     * @param tableName 临时表名
     */
    void createEmptyTable(String tableName);

    /**
     * 判断临时表是否存在
     *
     * @param tableName 临时表名
     * @return true 表示存在
     */
    boolean tableExists(String tableName);

    /**
     * 删除临时表
     *
     * @param tableName 临时表名
     */
    void dropTable(String tableName);

    /**
     * 查询临时表记录数
     *
     * @param tableName 临时表名
     * @return 记录数
     */
    long count(String tableName);

    /**
     * 设置表注释
     *
     * @param tableName 临时表名
     * @param comment   注释内容
     */
    void setTableComment(String tableName, String comment);

    /**
     * 获取表注释
     *
     * @param tableName 临时表名
     * @return 注释内容
     */
    String getTableComment(String tableName);

    /**
     * 列出所有符合前缀的临时表
     *
     * @param prefix 表名前缀
     * @return 表名列表
     */
    List<String> listTemporaryTables(String prefix);

    /**
     * 查询临时表数据
     *
     * @param tableName 临时表名
     * @param columns   列表
     * @return 查询结果
     */
    List<Map<String, Object>> query(String tableName, List<String> columns);
}

