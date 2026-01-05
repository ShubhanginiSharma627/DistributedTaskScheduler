package com.taskscheduler.dto;

import java.time.Instant;
import java.util.List;

/**
 * DTO for error responses.
 * Contains error information for API error responses.
 */
public class ErrorResponse {
    
    private String error;
    private String message;
    private int status;
    private Instant timestamp;
    private String path;
    private List<String> details;
    
   
    public ErrorResponse() {
        this.timestamp = Instant.now();
    }
    

    public ErrorResponse(String error, String message, int status, String path) {
        this();
        this.error = error;
        this.message = message;
        this.status = status;
        this.path = path;
    }
    
    
    public ErrorResponse(String error, String message, int status, String path, List<String> details) {
        this(error, message, status, path);
        this.details = details;
    }
    

    public String getError() {
        return error;
    }
    
    public void setError(String error) {
        this.error = error;
    }
    
    public String getMessage() {
        return message;
    }
    
    public void setMessage(String message) {
        this.message = message;
    }
    
    public int getStatus() {
        return status;
    }
    
    public void setStatus(int status) {
        this.status = status;
    }
    
    public Instant getTimestamp() {
        return timestamp;
    }
    
    public void setTimestamp(Instant timestamp) {
        this.timestamp = timestamp;
    }
    
    public String getPath() {
        return path;
    }
    
    public void setPath(String path) {
        this.path = path;
    }
    
    public List<String> getDetails() {
        return details;
    }
    
    public void setDetails(List<String> details) {
        this.details = details;
    }
}