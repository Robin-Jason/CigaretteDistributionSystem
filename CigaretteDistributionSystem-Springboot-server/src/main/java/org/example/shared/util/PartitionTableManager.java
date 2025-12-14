package org.example.shared.util;

import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * 分区表管理工具类
 * 
 * 功能：
 * 1. 分区创建
 * 2. 分区删除
 * 3. 分区查询
 * 4. 分区键计算
 * 
 * @author System
 * @version 1.0
 * @since 2025-11-29
 */
@Slf4j
@Component
public class PartitionTableManager {
    
    private final JdbcTemplate jdbcTemplate;
    
    
    public PartitionTableManager(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }
    
    /**
     * 生成分区名
     * 
     * @param year 年份
     * @param month 月份
     * @param weekSeq 周序号
     * @return 分区名，如：p_20251003
     */
    public static String generatePartitionName(Integer year, Integer month, Integer weekSeq) {
        return String.format("p_%d%02d%d", year, month, weekSeq);
    }
    
    /**
     * 计算分区键
     * 
     * @param year 年份
     * @param month 月份
     * @param weekSeq 周序号
     * @return 分区键，如：20251003
     */
    public static int calculatePartitionKey(Integer year, Integer month, Integer weekSeq) {
        return year * 10000 + month * 100 + weekSeq;
    }
    
    /**
     * 计算下一个分区键
     * 
     * @param year 年份
     * @param month 月份
     * @param weekSeq 周序号
     * @return 下一个分区键
     */
    public static int calculateNextPartitionKey(Integer year, Integer month, Integer weekSeq) {
        int nextWeekSeq = weekSeq + 1;
        int nextMonth = month;
        int nextYear = year;
        
        if (nextWeekSeq > 5) {
            nextWeekSeq = 1;
            nextMonth++;
            if (nextMonth > 12) {
                nextMonth = 1;
                nextYear++;
            }
        }
        
        return calculatePartitionKey(nextYear, nextMonth, nextWeekSeq);
    }
    
    /**
     * 确保分区存在，如果不存在则创建
     * 
     * @param tableName 表名
     * @param year 年份
     * @param month 月份
     * @param weekSeq 周序号
     */
    public void ensurePartitionExists(String tableName, Integer year, Integer month, Integer weekSeq) {
        String partitionName = generatePartitionName(year, month, weekSeq);
        
        if (partitionExists(tableName, partitionName)) {
            log.debug("分区已存在: {}.{}", tableName, partitionName);
            return;
        }
        
        // 计算当前分区键和下一个分区键
        int currentPartitionKey = calculatePartitionKey(year, month, weekSeq);
        int nextPartitionKey = calculateNextPartitionKey(year, month, weekSeq);
        
        // 查询所有已存在的分区（除了p_future），获取最大分区值
        Integer maxPartitionValue = getMaxPartitionValue(tableName);
        
        if (maxPartitionValue != null && nextPartitionKey <= maxPartitionValue) {
            // 如果新分区的值小于或等于最大分区值，说明分区已存在或时间顺序错误
            String errorMsg = String.format(
                "无法创建分区 %s: 下一个分区值 %d 必须大于已存在的最大分区值 %d",
                partitionName, nextPartitionKey, maxPartitionValue);
            log.error(errorMsg);
            throw new RuntimeException(errorMsg);
        }
        
        // 找到需要重组的源分区（应该是p_future或最大的分区）
        String sourcePartition = findSourcePartitionForReorganize(tableName, nextPartitionKey);
        
        // 创建分区
        String sql = String.format(
            "ALTER TABLE `%s` REORGANIZE PARTITION %s INTO (" +
            "PARTITION %s VALUES LESS THAN (%d)," +
            "PARTITION p_future VALUES LESS THAN MAXVALUE" +
            ")",
            tableName, sourcePartition, partitionName, nextPartitionKey);
        
        try {
            jdbcTemplate.execute(sql);
            log.info("创建分区成功: {}.{} (值范围: {} 到 {})", tableName, partitionName, currentPartitionKey, nextPartitionKey);
        } catch (Exception e) {
            log.error("创建分区失败: {}.{}", tableName, partitionName, e);
            throw new RuntimeException("创建分区失败: " + e.getMessage(), e);
        }
    }
    
