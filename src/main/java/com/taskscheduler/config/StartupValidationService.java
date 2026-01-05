package com.taskscheduler.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Service;

import com.taskscheduler.repository.TaskExecutionRepository;
import com.taskscheduler.repository.TaskRepository;
import com.taskscheduler.repository.WorkerHeartbeatRepository;
import com.taskscheduler.service.SystemRecoveryService;

/**
 * Service that validates system readiness on startup.
 * Performs database connectivity checks and system recovery.
 */
@Service
public class StartupValidationService {
    
    private static final Logger logger = LoggerFactory.getLogger(StartupValidationService.class);
    
    private final TaskRepository taskRepository;
    private final TaskExecutionRepository taskExecutionRepository;
    private final WorkerHeartbeatRepository workerHeartbeatRepository;
    private final SystemRecoveryService systemRecoveryService;
    
    @Autowired
    public StartupValidationService(
            TaskRepository taskRepository,
            TaskExecutionRepository taskExecutionRepository,
            WorkerHeartbeatRepository workerHeartbeatRepository,
            SystemRecoveryService systemRecoveryService) {
        this.taskRepository = taskRepository;
        this.taskExecutionRepository = taskExecutionRepository;
        this.workerHeartbeatRepository = workerHeartbeatRepository;
        this.systemRecoveryService = systemRecoveryService;
    }
    
    /**
     * Validates system readiness and performs recovery on application startup.
     * This runs before other services start to ensure data consistency.
     */
    @EventListener(ApplicationReadyEvent.class)
    @Order(0) 
    public void validateSystemReadiness() {
        logger.info("=== Starting System Validation and Recovery ===");
        
        try {
        
            validateDatabaseConnectivity();
          
            performSystemRecovery();
            
            logSystemStatistics();
            
            logger.info("=== System Validation and Recovery Completed Successfully ===");
            
        } catch (Exception e) {
            logger.error("System validation failed - application may not function correctly", e);
            throw new RuntimeException("System validation failed", e);
        }
    }
    
    /**
     * Validates that all required database tables are accessible.
     */
    private void validateDatabaseConnectivity() {
        logger.info("Validating database connectivity...");
        
        try {
            // Test basic connectivity by counting records
            long taskCount = taskRepository.count();
            long executionCount = taskExecutionRepository.count();
            long heartbeatCount = workerHeartbeatRepository.count();
            
            logger.info("Database connectivity validated - Tasks: {}, Executions: {}, Heartbeats: {}", 
                       taskCount, executionCount, heartbeatCount);
            
        } catch (Exception e) {
            logger.error("Database connectivity validation failed", e);
            throw new RuntimeException("Database connectivity validation failed", e);
        }
    }
    
    /**
     * Performs system recovery to handle any tasks left in inconsistent state.
     */
    private void performSystemRecovery() {
        logger.info("Performing system recovery...");
        
        try {
            systemRecoveryService.performStartupRecovery();
            logger.info("System recovery completed successfully");
            
        } catch (Exception e) {
            logger.error("System recovery failed", e);
            throw new RuntimeException("System recovery failed", e);
        }
    }
    
    /**
     * Logs current system statistics for monitoring and debugging.
     */
    private void logSystemStatistics() {
        try {
            long totalTasks = taskRepository.count();
            long pendingTasks = taskRepository.countByStatus(com.taskscheduler.entity.TaskStatus.PENDING);
            long runningTasks = taskRepository.countByStatus(com.taskscheduler.entity.TaskStatus.RUNNING);
            long successTasks = taskRepository.countByStatus(com.taskscheduler.entity.TaskStatus.SUCCESS);
            long failedTasks = taskRepository.countByStatus(com.taskscheduler.entity.TaskStatus.FAILED);
            
            long activeWorkers = workerHeartbeatRepository.count();
            long totalExecutions = taskExecutionRepository.count();
            
            logger.info("=== System Statistics ===");
            logger.info("Tasks - Total: {}, Pending: {}, Running: {}, Success: {}, Failed: {}", 
                       totalTasks, pendingTasks, runningTasks, successTasks, failedTasks);
            logger.info("Workers - Active: {}", activeWorkers);
            logger.info("Executions - Total: {}", totalExecutions);
            logger.info("========================");
            
        } catch (Exception e) {
            logger.warn("Failed to collect system statistics", e);
        }
    }
}