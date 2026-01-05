package com.taskscheduler.service;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.taskscheduler.config.TaskSchedulerConfig;
import com.taskscheduler.entity.Task;
import com.taskscheduler.entity.TaskStatus;
import com.taskscheduler.entity.WorkerHeartbeat;
import com.taskscheduler.repository.TaskRepository;
import com.taskscheduler.repository.WorkerHeartbeatRepository;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;

/**
 * Service for worker task processing functionality.
 * Handles task polling, execution coordination, worker registration, and heartbeat sending.
 */
@Service
public class WorkerService {
    
    private static final Logger logger = LoggerFactory.getLogger(WorkerService.class);
    
    private final TaskRepository taskRepository;
    private final WorkerHeartbeatRepository workerHeartbeatRepository;
    private final TaskExecutionService taskExecutionService;
    private final TaskSchedulerConfig config;
    private final ObjectMapper objectMapper;
    
    private final String workerId;
    private ScheduledExecutorService executorService;
    private volatile boolean running = false;
    
    public WorkerService(TaskRepository taskRepository,
                        WorkerHeartbeatRepository workerHeartbeatRepository,
                        TaskExecutionService taskExecutionService,
                        TaskSchedulerConfig config,
                        ObjectMapper objectMapper) {
        this.taskRepository = taskRepository;
        this.workerHeartbeatRepository = workerHeartbeatRepository;
        this.taskExecutionService = taskExecutionService;
        this.config = config;
        this.objectMapper = objectMapper;
        this.workerId = generateWorkerId();
    }
    
    /**
     * Initializes the worker service and starts background processing.
     * Registers the worker and starts heartbeat and task polling threads.
     */
    @PostConstruct
    public void initialize() {
        if (!config.getWorker().isEnabled()) {
            logger.info("Worker service is disabled in configuration");
            return;
        }
        
        logger.info("Initializing worker service with ID: {}", workerId);
        
        executorService = new ScheduledThreadPoolExecutor(2, r -> {
            Thread t = new Thread(r, "worker-" + workerId);
            t.setDaemon(true);
            return t;
        });
        
        registerWorker();
        
        running = true;
        startHeartbeatSender();
        startTaskProcessor();
        
        logger.info("Worker service started successfully");
    }
    
   
    @PreDestroy
    public void shutdown() {
        logger.info("Shutting down worker service: {}", workerId);
        
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
        
        logger.info("Worker service shutdown complete");
    }
    
    /**
     * Registers the worker with the system by creating a heartbeat record.
     * Uses READ_COMMITTED isolation for worker registration consistency.
     */
    @Transactional(isolation = Isolation.READ_COMMITTED, propagation = Propagation.REQUIRED)
    public void registerWorker() {
        try {
            WorkerHeartbeat heartbeat = workerHeartbeatRepository.findByWorkerId(workerId)
                    .orElse(new WorkerHeartbeat(workerId));
            
            
            WorkerMetadata metadata = new WorkerMetadata();
            metadata.setHostname(getHostname());
            metadata.setProcessId(getProcessId());
            metadata.setStartTime(Instant.now());
            
            String metadataJson = objectMapper.writeValueAsString(metadata);
            heartbeat.setWorkerMetadata(metadataJson);
            heartbeat.updateHeartbeat();
            
            workerHeartbeatRepository.save(heartbeat);
            
            logger.info("Worker {} registered successfully", workerId);
            
        } catch (Exception e) {
            logger.error("Failed to register worker {}", workerId, e);
            throw new RuntimeException("Worker registration failed", e);
        }
    }
    
   
    private void startHeartbeatSender() {
        long intervalMs = config.getWorker().getHeartbeatIntervalMs();
        
        executorService.scheduleWithFixedDelay(() -> {
            if (!running) {
                return;
            }
            
            try {
                sendHeartbeat();
            } catch (Exception e) {
                logger.error("Error sending heartbeat for worker {}", workerId, e);
            }
        }, intervalMs, intervalMs, TimeUnit.MILLISECONDS);
        
        logger.info("Heartbeat sender started with interval: {}ms", intervalMs);
    }
    
   
    private void startTaskProcessor() {
        executorService.scheduleWithFixedDelay(() -> {
            if (!running) {
                return;
            }
            
            try {
                processAssignedTasks();
            } catch (Exception e) {
                logger.error("Error processing tasks for worker {}", workerId, e);
            }
        }, 1000, 1000, TimeUnit.MILLISECONDS);
        
        logger.info("Task processor started for worker {}", workerId);
    }
    
