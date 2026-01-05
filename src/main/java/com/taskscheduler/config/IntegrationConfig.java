package com.taskscheduler.config;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.event.EventListener;
import org.springframework.core.annotation.Order;

import com.taskscheduler.service.DummyTaskExecutor;
import com.taskscheduler.service.HeartbeatMonitor;
import com.taskscheduler.service.HttpTaskExecutor;
import com.taskscheduler.service.MetricsLoggingService;
import com.taskscheduler.service.MonitoringService;
import com.taskscheduler.service.RetryManager;
import com.taskscheduler.service.SchedulerService;
import com.taskscheduler.service.ShellTaskExecutor;
import com.taskscheduler.service.SystemRecoveryService;
import com.taskscheduler.service.TaskExecutionService;
import com.taskscheduler.service.TaskExecutor;
import com.taskscheduler.service.TaskService;
import com.taskscheduler.service.WorkerService;

/**
 * Integration configuration that wires all components together.
 * Ensures proper initialization order and component dependencies.
 */
@Configuration
public class IntegrationConfig {
    
    private static final Logger logger = LoggerFactory.getLogger(IntegrationConfig.class);
    
    private final TaskSchedulerConfig config;
    
    @Autowired
    public IntegrationConfig(TaskSchedulerConfig config) {
        this.config = config;
    }
    
    /**
     * Creates a list of all available task executors for dependency injection.
     * This allows the TaskExecutionService to access all executor implementations.
     */
    @Bean
    public List<TaskExecutor> taskExecutors(
            HttpTaskExecutor httpTaskExecutor,
            ShellTaskExecutor shellTaskExecutor,
            DummyTaskExecutor dummyTaskExecutor) {
        
        logger.info("Registering task executors: HTTP, Shell, Dummy");
        return List.of(httpTaskExecutor, shellTaskExecutor, dummyTaskExecutor);
    }
    
    /**
     * Application startup event handler that initializes all services in proper order.
     * Ensures system recovery runs first, then starts monitoring and scheduling services.
     */
    @EventListener(ApplicationReadyEvent.class)
    @Order(1)
    public void onApplicationReady(ApplicationReadyEvent event) {
        logger.info("=== Distributed Task Scheduler Application Ready ===");
        
        
        logConfigurationSummary();
        
       
        validateServiceAvailability(event);
        
        logger.info("=== All components successfully wired and initialized ===");
    }
    
    /**
     * Logs the current configuration summary for debugging and monitoring.
     */
    private void logConfigurationSummary() {
        logger.info("Configuration Summary:");
        logger.info("  Scheduler: enabled={}, polling-interval={}ms", 
                   config.getScheduler().isEnabled(), 
                   config.getScheduler().getPollingIntervalMs());
        
        logger.info("  Worker: enabled={}, heartbeat-interval={}ms, heartbeat-timeout={}ms", 
                   config.getWorker().isEnabled(),
                   config.getWorker().getHeartbeatIntervalMs(),
                   config.getWorker().getHeartbeatTimeoutMs());
        
        logger.info("  Retry: max-retries={}, base-delay={}ms, max-delay={}ms", 
                   config.getRetry().getDefaultMaxRetries(),
                   config.getRetry().getBaseDelayMs(),
                   config.getRetry().getMaxDelayMs());
        
        logger.info("  Monitoring: failure-detection-interval={}ms, metrics-enabled={}", 
                   config.getMonitoring().getFailureDetectionIntervalMs(),
                   config.getMonitoring().isMetricsCollectionEnabled());
        
        logger.info("  Logging: correlation-id={}, structured={}, events={}, performance={}", 
                   config.getLogging().isCorrelationIdEnabled(),
                   config.getLogging().isStructuredLoggingEnabled(),
                   config.getLogging().isEventLoggingEnabled(),
                   config.getLogging().isPerformanceLoggingEnabled());
    }
    
    /**
     * Validates that all required services are properly available and configured.
     */
    private void validateServiceAvailability(ApplicationReadyEvent event) {
        var context = event.getApplicationContext();
        
       
        validateService(context, TaskService.class, "Task Service");
        validateService(context, TaskExecutionService.class, "Task Execution Service");
        validateService(context, SchedulerService.class, "Scheduler Service");
        validateService(context, WorkerService.class, "Worker Service");
        validateService(context, RetryManager.class, "Retry Manager");
        
     
        validateService(context, HeartbeatMonitor.class, "Heartbeat Monitor");
        validateService(context, MonitoringService.class, "Monitoring Service");
        validateService(context, SystemRecoveryService.class, "System Recovery Service");
        
 
        validateService(context, HttpTaskExecutor.class, "HTTP Task Executor");
        validateService(context, ShellTaskExecutor.class, "Shell Task Executor");
        validateService(context, DummyTaskExecutor.class, "Dummy Task Executor");
        
      
        try {
            validateService(context, MetricsLoggingService.class, "Metrics Logging Service");
        } catch (Exception e) {
            logger.info("Metrics Logging Service not available (may be disabled by configuration)");
        }
        
        logger.info("All required services validated successfully");
    }
    
    /**
     * Validates that a specific service is available in the application context.
     */
    private void validateService(org.springframework.context.ApplicationContext context, 
                               Class<?> serviceClass, String serviceName) {
        try {
            Object service = context.getBean(serviceClass);
            if (service != null) {
                logger.debug("✓ {} available: {}", serviceName, service.getClass().getSimpleName());
            } else {
                throw new RuntimeException(serviceName + " is null");
            }
        } catch (Exception e) {
            logger.error("✗ {} not available", serviceName, e);
            throw new RuntimeException("Required service not available: " + serviceName, e);
        }
    }
}