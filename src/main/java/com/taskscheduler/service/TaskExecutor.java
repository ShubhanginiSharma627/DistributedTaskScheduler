package com.taskscheduler.service;

import com.taskscheduler.entity.Task;

/**
 * Interface for executing different types of tasks in the distributed task scheduler.
 * Each task type has its own implementation that handles the specific execution logic.
 */
public interface TaskExecutor {
    
   
    ExecutionResult execute(Task task) throws TaskExecutionException;
    
    /**
     * Checks if this executor can handle the given task type.
     * 
     * @param task The task to check
     * @return true if this executor can handle the task, false otherwise
     */
    boolean canExecute(Task task);
}