package org.example.application.event;

import org.example.domain.event.DistributionPlanGenerationCompletedEvent;
import org.example.domain.event.DistributionPlanGenerationStartedEvent;
import org.example.domain.event.ExistingDataDeletedEvent;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationEventPublisher;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

/**
 * 分配方案监控事件监听链路简单验证：
 * 仅验证发布开始/删除/完成事件不会抛出异常，
 * 确保监听器在 Spring 容器中正常工作。
 */
@SpringBootTest
class DistributionPlanMonitorHandlerTest {

    @Autowired
    private ApplicationEventPublisher eventPublisher;

    @Test
    void publishDistributionPlanEvents_shouldNotThrow() {
        assertDoesNotThrow(() -> {
            DistributionPlanGenerationStartedEvent started =
                    new DistributionPlanGenerationStartedEvent(2025, 9, 3);
            eventPublisher.publishEvent(started);

            ExistingDataDeletedEvent deleted =
                    new ExistingDataDeletedEvent(2025, 9, 3, 10);
            eventPublisher.publishEvent(deleted);

            DistributionPlanGenerationCompletedEvent completed =
                    new DistributionPlanGenerationCompletedEvent(2025, 9, 3);
            completed.setStartTime(System.currentTimeMillis() - 1000);
            completed.setEndTime(System.currentTimeMillis());
            completed.setTotalCount(100);
            completed.setSuccessCount(95);
            completed.setFailedCount(5);
            completed.setProcessedCount(100);
            completed.setSuccess(true);
            completed.setMessage("分配成功");
            eventPublisher.publishEvent(completed);
        });
    }
}


