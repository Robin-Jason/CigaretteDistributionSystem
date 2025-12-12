package org.example.service.importer;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.entity.CigaretteDistributionInfoData;
import org.example.mapper.CigaretteDistributionInfoMapper;
import org.example.util.PartitionTableManager;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * 卷烟投放基础信息写入器：清分区 + 批量 upsert。
 *
 * <p>职责：确保分区存在、清分区、批量 upsert 卷烟投放基础信息。</p>
 *
 * @author Robin
 * @version 1.0
 * @since 2025-12-11
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CigaretteInfoWriter {

    private final CigaretteDistributionInfoMapper cigaretteDistributionInfoMapper;
    private final PartitionTableManager partitionTableManager;

    /**
     * 写入卷烟投放基础信息到指定分区（清空旧数据）。
     *
     * @param data    行数据列表
     * @param year    年份
     * @param month   月份
     * @param weekSeq 周序号
     * @return 插入条数
     * @example year=2025,month=9,weekSeq=3 时写入分区 cigarette_distribution_info_p20250903
     */
    public int writeToPartition(List<Map<String, Object>> data,
                                Integer year, Integer month, Integer weekSeq) {
        partitionTableManager.ensurePartitionExists("cigarette_distribution_info", year, month, weekSeq);

        QueryWrapper<CigaretteDistributionInfoData> qw = new QueryWrapper<>();
        qw.eq("YEAR", year).eq("MONTH", month).eq("WEEK_SEQ", weekSeq);
        int deleted = cigaretteDistributionInfoMapper.delete(qw);
        log.info("清空卷烟投放基础信息分区: {}-{}-{}, 删除 {} 条旧记录", year, month, weekSeq, deleted);

        List<CigaretteDistributionInfoData> dataList = new ArrayList<>();
        for (Map<String, Object> row : data) {
            CigaretteDistributionInfoData infoData = mapRow(row);
            if (infoData != null) {
                infoData.setYear(year);
                infoData.setMonth(month);
                infoData.setWeekSeq(weekSeq);
                dataList.add(infoData);
            }
        }
        if (dataList.isEmpty()) {
            return 0;
        }
        int inserted = cigaretteDistributionInfoMapper.batchUpsert(dataList);
        log.info("导入卷烟投放基础信息到分区表完成: {}-{}-{}, 插入 {} 条记录", year, month, weekSeq, inserted);
        return inserted;
    }

    private CigaretteDistributionInfoData mapRow(Map<String, Object> row) {
        CigaretteDistributionInfoData data = new CigaretteDistributionInfoData();
        data.setCigCode(getString(row, "CIG_CODE"));
        data.setCigName(getString(row, "CIG_NAME"));
        data.setDeliveryMethod(getString(row, "DELIVERY_METHOD"));
        data.setDeliveryEtype(getString(row, "DELIVERY_ETYPE"));
        data.setTag(getString(row, "TAG"));
        data.setDeliveryArea(getString(row, "DELIVERY_AREA"));
        data.setBz(getString(row, "BZ"));
        data.setAdv(getBigDecimal(row, "ADV"));
        data.setUrs(getBigDecimal(row, "URS"));
        data.setSupplyAttribute(getString(row, "SUPPLY_ATTRIBUTE"));
        data.setTagFilterConfig(getString(row, "TAG_FILTER_CONFIG"));
        return data;
    }

    private String getString(Map<String, Object> row, String key) {
        Object value = row.get(key);
        if (value == null) {
            for (Map.Entry<String, Object> entry : row.entrySet()) {
                if (Objects.equals(entry.getKey(), key) || entry.getKey().equalsIgnoreCase(key)) {
                    value = entry.getValue();
                    break;
                }
            }
        }
        return value != null ? value.toString().trim() : null;
    }

    private BigDecimal getBigDecimal(Map<String, Object> row, String key) {
        Object value = row.get(key);
        if (value == null) {
            for (Map.Entry<String, Object> entry : row.entrySet()) {
                if (entry.getKey().equalsIgnoreCase(key)) {
                    value = entry.getValue();
                    break;
                }
            }
        }
        if (value == null) {
            return null;
        }
        if (value instanceof BigDecimal) {
            return (BigDecimal) value;
        }
        if (value instanceof Number) {
            return BigDecimal.valueOf(((Number) value).doubleValue());
        }
        try {
            return new BigDecimal(value.toString());
        } catch (NumberFormatException ex) {
            return null;
        }
    }
}

