package com.taskscheduler.service;

import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.taskscheduler.dto.WorkerStatusResponse;
import com.taskscheduler.entity.TaskStatus;
import com.taskscheduler.entity.WorkerHeartbeat;
import com.taskscheduler.repository.TaskExecutionRepository;
import com.taskscheduler.repository.TaskRepository;
import com.taskscheduler.repository.WorkerHeartbeatRepository;

@Service
public class MonitoringService {
    
    private static final Logger logger = LoggerFactory.getLogger(MonitoringService.class);
    private static final Duration WORKER_TIMEOUT = Duration.ofSeconds(60);
    
    private final TaskRepository taskRepository;
    private final WorkerHeartbeatRepository workerHeartbeatRepository;
    private final TaskExecutionRepository taskExecutionRepository;
    private final Instant startupTime;
    
    @Autowired
    public MonitoringService(TaskRepository taskRepository,
                           WorkerHeartbeatRepository workerHeartbeatRepository,
                           TaskExecutionRepository taskExecutionRepository) {
        this.taskRepository = taskRepository;
        this.workerHeartbeatRepository = workerHeartbeatRepository;
        this.taskExecutionRepository = taskExecutionRepository;
        this.startupTime = Instant.now();
        
        logger.info("MonitoringService initialized at {}", startupTime);
    }
    
    /**
     * Gets the current count of active workers.
     * Workers are considered active if they sent a heartbeat within the timeout period.
     */
    public int getActiveWorkerCount() {
        Instant cutoffTime = Instant.now().minus(WORKER_TIMEOUT);
        return (int) workerHeartbeatRepository.countActiveWorkers(cutoffTime);
    }
    
    /**
     * Gets detailed status information for all workers.
     */
    public List<WorkerStatusResponse> getWorkerStatuses() {
        List<WorkerHeartbeat> allWorkers = workerHeartbeatRepository.findAllOrderedByHeartbeat();
        Instant now = Instant.now();
        Instant cutoffTime = now.minus(WORKER_TIMEOUT);
        
        return allWorkers.stream()
                .map(worker -> {
                    boolean isActive = worker.getLastHeartbeat().isAfter(cutoffTime);
                    long secondsSinceHeartbeat = Duration.between(worker.getLastHeartbeat(), now).getSeconds();
                    
                    return new WorkerStatusResponse(
                            worker.getWorkerId(),
                            worker.getLastHeartbeat(),
                            worker.getRegisteredAt(),
                            isActive,
                            secondsSinceHeartbeat,
                            worker.getWorkerMetadata()
                    );
                })
                .collect(Collectors.toList());
    }
    
    /**
     * Gets task count statistics by status.
     */
    public Map<String, Long> getTaskCountsByStatus() {
        Map<String, Long> counts = new HashMap<>();
        
        counts.put("PENDING", taskRepository.countByStatus(TaskStatus.PENDING));
        counts.put("RUNNING", taskRepository.countByStatus(TaskStatus.RUNNING));
        counts.put("SUCCESS", taskRepository.countByStatus(TaskStatus.SUCCESS));
        counts.put("FAILED", taskRepository.countByStatus(TaskStatus.FAILED));
        counts.put("TOTAL", taskRepository.count());
        
        return counts;
    }
    
    /**
     * Gets execution metrics for a specified time period.
     */
    public Map<String, Object> getExecutionMetrics(Duration period) {
        Instant endTime = Instant.now();
        Instant startTime = endTime.minus(period);
        
        Map<String, Object> metrics = new HashMap<>();
        

        long totalExecutions = taskExecutionRepository.countTotalExecutions(startTime, endTime);
        long successfulExecutions = taskExecutionRepository.countSuccessfulExecutions(startTime, endTime);
        long failedExecutions = taskExecutionRepository.countFailedExecutions(startTime, endTime);
        
        metrics.put("total_executions", totalExecutions);
        metrics.put("successful_executions", successfulExecutions);
        metrics.put("failed_executions", failedExecutions);
        

        if (totalExecutions > 0) {
            double successRate = (double) successfulExecutions / totalExecutions * 100.0;
            metrics.put("success_rate_percent", Math.round(successRate * 100.0) / 100.0);
        } else {
            metrics.put("success_rate_percent", 0.0);
        }
        
  
        Double avgExecutionTimeMs = taskExecutionRepository.getAverageExecutionTimeInMilliseconds(startTime, endTime);
        if (avgExecutionTimeMs != null) {
            metrics.put("average_execution_time_ms", Math.round(avgExecutionTimeMs));
        } else {
            metrics.put("average_execution_time_ms", 0);
        }
        
   
        List<?> runningExecutions = taskExecutionRepository.findRunningExecutions();
        metrics.put("currently_running", runningExecutions.size());
        
  
        Instant stuckCutoff = Instant.now().minus(Duration.ofMinutes(10));
        List<?> stuckExecutions = taskExecutionRepository.findStuckExecutions(stuckCutoff);
        metrics.put("potentially_stuck", stuckExecutions.size());
        
        metrics.put("period_hours", period.toHours());
        metrics.put("period_start", startTime);
        metrics.put("period_end", endTime);
        
        return metrics;
    }
    
    /**
     * Gets system uptime in seconds.
     */
    public long getUptimeSeconds() {
        return Duration.between(startupTime, Instant.now()).getSeconds();
    }
    
    /**
     * Gets the system startup time.
     */
    public Instant getStartupTime() {
        return startupTime;
    }
    
    /**
     * Determines overall system health status based on various metrics.
     * Returns "UP" if system is healthy, "DOWN" if critical issues detected.
     */
    public String getSystemHealthStatus() {
        try {
            
            int activeWorkers = getActiveWorkerCount();
            
         
            Instant stuckCutoff = Instant.now().minus(Duration.ofMinutes(10));
            List<?> stuckExecutions = taskExecutionRepository.findStuckExecutions(stuckCutoff);
            
           
            long pendingTasks = taskRepository.countByStatus(TaskStatus.PENDING);
            
            if (pendingTasks > 0 && activeWorkers == 0) {
                logger.warn("System health degraded: {} pending tasks but no active workers", pendingTasks);
                return "DEGRADED";
            }
            
            if (stuckExecutions.size() > 10) {
                logger.warn("System health degraded: {} potentially stuck executions", stuckExecutions.size());
                return "DEGRADED";
            }
            
            return "UP";
            
        } catch (Exception e) {
            logger.error("Error checking system health", e);
            return "DOWN";
        }
    }
    
    /**
     * Logs system metrics for monitoring purposes.
     * Called periodically to track system performance.
     */
    public void logSystemMetrics() {
        try {
            Map<String, Long> taskCounts = getTaskCountsByStatus();
            int activeWorkers = getActiveWorkerCount();
            long uptimeSeconds = getUptimeSeconds();
            
            logger.info("System metrics - Uptime: {}s, Active workers: {}, Tasks: {} pending, {} running, {} total",
                    uptimeSeconds, activeWorkers, 
                    taskCounts.get("PENDING"), taskCounts.get("RUNNING"), taskCounts.get("TOTAL"));
            
        
            Map<String, Object> hourlyMetrics = getExecutionMetrics(Duration.ofHours(1));
            logger.info("Hourly execution metrics - Total: {}, Success rate: {}%, Avg time: {}ms",
                    hourlyMetrics.get("total_executions"),
                    hourlyMetrics.get("success_rate_percent"),
                    hourlyMetrics.get("average_execution_time_ms"));
            
        } catch (Exception e) {
            logger.error("Error logging system metrics", e);
        }
    }
}