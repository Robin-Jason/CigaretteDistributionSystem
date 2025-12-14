package org.example.infrastructure.monitor;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * 事务监控组件
 * 定期检查长时间运行的事务和元数据锁等待情况
 * 
 * @author System
 * @version 1.0
 * @since 2025-11-29
 */
@Slf4j
@Component
public class TransactionMonitor {
    
    @Autowired
    private JdbcTemplate jdbcTemplate;
    
    /**
     * 监控长时间运行的事务
     * 每分钟执行一次
     */
    @Scheduled(fixedRate = 60000) // 60秒 = 60000毫秒
    public void monitorLongRunningTransactions() {
        try {
            // 查询运行时间超过60秒的事务
            List<Map<String, Object>> longTransactions = jdbcTemplate.queryForList(
                "SELECT " +
                "    trx_id, " +
                "    trx_state, " +
                "    trx_started, " +
                "    TIMESTAMPDIFF(SECOND, trx_started, NOW()) as duration_seconds, " +
                "    trx_mysql_thread_id, " +
                "    p.USER, " +
                "    p.HOST, " +
                "    LEFT(p.INFO, 200) as query " +
                "FROM information_schema.INNODB_TRX t " +
                "LEFT JOIN information_schema.PROCESSLIST p ON t.trx_mysql_thread_id = p.ID " +
                "WHERE TIMESTAMPDIFF(SECOND, trx_started, NOW()) > 60 " +
                "ORDER BY duration_seconds DESC"
            );
            
            if (!longTransactions.isEmpty()) {
                log.warn("⚠️ 发现 {} 个长时间运行的事务（>60秒）:", longTransactions.size());
                for (Map<String, Object> tx : longTransactions) {
                    log.warn("  事务ID: {}, 运行时长: {}秒, 连接ID: {}, 用户: {}, 查询: {}", 
                            tx.get("trx_id"),
                            tx.get("duration_seconds"),
                            tx.get("trx_mysql_thread_id"),
                            tx.get("USER"),
                            tx.get("query"));
                }
            } else {
                log.debug("✅ 未发现长时间运行的事务");
            }
        } catch (Exception e) {
            log.error("监控长时间运行的事务时发生错误", e);
        }
    }
    
    /**
     * 监控元数据锁等待
     * 每分钟执行一次
     */
    @Scheduled(fixedRate = 60000) // 60秒 = 60000毫秒
    public void monitorMetadataLockWaits() {
        try {
            // 查询等待元数据锁的连接
            List<Map<String, Object>> lockWaits = jdbcTemplate.queryForList(
                "SELECT " +
                "    ID, " +
                "    USER, " +
                "    HOST, " +
                "    COMMAND, " +
                "    TIME as wait_time_seconds, " +
                "    STATE, " +
                "    LEFT(INFO, 200) as QUERY " +
                "FROM information_schema.PROCESSLIST " +
                "WHERE STATE = 'Waiting for table metadata lock' " +
                "ORDER BY TIME DESC"
            );
            
            if (!lockWaits.isEmpty()) {
                log.warn("⚠️ 发现 {} 个等待元数据锁的连接:", lockWaits.size());
                for (Map<String, Object> wait : lockWaits) {
                    log.warn("  连接ID: {}, 等待时间: {}秒, 用户: {}, 查询: {}", 
                            wait.get("ID"),
                            wait.get("wait_time_seconds"),
                            wait.get("USER"),
                            wait.get("QUERY"));
                }
            } else {
                log.debug("✅ 未发现等待元数据锁的连接");
            }
        } catch (Exception e) {
            log.error("监控元数据锁等待时发生错误", e);
        }
    }
    
    /**
     * 获取数据库健康状态
     * 用于健康检查端点
     */
    public Map<String, Object> getDatabaseHealth() {
        Map<String, Object> health = new java.util.HashMap<>();
        
        try {
            // 检查长时间运行的事务
            Integer longTransactions = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM information_schema.INNODB_TRX " +
                "WHERE TIMESTAMPDIFF(SECOND, trx_started, NOW()) > 60",
                Integer.class
            );
            
            // 检查等待元数据锁的连接
            Integer lockWaits = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM information_schema.PROCESSLIST " +
                "WHERE STATE = 'Waiting for table metadata lock'",
                Integer.class
            );
            
            health.put("longTransactions", longTransactions != null ? longTransactions : 0);
            health.put("lockWaits", lockWaits != null ? lockWaits : 0);
            health.put("status", (longTransactions != null && longTransactions == 0 && 
                                 lockWaits != null && lockWaits == 0) ? "healthy" : "unhealthy");
            health.put("timestamp", System.currentTimeMillis());
            
        } catch (Exception e) {
            log.error("获取数据库健康状态时发生错误", e);
            health.put("status", "error");
            health.put("error", e.getMessage());
        }
        
        return health;
    }
}

