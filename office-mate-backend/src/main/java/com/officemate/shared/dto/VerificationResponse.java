package com.officemate.shared.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Response DTO for email verification operations.
 * Contains verification status and OTP delivery information.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VerificationResponse {
    
    /**
     * Flag indicating if OTP was successfully sent
     */
    private boolean otpSent;
    
    /**
     * Timestamp when the OTP expires
     */
    private LocalDateTime expiresAt;
    
    /**
     * Masked email address where OTP was sent
     */
    private String maskedEmail;
    
    /**
     * Flag indicating if verification was successful
     */
    private boolean verified;
    
    /**
     * User ID associated with the verification
     */
    private String userId;
    
    /**
     * Audit log ID for tracking email changes (optional)
     */
    private String auditLogId;
}
