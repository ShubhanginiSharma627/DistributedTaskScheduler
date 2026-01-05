package com.taskscheduler.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuration properties for the task scheduler application.
 * Maps application-specific properties from application.yml.
 */
@ConfigurationProperties(prefix = "task-scheduler")
public class TaskSchedulerConfig {
    
    private Scheduler scheduler = new Scheduler();
    private Worker worker = new Worker();
    private Retry retry = new Retry();
    private Monitoring monitoring = new Monitoring();
    private Logging logging = new Logging();
    
    public Scheduler getScheduler() {
        return scheduler;
    }
    
    public void setScheduler(Scheduler scheduler) {
        this.scheduler = scheduler;
    }
    
    public Worker getWorker() {
        return worker;
    }
    
    public void setWorker(Worker worker) {
        this.worker = worker;
    }
    
    public Retry getRetry() {
        return retry;
    }
    
    public void setRetry(Retry retry) {
        this.retry = retry;
    }
    
    public Monitoring getMonitoring() {
        return monitoring;
    }
    
    public void setMonitoring(Monitoring monitoring) {
        this.monitoring = monitoring;
    }
    
    public Logging getLogging() {
        return logging;
    }
    
    public void setLogging(Logging logging) {
        this.logging = logging;
    }
    
    public static class Scheduler {
        private long pollingIntervalMs = 1000;
        private boolean enabled = true;
        
        public long getPollingIntervalMs() {
            return pollingIntervalMs;
        }
        
        public void setPollingIntervalMs(long pollingIntervalMs) {
            this.pollingIntervalMs = pollingIntervalMs;
        }
        
        public boolean isEnabled() {
            return enabled;
        }
        
        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }
    }
    
    public static class Worker {
        private long heartbeatIntervalMs = 30000;
        private long heartbeatTimeoutMs = 60000;
        private boolean enabled = true;
        
        public long getHeartbeatIntervalMs() {
            return heartbeatIntervalMs;
        }
        
        public void setHeartbeatIntervalMs(long heartbeatIntervalMs) {
            this.heartbeatIntervalMs = heartbeatIntervalMs;
        }
        
        public long getHeartbeatTimeoutMs() {
            return heartbeatTimeoutMs;
        }
        
        public void setHeartbeatTimeoutMs(long heartbeatTimeoutMs) {
            this.heartbeatTimeoutMs = heartbeatTimeoutMs;
        }
        
        public boolean isEnabled() {
            return enabled;
        }
        
        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }
    }
    
    public static class Retry {
        private int defaultMaxRetries = 3;
        private long baseDelayMs = 1000;
        private long maxDelayMs = 300000;
        
        public int getDefaultMaxRetries() {
            return defaultMaxRetries;
        }
        
        public void setDefaultMaxRetries(int defaultMaxRetries) {
            this.defaultMaxRetries = defaultMaxRetries;
        }
        
        public long getBaseDelayMs() {
            return baseDelayMs;
        }
        
        public void setBaseDelayMs(long baseDelayMs) {
            this.baseDelayMs = baseDelayMs;
        }
        
        public long getMaxDelayMs() {
            return maxDelayMs;
        }
        
        public void setMaxDelayMs(long maxDelayMs) {
            this.maxDelayMs = maxDelayMs;
        }
    }
    
    public static class Monitoring {
        private long failureDetectionIntervalMs = 30000;
        private boolean metricsCollectionEnabled = true;
        private long metricsLoggingIntervalMs = 300000;
        
        public long getFailureDetectionIntervalMs() {
            return failureDetectionIntervalMs;
        }
        
        public void setFailureDetectionIntervalMs(long failureDetectionIntervalMs) {
            this.failureDetectionIntervalMs = failureDetectionIntervalMs;
        }
        
        public boolean isMetricsCollectionEnabled() {
            return metricsCollectionEnabled;
        }
        
        public void setMetricsCollectionEnabled(boolean metricsCollectionEnabled) {
            this.metricsCollectionEnabled = metricsCollectionEnabled;
        }
        
        public long getMetricsLoggingIntervalMs() {
            return metricsLoggingIntervalMs;
        }
        
        public void setMetricsLoggingIntervalMs(long metricsLoggingIntervalMs) {
            this.metricsLoggingIntervalMs = metricsLoggingIntervalMs;
        }
    }
    
    public static class Logging {
        private boolean correlationIdEnabled = true;
        private boolean structuredLoggingEnabled = true;
        private boolean eventLoggingEnabled = true;
        private boolean performanceLoggingEnabled = true;
        
        public boolean isCorrelationIdEnabled() {
            return correlationIdEnabled;
        }
        
        public void setCorrelationIdEnabled(boolean correlationIdEnabled) {
            this.correlationIdEnabled = correlationIdEnabled;
        }
        
        public boolean isStructuredLoggingEnabled() {
            return structuredLoggingEnabled;
        }
        
        public void setStructuredLoggingEnabled(boolean structuredLoggingEnabled) {
            this.structuredLoggingEnabled = structuredLoggingEnabled;
        }
        
        public boolean isEventLoggingEnabled() {
            return eventLoggingEnabled;
        }
        
        public void setEventLoggingEnabled(boolean eventLoggingEnabled) {
            this.eventLoggingEnabled = eventLoggingEnabled;
        }
        
        public boolean isPerformanceLoggingEnabled() {
            return performanceLoggingEnabled;
        }
        
        public void setPerformanceLoggingEnabled(boolean performanceLoggingEnabled) {
            this.performanceLoggingEnabled = performanceLoggingEnabled;
        }
    }
}