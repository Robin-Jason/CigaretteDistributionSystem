package org.example.application.event.handler;

import lombok.extern.slf4j.Slf4j;
import org.example.domain.event.DistributionPlanGenerationCompletedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * 分配方案统计处理器
 * 负责更新统计数据、生成报表等
 */
@Slf4j
@Component
public class DistributionPlanStatisticsHandler {
    
    @EventListener
    public void updateStatistics(DistributionPlanGenerationCompletedEvent event) {
        log.info("【统计】更新分配方案统计数据 - {}-{}-{}", 
                event.getYear(), event.getMonth(), event.getWeekSeq());
        
        // TODO: 更新统计数据
        // statisticsService.updateGenerationStats(
        //     event.getYear(), event.getMonth(), event.getWeekSeq(),
        //     event.getTotalCount(), event.getSuccessCount(), event.getFailedCount()
        // );
    }
}

