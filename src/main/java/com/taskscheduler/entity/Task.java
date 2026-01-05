package com.taskscheduler.entity;

import java.time.Instant;
import java.util.UUID;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.OptimisticLockType;
import org.hibernate.annotations.OptimisticLocking;
import org.hibernate.annotations.UpdateTimestamp;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

@Entity
@Table(name = "tasks", indexes = {
    @Index(name = "idx_tasks_due", columnList = "schedule_at, status"),
    @Index(name = "idx_tasks_worker", columnList = "worker_id, status")
})
@OptimisticLocking(type = OptimisticLockType.VERSION)
public class Task {
    
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id")
    private UUID id;
    
    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false, length = 50)
    private TaskType type;
    
    @NotNull
    @Column(name = "payload", nullable = false, columnDefinition = "TEXT")
    private String payload;
    
    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private TaskStatus status = TaskStatus.PENDING;
    
    @NotNull
    @Column(name = "schedule_at", nullable = false)
    private Instant scheduleAt;
    
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;
    
    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
    
    @Min(0)
    @Column(name = "retry_count", nullable = false)
    private Integer retryCount = 0;
    
    @Min(0)
    @Column(name = "max_retries", nullable = false)
    private Integer maxRetries = 3;
    
    @Column(name = "worker_id", length = 100)
    private String workerId;
    
    @Column(name = "assigned_at")
    private Instant assignedAt;
    
    @Column(name = "completed_at")
    private Instant completedAt;
    
    @Column(name = "execution_output", columnDefinition = "TEXT")
    private String executionOutput;
    
    @Column(name = "execution_metadata", columnDefinition = "TEXT")
    private String executionMetadata;
    
    @Version
    @Column(name = "version", nullable = false)
    private Long version = 0L;
    
 
    public Task() {}
    
    // Constructor for creating new tasks
    public Task(TaskType type, String payload, Instant scheduleAt) {
        this.type = type;
        this.payload = payload;
        this.scheduleAt = scheduleAt;
    }
    
    // Constructor with max retries
    public Task(TaskType type, String payload, Instant scheduleAt, Integer maxRetries) {
        this.type = type;
        this.payload = payload;
        this.scheduleAt = scheduleAt;
        this.maxRetries = maxRetries;
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
    
    public Long getVersion() {
        return version;
    }
    
    public void setVersion(Long version) {
        this.version = version;
    }
    
    @Override
    public String toString() {
        return "Task{" +
                "id=" + id +
                ", type=" + type +
                ", status=" + status +
                ", scheduleAt=" + scheduleAt +
                ", retryCount=" + retryCount +
                ", maxRetries=" + maxRetries +
                ", workerId='" + workerId + '\'' +
                '}';
    }
}