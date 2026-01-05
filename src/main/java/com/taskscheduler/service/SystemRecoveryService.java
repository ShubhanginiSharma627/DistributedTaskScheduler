package com.taskscheduler.service;

import java.time.Instant;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.taskscheduler.entity.Task;
import com.taskscheduler.entity.TaskStatus;
import com.taskscheduler.repository.TaskRepository;
import com.taskscheduler.repository.WorkerHeartbeatRepository;

/**
 * Service responsible for system recovery operations during startup.
 * Handles recovery of RUNNING tasks and cleanup of stale worker data.
 */
@Service
public class SystemRecoveryService {
    
    private static final Logger logger = LoggerFactory.getLogger(SystemRecoveryService.class);
    
    private final TaskRepository taskRepository;
    private final WorkerHeartbeatRepository workerHeartbeatRepository;
    
    @Autowired
    public SystemRecoveryService(TaskRepository taskRepository, 
                                WorkerHeartbeatRepository workerHeartbeatRepository) {
        this.taskRepository = taskRepository;
        this.workerHeartbeatRepository = workerHeartbeatRepository;
    }
    
    /**
     * Performs system recovery operations when the application starts.
     * This method is called after the application context is fully initialized.
     */
    @EventListener(ApplicationReadyEvent.class)
    @Transactional
    public void performStartupRecovery() {
        logger.info("Starting system recovery process...");
        
        try {
            
            int recoveredTasks = recoverRunningTasks();
            
            
            int cleanedWorkers = cleanupStaleWorkerData();
            
            logger.info("System recovery completed successfully. Recovered {} tasks, cleaned {} stale workers", 
                       recoveredTasks, cleanedWorkers);
            
        } catch (Exception e) {
            logger.error("System recovery failed", e);
            throw new RuntimeException("System recovery failed during startup", e);
        }
    }
    
    /**
     * Recovers tasks that were in RUNNING status during system shutdown.
     * These tasks are reset to PENDING status for reassignment.
     * 
     * @return number of tasks recovered
     */
    @Transactional
    public int recoverRunningTasks() {
        logger.info("Recovering RUNNING tasks from previous session...");
        
        
        List<Task> runningTasks = taskRepository.findByStatus(TaskStatus.RUNNING);
        
        if (runningTasks.isEmpty()) {
            logger.info("No RUNNING tasks found to recover");
            return 0;
        }
        
        logger.info("Found {} RUNNING tasks to recover", runningTasks.size());
        
        int recoveredCount = 0;
        for (Task task : runningTasks) {
            try {
                // Reset task to PENDING status for reassignment
                task.setStatus(TaskStatus.PENDING);
                task.setWorkerId(null);
                task.setAssignedAt(null);
                task.setUpdatedAt(Instant.now());
                
                taskRepository.save(task);
                recoveredCount++;
                
                logger.debug("Recovered task {} (type: {}, originally assigned to worker: {})", 
                           task.getId(), task.getType(), task.getWorkerId());
                
            } catch (Exception e) {
                logger.error("Failed to recover task {}", task.getId(), e);
               
            }
        }
        
        logger.info("Successfully recovered {} out of {} RUNNING tasks", recoveredCount, runningTasks.size());
        return recoveredCount;
    }
    
    /**
     * Cleans up stale worker heartbeat data from previous sessions.
     * This ensures the worker registry starts clean after system restart.
     * 
     * @return number of stale worker records cleaned
     */
    @Transactional
    public int cleanupStaleWorkerData() {
        logger.info("Cleaning up stale worker heartbeat data...");
        
        try {
            
            long existingWorkers = workerHeartbeatRepository.count();
            
            if (existingWorkers == 0) {
                logger.info("No stale worker data found to clean up");
                return 0;
            }
            workerHeartbeatRepository.deleteAll();
            
            logger.info("Cleaned up {} stale worker heartbeat records", existingWorkers);
            return (int) existingWorkers;
            
        } catch (Exception e) {
            logger.error("Failed to cleanup stale worker data", e);
            throw new RuntimeException("Worker data cleanup failed", e);
        }
    }
    
  
    @Transactional
    public RecoveryResult performManualRecovery() {
        logger.info("Performing manual system recovery...");
        
        int recoveredTasks = recoverRunningTasks();
        int cleanedWorkers = cleanupStaleWorkerData();
        
        RecoveryResult result = new RecoveryResult(recoveredTasks, cleanedWorkers, true);
        logger.info("Manual recovery completed: {}", result);
        
        return result;
    }
    
 
    public boolean isSystemStateConsistent() {
        try {
           
            List<Task> runningTasks = taskRepository.findByStatus(TaskStatus.RUNNING);
            
            for (Task task : runningTasks) {
                if (task.getWorkerId() != null && 
                    !workerHeartbeatRepository.existsById(task.getWorkerId())) {
                    logger.warn("Found orphaned RUNNING task {} assigned to non-existent worker {}", 
                               task.getId(), task.getWorkerId());
                    return false;
                }
            }
            
            return true;
            
        } catch (Exception e) {
            logger.error("Failed to check system state consistency", e);
            return false;
        }
    }
    
   
    public static class RecoveryResult {
        private final int recoveredTasks;
        private final int cleanedWorkers;
        private final boolean successful;
        
        public RecoveryResult(int recoveredTasks, int cleanedWorkers, boolean successful) {
            this.recoveredTasks = recoveredTasks;
            this.cleanedWorkers = cleanedWorkers;
            this.successful = successful;
        }
        
        public int getRecoveredTasks() {
            return recoveredTasks;
        }
        
        public int getCleanedWorkers() {
            return cleanedWorkers;
        }
        
        public boolean isSuccessful() {
            return successful;
        }
        
        @Override
        public String toString() {
            return String.format("RecoveryResult{recoveredTasks=%d, cleanedWorkers=%d, successful=%s}", 
                               recoveredTasks, cleanedWorkers, successful);
        }
    }
}