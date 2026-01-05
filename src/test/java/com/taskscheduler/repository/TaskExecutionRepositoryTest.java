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
import com.taskscheduler.entity.TaskExecution;
import com.taskscheduler.entity.TaskType;

/**
 * Test class for TaskExecutionRepository database operations.
 * Tests task execution history tracking and metrics functionality.
 */
@DataJpaTest
@ActiveProfiles("test")
class TaskExecutionRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private TaskExecutionRepository executionRepository;

    @Test
    void testSaveAndFindTaskExecution() {
       
        Task task = new Task(TaskType.DUMMY, "{\"message\": \"test\"}", Instant.now());
        Task savedTask = entityManager.persistAndFlush(task);
        
        TaskExecution execution = new TaskExecution(savedTask.getId(), "worker-1");
        execution.setStartedAt(Instant.now()); // Explicitly set started time
        execution.markCompleted(true, "Task completed successfully");
        
        
        TaskExecution savedExecution = executionRepository.save(execution);
        
      
        assertThat(savedExecution.getId()).isNotNull();
        assertThat(savedExecution.getTaskId()).isEqualTo(savedTask.getId());
        assertThat(savedExecution.getWorkerId()).isEqualTo("worker-1");
        assertThat(savedExecution.getSuccess()).isTrue();
        assertThat(savedExecution.getOutput()).isEqualTo("Task completed successfully");
        assertThat(savedExecution.getStartedAt()).isNotNull();
        assertThat(savedExecution.getCompletedAt()).isNotNull();
    }

    @Test
    void testFindByTaskIdOrderByStartedAtDesc() {
       
        Task task = new Task(TaskType.DUMMY, "{\"message\": \"test\"}", Instant.now());
        Task savedTask = entityManager.persistAndFlush(task);
        
        // Create multiple executions for the same task
        TaskExecution execution1 = new TaskExecution(savedTask.getId(), "worker-1");
        execution1.setStartedAt(Instant.now().minusSeconds(60));
        execution1.markCompleted(false, "First attempt failed");
        
        TaskExecution execution2 = new TaskExecution(savedTask.getId(), "worker-2");
        execution2.setStartedAt(Instant.now().minusSeconds(30));
        execution2.markCompleted(true, "Second attempt succeeded");
        
        entityManager.persistAndFlush(execution1);
        entityManager.persistAndFlush(execution2);
        
        
        List<TaskExecution> executions = executionRepository.findByTaskIdOrderByStartedAtDesc(savedTask.getId());
        
      
        assertThat(executions).hasSize(2);
        assertThat(executions.get(0).getWorkerId()).isEqualTo("worker-2"); // Most recent first
        assertThat(executions.get(1).getWorkerId()).isEqualTo("worker-1");
    }

    @Test
    void testFindByWorkerIdOrderByStartedAtDesc() {
       
        String workerId = "worker-1";
        
        Task task1 = new Task(TaskType.DUMMY, "{\"message\": \"task1\"}", Instant.now());
        Task task2 = new Task(TaskType.DUMMY, "{\"message\": \"task2\"}", Instant.now());
        Task savedTask1 = entityManager.persistAndFlush(task1);
        Task savedTask2 = entityManager.persistAndFlush(task2);
        
        TaskExecution execution1 = new TaskExecution(savedTask1.getId(), workerId);
        execution1.setStartedAt(Instant.now().minusSeconds(60));
        execution1.markCompleted(true, "Task 1 completed");
        
        TaskExecution execution2 = new TaskExecution(savedTask2.getId(), workerId);
        execution2.setStartedAt(Instant.now().minusSeconds(30));
        execution2.markCompleted(true, "Task 2 completed");
        
        // Different worker
        TaskExecution execution3 = new TaskExecution(savedTask1.getId(), "worker-2");
        execution3.setStartedAt(Instant.now().minusSeconds(45));
        execution3.markCompleted(true, "Task by different worker");
        
        entityManager.persistAndFlush(execution1);
        entityManager.persistAndFlush(execution2);
        entityManager.persistAndFlush(execution3);
        
        
        List<TaskExecution> workerExecutions = executionRepository.findByWorkerIdOrderByStartedAtDesc(workerId);
        
      
        assertThat(workerExecutions).hasSize(2);
        assertThat(workerExecutions).allMatch(exec -> exec.getWorkerId().equals(workerId));
    }

    @Test
    void testFindSuccessfulExecutionsInTimeRange() {
       
        Instant now = Instant.now();
        Instant startTime = now.minusSeconds(120);
        Instant endTime = now;
        
        Task task = new Task(TaskType.DUMMY, "{\"message\": \"test\"}", Instant.now());
        Task savedTask = entityManager.persistAndFlush(task);
        
        // Successful execution within range
        TaskExecution successExecution = new TaskExecution(savedTask.getId(), "worker-1");
        successExecution.setStartedAt(now.minusSeconds(90));
        successExecution.setCompletedAt(now.minusSeconds(60));
        successExecution.setSuccess(true);
        
        // Failed execution within range
        TaskExecution failedExecution = new TaskExecution(savedTask.getId(), "worker-2");
        failedExecution.setStartedAt(now.minusSeconds(80));
        failedExecution.setCompletedAt(now.minusSeconds(50));
        failedExecution.setSuccess(false);
        
        // Successful execution outside range
        TaskExecution outsideExecution = new TaskExecution(savedTask.getId(), "worker-3");
        outsideExecution.setStartedAt(now.minusSeconds(200));
        outsideExecution.setCompletedAt(now.minusSeconds(180));
        outsideExecution.setSuccess(true);
        
        entityManager.persistAndFlush(successExecution);
        entityManager.persistAndFlush(failedExecution);
        entityManager.persistAndFlush(outsideExecution);
        
        
        List<TaskExecution> successfulExecutions = executionRepository.findSuccessfulExecutionsInTimeRange(startTime, endTime);
        
      
        assertThat(successfulExecutions).hasSize(1);
        assertThat(successfulExecutions.get(0).getWorkerId()).isEqualTo("worker-1");
        assertThat(successfulExecutions.get(0).getSuccess()).isTrue();
    }

    @Test
    void testCountSuccessfulAndFailedExecutions() {
       
        Instant now = Instant.now();
        Instant startTime = now.minusSeconds(120);
        Instant endTime = now;
        
        Task task = new Task(TaskType.DUMMY, "{\"message\": \"test\"}", Instant.now());
        Task savedTask = entityManager.persistAndFlush(task);
        
        // Create successful executions
        for (int i = 0; i < 3; i++) {
            TaskExecution execution = new TaskExecution(savedTask.getId(), "worker-" + i);
            execution.setStartedAt(now.minusSeconds(80 + i * 10));
            execution.setCompletedAt(now.minusSeconds(60 + i * 10));
            execution.setSuccess(true);
            entityManager.persistAndFlush(execution);
        }
        
        // Create failed executions
        for (int i = 0; i < 2; i++) {
            TaskExecution execution = new TaskExecution(savedTask.getId(), "worker-fail-" + i);
            execution.setStartedAt(now.minusSeconds(60 + i * 10));
            execution.setCompletedAt(now.minusSeconds(40 + i * 10));
            execution.setSuccess(false);
            entityManager.persistAndFlush(execution);
        }
        
        
        long successfulCount = executionRepository.countSuccessfulExecutions(startTime, endTime);
        long failedCount = executionRepository.countFailedExecutions(startTime, endTime);
        long totalCount = executionRepository.countTotalExecutions(startTime, endTime);
        
      
        assertThat(successfulCount).isEqualTo(3);
        assertThat(failedCount).isEqualTo(2);
        assertThat(totalCount).isEqualTo(5);
    }

    @Test
    void testFindRunningExecutions() {
       
        Task task = new Task(TaskType.DUMMY, "{\"message\": \"test\"}", Instant.now());
        Task savedTask = entityManager.persistAndFlush(task);
        
        // Running execution (no completion time)
        TaskExecution runningExecution = new TaskExecution(savedTask.getId(), "worker-1");
        runningExecution.setStartedAt(Instant.now().minusSeconds(30));
     
        TaskExecution completedExecution = new TaskExecution(savedTask.getId(), "worker-2");
        completedExecution.setStartedAt(Instant.now().minusSeconds(60));
        completedExecution.markCompleted(true, "Completed");
        
        entityManager.persistAndFlush(runningExecution);
        entityManager.persistAndFlush(completedExecution);
        
        
        List<TaskExecution> runningExecutions = executionRepository.findRunningExecutions();
        
      
        assertThat(runningExecutions).hasSize(1);
        assertThat(runningExecutions.get(0).getWorkerId()).isEqualTo("worker-1");
        assertThat(runningExecutions.get(0).getCompletedAt()).isNull();
    }

    @Test
    void testFindStuckExecutions() {
       
        Instant baseTime = Instant.parse("2026-01-05T12:00:00Z");
        Instant cutoffTime = baseTime.minusSeconds(300); // 5 minutes before base time
        Instant stuckTime = baseTime.minusSeconds(600);  // 10 minutes before base time (should be found)
        Instant recentTime = baseTime.minusSeconds(60);  // 1 minute before base time (should NOT be found)
        
        Task task = new Task(TaskType.DUMMY, "{\"message\": \"test\"}", baseTime);
        Task savedTask = entityManager.persistAndFlush(task);
        
        // Stuck execution (started long ago, no completion) - should be found
        TaskExecution stuckExecution = new TaskExecution(savedTask.getId(), "worker-1");
        stuckExecution.setStartedAt(stuckTime);
        stuckExecution.setCompletedAt(null);
        stuckExecution.setSuccess(null);
        entityManager.persistAndFlush(stuckExecution);
        
        // Recent running execution - should NOT be found
        TaskExecution recentExecution = new TaskExecution(savedTask.getId(), "worker-2");
        recentExecution.setStartedAt(recentTime);
        recentExecution.setCompletedAt(null);
        recentExecution.setSuccess(null);
        entityManager.persistAndFlush(recentExecution);
        
        // Clear the persistence context to ensure we're querying the database
        entityManager.clear();
        
        
        List<TaskExecution> stuckExecutions = executionRepository.findStuckExecutions(cutoffTime);
        
      
        assertThat(stuckExecutions).hasSize(1);
        assertThat(stuckExecutions.get(0).getWorkerId()).isEqualTo("worker-1");
    }

    @Test
    void testMarkExecutionCompleted() {
       
        Task task = new Task(TaskType.DUMMY, "{\"message\": \"test\"}", Instant.now());
        Task savedTask = entityManager.persistAndFlush(task);
        
        TaskExecution execution = new TaskExecution(savedTask.getId(), "worker-1");
        execution.setStartedAt(Instant.now().minusSeconds(30));
        TaskExecution savedExecution = entityManager.persistAndFlush(execution);
        
        
        savedExecution.markCompleted(true, "Task completed successfully");
        entityManager.persistAndFlush(savedExecution);
        
      
        entityManager.clear();
        TaskExecution updated = executionRepository.findById(savedExecution.getId()).orElse(null);
        assertThat(updated).isNotNull();
        assertThat(updated.getSuccess()).isTrue();
        assertThat(updated.getOutput()).isEqualTo("Task completed successfully");
        assertThat(updated.getCompletedAt()).isNotNull();
    }

    @Test
    void testMarkExecutionFailed() {
       
        Task task = new Task(TaskType.DUMMY, "{\"message\": \"test\"}", Instant.now());
        Task savedTask = entityManager.persistAndFlush(task);
        
        TaskExecution execution = new TaskExecution(savedTask.getId(), "worker-1");
        execution.setStartedAt(Instant.now().minusSeconds(30));
        TaskExecution savedExecution = entityManager.persistAndFlush(execution);
        
        
        savedExecution.markFailed("Connection timeout");
        entityManager.persistAndFlush(savedExecution);
        
      
        entityManager.clear();
        TaskExecution updated = executionRepository.findById(savedExecution.getId()).orElse(null);
        assertThat(updated).isNotNull();
        assertThat(updated.getSuccess()).isFalse();
        assertThat(updated.getErrorMessage()).isEqualTo("Connection timeout");
        assertThat(updated.getCompletedAt()).isNotNull();
    }
}