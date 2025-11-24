package com.officemate.shared.service;

import com.officemate.shared.entity.SecurityEventLog;
import com.officemate.shared.repository.SecurityEventLogRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit tests for SecurityEventService
 */
@ExtendWith(MockitoExtension.class)
class SecurityEventServiceTest {

    @Mock
    private SecurityEventLogRepository securityEventLogRepository;

    @InjectMocks
    private SecurityEventService securityEventService;

    private UUID testUserId;
    private String testPhoneNumber;

    @BeforeEach
    void setUp() {
        testUserId = UUID.randomUUID();
        testPhoneNumber = "+1234567890";
    }

    @Test
    void testLogFailedLogin() {
        // Given
        String reason = "Invalid OTP";
        String additionalData = "attempt=3";

        // When
        securityEventService.logFailedLogin(testPhoneNumber, reason, additionalData);

        // Then
        ArgumentCaptor<SecurityEventLog> eventCaptor = ArgumentCaptor.forClass(SecurityEventLog.class);
        verify(securityEventLogRepository, timeout(1000)).save(eventCaptor.capture());

        SecurityEventLog capturedEvent = eventCaptor.getValue();
        assertNull(capturedEvent.getUserId()); // No user ID for failed login
        assertEquals(testPhoneNumber, capturedEvent.getPhoneNumber());
        assertEquals("LOGIN_FAILURE", capturedEvent.getEventType());
        assertEquals("Failed login attempt: " + reason, capturedEvent.getEventDescription());
        assertEquals("MEDIUM", capturedEvent.getSeverity());
        assertEquals(additionalData, capturedEvent.getAdditionalData());
        assertNotNull(capturedEvent.getTimestamp());
    }

    @Test
    void testLogSuccessfulLogin() {
        // When
        securityEventService.logSuccessfulLogin(testUserId, testPhoneNumber);

        // Then
        ArgumentCaptor<SecurityEventLog> eventCaptor = ArgumentCaptor.forClass(SecurityEventLog.class);
        verify(securityEventLogRepository, timeout(1000)).save(eventCaptor.capture());

        SecurityEventLog capturedEvent = eventCaptor.getValue();
        assertEquals(testUserId, capturedEvent.getUserId());
        assertEquals(testPhoneNumber, capturedEvent.getPhoneNumber());
        assertEquals("LOGIN_SUCCESS", capturedEvent.getEventType());
        assertEquals("Successful login", capturedEvent.getEventDescription());
        assertEquals("LOW", capturedEvent.getSeverity());
        assertNotNull(capturedEvent.getTimestamp());
    }

    @Test
    void testLogFailedOtpVerification() {
        // Given
        String otpType = "MOBILE";
        String reason = "Invalid OTP code";

        // When
        securityEventService.logFailedOtpVerification(testUserId, testPhoneNumber, otpType, reason);

        // Then
        ArgumentCaptor<SecurityEventLog> eventCaptor = ArgumentCaptor.forClass(SecurityEventLog.class);
        verify(securityEventLogRepository, timeout(1000)).save(eventCaptor.capture());

        SecurityEventLog capturedEvent = eventCaptor.getValue();
        assertEquals(testUserId, capturedEvent.getUserId());
        assertEquals(testPhoneNumber, capturedEvent.getPhoneNumber());
        assertEquals("OTP_FAILURE", capturedEvent.getEventType());
        assertEquals("Failed OTP verification (" + otpType + "): " + reason, capturedEvent.getEventDescription());
        assertEquals("MEDIUM", capturedEvent.getSeverity());
        assertNotNull(capturedEvent.getTimestamp());
    }

    @Test
    void testLogAccountLockout() {
        // Given
        String reason = "Too many failed attempts";

        // When
        securityEventService.logAccountLockout(testUserId, testPhoneNumber, reason);

        // Then
        ArgumentCaptor<SecurityEventLog> eventCaptor = ArgumentCaptor.forClass(SecurityEventLog.class);
        verify(securityEventLogRepository, timeout(1000)).save(eventCaptor.capture());

        SecurityEventLog capturedEvent = eventCaptor.getValue();
        assertEquals(testUserId, capturedEvent.getUserId());
        assertEquals(testPhoneNumber, capturedEvent.getPhoneNumber());
        assertEquals("ACCOUNT_LOCKED", capturedEvent.getEventType());
        assertEquals("Account locked: " + reason, capturedEvent.getEventDescription());
        assertEquals("HIGH", capturedEvent.getSeverity());
        assertNotNull(capturedEvent.getTimestamp());
    }

    @Test
    void testLogSuspiciousActivity() {
        // Given
        String corporateEmail = "test@company.com";
        String activityType = "MULTIPLE_LOGIN_ATTEMPTS";
        String description = "Multiple failed login attempts from different IPs";
        String severity = "HIGH";

        // When
        securityEventService.logSuspiciousActivity(testUserId, testPhoneNumber, corporateEmail, 
                activityType, description, severity);

        // Then
        ArgumentCaptor<SecurityEventLog> eventCaptor = ArgumentCaptor.forClass(SecurityEventLog.class);
        verify(securityEventLogRepository, timeout(1000)).save(eventCaptor.capture());

        SecurityEventLog capturedEvent = eventCaptor.getValue();
        assertEquals(testUserId, capturedEvent.getUserId());
        assertEquals(testPhoneNumber, capturedEvent.getPhoneNumber());
        assertEquals(corporateEmail, capturedEvent.getCorporateEmail());
        assertEquals("SUSPICIOUS_ACTIVITY", capturedEvent.getEventType());
        assertEquals(activityType + ": " + description, capturedEvent.getEventDescription());
        assertEquals(severity, capturedEvent.getSeverity());
        assertNotNull(capturedEvent.getTimestamp());
    }

    @Test
    void testLogRateLimitViolation() {
        // Given
        String endpoint = "/auth/login";
        String reason = "Too many requests";

        // When
        securityEventService.logRateLimitViolation(testPhoneNumber, endpoint, reason);

        // Then
        ArgumentCaptor<SecurityEventLog> eventCaptor = ArgumentCaptor.forClass(SecurityEventLog.class);
        verify(securityEventLogRepository, timeout(1000)).save(eventCaptor.capture());

        SecurityEventLog capturedEvent = eventCaptor.getValue();
        assertNull(capturedEvent.getUserId());
        assertEquals(testPhoneNumber, capturedEvent.getPhoneNumber());
        assertEquals("RATE_LIMIT_VIOLATION", capturedEvent.getEventType());
        assertEquals("Rate limit exceeded for " + endpoint + ": " + reason, capturedEvent.getEventDescription());
        assertEquals("MEDIUM", capturedEvent.getSeverity());
        assertNotNull(capturedEvent.getTimestamp());
    }

    @Test
    void testSecurityEventLoggingHandlesException() {
        // Given
        when(securityEventLogRepository.save(any(SecurityEventLog.class))).thenThrow(new RuntimeException("Database error"));

        // When & Then - should not throw exception
        assertDoesNotThrow(() -> {
            securityEventService.logFailedLogin(testPhoneNumber, "Test failure", null);
        });

        // Verify that save was attempted
        verify(securityEventLogRepository, timeout(1000)).save(any(SecurityEventLog.class));
    }
}