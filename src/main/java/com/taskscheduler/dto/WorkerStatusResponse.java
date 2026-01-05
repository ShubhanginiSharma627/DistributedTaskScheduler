package com.taskscheduler.dto;

import java.time.Instant;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Response DTO for worker status information.
 * Contains worker details and activity status.
 */
public class WorkerStatusResponse {
    
    @JsonProperty("worker_id")
    private String workerId;
    
    @JsonProperty("last_heartbeat")
    private Instant lastHeartbeat;
    
    @JsonProperty("registered_at")
    private Instant registeredAt;
    
    @JsonProperty("is_active")
    private boolean isActive;
    
    @JsonProperty("seconds_since_heartbeat")
    private long secondsSinceHeartbeat;
    
    @JsonProperty("metadata")
    private String metadata;
    

    public WorkerStatusResponse() {}
    

    public WorkerStatusResponse(String workerId, Instant lastHeartbeat, Instant registeredAt, 
                               boolean isActive, long secondsSinceHeartbeat, String metadata) {
        this.workerId = workerId;
        this.lastHeartbeat = lastHeartbeat;
        this.registeredAt = registeredAt;
        this.isActive = isActive;
        this.secondsSinceHeartbeat = secondsSinceHeartbeat;
        this.metadata = metadata;
    }
    

    public String getWorkerId() {
        return workerId;
    }
    
    public void setWorkerId(String workerId) {
        this.workerId = workerId;
    }
    
    public Instant getLastHeartbeat() {
        return lastHeartbeat;
    }
    
    public void setLastHeartbeat(Instant lastHeartbeat) {
        this.lastHeartbeat = lastHeartbeat;
    }
    
    public Instant getRegisteredAt() {
        return registeredAt;
    }
    
    public void setRegisteredAt(Instant registeredAt) {
        this.registeredAt = registeredAt;
    }
    
    public boolean isActive() {
        return isActive;
    }
    
    public void setActive(boolean active) {
        isActive = active;
    }
    
    public long getSecondsSinceHeartbeat() {
        return secondsSinceHeartbeat;
    }
    
    public void setSecondsSinceHeartbeat(long secondsSinceHeartbeat) {
        this.secondsSinceHeartbeat = secondsSinceHeartbeat;
    }
    
    public String getMetadata() {
        return metadata;
    }
    
    public void setMetadata(String metadata) {
        this.metadata = metadata;
    }
}