package org.example.application.event.handler;

import lombok.extern.slf4j.Slf4j;
import lombok.RequiredArgsConstructor;
import org.example.domain.event.DataImportStartedEvent;
import org.example.domain.event.DataImportCompletedEvent;
import org.example.domain.event.DataImportFailedEvent;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tag;
import io.micrometer.core.instrument.Tags;
import io.micrometer.core.instrument.DistributionSummary;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * 数据导入事件处理器
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DataImportEventHandler {
    
    private static final String METRIC_IMPORT_STARTED = "data_import_started_total";
    private static final String METRIC_IMPORT_COMPLETED = "data_import_completed_total";
    private static final String METRIC_IMPORT_DURATION = "data_import_duration_ms";

    private final MeterRegistry meterRegistry;
    
    @EventListener
    public void handleImportStarted(DataImportStartedEvent event) {
        log.info("【导入】数据导入开始 - batchId={}, {}-{}-{}, hasBaseFile={}, hasCigFile={}",
                event.getBatchId(), event.getYear(), event.getMonth(), event.getWeekSeq(),
                event.getHasBaseCustomerFile(), event.getHasCigaretteFile());

        meterRegistry.counter(METRIC_IMPORT_STARTED,
                Tags.of(Tag.of("batchId", safe(event.getBatchId())),
                        Tag.of("year", String.valueOf(event.getYear())),
                        Tag.of("month", String.valueOf(event.getMonth())),
                        Tag.of("weekSeq", String.valueOf(event.getWeekSeq()))))
                .increment();
    }
    
    @EventListener
    public void handleImportCompleted(DataImportCompletedEvent event) {
        log.info("【导入】数据导入完成 - batchId={}, {}-{}-{}, success={}, message={}", 
                event.getBatchId(), event.getYear(), event.getMonth(), event.getWeekSeq(),
                event.getSuccess(), event.getMessage());

        meterRegistry.counter(METRIC_IMPORT_COMPLETED,
                Tags.of(Tag.of("batchId", safe(event.getBatchId())),
                        Tag.of("year", String.valueOf(event.getYear())),
                        Tag.of("month", String.valueOf(event.getMonth())),
                        Tag.of("weekSeq", String.valueOf(event.getWeekSeq())),
                        Tag.of("success", String.valueOf(event.getSuccess()))))
                .increment();

        recordDuration(event.getStartTime(), event.getEndTime(),
                Tags.of(Tag.of("batchId", safe(event.getBatchId())),
                        Tag.of("success", String.valueOf(event.getSuccess()))));
    }
    
    @EventListener
    public void handleImportFailed(DataImportFailedEvent event) {
        log.error("【导入】数据导入失败 - batchId={}, {}-{}-{}, error={}", 
                event.getBatchId(), event.getYear(), event.getMonth(), event.getWeekSeq(),
                event.getErrorMessage(), event.getException());

        meterRegistry.counter(METRIC_IMPORT_COMPLETED,
                Tags.of(Tag.of("batchId", safe(event.getBatchId())),
                        Tag.of("year", String.valueOf(event.getYear())),
                        Tag.of("month", String.valueOf(event.getMonth())),
                        Tag.of("weekSeq", String.valueOf(event.getWeekSeq())),
                        Tag.of("success", "false")))
                .increment();
    }

    private void recordDuration(Long start, Long end, Tags tags) {
        if (start == null || end == null || end < start) {
            return;
        }
        long duration = end - start;
        DistributionSummary summary = meterRegistry.summary(METRIC_IMPORT_DURATION, tags);
        summary.record(duration);
    }

    private String safe(String value) {
        return value == null ? "unknown" : value;
    }
}

