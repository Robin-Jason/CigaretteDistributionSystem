package org.example.infrastructure.persistence.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.example.shared.dto.RegionCustomerRecord;

import java.util.List;
import java.util.Map;

/**
 * 区域客户统计表 Mapper（表：region_customer_statistics）
 * <p>用途：分区表的查询、批量 UPSERT、删除、计数等操作。</p>
 *
 * @author Robin
 * @version 1.0
 * @since 2025-12-10
 */
@Mapper
public interface RegionCustomerStatisticsMapper {

    /**
     * 查询指定分区全部记录
     *
     * @param year 年
     * @param month 月
     * @param weekSeq 周序
     * @return 结果列表（包含 REGION, D30-D1, TOTAL）
     */
    List<Map<String, Object>> findAll(@Param("year") Integer year,
                                      @Param("month") Integer month,
                                      @Param("weekSeq") Integer weekSeq);

    /**
     * 按区域查询一条记录
     *
     * @param year 年
     * @param month 月
     * @param weekSeq 周序
     * @param region 区域
     * @return 单条记录（REGION, D30-D1, TOTAL），无则返回 null
     */
    Map<String, Object> findByRegion(@Param("year") Integer year,
                                     @Param("month") Integer month,
                                     @Param("weekSeq") Integer weekSeq,
                                     @Param("region") String region);

    /**
     * 批量 UPSERT（多值 INSERT ... ON DUPLICATE KEY UPDATE）
     *
     * @param year 年
     * @param month 月
     * @param weekSeq 周序
     * @param records 记录列表
     * @return 影响行数
     */
    int batchUpsert(@Param("year") Integer year,
                    @Param("month") Integer month,
                    @Param("weekSeq") Integer weekSeq,
                    @Param("records") List<RegionCustomerRecord> records);

    /**
     * 按年月周删除
     *
     * @param year 年
     * @param month 月
     * @param weekSeq 周序
     * @return 删除行数
     */
    int deleteByYearMonthWeekSeq(@Param("year") Integer year,
                                 @Param("month") Integer month,
                                 @Param("weekSeq") Integer weekSeq);

    /**
     * 按区域删除
     *
     * @param year 年
     * @param month 月
     * @param weekSeq 周序
     * @param region 区域
     * @return 删除行数
     */
    int deleteByRegion(@Param("year") Integer year,
                       @Param("month") Integer month,
                       @Param("weekSeq") Integer weekSeq,
                       @Param("region") String region);

    /**
     * 计数
     *
     * @param year 年
     * @param month 月
     * @param weekSeq 周序
     * @return 记录数
     */
    Long count(@Param("year") Integer year,
               @Param("month") Integer month,
               @Param("weekSeq") Integer weekSeq);
}


