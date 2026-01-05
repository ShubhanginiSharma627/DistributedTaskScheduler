package com.taskscheduler.service;

import java.time.Instant;
import java.util.List;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.taskscheduler.config.TaskSchedulerConfig;
import com.taskscheduler.entity.TaskStatus;
import com.taskscheduler.entity.WorkerHeartbeat;
import com.taskscheduler.repository.TaskRepository;
import com.taskscheduler.repository.WorkerHeartbeatRepository;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;

/**
 * Service for monitoring worker heartbeats and detecting failures.
 * Handles worker failure detection and task reassignment when workers become unresponsive.
 * 
 * Requirements: 4.4, 4.5 - Worker failure detection and task reassignment
 */
@Service
public class HeartbeatMonitor {
    
    private static final Logger logger = LoggerFactory.getLogger(HeartbeatMonitor.class);
    
    private final WorkerHeartbeatRepository workerHeartbeatRepository;
    private final TaskRepository taskRepository;
    private final TaskSchedulerConfig config;
    
    private ScheduledExecutorService executorService;
    private volatile boolean running = false;
    
    public HeartbeatMonitor(WorkerHeartbeatRepository workerHeartbeatRepository,
                           TaskRepository taskRepository,
                           TaskSchedulerConfig config) {
        this.workerHeartbeatRepository = workerHeartbeatRepository;
        this.taskRepository = taskRepository;
        this.config = config;
    }
    
    /**
     * Initializes the heartbeat monitor and starts failure detection.
     * Starts background thread for periodic worker failure detection.
     */
    @PostConstruct
    public void initialize() {
        logger.info("Initializing heartbeat monitor");
        
        // Initialize single-threaded executor for failure detection
        executorService = new ScheduledThreadPoolExecutor(1, r -> {
            Thread t = new Thread(r, "heartbeat-monitor");
            t.setDaemon(true);
            return t;
        });
        
        // Start failure detection
        running = true;
        startFailureDetection();
        
        logger.info("Heartbeat monitor started successfully");
    }
    
