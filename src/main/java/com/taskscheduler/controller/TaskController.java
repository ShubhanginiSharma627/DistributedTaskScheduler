package com.taskscheduler.controller;

import java.util.Optional;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.taskscheduler.entity.Task;
import com.taskscheduler.service.TaskService;

@RestController
@RequestMapping("/tasks")
public class TaskController {
    
    private final TaskService taskService;
    
    @Autowired
    public TaskController(TaskService taskService) {
        this.taskService = taskService;
    }
    
  
    
    /**
     * Cancels a pending task.
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> cancelTask(@PathVariable UUID id) {
        boolean cancelled = taskService.cancelTask(id);
        
        if (!cancelled) {
            // Check if task exists to determine appropriate response
            Optional<Task> task = taskService.getTask(id);
            if (task.isEmpty()) {
                return ResponseEntity.notFound().build();
            } else {
                // Task exists but cannot be cancelled (not PENDING)
                return ResponseEntity.status(HttpStatus.CONFLICT).build();
            }
        }
        
        return ResponseEntity.ok().build();
    }
}