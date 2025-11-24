package com.officemate.shared.enums;

/**
 * Enum representing the status of a user account in the system.
 * Used for access control and account lifecycle management.
 */
public enum AccountStatus {
    /**
     * Account is active and fully verified (mobile and email)
     */
    ACTIVE,
    
    /**
     * Account is suspended due to policy violations or security concerns
     */
    SUSPENDED,
    
    /**
     * Account is pending corporate email verification
     */
    PENDING_EMAIL
}
