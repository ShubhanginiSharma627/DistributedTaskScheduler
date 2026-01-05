package com.taskscheduler.dto;

import java.time.Instant;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Response DTO for system health endpoint.
 * Contains system status, worker information, and task metrics.
 */
public class HealthResponse {
    
    @JsonProperty("status")
    private String status;
    
    @JsonProperty("timestamp")
    private Instant timestamp;
    
    @JsonProperty("uptime_seconds")
    private long uptimeSeconds;
    
    @JsonProperty("active_workers")
    private int activeWorkers;
    
    @JsonProperty("pending_tasks")
    private long pendingTasks;
    
    @JsonProperty("running_tasks")
    private long runningTasks;
    
    @JsonProperty("total_tasks")
    private long totalTasks;
    
    @JsonProperty("metrics")
    private Map<String, Object> metrics;
    

    public HealthResponse() {}
    
    
    public HealthResponse(String status, Instant timestamp, long uptimeSeconds, 
                         int activeWorkers, long pendingTasks, long runningTasks, 
                         long totalTasks, Map<String, Object> metrics) {
        this.status = status;
        this.timestamp = timestamp;
        this.uptimeSeconds = uptimeSeconds;
        this.activeWorkers = activeWorkers;
        this.pendingTasks = pendingTasks;
        this.runningTasks = runningTasks;
        this.totalTasks = totalTasks;
        this.metrics = metrics;
    }
    
   
    public String getStatus() {
        return status;
    }
    
    public void setStatus(String status) {
        this.status = status;
    }
    
    public Instant getTimestamp() {
        return timestamp;
    }
    
    public void setTimestamp(Instant timestamp) {
        this.timestamp = timestamp;
    }
    
    public long getUptimeSeconds() {
        return uptimeSeconds;
    }
    
    public void setUptimeSeconds(long uptimeSeconds) {
        this.uptimeSeconds = uptimeSeconds;
    }
    
    public int getActiveWorkers() {
        return activeWorkers;
    }
    
    public void setActiveWorkers(int activeWorkers) {
        this.activeWorkers = activeWorkers;
    }
    
    public long getPendingTasks() {
        return pendingTasks;
    }
    
    public void setPendingTasks(long pendingTasks) {
        this.pendingTasks = pendingTasks;
    }
    
    public long getRunningTasks() {
        return runningTasks;
    }
    
    public void setRunningTasks(long runningTasks) {
        this.runningTasks = runningTasks;
    }
    
    public long getTotalTasks() {
        return totalTasks;
    }
    
    public void setTotalTasks(long totalTasks) {
        this.totalTasks = totalTasks;
    }
    
    public Map<String, Object> getMetrics() {
        return metrics;
    }
    
    public void setMetrics(Map<String, Object> metrics) {
        this.metrics = metrics;
    }
}