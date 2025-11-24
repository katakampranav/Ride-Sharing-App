package com.officemate.shared.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * DTO containing token validation results and user permissions.
 * Used to verify JWT tokens and extract user information.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TokenValidation {
    
    /**
     * Flag indicating if the token is valid
     */
    private boolean valid;
    
    /**
     * User ID extracted from the token
     */
    private String userId;
    
    /**
     * List of permissions granted to the user
     */
    private List<String> permissions;
    
    /**
     * Flag indicating if mobile is verified
     */
    private boolean mobileVerified;
    
    /**
     * Flag indicating if email is verified
     */
    private boolean emailVerified;
    
    /**
     * Session ID associated with this token
     */
    private String sessionId;
    
    /**
     * Error message if validation failed
     */
    private String errorMessage;
}