    /**
     * 获取表中已存在分区的最大分区值（不包括p_future）
     * 
     * @param tableName 表名
     * @return 最大分区值，如果没有分区则返回null
     */
    private Integer getMaxPartitionValue(String tableName) {
        String sql = "SELECT PARTITION_DESCRIPTION FROM information_schema.PARTITIONS " +
                    "WHERE TABLE_SCHEMA = DATABASE() " +
                    "AND TABLE_NAME = ? " +
                    "AND PARTITION_NAME != 'p_future' " +
                    "AND PARTITION_DESCRIPTION IS NOT NULL " +
                    "AND PARTITION_DESCRIPTION != 'MAXVALUE' " +
                    "ORDER BY CAST(PARTITION_DESCRIPTION AS UNSIGNED) DESC " +
                    "LIMIT 1";
        
        try {
            List<String> results = jdbcTemplate.queryForList(sql, String.class, tableName);
            if (results.isEmpty()) {
                return null;
            }
            String maxValueStr = results.get(0);
            return Integer.parseInt(maxValueStr);
        } catch (Exception e) {
            log.warn("查询最大分区值失败: {}", tableName, e);
            return null;
        }
    }
    
    /**
     * 找到需要重组的源分区
     * 优先使用p_future，因为p_future包含所有未分配的值
     * 
     * @param tableName 表名
     * @param nextPartitionKey 下一个分区键
     * @return 源分区名
     */
    private String findSourcePartitionForReorganize(String tableName, int nextPartitionKey) {
        // 优先使用p_future，因为它包含所有大于最大分区的值
        if (partitionExists(tableName, "p_future")) {
            return "p_future";
        }
        
        // 如果没有p_future，说明所有分区都已经创建，无法创建新分区
        // 这种情况不应该发生，因为我们在前面已经检查了maxPartitionValue
        // 但为了安全，我们仍然抛出错误
        String errorMsg = String.format(
            "无法创建分区: 表 %s 没有 p_future 分区，无法创建新分区",
            tableName);
        log.error(errorMsg);
        throw new RuntimeException(errorMsg);
    }
    
    /**
     * 检查分区是否存在
     * 
     * @param tableName 表名
     * @param partitionName 分区名
     * @return true如果分区存在
     */
    public boolean partitionExists(String tableName, String partitionName) {
        String sql = "SELECT COUNT(*) FROM information_schema.PARTITIONS " +
                    "WHERE TABLE_SCHEMA = DATABASE() " +
                    "AND TABLE_NAME = ? " +
                    "AND PARTITION_NAME = ?";
        
        Integer count = jdbcTemplate.queryForObject(sql, Integer.class, tableName, partitionName);
        return count != null && count > 0;
    }
    
    /**
     * 截断指定分区（快速清空分区数据）
     *
     * @param tableName 表名
     * @param year 年份
     * @param month 月份
     * @param weekSeq 周序号
     * @return true 表示截断成功；false 表示分区不存在或执行失败
     */
    public boolean truncatePartition(String tableName, Integer year, Integer month, Integer weekSeq) {
        String partitionName = generatePartitionName(year, month, weekSeq);
        
        if (!partitionExists(tableName, partitionName)) {
            log.debug("截断分区跳过：{}.{} 不存在", tableName, partitionName);
            return false;
        }
        
        String sql = String.format("ALTER TABLE `%s` TRUNCATE PARTITION %s", tableName, partitionName);
        try {
            jdbcTemplate.execute(sql);
            log.info("截断分区成功：{}.{}", tableName, partitionName);
            return true;
        } catch (Exception e) {
            log.error("截断分区失败：{}.{}", tableName, partitionName, e);
            return false;
        }
    }
    
    /**
     * 查询表的所有分区
     * 
     * @param tableName 表名
     * @return 分区名列表
     */
    public List<String> listPartitions(String tableName) {
        String sql = "SELECT PARTITION_NAME FROM information_schema.PARTITIONS " +
                    "WHERE TABLE_SCHEMA = DATABASE() " +
                    "AND TABLE_NAME = ? " +
                    "AND PARTITION_NAME IS NOT NULL " +
                    "ORDER BY PARTITION_ORDINAL_POSITION";
        
        return jdbcTemplate.queryForList(sql, String.class, tableName);
    }
    
