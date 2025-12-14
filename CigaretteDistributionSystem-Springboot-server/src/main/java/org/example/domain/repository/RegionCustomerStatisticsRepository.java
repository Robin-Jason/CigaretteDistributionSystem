package org.example.domain.repository;

import org.example.shared.dto.RegionCustomerRecord;

import java.util.List;
import java.util.Map;

/**
 * 区域客户数统计表仓储接口（region_customer_statistics）
 */
public interface RegionCustomerStatisticsRepository {

    int deleteByYearMonthWeekSeq(Integer year, Integer month, Integer weekSeq);

    int batchUpsert(Integer year, Integer month, Integer weekSeq,
                    List<RegionCustomerRecord> list);

    Map<String, Object> findByRegion(Integer year, Integer month, Integer weekSeq, String regionName);

    /**
     * 查询指定分区的所有区域客户统计数据
     *
     * @param year    年份
     * @param month   月份
     * @param weekSeq 周序号
     * @return 区域客户统计数据列表
     */
    List<Map<String, Object>> findAll(Integer year, Integer month, Integer weekSeq);
}

