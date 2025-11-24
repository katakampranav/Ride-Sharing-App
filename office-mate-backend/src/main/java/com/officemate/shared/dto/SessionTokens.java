package com.officemate.shared.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * DTO containing JWT access and refresh tokens with session information.
 * Used for session creation and token refresh operations.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SessionTokens {
    
    /**
     * JWT access token for API authentication
     */
    private String accessToken;
    
    /**
     * JWT refresh token for obtaining new access tokens
     */
    private String refreshToken;
    
    /**
     * Timestamp when the access token expires
     */
    private LocalDateTime expiresAt;
    
    /**
     * Unique identifier for this session
     */
    private String sessionId;
    
    /**
     * User ID associated with this session
     */
    private String userId;
}
