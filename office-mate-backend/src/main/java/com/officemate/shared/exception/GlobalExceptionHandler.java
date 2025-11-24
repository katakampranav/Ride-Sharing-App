package com.officemate.shared.exception;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Global exception handler for the application.
 * Handles all custom exceptions and provides consistent error responses.
 * Enhanced with comprehensive validation error handling and input sanitization.
 */
@ControllerAdvice
public class GlobalExceptionHandler {
    
    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    /**
     * Handles validation errors from @Valid annotations on request bodies.
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidationException(
            MethodArgumentNotValidException ex, WebRequest request) {
        
        log.debug("Validation error occurred: {}", ex.getMessage());
        
        List<Map<String, String>> validationErrors = new ArrayList<>();
        
        for (FieldError error : ex.getBindingResult().getFieldErrors()) {
            Map<String, String> errorDetail = new HashMap<>();
            errorDetail.put("field", error.getField());
            errorDetail.put("message", error.getDefaultMessage());
            errorDetail.put("rejectedValue", String.valueOf(error.getRejectedValue()));
            validationErrors.add(errorDetail);
        }
        
        Map<String, Object> errorResponse = buildErrorResponse(
                "VALIDATION_ERROR",
                "Input validation failed",
                HttpStatus.BAD_REQUEST.value(),
                request
        );
        
        errorResponse.put("validationErrors", validationErrors);
        
        return new ResponseEntity<>(errorResponse, HttpStatus.BAD_REQUEST);
    }
    
    /**
     * Handles validation errors from @Validated annotations on method parameters.
     */
    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<Map<String, Object>> handleConstraintViolationException(
            ConstraintViolationException ex, WebRequest request) {
        
        log.debug("Constraint violation occurred: {}", ex.getMessage());
        
        List<Map<String, String>> validationErrors = new ArrayList<>();
        
        for (ConstraintViolation<?> violation : ex.getConstraintViolations()) {
            Map<String, String> errorDetail = new HashMap<>();
            errorDetail.put("field", violation.getPropertyPath().toString());
            errorDetail.put("message", violation.getMessage());
            errorDetail.put("rejectedValue", String.valueOf(violation.getInvalidValue()));
            validationErrors.add(errorDetail);
        }
        
        Map<String, Object> errorResponse = buildErrorResponse(
                "VALIDATION_ERROR",
                "Input validation failed",
                HttpStatus.BAD_REQUEST.value(),
                request
        );
        
        errorResponse.put("validationErrors", validationErrors);
        
        return new ResponseEntity<>(errorResponse, HttpStatus.BAD_REQUEST);
    }
    
    /**
     * Handles bind exceptions from form data validation.
     */
    @ExceptionHandler(BindException.class)
    public ResponseEntity<Map<String, Object>> handleBindException(
            BindException ex, WebRequest request) {
        
        log.debug("Bind exception occurred: {}", ex.getMessage());
        
        List<Map<String, String>> validationErrors = new ArrayList<>();
        
        for (FieldError error : ex.getFieldErrors()) {
            Map<String, String> errorDetail = new HashMap<>();
            errorDetail.put("field", error.getField());
            errorDetail.put("message", error.getDefaultMessage());
            errorDetail.put("rejectedValue", String.valueOf(error.getRejectedValue()));
            validationErrors.add(errorDetail);
        }
        
        Map<String, Object> errorResponse = buildErrorResponse(
                "VALIDATION_ERROR",
                "Input validation failed",
                HttpStatus.BAD_REQUEST.value(),
                request
        );
        
        errorResponse.put("validationErrors", validationErrors);
        
        return new ResponseEntity<>(errorResponse, HttpStatus.BAD_REQUEST);
    }
    
    /**
     * Handles type mismatch exceptions (e.g., invalid enum values).
     */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<Map<String, Object>> handleTypeMismatchException(
            MethodArgumentTypeMismatchException ex, WebRequest request) {
        
        log.debug("Type mismatch exception: {}", ex.getMessage());
        
        String message = String.format("Invalid value '%s' for parameter '%s'. Expected type: %s",
                ex.getValue(), ex.getName(), 
                ex.getRequiredType() != null ? ex.getRequiredType().getSimpleName() : "unknown");
        
        Map<String, Object> errorResponse = buildErrorResponse(
                "TYPE_MISMATCH_ERROR",
                message,
                HttpStatus.BAD_REQUEST.value(),
                request
        );
        
        return new ResponseEntity<>(errorResponse, HttpStatus.BAD_REQUEST);
    }
    
