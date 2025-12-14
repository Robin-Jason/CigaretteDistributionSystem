package org.example.application.service.query;

import lombok.extern.slf4j.Slf4j;
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

    /**
     * 按时间分区查询并输出指定字段顺序。
     * <p>
     * 查询指定时间分区的预测数据，并按照预定义的字段顺序返回结果。
     * 字段顺序：cig_code, cig_name, deployinfo_code, delivery_method, delivery_etype, delivery_area, tag, tag_filter_config, adv, D30-D1, bz, actual_delivery
     * </p>
     *
     * @param year    年份
     * @param month   月份
     * @param weekSeq 周序号
     * @return 预测数据列表，每个Map包含有序的字段
     * @example
     * <pre>
     *     List<Map<String, Object>> data = service.listByTime(2025, 9, 3);
     *     // 返回: 2025年9月第3周的预测数据，字段按指定顺序排列
     *     // data.get(0).keySet() 的顺序: [cig_code, cig_name, deployinfo_code, delivery_method, ...]
     * </pre>
     */
    @Override
    public List<Map<String, Object>> listByTime(Integer year, Integer month, Integer weekSeq) {
        List<Map<String, Object>> rows = predictionRepository.findAllWithAdv(year, month, weekSeq);
        return rows.stream()
                .map(this::toOrderedRecord)
                .collect(Collectors.toList());
    }

    /**
     * 按时间分区查询价位段预测表，并输出指定字段顺序。
     * <p>
     * 查询指定时间分区的价位段预测数据，并按照预定义的字段顺序返回结果。
     * 字段顺序与listByTime相同。
     * </p>
     *
     * @param year    年份
     * @param month   月份
     * @param weekSeq 周序号
     * @return 价位段预测数据列表，每个Map包含有序的字段
     * @example
     * <pre>
     *     List<Map<String, Object>> data = service.listPriceByTime(2025, 9, 3);
     *     // 返回: 2025年9月第3周的价位段预测数据，字段按指定顺序排列
     * </pre>
     */
    @Override
    public List<Map<String, Object>> listPriceByTime(Integer year, Integer month, Integer weekSeq) {
        List<Map<String, Object>> rows = predictionPriceRepository.findAll(year, month, weekSeq);
        return rows.stream()
                .map(this::toOrderedRecord)
                .collect(Collectors.toList());
    }

    /**
     * 将Map行数据转换为有序的记录格式。
     * <p>
     * 将DAO层返回的Map数据转换为有序的LinkedHashMap，确保字段顺序符合前端展示要求。
     * 字段顺序：cig_code, cig_name, deployinfo_code, delivery_method, delivery_etype, delivery_area, tag, tag_filter_config, adv, D30-D1, bz, actual_delivery
     * </p>
     *
     * @param row Map行数据
     * @return 有序的记录Map
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
        // 预投放量（来自 info 表，可在行中预留字段 ADV 或 ADV_AMOUNT）
        ordered.put("adv", row.get("ADV") != null ? row.get("ADV") : row.get("adv"));
        // D30 - D1
        for (int i = 30; i >= 1; i--) {
            String key = "D" + i;
            ordered.put(key, row.get(key));
        }
        ordered.put("bz", row.get("BZ") != null ? row.get("BZ") : row.get("bz"));
        // 实际投放量（prediction 表 ACTUAL_DELIVERY）
        ordered.put("actual_delivery", row.get("ACTUAL_DELIVERY"));
        return ordered;
    }
}
