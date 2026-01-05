package com.taskscheduler.util;

import java.util.UUID;

import org.slf4j.MDC;

/**
 * Utility class for managing logging context and correlation IDs.
 * Provides methods for setting and clearing context information in logs.
 */
public class LoggingContext {
    
    public static final String CORRELATION_ID = "correlationId";
    public static final String TASK_ID = "taskId";
    public static final String WORKER_ID = "workerId";
    public static final String OPERATION = "operation";
    public static final String USER_ID = "userId";
    
   
    public static void setCorrelationId(String correlationId) {
        if (correlationId == null || correlationId.trim().isEmpty()) {
            correlationId = generateCorrelationId();
        }
        MDC.put(CORRELATION_ID, correlationId);
    }
    
   
    public static String getCorrelationId() {
        return MDC.get(CORRELATION_ID);
    }
    
   
    public static String generateCorrelationId() {
        return UUID.randomUUID().toString().substring(0, 8);
    }
    
  
    public static void setTaskId(String taskId) {
        if (taskId != null) {
            MDC.put(TASK_ID, taskId);
        }
    }
    
   
    public static void setWorkerId(String workerId) {
        if (workerId != null) {
            MDC.put(WORKER_ID, workerId);
        }
    }
    
   
    public static void setOperation(String operation) {
        if (operation != null) {
            MDC.put(OPERATION, operation);
        }
    }
    
  
    public static void setUserId(String userId) {
        if (userId != null) {
            MDC.put(USER_ID, userId);
        }
    }
    
   
    public static String getTaskId() {
        return MDC.get(TASK_ID);
    }
    
   
    public static String getWorkerId() {
        return MDC.get(WORKER_ID);
    }
    
    
    public static String getOperation() {
        return MDC.get(OPERATION);
    }
    
    
    public static void clear(String key) {
        MDC.remove(key);
    }
  
    public static void clearCorrelationId() {
        MDC.remove(CORRELATION_ID);
    }
    
   
    public static void clearTaskId() {
        MDC.remove(TASK_ID);
    }
    
    
    public static void clearWorkerId() {
        MDC.remove(WORKER_ID);
    }
    
   
    public static void clearAll() {
        MDC.clear();
    }
    
    /**
     * Executes a runnable with a specific correlation ID.
     * Automatically cleans up the context after execution.
     */
    public static void withCorrelationId(String correlationId, Runnable runnable) {
        String previousCorrelationId = getCorrelationId();
        try {
            setCorrelationId(correlationId);
            runnable.run();
        } finally {
            if (previousCorrelationId != null) {
                setCorrelationId(previousCorrelationId);
            } else {
                clearCorrelationId();
            }
        }
    }
    
    /**
     * Executes a runnable with task context (task ID and correlation ID).
     * Automatically cleans up the context after execution.
     */
    public static void withTaskContext(String taskId, String correlationId, Runnable runnable) {
        String previousTaskId = getTaskId();
        String previousCorrelationId = getCorrelationId();
        try {
            setTaskId(taskId);
            setCorrelationId(correlationId);
            runnable.run();
        } finally {
            if (previousTaskId != null) {
                setTaskId(previousTaskId);
            } else {
                clearTaskId();
            }
            if (previousCorrelationId != null) {
                setCorrelationId(previousCorrelationId);
            } else {
                clearCorrelationId();
            }
        }
    }
    
    /**
     * Executes a runnable with worker context (worker ID and correlation ID).
     * Automatically cleans up the context after execution.
     */
    public static void withWorkerContext(String workerId, String correlationId, Runnable runnable) {
        String previousWorkerId = getWorkerId();
        String previousCorrelationId = getCorrelationId();
        try {
            setWorkerId(workerId);
            setCorrelationId(correlationId);
            runnable.run();
        } finally {
            if (previousWorkerId != null) {
                setWorkerId(previousWorkerId);
            } else {
                clearWorkerId();
            }
            if (previousCorrelationId != null) {
                setCorrelationId(previousCorrelationId);
            } else {
                clearCorrelationId();
            }
        }
    }
}