package com.taskscheduler.dto.validation;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.taskscheduler.dto.TaskCreateRequest;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

/**
 * Validator for task payloads based on task type.
 * Validates that the payload contains required fields for each task type.
 */
public class TaskPayloadValidator implements ConstraintValidator<ValidTaskPayload, TaskCreateRequest> {
    
    private final ObjectMapper objectMapper = new ObjectMapper();
    
    @Override
    public void initialize(ValidTaskPayload constraintAnnotation) {
       
    }
    
    @Override
    public boolean isValid(TaskCreateRequest request, ConstraintValidatorContext context) {
        if (request == null || request.getType() == null || request.getPayload() == null) {
            return false;
        }
        
        try {
            JsonNode payloadNode = objectMapper.readTree(request.getPayload());
            
            switch (request.getType()) {
                case HTTP:
                    return validateHttpPayload(payloadNode, context);
                case SHELL:
                    return validateShellPayload(payloadNode, context);
                case DUMMY:
                    return validateDummyPayload(payloadNode, context);
                default:
                    addConstraintViolation(context, "Unsupported task type: " + request.getType());
                    return false;
            }
        } catch (Exception e) {
            addConstraintViolation(context, "Invalid JSON payload: " + e.getMessage());
            return false;
        }
    }
    
    private boolean validateHttpPayload(JsonNode payload, ConstraintValidatorContext context) {
        boolean valid = true;
        
        // Required: url
        if (!payload.has("url") || payload.get("url").asText().trim().isEmpty()) {
            addConstraintViolation(context, "HTTP task payload must contain 'url' field");
            valid = false;
        }
        
        // Required: method
        if (!payload.has("method") || payload.get("method").asText().trim().isEmpty()) {
            addConstraintViolation(context, "HTTP task payload must contain 'method' field");
            valid = false;
        } else {
            String method = payload.get("method").asText().toUpperCase();
            if (!isValidHttpMethod(method)) {
                addConstraintViolation(context, "Invalid HTTP method: " + method);
                valid = false;
            }
        }
        
  
        if (payload.has("headers") && !payload.get("headers").isObject()) {
            addConstraintViolation(context, "HTTP task payload 'headers' must be an object");
            valid = false;
        }
        
       
        if (payload.has("body") && payload.get("body").isNull()) {
            addConstraintViolation(context, "HTTP task payload 'body' cannot be null");
            valid = false;
        }
        
        return valid;
    }
    
    private boolean validateShellPayload(JsonNode payload, ConstraintValidatorContext context) {
        boolean valid = true;
        
        // Required: command
        if (!payload.has("command") || payload.get("command").asText().trim().isEmpty()) {
            addConstraintViolation(context, "Shell task payload must contain 'command' field");
            valid = false;
        }
        
  
        if (payload.has("workingDirectory") && payload.get("workingDirectory").asText().trim().isEmpty()) {
            addConstraintViolation(context, "Shell task payload 'workingDirectory' cannot be empty");
            valid = false;
        }
        
       
        if (payload.has("environment") && !payload.get("environment").isObject()) {
            addConstraintViolation(context, "Shell task payload 'environment' must be an object");
            valid = false;
        }
        
        return valid;
    }
    
    private boolean validateDummyPayload(JsonNode payload, ConstraintValidatorContext context) {
        boolean valid = true;
        
        // Required: sleepDurationMs
        if (!payload.has("sleepDurationMs")) {
            addConstraintViolation(context, "Dummy task payload must contain 'sleepDurationMs' field");
            valid = false;
        } else {
            JsonNode sleepNode = payload.get("sleepDurationMs");
            if (!sleepNode.isNumber() || sleepNode.asLong() < 0) {
                addConstraintViolation(context, "Dummy task payload 'sleepDurationMs' must be a non-negative number");
                valid = false;
            }
        }
        

        if (payload.has("logMessage") && payload.get("logMessage").asText().trim().isEmpty()) {
            addConstraintViolation(context, "Dummy task payload 'logMessage' cannot be empty");
            valid = false;
        }
        
        return valid;
    }
    
    private boolean isValidHttpMethod(String method) {
        return method.equals("GET") || method.equals("POST") || method.equals("PUT") || 
               method.equals("DELETE") || method.equals("PATCH") || method.equals("HEAD") || 
               method.equals("OPTIONS");
    }
    
    private void addConstraintViolation(ConstraintValidatorContext context, String message) {
        context.disableDefaultConstraintViolation();
        context.buildConstraintViolationWithTemplate(message).addConstraintViolation();
    }
}