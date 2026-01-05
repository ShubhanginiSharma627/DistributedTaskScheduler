package com.taskscheduler.service;

import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.taskscheduler.entity.Task;
import com.taskscheduler.entity.TaskType;

/**
 * TaskExecutor implementation for dummy tasks.
 * Executes dummy tasks that sleep for a specified duration and log a message.
 * Useful for testing and demonstration purposes.
 */
@Component
public class DummyTaskExecutor implements TaskExecutor {
    
    private static final Logger logger = LoggerFactory.getLogger(DummyTaskExecutor.class);
    
    private final ObjectMapper objectMapper;
    
    public DummyTaskExecutor(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }
    
    @Override
    public ExecutionResult execute(Task task) throws TaskExecutionException {
        if (!canExecute(task)) {
            throw new TaskExecutionException("DummyTaskExecutor cannot execute task of type: " + task.getType());
        }
        
        try {
           
            JsonNode payloadNode = objectMapper.readTree(task.getPayload());
            
            long sleepDurationMs = payloadNode.has("sleepDurationMs") ? 
                payloadNode.get("sleepDurationMs").asLong() : 1000L;
            String logMessage = payloadNode.has("logMessage") ? 
                payloadNode.get("logMessage").asText() : "Dummy task executed";
            
            logger.info("Starting dummy task execution: {}", logMessage);
            
          
            Thread.sleep(sleepDurationMs);
            
          
            Map<String, Object> metadata = new HashMap<>();
            metadata.put("sleepDurationMs", sleepDurationMs);
            metadata.put("logMessage", logMessage);
            
            String output = String.format("Dummy task completed successfully. Slept for %d ms. Message: %s", 
                sleepDurationMs, logMessage);
            
            logger.info("Dummy task completed: {}", logMessage);
            
            return ExecutionResult.success(output, metadata);
            
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            logger.warn("Dummy task was interrupted");
            return ExecutionResult.failure("Dummy task was interrupted");
            
        } catch (Exception e) {
            logger.error("Dummy task execution failed unexpectedly", e);
            throw new TaskExecutionException("Dummy task execution failed: " + e.getMessage(), e);
        }
    }
    
    @Override
    public boolean canExecute(Task task) {
        return task.getType() == TaskType.DUMMY;
    }
}