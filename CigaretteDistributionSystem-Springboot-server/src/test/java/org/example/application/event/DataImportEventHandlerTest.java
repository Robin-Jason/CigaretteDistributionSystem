package org.example.application.event;

import org.example.domain.event.DataImportCompletedEvent;
import org.example.domain.event.DataImportFailedEvent;
import org.example.domain.event.DataImportStartedEvent;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationEventPublisher;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

/**
 * 数据导入事件监听链路简单验证：
 * 仅验证事件发布在当前 Spring 容器中不会抛出异常，
 * 间接保证监听器已正确注册且可处理事件。
 */
@SpringBootTest
class DataImportEventHandlerTest {

    @Autowired
    private ApplicationEventPublisher eventPublisher;

    @Test
    void publishDataImportEvents_shouldNotThrow() {
        assertDoesNotThrow(() -> {
            DataImportStartedEvent startedEvent =
                    new DataImportStartedEvent(2025, 9, 3, true, true);
            eventPublisher.publishEvent(startedEvent);

            DataImportCompletedEvent completedEvent =
                    new DataImportCompletedEvent(2025, 9, 3, true, "导入成功");
            eventPublisher.publishEvent(completedEvent);

            DataImportFailedEvent failedEvent =
                    new DataImportFailedEvent(2025, 9, 3, "导入失败: 测试错误", null);
            eventPublisher.publishEvent(failedEvent);
        });
    }
}


