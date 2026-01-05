package com.taskscheduler.service;

import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import com.taskscheduler.events.TaskEvents;
import com.taskscheduler.events.WorkerEvents;

/**
 * Service for periodic logging of system metrics and health information.
 * Runs scheduled tasks to log system status for monitoring purposes.
 */
@Service
@ConditionalOnProperty(name = "task-scheduler.monitoring.metrics-collection-enabled", havingValue = "true", matchIfMissing = true)
public class MetricsLoggingService {
    
    private static final Logger logger = LoggerFactory.getLogger(MetricsLoggingService.class);
    
    private final MonitoringService monitoringService;
    private final TaskEvents taskEvents;
    private final WorkerEvents workerEvents;
    
    @Autowired
    public MetricsLoggingService(MonitoringService monitoringService,
                               TaskEvents taskEvents,
                               WorkerEvents workerEvents) {
        this.monitoringService = monitoringService;
        this.taskEvents = taskEvents;
        this.workerEvents = workerEvents;
        
        logger.info("MetricsLoggingService initialized - Periodic metrics logging enabled");
    }
    
    /**
     * Logs system metrics every 5 minutes.
     * Provides regular health and performance information.
     */
    @Scheduled(fixedRateString = "${task-scheduler.monitoring.metrics-logging-interval-ms:300000}")
    public void logSystemMetrics() {
        try {
            logger.debug("Starting periodic system metrics logging");
            
            
            monitoringService.logSystemMetrics();
            
          
            Map<String, Long> taskCounts = monitoringService.getTaskCountsByStatus();
            taskEvents.logTaskMetrics(
                    taskCounts.get("TOTAL"),
                    taskCounts.get("PENDING"),
                    taskCounts.get("RUNNING"),
                    taskCounts.get("SUCCESS"),
                    taskCounts.get("FAILED")
            );
            
         
            int activeWorkers = monitoringService.getActiveWorkerCount();
            logger.info("SYSTEM_METRICS_SUMMARY - Active workers: {}, System uptime: {}s", 
                    activeWorkers, monitoringService.getUptimeSeconds());
            
        } catch (Exception e) {
            logger.error("Error during periodic metrics logging", e);
        }
    }
    
    /**
     * Logs detailed system health every 15 minutes.
     * Provides comprehensive health status information.
     */
    @Scheduled(fixedRate = 900000) // 15 minutes
    public void logDetailedHealthMetrics() {
        try {
            logger.debug("Starting detailed health metrics logging");
            
            String healthStatus = monitoringService.getSystemHealthStatus();
            long uptimeSeconds = monitoringService.getUptimeSeconds();
            
            logger.info("DETAILED_HEALTH_CHECK - System status: {}, Uptime: {}s", 
                    healthStatus, uptimeSeconds);
            
            
            monitoringService.getWorkerStatuses().forEach(worker -> {
                if (worker.isActive()) {
                    logger.debug("ACTIVE_WORKER - Worker: {}, Last heartbeat: {}s ago", 
                            worker.getWorkerId(), worker.getSecondsSinceHeartbeat());
                } else {
                    logger.warn("INACTIVE_WORKER - Worker: {}, Last heartbeat: {}s ago", 
                            worker.getWorkerId(), worker.getSecondsSinceHeartbeat());
                }
            });
            
        } catch (Exception e) {
            logger.error("Error during detailed health metrics logging", e);
        }
    }
    
    /**
     * Logs performance metrics every hour.
     * Provides execution performance and success rate information.
     */
    @Scheduled(fixedRate = 3600000) // 1 hour
    public void logPerformanceMetrics() {
        try {
            logger.debug("Starting performance metrics logging");
            
            Map<String, Object> hourlyMetrics = monitoringService.getExecutionMetrics(
                    java.time.Duration.ofHours(1));
            
            logger.info("HOURLY_PERFORMANCE_METRICS - Executions: {}, Success rate: {}%, " +
                    "Avg execution time: {}ms, Currently running: {}, Potentially stuck: {}", 
                    hourlyMetrics.get("total_executions"),
                    hourlyMetrics.get("success_rate_percent"),
                    hourlyMetrics.get("average_execution_time_ms"),
                    hourlyMetrics.get("currently_running"),
                    hourlyMetrics.get("potentially_stuck"));
            
           
            Map<String, Object> dailyMetrics = monitoringService.getExecutionMetrics(
                    java.time.Duration.ofDays(1));
            
            logger.info("DAILY_PERFORMANCE_METRICS - Executions: {}, Success rate: {}%, " +
                    "Avg execution time: {}ms", 
                    dailyMetrics.get("total_executions"),
                    dailyMetrics.get("success_rate_percent"),
                    dailyMetrics.get("average_execution_time_ms"));
            
        } catch (Exception e) {
            logger.error("Error during performance metrics logging", e);
        }
    }
    
    /**
     * Logs system startup completion.
     * Called once after application startup is complete.
     */
    @Scheduled(initialDelay = 30000, fixedRate = Long.MAX_VALUE) // Run once after 30 seconds
    public void logSystemStartupComplete() {
        try {
            logger.info("SYSTEM_STARTUP_COMPLETE - Distributed Task Scheduler is fully operational");
            
            
            Map<String, Long> taskCounts = monitoringService.getTaskCountsByStatus();
            int activeWorkers = monitoringService.getActiveWorkerCount();
            
            logger.info("INITIAL_SYSTEM_STATE - Tasks: {} total, {} pending, {} running; Workers: {} active", 
                    taskCounts.get("TOTAL"), taskCounts.get("PENDING"), 
                    taskCounts.get("RUNNING"), activeWorkers);
            
        } catch (Exception e) {
            logger.error("Error logging system startup completion", e);
        }
    }
}