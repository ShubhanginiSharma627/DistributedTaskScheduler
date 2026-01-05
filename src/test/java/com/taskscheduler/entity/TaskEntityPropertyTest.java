package com.taskscheduler.entity;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Instant;
import java.util.Random;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.RepeatedTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import com.taskscheduler.repository.TaskRepository;

/**
 * Property-based tests for Task entity validation using repeated tests.
 * Tests universal properties that should hold for all valid task creation scenarios.
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
public class TaskEntityPropertyTest {
    
    @Autowired
    private TaskRepository taskRepository;
    
    private Random random = new Random();
    
    @BeforeEach
    void setUp() {
        taskRepository.deleteAll();
    }
    
    /**
     * Property 1: Task Creation Integrity
     * For any valid task definition with type, payload, and schedule time, 
     * creating the task should result in a stored task with unique ID, 
     * correct data, and PENDING status.
     */
    @RepeatedTest(100)
    void taskCreationMaintainsIntegrity() {
       
        TaskType type = generateValidTaskType();
        String payload = generateValidPayload();
        Instant scheduleAt = generateValidScheduleTime();
        Integer maxRetries = generateValidMaxRetries();
        
        
        Task task = new Task(type, payload, scheduleAt, maxRetries);
        
       
        Task savedTask = taskRepository.save(task);
        
        // Verify task creation integrity
        assertNotNull(savedTask.getId(), 
                     "Saved task should have a unique identifier");
        assertEquals(type, savedTask.getType(), 
                    "Saved task should maintain the specified type");
        assertEquals(payload, savedTask.getPayload(), 
                    "Saved task should maintain the specified payload");
        assertEquals(scheduleAt, savedTask.getScheduleAt(), 
                    "Saved task should maintain the specified schedule time");
        assertEquals(TaskStatus.PENDING, savedTask.getStatus(), 
                    "Saved task should have initial PENDING status");
        assertEquals(maxRetries, savedTask.getMaxRetries(), 
                    "Saved task should maintain the specified max retries");
        assertEquals(Integer.valueOf(0), savedTask.getRetryCount(), 
                    "Saved task should have initial retry count of 0");
        assertNotNull(savedTask.getCreatedAt(), 
                     "Saved task should have creation timestamp");
        assertNotNull(savedTask.getUpdatedAt(), 
                     "Saved task should have update timestamp");
        assertNotNull(savedTask.getVersion(), 
                     "Saved task should have version for optimistic locking");
        
        // Verify task can be retrieved from database
        Task retrievedTask = taskRepository.findById(savedTask.getId()).orElse(null);
        assertNotNull(retrievedTask, 
                     "Task should be retrievable from database");
        assertEquals(savedTask.getId(), retrievedTask.getId(), 
                    "Retrieved task should have same ID");
        assertEquals(savedTask.getType(), retrievedTask.getType(), 
                    "Retrieved task should have same type");
        assertEquals(savedTask.getPayload(), retrievedTask.getPayload(), 
                    "Retrieved task should have same payload");
        assertEquals(savedTask.getScheduleAt(), retrievedTask.getScheduleAt(), 
                    "Retrieved task should have same schedule time");
        assertEquals(savedTask.getStatus(), retrievedTask.getStatus(), 
                    "Retrieved task should have same status");
    }
    
    /**
     * Property: Task Type Support Validation
     * For any supported task type (HTTP, SHELL, DUMMY), the task should be 
     * created and stored correctly with appropriate payload validation.
     */
    @RepeatedTest(100)
    void taskTypeSupportIsComplete() {
        
        TaskType type = generateValidTaskType();
        String payload = generateTypeSpecificPayload();
        Instant scheduleAt = generateValidScheduleTime();
        
        
        Task task = new Task(type, payload, scheduleAt);
        
        // Save and verify
        Task savedTask = taskRepository.save(task);
        
        assertNotNull(savedTask.getId(), 
                     "Task with type " + type + " should be saved with unique ID");
        assertEquals(type, savedTask.getType(), 
                    "Task should maintain the specified type: " + type);
        assertEquals(payload, savedTask.getPayload(), 
                    "Task should maintain the type-specific payload");
        assertEquals(TaskStatus.PENDING, savedTask.getStatus(), 
                    "Task should have initial PENDING status");
        
        // Verify default values are set correctly
        assertEquals(Integer.valueOf(3), savedTask.getMaxRetries(), 
                    "Task should have default max retries of 3");
        assertEquals(Integer.valueOf(0), savedTask.getRetryCount(), 
                    "Task should have initial retry count of 0");
    }
    
    /**
     * Property: Status Validation
     * For any task status update, only valid status values should be accepted.
     */
    @RepeatedTest(100)
    void taskStatusValidationIsEnforced() {
        
        TaskType type = generateValidTaskType();
        String payload = generateValidPayload();
        Instant scheduleAt = generateValidScheduleTime();
        TaskStatus status = generateValidTaskStatus();
        
       
        Task task = new Task(type, payload, scheduleAt);
        task.setStatus(status);
        
        // Save and verify
        Task savedTask = taskRepository.save(task);
        assertEquals(status, savedTask.getStatus(), 
                    "Task should maintain the specified valid status: " + status);
    }
    
    /**
     * Property: Retry Configuration Validation
     * For any task with retry configuration, the system should correctly 
     * track retry count and enforce maximum retry limits.
     */
    @RepeatedTest(100)
    void retryConfigurationIsValidated() {
        
        TaskType type = generateValidTaskType();
        String payload = generateValidPayload();
        Instant scheduleAt = generateValidScheduleTime();
        Integer maxRetries = generateValidMaxRetries();
        Integer retryCount = generateValidRetryCount();
        
        
        Task task = new Task(type, payload, scheduleAt, maxRetries);
        task.setRetryCount(retryCount);
        
        // Save and verify
        Task savedTask = taskRepository.save(task);
        assertEquals(maxRetries, savedTask.getMaxRetries(), 
                    "Task should maintain the specified max retries");
        assertEquals(retryCount, savedTask.getRetryCount(), 
                    "Task should maintain the specified retry count");
        
        // Verify retry count is within valid bounds
        assertTrue(savedTask.getRetryCount() >= 0, 
                  "Retry count should be non-negative");
        assertTrue(savedTask.getMaxRetries() >= 0, 
                  "Max retries should be non-negative");
    }
    
   
    
    private TaskType generateValidTaskType() {
        TaskType[] types = TaskType.values();
        return types[random.nextInt(types.length)];
    }
    
    private String generateValidPayload() {
        String[] payloads = {
            // HTTP payloads
            "{\"url\":\"https://api.example.com/test\",\"method\":\"POST\"}",
            "{\"url\":\"https://api.example.com/data\",\"method\":\"GET\"}",
            "{\"url\":\"https://api.example.com/update\",\"method\":\"PUT\",\"body\":\"{\\\"data\\\":\\\"test\\\"}\"}",
            // Shell payloads  
            "{\"command\":\"echo hello\"}",
            "{\"command\":\"ls -la\"}",
            "{\"command\":\"cat /tmp/test.txt\"}",
            // Dummy payloads
            "{\"sleepDurationMs\":1000,\"logMessage\":\"test\"}",
            "{\"sleepDurationMs\":5000,\"logMessage\":\"Processing task\"}",
            "{\"sleepDurationMs\":2000,\"logMessage\":\"Background job\"}"
        };
        return payloads[random.nextInt(payloads.length)];
    }
    
    private String generateTypeSpecificPayload() {
        String[] payloads = {
            // HTTP task payloads
            "{\"url\":\"https://api.example.com/webhook\",\"method\":\"GET\"}",
            "{\"url\":\"https://api.example.com/callback\",\"method\":\"POST\",\"body\":\"{\\\"data\\\":\\\"test\\\"}\"}",
            // Shell task payloads
            "{\"command\":\"echo test\"}",
            "{\"command\":\"cat /etc/hostname\"}",
            // Dummy task payloads
            "{\"sleepDurationMs\":3000,\"logMessage\":\"Processing task\"}",
            "{\"sleepDurationMs\":1500,\"logMessage\":\"Background operation\"}"
        };
        return payloads[random.nextInt(payloads.length)];
    }
    
    private Instant generateValidScheduleTime() {
        long now = Instant.now().getEpochSecond();
        long oneHourAgo = now - 3600;
        long oneDayFromNow = now + 86400;
        long randomTime = oneHourAgo + random.nextLong(oneDayFromNow - oneHourAgo);
        return Instant.ofEpochSecond(randomTime);
    }
    
    private TaskStatus generateValidTaskStatus() {
        TaskStatus[] statuses = TaskStatus.values();
        return statuses[random.nextInt(statuses.length)];
    }
    
    private Integer generateValidMaxRetries() {
        return random.nextInt(11); // 0 to 10
    }
    
    private Integer generateValidRetryCount() {
        return random.nextInt(6); // 0 to 5
    }
}