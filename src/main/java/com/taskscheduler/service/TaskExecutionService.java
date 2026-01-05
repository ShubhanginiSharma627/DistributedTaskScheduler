package com.taskscheduler.service;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.taskscheduler.entity.Task;
import com.taskscheduler.entity.TaskExecution;
import com.taskscheduler.entity.TaskStatus;
import com.taskscheduler.repository.TaskExecutionRepository;
import com.taskscheduler.repository.TaskRepository;

/**
 * Service for orchestrating task execution.
 * Handles task execution coordination, result processing, and status updates.
 */
@Service
public class TaskExecutionService {
    
    private static final Logger logger = LoggerFactory.getLogger(TaskExecutionService.class);
    
    private final List<TaskExecutor> taskExecutors;
    private final TaskRepository taskRepository;
    private final TaskExecutionRepository taskExecutionRepository;
    private final ObjectMapper objectMapper;
    
    public TaskExecutionService(List<TaskExecutor> taskExecutors,
                               TaskRepository taskRepository,
                               TaskExecutionRepository taskExecutionRepository,
                               ObjectMapper objectMapper) {
        this.taskExecutors = taskExecutors;
        this.taskRepository = taskRepository;
        this.taskExecutionRepository = taskExecutionRepository;
        this.objectMapper = objectMapper;
    }
    
    /**
     * Executes a task using the appropriate executor and handles the result.
     * Creates execution history record and updates task status based on result.
     * Uses READ_COMMITTED isolation for consistent task execution state.
     * 
     * @param task The task to execute
     * @param workerId The ID of the worker executing the task
     * @return ExecutionResult containing the execution outcome
     */
    @Transactional(isolation = Isolation.READ_COMMITTED, propagation = Propagation.REQUIRED)
    public ExecutionResult executeTask(Task task, String workerId) {
        logger.info("Starting execution of task {} by worker {}", task.getId(), workerId);
        
      
        TaskExecution execution = new TaskExecution(task.getId(), workerId);
        execution = taskExecutionRepository.save(execution);
        
        try {
        
            TaskExecutor executor = findExecutorForTask(task);
            if (executor == null) {
                String errorMessage = "No executor found for task type: " + task.getType();
                logger.error(errorMessage);
                
               
                execution.markFailed(errorMessage);
                taskExecutionRepository.save(execution);
                
              
                updateTaskStatus(task, TaskStatus.FAILED, null, errorMessage, null);
                
                return ExecutionResult.failure(errorMessage);
            }
            

            ExecutionResult result = executor.execute(task);
            
          
            processExecutionResult(task, execution, result);
            
            logger.info("Task {} execution completed with success: {}", task.getId(), result.isSuccess());
            return result;
            
        } catch (TaskExecutionException e) {
          
            logger.error("Task {} execution failed with unrecoverable error", task.getId(), e);
            
            execution.markFailed(e.getMessage());
            taskExecutionRepository.save(execution);
            
            updateTaskStatus(task, TaskStatus.FAILED, null, e.getMessage(), null);
            
            return ExecutionResult.failure(e.getMessage());
            
        } catch (Exception e) {
            
            logger.error("Task {} execution failed with unexpected error", task.getId(), e);
            
            execution.markFailed("Unexpected error: " + e.getMessage());
            taskExecutionRepository.save(execution);
            

            return ExecutionResult.failure("Unexpected error: " + e.getMessage());
        }
    }
    
    
    private void processExecutionResult(Task task, TaskExecution execution, ExecutionResult result) {
        Instant completedAt = Instant.now();
        
       
        execution.setCompletedAt(completedAt);
        execution.setSuccess(result.isSuccess());
        
        if (result.isSuccess()) {
            execution.setOutput(result.getOutput());
            
          
            String metadataJson = serializeMetadata(result.getMetadata());
            updateTaskStatus(task, TaskStatus.SUCCESS, completedAt, result.getOutput(), metadataJson);
            
            logger.info("Task {} completed successfully", task.getId());
            
        } else {
            execution.setErrorMessage(result.getErrorMessage());
            
           
            logger.warn("Task {} execution failed: {}", task.getId(), result.getErrorMessage());
        }
        

        if (result.getMetadata() != null) {
            String executionMetadataJson = serializeMetadata(result.getMetadata());
            execution.setExecutionMetadata(executionMetadataJson);
        }
        
        taskExecutionRepository.save(execution);
    }
    
    /**
     * Updates task status and completion details.
     * Uses separate transaction with READ_COMMITTED isolation for status updates.
     */
    @Transactional(isolation = Isolation.READ_COMMITTED, propagation = Propagation.REQUIRES_NEW)
    private void updateTaskStatus(Task task, TaskStatus newStatus, Instant completedAt, 
                                 String output, String metadata) {
        try {
            int updated = taskRepository.updateTaskCompletion(
                task.getId(), 
                newStatus, 
                completedAt, 
                output, 
                metadata, 
                Instant.now()
            );
            
            if (updated == 0) {
                logger.warn("Failed to update task {} status to {}", task.getId(), newStatus);
            }
            
        } catch (Exception e) {
            logger.error("Error updating task {} status", task.getId(), e);
        }
    }
    
   
    private TaskExecutor findExecutorForTask(Task task) {
        return taskExecutors.stream()
                .filter(executor -> executor.canExecute(task))
                .findFirst()
                .orElse(null);
    }
    
  
    private String serializeMetadata(Map<String, Object> metadata) {
        if (metadata == null || metadata.isEmpty()) {
            return null;
        }
        
        try {
            return objectMapper.writeValueAsString(metadata);
        } catch (JsonProcessingException e) {
            logger.warn("Failed to serialize metadata to JSON", e);
            return metadata.toString();
        }
    }
    
    
    public List<TaskExecution> getTaskExecutionHistory(Task task) {
        return taskExecutionRepository.findByTaskIdOrderByStartedAtDesc(task.getId());
    }
    
    
    public boolean isTaskCurrentlyExecuting(Task task) {
        List<TaskExecution> runningExecutions = taskExecutionRepository.findRunningExecutions();
        return runningExecutions.stream()
                .anyMatch(execution -> execution.getTaskId().equals(task.getId()));
    }
    
    
    public TaskExecution getMostRecentExecution(Task task) {
        return taskExecutionRepository.findMostRecentExecutionForTask(task.getId());
    }
}