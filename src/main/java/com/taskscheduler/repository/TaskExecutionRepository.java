package com.taskscheduler.repository;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.taskscheduler.entity.TaskExecution;

/**
 * Repository interface for TaskExecution entity operations.
 * Provides methods for tracking task execution history and metrics.
 */
@Repository
public interface TaskExecutionRepository extends JpaRepository<TaskExecution, UUID> {
    
   
    @Query("SELECT te FROM TaskExecution te WHERE te.taskId = :taskId ORDER BY te.startedAt DESC")
    List<TaskExecution> findByTaskIdOrderByStartedAtDesc(@Param("taskId") UUID taskId);
   
    Page<TaskExecution> findByTaskIdOrderByStartedAtDesc(UUID taskId, Pageable pageable);
    
    @Query("SELECT te FROM TaskExecution te WHERE te.workerId = :workerId ORDER BY te.startedAt DESC")
    List<TaskExecution> findByWorkerIdOrderByStartedAtDesc(@Param("workerId") String workerId);
    
  
    Page<TaskExecution> findByWorkerIdOrderByStartedAtDesc(String workerId, Pageable pageable);
    
    /**
     * Used for success rate metrics and reporting.
     */
    @Query("SELECT te FROM TaskExecution te WHERE te.success = true AND te.completedAt BETWEEN :startTime AND :endTime " +
           "ORDER BY te.completedAt DESC")
    List<TaskExecution> findSuccessfulExecutionsInTimeRange(@Param("startTime") Instant startTime, 
                                                           @Param("endTime") Instant endTime);
    
    /**
     * Used for failure pattern analysis and monitoring.
     */
    @Query("SELECT te FROM TaskExecution te WHERE te.success = false AND te.completedAt BETWEEN :startTime AND :endTime " +
           "ORDER BY te.completedAt DESC")
    List<TaskExecution> findFailedExecutionsInTimeRange(@Param("startTime") Instant startTime, 
                                                       @Param("endTime") Instant endTime);
    
    /**
     * Used for performance metrics and monitoring.
     */
    @Query(value = "SELECT AVG(DATEDIFF('MILLISECOND', te.started_at, te.completed_at)) FROM task_executions te " +
           "WHERE te.success = true AND te.completed_at IS NOT NULL AND te.completed_at BETWEEN :startTime AND :endTime", 
           nativeQuery = true)
    Double getAverageExecutionTimeInMilliseconds(@Param("startTime") Instant startTime, @Param("endTime") Instant endTime);
    
    /**
     * Used for success rate calculations.
     */
    @Query("SELECT COUNT(te) FROM TaskExecution te WHERE te.success = true AND te.completedAt BETWEEN :startTime AND :endTime")
    long countSuccessfulExecutions(@Param("startTime") Instant startTime, @Param("endTime") Instant endTime);
    
    /**
     * Used for failure rate calculations.
     */
    @Query("SELECT COUNT(te) FROM TaskExecution te WHERE te.success = false AND te.completedAt BETWEEN :startTime AND :endTime")
    long countFailedExecutions(@Param("startTime") Instant startTime, @Param("endTime") Instant endTime);
    
    /**
     * Used for overall execution metrics.
     */
    @Query("SELECT COUNT(te) FROM TaskExecution te WHERE te.completedAt BETWEEN :startTime AND :endTime")
    long countTotalExecutions(@Param("startTime") Instant startTime, @Param("endTime") Instant endTime);
    
   
    @Query("SELECT te FROM TaskExecution te WHERE te.completedAt IS NULL ORDER BY te.startedAt ASC")
    List<TaskExecution> findRunningExecutions();
    
    
    @Query("SELECT te FROM TaskExecution te WHERE te.startedAt < :cutoffTime AND te.completedAt IS NULL " +
           "ORDER BY te.startedAt ASC")
    List<TaskExecution> findStuckExecutions(@Param("cutoffTime") Instant cutoffTime);
    
   
    @Query("SELECT te FROM TaskExecution te WHERE te.taskId = :taskId ORDER BY te.startedAt DESC LIMIT 1")
    TaskExecution findMostRecentExecutionForTask(@Param("taskId") UUID taskId);
    
    
    @Query("DELETE FROM TaskExecution te WHERE te.completedAt < :cutoffTime")
    int deleteOldExecutions(@Param("cutoffTime") Instant cutoffTime);
    
   
    Page<TaskExecution> findBySuccessOrderByStartedAtDesc(Boolean success, Pageable pageable);
}