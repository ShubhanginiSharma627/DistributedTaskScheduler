package com.taskscheduler.repository;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.test.context.ActiveProfiles;

import com.taskscheduler.entity.WorkerHeartbeat;

/**
 * Test class for WorkerHeartbeatRepository database operations.
 * Tests worker heartbeat tracking and failure detection functionality.
 */
@DataJpaTest
@ActiveProfiles("test")
class WorkerHeartbeatRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private WorkerHeartbeatRepository heartbeatRepository;

    @Test
    void testSaveAndFindWorkerHeartbeat() {

        String workerId = "worker-1";
        WorkerHeartbeat heartbeat = new WorkerHeartbeat(workerId);
        heartbeat.setRegisteredAt(Instant.now()); // Explicitly set registered time
        
        WorkerHeartbeat savedHeartbeat = heartbeatRepository.save(heartbeat);
        

        assertThat(savedHeartbeat.getWorkerId()).isEqualTo(workerId);
        assertThat(savedHeartbeat.getLastHeartbeat()).isNotNull();
        assertThat(savedHeartbeat.getRegisteredAt()).isNotNull();
    }

    @Test
    void testFindByWorkerId() {
        
        String workerId = "worker-1";
        WorkerHeartbeat heartbeat = new WorkerHeartbeat(workerId, "{\"version\": \"1.0\"}");
        entityManager.persistAndFlush(heartbeat);
        
     
        Optional<WorkerHeartbeat> found = heartbeatRepository.findByWorkerId(workerId);
        
  
        assertThat(found).isPresent();
        assertThat(found.get().getWorkerId()).isEqualTo(workerId);
        assertThat(found.get().getWorkerMetadata()).contains("version");
    }

    @Test
    void testUpdateHeartbeat() {
        
        String workerId = "worker-1";
        WorkerHeartbeat heartbeat = new WorkerHeartbeat(workerId);
        entityManager.persistAndFlush(heartbeat);
        
        Instant newHeartbeatTime = Instant.now().plusSeconds(30);
        
     
        int rowsAffected = heartbeatRepository.updateHeartbeat(workerId, newHeartbeatTime);
        
        
        assertThat(rowsAffected).isEqualTo(1);
        
       
        entityManager.clear();
        WorkerHeartbeat updated = heartbeatRepository.findByWorkerId(workerId).orElse(null);
        assertThat(updated).isNotNull();
        assertThat(updated.getLastHeartbeat()).isEqualTo(newHeartbeatTime);
    }

    @Test
    void testFindStaleWorkers() {
       
        Instant now = Instant.now();
        Instant staleTime = now.minusSeconds(120); // 2 minutes ago
        Instant recentTime = now.minusSeconds(30);  // 30 seconds ago
        
        WorkerHeartbeat staleWorker = new WorkerHeartbeat("stale-worker");
        staleWorker.setLastHeartbeat(staleTime);
        
        WorkerHeartbeat activeWorker = new WorkerHeartbeat("active-worker");
        activeWorker.setLastHeartbeat(recentTime);
        
        entityManager.persistAndFlush(staleWorker);
        entityManager.persistAndFlush(activeWorker);
        
        Instant cutoffTime = now.minusSeconds(60); // 1 minute ago
        
      
        List<WorkerHeartbeat> staleWorkers = heartbeatRepository.findStaleWorkers(cutoffTime);
        
       
        assertThat(staleWorkers).hasSize(1);
        assertThat(staleWorkers.get(0).getWorkerId()).isEqualTo("stale-worker");
    }

    @Test
    void testFindActiveWorkers() {
       
        Instant now = Instant.now();
        Instant staleTime = now.minusSeconds(120); // 2 minutes ago
        Instant recentTime = now.minusSeconds(30);  // 30 seconds ago
        
        WorkerHeartbeat staleWorker = new WorkerHeartbeat("stale-worker");
        staleWorker.setLastHeartbeat(staleTime);
        
        WorkerHeartbeat activeWorker = new WorkerHeartbeat("active-worker");
        activeWorker.setLastHeartbeat(recentTime);
        
        entityManager.persistAndFlush(staleWorker);
        entityManager.persistAndFlush(activeWorker);
        
        Instant cutoffTime = now.minusSeconds(60); // 1 minute ago
        
        
        List<WorkerHeartbeat> activeWorkers = heartbeatRepository.findActiveWorkers(cutoffTime);
        
     
        assertThat(activeWorkers).hasSize(1);
        assertThat(activeWorkers.get(0).getWorkerId()).isEqualTo("active-worker");
    }

    @Test
    void testCountActiveWorkers() {
  
        Instant now = Instant.now();
        Instant staleTime = now.minusSeconds(120);
        Instant recentTime1 = now.minusSeconds(30);
        Instant recentTime2 = now.minusSeconds(15);
        
        WorkerHeartbeat staleWorker = new WorkerHeartbeat("stale-worker");
        staleWorker.setLastHeartbeat(staleTime);
        
        WorkerHeartbeat activeWorker1 = new WorkerHeartbeat("active-worker-1");
        activeWorker1.setLastHeartbeat(recentTime1);
        
        WorkerHeartbeat activeWorker2 = new WorkerHeartbeat("active-worker-2");
        activeWorker2.setLastHeartbeat(recentTime2);
        
        entityManager.persistAndFlush(staleWorker);
        entityManager.persistAndFlush(activeWorker1);
        entityManager.persistAndFlush(activeWorker2);
        
        Instant cutoffTime = now.minusSeconds(60);
        
     
        long activeCount = heartbeatRepository.countActiveWorkers(cutoffTime);
        
    
        assertThat(activeCount).isEqualTo(2);
    }

    @Test
    void testUpdateHeartbeatWithMetadata() {
       
        String workerId = "worker-1";
        WorkerHeartbeat heartbeat = new WorkerHeartbeat(workerId);
        entityManager.persistAndFlush(heartbeat);
        
        Instant newHeartbeatTime = Instant.now().plusSeconds(30);
        String newMetadata = "{\"status\": \"processing\", \"tasks\": 5}";
        
     
        int rowsAffected = heartbeatRepository.updateHeartbeatWithMetadata(
            workerId, newHeartbeatTime, newMetadata
        );
        

        assertThat(rowsAffected).isEqualTo(1);
        
        
        entityManager.clear();
        WorkerHeartbeat updated = heartbeatRepository.findByWorkerId(workerId).orElse(null);
        assertThat(updated).isNotNull();
        assertThat(updated.getLastHeartbeat()).isEqualTo(newHeartbeatTime);
        assertThat(updated.getWorkerMetadata()).isEqualTo(newMetadata);
    }

    @Test
    void testCleanupStaleHeartbeats() {
       
        Instant now = Instant.now();
        Instant veryOldTime = now.minusSeconds(3600); // 1 hour ago
        Instant recentTime = now.minusSeconds(30);
        
        WorkerHeartbeat veryOldWorker = new WorkerHeartbeat("very-old-worker");
        veryOldWorker.setLastHeartbeat(veryOldTime);
        
        WorkerHeartbeat recentWorker = new WorkerHeartbeat("recent-worker");
        recentWorker.setLastHeartbeat(recentTime);
        
        entityManager.persistAndFlush(veryOldWorker);
        entityManager.persistAndFlush(recentWorker);
        
        Instant cleanupCutoff = now.minusSeconds(1800); // 30 minutes ago
        
      
        int deletedCount = heartbeatRepository.cleanupStaleHeartbeats(cleanupCutoff);
        
     
        assertThat(deletedCount).isEqualTo(1);
        
        // Verify only recent worker remains
        List<WorkerHeartbeat> remaining = heartbeatRepository.findAll();
        assertThat(remaining).hasSize(1);
        assertThat(remaining.get(0).getWorkerId()).isEqualTo("recent-worker");
    }
}