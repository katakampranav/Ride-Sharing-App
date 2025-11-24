package com.officemate.shared.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Response DTO for authentication operations including login and registration.
 * Contains JWT tokens and user verification status.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuthResponse {
    
    /**
     * JWT access token for API authentication
     */
    private String accessToken;
    
    /**
     * JWT refresh token for obtaining new access tokens
     */
    private String refreshToken;
    
    /**
     * Unique identifier for the authenticated user
     */
    private String userId;
    
    /**
     * Flag indicating if the user's mobile number has been verified
     */
    private boolean mobileVerified;
    
    /**
     * Flag indicating if the user's corporate email has been verified
     */
    private boolean emailVerified;
    
    /**
     * Flag indicating if the user has completed their profile setup
     */
    private boolean profileComplete;
    
    /**
     * Timestamp when the access token expires
     */
    private LocalDateTime expiresAt;
}
