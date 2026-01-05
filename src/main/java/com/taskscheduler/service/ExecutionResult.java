package com.taskscheduler.service;

import java.time.Instant;
import java.util.Map;

/**
 * Represents the result of a task execution.
 * Contains success status, output, and metadata from the execution.
 */
public class ExecutionResult {
    
    private final boolean success;
    private final String output;
    private final String errorMessage;
    private final Map<String, Object> metadata;
    private final Instant executedAt;
    
    public ExecutionResult(boolean success, String output, String errorMessage, Map<String, Object> metadata) {
        this.success = success;
        this.output = output;
        this.errorMessage = errorMessage;
        this.metadata = metadata;
        this.executedAt = Instant.now();
    }
    
    public ExecutionResult(boolean success, String output, Map<String, Object> metadata) {
        this(success, output, null, metadata);
    }
    
    public ExecutionResult(boolean success, String output) {
        this(success, output, null, null);
    }
    
    public static ExecutionResult success(String output, Map<String, Object> metadata) {
        return new ExecutionResult(true, output, metadata);
    }
    
    public static ExecutionResult success(String output) {
        return new ExecutionResult(true, output);
    }
    
    public static ExecutionResult failure(String errorMessage, Map<String, Object> metadata) {
        return new ExecutionResult(false, null, errorMessage, metadata);
    }
    
    public static ExecutionResult failure(String errorMessage) {
        return new ExecutionResult(false, null, errorMessage, null);
    }
    
    public boolean isSuccess() {
        return success;
    }
    
    public String getOutput() {
        return output;
    }
    
    public String getErrorMessage() {
        return errorMessage;
    }
    
    public Map<String, Object> getMetadata() {
        return metadata;
    }
    
    public Instant getExecutedAt() {
        return executedAt;
    }
    
    @Override
    public String toString() {
        return "ExecutionResult{" +
                "success=" + success +
                ", output='" + output + '\'' +
                ", errorMessage='" + errorMessage + '\'' +
                ", metadata=" + metadata +
                ", executedAt=" + executedAt +
                '}';
    }
}