    /**
     * Handles malformed JSON or request body parsing errors.
     */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<Map<String, Object>> handleHttpMessageNotReadableException(
            HttpMessageNotReadableException ex, WebRequest request) {
        
        log.debug("HTTP message not readable: {}", ex.getMessage());
        
        String message = "Invalid request format. Please check your JSON syntax and data types.";
        
        // Check for specific JSON parsing errors
        if (ex.getMessage().contains("JSON parse error")) {
            message = "Invalid JSON format in request body";
        } else if (ex.getMessage().contains("Cannot deserialize")) {
            message = "Invalid data format in request body";
        }
        
        Map<String, Object> errorResponse = buildErrorResponse(
                "INVALID_REQUEST_FORMAT",
                message,
                HttpStatus.BAD_REQUEST.value(),
                request
        );
        
        return new ResponseEntity<>(errorResponse, HttpStatus.BAD_REQUEST);
    }
    
    /**
     * Handles IllegalArgumentException for input sanitization failures.
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, Object>> handleIllegalArgumentException(
            IllegalArgumentException ex, WebRequest request) {
        
        log.warn("Illegal argument exception: {}", ex.getMessage());
        
        Map<String, Object> errorResponse = buildErrorResponse(
                "INVALID_INPUT",
                ex.getMessage(),
                HttpStatus.BAD_REQUEST.value(),
                request
        );
        
        return new ResponseEntity<>(errorResponse, HttpStatus.BAD_REQUEST);
    }

    /**
     * Handles CorporateEmailException thrown during email operations.
     */
    @ExceptionHandler(CorporateEmailException.class)
    public ResponseEntity<Map<String, Object>> handleCorporateEmailException(
            CorporateEmailException ex, WebRequest request) {
        
        Map<String, Object> errorResponse = buildErrorResponse(
                ex.getErrorCode(),
                ex.getMessage(),
                HttpStatus.BAD_REQUEST.value(),
                request
        );
        
        return new ResponseEntity<>(errorResponse, HttpStatus.BAD_REQUEST);
    }

    /**
     * Handles WalletException thrown during wallet operations.
     */
    @ExceptionHandler(WalletException.class)
    public ResponseEntity<Map<String, Object>> handleWalletException(
            WalletException ex, WebRequest request) {
        
        Map<String, Object> errorResponse = buildErrorResponse(
                ex.getErrorCode(),
                ex.getMessage(),
                HttpStatus.BAD_REQUEST.value(),
                request
        );
        
        return new ResponseEntity<>(errorResponse, HttpStatus.BAD_REQUEST);
    }

    /**
     * Handles ProfileAccessException thrown when verification requirements are not met.
     */
    @ExceptionHandler(ProfileAccessException.class)
    public ResponseEntity<Map<String, Object>> handleProfileAccessException(
            ProfileAccessException ex, WebRequest request) {
        
        Map<String, Object> errorResponse = buildErrorResponse(
                ex.getErrorCode(),
                ex.getMessage(),
                HttpStatus.FORBIDDEN.value(),
                request
        );
        
        // Add verification status details
        Map<String, Boolean> verificationStatus = new HashMap<>();
        verificationStatus.put("mobileVerified", ex.isMobileVerified());
        verificationStatus.put("emailVerified", ex.isEmailVerified());
        errorResponse.put("verificationStatus", verificationStatus);
        
        return new ResponseEntity<>(errorResponse, HttpStatus.FORBIDDEN);
    }

    /**
     * Handles SafetyException thrown during safety-related operations.
     */
    @ExceptionHandler(SafetyException.class)
    public ResponseEntity<Map<String, Object>> handleSafetyException(
            SafetyException ex, WebRequest request) {
        
        Map<String, Object> errorResponse = buildErrorResponse(
                "SAFETY_ERROR",
                ex.getMessage(),
                HttpStatus.BAD_REQUEST.value(),
                request
        );
        
        return new ResponseEntity<>(errorResponse, HttpStatus.BAD_REQUEST);
    }

    /**
     * Handles generic exceptions not caught by specific handlers.
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleGlobalException(
            Exception ex, WebRequest request) {
        
        log.error("Unexpected error occurred", ex);
        
        Map<String, Object> errorResponse = buildErrorResponse(
                "INTERNAL_SERVER_ERROR",
                "An unexpected error occurred. Please try again later.",
                HttpStatus.INTERNAL_SERVER_ERROR.value(),
                request
        );
        
        return new ResponseEntity<>(errorResponse, HttpStatus.INTERNAL_SERVER_ERROR);
    }

    /**
     * Builds a standardized error response structure.
     */
    private Map<String, Object> buildErrorResponse(
            String errorCode, String message, int status, WebRequest request) {
        
        Map<String, Object> error = new HashMap<>();
        error.put("code", errorCode);
        error.put("message", message);
        error.put("timestamp", LocalDateTime.now());
        error.put("requestId", UUID.randomUUID().toString());
        error.put("path", request.getDescription(false).replace("uri=", ""));
        
        Map<String, Object> response = new HashMap<>();
        response.put("error", error);
        response.put("status", status);
        
        return response;
    }
}
