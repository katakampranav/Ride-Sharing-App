package com.officemate.shared.exception;

/**
 * Exception thrown when users attempt to access profile features without proper verification.
 * This enforces the requirement that both mobile and email verification must be completed
 * before accessing ride-sharing features.
 */
public class ProfileAccessException extends RuntimeException {

    private final String errorCode;
    private final boolean mobileVerified;
    private final boolean emailVerified;

    public ProfileAccessException(String message, boolean mobileVerified, boolean emailVerified) {
        super(message);
        this.errorCode = "PROFILE_ACCESS_DENIED";
        this.mobileVerified = mobileVerified;
        this.emailVerified = emailVerified;
    }

    public ProfileAccessException(String message, String errorCode, boolean mobileVerified, boolean emailVerified) {
        super(message);
        this.errorCode = errorCode;
        this.mobileVerified = mobileVerified;
        this.emailVerified = emailVerified;
    }

    public ProfileAccessException(String message, boolean mobileVerified, boolean emailVerified, Throwable cause) {
        super(message, cause);
        this.errorCode = "PROFILE_ACCESS_DENIED";
        this.mobileVerified = mobileVerified;
        this.emailVerified = emailVerified;
    }

    public String getErrorCode() {
        return errorCode;
    }

    public boolean isMobileVerified() {
        return mobileVerified;
    }

    public boolean isEmailVerified() {
        return emailVerified;
    }
}
