package com.taskscheduler.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.Arrays;
import java.util.Collections;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.taskscheduler.entity.Task;
import com.taskscheduler.entity.TaskStatus;
import com.taskscheduler.entity.TaskType;
import com.taskscheduler.repository.TaskRepository;
import com.taskscheduler.repository.WorkerHeartbeatRepository;

/**
 * Unit tests for SystemRecoveryService.
 * Tests system recovery functionality including RUNNING task recovery and worker cleanup.
 */
@ExtendWith(MockitoExtension.class)
class SystemRecoveryServiceTest {

    @Mock
    private TaskRepository taskRepository;

    @Mock
    private WorkerHeartbeatRepository workerHeartbeatRepository;

    @InjectMocks
    private SystemRecoveryService systemRecoveryService;

    private Task runningTask1;
    private Task runningTask2;

    @BeforeEach
    void setUp() {
        runningTask1 = new Task();
        runningTask1.setId(UUID.randomUUID());
        runningTask1.setType(TaskType.HTTP);
        runningTask1.setStatus(TaskStatus.RUNNING);
        runningTask1.setWorkerId("worker-1");
        runningTask1.setAssignedAt(Instant.now().minusSeconds(300));
        runningTask1.setPayload("{\"url\":\"http://example.com\"}");
        runningTask1.setScheduleAt(Instant.now().minusSeconds(600));

        runningTask2 = new Task();
        runningTask2.setId(UUID.randomUUID());
        runningTask2.setType(TaskType.SHELL);
        runningTask2.setStatus(TaskStatus.RUNNING);
        runningTask2.setWorkerId("worker-2");
        runningTask2.setAssignedAt(Instant.now().minusSeconds(200));
        runningTask2.setPayload("{\"command\":\"echo test\"}");
        runningTask2.setScheduleAt(Instant.now().minusSeconds(400));
    }

    @Test
    void testRecoverRunningTasks_WithRunningTasks_ShouldResetToPending() {
        
        when(taskRepository.findByStatus(TaskStatus.RUNNING))
                .thenReturn(Arrays.asList(runningTask1, runningTask2));
        when(taskRepository.save(any(Task.class))).thenReturn(runningTask1);

        
        int recoveredCount = systemRecoveryService.recoverRunningTasks();

       
        assertEquals(2, recoveredCount);
        
        // Verify task 1 was reset
        assertEquals(TaskStatus.PENDING, runningTask1.getStatus());
        assertNull(runningTask1.getWorkerId());
        assertNull(runningTask1.getAssignedAt());
        assertNotNull(runningTask1.getUpdatedAt());
        
        // Verify task 2 was reset
        assertEquals(TaskStatus.PENDING, runningTask2.getStatus());
        assertNull(runningTask2.getWorkerId());
        assertNull(runningTask2.getAssignedAt());
        assertNotNull(runningTask2.getUpdatedAt());
        
        // Verify repository interactions
        verify(taskRepository).findByStatus(TaskStatus.RUNNING);
        verify(taskRepository, times(2)).save(any(Task.class));
    }

    @Test
    void testRecoverRunningTasks_WithNoRunningTasks_ShouldReturnZero() {
        
        when(taskRepository.findByStatus(TaskStatus.RUNNING))
                .thenReturn(Collections.emptyList());

       
        int recoveredCount = systemRecoveryService.recoverRunningTasks();

      
        assertEquals(0, recoveredCount);
        verify(taskRepository).findByStatus(TaskStatus.RUNNING);
        verify(taskRepository, never()).save(any(Task.class));
    }

    @Test
    void testCleanupStaleWorkerData_WithExistingWorkers_ShouldDeleteAll() {
        
        when(workerHeartbeatRepository.count()).thenReturn(3L);

   
        int cleanedCount = systemRecoveryService.cleanupStaleWorkerData();

 
        assertEquals(3, cleanedCount);
        verify(workerHeartbeatRepository).count();
        verify(workerHeartbeatRepository).deleteAll();
    }

    @Test
    void testCleanupStaleWorkerData_WithNoWorkers_ShouldReturnZero() {
       
        when(workerHeartbeatRepository.count()).thenReturn(0L);

   
        int cleanedCount = systemRecoveryService.cleanupStaleWorkerData();

 
        assertEquals(0, cleanedCount);
        verify(workerHeartbeatRepository).count();
        verify(workerHeartbeatRepository, never()).deleteAll();
    }

    @Test
    void testPerformManualRecovery_ShouldReturnRecoveryResult() {
     
        when(taskRepository.findByStatus(TaskStatus.RUNNING))
                .thenReturn(Arrays.asList(runningTask1));
        when(taskRepository.save(any(Task.class))).thenReturn(runningTask1);
        when(workerHeartbeatRepository.count()).thenReturn(2L);

   
        SystemRecoveryService.RecoveryResult result = systemRecoveryService.performManualRecovery();

 
        assertNotNull(result);
        assertTrue(result.isSuccessful());
        assertEquals(1, result.getRecoveredTasks());
        assertEquals(2, result.getCleanedWorkers());
    }

    @Test
    void testIsSystemStateConsistent_WithConsistentState_ShouldReturnTrue() {

        when(taskRepository.findByStatus(TaskStatus.RUNNING))
                .thenReturn(Collections.emptyList());

 
        boolean isConsistent = systemRecoveryService.isSystemStateConsistent();

       
        assertTrue(isConsistent);
        verify(taskRepository).findByStatus(TaskStatus.RUNNING);
    }

    @Test
    void testIsSystemStateConsistent_WithOrphanedTasks_ShouldReturnFalse() {
     
        Task orphanedTask = new Task();
        orphanedTask.setId(UUID.randomUUID());
        orphanedTask.setStatus(TaskStatus.RUNNING);
        orphanedTask.setWorkerId("non-existent-worker");
        
        when(taskRepository.findByStatus(TaskStatus.RUNNING))
                .thenReturn(Arrays.asList(orphanedTask));
        when(workerHeartbeatRepository.existsById("non-existent-worker"))
                .thenReturn(false);


        boolean isConsistent = systemRecoveryService.isSystemStateConsistent();


        assertFalse(isConsistent);
        verify(taskRepository).findByStatus(TaskStatus.RUNNING);
        verify(workerHeartbeatRepository).existsById("non-existent-worker");
    }

    @Test
    void testRecoveryResult_ToString() {
    
        SystemRecoveryService.RecoveryResult result = 
                new SystemRecoveryService.RecoveryResult(5, 3, true);

        
        String resultString = result.toString();

   
        assertTrue(resultString.contains("recoveredTasks=5"));
        assertTrue(resultString.contains("cleanedWorkers=3"));
        assertTrue(resultString.contains("successful=true"));
    }
}