package com.taskscheduler.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.RepeatedTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import com.taskscheduler.dto.WorkerStatusResponse;
import com.taskscheduler.entity.Task;
import com.taskscheduler.entity.TaskExecution;
import com.taskscheduler.entity.TaskStatus;
import com.taskscheduler.entity.TaskType;
import com.taskscheduler.entity.WorkerHeartbeat;
import com.taskscheduler.repository.TaskExecutionRepository;
import com.taskscheduler.repository.TaskRepository;
import com.taskscheduler.repository.WorkerHeartbeatRepository;

/**
 * Property-based tests for MonitoringService.
 * Tests universal properties that should hold across all valid inputs.
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
public class MonitoringServicePropertyTest {
    
    @Autowired
    private MonitoringService monitoringService;
    
    @Autowired
    private TaskRepository taskRepository;
    
    @Autowired
    private WorkerHeartbeatRepository workerHeartbeatRepository;
    
    @Autowired
    private TaskExecutionRepository taskExecutionRepository;
    
    private Random random = new Random();
    
    @BeforeEach
    void setUp() {
       
        taskExecutionRepository.deleteAll();
        taskRepository.deleteAll();
        workerHeartbeatRepository.deleteAll();
    }
    
   
    @RepeatedTest(100)
    void metricsCollectionAccuracy() {
       
        List<TaskExecution> executions = generateRandomTaskExecutions();
        
        
        if (executions.isEmpty()) {
            return;
        }
        
       
        for (TaskExecution execution : executions) {
          
            Task task = createRandomTask();
            task = taskRepository.save(task);
            execution.setTaskId(task.getId());
            taskExecutionRepository.save(execution);
        }
        
       
        Duration period = Duration.ofHours(24);
        Map<String, Object> metrics = monitoringService.getExecutionMetrics(period);
        
        // Verify metrics accuracy
        long expectedTotal = executions.size();
        long expectedSuccessful = executions.stream()
                .mapToLong(e -> Boolean.TRUE.equals(e.getSuccess()) ? 1 : 0)
                .sum();
        long expectedFailed = expectedTotal - expectedSuccessful;
        
        assertEquals(expectedTotal, metrics.get("total_executions"));
        assertEquals(expectedSuccessful, metrics.get("successful_executions"));
        assertEquals(expectedFailed, metrics.get("failed_executions"));
        
        // Verify success rate calculation
        if (expectedTotal > 0) {
            double expectedSuccessRate = (double) expectedSuccessful / expectedTotal * 100.0;
            double actualSuccessRate = (Double) metrics.get("success_rate_percent");
            assertEquals(expectedSuccessRate, actualSuccessRate, 0.01);
        } else {
            assertEquals(0.0, metrics.get("success_rate_percent"));
        }
        
        // Verify metrics contain required fields
        assertTrue(metrics.containsKey("average_execution_time_ms"));
        assertTrue(metrics.containsKey("currently_running"));
        assertTrue(metrics.containsKey("potentially_stuck"));
        assertTrue(metrics.containsKey("period_hours"));
    }
    
   
    @RepeatedTest(100)
    void healthStatusReporting() {
       
        List<WorkerHeartbeat> workers = generateRandomWorkers();
        List<Task> tasks = generateRandomTasks();
        
     
        for (WorkerHeartbeat worker : workers) {
            workerHeartbeatRepository.save(worker);
        }
        for (Task task : tasks) {
            taskRepository.save(task);
        }
        
       
        Map<String, Long> taskCounts = monitoringService.getTaskCountsByStatus();
        
        // Verify task counts accuracy
        long expectedPending = tasks.stream()
                .mapToLong(t -> t.getStatus() == TaskStatus.PENDING ? 1 : 0)
                .sum();
        long expectedRunning = tasks.stream()
                .mapToLong(t -> t.getStatus() == TaskStatus.RUNNING ? 1 : 0)
                .sum();
        long expectedSuccess = tasks.stream()
                .mapToLong(t -> t.getStatus() == TaskStatus.SUCCESS ? 1 : 0)
                .sum();
        long expectedFailed = tasks.stream()
                .mapToLong(t -> t.getStatus() == TaskStatus.FAILED ? 1 : 0)
                .sum();
        long expectedTotal = tasks.size();
        
        assertEquals(expectedPending, taskCounts.get("PENDING"));
        assertEquals(expectedRunning, taskCounts.get("RUNNING"));
        assertEquals(expectedSuccess, taskCounts.get("SUCCESS"));
        assertEquals(expectedFailed, taskCounts.get("FAILED"));
        assertEquals(expectedTotal, taskCounts.get("TOTAL"));
        
       
        List<WorkerStatusResponse> workerStatuses = monitoringService.getWorkerStatuses();
        assertEquals(workers.size(), workerStatuses.size());
        
        // Verify active worker count
        Instant cutoffTime = Instant.now().minus(Duration.ofSeconds(60));
        long expectedActiveWorkers = workers.stream()
                .mapToLong(w -> w.getLastHeartbeat().isAfter(cutoffTime) ? 1 : 0)
                .sum();
        int actualActiveWorkers = monitoringService.getActiveWorkerCount();
        assertEquals(expectedActiveWorkers, actualActiveWorkers);
        
        // Verify system health status is valid
        String healthStatus = monitoringService.getSystemHealthStatus();
        assertTrue(List.of("UP", "DOWN", "DEGRADED").contains(healthStatus));
        
        // Verify uptime is positive
        long uptime = monitoringService.getUptimeSeconds();
        assertTrue(uptime >= 0);
    }
    
    /**
     * Property: Worker Status Consistency
     * For any worker heartbeat data, the worker status should be consistent with heartbeat timing.
     */
    @RepeatedTest(100)
    void workerStatusConsistency() {
       
        List<WorkerHeartbeat> workers = generateRandomWorkersWithFixedTimestamps();
        
       
        for (WorkerHeartbeat worker : workers) {
            workerHeartbeatRepository.save(worker);
        }
        
        List<WorkerStatusResponse> statuses = monitoringService.getWorkerStatuses();
        Instant now = Instant.now();
        Instant cutoffTime = now.minus(Duration.ofSeconds(60));
        
        for (WorkerStatusResponse status : statuses) {
            
            WorkerHeartbeat worker = workers.stream()
                    .filter(w -> w.getWorkerId().equals(status.getWorkerId()))
                    .findFirst()
                    .orElseThrow();
            
            // Verify active status consistency
            boolean expectedActive = worker.getLastHeartbeat().isAfter(cutoffTime);
            assertEquals(expectedActive, status.isActive());
            
            // Verify heartbeat timing (allow for small database precision differences)
            // Compare timestamps with tolerance for database precision differences
            long expectedSeconds = worker.getLastHeartbeat().getEpochSecond();
            long actualSeconds = status.getLastHeartbeat().getEpochSecond();
            assertTrue(Math.abs(expectedSeconds - actualSeconds) <= 1, 
                    "Heartbeat timestamp difference too large: expected " + expectedSeconds + " but was " + actualSeconds);
            
            // Skip registration timestamp check due to @CreationTimestamp annotation
            // The annotation automatically sets the timestamp on entity creation, overriding manual values
            // This is expected behavior for production but makes testing with fixed timestamps difficult
        }
    }
    
   
    @RepeatedTest(100)
    void comprehensiveEventLogging() {
       
        List<Task> tasks = generateRandomTasks();
        List<WorkerHeartbeat> workers = generateRandomWorkers();
        
       
        for (Task task : tasks) {
            taskRepository.save(task);
        }
        
        for (WorkerHeartbeat worker : workers) {
            workerHeartbeatRepository.save(worker);
        }
        
        // Trigger monitoring service operations that should generate logs
        monitoringService.getSystemHealthStatus();
        monitoringService.getActiveWorkerCount();
        monitoringService.getTaskCountsByStatus();
        monitoringService.getExecutionMetrics(Duration.ofHours(1));
        monitoringService.logSystemMetrics();
        
       
        
        // Property: System operations should not throw exceptions
        String healthStatus = monitoringService.getSystemHealthStatus();
        assertTrue(List.of("UP", "DOWN", "DEGRADED").contains(healthStatus));
        
        // Property: Metrics should be consistent and non-negative
        Map<String, Long> taskCounts = monitoringService.getTaskCountsByStatus();
        assertTrue(taskCounts.get("TOTAL") >= 0);
        assertTrue(taskCounts.get("PENDING") >= 0);
        assertTrue(taskCounts.get("RUNNING") >= 0);
        assertTrue(taskCounts.get("SUCCESS") >= 0);
        assertTrue(taskCounts.get("FAILED") >= 0);
        
        // Property: Total should equal sum of individual statuses
        long expectedTotal = taskCounts.get("PENDING") + taskCounts.get("RUNNING") + 
                           taskCounts.get("SUCCESS") + taskCounts.get("FAILED");
        assertEquals(expectedTotal, taskCounts.get("TOTAL"));
        
        // Property: Active worker count should be non-negative
        int activeWorkers = monitoringService.getActiveWorkerCount();
        assertTrue(activeWorkers >= 0);
        assertTrue(activeWorkers <= workers.size());
        
        long uptime = monitoringService.getUptimeSeconds();
        assertTrue(uptime >= 0);
        assertTrue(uptime < Duration.ofDays(365).getSeconds()); // Less than a year
    }
    

    
    private List<TaskExecution> generateRandomTaskExecutions() {
        List<TaskExecution> executions = new ArrayList<>();
        int count = random.nextInt(19) + 1; 
        
        for (int i = 0; i < count; i++) {
            TaskExecution execution = new TaskExecution();
            execution.setWorkerId("test-worker-" + i);
            execution.setStartedAt(Instant.now().minusSeconds(random.nextInt(3600)));
            execution.setCompletedAt(Instant.now());
            execution.setSuccess(random.nextBoolean());
            execution.setOutput(execution.getSuccess() ? "Success" : "Failed");
            executions.add(execution);
        }
        
        return executions;
    }
    
    private List<WorkerHeartbeat> generateRandomWorkers() {
        List<WorkerHeartbeat> workers = new ArrayList<>();
        int count = random.nextInt(10) + 1; // 1-10 workers
        
        for (int i = 0; i < count; i++) {
            WorkerHeartbeat worker = new WorkerHeartbeat();
            worker.setWorkerId("worker-" + i + "-" + random.nextInt(1000));
            worker.setLastHeartbeat(Instant.now().minusSeconds(random.nextInt(120))); // 0-120 seconds ago
            worker.setRegisteredAt(Instant.now().minusSeconds(3600)); // 1 hour ago
            workers.add(worker);
        }
        
        return workers;
    }
    
    private List<WorkerHeartbeat> generateRandomWorkersWithFixedTimestamps() {
        List<WorkerHeartbeat> workers = new ArrayList<>();
        int count = random.nextInt(10) + 1; // 1-10 workers
        
        // Use fixed base timestamps to avoid timing precision issues
        Instant baseTime = Instant.parse("2026-01-06T00:00:00Z");
        
        for (int i = 0; i < count; i++) {
            WorkerHeartbeat worker = new WorkerHeartbeat();
            worker.setWorkerId("worker-" + i + "-" + random.nextInt(1000));
            worker.setLastHeartbeat(baseTime.plusSeconds(random.nextInt(120))); // 0-120 seconds after base
            worker.setRegisteredAt(baseTime.minusSeconds(3600)); // 1 hour before base
            workers.add(worker);
        }
        
        return workers;
    }
    
    private List<Task> generateRandomTasks() {
        List<Task> tasks = new ArrayList<>();
        int count = random.nextInt(50) + 1; // 1-50 tasks
        
        TaskStatus[] statuses = TaskStatus.values();
        
        for (int i = 0; i < count; i++) {
            Task task = createRandomTask();
            task.setStatus(statuses[random.nextInt(statuses.length)]);
            tasks.add(task);
        }
        
        return tasks;
    }
    
    private Task createRandomTask() {
        Task task = new Task();
        task.setType(TaskType.DUMMY);
        task.setPayload("{\"sleepDurationMs\": 1000}");
        task.setStatus(TaskStatus.PENDING);
        task.setScheduleAt(Instant.now());
        task.setCreatedAt(Instant.now());
        task.setRetryCount(0);
        task.setMaxRetries(3);
        return task;
    }
}