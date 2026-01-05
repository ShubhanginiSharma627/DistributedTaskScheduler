package com.taskscheduler.service;

import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.taskscheduler.entity.Task;
import com.taskscheduler.entity.TaskType;

/**
 * TaskExecutor implementation for HTTP tasks.
 * Executes HTTP requests based on task payload configuration.
 */
@Component
public class HttpTaskExecutor implements TaskExecutor {
    
    private static final Logger logger = LoggerFactory.getLogger(HttpTaskExecutor.class);
    
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    
    public HttpTaskExecutor(RestTemplate restTemplate, ObjectMapper objectMapper) {
        this.restTemplate = restTemplate;
        this.objectMapper = objectMapper;
    }
    
    @Override
    public ExecutionResult execute(Task task) throws TaskExecutionException {
        if (!canExecute(task)) {
            throw new TaskExecutionException("HttpTaskExecutor cannot execute task of type: " + task.getType());
        }
        
        try {
          
            JsonNode payloadNode = objectMapper.readTree(task.getPayload());
            
            String url = payloadNode.get("url").asText();
            String method = payloadNode.has("method") ? payloadNode.get("method").asText() : "GET";
            
     
            HttpHeaders headers = new HttpHeaders();
            if (payloadNode.has("headers")) {
                JsonNode headersNode = payloadNode.get("headers");
                headersNode.fields().forEachRemaining(entry -> {
                    headers.add(entry.getKey(), entry.getValue().asText());
                });
            }
            
       
            String requestBody = null;
            if (payloadNode.has("body")) {
                requestBody = payloadNode.get("body").asText();
            }
            
           
            HttpEntity<String> entity = new HttpEntity<>(requestBody, headers);
            
           
            HttpMethod httpMethod = HttpMethod.valueOf(method.toUpperCase());
            ResponseEntity<String> response = restTemplate.exchange(url, httpMethod, entity, String.class);
            
       
            Map<String, Object> metadata = new HashMap<>();
            metadata.put("statusCode", response.getStatusCode().value());
            metadata.put("method", method);
            metadata.put("url", url);
            metadata.put("responseHeaders", response.getHeaders().toSingleValueMap());
            
         
            boolean success = response.getStatusCode().is2xxSuccessful();
            String output = response.getBody();
            
            logger.info("HTTP task executed: {} {} - Status: {}", method, url, response.getStatusCode());
            
            return new ExecutionResult(success, output, null, metadata);
            
        } catch (HttpClientErrorException e) {
           
            Map<String, Object> metadata = new HashMap<>();
            metadata.put("statusCode", e.getStatusCode().value());
            metadata.put("error", "Client error");
            
            logger.warn("HTTP task failed with client error: {}", e.getMessage());
            return ExecutionResult.failure("HTTP client error: " + e.getMessage(), metadata);
            
        } catch (HttpServerErrorException e) {
           
            Map<String, Object> metadata = new HashMap<>();
            metadata.put("statusCode", e.getStatusCode().value());
            metadata.put("error", "Server error");
            
            logger.warn("HTTP task failed with server error: {}", e.getMessage());
            return ExecutionResult.failure("HTTP server error: " + e.getMessage(), metadata);
            
        } catch (ResourceAccessException e) {

            Map<String, Object> metadata = new HashMap<>();
            metadata.put("error", "Network error");
            
            logger.warn("HTTP task failed with network error: {}", e.getMessage());
            return ExecutionResult.failure("HTTP network error: " + e.getMessage(), metadata);
            
        } catch (Exception e) {
            logger.error("HTTP task execution failed unexpectedly", e);
            throw new TaskExecutionException("HTTP task execution failed: " + e.getMessage(), e);
        }
    }
    
    @Override
    public boolean canExecute(Task task) {
        return task.getType() == TaskType.HTTP;
    }
}