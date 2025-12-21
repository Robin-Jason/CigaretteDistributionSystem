package org.example.application.service.query.impl;

import lombok.extern.slf4j.Slf4j;
import org.example.application.service.query.PartitionPredictionQueryService;
import org.example.domain.repository.CigaretteDistributionPredictionRepository;
import org.example.infrastructure.persistence.po.CigaretteDistributionPredictionPO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

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
    private CigaretteDistributionPredictionRepository predictionRepository;

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
     *     List<CigaretteDistributionPredictionPO> data = service.queryPredictionByTime(2025, 9, 3);
     *     // 返回: 2025年9月第3周的所有预测数据实体列表
     * </pre>
     */
    @Override
    public List<CigaretteDistributionPredictionPO> queryPredictionByTime(Integer year, Integer month, Integer weekSeq) {
        List<Map<String, Object>> rows = predictionRepository.findAll(year, month, weekSeq);
        List<CigaretteDistributionPredictionPO> result = new ArrayList<>();
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
        long count = predictionRepository.count(year, month, weekSeq);
        if (count == 0) {
            res.put("success", true);
            res.put("message", "未找到需要删除的数据");
            res.put("deletedCount", 0);
            return res;
        }
        int deleted = predictionRepository.deleteByYearMonthWeekSeq(year, month, weekSeq);
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
     * 将DAO层返回的Map数据转换为CigaretteDistributionPredictionPO实体对象。
     * </p>
     *
     * @param row Map行数据
     * @return 预测数据实体对象
     */
    private CigaretteDistributionPredictionPO mapRowToEntity(Map<String, Object> row) {
        CigaretteDistributionPredictionPO data = new CigaretteDistributionPredictionPO();
        data.setId(org.example.shared.util.WriteBackHelper.toInteger(row.get("id")));
        data.setCigCode((String) row.get("CIG_CODE"));
        data.setCigName((String) row.get("CIG_NAME"));
        data.setYear(org.example.shared.util.WriteBackHelper.toInteger(row.get("YEAR")));
        data.setMonth(org.example.shared.util.WriteBackHelper.toInteger(row.get("MONTH")));
        data.setWeekSeq(org.example.shared.util.WriteBackHelper.toInteger(row.get("WEEK_SEQ")));
        data.setDeliveryArea((String) row.get("DELIVERY_AREA"));
        data.setDeliveryMethod((String) row.get("DELIVERY_METHOD"));
        data.setDeliveryEtype((String) row.get("DELIVERY_ETYPE"));
        data.setD30(org.example.shared.util.WriteBackHelper.toBigDecimal(row.get("D30")));
        data.setD29(org.example.shared.util.WriteBackHelper.toBigDecimal(row.get("D29")));
        data.setD28(org.example.shared.util.WriteBackHelper.toBigDecimal(row.get("D28")));
        data.setD27(org.example.shared.util.WriteBackHelper.toBigDecimal(row.get("D27")));
        data.setD26(org.example.shared.util.WriteBackHelper.toBigDecimal(row.get("D26")));
        data.setD25(org.example.shared.util.WriteBackHelper.toBigDecimal(row.get("D25")));
        data.setD24(org.example.shared.util.WriteBackHelper.toBigDecimal(row.get("D24")));
        data.setD23(org.example.shared.util.WriteBackHelper.toBigDecimal(row.get("D23")));
        data.setD22(org.example.shared.util.WriteBackHelper.toBigDecimal(row.get("D22")));
        data.setD21(org.example.shared.util.WriteBackHelper.toBigDecimal(row.get("D21")));
        data.setD20(org.example.shared.util.WriteBackHelper.toBigDecimal(row.get("D20")));
        data.setD19(org.example.shared.util.WriteBackHelper.toBigDecimal(row.get("D19")));
        data.setD18(org.example.shared.util.WriteBackHelper.toBigDecimal(row.get("D18")));
        data.setD17(org.example.shared.util.WriteBackHelper.toBigDecimal(row.get("D17")));
        data.setD16(org.example.shared.util.WriteBackHelper.toBigDecimal(row.get("D16")));
        data.setD15(org.example.shared.util.WriteBackHelper.toBigDecimal(row.get("D15")));
        data.setD14(org.example.shared.util.WriteBackHelper.toBigDecimal(row.get("D14")));
        data.setD13(org.example.shared.util.WriteBackHelper.toBigDecimal(row.get("D13")));
        data.setD12(org.example.shared.util.WriteBackHelper.toBigDecimal(row.get("D12")));
        data.setD11(org.example.shared.util.WriteBackHelper.toBigDecimal(row.get("D11")));
        data.setD10(org.example.shared.util.WriteBackHelper.toBigDecimal(row.get("D10")));
        data.setD9(org.example.shared.util.WriteBackHelper.toBigDecimal(row.get("D9")));
        data.setD8(org.example.shared.util.WriteBackHelper.toBigDecimal(row.get("D8")));
        data.setD7(org.example.shared.util.WriteBackHelper.toBigDecimal(row.get("D7")));
        data.setD6(org.example.shared.util.WriteBackHelper.toBigDecimal(row.get("D6")));
        data.setD5(org.example.shared.util.WriteBackHelper.toBigDecimal(row.get("D5")));
        data.setD4(org.example.shared.util.WriteBackHelper.toBigDecimal(row.get("D4")));
        data.setD3(org.example.shared.util.WriteBackHelper.toBigDecimal(row.get("D3")));
        data.setD2(org.example.shared.util.WriteBackHelper.toBigDecimal(row.get("D2")));
        data.setD1(org.example.shared.util.WriteBackHelper.toBigDecimal(row.get("D1")));
        data.setBz((String) row.get("BZ"));
        data.setActualDelivery(org.example.shared.util.WriteBackHelper.toBigDecimal(row.get("ACTUAL_DELIVERY")));
        data.setDeployinfoCode((String) row.get("DEPLOYINFO_CODE"));
        data.setTag((String) row.get("TAG"));
        data.setTagFilterConfig(row.get("TAG_FILTER_CONFIG") != null ? row.get("TAG_FILTER_CONFIG").toString() : null);
        return data;
    }

}

