package com.taskscheduler.dto;

import java.time.Instant;
import java.util.UUID;

import com.taskscheduler.entity.Task;
import com.taskscheduler.entity.TaskStatus;
import com.taskscheduler.entity.TaskType;

/**
 * DTO for task responses.
 * Contains all task information for API responses.
 */
public class TaskResponse {
    
    private UUID id;
    private TaskType type;
    private String payload;
    private TaskStatus status;
    private Instant scheduleAt;
    private Instant createdAt;
    private Instant updatedAt;
    private Integer retryCount;
    private Integer maxRetries;
    private String workerId;
    private Instant assignedAt;
    private Instant completedAt;
    private String executionOutput;
    private String executionMetadata;
    

    public TaskResponse() {}
    
   
    public TaskResponse(Task task) {
        this.id = task.getId();
        this.type = task.getType();
        this.payload = task.getPayload();
        this.status = task.getStatus();
        this.scheduleAt = task.getScheduleAt();
        this.createdAt = task.getCreatedAt();
        this.updatedAt = task.getUpdatedAt();
        this.retryCount = task.getRetryCount();
        this.maxRetries = task.getMaxRetries();
        this.workerId = task.getWorkerId();
        this.assignedAt = task.getAssignedAt();
        this.completedAt = task.getCompletedAt();
        this.executionOutput = task.getExecutionOutput();
        this.executionMetadata = task.getExecutionMetadata();
    }
    
  
    public static TaskResponse from(Task task) {
        return new TaskResponse(task);
    }
    
   
    public UUID getId() {
        return id;
    }
    
    public void setId(UUID id) {
        this.id = id;
    }
    
    public TaskType getType() {
        return type;
    }
    
    public void setType(TaskType type) {
        this.type = type;
    }
    
    public String getPayload() {
        return payload;
    }
    
    public void setPayload(String payload) {
        this.payload = payload;
    }
    
    public TaskStatus getStatus() {
        return status;
    }
    
    public void setStatus(TaskStatus status) {
        this.status = status;
    }
    
    public Instant getScheduleAt() {
        return scheduleAt;
    }
    
    public void setScheduleAt(Instant scheduleAt) {
        this.scheduleAt = scheduleAt;
    }
    
    public Instant getCreatedAt() {
        return createdAt;
    }
    
    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }
    
    public Instant getUpdatedAt() {
        return updatedAt;
    }
    
    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }
    
    public Integer getRetryCount() {
        return retryCount;
    }
    
    public void setRetryCount(Integer retryCount) {
        this.retryCount = retryCount;
    }
    
    public Integer getMaxRetries() {
        return maxRetries;
    }
    
    public void setMaxRetries(Integer maxRetries) {
        this.maxRetries = maxRetries;
    }
    
    public String getWorkerId() {
        return workerId;
    }
    
    public void setWorkerId(String workerId) {
        this.workerId = workerId;
    }
    
    public Instant getAssignedAt() {
        return assignedAt;
    }
    
    public void setAssignedAt(Instant assignedAt) {
        this.assignedAt = assignedAt;
    }
    
    public Instant getCompletedAt() {
        return completedAt;
    }
    
    public void setCompletedAt(Instant completedAt) {
        this.completedAt = completedAt;
    }
    
    public String getExecutionOutput() {
        return executionOutput;
    }
    
    public void setExecutionOutput(String executionOutput) {
        this.executionOutput = executionOutput;
    }
    
    public String getExecutionMetadata() {
        return executionMetadata;
    }
    
    public void setExecutionMetadata(String executionMetadata) {
        this.executionMetadata = executionMetadata;
    }
}