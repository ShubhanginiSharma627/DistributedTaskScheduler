package com.taskscheduler.service;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.taskscheduler.entity.Task;
import com.taskscheduler.entity.TaskType;

/**
 * TaskExecutor implementation for shell command tasks.
 * Executes shell commands based on task payload configuration.
 */
@Component
public class ShellTaskExecutor implements TaskExecutor {
    
    private static final Logger logger = LoggerFactory.getLogger(ShellTaskExecutor.class);
    private static final int DEFAULT_TIMEOUT_SECONDS = 300; // 5 minutes
    
    private final ObjectMapper objectMapper;
    
    public ShellTaskExecutor(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }
    
    @Override
    public ExecutionResult execute(Task task) throws TaskExecutionException {
        if (!canExecute(task)) {
            throw new TaskExecutionException("ShellTaskExecutor cannot execute task of type: " + task.getType());
        }
        
        try {
            
            JsonNode payloadNode = objectMapper.readTree(task.getPayload());
            
            String command = payloadNode.get("command").asText();
            String workingDirectory = payloadNode.has("workingDirectory") ? 
                payloadNode.get("workingDirectory").asText() : null;
            
         
            ProcessBuilder processBuilder = new ProcessBuilder();
            
            
            String[] commandParts = command.split("\\s+");
            processBuilder.command(commandParts);
            
   
            if (workingDirectory != null) {
                processBuilder.directory(new File(workingDirectory));
            }
            
           
            if (payloadNode.has("environment")) {
                JsonNode envNode = payloadNode.get("environment");
                Map<String, String> environment = processBuilder.environment();
                envNode.fields().forEachRemaining(entry -> {
                    environment.put(entry.getKey(), entry.getValue().asText());
                });
            }
            
          
            processBuilder.redirectErrorStream(true);
            
            logger.info("Executing shell command: {}", command);
            
          
            Process process = processBuilder.start();
            
            
            StringBuilder output = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    output.append(line).append("\n");
                }
            }
            
          
            boolean finished = process.waitFor(DEFAULT_TIMEOUT_SECONDS, TimeUnit.SECONDS);
            
            if (!finished) {
                
                process.destroyForcibly();
                Map<String, Object> metadata = new HashMap<>();
                metadata.put("timeout", true);
                metadata.put("timeoutSeconds", DEFAULT_TIMEOUT_SECONDS);
                
                logger.warn("Shell command timed out after {} seconds: {}", DEFAULT_TIMEOUT_SECONDS, command);
                return ExecutionResult.failure("Command timed out after " + DEFAULT_TIMEOUT_SECONDS + " seconds", metadata);
            }
            
            int exitCode = process.exitValue();
            
            
            Map<String, Object> metadata = new HashMap<>();
            metadata.put("exitCode", exitCode);
            metadata.put("command", command);
            if (workingDirectory != null) {
                metadata.put("workingDirectory", workingDirectory);
            }
            
       
            boolean success = exitCode == 0;
            String outputString = output.toString().trim();
            
            if (success) {
                logger.info("Shell command executed successfully: {} (exit code: {})", command, exitCode);
                return ExecutionResult.success(outputString, metadata);
            } else {
                logger.warn("Shell command failed: {} (exit code: {})", command, exitCode);
                return ExecutionResult.failure("Command failed with exit code: " + exitCode, metadata);
            }
            
        } catch (IOException e) {
            logger.error("Failed to start shell command process", e);
            return ExecutionResult.failure("Failed to start process: " + e.getMessage());
            
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            logger.error("Shell command execution was interrupted", e);
            return ExecutionResult.failure("Command execution was interrupted: " + e.getMessage());
            
        } catch (Exception e) {
            logger.error("Shell command execution failed unexpectedly", e);
            throw new TaskExecutionException("Shell command execution failed: " + e.getMessage(), e);
        }
    }
    
    @Override
    public boolean canExecute(Task task) {
        return task.getType() == TaskType.SHELL;
    }
}