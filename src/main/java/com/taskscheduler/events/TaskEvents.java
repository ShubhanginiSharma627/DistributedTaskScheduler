package com.taskscheduler.events;

import java.time.Instant;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.taskscheduler.entity.Task;
import com.taskscheduler.entity.TaskStatus;
import com.taskscheduler.util.LoggingContext;

/**
 * Event logging component for task-related events.
 * Provides structured logging for task state transitions and lifecycle events.
 */
@Component
public class TaskEvents {
    
    private static final Logger logger = LoggerFactory.getLogger(TaskEvents.class);
    
    /**
     * Logs task creation event.
     */
    public void logTaskCreated(Task task) {
        LoggingContext.withTaskContext(task.getId().toString(), null, () -> {
            logger.info("TASK_CREATED - Task created: type={}, scheduleAt={}, maxRetries={}", 
                    task.getType(), task.getScheduleAt(), task.getMaxRetries());
        });
    }
    
    /**
     * Logs task status change event.
     */
    public void logTaskStatusChanged(UUID taskId, TaskStatus fromStatus, TaskStatus toStatus, String workerId) {
        LoggingContext.withTaskContext(taskId.toString(), null, () -> {
            if (workerId != null) {
                LoggingContext.setWorkerId(workerId);
            }
            logger.info("TASK_STATUS_CHANGED - Status changed: from={}, to={}, workerId={}", 
                    fromStatus, toStatus, workerId);
        });
    }
    
    /**
     * Logs task assignment event.
     */
    public void logTaskAssigned(UUID taskId, String workerId, Instant assignedAt) {
        LoggingContext.withTaskContext(taskId.toString(), null, () -> {
            LoggingContext.setWorkerId(workerId);
            logger.info("TASK_ASSIGNED - Task assigned to worker: workerId={}, assignedAt={}", 
                    workerId, assignedAt);
        });
    }
    
    /**
     * Logs task execution started event.
     */
    public void logTaskExecutionStarted(UUID taskId, String workerId) {
        LoggingContext.withTaskContext(taskId.toString(), null, () -> {
            LoggingContext.setWorkerId(workerId);
            logger.info("TASK_EXECUTION_STARTED - Task execution started by worker: workerId={}", workerId);
        });
    }
    
    /**
     * Logs task execution completed event.
     */
    public void logTaskExecutionCompleted(UUID taskId, String workerId, boolean success, 
                                        long executionTimeMs, String output) {
        LoggingContext.withTaskContext(taskId.toString(), null, () -> {
            LoggingContext.setWorkerId(workerId);
            if (success) {
                logger.info("TASK_EXECUTION_SUCCESS - Task completed successfully: workerId={}, executionTimeMs={}, outputLength={}", 
                        workerId, executionTimeMs, output != null ? output.length() : 0);
            } else {
                logger.warn("TASK_EXECUTION_FAILED - Task execution failed: workerId={}, executionTimeMs={}, error={}", 
                        workerId, executionTimeMs, output);
            }
        });
    }
    
    /**
     * Logs task retry event.
     */
    public void logTaskRetry(UUID taskId, int retryCount, int maxRetries, Instant nextScheduleAt) {
        LoggingContext.withTaskContext(taskId.toString(), null, () -> {
            logger.info("TASK_RETRY - Task scheduled for retry: retryCount={}, maxRetries={}, nextScheduleAt={}", 
                    retryCount, maxRetries, nextScheduleAt);
        });
    }
    
    /**
     * Logs task failure after max retries exceeded.
     */
    public void logTaskFailedPermanently(UUID taskId, int retryCount) {
        LoggingContext.withTaskContext(taskId.toString(), null, () -> {
            logger.error("TASK_FAILED_PERMANENTLY - Task failed permanently after {} retries", retryCount);
        });
    }
    
    /**
     * Logs task cancellation event.
     */
    public void logTaskCancelled(UUID taskId, String reason) {
        LoggingContext.withTaskContext(taskId.toString(), null, () -> {
            logger.info("TASK_CANCELLED - Task cancelled: reason={}", reason);
        });
    }
    
    /**
     * Logs task reassignment due to worker failure.
     */
    public void logTaskReassigned(UUID taskId, String fromWorkerId, String reason) {
        LoggingContext.withTaskContext(taskId.toString(), null, () -> {
            LoggingContext.setWorkerId(fromWorkerId);
            logger.warn("TASK_REASSIGNED - Task reassigned due to worker failure: fromWorkerId={}, reason={}", 
                    fromWorkerId, reason);
        });
    }
    
    /**
     * Logs task timeout event.
     */
    public void logTaskTimeout(UUID taskId, String workerId, long timeoutMs) {
        LoggingContext.withTaskContext(taskId.toString(), null, () -> {
            LoggingContext.setWorkerId(workerId);
            logger.warn("TASK_TIMEOUT - Task execution timed out: workerId={}, timeoutMs={}", 
                    workerId, timeoutMs);
        });
    }
    
    /**
     * Logs task validation error.
     */
    public void logTaskValidationError(String taskData, String validationError) {
        logger.warn("TASK_VALIDATION_ERROR - Task validation failed: error={}, taskData={}", 
                validationError, taskData);
    }
    
    /**
     * Logs task scheduling error.
     */
    public void logTaskSchedulingError(UUID taskId, String error) {
        LoggingContext.withTaskContext(taskId.toString(), null, () -> {
            logger.error("TASK_SCHEDULING_ERROR - Task scheduling failed: error={}", error);
        });
    }
    
    /**
     * Logs bulk task operations.
     */
    public void logBulkTaskOperation(String operation, int taskCount, long durationMs) {
        logger.info("BULK_TASK_OPERATION - Bulk operation completed: operation={}, taskCount={}, durationMs={}", 
                operation, taskCount, durationMs);
    }
    
    /**
     * Logs task metrics summary.
     */
    public void logTaskMetrics(long totalTasks, long pendingTasks, long runningTasks, 
                              long successTasks, long failedTasks) {
        logger.info("TASK_METRICS - Current task statistics: total={}, pending={}, running={}, success={}, failed={}", 
                totalTasks, pendingTasks, runningTasks, successTasks, failedTasks);
    }
}