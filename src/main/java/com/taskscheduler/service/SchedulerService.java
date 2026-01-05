package com.taskscheduler.service;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.taskscheduler.config.TaskSchedulerConfig;
import com.taskscheduler.entity.Task;
import com.taskscheduler.entity.TaskStatus;
import com.taskscheduler.repository.TaskRepository;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;

@Service
public class SchedulerService {
    
    private static final Logger logger = LoggerFactory.getLogger(SchedulerService.class);
    
    private final TaskRepository taskRepository;
    private final TaskSchedulerConfig config;
    private final ScheduledExecutorService schedulerExecutor;
    private final AtomicBoolean isRunning;
    private final String schedulerId;
    
    public SchedulerService(TaskRepository taskRepository, TaskSchedulerConfig config) {
        this.taskRepository = taskRepository;
        this.config = config;
        this.schedulerExecutor = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "task-scheduler");
            t.setDaemon(true);
            return t;
        });
        this.isRunning = new AtomicBoolean(false);
        this.schedulerId = "scheduler-" + UUID.randomUUID().toString().substring(0, 8);
        
        logger.info("SchedulerService initialized with ID: {}", schedulerId);
    }
    
    @PostConstruct
    public void startScheduler() {
        if (!config.getScheduler().isEnabled()) {
            logger.info("Scheduler is disabled in configuration, not starting");
            return;
        }
        
        if (isRunning.compareAndSet(false, true)) {
            long pollingInterval = config.getScheduler().getPollingIntervalMs();
            
            logger.info("Starting scheduler with polling interval: {}ms", pollingInterval);
            
            schedulerExecutor.scheduleWithFixedDelay(
                this::pollAndAssignTasks,
                0, // Initial delay
                pollingInterval,
                TimeUnit.MILLISECONDS
            );
            
            logger.info("Scheduler started successfully");
        } else {
            logger.warn("Scheduler is already running");
        }
    }
    
    @PreDestroy
    public void stopScheduler() {
        if (isRunning.compareAndSet(true, false)) {
            logger.info("Stopping scheduler...");
            
            schedulerExecutor.shutdown();
            
            try {
                if (!schedulerExecutor.awaitTermination(30, TimeUnit.SECONDS)) {
                    logger.warn("Scheduler did not terminate gracefully, forcing shutdown");
                    schedulerExecutor.shutdownNow();
                    
                    if (!schedulerExecutor.awaitTermination(5, TimeUnit.SECONDS)) {
                        logger.error("Scheduler did not terminate after forced shutdown");
                    }
                }
                
                logger.info("Scheduler stopped successfully");
                
            } catch (InterruptedException e) {
                logger.warn("Interrupted while waiting for scheduler shutdown");
                schedulerExecutor.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }
    }
    
    private void pollAndAssignTasks() {
        if (!isRunning.get()) {
            return;
        }
        
        try {
            Instant currentTime = Instant.now();
            
            List<Task> dueTasks = taskRepository.findDueTasks(TaskStatus.PENDING, currentTime);
            
            if (!dueTasks.isEmpty()) {
                logger.debug("Found {} due tasks to process", dueTasks.size());
                
                for (Task task : dueTasks) {
                    try {
                        assignTaskToWorker(task);
                    } catch (Exception e) {
                        logger.error("Error assigning task {} to worker", task.getId(), e);
                    }
                }
            }
            
        } catch (Exception e) {
            logger.error("Error during task polling cycle", e);
          
        }
    }
    
    /**
     * Atomically assigns a task to a worker.
     * Uses database atomic operations to prevent duplicate assignment.
     * Uses SERIALIZABLE isolation to ensure atomic task assignment across concurrent schedulers.
     */
    @Transactional(isolation = Isolation.SERIALIZABLE, propagation = Propagation.REQUIRES_NEW)
    public boolean assignTaskToWorker(Task task) {
        try {
            
            String workerId = generateWorkerAssignment();
            Instant assignedAt = Instant.now();
            
       
            int rowsUpdated = taskRepository.atomicAssignTask(
                task.getId(),
                TaskStatus.PENDING,  
                TaskStatus.RUNNING,  
                workerId,
                assignedAt
            );
            
            if (rowsUpdated == 1) {
                
                logger.info("Successfully assigned task {} to worker {} at {}", 
                           task.getId(), workerId, assignedAt);
                
             
                logTaskAssignment(task, workerId, assignedAt);
                
                return true;
                
            } else {
                
                logger.debug("Failed to assign task {} - already claimed by another scheduler", 
                            task.getId());
                return false;
            }
            
        } catch (Exception e) {
            logger.error("Error during atomic task assignment for task {}", task.getId(), e);
            return false;
        }
    }
    
   
    private String generateWorkerAssignment() {
        
        return "worker-" + UUID.randomUUID().toString().substring(0, 8);
    }
    
    
    private void logTaskAssignment(Task task, String workerId, Instant assignedAt) {
        logger.info("TASK_ASSIGNED: taskId={}, taskType={}, workerId={}, schedulerId={}, assignedAt={}", 
                   task.getId(), task.getType(), workerId, schedulerId, assignedAt);
    }
    
    
    public boolean isRunning() {
        return isRunning.get();
    }
    
   
    public String getSchedulerId() {
        return schedulerId;
    }
    
    
    public long getPollingIntervalMs() {
        return config.getScheduler().getPollingIntervalMs();
    }
    
   
    public void triggerPollingCycle() {
        if (isRunning.get()) {
            logger.debug("Manually triggering polling cycle");
            pollAndAssignTasks();
        } else {
            logger.warn("Cannot trigger polling cycle - scheduler is not running");
        }
    }
}