package com.taskscheduler.service;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.taskscheduler.entity.Task;
import com.taskscheduler.entity.TaskExecution;
import com.taskscheduler.entity.TaskStatus;
import com.taskscheduler.entity.TaskType;
import com.taskscheduler.repository.TaskRepository;

@Service
public class TaskService {
    
    private static final Logger logger = LoggerFactory.getLogger(TaskService.class);
    
    private final TaskRepository taskRepository;
    private final TaskExecutionService taskExecutionService;
    private final RetryManager retryManager;
    
    public TaskService(TaskRepository taskRepository, 
                      TaskExecutionService taskExecutionService,
                      RetryManager retryManager) {
        this.taskRepository = taskRepository;
        this.taskExecutionService = taskExecutionService;
        this.retryManager = retryManager;
    }
    
    @Transactional(isolation = Isolation.READ_COMMITTED, propagation = Propagation.REQUIRED)
    public Task createTask(TaskType type, String payload, Instant scheduleAt, Integer maxRetries) {
        logger.info("Creating new task: type={}, scheduleAt={}", type, scheduleAt);
        
        Task task = new Task(type, payload, scheduleAt, maxRetries);
        
        retryManager.applyDefaultRetryConfiguration(task);
        
        Task savedTask = taskRepository.save(task);
        
        logger.info("Task created successfully: id={}, type={}, status={}", 
                   savedTask.getId(), savedTask.getType(), savedTask.getStatus());
        
        return savedTask;
    }
    
    @Transactional(isolation = Isolation.READ_COMMITTED, propagation = Propagation.REQUIRED, readOnly = true)
    public Optional<TaskWithHistory> getTaskWithHistory(UUID taskId) {
        logger.debug("Retrieving task with history: id={}", taskId);
        
        Optional<Task> taskOptional = taskRepository.findById(taskId);
        
        if (taskOptional.isEmpty()) {
            logger.debug("Task not found: id={}", taskId);
            return Optional.empty();
        }
        
        Task task = taskOptional.get();
        List<TaskExecution> executionHistory = taskExecutionService.getTaskExecutionHistory(task);
        
        return Optional.of(new TaskWithHistory(task, executionHistory));
    }
    
    @Transactional(isolation = Isolation.READ_COMMITTED, propagation = Propagation.REQUIRED, readOnly = true)
    public Optional<Task> getTask(UUID taskId) {
        logger.debug("Retrieving task: id={}", taskId);
        return taskRepository.findById(taskId);
    }
    
    /**
     * Lists tasks with filtering and pagination.
     * Uses READ_COMMITTED isolation for consistent task listing.
     */
    @Transactional(isolation = Isolation.READ_COMMITTED, propagation = Propagation.REQUIRED, readOnly = true)
    public Page<Task> listTasks(TaskStatus status, TaskType type, Pageable pageable) {
        logger.debug("Listing tasks: status={}, type={}, page={}", status, type, pageable.getPageNumber());
        
        Page<Task> taskPage;
        
        if (status != null && type != null) {
            taskPage = taskRepository.findByStatusAndType(status, type, pageable);
        } else if (status != null) {
            taskPage = taskRepository.findByStatus(status, pageable);
        } else if (type != null) {
            taskPage = taskRepository.findByType(type, pageable);
        } else {
            taskPage = taskRepository.findAll(pageable);
        }
        
        logger.debug("Found {} tasks (total: {})", taskPage.getNumberOfElements(), taskPage.getTotalElements());
        return taskPage;
    }
    
    @Transactional(isolation = Isolation.SERIALIZABLE, propagation = Propagation.REQUIRED)
    public boolean cancelTask(UUID taskId) {
        logger.info("Attempting to cancel task: id={}", taskId);
        
        Optional<Task> taskOptional = taskRepository.findById(taskId);
        
        if (taskOptional.isEmpty()) {
            logger.warn("Cannot cancel task - not found: id={}", taskId);
            return false;
        }
        
        Task task = taskOptional.get();
        
        if (task.getStatus() != TaskStatus.PENDING) {
            logger.warn("Cannot cancel task - not in PENDING status: id={}, status={}", 
                       taskId, task.getStatus());
            return false;
        }
        
        taskRepository.delete(task);
        
        logger.info("Task cancelled successfully: id={}", taskId);
        return true;
    }
    
    @Transactional(isolation = Isolation.SERIALIZABLE, propagation = Propagation.REQUIRED)
    public boolean updateTaskStatus(UUID taskId, TaskStatus currentStatus, TaskStatus newStatus) {
        logger.debug("Updating task status: id={}, from={}, to={}", taskId, currentStatus, newStatus);
        
        int updated = taskRepository.updateTaskStatus(taskId, currentStatus, newStatus, Instant.now());
        
        if (updated > 0) {
            logger.info("Task status updated successfully: id={}, status={}", taskId, newStatus);
            return true;
        } else {
            logger.warn("Failed to update task status - concurrent modification: id={}", taskId);
            return false;
        }
    }
    
    @Transactional(isolation = Isolation.READ_COMMITTED, propagation = Propagation.REQUIRED, readOnly = true)
    public TaskStatusCounts getTaskStatusCounts() {
        logger.debug("Getting task status counts");
        
        long pendingCount = taskRepository.countByStatus(TaskStatus.PENDING);
        long runningCount = taskRepository.countByStatus(TaskStatus.RUNNING);
        long successCount = taskRepository.countByStatus(TaskStatus.SUCCESS);
        long failedCount = taskRepository.countByStatus(TaskStatus.FAILED);
        
        return new TaskStatusCounts(pendingCount, runningCount, successCount, failedCount);
    }
    
    @Transactional(isolation = Isolation.READ_COMMITTED, propagation = Propagation.REQUIRED, readOnly = true)
    public boolean existsInStatus(UUID taskId, TaskStatus status) {
        Optional<Task> taskOptional = taskRepository.findById(taskId);
        return taskOptional.isPresent() && taskOptional.get().getStatus() == status;
    }
    
    public static class TaskWithHistory {
        private final Task task;
        private final List<TaskExecution> executionHistory;
        
        public TaskWithHistory(Task task, List<TaskExecution> executionHistory) {
            this.task = task;
            this.executionHistory = executionHistory;
        }
        
        public Task getTask() {
            return task;
        }
        
        public List<TaskExecution> getExecutionHistory() {
            return executionHistory;
        }
    }
    
    /**
     * Container for task status counts.
     */
    public static class TaskStatusCounts {
        private final long pendingCount;
        private final long runningCount;
        private final long successCount;
        private final long failedCount;
        
        public TaskStatusCounts(long pendingCount, long runningCount, long successCount, long failedCount) {
            this.pendingCount = pendingCount;
            this.runningCount = runningCount;
            this.successCount = successCount;
            this.failedCount = failedCount;
        }
        
        public long getPendingCount() {
            return pendingCount;
        }
        
        public long getRunningCount() {
            return runningCount;
        }
        
        public long getSuccessCount() {
            return successCount;
        }
        
        public long getFailedCount() {
            return failedCount;
        }
        
        public long getTotalCount() {
            return pendingCount + runningCount + successCount + failedCount;
        }
        
        @Override
        public String toString() {
            return "TaskStatusCounts{" +
                    "pending=" + pendingCount +
                    ", running=" + runningCount +
                    ", success=" + successCount +
                    ", failed=" + failedCount +
                    ", total=" + getTotalCount() +
                    '}';
        }
    }
}