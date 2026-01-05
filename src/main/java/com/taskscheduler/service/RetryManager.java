package com.taskscheduler.service;

import java.time.Instant;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.taskscheduler.entity.Task;
import com.taskscheduler.entity.TaskStatus;
import com.taskscheduler.repository.TaskRepository;

/**
 * Service responsible for handling task failures and retry logic.
 * Implements exponential backoff retry scheduling and failure marking.
 */
@Service
public class RetryManager {
    
    private static final Logger logger = LoggerFactory.getLogger(RetryManager.class);
    

    private static final int DEFAULT_MAX_RETRIES = 3;
    private static final long BASE_DELAY_MS = 1000L; // 1 second
    private static final long MAX_DELAY_MS = 300000L; // 5 minutes
    
    private final TaskRepository taskRepository;
    

    @Value("${task.scheduler.retry.max-retries:3}")
    private int defaultMaxRetries;
    
    @Value("${task.scheduler.retry.base-delay-ms:1000}")
    private long baseDelayMs;
    
    @Value("${task.scheduler.retry.max-delay-ms:300000}")
    private long maxDelayMs;
    
    public RetryManager(TaskRepository taskRepository) {
        this.taskRepository = taskRepository;
    }
    
    /**
     * Handles a task failure by either rescheduling for retry or marking as permanently failed.
     * Implements exponential backoff for retry scheduling.
     * Uses SERIALIZABLE isolation to ensure consistent retry handling across concurrent operations.
     * @param task The failed task to handle
     * @return true if task was rescheduled for retry, false if marked as permanently failed
     */
    @Transactional(isolation = Isolation.SERIALIZABLE, propagation = Propagation.REQUIRES_NEW)
    public boolean handleTaskFailure(Task task) {
        if (task == null) {
            logger.warn("Cannot handle failure for null task");
            return false;
        }
        
        logger.info("Handling failure for task {} (retry count: {}, max retries: {})", 
                   task.getId(), task.getRetryCount(), task.getMaxRetries());
        
      
        if (task.getRetryCount() < task.getMaxRetries()) {
            return rescheduleTaskForRetry(task);
        } else {
            return markTaskAsPermanentlyFailed(task);
        }
    }
    
    /**
     * Reschedules a task for retry with exponential backoff delay.
     * Increments retry count and sets new schedule time.
     * @param task The task to reschedule
     * @return true if rescheduling was successful
     */
    private boolean rescheduleTaskForRetry(Task task) {
        try {
           
            long retryDelay = calculateRetryDelay(task.getRetryCount());
            Instant newScheduleTime = Instant.now().plusMillis(retryDelay);
            
            logger.info("Rescheduling task {} for retry in {} ms (attempt {} of {})", 
                       task.getId(), retryDelay, task.getRetryCount() + 1, task.getMaxRetries());
            

            int updated = taskRepository.incrementRetryAndReschedule(
                task.getId(),
                TaskStatus.PENDING,
                newScheduleTime,
                Instant.now()
            );
            
            if (updated > 0) {
                logger.info("Task {} successfully rescheduled for retry at {}", 
                           task.getId(), newScheduleTime);
                return true;
            } else {
                logger.warn("Failed to reschedule task {} - task may have been modified concurrently", 
                           task.getId());
                return false;
            }
            
        } catch (Exception e) {
            logger.error("Error rescheduling task {} for retry", task.getId(), e);
            return false;
        }
    }
    
    /**
     * Marks a task as permanently failed when retry limit is exceeded.
     * @param task The task to mark as failed
     * @return true if marking was successful
     */
    private boolean markTaskAsPermanentlyFailed(Task task) {
        try {
            logger.info("Marking task {} as permanently failed (retry limit {} exceeded)", 
                       task.getId(), task.getMaxRetries());
            
            int updated = taskRepository.updateTaskStatus(
                task.getId(),
                task.getStatus(), 
                TaskStatus.FAILED,
                Instant.now()
            );
            
            if (updated > 0) {
                logger.info("Task {} marked as permanently failed", task.getId());
                return true;
            } else {
                logger.warn("Failed to mark task {} as failed - task may have been modified concurrently", 
                           task.getId());
                return false;
            }
            
        } catch (Exception e) {
            logger.error("Error marking task {} as permanently failed", task.getId(), e);
            return false;
        }
    }
    
