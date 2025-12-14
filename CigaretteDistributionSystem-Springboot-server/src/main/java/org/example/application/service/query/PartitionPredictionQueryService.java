package org.example.application.service.query;

import org.example.infrastructure.persistence.po.CigaretteDistributionPredictionPO;

import java.util.List;
import java.util.Map;

/**
 * 分区预测数据查询服务接口
 * <p>
 * 面向计算服务的内部调用，提供分区预测数据的查询和删除功能。
 * 主要用于一键生成分配方案流程中的预测数据管理。
 * </p>
 *
 * @author Robin
 * @version 1.0
 * @since 2025-12-12
 */
public interface PartitionPredictionQueryService {

    /**
     * 按时间查询 prediction 分区表，返回实体列表。
     * <p>
     * 根据年份、月份、周序号查询对应分区的预测数据。
     * </p>
     *
     * @param year    年份
     * @param month   月份
     * @param weekSeq 周序号
     * @return 预测数据实体列表
     * @example
     * <pre>
     *     List<CigaretteDistributionPredictionPO> data = service.queryPredictionByTime(2025, 9, 3);
     *     // 返回: 2025年9月第3周的所有预测数据
     * </pre>
     */
    List<CigaretteDistributionPredictionPO> queryPredictionByTime(Integer year, Integer month, Integer weekSeq);

    /**
     * 按时间删除 prediction 分区表数据，返回删除统计。
     * <p>
     * 删除指定时间分区的所有预测数据，并返回删除结果统计信息。
     * </p>
     *
     * @param year    年份
     * @param month   月份
     * @param weekSeq 周序号
     * @return 删除结果Map，包含success、message、deletedCount等字段
     * @example
     * <pre>
     *     Map<String, Object> result = service.deletePredictionByTime(2025, 9, 3);
     *     // 返回: {"success": true, "message": "删除成功", "deletedCount": 100}
     * </pre>
     */
    Map<String, Object> deletePredictionByTime(Integer year, Integer month, Integer weekSeq);
}
