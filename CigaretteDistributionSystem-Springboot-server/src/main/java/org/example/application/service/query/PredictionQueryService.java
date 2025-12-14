package org.example.application.service.query;

import java.util.List;
import java.util.Map;

/**
 * 预测分区数据查询服务接口
 * <p>
 * 提供预测分区数据的查询功能，支持按时间分区查询预测表和价位段预测表。
 * 主要用于前端展示和数据导出功能。
 * </p>
 *
 * @author Robin
 * @version 1.0
 * @since 2025-12-12
 */
public interface PredictionQueryService {

    /**
     * 按时间分区查询并输出指定字段顺序。
     * <p>
     * 查询指定时间分区的预测数据，并按照预定义的字段顺序返回结果。
     * </p>
     *
     * @param year    年份
     * @param month   月份
     * @param weekSeq 周序号
     * @return 预测数据列表，每个Map包含有序的字段（cig_code, cig_name, deployinfo_code, delivery_method等）
     * @example
     * <pre>
     *     List<Map<String, Object>> data = service.listByTime(2025, 9, 3);
     *     // 返回: 2025年9月第3周的预测数据，字段按指定顺序排列
     * </pre>
     */
    List<Map<String, Object>> listByTime(Integer year, Integer month, Integer weekSeq);

    /**
     * 按时间分区查询价位段预测表，并输出指定字段顺序。
     * <p>
     * 查询指定时间分区的价位段预测数据，并按照预定义的字段顺序返回结果。
     * </p>
     *
     * @param year    年份
     * @param month   月份
     * @param weekSeq 周序号
     * @return 价位段预测数据列表，每个Map包含有序的字段
     * @example
     * <pre>
     *     List<Map<String, Object>> data = service.listPriceByTime(2025, 9, 3);
     *     // 返回: 2025年9月第3周的价位段预测数据，字段按指定顺序排列
     * </pre>
     */
    List<Map<String, Object>> listPriceByTime(Integer year, Integer month, Integer weekSeq);
}
