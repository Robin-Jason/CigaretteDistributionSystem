package org.example.application.service.prediction.impl;

import lombok.extern.slf4j.Slf4j;
import org.example.application.service.prediction.PredictionQueryService;
import org.example.domain.repository.CigaretteDistributionPredictionRepository;
import org.example.domain.repository.CigaretteDistributionPredictionPriceRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 预测分区数据查询服务实现类
 * <p>
 * 提供预测分区数据的查询功能，支持按时间分区查询预测表和价位段预测表。
 * 负责将查询结果转换为有序的Map格式，便于前端展示和数据导出。
 * </p>
 *
 * @author Robin
 * @version 1.0
 * @since 2025-12-12
 */
@Slf4j
@Service
public class PredictionQueryServiceImpl implements PredictionQueryService {

    @Autowired
    private CigaretteDistributionPredictionRepository predictionRepository;
    @Autowired
    private CigaretteDistributionPredictionPriceRepository predictionPriceRepository;

    @Override
    public List<Map<String, Object>> listByTime(Integer year, Integer month, Integer weekSeq) {
        List<Map<String, Object>> rows = predictionRepository.findAllWithAdv(year, month, weekSeq);
        return rows.stream()
                .map(this::toOrderedRecord)
                .collect(Collectors.toList());
    }

    @Override
    public List<Map<String, Object>> listPriceByTime(Integer year, Integer month, Integer weekSeq) {
        List<Map<String, Object>> rows = predictionPriceRepository.findAll(year, month, weekSeq);
        return rows.stream()
                .map(this::toOrderedRecord)
                .collect(Collectors.toList());
    }

    /**
     * 将Map行数据转换为有序的记录格式。
     */
    private Map<String, Object> toOrderedRecord(Map<String, Object> row) {
        LinkedHashMap<String, Object> ordered = new LinkedHashMap<>();
        ordered.put("cig_code", row.get("CIG_CODE"));
        ordered.put("cig_name", row.get("CIG_NAME"));
        ordered.put("deployinfo_code", row.get("DEPLOYINFO_CODE"));
        ordered.put("delivery_method", row.get("DELIVERY_METHOD"));
        ordered.put("delivery_etype", row.get("DELIVERY_ETYPE"));
        ordered.put("delivery_area", row.get("DELIVERY_AREA"));
        ordered.put("tag", row.get("TAG"));
        ordered.put("tag_filter_config", row.get("TAG_FILTER_CONFIG"));
        ordered.put("adv", row.get("ADV") != null ? row.get("ADV") : row.get("adv"));
        for (int i = 30; i >= 1; i--) {
            String key = "D" + i;
            ordered.put(key, row.get(key));
        }
        ordered.put("bz", row.get("BZ") != null ? row.get("BZ") : row.get("bz"));
        ordered.put("actual_delivery", row.get("ACTUAL_DELIVERY"));
        return ordered;
    }
}
