package org.example.application.service.coordinator;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.shared.util.PartitionTableManager;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

/**
 * customer_filter 分区表自动清理服务
 * 
 * 功能：
 * 1. 自动清理超过保留期的分区（默认保留1周）
 * 2. 支持手动触发清理
 * 
 * @author System
 * @version 1.0
 * @since 2025-12-20
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CustomerFilterPartitionCleanupService {

    private final PartitionTableManager partitionTableManager;
    
    private static final String CUSTOMER_FILTER_TABLE = "customer_filter";
    private static final int RETAIN_WEEKS = 1; // 保留1周的数据

    /**
     * 自动清理旧分区
     * 每天凌晨2点执行
     */
    @Scheduled(cron = "0 0 2 * * ?")
    public void autoCleanupOldPartitions() {
        log.info("开始自动清理 customer_filter 表的旧分区（保留最近{}周）", RETAIN_WEEKS);
        try {
            cleanupOldPartitions(RETAIN_WEEKS);
            log.info("自动清理完成");
        } catch (Exception e) {
            log.error("自动清理失败", e);
        }
    }

    /**
     * 清理超过保留期的分区
     *
     * @param retainWeeks 保留周数
     */
    public void cleanupOldPartitions(int retainWeeks) {
        log.info("开始清理 customer_filter 表超过{}周的分区", retainWeeks);
        
        // 计算保留截止时间
        LocalDate now = LocalDate.now();
        LocalDate cutoffDate = now.minus(retainWeeks, ChronoUnit.WEEKS);
        
        // 获取所有分区
        java.util.List<String> partitions = partitionTableManager.listPartitions(CUSTOMER_FILTER_TABLE);
        log.info("customer_filter 表共有 {} 个分区", partitions.size());
        
        int cleanedCount = 0;
        for (String partitionName : partitions) {
            if ("p_future".equals(partitionName)) {
                continue; // 跳过 p_future 分区
            }
            
            // 解析分区名：p_YYYYMMW
            try {
                if (partitionName.startsWith("p_") && partitionName.length() >= 8) {
                    String dateStr = partitionName.substring(2);
                    int year = Integer.parseInt(dateStr.substring(0, 4));
                    int month = Integer.parseInt(dateStr.substring(4, 6));
                    int weekSeq = Integer.parseInt(dateStr.substring(6));
                    
                    // 计算分区对应的日期（简化处理：假设每月第1周对应第1天）
                    LocalDate partitionDate = LocalDate.of(year, month, 1);
                    
                    // 如果分区日期早于截止日期，删除分区
                    if (partitionDate.isBefore(cutoffDate)) {
                        try {
                            partitionTableManager.truncatePartition(CUSTOMER_FILTER_TABLE, year, month, weekSeq);
                            log.info("清理分区: {}.{} (日期: {})", CUSTOMER_FILTER_TABLE, partitionName, partitionDate);
                            cleanedCount++;
                        } catch (Exception e) {
                            log.warn("清理分区失败: {}.{}", CUSTOMER_FILTER_TABLE, partitionName, e);
                        }
                    }
                }
            } catch (Exception e) {
                log.warn("解析分区名失败: {}", partitionName, e);
            }
        }
        
        log.info("清理完成: 共清理 {} 个分区", cleanedCount);
    }

    /**
     * 手动清理指定年份的所有分区
     *
     * @param year 年份
     */
    public void cleanupByYear(Integer year) {
        log.info("手动清理 customer_filter 表 {} 年的所有分区", year);
        partitionTableManager.dropPartitionsByYear(CUSTOMER_FILTER_TABLE, year);
    }
}

