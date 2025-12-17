package org.example.application.service;

import lombok.extern.slf4j.Slf4j;
import org.example.CigaretteDistributionApplication;
import org.example.application.service.encode.EncodeService;
import org.example.application.service.query.PartitionPredictionQueryService;
import org.example.infrastructure.persistence.po.CigaretteDistributionPredictionPO;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 单卷烟单区域编码功能集成测试。
 * <p>
 * 使用 2025 年 9 月第 3 周的预测数据，遍历预测表中每条记录，
         * 调用 {@link EncodeService#encodeForSpecificArea(String, String, String, String, String, List)}
 * 生成单卷烟单区域的编码表达式，并输出到日志中。
 * </p>
 */
@Slf4j
@SpringBootTest(classes = CigaretteDistributionApplication.class)
public class EncodingAggregationIntegrationTest {

    @Autowired
    private EncodeService encodeService;

    @Autowired
    private PartitionPredictionQueryService partitionPredictionQueryService;

    /**
     * 以 2025/9/3 批次为参数，输出预测表中每条记录的编码表达式。
     * <p>
     * 1. 查询该批次所有预测记录；
     * 2. 按卷烟编码分组，获取每支卷烟的全部区域记录列表；
     * 3. 对每条记录调用 encodeForSpecificArea 生成编码表达式；
     * 4. 按“卷烟 + 区域”逐条输出编码结果，便于人工比对编码规则表。
     * </p>
     */
    @Test
    void encodeEachRecord_in2025_9_3_batch() {
        Integer year = 2025;
        Integer month = 9;
        Integer weekSeq = 3;

        List<CigaretteDistributionPredictionPO> allRecords =
                partitionPredictionQueryService.queryPredictionByTime(year, month, weekSeq);

        Map<String, List<CigaretteDistributionPredictionPO>> recordsByCig =
                allRecords.stream().collect(Collectors.groupingBy(CigaretteDistributionPredictionPO::getCigCode));

        recordsByCig.forEach((cigCode, records) -> {
            String cigName = records.get(0).getCigName();
            log.info("====== 卷烟 {} - {} ======", cigCode, cigName);

            for (CigaretteDistributionPredictionPO record : records) {
                String expr = encodeService.encodeForSpecificArea(
                        record.getCigCode(),
                        record.getCigName(),
                        record.getDeliveryMethod(),
                        record.getDeliveryEtype(),
                        record.getDeliveryArea(),
                        records
                );
                log.info("区域={}，投放方式={}，扩展类型={} -> 编码表达式={}",
                        record.getDeliveryArea(),
                        record.getDeliveryMethod(),
                        record.getDeliveryEtype(),
                        expr);
            }
        });
    }
}


