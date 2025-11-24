package com.officemate.shared.exception;

/**
 * Custom exception for safety-related errors.
 * Thrown when there are issues with emergency contacts, family sharing, or SOS features.
 */
public class SafetyException extends RuntimeException {

    /**
     * Constructs a new SafetyException with the specified detail message.
     *
     * @param message the detail message
     */
    public SafetyException(String message) {
        super(message);
    }

    /**
     * Constructs a new SafetyException with the specified detail message and cause.
     *
     * @param message the detail message
     * @param cause the cause of the exception
     */
    public SafetyException(String message, Throwable cause) {
        super(message, cause);
    }
}
