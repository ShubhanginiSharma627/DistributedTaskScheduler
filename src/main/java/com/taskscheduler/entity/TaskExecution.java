package com.taskscheduler.entity;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;

/**
 * TaskExecution entity for tracking execution history of tasks in the distributed task scheduler.
 * Records detailed information about each task execution attempt.
 */
@Entity
@Table(name = "task_executions", indexes = {
    @Index(name = "idx_executions_task", columnList = "task_id"),
    @Index(name = "idx_executions_worker", columnList = "worker_id")
})
public class TaskExecution {
    
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id")
    private UUID id;
    
    @NotNull
    @Column(name = "task_id", nullable = false)
    private UUID taskId;
    
    @NotNull
    @Column(name = "worker_id", nullable = false, length = 100)
    private String workerId;
    
    @Column(name = "started_at", nullable = false, updatable = false)
    private Instant startedAt;
    
    @Column(name = "completed_at")
    private Instant completedAt;
    
    @Column(name = "success")
    private Boolean success;
    
    @Column(name = "output", columnDefinition = "TEXT")
    private String output;
    
    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;
    
    @Column(name = "execution_metadata", columnDefinition = "TEXT")
    private String executionMetadata;
    

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "task_id", insertable = false, updatable = false)
    private Task task;
    

    public TaskExecution() {}
    
    // Constructor for creating new task execution
    public TaskExecution(UUID taskId, String workerId) {
        this.taskId = taskId;
        this.workerId = workerId;
        this.startedAt = Instant.now();
    }
    

    public UUID getId() {
        return id;
    }
    
    public void setId(UUID id) {
        this.id = id;
    }
    
    public UUID getTaskId() {
        return taskId;
    }
    
    public void setTaskId(UUID taskId) {
        this.taskId = taskId;
    }
    
    public String getWorkerId() {
        return workerId;
    }
    
    public void setWorkerId(String workerId) {
        this.workerId = workerId;
    }
    
    public Instant getStartedAt() {
        return startedAt;
    }
    
    public void setStartedAt(Instant startedAt) {
        this.startedAt = startedAt;
    }
    
    public Instant getCompletedAt() {
        return completedAt;
    }
    
    public void setCompletedAt(Instant completedAt) {
        this.completedAt = completedAt;
    }
    
    public Boolean getSuccess() {
        return success;
    }
    
    public void setSuccess(Boolean success) {
        this.success = success;
    }
    
    public String getOutput() {
        return output;
    }
    
    public void setOutput(String output) {
        this.output = output;
    }
    
    public String getErrorMessage() {
        return errorMessage;
    }
    
    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }
    
    public String getExecutionMetadata() {
        return executionMetadata;
    }
    
    public void setExecutionMetadata(String executionMetadata) {
        this.executionMetadata = executionMetadata;
    }
    
    public Task getTask() {
        return task;
    }
    
    public void setTask(Task task) {
        this.task = task;
    }
    
    /**
     * Marks the execution as completed with success status
     */
    public void markCompleted(boolean success, String output) {
        this.completedAt = Instant.now();
        this.success = success;
        this.output = output;
    }
    
    /**
     * Marks the execution as failed with error message
     */
    public void markFailed(String errorMessage) {
        this.completedAt = Instant.now();
        this.success = false;
        this.errorMessage = errorMessage;
    }
    
    @Override
    public String toString() {
        return "TaskExecution{" +
                "id=" + id +
                ", taskId=" + taskId +
                ", workerId='" + workerId + '\'' +
                ", startedAt=" + startedAt +
                ", completedAt=" + completedAt +
                ", success=" + success +
                '}';
    }
}