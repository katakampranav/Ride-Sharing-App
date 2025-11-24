package com.officemate.shared.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Response DTO for user registration operations.
 * Contains user ID and OTP delivery status.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RegistrationResponse {
    
    /**
     * Unique identifier for the newly registered user
     */
    private String userId;
    
    /**
     * Flag indicating if OTP was successfully sent
     */
    private boolean otpSent;
    
    /**
     * Timestamp when the OTP expires
     */
    private LocalDateTime expiresAt;
    
    /**
     * Masked phone number where OTP was sent
     */
    private String maskedPhoneNumber;
}
