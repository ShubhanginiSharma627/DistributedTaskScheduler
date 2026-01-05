package com.taskscheduler.service;

/**
 * Exception thrown when task execution fails in an unrecoverable way.
 * This exception indicates that the task should not be retried.
 */
public class TaskExecutionException extends Exception {
    
    public TaskExecutionException(String message) {
        super(message);
    }
    
    public TaskExecutionException(String message, Throwable cause) {
        super(message, cause);
    }
}