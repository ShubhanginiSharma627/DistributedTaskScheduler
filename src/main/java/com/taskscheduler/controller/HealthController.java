package com.taskscheduler.controller;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.taskscheduler.dto.HealthResponse;
import com.taskscheduler.dto.WorkerStatusResponse;
import com.taskscheduler.service.MonitoringService;
import com.taskscheduler.service.SystemRecoveryService;

/**
 * REST controller for system health and monitoring endpoints.
 * Provides health status, worker information, and system metrics.
 */
@RestController
@RequestMapping("/health")
public class HealthController {
    
    private static final Logger logger = LoggerFactory.getLogger(HealthController.class);
    
    private final MonitoringService monitoringService;
    private final SystemRecoveryService systemRecoveryService;
    
    @Autowired
    public HealthController(MonitoringService monitoringService, SystemRecoveryService systemRecoveryService) {
        this.monitoringService = monitoringService;
        this.systemRecoveryService = systemRecoveryService;
    }
    
    /**
     * Gets overall system health status.
     * Returns system status, active workers, task counts, and uptime.
     */
    @GetMapping
    public ResponseEntity<HealthResponse> getHealth() {
        try {
            logger.debug("Health check requested");
            
            String systemStatus = monitoringService.getSystemHealthStatus();
            Instant timestamp = Instant.now();
            long uptimeSeconds = monitoringService.getUptimeSeconds();
            int activeWorkers = monitoringService.getActiveWorkerCount();
            
            Map<String, Long> taskCounts = monitoringService.getTaskCountsByStatus();
            long pendingTasks = taskCounts.get("PENDING");
            long runningTasks = taskCounts.get("RUNNING");
            long totalTasks = taskCounts.get("TOTAL");
            
        
            Map<String, Object> metrics = monitoringService.getExecutionMetrics(Duration.ofHours(1));
            
            HealthResponse response = new HealthResponse(
                    systemStatus,
                    timestamp,
                    uptimeSeconds,
                    activeWorkers,
                    pendingTasks,
                    runningTasks,
                    totalTasks,
                    metrics
            );
            
            logger.debug("Health check completed - Status: {}, Workers: {}, Pending: {}", 
                    systemStatus, activeWorkers, pendingTasks);
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("Error during health check", e);
            
          
            HealthResponse errorResponse = new HealthResponse(
                    "DOWN",
                    Instant.now(),
                    monitoringService.getUptimeSeconds(),
                    0,
                    0,
                    0,
                    0,
                    Map.of("error", "Health check failed: " + e.getMessage())
            );
            
            return ResponseEntity.status(503).body(errorResponse);
        }
    }
    
    /**
     * Gets detailed information about all workers.
     * Returns worker status, heartbeat information, and activity status.
     */
    @GetMapping("/workers")
    public ResponseEntity<List<WorkerStatusResponse>> getWorkerStatuses() {
        try {
            logger.debug("Worker status check requested");
            
            List<WorkerStatusResponse> workerStatuses = monitoringService.getWorkerStatuses();
            
            logger.debug("Worker status check completed - {} workers found", workerStatuses.size());
            
            return ResponseEntity.ok(workerStatuses);
            
        } catch (Exception e) {
            logger.error("Error getting worker statuses", e);
            return ResponseEntity.status(500).build();
        }
    }
    
    /**
     * Gets execution metrics for a specified time period.
     * Supports query parameters for customizing the metrics period.
     */
    @GetMapping("/metrics")
    public ResponseEntity<Map<String, Object>> getMetrics(
            @RequestParam(value = "hours", defaultValue = "1") int hours) {
        try {
            logger.debug("Metrics requested for {} hours", hours);
            
            if (hours < 1 || hours > 168) { // Max 1 week
                return ResponseEntity.badRequest().build();
            }
            
            Duration period = Duration.ofHours(hours);
            Map<String, Object> metrics = monitoringService.getExecutionMetrics(period);
            
            logger.debug("Metrics retrieved for {} hour period", hours);
            
            return ResponseEntity.ok(metrics);
            
        } catch (Exception e) {
            logger.error("Error getting metrics", e);
            return ResponseEntity.status(500).build();
        }
    }
    
