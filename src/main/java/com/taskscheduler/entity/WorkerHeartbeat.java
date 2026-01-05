package com.taskscheduler.entity;

import java.time.Instant;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.OptimisticLockType;
import org.hibernate.annotations.OptimisticLocking;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import jakarta.validation.constraints.NotNull;

/**
 * WorkerHeartbeat entity for tracking worker liveness in the distributed task scheduler.
 * Workers send periodic heartbeats to indicate they are alive and processing tasks.
 * Uses optimistic locking to handle concurrent heartbeat updates safely.
 */
@Entity
@Table(name = "worker_heartbeats", indexes = {
    @Index(name = "idx_heartbeat_timeout", columnList = "last_heartbeat")
})
@OptimisticLocking(type = OptimisticLockType.VERSION)
public class WorkerHeartbeat {
    
    @Id
    @Column(name = "worker_id", length = 100)
    private String workerId;
    
    @NotNull
    @Column(name = "last_heartbeat", nullable = false)
    private Instant lastHeartbeat = Instant.now();
    
    @Column(name = "worker_metadata", columnDefinition = "TEXT")
    private String workerMetadata;
    
    @CreationTimestamp
    @Column(name = "registered_at", nullable = false, updatable = false)
    private Instant registeredAt;
    
    @Version
    @Column(name = "version", nullable = false)
    private Long version = 0L;
    
  
    public WorkerHeartbeat() {}
    
    // Constructor for creating new worker heartbeat
    public WorkerHeartbeat(String workerId) {
        this.workerId = workerId;
    }
    
    // Constructor with metadata
    public WorkerHeartbeat(String workerId, String workerMetadata) {
        this.workerId = workerId;
        this.workerMetadata = workerMetadata;
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
    
    public String getWorkerMetadata() {
        return workerMetadata;
    }
    
    public void setWorkerMetadata(String workerMetadata) {
        this.workerMetadata = workerMetadata;
    }
    
    public Instant getRegisteredAt() {
        return registeredAt;
    }
    
    public void setRegisteredAt(Instant registeredAt) {
        this.registeredAt = registeredAt;
    }
    
    public Long getVersion() {
        return version;
    }
    
    public void setVersion(Long version) {
        this.version = version;
    }
    
    /**
     * Updates the last heartbeat timestamp to the current time
     */
    public void updateHeartbeat() {
        this.lastHeartbeat = Instant.now();
    }
    
    @Override
    public String toString() {
        return "WorkerHeartbeat{" +
                "workerId='" + workerId + '\'' +
                ", lastHeartbeat=" + lastHeartbeat +
                ", registeredAt=" + registeredAt +
                '}';
    }
}