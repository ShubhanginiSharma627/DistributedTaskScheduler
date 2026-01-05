package com.taskscheduler.dto.validation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

/**
 * Custom validation annotation for task payloads.
 * Validates that the payload is valid JSON and contains required fields for the task type.
 */
@Documented
@Constraint(validatedBy = TaskPayloadValidator.class)
@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface ValidTaskPayload {
    String message() default "Invalid task payload for the specified task type";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}