package com.officemate.modules.auth.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.redis.core.RedisHash;
import org.springframework.data.redis.core.TimeToLive;
import org.springframework.data.redis.core.index.Indexed;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Redis entity representing an active user session.
 * Stored in Redis with TTL for automatic expiration.
 */
@RedisHash("user_sessions")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserSession implements Serializable {
    
    private static final long serialVersionUID = 1L;
    
    /**
     * Unique session identifier (Redis key)
     */
    @Id
    private String sessionId;
    
    /**
     * User ID associated with this session
     */
    @Indexed
    private String userId;
    
    /**
     * Device type (IOS, ANDROID, WEB)
     */
    private String deviceType;
    
    /**
     * Unique device identifier
     */
    private String deviceId;
    
    /**
     * Application version
     */
    private String appVersion;
    
    /**
     * List of permissions granted in this session
     */
    private List<String> permissions;
    
    /**
     * Timestamp when session was created
     */
    private LocalDateTime createdAt;
    
    /**
     * Timestamp of last access
     */
    private LocalDateTime lastAccessAt;
    
    /**
     * Timestamp when session expires
     */
    private LocalDateTime expiresAt;
    
    /**
     * Refresh token associated with this session
     */
    private String refreshToken;
    
    /**
     * Flag indicating if mobile is verified
     */
    private boolean mobileVerified;
    
    /**
     * Flag indicating if email is verified
     */
    private boolean emailVerified;
    
    /**
     * Time to live in seconds (for Redis TTL)
     */
    @TimeToLive
    private Long ttl;
    
    /**
     * Updates the last access timestamp
     */
    public void updateLastAccess() {
        this.lastAccessAt = LocalDateTime.now();
    }
}
