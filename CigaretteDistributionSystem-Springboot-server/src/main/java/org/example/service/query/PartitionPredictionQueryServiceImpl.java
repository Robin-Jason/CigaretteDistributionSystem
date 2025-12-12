package org.example.service.query;

import lombok.extern.slf4j.Slf4j;
import org.example.dao.CigaretteDistributionPredictionDAO;
import org.example.entity.CigaretteDistributionPredictionData;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 分区预测数据查询服务实现类
 * <p>
 * 供计算服务内部使用，提供分区预测数据的查询和删除功能。
 * 负责将DAO层返回的Map数据转换为实体对象。
 * </p>
 *
 * @author Robin
 * @version 1.0
 * @since 2025-12-12
 */
@Slf4j
@Service
public class PartitionPredictionQueryServiceImpl implements PartitionPredictionQueryService {

    @Autowired
    private CigaretteDistributionPredictionDAO predictionDAO;

    /**
     * 按时间查询 prediction 分区表，返回实体列表。
     * <p>
     * 根据年份、月份、周序号查询对应分区的预测数据，并将Map结果转换为实体对象。
     * </p>
     *
     * @param year    年份
     * @param month   月份
     * @param weekSeq 周序号
     * @return 预测数据实体列表
     * @example
     * <pre>
     *     List<CigaretteDistributionPredictionData> data = service.queryPredictionByTime(2025, 9, 3);
     *     // 返回: 2025年9月第3周的所有预测数据实体列表
     * </pre>
     */
    @Override
    public List<CigaretteDistributionPredictionData> queryPredictionByTime(Integer year, Integer month, Integer weekSeq) {
        List<Map<String, Object>> rows = predictionDAO.findAll(year, month, weekSeq);
        List<CigaretteDistributionPredictionData> result = new ArrayList<>();
        for (Map<String, Object> row : rows) {
            result.add(mapRowToEntity(row));
        }
        return result;
    }

    /**
     * 按时间删除 prediction 分区表数据，返回删除统计。
     * <p>
     * 删除指定时间分区的所有预测数据，并返回删除结果统计信息。
     * 如果分区中没有数据，则返回成功但删除数量为0。
     * </p>
     *
     * @param year    年份
     * @param month   月份
     * @param weekSeq 周序号
     * @return 删除结果Map，包含success、message、deletedCount、year、month、weekSeq等字段
     * @example
     * <pre>
     *     Map<String, Object> result = service.deletePredictionByTime(2025, 9, 3);
     *     // 返回: {"success": true, "message": "删除成功", "deletedCount": 100, "year": 2025, "month": 9, "weekSeq": 3}
     * </pre>
     */
    @Override
    public Map<String, Object> deletePredictionByTime(Integer year, Integer month, Integer weekSeq) {
        Map<String, Object> res = new HashMap<>();
        long count = predictionDAO.count(year, month, weekSeq);
        if (count == 0) {
            res.put("success", true);
            res.put("message", "未找到需要删除的数据");
            res.put("deletedCount", 0);
            return res;
        }
        int deleted = predictionDAO.deleteByYearMonthWeekSeq(year, month, weekSeq);
        res.put("success", true);
        res.put("message", "删除成功");
        res.put("deletedCount", deleted);
        res.put("year", year);
        res.put("month", month);
        res.put("weekSeq", weekSeq);
        return res;
    }

