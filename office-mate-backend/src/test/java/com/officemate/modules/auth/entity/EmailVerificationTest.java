package com.officemate.modules.auth.entity;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for EmailVerification entity
 */
class EmailVerificationTest {

    private EmailVerification emailVerification;
    private UUID userId;
    private String corporateEmail;
    private String otpHash;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        corporateEmail = "test@company.com";
        otpHash = "hashed_otp_value";
        
        emailVerification = EmailVerification.builder()
            .userId(userId)
            .corporateEmail(corporateEmail)
            .otpHash(otpHash)
            .build();
    }

    @Test
    void testEmailVerificationCreation() {
        assertNotNull(emailVerification);
        assertEquals(userId, emailVerification.getUserId());
        assertEquals(corporateEmail, emailVerification.getCorporateEmail());
        assertEquals(otpHash, emailVerification.getOtpHash());
        assertEquals(0, emailVerification.getAttempts());
        assertFalse(emailVerification.getVerified());
    }

    @Test
    void testIncrementAttempts() {
        assertEquals(0, emailVerification.getAttempts());
        
        emailVerification.incrementAttempts();
        assertEquals(1, emailVerification.getAttempts());
        
        emailVerification.incrementAttempts();
        assertEquals(2, emailVerification.getAttempts());
    }

    @Test
    void testMarkAsVerified() {
        assertFalse(emailVerification.getVerified());
        
        emailVerification.markAsVerified();
        assertTrue(emailVerification.getVerified());
    }

    @Test
    void testIsExpired() {
        // Set expiry to past
        emailVerification.setExpiresAt(LocalDateTime.now().minusMinutes(1));
        assertTrue(emailVerification.isExpired());
        
        // Set expiry to future
        emailVerification.setExpiresAt(LocalDateTime.now().plusMinutes(5));
        assertFalse(emailVerification.isExpired());
    }

    @Test
    void testHasExceededAttempts() {
        int maxAttempts = 3;
        
        assertFalse(emailVerification.hasExceededAttempts(maxAttempts));
        
        emailVerification.incrementAttempts();
        emailVerification.incrementAttempts();
        emailVerification.incrementAttempts();
        
        assertTrue(emailVerification.hasExceededAttempts(maxAttempts));
    }

    @Test
    void testOnCreateSetsExpiryTimestamp() {
        EmailVerification newVerification = EmailVerification.builder()
            .userId(userId)
            .corporateEmail(corporateEmail)
            .otpHash(otpHash)
            .build();
        
        // Simulate @PrePersist
        newVerification.onCreate();
        
        assertNotNull(newVerification.getExpiresAt());
        assertTrue(newVerification.getExpiresAt().isAfter(LocalDateTime.now().plusMinutes(9)));
        assertTrue(newVerification.getExpiresAt().isBefore(LocalDateTime.now().plusMinutes(11)));
    }
}
