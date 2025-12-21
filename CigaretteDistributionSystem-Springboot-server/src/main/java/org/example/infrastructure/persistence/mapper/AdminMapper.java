package org.example.infrastructure.persistence.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Map;

/**
 * 管理类 Mapper：包含对 information_schema、DDL/分区等管理 SQL 的封装。
 * 目的是替换原来使用 JdbcTemplate 的实现，统一使用 MyBatis 执行管理类 SQL。
 */
@Mapper
public interface AdminMapper {

    /**
     * 列出表的列信息（返回 information_schema.COLUMNS 行）
     */
    List<Map<String, Object>> listTableColumns(@Param("tableName") String tableName);

    /**
     * 执行任意 SQL（主要用于 DDL/ALTER 等无返回值的语句）
     */
    void executeSql(@Param("sql") String sql);

    /**
     * 列出表的分区名（按顺序）
     */
    List<String> listPartitions(@Param("tableName") String tableName);

    /**
     * 统计指定分区是否存在
     */
    Integer countPartition(@Param("tableName") String tableName, @Param("partitionName") String partitionName);

    /**
     * 查询最大分区描述（PARTITION_DESCRIPTION）
     */
    String getMaxPartitionDescription(@Param("tableName") String tableName);

    // ===================== 事务监控相关方法 =====================

    /**
     * 查询长时间运行的事务（运行时间超过指定秒数）
     *
     * @param thresholdSeconds 阈值秒数
     * @return 长时间运行的事务列表
     */
    List<Map<String, Object>> listLongRunningTransactions(@Param("thresholdSeconds") int thresholdSeconds);

    /**
     * 查询等待元数据锁的连接
     *
     * @return 等待元数据锁的连接列表
     */
    List<Map<String, Object>> listMetadataLockWaits();

    /**
     * 统计长时间运行的事务数量
     *
     * @param thresholdSeconds 阈值秒数
     * @return 长时间运行的事务数量
     */
    Integer countLongRunningTransactions(@Param("thresholdSeconds") int thresholdSeconds);

    /**
     * 统计等待元数据锁的连接数量
     *
     * @return 等待元数据锁的连接数量
     */
    Integer countMetadataLockWaits();
}


