package org.example.application.service.encode;

import java.util.List;

/**
 * 单卷烟多区域投放聚合编码查询服务（面向前端懒加载）。
 *
 * <p>用于前端在点击某支卷烟时，按批次（year/month/weekSeq）返回该卷烟的聚合编码表达式列表。</p>
 *
 * @author Robin
 * @since 2025-12-17
 */
public interface AggregatedEncodingQueryService {

    /**
     * 查询并生成指定卷烟在指定批次下的“多区域聚合编码表达式”。
     *
     * @param year    年份
     * @param month   月份
     * @param weekSeq 周序号
     * @param cigCode 卷烟代码
     * @return 聚合编码表达式列表（按稳定排序输出）；无数据时返回空列表
     *
     * @example GET /api/prediction/aggregated-encodings?year=2025&month=9&weekSeq=3&cigCode=42010020
     */
    List<String> listAggregatedEncodings(Integer year, Integer month, Integer weekSeq, String cigCode);
}


