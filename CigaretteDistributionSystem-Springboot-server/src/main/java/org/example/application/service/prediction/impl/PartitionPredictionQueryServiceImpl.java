package org.example.application.service.prediction.impl;

import lombok.extern.slf4j.Slf4j;
import org.example.application.service.prediction.PartitionPredictionQueryService;
import org.example.domain.repository.CigaretteDistributionPredictionRepository;
import org.example.infrastructure.persistence.po.CigaretteDistributionPredictionPO;
import org.example.shared.util.WriteBackHelper;
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

    @Override
    public List<CigaretteDistributionPredictionPO> queryPredictionByTime(Integer year, Integer month, Integer weekSeq) {
        List<Map<String, Object>> rows = predictionRepository.findAll(year, month, weekSeq);
        List<CigaretteDistributionPredictionPO> result = new ArrayList<>();
        for (Map<String, Object> row : rows) {
            result.add(mapRowToEntity(row));
        }
        return result;
    }

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

    private CigaretteDistributionPredictionPO mapRowToEntity(Map<String, Object> row) {
        CigaretteDistributionPredictionPO data = new CigaretteDistributionPredictionPO();
        data.setId(WriteBackHelper.toInteger(row.get("id")));
        data.setCigCode((String) row.get("CIG_CODE"));
        data.setCigName((String) row.get("CIG_NAME"));
        data.setYear(WriteBackHelper.toInteger(row.get("YEAR")));
        data.setMonth(WriteBackHelper.toInteger(row.get("MONTH")));
        data.setWeekSeq(WriteBackHelper.toInteger(row.get("WEEK_SEQ")));
        data.setDeliveryArea((String) row.get("DELIVERY_AREA"));
        data.setDeliveryMethod((String) row.get("DELIVERY_METHOD"));
        data.setDeliveryEtype((String) row.get("DELIVERY_ETYPE"));
        data.setD30(WriteBackHelper.toBigDecimal(row.get("D30")));
        data.setD29(WriteBackHelper.toBigDecimal(row.get("D29")));
        data.setD28(WriteBackHelper.toBigDecimal(row.get("D28")));
        data.setD27(WriteBackHelper.toBigDecimal(row.get("D27")));
        data.setD26(WriteBackHelper.toBigDecimal(row.get("D26")));
        data.setD25(WriteBackHelper.toBigDecimal(row.get("D25")));
        data.setD24(WriteBackHelper.toBigDecimal(row.get("D24")));
        data.setD23(WriteBackHelper.toBigDecimal(row.get("D23")));
        data.setD22(WriteBackHelper.toBigDecimal(row.get("D22")));
        data.setD21(WriteBackHelper.toBigDecimal(row.get("D21")));
        data.setD20(WriteBackHelper.toBigDecimal(row.get("D20")));
        data.setD19(WriteBackHelper.toBigDecimal(row.get("D19")));
        data.setD18(WriteBackHelper.toBigDecimal(row.get("D18")));
        data.setD17(WriteBackHelper.toBigDecimal(row.get("D17")));
        data.setD16(WriteBackHelper.toBigDecimal(row.get("D16")));
        data.setD15(WriteBackHelper.toBigDecimal(row.get("D15")));
        data.setD14(WriteBackHelper.toBigDecimal(row.get("D14")));
        data.setD13(WriteBackHelper.toBigDecimal(row.get("D13")));
        data.setD12(WriteBackHelper.toBigDecimal(row.get("D12")));
        data.setD11(WriteBackHelper.toBigDecimal(row.get("D11")));
        data.setD10(WriteBackHelper.toBigDecimal(row.get("D10")));
        data.setD9(WriteBackHelper.toBigDecimal(row.get("D9")));
        data.setD8(WriteBackHelper.toBigDecimal(row.get("D8")));
        data.setD7(WriteBackHelper.toBigDecimal(row.get("D7")));
        data.setD6(WriteBackHelper.toBigDecimal(row.get("D6")));
        data.setD5(WriteBackHelper.toBigDecimal(row.get("D5")));
        data.setD4(WriteBackHelper.toBigDecimal(row.get("D4")));
        data.setD3(WriteBackHelper.toBigDecimal(row.get("D3")));
        data.setD2(WriteBackHelper.toBigDecimal(row.get("D2")));
        data.setD1(WriteBackHelper.toBigDecimal(row.get("D1")));
        data.setBz((String) row.get("BZ"));
        data.setActualDelivery(WriteBackHelper.toBigDecimal(row.get("ACTUAL_DELIVERY")));
        data.setDeployinfoCode((String) row.get("DEPLOYINFO_CODE"));
        data.setTag((String) row.get("TAG"));
        data.setTagFilterConfig(row.get("TAG_FILTER_CONFIG") != null ? row.get("TAG_FILTER_CONFIG").toString() : null);
        return data;
    }
}
