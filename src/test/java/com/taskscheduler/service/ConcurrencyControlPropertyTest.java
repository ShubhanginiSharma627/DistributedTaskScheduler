package com.taskscheduler.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.RepeatedTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.support.TransactionTemplate;

import com.taskscheduler.entity.Task;
import com.taskscheduler.entity.TaskStatus;
import com.taskscheduler.entity.TaskType;
import com.taskscheduler.repository.TaskRepository;

@SpringBootTest
@ActiveProfiles("test")
public class ConcurrencyControlPropertyTest {
    
    @Autowired
    private TaskRepository taskRepository;
    
    @Autowired
    private TaskService taskService;
    
    @Autowired
    private SchedulerService schedulerService;
    
    @Autowired
    private TransactionTemplate transactionTemplate;
    
    private Random random = new Random();
    private ExecutorService executorService;
    
    @BeforeEach
    void setUp() {
        taskRepository.deleteAll();
        executorService = Executors.newFixedThreadPool(5);
    }
    
    @AfterEach
    void tearDown() {
        if (executorService != null && !executorService.isShutdown()) {
            executorService.shutdown();
            try {
                if (!executorService.awaitTermination(5, TimeUnit.SECONDS)) {
                    executorService.shutdownNow();
                }
            } catch (InterruptedException e) {
                executorService.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }
    }
    
    @RepeatedTest(100)
    void concurrentTaskModificationsPreventsRaceConditions() throws Exception {
        Task initialTask = createRandomTask();
        final Task savedTask = transactionTemplate.execute(status -> taskRepository.save(initialTask));
        final UUID taskId = savedTask.getId();
        
        int concurrentOperations = random.nextInt(4) + 2;
        
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch completionLatch = new CountDownLatch(concurrentOperations);
        AtomicInteger successfulUpdates = new AtomicInteger(0);
        List<CompletableFuture<Boolean>> futures = new ArrayList<>();
        
        for (int i = 0; i < concurrentOperations; i++) {
            CompletableFuture<Boolean> future = CompletableFuture.supplyAsync(() -> {
                try {
                    startLatch.await(5, TimeUnit.SECONDS);
                    
                    Boolean success = transactionTemplate.execute(status -> {
                        try {
                            return taskService.updateTaskStatus(
                                taskId, 
                                TaskStatus.PENDING, 
                                TaskStatus.RUNNING
                            );
                        } catch (Exception e) {
                            return false;
                        }
                    });
                    
                    if (Boolean.TRUE.equals(success)) {
                        successfulUpdates.incrementAndGet();
                    }
                    
                    return Boolean.TRUE.equals(success);
                    
                } catch (Exception e) {
                    return false;
                } finally {
                    completionLatch.countDown();
                }
            }, executorService);
            
            futures.add(future);
        }
        
        startLatch.countDown();
        
        assertTrue(completionLatch.await(10, TimeUnit.SECONDS), 
                  "Not all concurrent operations completed within timeout");
        
        assertEquals(1, successfulUpdates.get(), 
                    "Exactly one concurrent operation should succeed due to concurrency control");
        
        Task finalTask = transactionTemplate.execute(status -> 
            taskRepository.findById(taskId).orElseThrow()
        );
        assertEquals(TaskStatus.RUNNING, finalTask.getStatus(), 
                    "Final task status should be RUNNING after successful update");
        
        assertTrue(finalTask.getVersion() > savedTask.getVersion(), 
                  "Task version should be incremented after successful update");
    }
    
    @RepeatedTest(100)
    void concurrentTaskAssignmentIsAtomic() throws Exception {
        Task task = createRandomTask();
        task.setScheduleAt(Instant.now().minusSeconds(1));
        task = taskRepository.save(task);
        final Task finalTask = task;
        
        int schedulerCount = random.nextInt(3) + 2;
        
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch completionLatch = new CountDownLatch(schedulerCount);
        AtomicInteger successfulAssignments = new AtomicInteger(0);
        List<String> assignedWorkerIds = new ArrayList<>();
        
        for (int i = 0; i < schedulerCount; i++) {
            CompletableFuture.runAsync(() -> {
                try {
                    startLatch.await(5, TimeUnit.SECONDS);
                    
                    boolean success = schedulerService.assignTaskToWorker(finalTask);
                    
                    if (success) {
                        successfulAssignments.incrementAndGet();
                        synchronized (assignedWorkerIds) {
                            Task updatedTask = taskRepository.findById(finalTask.getId()).orElse(null);
                            if (updatedTask != null && updatedTask.getWorkerId() != null) {
                                assignedWorkerIds.add(updatedTask.getWorkerId());
                            }
                        }
                    }
                    
                } catch (Exception e) {
                } finally {
                    completionLatch.countDown();
                }
            }, executorService);
        }
        
        startLatch.countDown();
        
        assertTrue(completionLatch.await(10, TimeUnit.SECONDS), 
                  "Not all concurrent assignment operations completed within timeout");
        
        assertEquals(1, successfulAssignments.get(), 
                    "Exactly one scheduler should successfully assign the task");
        
        Task assignedTask = taskRepository.findById(finalTask.getId()).orElseThrow();
        assertEquals(TaskStatus.RUNNING, assignedTask.getStatus(), 
                    "Task should be in RUNNING status after assignment");
        assertTrue(assignedTask.getWorkerId() != null && !assignedTask.getWorkerId().isEmpty(), 
                  "Task should have a worker ID assigned");
        assertTrue(assignedTask.getAssignedAt() != null, 
                  "Task should have assignment timestamp");
        
        synchronized (assignedWorkerIds) {
            assertTrue(assignedWorkerIds.size() <= 1, 
                      "At most one worker should be assigned to the task");
        }
    }
    
    @RepeatedTest(100)
    void concurrentTaskCreationMaintainsConsistency() throws Exception {
        int taskCount = random.nextInt(4) + 3;
        
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch completionLatch = new CountDownLatch(taskCount);
        List<UUID> createdTaskIds = new ArrayList<>();
        AtomicInteger successfulCreations = new AtomicInteger(0);
        
        for (int i = 0; i < taskCount; i++) {
            final int taskIndex = i;
            
            CompletableFuture.runAsync(() -> {
                try {
                    startLatch.await(5, TimeUnit.SECONDS);
                    
                    String payload = "{\"taskIndex\": " + taskIndex + ", \"timestamp\": \"" + 
                                   Instant.now() + "\"}";
                    
                    Task createdTask = taskService.createTask(
                        TaskType.DUMMY, 
                        payload, 
                        Instant.now().plusSeconds(random.nextInt(3600)), 
                        3
                    );
                    
                    synchronized (createdTaskIds) {
                        createdTaskIds.add(createdTask.getId());
                    }
                    successfulCreations.incrementAndGet();
                    
                } catch (Exception e) {
                    System.err.println("Task creation failed: " + e.getMessage());
                } finally {
                    completionLatch.countDown();
                }
            }, executorService);
        }
        
        startLatch.countDown();
        
        assertTrue(completionLatch.await(15, TimeUnit.SECONDS), 
                  "Not all concurrent creation operations completed within timeout");
        
        synchronized (createdTaskIds) {
            assertEquals(successfulCreations.get(), createdTaskIds.size(), 
                        "Number of created task IDs should match successful creations");
            
            long uniqueIdCount = createdTaskIds.stream().distinct().count();
            assertEquals(createdTaskIds.size(), uniqueIdCount, 
                        "All created task IDs should be unique");
        }
        
        for (UUID taskId : createdTaskIds) {
            Task task = taskRepository.findById(taskId).orElse(null);
            assertTrue(task != null, "Created task should exist in database");
            assertEquals(TaskStatus.PENDING, task.getStatus(), 
                        "Created task should have PENDING status");
            assertEquals(TaskType.DUMMY, task.getType(), 
                        "Created task should have correct type");
            assertTrue(task.getVersion() != null && task.getVersion() >= 0, 
                      "Created task should have valid version for optimistic locking");
        }
    }
    
    @RepeatedTest(100)
    void concurrentStatusUpdatesOnDifferentTasksAreIndependent() throws Exception {
        int taskCount = random.nextInt(3) + 3;
        List<Task> tasks = new ArrayList<>();
        
        for (int i = 0; i < taskCount; i++) {
            Task task = createRandomTask();
            task = taskRepository.save(task);
            tasks.add(task);
        }
        
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch completionLatch = new CountDownLatch(taskCount);
        AtomicInteger successfulUpdates = new AtomicInteger(0);
        
        for (int i = 0; i < taskCount; i++) {
            final Task task = tasks.get(i);
            
            CompletableFuture.runAsync(() -> {
                try {
                    startLatch.await(5, TimeUnit.SECONDS);
                    
                    boolean success = taskService.updateTaskStatus(
                        task.getId(), 
                        TaskStatus.PENDING, 
                        TaskStatus.RUNNING
                    );
                    
                    if (success) {
                        successfulUpdates.incrementAndGet();
                    }
                    
                } catch (Exception e) {
                    System.err.println("Status update failed: " + e.getMessage());
                } finally {
                    completionLatch.countDown();
                }
            }, executorService);
        }
        
        startLatch.countDown();
        
        assertTrue(completionLatch.await(10, TimeUnit.SECONDS), 
                  "Not all concurrent update operations completed within timeout");
        
        assertEquals(taskCount, successfulUpdates.get(), 
                    "All concurrent updates on different tasks should succeed");
        
        for (Task originalTask : tasks) {
            Task updatedTask = taskRepository.findById(originalTask.getId()).orElseThrow();
            assertEquals(TaskStatus.RUNNING, updatedTask.getStatus(), 
                        "Task status should be updated to RUNNING");
            assertTrue(updatedTask.getVersion() > originalTask.getVersion(), 
                      "Task version should be incremented after update");
        }
    }
    
    private Task createRandomTask() {
        Task task = new Task();
        task.setType(TaskType.values()[random.nextInt(TaskType.values().length)]);
        task.setPayload("{\"test\": \"payload-" + random.nextInt(1000) + "\"}");
        task.setStatus(TaskStatus.PENDING);
        task.setScheduleAt(Instant.now().plusSeconds(random.nextInt(3600)));
        task.setRetryCount(0);
        task.setMaxRetries(3);
        return task;
    }
}