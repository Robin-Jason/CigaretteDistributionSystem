package org.example.service;

import org.example.dto.DataImportRequestDto;

import java.util.Map;

/**
 * Excel导入服务接口
 *
 * 【核心功能】
 * - 解析、验证并导入 Excel 数据到分区表（按 YEAR/MONTH/WEEK_SEQ 的 RANGE 分区）
 * - 主要目标表：cigarette_distribution_info、region_customer_statistics、base_customer_info
 *
 * 【导入约定】
 * - 卷烟基础信息：写入 cigarette_distribution_info 分区表，对应分区键(year, month, weekSeq)
 * - 区域客户数：写入 region_customer_statistics 分区表，分区键同上
 * - 客户基础信息：写入 base_customer_info（非分区表），支持增量/覆盖
 *
 * 【导入策略】
 * - 导入前按时间分区清理/覆盖目标分区
 * - 事务安全：失败自动回滚
 * - 批量写入提升性能，并返回详细统计
 *
 * @since 2025-10-10
 */
public interface ExcelImportService {

    /**
     * 统一数据导入接口（客户表可选，卷烟表必传）。
     * 
     * @param request 导入请求，包含年份、月份、周序号及两个 Excel 文件（客户表可选）
     * @return 导入结果，包含 success、message、分表统计等信息
     */
    Map<String, Object> importData(DataImportRequestDto request);
}