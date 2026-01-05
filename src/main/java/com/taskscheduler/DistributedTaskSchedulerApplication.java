package com.taskscheduler;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.transaction.annotation.EnableTransactionManagement;

import com.taskscheduler.config.TaskSchedulerConfig;

/**
 * Main application class for the Distributed Task Scheduler.
 * 
 * Enables:
 * - Spring Boot auto-configuration
 * - Scheduled task execution (@EnableScheduling)
 * - Configuration properties binding
 * - Transaction management
 */
@SpringBootApplication(scanBasePackages = {
    "com.taskscheduler.config",
    "com.taskscheduler.controller", 
    "com.taskscheduler.service",
    "com.taskscheduler.repository",
    "com.taskscheduler.events",
    "com.taskscheduler.dto"
})
@EnableScheduling
@EnableTransactionManagement
@EnableConfigurationProperties(TaskSchedulerConfig.class)
public class DistributedTaskSchedulerApplication {

    public static void main(String[] args) {
        SpringApplication.run(DistributedTaskSchedulerApplication.class, args);
    }
}