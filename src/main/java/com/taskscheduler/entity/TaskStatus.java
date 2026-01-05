package com.taskscheduler.entity;

/**
 * Enumeration of possible task statuses in the distributed task scheduler.
 * Represents the lifecycle states of a task from creation to completion.
 */
public enum TaskStatus {
    PENDING,
    RUNNING,
    SUCCESS,
    FAILED
}