    /**
     * 删除旧分区（保留最近N周的数据）
     * 
     * @param tableName 表名
     * @param retainWeeks 保留周数
     */
    public void dropOldPartitions(String tableName, Integer retainWeeks) {
        // 计算保留截止时间
        // 这里简化处理，实际应该根据当前时间计算
        // TODO: 实现根据当前时间计算保留截止时间
        
        List<String> partitions = listPartitions(tableName);
        log.info("表 {} 共有 {} 个分区", tableName, partitions.size());
        
        // 过滤出需要删除的分区（除了p_future）
        List<String> partitionsToDrop = new ArrayList<>();
        for (String partitionName : partitions) {
            if (!"p_future".equals(partitionName)) {
                // 解析分区名，获取时间信息
                // 如果分区时间早于保留截止时间，则删除
                // TODO: 实现时间判断逻辑
                partitionsToDrop.add(partitionName);
            }
        }
        
        // 删除分区
        for (String partitionName : partitionsToDrop) {
            try {
                String sql = String.format(
                    "ALTER TABLE `%s` DROP PARTITION %s",
                    tableName, partitionName);
                jdbcTemplate.execute(sql);
                log.info("删除分区成功: {}.{}", tableName, partitionName);
            } catch (Exception e) {
                log.error("删除分区失败: {}.{}", tableName, partitionName, e);
            }
        }
    }
    
    /**
     * 删除指定年份的所有分区
     * 
     * @param tableName 表名
     * @param year 年份
     * @return 删除的分区数量
     */
    public int dropPartitionsByYear(String tableName, Integer year) {
        log.info("开始删除表 {} 中 {} 年的所有分区", tableName, year);
        
        List<String> partitions = listPartitions(tableName);
        List<String> partitionsToDrop = new ArrayList<>();
        
        // 分区名格式：p_YYYYMMW (例如：p_20990103)
        String yearPrefix = String.format("p_%d", year);
        
        for (String partitionName : partitions) {
            if (!"p_future".equals(partitionName) && partitionName.startsWith(yearPrefix)) {
                partitionsToDrop.add(partitionName);
            }
        }
        
        if (partitionsToDrop.isEmpty()) {
            log.info("表 {} 中没有找到 {} 年的分区", tableName, year);
            return 0;
        }
        
        log.info("找到 {} 个 {} 年的分区需要删除: {}", partitionsToDrop.size(), year, partitionsToDrop);
        
        int droppedCount = 0;
        for (String partitionName : partitionsToDrop) {
            try {
                String sql = String.format(
                    "ALTER TABLE `%s` DROP PARTITION %s",
                    tableName, partitionName);
                jdbcTemplate.execute(sql);
                log.info("删除分区成功: {}.{}", tableName, partitionName);
                droppedCount++;
            } catch (Exception e) {
                log.error("删除分区失败: {}.{}", tableName, partitionName, e);
            }
        }
        
        log.info("删除完成: 表 {} 中 {} 年的分区，成功删除 {} 个", tableName, year, droppedCount);
        return droppedCount;
    }
    
    /**
     * 删除所有表中指定年份的分区
     * 
     * @param year 年份
     * @return 删除结果Map，key为表名，value为删除的分区数量
     */
    public java.util.Map<String, Integer> dropPartitionsByYearFromAllTables(Integer year) {
        log.info("开始删除所有表中 {} 年的分区", year);
        
        java.util.Map<String, Integer> result = new java.util.HashMap<>();
        
        // 需要处理的表列表
        String[] tables = {
            "region_customer_statistics",
            "cigarette_distribution_info",
            "cigarette_distribution_prediction"
        };
        
        for (String tableName : tables) {
            try {
                int count = dropPartitionsByYear(tableName, year);
                result.put(tableName, count);
            } catch (Exception e) {
                log.error("删除表 {} 中 {} 年的分区时发生错误", tableName, year, e);
                result.put(tableName, -1); // -1 表示失败
            }
        }
        
        int totalDropped = result.values().stream()
            .filter(count -> count > 0)
            .mapToInt(Integer::intValue)
            .sum();
        
        log.info("删除完成: 所有表中 {} 年的分区，共删除 {} 个", year, totalDropped);
        return result;
    }
    
}

