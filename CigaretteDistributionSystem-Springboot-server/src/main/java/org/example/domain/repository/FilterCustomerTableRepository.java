package org.example.domain.repository;

import java.util.List;
import java.util.Map;

/**
 * 客户过滤表仓储接口（分区表）
 * <p>
 * 定义对 customer_filter 分区表数据的抽象访问方式。
 * </p>
 *
 * @author Robin
 * @version 2.0
 * @since 2025-12-12
 */
public interface FilterCustomerTableRepository {

    /**
     * 确保分区存在并插入数据到 customer_filter 分区表
     *
     * @param year        年份
     * @param month       月份
     * @param weekSeq     周序号
     * @param whereClause 查询条件（可为空，包含 WHERE 关键字）
     */
    void ensurePartitionAndInsertData(Integer year, Integer month, Integer weekSeq, String whereClause);
    
    /**
     * 截断分区数据（TRUNCATE PARTITION，比表截断更快且不会触发元数据锁）
     *
     * @param year    年份
     * @param month   月份
     * @param weekSeq 周序号
     */
    void truncatePartition(Integer year, Integer month, Integer weekSeq);
    
    /**
     * 统计分区记录数
     *
     * @param year    年份
     * @param month   月份
     * @param weekSeq 周序号
     * @return 记录数
     */
    Long countPartition(Integer year, Integer month, Integer weekSeq);
    
    /**
     * 查询分区数据
     *
     * @param year    年份
     * @param month   月份
     * @param weekSeq 周序号
     * @param columns 列名列表（为空则查询全部）
     * @return 结果列表
     */
    List<Map<String, Object>> queryPartition(Integer year, Integer month, Integer weekSeq, List<String> columns);
    
    /**
     * 扫描分区的 DISTINCT ORDER_CYCLE
     *
     * @param year    年份
     * @param month   月份
     * @param weekSeq 周序号
     * @return 订单周期列表
     */
    List<String> listOrderCyclesPartition(Integer year, Integer month, Integer weekSeq);
    
    /**
     * 按指定列取 DISTINCT 组合（忽略 NULL）
     *
     * @param year    年份
     * @param month   月份
     * @param weekSeq 周序号
     * @param columns 列名列表
     * @return 列值组合列表
     */
    List<Map<String, Object>> listDistinctCombinationsPartition(Integer year, Integer month, Integer weekSeq, List<String> columns);
    
    /**
     * 按条件统计 GRADE 分组客户数
     *
     * @param year              年份
     * @param month             月份
     * @param weekSeq           周序号
     * @param filters           列名->值的等值过滤
     * @param tagColumn         标签列名（可空）
     * @param tagOperator       标签比较符（可空，如 '=' 或 '<>'）
     * @param tagValue          标签值（可空）
     * @param orderCyclePattern 订单周期 LIKE 模式（可空，如 '单周%'）
     * @return 统计结果，包含 GRADE、CUSTOMER_COUNT
     */
    List<Map<String, Object>> statGradesPartition(Integer year, Integer month, Integer weekSeq,
                                         Map<String, String> filters,
                                         String tagColumn,
                                         String tagOperator,
                                         Object tagValue,
                                         String orderCyclePattern);
}

