package org.example.application.event.handler;

import lombok.extern.slf4j.Slf4j;
import lombok.RequiredArgsConstructor;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tag;
import io.micrometer.core.instrument.Tags;
import io.micrometer.core.instrument.DistributionSummary;
import org.example.domain.event.DistributionPlanGenerationStartedEvent;
import org.example.domain.event.DistributionPlanGenerationCompletedEvent;
import org.example.domain.event.ExistingDataDeletedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * 分配方案监控处理器
 * 负责记录监控指标、性能统计等
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DistributionPlanMonitorHandler {

    private static final String METRIC_PLAN_STARTED = "distribution_plan_started_total";
    private static final String METRIC_PLAN_COMPLETED = "distribution_plan_completed_total";
    private static final String METRIC_PLAN_DURATION = "distribution_plan_duration_ms";
    private static final String METRIC_PLAN_FAILED_COUNT = "distribution_plan_failed_count";

    private final MeterRegistry meterRegistry;
    
    @EventListener
    public void handlePlanStarted(DistributionPlanGenerationStartedEvent event) {
        log.info("【监控】分配方案生成开始 - batchId={}, {}-{}-{}", 
                event.getBatchId(), event.getYear(), event.getMonth(), event.getWeekSeq());
        // TODO: 可以集成 Micrometer、Prometheus 等监控系统

        meterRegistry.counter(METRIC_PLAN_STARTED,
                Tags.of(Tag.of("batchId", safe(event.getBatchId())),
                        Tag.of("year", String.valueOf(event.getYear())),
                        Tag.of("month", String.valueOf(event.getMonth())),
                        Tag.of("weekSeq", String.valueOf(event.getWeekSeq()))))
                .increment();
    }
    
    @EventListener
    public void handleDataDeleted(ExistingDataDeletedEvent event) {
        log.info("【监控】删除现有数据 - batchId={}, {}-{}-{}, deletedCount={}", 
                event.getBatchId(), event.getYear(), event.getMonth(), event.getWeekSeq(), 
                event.getDeletedCount());
    }
    
    @EventListener
    public void handlePlanCompleted(DistributionPlanGenerationCompletedEvent event) {
        long duration = event.getEndTime() != null && event.getStartTime() != null 
            ? event.getEndTime() - event.getStartTime() : 0;
        log.info("【监控】分配方案生成完成 - batchId={}, {}-{}-{}, duration={}ms, success={}/{}, failed={}, processedCount={}", 
                event.getBatchId(), event.getYear(), event.getMonth(), event.getWeekSeq(),
                duration, event.getSuccessCount(), event.getTotalCount(), event.getFailedCount(),
                event.getProcessedCount());
        
        // TODO: 记录到监控系统
        // metricsService.recordTimer("distribution.plan.generation.duration", duration);
        // metricsService.recordGauge("distribution.plan.success.rate", 
        //     event.getSuccessCount() / (double) event.getTotalCount());

        Tags baseTags = Tags.of(
                Tag.of("batchId", safe(event.getBatchId())),
                Tag.of("year", String.valueOf(event.getYear())),
                Tag.of("month", String.valueOf(event.getMonth())),
                Tag.of("weekSeq", String.valueOf(event.getWeekSeq())),
                Tag.of("success", String.valueOf(event.getSuccess()))
        );

        meterRegistry.counter(METRIC_PLAN_COMPLETED, baseTags).increment();

        recordDuration(event.getStartTime(), event.getEndTime(),
                baseTags.and(Tag.of("success", String.valueOf(event.getSuccess()))));

        if (event.getFailedCount() != null) {
            DistributionSummary summary = meterRegistry.summary(METRIC_PLAN_FAILED_COUNT, baseTags);
            summary.record(event.getFailedCount());
        }
    }

    private void recordDuration(Long start, Long end, Tags tags) {
        if (start == null || end == null || end < start) {
            return;
        }
        long duration = end - start;
        DistributionSummary summary = meterRegistry.summary(METRIC_PLAN_DURATION, tags);
        summary.record(duration);
    }

    private String safe(String value) {
        return value == null ? "unknown" : value;
    }
}

