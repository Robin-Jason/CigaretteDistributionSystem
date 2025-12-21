package org.example.infrastructure.persistence.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Map;

/**
 * 客户过滤表 Mapper（分区表）
 * <p>用途：操作 customer_filter 分区表及其数据，SQL 见 resources/mapper/FilterCustomerTableMapper.xml。</p>
 *
 * @author Robin
 * @version 2.0
 * @since 2025-12-10
 */
@Mapper
public interface FilterCustomerTableMapper {

    /**
     * 确保分区存在并插入数据到 customer_filter 分区表（动态字段版本）
     *
     * @param year        年份
     * @param month       月份
     * @param weekSeq     周序号
     * @param whereClause 查询条件（可为空，包含 WHERE 关键字）
     * @param columns     要插入的字段列表（动态）
     */
    void ensurePartitionAndInsertDataDynamic(@Param("year") Integer year,
                                            @Param("month") Integer month,
                                            @Param("weekSeq") Integer weekSeq,
                                            @Param("whereClause") String whereClause,
                                            @Param("columns") List<String> columns);

    /**
     * 截断分区数据
     *
     * @param partitionName 分区名（如：p_20251003）
     */
    void truncatePartition(@Param("partitionName") String partitionName);

    /**
     * 统计分区记录数
     *
     * @param year    年份
     * @param month   月份
     * @param weekSeq 周序号
     * @return 记录数
     */
    Long countPartition(@Param("year") Integer year,
                       @Param("month") Integer month,
                       @Param("weekSeq") Integer weekSeq);

    /**
     * 查询分区数据
     *
     * @param year    年份
     * @param month   月份
     * @param weekSeq 周序号
     * @param columns 列名列表（为空则查询全部）
     * @return 结果列表
     */
    List<Map<String, Object>> queryPartition(@Param("year") Integer year,
                                              @Param("month") Integer month,
                                              @Param("weekSeq") Integer weekSeq,
                                              @Param("columns") List<String> columns);

    /**
     * 扫描分区的 DISTINCT ORDER_CYCLE
     *
     * @param year    年份
     * @param month   月份
     * @param weekSeq 周序号
     * @return 订单周期列表
     */
    List<String> listOrderCyclesPartition(@Param("year") Integer year,
                                         @Param("month") Integer month,
                                         @Param("weekSeq") Integer weekSeq);

    /**
     * 按指定列取 DISTINCT 组合（忽略 NULL）- 分区版本
     *
     * @param year    年份
     * @param month   月份
     * @param weekSeq 周序号
     * @param columns 列名列表
     * @return 列值组合列表
     */
    List<Map<String, Object>> listDistinctCombinationsPartition(@Param("year") Integer year,
                                                                 @Param("month") Integer month,
                                                                 @Param("weekSeq") Integer weekSeq,
                                                                 @Param("columns") List<String> columns);

    /**
     * 按条件统计 GRADE 分组客户数 - 分区版本
     *
     * @param year              年份
     * @param month            月份
     * @param weekSeq           周序号
     * @param filters           列名->值的等值过滤
     * @param tagColumn         标签列名（可空）
     * @param tagOperator       标签比较符（可空，如 '=' 或 '<>'）
     * @param tagValue          标签值（可空）
     * @param orderCyclePattern 订单周期 LIKE 模式（可空，如 '单周%'）
     * @return 统计结果，包含 GRADE、CUSTOMER_COUNT
     */
    List<Map<String, Object>> statGradesPartition(@Param("year") Integer year,
                                                   @Param("month") Integer month,
                                                   @Param("weekSeq") Integer weekSeq,
                                                   @Param("filters") Map<String, String> filters,
                                                   @Param("tagColumn") String tagColumn,
                                                   @Param("tagOperator") String tagOperator,
                                                   @Param("tagValue") Object tagValue,
                                                   @Param("orderCyclePattern") String orderCyclePattern);
}

