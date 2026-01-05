package com.taskscheduler.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

/**
 * Main application configuration class.
 * Defines beans and enables configuration properties.
 */
@Configuration
@EnableConfigurationProperties(TaskSchedulerConfig.class)
public class ApplicationConfig {
    
    /**
     * RestTemplate bean for HTTP task execution.
     */
    @Bean
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }
}