    /**
     * Shuts down the heartbeat monitor gracefully.
     * Stops background threads and cleans up resources.
     */
    @PreDestroy
    public void shutdown() {
        logger.info("Shutting down heartbeat monitor");
        
        running = false;
        
        if (executorService != null) {
            executorService.shutdown();
            try {
                if (!executorService.awaitTermination(10, TimeUnit.SECONDS)) {
                    executorService.shutdownNow();
                }
            } catch (InterruptedException e) {
                executorService.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }
        
        logger.info("Heartbeat monitor shutdown complete");
    }
    
    /**
     * Starts the failure detection thread.
     * Periodically checks for workers that have missed heartbeats and reassigns their tasks.
     * 
     * Requirements: 4.4 - Detect worker failures within 60 seconds
     */
    private void startFailureDetection() {
        long intervalMs = config.getMonitoring().getFailureDetectionIntervalMs();
        
        executorService.scheduleWithFixedDelay(() -> {
            if (!running) {
                return;
            }
            
            try {
                detectAndHandleFailures();
            } catch (Exception e) {
                logger.error("Error during failure detection", e);
            }
        }, intervalMs, intervalMs, TimeUnit.MILLISECONDS);
        
        logger.info("Failure detection started with interval: {}ms", intervalMs);
    }
    
    /**
     * Detects worker failures and handles task reassignment.
     * Finds workers that haven't sent heartbeats within the timeout period
     * and reassigns their running tasks back to PENDING status.
     */
    @Transactional(isolation = Isolation.READ_COMMITTED, propagation = Propagation.REQUIRED)
    public void detectAndHandleFailures() {
        try {
            long timeoutMs = config.getWorker().getHeartbeatTimeoutMs();
            Instant cutoffTime = Instant.now().minusMillis(timeoutMs);
            
           
            List<WorkerHeartbeat> staleWorkers = workerHeartbeatRepository.findStaleWorkers(cutoffTime);
            
            if (staleWorkers.isEmpty()) {
                logger.debug("No stale workers detected");
                return;
            }
            
            logger.warn("Detected {} stale workers", staleWorkers.size());
            
            for (WorkerHeartbeat staleWorker : staleWorkers) {
                handleWorkerFailure(staleWorker);
            }
            
          
            cleanupStaleHeartbeats();
            
        } catch (Exception e) {
            logger.error("Error during failure detection and handling", e);
        }
    }
    
    /**
     * Handles the failure of a specific worker.
     * Reassigns all running tasks from the failed worker back to PENDING status.
     * Uses SERIALIZABLE isolation to ensure atomic task reassignment.
     */
    @Transactional(isolation = Isolation.SERIALIZABLE, propagation = Propagation.REQUIRES_NEW)
    public void handleWorkerFailure(WorkerHeartbeat failedWorker) {
        String workerId = failedWorker.getWorkerId();
        Instant lastHeartbeat = failedWorker.getLastHeartbeat();
        
        logger.warn("Handling failure for worker {} (last heartbeat: {})", workerId, lastHeartbeat);
        
        try {
         
            int reassignedTasks = taskRepository.resetAbandonedTasks(
                workerId, 
                TaskStatus.RUNNING, 
                TaskStatus.PENDING, 
                Instant.now()
            );
            
            if (reassignedTasks > 0) {
                logger.warn("Reassigned {} abandoned tasks from failed worker {}", reassignedTasks, workerId);
            } else {
                logger.info("No tasks to reassign from failed worker {}", workerId);
            }
            
           
            logWorkerFailureEvent(failedWorker, reassignedTasks);
            
        } catch (Exception e) {
            logger.error("Error handling failure for worker {}", workerId, e);
        }
    }
    
    /**
     * Cleans up stale heartbeat records to prevent unbounded growth.
     * Removes heartbeat records for workers that have been inactive for extended periods.
     * Uses READ_COMMITTED isolation for cleanup operations.
     */
    @Transactional(isolation = Isolation.READ_COMMITTED, propagation = Propagation.REQUIRES_NEW)
    public void cleanupStaleHeartbeats() {
        try {
            // Clean up heartbeats older than 24 hours
            long cleanupThresholdMs = 24 * 60 * 60 * 1000; // 24 hours
            Instant cleanupCutoff = Instant.now().minusMillis(cleanupThresholdMs);
            
            int cleanedUp = workerHeartbeatRepository.cleanupStaleHeartbeats(cleanupCutoff);
            
            if (cleanedUp > 0) {
                logger.info("Cleaned up {} stale heartbeat records", cleanedUp);
            }
            
        } catch (Exception e) {
            logger.error("Error during heartbeat cleanup", e);
        }
    }
    
    /**
     * Logs a worker failure event for monitoring and alerting.
     * Creates structured log entries that can be consumed by monitoring systems.
     * 
     */
    private void logWorkerFailureEvent(WorkerHeartbeat failedWorker, int reassignedTasks) {
        String workerId = failedWorker.getWorkerId();
        Instant lastHeartbeat = failedWorker.getLastHeartbeat();
        Instant registeredAt = failedWorker.getRegisteredAt();
        
        
        long activeTimeMs = lastHeartbeat.toEpochMilli() - registeredAt.toEpochMilli();
        
     
        long timeSinceHeartbeatMs = Instant.now().toEpochMilli() - lastHeartbeat.toEpochMilli();
        
        logger.warn("WORKER_FAILURE_EVENT: workerId={}, lastHeartbeat={}, registeredAt={}, " +
                   "activeTimeMs={}, timeSinceHeartbeatMs={}, reassignedTasks={}", 
                   workerId, lastHeartbeat, registeredAt, activeTimeMs, timeSinceHeartbeatMs, reassignedTasks);
    }
    
    /**
     * Gets the current status of worker monitoring.
     * Returns information about active and failed workers for health checks.
     */
    public WorkerMonitoringStatus getMonitoringStatus() {
        try {
            long timeoutMs = config.getWorker().getHeartbeatTimeoutMs();
            Instant cutoffTime = Instant.now().minusMillis(timeoutMs);
            
            // Count active workers
            long activeWorkerCount = workerHeartbeatRepository.countActiveWorkers(cutoffTime);
            
            // Find stale workers
            List<WorkerHeartbeat> staleWorkers = workerHeartbeatRepository.findStaleWorkers(cutoffTime);
            
            return new WorkerMonitoringStatus(
                activeWorkerCount,
                staleWorkers.size(),
                timeoutMs,
                running
            );
            
        } catch (Exception e) {
            logger.error("Error getting monitoring status", e);
            return new WorkerMonitoringStatus(0, 0, 0, false);
        }
    }
    
    /**
     * Gets a list of currently active workers.
     * Used for monitoring and administrative purposes.
     */
    public List<WorkerHeartbeat> getActiveWorkers() {
        try {
            long timeoutMs = config.getWorker().getHeartbeatTimeoutMs();
            Instant cutoffTime = Instant.now().minusMillis(timeoutMs);
            
            return workerHeartbeatRepository.findActiveWorkers(cutoffTime);
            
        } catch (Exception e) {
            logger.error("Error getting active workers", e);
            return List.of();
        }
    }
    
    /**
     * Gets a list of workers that have failed (missed heartbeats).
     * Used for monitoring and debugging purposes.
     */
    public List<WorkerHeartbeat> getFailedWorkers() {
        try {
            long timeoutMs = config.getWorker().getHeartbeatTimeoutMs();
            Instant cutoffTime = Instant.now().minusMillis(timeoutMs);
            
            return workerHeartbeatRepository.findStaleWorkers(cutoffTime);
            
        } catch (Exception e) {
            logger.error("Error getting failed workers", e);
            return List.of();
        }
    }
    
    /**
     * Checks if the heartbeat monitor is currently running.
     */
    public boolean isRunning() {
        return running;
    }
    
    /**
     * Status information for worker monitoring.
     */
    public static class WorkerMonitoringStatus {
        private final long activeWorkerCount;
        private final long failedWorkerCount;
        private final long heartbeatTimeoutMs;
        private final boolean monitoringActive;
        
        public WorkerMonitoringStatus(long activeWorkerCount, long failedWorkerCount, 
                                    long heartbeatTimeoutMs, boolean monitoringActive) {
            this.activeWorkerCount = activeWorkerCount;
            this.failedWorkerCount = failedWorkerCount;
            this.heartbeatTimeoutMs = heartbeatTimeoutMs;
            this.monitoringActive = monitoringActive;
        }
        
        public long getActiveWorkerCount() {
            return activeWorkerCount;
        }
        
        public long getFailedWorkerCount() {
            return failedWorkerCount;
        }
        
        public long getHeartbeatTimeoutMs() {
            return heartbeatTimeoutMs;
        }
        
        public boolean isMonitoringActive() {
            return monitoringActive;
        }
        
        @Override
        public String toString() {
            return "WorkerMonitoringStatus{" +
                    "activeWorkerCount=" + activeWorkerCount +
                    ", failedWorkerCount=" + failedWorkerCount +
                    ", heartbeatTimeoutMs=" + heartbeatTimeoutMs +
                    ", monitoringActive=" + monitoringActive +
                    '}';
        }
    }
}