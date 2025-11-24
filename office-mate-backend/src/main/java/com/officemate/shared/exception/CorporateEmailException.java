package com.officemate.shared.exception;

/**
 * Exception thrown when corporate email operations fail.
 * This includes validation errors, verification failures, and email update issues.
 */
public class CorporateEmailException extends RuntimeException {

    private final String errorCode;

    public CorporateEmailException(String message) {
        super(message);
        this.errorCode = "CORPORATE_EMAIL_ERROR";
    }

    public CorporateEmailException(String message, String errorCode) {
        super(message);
        this.errorCode = errorCode;
    }

    public CorporateEmailException(String message, Throwable cause) {
        super(message, cause);
        this.errorCode = "CORPORATE_EMAIL_ERROR";
    }

    public CorporateEmailException(String message, String errorCode, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
    }

    public String getErrorCode() {
        return errorCode;
    }
}
