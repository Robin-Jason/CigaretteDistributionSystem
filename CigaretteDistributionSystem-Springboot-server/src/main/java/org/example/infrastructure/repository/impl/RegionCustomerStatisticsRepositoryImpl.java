package org.example.infrastructure.repository.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.shared.dto.RegionCustomerRecord;
import org.example.domain.repository.RegionCustomerStatisticsRepository;
import org.example.infrastructure.persistence.mapper.RegionCustomerStatisticsMapper;
import org.example.shared.util.PartitionTableManager;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;

/**
 * 基于 MyBatis-Plus Mapper 的区域客户统计仓储实现。
 * <p>
 * 负责封装分区表管理、批量处理、日志记录等横切关注点。
 * </p>
 *
 * @author Robin
 * @version 1.0
 * @since 2025-12-12
 */
@Slf4j
@Repository
@RequiredArgsConstructor
public class RegionCustomerStatisticsRepositoryImpl implements RegionCustomerStatisticsRepository {

    private static final String TABLE_NAME = "region_customer_statistics";
    private static final int BATCH_SIZE = 1000;

    private final RegionCustomerStatisticsMapper regionCustomerStatisticsMapper;
    private final PartitionTableManager partitionTableManager;

    /**
     * 删除指定年月周的所有区域客户统计
     *
     * @param year    年份
     * @param month   月份
     * @param weekSeq 周序号
     * @return 删除的记录数
     */
    @Override
    public int deleteByYearMonthWeekSeq(Integer year, Integer month, Integer weekSeq) {
        log.debug("删除区域客户统计: {}-{}-{}", year, month, weekSeq);
        int count = regionCustomerStatisticsMapper.deleteByYearMonthWeekSeq(year, month, weekSeq);
        log.info("删除区域客户统计完成: {}-{}-{}, 删除 {} 条记录", year, month, weekSeq, count);
        return count;
    }

    /**
     * 批量 UPSERT 区域客户统计记录
     * <p>
     * 支持分批处理，每批最多 BATCH_SIZE 条记录。
     * </p>
     *
     * @param year    年份
     * @param month   月份
     * @param weekSeq 周序号
     * @param list    记录列表
     * @return 插入的记录数
     */
    @Override
    public int batchUpsert(Integer year, Integer month, Integer weekSeq,
                           List<RegionCustomerRecord> list) {
        if (list == null || list.isEmpty()) {
            log.debug("批量UPSERT区域客户统计: {}-{}-{}, 记录列表为空", year, month, weekSeq);
            return 0;
        }

        // 确保分区存在
        partitionTableManager.ensurePartitionExists(TABLE_NAME, year, month, weekSeq);

        // 分批处理
        int totalInserted = 0;
        int totalSize = list.size();
        for (int offset = 0; offset < totalSize; offset += BATCH_SIZE) {
            int endIndex = Math.min(offset + BATCH_SIZE, totalSize);
            List<RegionCustomerRecord> batch = list.subList(offset, endIndex);
            int count = regionCustomerStatisticsMapper.batchUpsert(year, month, weekSeq, batch);
            totalInserted += count;
        }

        log.info("批量UPSERT区域客户统计完成: {}-{}-{}, 插入 {} 条记录", year, month, weekSeq, totalInserted);
        return totalInserted;
    }

    /**
     * 按区域查询一条记录
     *
     * @param year       年份
     * @param month      月份
     * @param weekSeq    周序号
     * @param regionName 区域名称
     * @return 客户统计Map，如果不存在返回null
     */
    @Override
    public Map<String, Object> findByRegion(Integer year, Integer month, Integer weekSeq, String regionName) {
        // 确保分区存在
        partitionTableManager.ensurePartitionExists(TABLE_NAME, year, month, weekSeq);
        return regionCustomerStatisticsMapper.findByRegion(year, month, weekSeq, regionName);
    }

    /**
     * 查询指定分区的所有区域客户统计数据
     *
     * @param year    年份
     * @param month   月份
     * @param weekSeq 周序号
     * @return 区域客户统计数据列表
     */
    @Override
    public List<Map<String, Object>> findAll(Integer year, Integer month, Integer weekSeq) {
        // 确保分区存在
        partitionTableManager.ensurePartitionExists(TABLE_NAME, year, month, weekSeq);
        List<Map<String, Object>> result = regionCustomerStatisticsMapper.findAll(year, month, weekSeq);
        log.debug("查询区域客户统计: {}-{}-{}, 返回 {} 条记录", year, month, weekSeq, result.size());
        return result;
    }
}

