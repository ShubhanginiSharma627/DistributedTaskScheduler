package com.taskscheduler.dto;

import java.util.List;

import org.springframework.data.domain.Page;

/**
 * DTO for paginated task list responses.
 * Contains task list with pagination metadata.
 */
public class TaskListResponse {
    
    private List<TaskResponse> tasks;
    private int page;
    private int size;
    private long totalElements;
    private int totalPages;
    private boolean first;
    private boolean last;
    
    
    public TaskListResponse() {}
    
  
    public TaskListResponse(Page<TaskResponse> page) {
        this.tasks = page.getContent();
        this.page = page.getNumber();
        this.size = page.getSize();
        this.totalElements = page.getTotalElements();
        this.totalPages = page.getTotalPages();
        this.first = page.isFirst();
        this.last = page.isLast();
    }
    
 
    public static TaskListResponse from(Page<TaskResponse> page) {
        return new TaskListResponse(page);
    }
    

    public List<TaskResponse> getTasks() {
        return tasks;
    }
    
    public void setTasks(List<TaskResponse> tasks) {
        this.tasks = tasks;
    }
    
    public int getPage() {
        return page;
    }
    
    public void setPage(int page) {
        this.page = page;
    }
    
    public int getSize() {
        return size;
    }
    
    public void setSize(int size) {
        this.size = size;
    }
    
    public long getTotalElements() {
        return totalElements;
    }
    
    public void setTotalElements(long totalElements) {
        this.totalElements = totalElements;
    }
    
    public int getTotalPages() {
        return totalPages;
    }
    
    public void setTotalPages(int totalPages) {
        this.totalPages = totalPages;
    }
    
    public boolean isFirst() {
        return first;
    }
    
    public void setFirst(boolean first) {
        this.first = first;
    }
    
    public boolean isLast() {
        return last;
    }
    
    public void setLast(boolean last) {
        this.last = last;
    }
}