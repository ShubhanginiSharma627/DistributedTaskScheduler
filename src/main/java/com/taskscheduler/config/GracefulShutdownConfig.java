package com.taskscheduler.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.event.ContextClosedEvent;
import org.springframework.context.event.EventListener;

import com.taskscheduler.service.SchedulerService;
import com.taskscheduler.service.WorkerService;

/**
 * Configuration for graceful shutdown handling.
 * Ensures proper cleanup of resources during application shutdown.
 */
@Configuration
public class GracefulShutdownConfig {
    
    private static final Logger logger = LoggerFactory.getLogger(GracefulShutdownConfig.class);
    
    private final SchedulerService schedulerService;
    private final WorkerService workerService;
    
    @Autowired
    public GracefulShutdownConfig(SchedulerService schedulerService, WorkerService workerService) {
        this.schedulerService = schedulerService;
        this.workerService = workerService;
    }
    
    /**
     * Handles graceful shutdown when the application context is closing.
     * Stops scheduler and worker services to prevent new task assignments.
     */
    @EventListener(ContextClosedEvent.class)
    public void handleContextClosed() {
        logger.info("Application shutdown initiated - performing graceful cleanup");
        
        try {
            // Stop scheduler to prevent new task assignments
            if (schedulerService != null) {
                logger.info("Stopping scheduler service...");
                schedulerService.stopScheduler();
                logger.info("Scheduler service stopped");
            }
            
            // Stop worker service to complete current tasks
            if (workerService != null) {
                logger.info("Stopping worker service...");
                workerService.shutdown();
                logger.info("Worker service stopped");
            }
            
            logger.info("Graceful shutdown completed successfully");
            
        } catch (Exception e) {
            logger.error("Error during graceful shutdown", e);
        }
    }
}