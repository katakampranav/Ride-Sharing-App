package com.officemate.modules.auth.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.redis.core.RedisHash;
import org.springframework.data.redis.core.TimeToLive;
import org.springframework.data.redis.core.index.Indexed;

import java.time.LocalDateTime;

/**
 * OTP Record stored in Redis with TTL (Time To Live).
 * Used for both mobile and email OTP verification.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@RedisHash("otp")
public class OTPRecord {

    /**
     * Redis key format: "phone:+1234567890" or "email:user@company.com"
     */
    @Id
    private String key;

    /**
     * Hashed OTP for security (never store plain text OTP)
     */
    private String otpHash;

    /**
     * Number of verification attempts
     */
    private Integer attempts;

    /**
     * OTP creation timestamp
     */
    private LocalDateTime createdAt;

    /**
     * OTP expiration timestamp
     */
    private LocalDateTime expiresAt;

    /**
     * Whether OTP has been verified
     */
    private Boolean verified;

    /**
     * OTP type: MOBILE or EMAIL
     */
    @Indexed
    private OTPType type;

    /**
     * TTL in seconds (5 minutes = 300 seconds)
     * Redis will automatically delete this record after TTL expires
     */
    @TimeToLive
    private Long timeToLive;

    /**
     * OTP Type enum
     */
    public enum OTPType {
        MOBILE,
        EMAIL
    }

    /**
     * Increment attempt counter
     */
    public void incrementAttempts() {
        this.attempts = (this.attempts == null ? 0 : this.attempts) + 1;
    }

    /**
     * Check if OTP has expired
     */
    public boolean isExpired() {
        return LocalDateTime.now().isAfter(this.expiresAt);
    }

    /**
     * Check if max attempts reached
     */
    public boolean isMaxAttemptsReached(int maxAttempts) {
        return this.attempts != null && this.attempts >= maxAttempts;
    }
}