    /**
     * Calculates retry delay using exponential backoff algorithm.
     * Delay = baseDelay * (2 ^ retryCount), capped at maxDelay.
     * @param retryCount The current retry count (0-based)
     * @return Delay in milliseconds
     */
    public long calculateRetryDelay(int retryCount) {
        if (retryCount < 0) {
            return baseDelayMs;
        }
        
       
        long delay = baseDelayMs * (1L << retryCount); 
        
       
        return Math.min(delay, maxDelayMs);
    }
    
    /**
     * Processes all tasks that have exceeded their retry limit and marks them as failed.
     * This method can be called periodically to clean up tasks that should be permanently failed.
     * Uses READ_COMMITTED isolation for batch processing of failed tasks.
     * @return Number of tasks marked as permanently failed
     */
    @Transactional(isolation = Isolation.READ_COMMITTED, propagation = Propagation.REQUIRED)
    public int processTasksExceedingRetryLimit() {
        try {
            List<Task> tasksExceedingLimit = taskRepository.findTasksExceedingRetryLimit(TaskStatus.PENDING);
            
            int failedCount = 0;
            for (Task task : tasksExceedingLimit) {
                if (markTaskAsPermanentlyFailed(task)) {
                    failedCount++;
                }
            }
            
            if (failedCount > 0) {
                logger.info("Marked {} tasks as permanently failed due to retry limit exceeded", failedCount);
            }
            
            return failedCount;
            
        } catch (Exception e) {
            logger.error("Error processing tasks exceeding retry limit", e);
            return 0;
        }
    }
    
    /**
     * Applies default retry configuration to a task if not already set.
     * @param task The task to configure
     */
    public void applyDefaultRetryConfiguration(Task task) {
        if (task == null) {
            return;
        }
        
        
        if (task.getMaxRetries() == null || task.getMaxRetries() < 0) {
            task.setMaxRetries(defaultMaxRetries);
            logger.debug("Applied default max retries ({}) to task {}", defaultMaxRetries, task.getId());
        }
        
     
        if (task.getRetryCount() == null) {
            task.setRetryCount(0);
        }
    }
    
    /**
     * Checks if a task can be retried based on its current retry count.
     * 
     * @param task The task to check
     * @return true if task can be retried, false otherwise
     */
    public boolean canTaskBeRetried(Task task) {
        if (task == null) {
            return false;
        }
        
        return task.getRetryCount() < task.getMaxRetries();
    }
    
    /**
     * Gets the next retry time for a task based on its current retry count.
     * 
     * @param task The task to calculate retry time for
     * @return The next retry time, or null if task cannot be retried
     */
    public Instant getNextRetryTime(Task task) {
        if (!canTaskBeRetried(task)) {
            return null;
        }
        
        long delay = calculateRetryDelay(task.getRetryCount());
        return Instant.now().plusMillis(delay);
    }
    
    /**
     * Gets retry configuration information for monitoring/debugging.
     */
    public RetryConfiguration getRetryConfiguration() {
        return new RetryConfiguration(defaultMaxRetries, baseDelayMs, maxDelayMs);
    }
    
    /**
     * Configuration holder for retry settings.
     */
    public static class RetryConfiguration {
        private final int defaultMaxRetries;
        private final long baseDelayMs;
        private final long maxDelayMs;
        
        public RetryConfiguration(int defaultMaxRetries, long baseDelayMs, long maxDelayMs) {
            this.defaultMaxRetries = defaultMaxRetries;
            this.baseDelayMs = baseDelayMs;
            this.maxDelayMs = maxDelayMs;
        }
        
        public int getDefaultMaxRetries() {
            return defaultMaxRetries;
        }
        
        public long getBaseDelayMs() {
            return baseDelayMs;
        }
        
        public long getMaxDelayMs() {
            return maxDelayMs;
        }
        
        @Override
        public String toString() {
            return "RetryConfiguration{" +
                    "defaultMaxRetries=" + defaultMaxRetries +
                    ", baseDelayMs=" + baseDelayMs +
                    ", maxDelayMs=" + maxDelayMs +
                    '}';
        }
    }
}