package com.taskscheduler.dto;

import java.time.Instant;

import com.taskscheduler.dto.validation.ValidTaskPayload;
import com.taskscheduler.entity.TaskType;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * DTO for task creation requests.
 * Contains validation annotations for input validation.
 */
@ValidTaskPayload
public class TaskCreateRequest {
    
    @NotNull(message = "Task type is required")
    private TaskType type;
    
    @NotBlank(message = "Payload cannot be empty")
    private String payload;
    
    @NotNull(message = "Schedule time is required")
    private Instant scheduleAt;
    
    @Min(value = 0, message = "Max retries must be non-negative")
    private Integer maxRetries = 3;
    

    public TaskCreateRequest() {}
    
 
    public TaskCreateRequest(TaskType type, String payload, Instant scheduleAt, Integer maxRetries) {
        this.type = type;
        this.payload = payload;
        this.scheduleAt = scheduleAt;
        this.maxRetries = maxRetries;
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
    
    public Instant getScheduleAt() {
        return scheduleAt;
    }
    
    public void setScheduleAt(Instant scheduleAt) {
        this.scheduleAt = scheduleAt;
    }
    
    public Integer getMaxRetries() {
        return maxRetries;
    }
    
    public void setMaxRetries(Integer maxRetries) {
        this.maxRetries = maxRetries;
    }
}