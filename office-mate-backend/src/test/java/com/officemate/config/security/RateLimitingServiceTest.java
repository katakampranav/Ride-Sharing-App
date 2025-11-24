package com.officemate.config.security;

import com.officemate.shared.service.SecurityEventService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RateLimitingServiceTest {

    @Mock
    private RedisTemplate<String, String> redisTemplate;

    @Mock
    private SecurityEventService securityEventService;

    @Mock
    private ValueOperations<String, String> valueOperations;

    private RateLimitingService rateLimitingService;

    @BeforeEach
    void setUp() {
        rateLimitingService = new RateLimitingService(redisTemplate, securityEventService);
        
        // Set test configuration values
        ReflectionTestUtils.setField(rateLimitingService, "maxFailedAttempts", 5);
        ReflectionTestUtils.setField(rateLimitingService, "lockoutDurationMinutes", 30);
        ReflectionTestUtils.setField(rateLimitingService, "suspiciousActivityThreshold", 10);
    }

    @Test
    void testIsAllowed_WithinLimit_ShouldReturnTrue() {
        // Arrange
        String key = "test_user";
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.increment("rate_limit:" + key)).thenReturn(1L);

        // Act
        boolean result = rateLimitingService.isAllowed(key, 5, Duration.ofMinutes(1));

        // Assert
        assertTrue(result);
        verify(redisTemplate).expire("rate_limit:" + key, Duration.ofMinutes(1).getSeconds(), TimeUnit.SECONDS);
    }

    @Test
    void testIsAllowed_ExceedsLimit_ShouldReturnFalse() {
        // Arrange
        String key = "test_user";
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.increment("rate_limit:" + key)).thenReturn(6L);

        // Act
        boolean result = rateLimitingService.isAllowed(key, 5, Duration.ofMinutes(1));

        // Assert
        assertFalse(result);
    }

    @Test
    void testIsAccountLocked_WhenLocked_ShouldReturnTrue() {
        // Arrange
        String identifier = "test_user";
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get("account_lockout:" + identifier)).thenReturn("2024-01-01T12:00:00");

        // Act
        boolean result = rateLimitingService.isAccountLocked(identifier);

        // Assert
        assertTrue(result);
    }

    @Test
    void testIsAccountLocked_WhenNotLocked_ShouldReturnFalse() {
        // Arrange
        String identifier = "test_user";
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get("account_lockout:" + identifier)).thenReturn(null);

        // Act
        boolean result = rateLimitingService.isAccountLocked(identifier);

        // Assert
        assertFalse(result);
    }

    @Test
    void testRecordFailedAttempt_BelowThreshold_ShouldNotLock() {
        // Arrange
        String identifier = "test_user";
        String userId = "user123";
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.increment("failed_attempts:" + identifier)).thenReturn(3L);

        // Act
        boolean shouldLock = rateLimitingService.recordFailedAttempt(identifier, "LOGIN", userId);

        // Assert
        assertFalse(shouldLock);
        verify(securityEventService).logFailedLogin(eq(identifier), contains("LOGIN failed attempt #3"), any());
    }

    @Test
    void testRecordFailedAttempt_ExceedsThreshold_ShouldLock() {
        // Arrange
        String identifier = "test_user";
        String userId = "user123";
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.increment("failed_attempts:" + identifier)).thenReturn(5L);

        // Act
        boolean shouldLock = rateLimitingService.recordFailedAttempt(identifier, "LOGIN", userId);

        // Assert
        assertTrue(shouldLock);
        verify(valueOperations).set(eq("account_lockout:" + identifier), any(String.class), eq(Duration.ofMinutes(30)));
        verify(securityEventService).logAccountLockout(any(), eq(identifier), contains("Too many failed LOGIN attempts"));
    }

    @Test
    void testClearFailedAttempts_ShouldDeleteKey() {
        // Arrange
        String identifier = "test_user";

        // Act
        rateLimitingService.clearFailedAttempts(identifier);

        // Assert
        verify(redisTemplate).delete("failed_attempts:" + identifier);
    }

    @Test
    void testTrackSuspiciousActivity_BelowThreshold_ShouldNotLock() {
        // Arrange
        String identifier = "test_user";
        String userId = "user123";
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.increment("suspicious:" + identifier + ":BRUTE_FORCE")).thenReturn(5L);

        // Act
        boolean shouldLock = rateLimitingService.trackSuspiciousActivity(identifier, "BRUTE_FORCE", userId);

        // Assert
        assertFalse(shouldLock);
        verify(securityEventService).logSuspiciousActivity(any(), any(), any(), eq("BRUTE_FORCE"), 
                contains("Suspicious activity count: 5"), eq("MEDIUM"));
    }

    @Test
    void testTrackSuspiciousActivity_ExceedsThreshold_ShouldLock() {
        // Arrange
        String identifier = "test_user";
        String userId = "user123";
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.increment("suspicious:" + identifier + ":BRUTE_FORCE")).thenReturn(10L);

        // Act
        boolean shouldLock = rateLimitingService.trackSuspiciousActivity(identifier, "BRUTE_FORCE", userId);

        // Assert
        assertTrue(shouldLock);
        verify(valueOperations).set(eq("account_lockout:" + identifier), any(String.class), eq(Duration.ofMinutes(30)));
        verify(securityEventService).logSuspiciousActivity(any(), any(), any(), eq("BRUTE_FORCE"), 
                contains("Suspicious activity count: 10"), eq("HIGH"));
    }

    @Test
    void testShouldRequireCaptcha_HighFailedAttempts_ShouldReturnTrue() {
        // Arrange
        String identifier = "test_user";
        String ipAddress = "192.168.1.1";
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get("failed_attempts:" + identifier)).thenReturn("3");
        when(valueOperations.get("failed_attempts:ip:" + ipAddress)).thenReturn("1");

        // Act
        boolean result = rateLimitingService.shouldRequireCaptcha(identifier, ipAddress);

        // Assert
        assertTrue(result);
    }

    @Test
    void testShouldRequireCaptcha_LowFailedAttempts_ShouldReturnFalse() {
        // Arrange
        String identifier = "test_user";
        String ipAddress = "192.168.1.1";
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get("failed_attempts:" + identifier)).thenReturn("1");
        when(valueOperations.get("failed_attempts:ip:" + ipAddress)).thenReturn("1");

        // Act
        boolean result = rateLimitingService.shouldRequireCaptcha(identifier, ipAddress);

        // Assert
        assertFalse(result);
    }

    @Test
    void testUnlockAccount_ShouldDeleteLockoutKey() {
        // Arrange
        String identifier = "test_user";

        // Act
        rateLimitingService.unlockAccount(identifier);

        // Assert
        verify(redisTemplate).delete("account_lockout:" + identifier);
    }

    @Test
    void testGetLockoutEndTime_WhenLocked_ShouldReturnDateTime() {
        // Arrange
        String identifier = "test_user";
        String lockoutTime = "2024-01-01T12:30:00";
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get("account_lockout:" + identifier)).thenReturn(lockoutTime);

        // Act
        LocalDateTime result = rateLimitingService.getLockoutEndTime(identifier);

        // Assert
        assertNotNull(result);
        assertEquals(LocalDateTime.parse(lockoutTime), result);
    }

    @Test
    void testGetLockoutEndTime_WhenNotLocked_ShouldReturnNull() {
        // Arrange
        String identifier = "test_user";
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get("account_lockout:" + identifier)).thenReturn(null);

        // Act
        LocalDateTime result = rateLimitingService.getLockoutEndTime(identifier);

        // Assert
        assertNull(result);
    }

    @Test
    void testRecordIpFailedAttempt_ShouldIncrementAndSetExpiryOnFirstAttempt() {
        // Arrange
        String ipAddress = "192.168.1.1";
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.increment("failed_attempts:ip:" + ipAddress)).thenReturn(1L);

        // Act
        rateLimitingService.recordIpFailedAttempt(ipAddress, "LOGIN");

        // Assert
        verify(valueOperations).increment("failed_attempts:ip:" + ipAddress);
        verify(redisTemplate).expire("failed_attempts:ip:" + ipAddress, Duration.ofHours(1));
    }

    @Test
    void testRecordIpFailedAttempt_HighCount_ShouldLogViolation() {
        // Arrange
        String ipAddress = "192.168.1.1";
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.increment("failed_attempts:ip:" + ipAddress)).thenReturn(5L);

        // Act
        rateLimitingService.recordIpFailedAttempt(ipAddress, "LOGIN");

        // Assert
        verify(securityEventService).logRateLimitViolation(eq(ipAddress), eq("LOGIN"), 
                contains("High failure count from IP: 5"));
    }
}