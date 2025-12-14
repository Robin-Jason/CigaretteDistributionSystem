package org.example.application.service;

import java.util.Map;

/**
 * 区域客户数统计表构建服务接口（原三层结构恢复用）
 */
public interface RegionCustomerStatisticsBuildService {

    /**
     * 构建全量区域客户数表
     *
     * @param year    年份
     * @param month   月份
     * @param weekSeq 周序号
     * @param temporaryTableName 临时表名
     * @return 构建结果
     */
    Map<String, Object> buildRegionCustomerStatistics(Integer year, Integer month, Integer weekSeq, String temporaryTableName);
}

