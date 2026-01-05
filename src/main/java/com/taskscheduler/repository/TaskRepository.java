package com.taskscheduler.repository;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.taskscheduler.entity.Task;
import com.taskscheduler.entity.TaskStatus;
import com.taskscheduler.entity.TaskType;

@Repository
public interface TaskRepository extends JpaRepository<Task, UUID> {
    
    @Query("SELECT t FROM Task t WHERE t.status = :status AND t.scheduleAt <= :currentTime ORDER BY t.scheduleAt ASC")
    List<Task> findDueTasks(@Param("status") TaskStatus status, @Param("currentTime") Instant currentTime);
    
    @Modifying
    @Query("UPDATE Task t SET t.status = :newStatus, t.workerId = :workerId, t.assignedAt = :assignedAt " +
           "WHERE t.id = :taskId AND t.status = :currentStatus")
    int atomicAssignTask(@Param("taskId") UUID taskId, 
                        @Param("currentStatus") TaskStatus currentStatus,
                        @Param("newStatus") TaskStatus newStatus,
                        @Param("workerId") String workerId, 
                        @Param("assignedAt") Instant assignedAt);
    
    /**
     * Updates task status atomically with optimistic locking.
     * Used for status transitions during task lifecycle.
     * Includes version increment for proper optimistic locking.
     */
    @Modifying
    @Query("UPDATE Task t SET t.status = :newStatus, t.updatedAt = :updatedAt, t.version = t.version + 1 " +
           "WHERE t.id = :taskId AND t.status = :currentStatus")
    int updateTaskStatus(@Param("taskId") UUID taskId,
                         @Param("currentStatus") TaskStatus currentStatus,
                         @Param("newStatus") TaskStatus newStatus,
                         @Param("updatedAt") Instant updatedAt);
    
    /**
     * Updates task status with completion details.
     * Used when task execution completes successfully or fails.
     */
    @Modifying
    @Query("UPDATE Task t SET t.status = :newStatus, t.completedAt = :completedAt, " +
           "t.executionOutput = :output, t.executionMetadata = :metadata, t.updatedAt = :updatedAt " +
           "WHERE t.id = :taskId")
    int updateTaskCompletion(@Param("taskId") UUID taskId,
                            @Param("newStatus") TaskStatus newStatus,
                            @Param("completedAt") Instant completedAt,
                            @Param("output") String output,
                            @Param("metadata") String metadata,
                            @Param("updatedAt") Instant updatedAt);
    
    /**
     * Increments retry count and reschedules task.
     * Used by retry manager for failed task rescheduling.
     */
    @Modifying
    @Query("UPDATE Task t SET t.retryCount = t.retryCount + 1, t.status = :newStatus, " +
           "t.scheduleAt = :newScheduleAt, t.workerId = NULL, t.assignedAt = NULL, t.updatedAt = :updatedAt " +
           "WHERE t.id = :taskId")
    int incrementRetryAndReschedule(@Param("taskId") UUID taskId,
                                   @Param("newStatus") TaskStatus newStatus,
                                   @Param("newScheduleAt") Instant newScheduleAt,
                                   @Param("updatedAt") Instant updatedAt);
    
    /**
     * Resets abandoned tasks back to PENDING status.
     * Used when worker failures are detected to reassign tasks.
     */
    @Modifying
    @Query("UPDATE Task t SET t.status = :newStatus, t.workerId = NULL, t.assignedAt = NULL, t.updatedAt = :updatedAt " +
           "WHERE t.workerId = :workerId AND t.status = :currentStatus")
    int resetAbandonedTasks(@Param("workerId") String workerId,
                           @Param("currentStatus") TaskStatus currentStatus,
                           @Param("newStatus") TaskStatus newStatus,
                           @Param("updatedAt") Instant updatedAt);
   
    @Query("SELECT t FROM Task t WHERE t.workerId = :workerId AND t.status = :status ORDER BY t.assignedAt ASC")
    List<Task> findTasksByWorkerAndStatus(@Param("workerId") String workerId, @Param("status") TaskStatus status);
    
   
    Page<Task> findByStatus(TaskStatus status, Pageable pageable);
    
   
    List<Task> findByStatus(TaskStatus status);
  
    Page<Task> findByType(TaskType type, Pageable pageable);
    
  
    Page<Task> findByStatusAndType(TaskStatus status, TaskType type, Pageable pageable);
    
   
    @Query("SELECT t FROM Task t WHERE t.scheduleAt BETWEEN :startTime AND :endTime ORDER BY t.scheduleAt ASC")
    List<Task> findTasksInTimeRange(@Param("startTime") Instant startTime, @Param("endTime") Instant endTime);
    
   
    long countByStatus(TaskStatus status);
    
    @Query("SELECT t FROM Task t WHERE t.retryCount >= t.maxRetries AND t.status = :status")
    List<Task> findTasksExceedingRetryLimit(@Param("status") TaskStatus status);
}