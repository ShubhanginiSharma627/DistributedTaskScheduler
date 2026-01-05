package com.taskscheduler.repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.taskscheduler.entity.WorkerHeartbeat;

/**
 * Repository interface for WorkerHeartbeat entity operations.
 * Provides methods for tracking worker liveness and cleanup operations.
 */
@Repository
public interface WorkerHeartbeatRepository extends JpaRepository<WorkerHeartbeat, String> {
    
   
    @Modifying
    @Query("UPDATE WorkerHeartbeat w SET w.lastHeartbeat = :heartbeatTime WHERE w.workerId = :workerId")
    int updateHeartbeat(@Param("workerId") String workerId, @Param("heartbeatTime") Instant heartbeatTime);
    
   
    @Query("SELECT w FROM WorkerHeartbeat w WHERE w.lastHeartbeat < :cutoffTime")
    List<WorkerHeartbeat> findStaleWorkers(@Param("cutoffTime") Instant cutoffTime);
  
    @Query("SELECT w FROM WorkerHeartbeat w WHERE w.lastHeartbeat >= :cutoffTime ORDER BY w.lastHeartbeat DESC")
    List<WorkerHeartbeat> findActiveWorkers(@Param("cutoffTime") Instant cutoffTime);
    
 
    @Modifying
    @Query("DELETE FROM WorkerHeartbeat w WHERE w.lastHeartbeat < :cutoffTime")
    int cleanupStaleHeartbeats(@Param("cutoffTime") Instant cutoffTime);
    
    
    @Query("SELECT COUNT(w) FROM WorkerHeartbeat w WHERE w.lastHeartbeat >= :cutoffTime")
    long countActiveWorkers(@Param("cutoffTime") Instant cutoffTime);
    
    Optional<WorkerHeartbeat> findByWorkerId(String workerId);
    
   
    @Modifying
    @Query("UPDATE WorkerHeartbeat w SET w.lastHeartbeat = :heartbeatTime, w.workerMetadata = :metadata " +
           "WHERE w.workerId = :workerId")
    int updateHeartbeatWithMetadata(@Param("workerId") String workerId, 
                                   @Param("heartbeatTime") Instant heartbeatTime,
                                   @Param("metadata") String metadata);
  
    @Query("SELECT w FROM WorkerHeartbeat w ORDER BY w.lastHeartbeat DESC")
    List<WorkerHeartbeat> findAllOrderedByHeartbeat();
}