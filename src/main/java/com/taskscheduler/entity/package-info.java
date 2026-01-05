/**
 * Entity classes for the distributed task scheduler.
 * 
 * Core entities:
 * - Task: Represents a unit of work to be executed with scheduling and retry information
 * - TaskType: Enumeration of supported task types (HTTP, SHELL, DUMMY)
 * - TaskStatus: Enumeration of task lifecycle states (PENDING, RUNNING, SUCCESS, FAILED)
 * - WorkerHeartbeat: Tracks worker liveness through periodic heartbeat signals
 * - TaskExecution: Records detailed execution history for each task attempt
 * 
 * These entities form the foundation of the distributed task scheduling system,
 * providing data persistence and tracking capabilities for reliable task execution
 * with proper JPA relationships and database constraints.
 */
package com.taskscheduler.entity;