    /**
     * 将Map行数据转换为实体对象。
     * <p>
     * 将DAO层返回的Map数据转换为CigaretteDistributionPredictionData实体对象。
     * </p>
     *
     * @param row Map行数据
     * @return 预测数据实体对象
     */
    private CigaretteDistributionPredictionData mapRowToEntity(Map<String, Object> row) {
        CigaretteDistributionPredictionData data = new CigaretteDistributionPredictionData();
        data.setId(extractInteger(row.get("id")));
        data.setCigCode((String) row.get("CIG_CODE"));
        data.setCigName((String) row.get("CIG_NAME"));
        data.setYear(extractInteger(row.get("YEAR")));
        data.setMonth(extractInteger(row.get("MONTH")));
        data.setWeekSeq(extractInteger(row.get("WEEK_SEQ")));
        data.setDeliveryArea((String) row.get("DELIVERY_AREA"));
        data.setDeliveryMethod((String) row.get("DELIVERY_METHOD"));
        data.setDeliveryEtype((String) row.get("DELIVERY_ETYPE"));
        data.setD30(getBigDecimal(row.get("D30")));
        data.setD29(getBigDecimal(row.get("D29")));
        data.setD28(getBigDecimal(row.get("D28")));
        data.setD27(getBigDecimal(row.get("D27")));
        data.setD26(getBigDecimal(row.get("D26")));
        data.setD25(getBigDecimal(row.get("D25")));
        data.setD24(getBigDecimal(row.get("D24")));
        data.setD23(getBigDecimal(row.get("D23")));
        data.setD22(getBigDecimal(row.get("D22")));
        data.setD21(getBigDecimal(row.get("D21")));
        data.setD20(getBigDecimal(row.get("D20")));
        data.setD19(getBigDecimal(row.get("D19")));
        data.setD18(getBigDecimal(row.get("D18")));
        data.setD17(getBigDecimal(row.get("D17")));
        data.setD16(getBigDecimal(row.get("D16")));
        data.setD15(getBigDecimal(row.get("D15")));
        data.setD14(getBigDecimal(row.get("D14")));
        data.setD13(getBigDecimal(row.get("D13")));
        data.setD12(getBigDecimal(row.get("D12")));
        data.setD11(getBigDecimal(row.get("D11")));
        data.setD10(getBigDecimal(row.get("D10")));
        data.setD9(getBigDecimal(row.get("D9")));
        data.setD8(getBigDecimal(row.get("D8")));
        data.setD7(getBigDecimal(row.get("D7")));
        data.setD6(getBigDecimal(row.get("D6")));
        data.setD5(getBigDecimal(row.get("D5")));
        data.setD4(getBigDecimal(row.get("D4")));
        data.setD3(getBigDecimal(row.get("D3")));
        data.setD2(getBigDecimal(row.get("D2")));
        data.setD1(getBigDecimal(row.get("D1")));
        data.setBz((String) row.get("BZ"));
        data.setActualDelivery(getBigDecimal(row.get("ACTUAL_DELIVERY")));
        data.setDeployinfoCode((String) row.get("DEPLOYINFO_CODE"));
        data.setTag((String) row.get("TAG"));
        data.setTagFilterConfig(row.get("TAG_FILTER_CONFIG") != null ? row.get("TAG_FILTER_CONFIG").toString() : null);
        return data;
    }

    /**
     * 提取整数类型值。
     * <p>
     * 从对象中提取整数值，支持多种数字类型转换。
     * </p>
     *
     * @param obj 原始对象
     * @return 整数值，如果无法转换则返回 null
     */
    private Integer extractInteger(Object obj) {
        if (obj == null) return null;
        if (obj instanceof Integer) return (Integer) obj;
        if (obj instanceof Number) return ((Number) obj).intValue();
        try {
            return Integer.parseInt(obj.toString());
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * 提取BigDecimal类型值。
     * <p>
     * 从对象中提取BigDecimal值，支持多种数字类型转换。
     * </p>
     *
     * @param obj 原始对象
     * @return BigDecimal值，如果无法转换则返回 null
     */
    private BigDecimal getBigDecimal(Object obj) {
        if (obj == null) return null;
        if (obj instanceof BigDecimal) return (BigDecimal) obj;
        if (obj instanceof Number) return new BigDecimal(obj.toString());
        try {
            return new BigDecimal(obj.toString());
        } catch (Exception e) {
            return null;
        }
    }
}
