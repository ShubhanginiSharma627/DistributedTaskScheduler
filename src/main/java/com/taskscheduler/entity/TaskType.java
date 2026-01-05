package com.taskscheduler.entity;

/**
 * Enumeration of supported task types in the distributed task scheduler.
 * Each type corresponds to a different execution strategy.
 */
public enum TaskType {
    HTTP,
    SHELL,
    DUMMY
}