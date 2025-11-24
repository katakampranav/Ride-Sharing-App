package com.officemate.modules.auth.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.redis.core.RedisHash;
import org.springframework.data.redis.core.TimeToLive;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * Redis entity representing a revoked JWT token.
 * Stored in Redis with TTL matching the token's original expiration.
 * Used for token blacklisting to prevent use of revoked tokens.
 */
@RedisHash("revoked_tokens")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RevokedToken implements Serializable {
    
    private static final long serialVersionUID = 1L;
    
    /**
     * JWT token identifier (jti claim)
     */
    @Id
    private String tokenId;
    
    /**
     * User ID who owned this token
     */
    private String userId;
    
    /**
     * Timestamp when token was revoked
     */
    private LocalDateTime revokedAt;
    
    /**
     * Reason for revocation
     */
    private String reason;
    
    /**
     * Time to live in seconds (for Redis TTL)
     * Should match the token's remaining validity period
     */
    @TimeToLive
    private Long ttl;
}
