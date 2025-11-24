package com.officemate.config.security;

import com.officemate.shared.service.SecurityEventService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.concurrent.TimeUnit;

/**
 * Enhanced service for implementing rate limiting and abuse prevention using Redis.
 * Tracks request counts per user/IP and enforces rate limits with account lockout capabilities.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class RateLimitingService {

    private final RedisTemplate<String, String> redisTemplate;
    private final SecurityEventService securityEventService;

    private static final String RATE_LIMIT_PREFIX = "rate_limit:";
    private static final String OTP_LIMIT_PREFIX = "otp_limit:";
    private static final String LOGIN_LIMIT_PREFIX = "login_limit:";
    private static final String ACCOUNT_LOCKOUT_PREFIX = "account_lockout:";
    private static final String SUSPICIOUS_ACTIVITY_PREFIX = "suspicious:";
    private static final String FAILED_ATTEMPTS_PREFIX = "failed_attempts:";
    
    @Value("${app.security.lockout.max-failed-attempts:5}")
    private int maxFailedAttempts;
    
    @Value("${app.security.lockout.duration-minutes:30}")
    private int lockoutDurationMinutes;
    
    @Value("${app.security.suspicious.threshold:10}")
    private int suspiciousActivityThreshold;

    /**
     * Check if a request is allowed based on rate limiting rules.
     *
     * @param key the unique identifier (userId, IP, etc.)
     * @param maxRequests maximum number of requests allowed
     * @param duration time window for rate limiting
     * @return true if request is allowed, false if rate limit exceeded
     */
    public boolean isAllowed(String key, int maxRequests, Duration duration) {
        String redisKey = RATE_LIMIT_PREFIX + key;
        
        try {
            // Increment counter
            Long currentCount = redisTemplate.opsForValue().increment(redisKey);
            
            if (currentCount == null) {
                return false;
            }
            
            // Set expiry on first request
            if (currentCount == 1) {
                redisTemplate.expire(redisKey, duration.getSeconds(), TimeUnit.SECONDS);
            }
            
            // Check if limit exceeded
            boolean allowed = currentCount <= maxRequests;
            
            if (!allowed) {
                log.warn("Rate limit exceeded for key: {} (count: {}, max: {})", 
                        key, currentCount, maxRequests);
            }
            
            return allowed;
        } catch (Exception e) {
            log.error("Error checking rate limit for key: {}", key, e);
            // Fail open - allow request if Redis is unavailable
            return true;
        }
    }

    /**
     * Check if OTP request is allowed for a phone number.
     *
     * @param phoneNumber the phone number requesting OTP
     * @param maxRequests maximum OTP requests allowed per hour
     * @return true if OTP request is allowed
     */
    public boolean isOtpRequestAllowed(String phoneNumber, int maxRequests) {
        String key = OTP_LIMIT_PREFIX + phoneNumber;
        return isAllowed(key, maxRequests, Duration.ofHours(1));
    }

    /**
     * Check if login attempt is allowed for a user.
     *
     * @param identifier user identifier (phone number or email)
     * @param maxAttempts maximum login attempts allowed per hour
     * @return true if login attempt is allowed
     */
    public boolean isLoginAttemptAllowed(String identifier, int maxAttempts) {
        String key = LOGIN_LIMIT_PREFIX + identifier;
        return isAllowed(key, maxAttempts, Duration.ofHours(1));
    }

    /**
     * Check if general API request is allowed for a user.
     *
     * @param userId the user ID
     * @param maxRequests maximum requests allowed per minute
     * @return true if request is allowed
     */
    public boolean isApiRequestAllowed(String userId, int maxRequests) {
        String key = "api:" + userId;
        return isAllowed(key, maxRequests, Duration.ofMinutes(1));
    }

    /**
     * Reset rate limit for a specific key.
     *
     * @param key the rate limit key to reset
     */
    public void resetRateLimit(String key) {
        try {
            String redisKey = RATE_LIMIT_PREFIX + key;
            redisTemplate.delete(redisKey);
            log.info("Rate limit reset for key: {}", key);
        } catch (Exception e) {
            log.error("Error resetting rate limit for key: {}", key, e);
        }
    }

    /**
     * Get remaining requests for a key.
     *
     * @param key the unique identifier
     * @param maxRequests maximum number of requests allowed
     * @return number of remaining requests
     */
    public int getRemainingRequests(String key, int maxRequests) {
        try {
            String redisKey = RATE_LIMIT_PREFIX + key;
            String countStr = redisTemplate.opsForValue().get(redisKey);
            
            if (countStr == null) {
                return maxRequests;
            }
            
            int currentCount = Integer.parseInt(countStr);
            return Math.max(0, maxRequests - currentCount);
        } catch (Exception e) {
            log.error("Error getting remaining requests for key: {}", key, e);
            return maxRequests;
        }
    }

    /**
     * Get time until rate limit resets.
     *
     * @param key the unique identifier
     * @return seconds until reset, or -1 if no limit set
     */
    public long getTimeUntilReset(String key) {
        try {
            String redisKey = RATE_LIMIT_PREFIX + key;
            Long ttl = redisTemplate.getExpire(redisKey, TimeUnit.SECONDS);
            return ttl != null ? ttl : -1;
        } catch (Exception e) {
            log.error("Error getting TTL for key: {}", key, e);
            return -1;
        }
    }

    /**
     * Check if an account is currently locked out.
     *
     * @param identifier user identifier (phone number or email)
     * @return true if account is locked
     */
    public boolean isAccountLocked(String identifier) {
        try {
            String lockoutKey = ACCOUNT_LOCKOUT_PREFIX + identifier;
            String lockoutValue = redisTemplate.opsForValue().get(lockoutKey);
            return lockoutValue != null;
        } catch (Exception e) {
            log.error("Error checking account lockout for identifier: {}", identifier, e);
            return false;
        }
    }

    /**
     * Lock an account due to suspicious activity or too many failed attempts.
     *
     * @param identifier user identifier
     * @param reason reason for lockout
     * @param userId user ID if available
     */
    public void lockAccount(String identifier, String reason, String userId) {
        try {
            String lockoutKey = ACCOUNT_LOCKOUT_PREFIX + identifier;
            LocalDateTime lockoutEnd = LocalDateTime.now().plusMinutes(lockoutDurationMinutes);
            
            redisTemplate.opsForValue().set(lockoutKey, lockoutEnd.toString(), 
                    Duration.ofMinutes(lockoutDurationMinutes));
            
            // Log security event
            try {
                securityEventService.logAccountLockout(
                    userId != null ? java.util.UUID.fromString(userId) : null, 
                    identifier, reason);
            } catch (IllegalArgumentException e) {
                // Handle invalid UUID format gracefully
                securityEventService.logAccountLockout(null, identifier, reason);
            }
            
            log.warn("Account locked for {}: {} (Duration: {} minutes)", 
                    identifier, reason, lockoutDurationMinutes);
            
        } catch (Exception e) {
            log.error("Error locking account for identifier: {}", identifier, e);
        }
    }

    /**
     * Unlock an account manually (admin function).
     *
     * @param identifier user identifier
     */
    public void unlockAccount(String identifier) {
        try {
            String lockoutKey = ACCOUNT_LOCKOUT_PREFIX + identifier;
            redisTemplate.delete(lockoutKey);
            
            log.info("Account unlocked for identifier: {}", identifier);
            
        } catch (Exception e) {
            log.error("Error unlocking account for identifier: {}", identifier, e);
        }
    }

    /**
     * Record a failed authentication attempt and check for lockout conditions.
     *
     * @param identifier user identifier
     * @param attemptType type of attempt (LOGIN, OTP_VERIFICATION, etc.)
     * @param userId user ID if available
     * @return true if account should be locked
     */
    public boolean recordFailedAttempt(String identifier, String attemptType, String userId) {
        try {
            String failedKey = FAILED_ATTEMPTS_PREFIX + identifier;
            Long failedCount = redisTemplate.opsForValue().increment(failedKey);
            
            if (failedCount == null) {
                return false;
            }
            
            // Set expiry on first failed attempt (1 hour window)
            if (failedCount == 1) {
                redisTemplate.expire(failedKey, Duration.ofHours(1));
            }
            
            // Log security event for failed attempt
            securityEventService.logFailedLogin(identifier, 
                    attemptType + " failed attempt #" + failedCount, 
                    "attemptType=" + attemptType);
            
            // Check if lockout threshold reached
            if (failedCount >= maxFailedAttempts) {
                lockAccount(identifier, 
                        "Too many failed " + attemptType + " attempts (" + failedCount + ")", 
                        userId);
                return true;
            }
            
            log.warn("Failed {} attempt #{} for identifier: {}", attemptType, failedCount, identifier);
            return false;
            
        } catch (Exception e) {
            log.error("Error recording failed attempt for identifier: {}", identifier, e);
            return false;
        }
    }

    /**
     * Clear failed attempts counter after successful authentication.
     *
     * @param identifier user identifier
     */
    public void clearFailedAttempts(String identifier) {
        try {
            String failedKey = FAILED_ATTEMPTS_PREFIX + identifier;
            redisTemplate.delete(failedKey);
            
            log.debug("Cleared failed attempts for identifier: {}", identifier);
            
        } catch (Exception e) {
            log.error("Error clearing failed attempts for identifier: {}", identifier, e);
        }
    }

    /**
     * Track suspicious activity patterns.
     *
     * @param identifier user identifier or IP address
     * @param activityType type of suspicious activity
     * @param userId user ID if available
     * @return true if activity threshold exceeded
     */
    public boolean trackSuspiciousActivity(String identifier, String activityType, String userId) {
        try {
            String suspiciousKey = SUSPICIOUS_ACTIVITY_PREFIX + identifier + ":" + activityType;
            Long activityCount = redisTemplate.opsForValue().increment(suspiciousKey);
            
            if (activityCount == null) {
                return false;
            }
            
            // Set expiry on first suspicious activity (24 hours window)
            if (activityCount == 1) {
                redisTemplate.expire(suspiciousKey, Duration.ofHours(24));
            }
            
            // Log suspicious activity
            try {
                securityEventService.logSuspiciousActivity(
                    userId != null ? java.util.UUID.fromString(userId) : null,
                    identifier.contains("@") ? null : identifier, // phone if not email
                    identifier.contains("@") ? identifier : null, // email if contains @
                    activityType,
                    "Suspicious activity count: " + activityCount,
                    activityCount >= suspiciousActivityThreshold ? "HIGH" : "MEDIUM"
                );
            } catch (IllegalArgumentException e) {
                // Handle invalid UUID format gracefully
                securityEventService.logSuspiciousActivity(
                    null,
                    identifier.contains("@") ? null : identifier,
                    identifier.contains("@") ? identifier : null,
                    activityType,
                    "Suspicious activity count: " + activityCount,
                    activityCount >= suspiciousActivityThreshold ? "HIGH" : "MEDIUM"
                );
            }
            
            // Check if threshold exceeded
            if (activityCount >= suspiciousActivityThreshold) {
                lockAccount(identifier, 
                        "Suspicious activity threshold exceeded: " + activityType + " (" + activityCount + ")", 
                        userId);
                return true;
            }
            
            log.warn("Suspicious activity #{} detected for {}: {}", activityCount, identifier, activityType);
            return false;
            
        } catch (Exception e) {
            log.error("Error tracking suspicious activity for identifier: {}", identifier, e);
            return false;
        }
    }

    /**
     * Check if CAPTCHA should be required based on risk factors.
     *
     * @param identifier user identifier
     * @param ipAddress client IP address
     * @return true if CAPTCHA should be required
     */
    public boolean shouldRequireCaptcha(String identifier, String ipAddress) {
        try {
            // Check failed attempts for user
            String failedKey = FAILED_ATTEMPTS_PREFIX + identifier;
            String failedCountStr = redisTemplate.opsForValue().get(failedKey);
            int failedCount = failedCountStr != null ? Integer.parseInt(failedCountStr) : 0;
            
            // Check failed attempts for IP
            String ipFailedKey = FAILED_ATTEMPTS_PREFIX + "ip:" + ipAddress;
            String ipFailedCountStr = redisTemplate.opsForValue().get(ipFailedKey);
            int ipFailedCount = ipFailedCountStr != null ? Integer.parseInt(ipFailedCountStr) : 0;
            
            // Require CAPTCHA if:
            // - User has 2+ failed attempts, OR
            // - IP has 3+ failed attempts, OR
            // - Account is approaching lockout threshold
            boolean requireCaptcha = failedCount >= 2 || ipFailedCount >= 3 || 
                                   failedCount >= (maxFailedAttempts - 2);
            
            if (requireCaptcha) {
                log.info("CAPTCHA required for identifier: {} (failed: {}, IP failed: {})", 
                        identifier, failedCount, ipFailedCount);
            }
            
            return requireCaptcha;
            
        } catch (Exception e) {
            log.error("Error checking CAPTCHA requirement for identifier: {}", identifier, e);
            // Fail safe - require CAPTCHA on error
            return true;
        }
    }

    /**
     * Record failed attempt for IP address.
     *
     * @param ipAddress client IP address
     * @param attemptType type of attempt
     */
    public void recordIpFailedAttempt(String ipAddress, String attemptType) {
        try {
            String ipFailedKey = FAILED_ATTEMPTS_PREFIX + "ip:" + ipAddress;
            Long failedCount = redisTemplate.opsForValue().increment(ipFailedKey);
            
            if (failedCount == null) {
                return;
            }
            
            // Set expiry on first failed attempt (1 hour window)
            if (failedCount == 1) {
                redisTemplate.expire(ipFailedKey, Duration.ofHours(1));
            }
            
            // Log rate limit violation for high IP failure count
            if (failedCount >= 5) {
                securityEventService.logRateLimitViolation(
                        ipAddress, attemptType, 
                        "High failure count from IP: " + failedCount);
            }
            
            log.debug("IP failed {} attempt #{} for: {}", attemptType, failedCount, ipAddress);
            
        } catch (Exception e) {
            log.error("Error recording IP failed attempt for: {}", ipAddress, e);
        }
    }

    /**
     * Get lockout information for an identifier.
     *
     * @param identifier user identifier
     * @return lockout end time or null if not locked
     */
    public LocalDateTime getLockoutEndTime(String identifier) {
        try {
            String lockoutKey = ACCOUNT_LOCKOUT_PREFIX + identifier;
            String lockoutValue = redisTemplate.opsForValue().get(lockoutKey);
            
            if (lockoutValue != null) {
                return LocalDateTime.parse(lockoutValue);
            }
            
            return null;
            
        } catch (Exception e) {
            log.error("Error getting lockout end time for identifier: {}", identifier, e);
            return null;
        }
    }
}
