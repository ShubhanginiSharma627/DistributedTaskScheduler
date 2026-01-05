package com.taskscheduler.repository;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.test.context.ActiveProfiles;

import com.taskscheduler.entity.Task;
import com.taskscheduler.entity.TaskStatus;
import com.taskscheduler.entity.TaskType;

/**
 * Test class for TaskRepository database operations.
 * Tests core repository functionality including custom queries and atomic operations.
 */
@DataJpaTest
@ActiveProfiles("test")
class TaskRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private TaskRepository taskRepository;

    @Test
    void testSaveAndFindTask() {
        
        Task task = new Task(TaskType.DUMMY, "{\"message\": \"test\"}", Instant.now());
        
    
        Task savedTask = taskRepository.save(task);
        
        
        assertThat(savedTask.getId()).isNotNull();
        assertThat(savedTask.getType()).isEqualTo(TaskType.DUMMY);
        assertThat(savedTask.getStatus()).isEqualTo(TaskStatus.PENDING);
        assertThat(savedTask.getRetryCount()).isEqualTo(0);
        assertThat(savedTask.getMaxRetries()).isEqualTo(3);
    }

    @Test
    void testFindDueTasks() {
        
        Instant now = Instant.now();
        Instant past = now.minusSeconds(60);
        Instant future = now.plusSeconds(60);
        
        Task dueTask = new Task(TaskType.DUMMY, "{\"message\": \"due\"}", past);
        Task futureTask = new Task(TaskType.DUMMY, "{\"message\": \"future\"}", future);
        
        entityManager.persistAndFlush(dueTask);
        entityManager.persistAndFlush(futureTask);
        
    
        List<Task> dueTasks = taskRepository.findDueTasks(TaskStatus.PENDING, now);
        
        
        assertThat(dueTasks).hasSize(1);
        assertThat(dueTasks.get(0).getPayload()).contains("due");
    }

    @Test
    void testAtomicAssignTask() {
        
        Task task = new Task(TaskType.DUMMY, "{\"message\": \"test\"}", Instant.now());
        Task savedTask = entityManager.persistAndFlush(task);
        String workerId = "worker-1";
        Instant assignedAt = Instant.now();
        
    
        int rowsAffected = taskRepository.atomicAssignTask(
            savedTask.getId(), 
            TaskStatus.PENDING, 
            TaskStatus.RUNNING, 
            workerId, 
            assignedAt
        );
        
        
        assertThat(rowsAffected).isEqualTo(1);
        
        // Verify task was updated
        entityManager.clear();
        Task updatedTask = taskRepository.findById(savedTask.getId()).orElse(null);
        assertThat(updatedTask).isNotNull();
        assertThat(updatedTask.getStatus()).isEqualTo(TaskStatus.RUNNING);
        assertThat(updatedTask.getWorkerId()).isEqualTo(workerId);
        assertThat(updatedTask.getAssignedAt()).isNotNull();
    }

    @Test
    void testAtomicAssignTaskAlreadyAssigned() {
        
        Task task = new Task(TaskType.DUMMY, "{\"message\": \"test\"}", Instant.now());
        task.setStatus(TaskStatus.RUNNING);
        task.setWorkerId("existing-worker");
        Task savedTask = entityManager.persistAndFlush(task);
        
     
        int rowsAffected = taskRepository.atomicAssignTask(
            savedTask.getId(), 
            TaskStatus.PENDING, 
            TaskStatus.RUNNING, 
            "new-worker", 
            Instant.now()
        );
        
        
        assertThat(rowsAffected).isEqualTo(0);
    }

    @Test
    void testFindTasksByWorkerAndStatus() {
        
        String workerId = "worker-1";
        Task assignedTask = new Task(TaskType.DUMMY, "{\"message\": \"assigned\"}", Instant.now());
        assignedTask.setStatus(TaskStatus.RUNNING);
        assignedTask.setWorkerId(workerId);
        assignedTask.setAssignedAt(Instant.now());
        
        Task otherTask = new Task(TaskType.DUMMY, "{\"message\": \"other\"}", Instant.now());
        otherTask.setStatus(TaskStatus.RUNNING);
        otherTask.setWorkerId("other-worker");
        
        entityManager.persistAndFlush(assignedTask);
        entityManager.persistAndFlush(otherTask);
        
    
        List<Task> workerTasks = taskRepository.findTasksByWorkerAndStatus(workerId, TaskStatus.RUNNING);
        
        
        assertThat(workerTasks).hasSize(1);
        assertThat(workerTasks.get(0).getWorkerId()).isEqualTo(workerId);
    }

    @Test
    void testCountByStatus() {
        
        Task pendingTask1 = new Task(TaskType.DUMMY, "{\"message\": \"pending1\"}", Instant.now());
        Task pendingTask2 = new Task(TaskType.DUMMY, "{\"message\": \"pending2\"}", Instant.now());
        Task runningTask = new Task(TaskType.DUMMY, "{\"message\": \"running\"}", Instant.now());
        runningTask.setStatus(TaskStatus.RUNNING);
        
        entityManager.persistAndFlush(pendingTask1);
        entityManager.persistAndFlush(pendingTask2);
        entityManager.persistAndFlush(runningTask);
        
    
        long pendingCount = taskRepository.countByStatus(TaskStatus.PENDING);
        long runningCount = taskRepository.countByStatus(TaskStatus.RUNNING);
        
        
        assertThat(pendingCount).isEqualTo(2);
        assertThat(runningCount).isEqualTo(1);
    }

    @Test
    void testResetAbandonedTasks() {
        
        String workerId = "failed-worker";
        Task abandonedTask1 = new Task(TaskType.DUMMY, "{\"message\": \"abandoned1\"}", Instant.now());
        abandonedTask1.setStatus(TaskStatus.RUNNING);
        abandonedTask1.setWorkerId(workerId);
        abandonedTask1.setAssignedAt(Instant.now());
        
        Task abandonedTask2 = new Task(TaskType.DUMMY, "{\"message\": \"abandoned2\"}", Instant.now());
        abandonedTask2.setStatus(TaskStatus.RUNNING);
        abandonedTask2.setWorkerId(workerId);
        abandonedTask2.setAssignedAt(Instant.now());
        
        entityManager.persistAndFlush(abandonedTask1);
        entityManager.persistAndFlush(abandonedTask2);
        
    
        int rowsAffected = taskRepository.resetAbandonedTasks(
            workerId, 
            TaskStatus.RUNNING, 
            TaskStatus.PENDING, 
            Instant.now()
        );
        
        
        assertThat(rowsAffected).isEqualTo(2);
        
        // Verify tasks were reset
        entityManager.clear();
        List<Task> resetTasks = taskRepository.findAll();
        assertThat(resetTasks).allMatch(task -> 
            task.getStatus() == TaskStatus.PENDING && 
            task.getWorkerId() == null && 
            task.getAssignedAt() == null
        );
    }
}