    /**
     * Gets task count statistics by status.
     * Provides breakdown of tasks in different states.
     */
    @GetMapping("/tasks/counts")
    public ResponseEntity<Map<String, Long>> getTaskCounts() {
        try {
            logger.debug("Task counts requested");
            
            Map<String, Long> taskCounts = monitoringService.getTaskCountsByStatus();
            
            logger.debug("Task counts retrieved - Total: {}", taskCounts.get("TOTAL"));
            
            return ResponseEntity.ok(taskCounts);
            
        } catch (Exception e) {
            logger.error("Error getting task counts", e);
            return ResponseEntity.status(500).build();
        }
    }
    
    /**
     * Simple liveness probe endpoint.
     * Returns 200 OK if the application is running.
     */
    @GetMapping("/live")
    public ResponseEntity<Map<String, Object>> getLiveness() {
        return ResponseEntity.ok(Map.of(
                "status", "UP",
                "timestamp", Instant.now(),
                "uptime_seconds", monitoringService.getUptimeSeconds()
        ));
    }
    
    /**
     * Readiness probe endpoint.
     * Returns 200 OK if the application is ready to serve traffic.
     * Checks database connectivity and basic system health.
     */
    @GetMapping("/ready")
    public ResponseEntity<Map<String, Object>> getReadiness() {
        try {
            String healthStatus = monitoringService.getSystemHealthStatus();
            
            if ("DOWN".equals(healthStatus)) {
                return ResponseEntity.status(503).body(Map.of(
                        "status", "NOT_READY",
                        "timestamp", Instant.now(),
                        "reason", "System health check failed"
                ));
            }
            
            return ResponseEntity.ok(Map.of(
                    "status", "READY",
                    "timestamp", Instant.now(),
                    "health_status", healthStatus
            ));
            
        } catch (Exception e) {
            logger.error("Readiness check failed", e);
            return ResponseEntity.status(503).body(Map.of(
                    "status", "NOT_READY",
                    "timestamp", Instant.now(),
                    "error", e.getMessage()
            ));
        }
    }
    
    /**
     * Performs manual system recovery operation.
     * Recovers RUNNING tasks and cleans up stale worker data.
     */
    @PostMapping("/recovery")
    public ResponseEntity<Map<String, Object>> performRecovery() {
        try {
            logger.info("Manual system recovery requested");
            
            SystemRecoveryService.RecoveryResult result = systemRecoveryService.performManualRecovery();
            
            Map<String, Object> response = Map.of(
                    "status", result.isSuccessful() ? "SUCCESS" : "FAILED",
                    "timestamp", Instant.now(),
                    "recovered_tasks", result.getRecoveredTasks(),
                    "cleaned_workers", result.getCleanedWorkers(),
                    "message", "System recovery completed successfully"
            );
            
            logger.info("Manual system recovery completed: {}", result);
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("Manual system recovery failed", e);
            
            Map<String, Object> errorResponse = Map.of(
                    "status", "FAILED",
                    "timestamp", Instant.now(),
                    "error", e.getMessage(),
                    "message", "System recovery failed"
            );
            
            return ResponseEntity.status(500).body(errorResponse);
        }
    }
    
    /**
     * Checks system state consistency.
     * Verifies that the system is in a consistent state without orphaned tasks.
     */
    @GetMapping("/consistency")
    public ResponseEntity<Map<String, Object>> checkConsistency() {
        try {
            logger.debug("System consistency check requested");
            
            boolean isConsistent = systemRecoveryService.isSystemStateConsistent();
            
            Map<String, Object> response = Map.of(
                    "status", isConsistent ? "CONSISTENT" : "INCONSISTENT",
                    "timestamp", Instant.now(),
                    "consistent", isConsistent,
                    "message", isConsistent ? "System state is consistent" : "System state inconsistencies detected"
            );
            
            logger.debug("System consistency check completed - Consistent: {}", isConsistent);
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("System consistency check failed", e);
            
            Map<String, Object> errorResponse = Map.of(
                    "status", "ERROR",
                    "timestamp", Instant.now(),
                    "error", e.getMessage(),
                    "message", "Consistency check failed"
            );
            
            return ResponseEntity.status(500).body(errorResponse);
        }
    }
}