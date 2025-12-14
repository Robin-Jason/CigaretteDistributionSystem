package org.example.domain.repository;

import java.util.List;
import java.util.Map;

/**
 * 临时客户表仓储接口
 * <p>
 * 定义对临时客户表数据的抽象访问方式。
 * </p>
 *
 * @author Robin
 * @version 1.0
 * @since 2025-12-12
 */
public interface TemporaryCustomerTableRepository {

    /**
     * 基于 base_customer_info 创建临时表
     *
     * @param tableName   临时表名
     * @param whereClause 查询条件（可为空，包含 WHERE 关键字）
     */
    void createTableFromBase(String tableName, String whereClause);

    /**
     * 创建与 base_customer_info 结构相同的空临时表
     *
     * @param tableName 临时表名
     */
    void createEmptyTable(String tableName);

    /**
     * 判断临时表是否存在
     *
     * @param tableName 临时表名
     * @return 1 存在，0 不存在
     */
    Long tableExists(String tableName);

    /**
     * 删除临时表
     *
     * @param tableName 临时表名
     */
    void dropTable(String tableName);

    /**
     * 统计临时表记录数
     *
     * @param tableName 临时表名
     * @return 记录数
     */
    Long count(String tableName);

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
     * 列出符合前缀的临时表
     *
     * @param prefix 表名前缀
     * @return 表名列表
     */
    List<String> listTemporaryTables(String prefix);

    /**
     * 查询临时表数据
     *
     * @param tableName 临时表名
     * @param columns   列名列表（为空则查询全部）
     * @return 结果列表
     */
    List<Map<String, Object>> query(String tableName, List<String> columns);

    /**
     * 扫描临时表的 DISTINCT ORDER_CYCLE
     *
     * @param tableName 临时表名
     * @return 订单周期列表
     */
    List<String> listOrderCycles(String tableName);

    /**
     * 按指定列取 DISTINCT 组合（忽略 NULL）
     *
     * @param tableName 临时表名
     * @param columns   列名列表
     * @return 列值组合列表
     */
    List<Map<String, Object>> listDistinctCombinations(String tableName, List<String> columns);

    /**
     * 按条件统计 GRADE 分组客户数
     *
     * @param tableName         临时表名
     * @param filters           列名->值的等值过滤
     * @param tagColumn         标签列名（可空）
     * @param tagOperator       标签比较符（可空，如 '=' 或 '<>'）
     * @param tagValue          标签值（可空）
     * @param orderCyclePattern 订单周期 LIKE 模式（可空，如 '单周%'）
     * @return 统计结果，包含 GRADE、CUSTOMER_COUNT
     */
    List<Map<String, Object>> statGrades(String tableName,
                                         Map<String, String> filters,
                                         String tagColumn,
                                         String tagOperator,
                                         Object tagValue,
                                         String orderCyclePattern);
}

