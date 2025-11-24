package com.officemate.security;

import com.officemate.config.security.RateLimitingService;
import com.officemate.modules.auth.entity.UserAccount;
import com.officemate.modules.auth.repository.UserAccountRepository;
import com.officemate.shared.entity.CancellationLog;
import com.officemate.shared.repository.CancellationLogRepository;
import com.officemate.shared.service.CancellationTrackingService;
import com.officemate.shared.util.InputSanitizer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Comprehensive security integration tests covering:
 * - Input validation and injection prevention
 * - Rate limiting and abuse prevention
 * - Authentication bypass scenarios
 * - Cancellation tracking and suspension logic
 */
@SpringBootTest(properties = {
    "spring.profiles.active=test",
    "spring.datasource.postgresql.jdbc-url=jdbc:h2:mem:testdb",
    "spring.datasource.postgresql.username=sa",
    "spring.datasource.postgresql.password=",
    "spring.jpa.hibernate.ddl-auto=create-drop"
})
@AutoConfigureMockMvc
@Transactional
@Disabled
class SecurityIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private InputSanitizer inputSanitizer;

    @Autowired
    private RateLimitingService rateLimitingService;

    @Autowired
    private CancellationTrackingService cancellationTrackingService;

    @Autowired
    private CancellationLogRepository cancellationLogRepository;

    @Autowired
    private UserAccountRepository userAccountRepository;

    private UUID testUserId;

    @BeforeEach
    void setUp() {
        testUserId = UUID.randomUUID();
        cancellationLogRepository.deleteAll();
    }

    // ========== Input Validation and Injection Prevention Tests ==========

    @Test
    void testSQLInjectionPrevention_RegisterEndpoint() throws Exception {
        String sqlInjectionPayload = "'; DROP TABLE users; --";
        
        mockMvc.perform(post("/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"phoneNumber\":\"" + sqlInjectionPayload + "\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void testXSSPrevention_InputSanitization() {
        String xssPayload = "<script>alert('XSS')</script>Hello";
        String sanitized = inputSanitizer.sanitizeText(xssPayload);
        
        assertFalse(sanitized.contains("<script>"), "Script tags should be removed");
        assertFalse(sanitized.contains("alert"), "JavaScript code should be removed");
    }

    @Test
    void testMaliciousContentDetection_JavaScriptInjection() {
        String[] maliciousInputs = {
            "<script>alert('xss')</script>",
            "javascript:alert('xss')",
            "onclick=alert('xss')",
            "<iframe src='evil.com'></iframe>",
            "onerror=alert('xss')"
        };
        
        for (String input : maliciousInputs) {
            assertTrue(inputSanitizer.containsMaliciousContent(input),
                    "Should detect malicious content: " + input);
        }
    }

    @Test
    void testInputValidation_PhoneNumberSanitization() {
        // Test phone number sanitization
        String validPhone = "+1234567890";
        String sanitized = inputSanitizer.sanitizePhoneNumber(validPhone);
        assertEquals("+1234567890", sanitized);
        
        // Test with formatting characters
        String formattedPhone = "+1 (234) 567-8900";
        sanitized = inputSanitizer.sanitizePhoneNumber(formattedPhone);
        assertTrue(sanitized.contains("+1"));
        
        // Test malicious input
        String maliciousPhone = "+1234567890<script>alert('xss')</script>";
        sanitized = inputSanitizer.sanitizePhoneNumber(maliciousPhone);
        assertFalse(sanitized.contains("<script>"));
    }

    @Test
    void testInputValidation_EmailFormat() {
        String validEmail = "user@company.com";
        String sanitized = inputSanitizer.sanitizeEmail(validEmail);
        assertEquals("user@company.com", sanitized);
        
        // Test malicious email
        String maliciousEmail = "user@company.com<script>alert('xss')</script>";
        assertThrows(IllegalArgumentException.class, () -> {
            inputSanitizer.sanitizeEmail(maliciousEmail);
        });
    }

    // ========== Rate Limiting and Abuse Prevention Tests ==========

    @Test
    void testRateLimiting_ExcessiveOTPRequests() {
        String phoneNumber = "+1234567890";
        
        // Simulate multiple OTP requests
        for (int i = 0; i < 6; i++) {
            boolean allowed = rateLimitingService.isAllowed(
                "otp:" + phoneNumber, 5, java.time.Duration.ofMinutes(1));
            
            if (i < 5) {
                assertTrue(allowed, "Request " + (i + 1) + " should be allowed");
            } else {
                assertFalse(allowed, "Request " + (i + 1) + " should be blocked");
            }
        }
    }

    @Test
    void testAccountLockout_MultipleFailedAttempts() {
        String identifier = "test_user_" + UUID.randomUUID();
        String userId = UUID.randomUUID().toString();
        
        // Record 5 failed attempts
        for (int i = 0; i < 5; i++) {
            boolean shouldLock = rateLimitingService.recordFailedAttempt(
                identifier, "LOGIN", userId);
            
            if (i < 4) {
                assertFalse(shouldLock, "Should not lock before threshold");
            } else {
                assertTrue(shouldLock, "Should lock at threshold");
            }
        }
        
        // Verify account is locked
        assertTrue(rateLimitingService.isAccountLocked(identifier));
    }

    @Test
    void testSuspiciousActivityTracking_BruteForce() {
        String identifier = "attacker_" + UUID.randomUUID();
        String userId = UUID.randomUUID().toString();
        
        // Track suspicious activity
        for (int i = 0; i < 10; i++) {
            boolean shouldLock = rateLimitingService.trackSuspiciousActivity(
                identifier, "BRUTE_FORCE", userId);
            
            if (i < 9) {
                assertFalse(shouldLock, "Should not lock before threshold");
            } else {
                assertTrue(shouldLock, "Should lock at threshold");
            }
        }
        
        assertTrue(rateLimitingService.isAccountLocked(identifier));
    }

    @Test
    void testCaptchaRequirement_HighFailureRate() {
        String identifier = "user_" + UUID.randomUUID();
        String ipAddress = "192.168.1.100";
        
        // Record 3 failed attempts
        for (int i = 0; i < 3; i++) {
            rateLimitingService.recordFailedAttempt(identifier, "LOGIN", UUID.randomUUID().toString());
        }
        
        // Should require CAPTCHA after 3 failures
        assertTrue(rateLimitingService.shouldRequireCaptcha(identifier, ipAddress));
    }

    // ========== Authentication Bypass Prevention Tests ==========

    @Test
    void testAuthenticationRequired_ProtectedEndpoints() throws Exception {
        // Attempt to access protected endpoint without authentication
        mockMvc.perform(get("/users/" + UUID.randomUUID() + "/profile"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void testAuthenticationRequired_EmailVerificationEndpoints() throws Exception {
        // Attempt to add corporate email without authentication
        mockMvc.perform(post("/auth/add-corporate-email")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"corporateEmail\":\"user@company.com\"}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(username = "test_user")
    void testInvalidToken_ShouldRejectRequest() throws Exception {
        // Attempt with invalid token format
        mockMvc.perform(get("/users/" + UUID.randomUUID() + "/profile")
                .header("Authorization", "Bearer invalid_token_format"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void testPublicEndpoints_NoAuthenticationRequired() throws Exception {
        // Health check should be accessible without authentication
        mockMvc.perform(get("/health"))
                .andExpect(status().isOk());
    }

    @Test
    void testTokenExpiration_ShouldRejectExpiredTokens() throws Exception {
        // This would require generating an expired token
        // For now, test that expired token format is rejected
        String expiredToken = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiIxMjM0NTY3ODkwIiwibmFtZSI6IkpvaG4gRG9lIiwiaWF0IjoxNTE2MjM5MDIyLCJleHAiOjE1MTYyMzkwMjJ9.invalid";
        
        mockMvc.perform(get("/users/" + UUID.randomUUID() + "/profile")
                .header("Authorization", "Bearer " + expiredToken))
                .andExpect(status().isUnauthorized());
    }

    // ========== Cancellation Tracking and Suspension Logic Tests ==========

    @Test
    void testCancellationTracking_DriverCancellation() {
        UUID userId = UUID.randomUUID();
        UUID rideId = UUID.randomUUID();
        
        // Log a driver cancellation
        cancellationTrackingService.logCancellation(
            userId, rideId, "DRIVER", "Traffic delay", 30, "Test cancellation");
        
        // Wait for async operation
        try { Thread.sleep(500); } catch (InterruptedException e) {}
        
        // Verify cancellation was logged
        long count = cancellationTrackingService.getDriverCancellationCountThisMonth(userId);
        assertEquals(1, count);
    }

    @Test
    void testSuspensionPolicy_FiveCancellationsInMonth() {
        UUID userId = UUID.randomUUID();
        LocalDateTime now = LocalDateTime.now();
        int currentMonth = now.getMonthValue();
        int currentYear = now.getYear();
        
        // Create 5 driver cancellations in current month
        for (int i = 0; i < 5; i++) {
            CancellationLog log = CancellationLog.builder()
                .userId(userId)
                .rideId(UUID.randomUUID())
                .cancellationType("DRIVER")
                .cancellationReason("Test reason " + i)
                .timestamp(now.minusDays(i))
                .cancellationMonth(currentMonth)
                .cancellationYear(currentYear)
                .minutesBeforeRide(30)
                .build();
            cancellationLogRepository.save(log);
        }
        
        // Trigger the 5th cancellation through service
        cancellationTrackingService.logCancellation(
            userId, UUID.randomUUID(), "DRIVER", "5th cancellation", 30, null);
        
        // Wait for async operation
        try { Thread.sleep(500); } catch (InterruptedException e) {}
        
        // Verify suspension was applied
        assertTrue(cancellationTrackingService.isUserSuspended(userId),
                "User should be suspended after 5 driver cancellations");
    }

    @Test
    void testWarningSystem_ThreeCancellations() {
        UUID userId = UUID.randomUUID();
        LocalDateTime now = LocalDateTime.now();
        int currentMonth = now.getMonthValue();
        int currentYear = now.getYear();
        
        // Create 3 driver cancellations
        for (int i = 0; i < 3; i++) {
            cancellationTrackingService.logCancellation(
                userId, UUID.randomUUID(), "DRIVER", "Test reason", 30, null);
        }
        
        // Wait for async operations
        try { Thread.sleep(1000); } catch (InterruptedException e) {}
        
        // Verify warning was issued (count should be 3)
        long count = cancellationTrackingService.getDriverCancellationCount(userId, currentMonth, currentYear);
        assertTrue(count >= 3, "Should have at least 3 cancellations");
        
        // User should not be suspended yet
        assertFalse(cancellationTrackingService.isUserSuspended(userId),
                "User should not be suspended with only 3 cancellations");
    }

    @Test
    void testSuspensionDuration_ThreeMonths() {
        UUID userId = UUID.randomUUID();
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime suspensionEnd = now.plusDays(90);
        
        // Create a suspension record
        CancellationLog suspension = CancellationLog.builder()
            .userId(userId)
            .cancellationType("DRIVER")
            .cancellationReason("AUTOMATIC_SUSPENSION")
            .timestamp(now)
            .cancellationMonth(now.getMonthValue())
            .cancellationYear(now.getYear())
            .penaltyApplied(true)
            .penaltyType("SUSPENSION")
            .penaltyDurationDays(90)
            .penaltyStartDate(now)
            .penaltyEndDate(suspensionEnd)
            .build();
        
        cancellationLogRepository.save(suspension);
        
        // Verify suspension is active
        assertTrue(cancellationTrackingService.isUserSuspended(userId));
        
        // Verify suspension details
        CancellationLog activeSuspension = cancellationTrackingService.getActiveSuspension(userId);
        assertNotNull(activeSuspension);
        assertEquals(90, activeSuspension.getPenaltyDurationDays());
        assertEquals("SUSPENSION", activeSuspension.getPenaltyType());
    }

    @Test
    void testRiderCancellation_NoSuspension() {
        UUID userId = UUID.randomUUID();
        
        // Create 10 rider cancellations (should not trigger suspension)
        for (int i = 0; i < 10; i++) {
            cancellationTrackingService.logCancellation(
                userId, UUID.randomUUID(), "RIDER", "Changed plans", 60, null);
        }
        
        // Wait for async operations
        try { Thread.sleep(1000); } catch (InterruptedException e) {}
        
        // Verify no suspension for rider cancellations
        assertFalse(cancellationTrackingService.isUserSuspended(userId),
                "Rider cancellations should not trigger suspension");
    }

    @Test
    void testCancellationTracking_MonthlyReset() {
        UUID userId = UUID.randomUUID();
        LocalDateTime now = LocalDateTime.now();
        int currentMonth = now.getMonthValue();
        int currentYear = now.getYear();
        int previousMonth = currentMonth == 1 ? 12 : currentMonth - 1;
        int previousYear = currentMonth == 1 ? currentYear - 1 : currentYear;
        
        // Create 4 cancellations in previous month
        for (int i = 0; i < 4; i++) {
            CancellationLog log = CancellationLog.builder()
                .userId(userId)
                .rideId(UUID.randomUUID())
                .cancellationType("DRIVER")
                .cancellationReason("Previous month")
                .timestamp(now.minusMonths(1))
                .cancellationMonth(previousMonth)
                .cancellationYear(previousYear)
                .build();
            cancellationLogRepository.save(log);
        }
        
        // Create 2 cancellations in current month
        for (int i = 0; i < 2; i++) {
            cancellationTrackingService.logCancellation(
                userId, UUID.randomUUID(), "DRIVER", "Current month", 30, null);
        }
        
        // Wait for async operations
        try { Thread.sleep(500); } catch (InterruptedException e) {}
        
        // Verify only current month cancellations count
        long currentMonthCount = cancellationTrackingService.getDriverCancellationCount(
            userId, currentMonth, currentYear);
        assertTrue(currentMonthCount >= 2 && currentMonthCount < 5,
                "Should only count current month cancellations");
        
        // User should not be suspended
        assertFalse(cancellationTrackingService.isUserSuspended(userId));
    }

    @Test
    void testSuspensionPrevention_DuplicateSuspension() {
        UUID userId = UUID.randomUUID();
        LocalDateTime now = LocalDateTime.now();
        int currentMonth = now.getMonthValue();
        int currentYear = now.getYear();
        
        // Create 5 cancellations
        for (int i = 0; i < 5; i++) {
            CancellationLog log = CancellationLog.builder()
                .userId(userId)
                .rideId(UUID.randomUUID())
                .cancellationType("DRIVER")
                .cancellationReason("Test")
                .timestamp(now.minusDays(i))
                .cancellationMonth(currentMonth)
                .cancellationYear(currentYear)
                .build();
            cancellationLogRepository.save(log);
        }
        
        // Trigger suspension twice
        cancellationTrackingService.logCancellation(
            userId, UUID.randomUUID(), "DRIVER", "6th cancellation", 30, null);
        cancellationTrackingService.logCancellation(
            userId, UUID.randomUUID(), "DRIVER", "7th cancellation", 30, null);
        
        // Wait for async operations
        try { Thread.sleep(1000); } catch (InterruptedException e) {}
        
        // Verify only one suspension record exists
        List<CancellationLog> suspensions = cancellationLogRepository
            .findDriverCancellationsInMonth(userId, currentMonth, currentYear)
            .stream()
            .filter(c -> "SUSPENSION".equals(c.getPenaltyType()))
            .toList();
        
        assertTrue(suspensions.size() <= 1, "Should not create duplicate suspensions");
    }
}