    /**
     * Sends a heartbeat signal to indicate worker liveness.
     * Updates the last heartbeat timestamp in the database.
     * Uses READ_COMMITTED isolation for heartbeat updates.
     */
    @Transactional(isolation = Isolation.READ_COMMITTED, propagation = Propagation.REQUIRES_NEW)
    public void sendHeartbeat() {
        try {
            Instant now = Instant.now();
            int updated = workerHeartbeatRepository.updateHeartbeat(workerId, now);
            
            if (updated == 0) {
                // Worker record doesn't exist, create it
                registerWorker();
            }
            
            logger.debug("Heartbeat sent for worker {}", workerId);
            
        } catch (Exception e) {
            logger.error("Failed to send heartbeat for worker {}", workerId, e);
        }
    }
    
    /**
     * Processes tasks assigned to this worker.
     * Retrieves assigned tasks and executes them using TaskExecutionService.
     * Uses READ_COMMITTED isolation for consistent task retrieval.
     */
    @Transactional(isolation = Isolation.READ_COMMITTED, propagation = Propagation.REQUIRED)
    public void processAssignedTasks() {
        try {
           
            List<Task> assignedTasks = taskRepository.findTasksByWorkerAndStatus(workerId, TaskStatus.RUNNING);
            
            if (assignedTasks.isEmpty()) {
                logger.debug("No assigned tasks found for worker {}", workerId);
                return;
            }
            
            logger.info("Found {} assigned tasks for worker {}", assignedTasks.size(), workerId);
            
        
            for (Task task : assignedTasks) {
                try {
                    logger.info("Processing task {} for worker {}", task.getId(), workerId);
                    
                   
                    ExecutionResult result = taskExecutionService.executeTask(task, workerId);
                    
                    
                    reportTaskResult(task, result);
                    
                } catch (Exception e) {
                    logger.error("Error processing task {} for worker {}", task.getId(), workerId, e);
                    
               
                    ExecutionResult failureResult = ExecutionResult.failure("Worker processing error: " + e.getMessage());
                    reportTaskResult(task, failureResult);
                }
            }
            
        } catch (Exception e) {
            logger.error("Error retrieving assigned tasks for worker {}", workerId, e);
        }
    }
    
   
    private void reportTaskResult(Task task, ExecutionResult result) {
        if (result.isSuccess()) {
            logger.info("Task {} completed successfully by worker {}", task.getId(), workerId);
        } else {
            logger.warn("Task {} failed for worker {}: {}", task.getId(), workerId, result.getErrorMessage());
        }
        

    }
    
 
    private String generateWorkerId() {
        String hostname = getHostname();
        String uuid = UUID.randomUUID().toString().substring(0, 8);
        return hostname + "-" + uuid;
    }
    
 
    private String getHostname() {
        try {
            return java.net.InetAddress.getLocalHost().getHostName();
        } catch (Exception e) {
            logger.warn("Could not determine hostname, using 'unknown'", e);
            return "unknown";
        }
    }
    
  
    private String getProcessId() {
        try {
            return String.valueOf(ProcessHandle.current().pid());
        } catch (Exception e) {
            logger.warn("Could not determine process ID, using 'unknown'", e);
            return "unknown";
        }
    }

    public String getWorkerId() {
        return workerId;
    }
    
    
    public boolean isRunning() {
        return running;
    }
    
 
    public int getAssignedTaskCount() {
        try {
            List<Task> assignedTasks = taskRepository.findTasksByWorkerAndStatus(workerId, TaskStatus.RUNNING);
            return assignedTasks.size();
        } catch (Exception e) {
            logger.error("Error getting assigned task count for worker {}", workerId, e);
            return 0;
        }
    }
    
    /**
     * Worker metadata for tracking worker information.
     */
    public static class WorkerMetadata {
        private String hostname;
        private String processId;
        private Instant startTime;
        
        public String getHostname() {
            return hostname;
        }
        
        public void setHostname(String hostname) {
            this.hostname = hostname;
        }
        
        public String getProcessId() {
            return processId;
        }
        
        public void setProcessId(String processId) {
            this.processId = processId;
        }
        
        public Instant getStartTime() {
            return startTime;
        }
        
        public void setStartTime(Instant startTime) {
            this.startTime = startTime;
        }
    }
}