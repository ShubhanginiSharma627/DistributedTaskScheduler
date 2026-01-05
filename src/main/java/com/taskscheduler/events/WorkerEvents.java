package com.taskscheduler.events;

import java.time.Instant;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.taskscheduler.util.LoggingContext;

/**
 * Event logging component for worker-related events.
 * Provides structured logging for worker lifecycle and activity events.
 */
@Component
public class WorkerEvents {
    
    private static final Logger logger = LoggerFactory.getLogger(WorkerEvents.class);
    
    /**
     * Logs worker registration event.
     */
    public void logWorkerRegistered(String workerId, Instant registeredAt, String metadata) {
        LoggingContext.withWorkerContext(workerId, null, () -> {
            logger.info("WORKER_REGISTERED - Worker registered: registeredAt={}, metadata={}", 
                    registeredAt, metadata);
        });
    }
    
    /**
     * Logs worker heartbeat event.
     */
    public void logWorkerHeartbeat(String workerId, Instant heartbeatTime) {
        LoggingContext.withWorkerContext(workerId, null, () -> {
            logger.debug("WORKER_HEARTBEAT - Heartbeat received: heartbeatTime={}", heartbeatTime);
        });
    }
    
    /**
     * Logs worker failure detection event.
     */
    public void logWorkerFailureDetected(String workerId, Instant lastHeartbeat, long timeoutMs) {
        LoggingContext.withWorkerContext(workerId, null, () -> {
            logger.warn("WORKER_FAILURE_DETECTED - Worker failure detected: lastHeartbeat={}, timeoutMs={}", 
                    lastHeartbeat, timeoutMs);
        });
    }
    
    /**
     * Logs worker recovery event.
     */
    public void logWorkerRecovered(String workerId, Instant recoveryTime) {
        LoggingContext.withWorkerContext(workerId, null, () -> {
            logger.info("WORKER_RECOVERED - Worker recovered: recoveryTime={}", recoveryTime);
        });
    }
    
    /**
     * Logs worker shutdown event.
     */
    public void logWorkerShutdown(String workerId, String reason) {
        LoggingContext.withWorkerContext(workerId, null, () -> {
            logger.info("WORKER_SHUTDOWN - Worker shutdown: reason={}", reason);
        });
    }
    
    /**
     * Logs worker task polling event.
     */
    public void logWorkerTaskPoll(String workerId, int tasksRetrieved) {
        LoggingContext.withWorkerContext(workerId, null, () -> {
            logger.debug("WORKER_TASK_POLL - Worker polled for tasks: tasksRetrieved={}", tasksRetrieved);
        });
    }
    
    /**
     * Logs worker task execution start.
     */
    public void logWorkerTaskExecutionStart(String workerId, String taskId, String taskType) {
        LoggingContext.withWorkerContext(workerId, null, () -> {
            LoggingContext.setTaskId(taskId);
            logger.info("WORKER_TASK_EXECUTION_START - Worker started task execution: taskId={}, taskType={}", 
                    taskId, taskType);
        });
    }
    
    /**
     * Logs worker task execution completion.
     */
    public void logWorkerTaskExecutionComplete(String workerId, String taskId, boolean success, long durationMs) {
        LoggingContext.withWorkerContext(workerId, null, () -> {
            LoggingContext.setTaskId(taskId);
            if (success) {
                logger.info("WORKER_TASK_EXECUTION_SUCCESS - Worker completed task successfully: taskId={}, durationMs={}", 
                        taskId, durationMs);
            } else {
                logger.warn("WORKER_TASK_EXECUTION_FAILED - Worker task execution failed: taskId={}, durationMs={}", 
                        taskId, durationMs);
            }
        });
    }
    
    /**
     * Logs worker error event.
     */
    public void logWorkerError(String workerId, String operation, String error, Exception exception) {
        LoggingContext.withWorkerContext(workerId, null, () -> {
            LoggingContext.setOperation(operation);
            if (exception != null) {
                logger.error("WORKER_ERROR - Worker error during operation: operation={}, error={}", 
                        operation, error, exception);
            } else {
                logger.error("WORKER_ERROR - Worker error during operation: operation={}, error={}", 
                        operation, error);
            }
        });
    }
    
    /**
     * Logs worker performance metrics.
     */
    public void logWorkerMetrics(String workerId, int tasksProcessed, long avgExecutionTimeMs, 
                                double successRate, Instant periodStart, Instant periodEnd) {
        LoggingContext.withWorkerContext(workerId, null, () -> {
            logger.info("WORKER_METRICS - Worker performance metrics: tasksProcessed={}, avgExecutionTimeMs={}, " +
                    "successRate={}%, periodStart={}, periodEnd={}", 
                    tasksProcessed, avgExecutionTimeMs, successRate, periodStart, periodEnd);
        });
    }
    
    /**
     * Logs worker status change.
     */
    public void logWorkerStatusChange(String workerId, String fromStatus, String toStatus, String reason) {
        LoggingContext.withWorkerContext(workerId, null, () -> {
            logger.info("WORKER_STATUS_CHANGE - Worker status changed: from={}, to={}, reason={}", 
                    fromStatus, toStatus, reason);
        });
    }
    
    /**
     * Logs worker cleanup event.
     */
    public void logWorkerCleanup(String workerId, int abandonedTasks) {
        LoggingContext.withWorkerContext(workerId, null, () -> {
            logger.info("WORKER_CLEANUP - Worker cleanup completed: abandonedTasks={}", abandonedTasks);
        });
    }
    
    /**
     * Logs worker connection event.
     */
    public void logWorkerConnection(String workerId, String connectionType, boolean connected) {
        LoggingContext.withWorkerContext(workerId, null, () -> {
            if (connected) {
                logger.info("WORKER_CONNECTED - Worker connected: connectionType={}", connectionType);
            } else {
                logger.warn("WORKER_DISCONNECTED - Worker disconnected: connectionType={}", connectionType);
            }
        });
    }
    
    /**
     * Logs worker resource usage.
     */
    public void logWorkerResourceUsage(String workerId, double cpuUsage, long memoryUsageMB, 
                                      int activeThreads) {
        LoggingContext.withWorkerContext(workerId, null, () -> {
            logger.debug("WORKER_RESOURCE_USAGE - Worker resource usage: cpuUsage={}%, memoryUsageMB={}, activeThreads={}", 
                    cpuUsage, memoryUsageMB, activeThreads);
        });
    }
}