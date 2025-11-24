package com.officemate.modules.auth.service;

import com.officemate.modules.auth.entity.EmailChangeAuditLog;
import com.officemate.modules.auth.entity.EmailVerification;
import com.officemate.modules.auth.entity.UserAccount;
import com.officemate.modules.auth.repository.EmailChangeAuditLogRepository;
import com.officemate.modules.auth.repository.EmailVerificationRepository;
import com.officemate.modules.auth.repository.UserAccountRepository;
import com.officemate.shared.dto.VerificationResponse;
import com.officemate.shared.enums.AccountStatus;
import com.officemate.shared.exception.CorporateEmailException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for EmailVerificationService.
 * Tests email validation, OTP verification workflows, and one email per account enforcement.
 */
@ExtendWith(MockitoExtension.class)
class EmailVerificationServiceTest {

    @Mock
    private EmailVerificationRepository emailVerificationRepository;

    @Mock
    private UserAccountRepository userAccountRepository;

    @Mock
    private EmailChangeAuditLogRepository emailChangeAuditLogRepository;

    @Mock
    private OTPService otpService;

    @InjectMocks
    private EmailVerificationService emailVerificationService;

    private UUID testUserId;
    private UserAccount testUserAccount;
    private String testEmail;
    private String testPhoneNumber;

    @BeforeEach
    void setUp() {
        testUserId = UUID.randomUUID();
        testEmail = "user@company.com";
        testPhoneNumber = "+1234567890";

        testUserAccount = UserAccount.builder()
                .userId(testUserId)
                .phoneNumber(testPhoneNumber)
                .phoneVerified(true)
                .accountStatus(AccountStatus.ACTIVE)
                .build();

        // Set configuration values
        ReflectionTestUtils.setField(emailVerificationService, "otpLength", 6);
        ReflectionTestUtils.setField(emailVerificationService, "otpExpirationMinutes", 10);
        ReflectionTestUtils.setField(emailVerificationService, "maxAttempts", 3);
    }

    // ========== Email Validation Tests ==========

    @Test
    void testSendEmailOTP_ValidEmail_Success() {
        // Arrange
        when(userAccountRepository.findById(testUserId)).thenReturn(Optional.of(testUserAccount));
        when(userAccountRepository.existsByCorporateEmail(testEmail)).thenReturn(false);
        when(emailVerificationRepository.findActiveVerificationByUserId(eq(testUserId), any(LocalDateTime.class)))
                .thenReturn(Optional.empty());
        when(emailVerificationRepository.save(any(EmailVerification.class)))
                .thenAnswer(invocation -> {
                    EmailVerification ev = invocation.getArgument(0);
                    // Simulate @PrePersist
                    if (ev.getExpiresAt() == null) {
                        ev.setExpiresAt(LocalDateTime.now().plusMinutes(10));
                    }
                    return ev;
                });

        // Act
        VerificationResponse response = emailVerificationService.sendEmailOTP(testUserId, testEmail);

        // Assert
        assertNotNull(response);
        assertTrue(response.isOtpSent());
        assertNotNull(response.getExpiresAt());
        assertNotNull(response.getMaskedEmail());
        assertFalse(response.isVerified());
        verify(emailVerificationRepository, times(1)).save(any(EmailVerification.class));
    }

    @Test
    void testSendEmailOTP_InvalidEmailFormat() {
        // Arrange
        String invalidEmail = "not-an-email";

        // Act & Assert
        CorporateEmailException exception = assertThrows(
                CorporateEmailException.class,
                () -> emailVerificationService.sendEmailOTP(testUserId, invalidEmail)
        );

        assertEquals("INVALID_EMAIL_FORMAT", exception.getErrorCode());
        verify(emailVerificationRepository, never()).save(any());
    }

    @Test
    void testSendEmailOTP_UserNotFound() {
        // Arrange
        when(userAccountRepository.findById(testUserId)).thenReturn(Optional.empty());

        // Act & Assert
        CorporateEmailException exception = assertThrows(
                CorporateEmailException.class,
                () -> emailVerificationService.sendEmailOTP(testUserId, testEmail)
        );

        assertEquals("USER_NOT_FOUND", exception.getErrorCode());
        verify(emailVerificationRepository, never()).save(any());
    }

    @Test
    void testSendEmailOTP_EmailAlreadyExists() {
        // Arrange
        when(userAccountRepository.findById(testUserId)).thenReturn(Optional.of(testUserAccount));
        when(userAccountRepository.existsByCorporateEmail(testEmail)).thenReturn(true);

        // Act & Assert
        CorporateEmailException exception = assertThrows(
                CorporateEmailException.class,
                () -> emailVerificationService.sendEmailOTP(testUserId, testEmail)
        );

        assertEquals("EMAIL_ALREADY_EXISTS", exception.getErrorCode());
        verify(emailVerificationRepository, never()).save(any());
    }

    @Test
    void testSendEmailOTP_ActiveVerificationExists() {
        // Arrange
        EmailVerification activeVerification = EmailVerification.builder()
                .userId(testUserId)
                .corporateEmail(testEmail)
                .expiresAt(LocalDateTime.now().plusMinutes(5))
                .build();

        when(userAccountRepository.findById(testUserId)).thenReturn(Optional.of(testUserAccount));
        when(userAccountRepository.existsByCorporateEmail(testEmail)).thenReturn(false);
        when(emailVerificationRepository.findActiveVerificationByUserId(eq(testUserId), any(LocalDateTime.class)))
                .thenReturn(Optional.of(activeVerification));

        // Act & Assert
        CorporateEmailException exception = assertThrows(
                CorporateEmailException.class,
                () -> emailVerificationService.sendEmailOTP(testUserId, testEmail)
        );

        assertEquals("ACTIVE_VERIFICATION_EXISTS", exception.getErrorCode());
        verify(emailVerificationRepository, never()).save(any());
    }

    // ========== OTP Verification Tests ==========

    @Test
    void testVerifyEmailOTP_Success() {
        // Arrange
        String otp = "123456";
        String otpHash = hashOTP(otp);

        EmailVerification emailVerification = EmailVerification.builder()
                .verificationId(UUID.randomUUID())
                .userId(testUserId)
                .corporateEmail(testEmail)
                .otpHash(otpHash)
                .attempts(0)
                .verified(false)
                .expiresAt(LocalDateTime.now().plusMinutes(5))
                .build();

        when(emailVerificationRepository.findActiveVerificationByUserId(eq(testUserId), any(LocalDateTime.class)))
                .thenReturn(Optional.of(emailVerification));
        when(emailVerificationRepository.save(any(EmailVerification.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(userAccountRepository.findById(testUserId)).thenReturn(Optional.of(testUserAccount));
        when(userAccountRepository.save(any(UserAccount.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        VerificationResponse response = emailVerificationService.verifyEmailOTP(testUserId, otp);

        // Assert
        assertNotNull(response);
        assertTrue(response.isVerified());
        // Verify saves: once for marking verified, once for updating user account
        verify(emailVerificationRepository, atLeastOnce()).save(any(EmailVerification.class));
        verify(userAccountRepository, times(1)).save(any(UserAccount.class));
    }

    @Test
    void testVerifyEmailOTP_InvalidOTP() {
        // Arrange
        String correctOtp = "123456";
        String wrongOtp = "654321";
        String otpHash = hashOTP(correctOtp);

        EmailVerification emailVerification = EmailVerification.builder()
                .verificationId(UUID.randomUUID())
                .userId(testUserId)
                .corporateEmail(testEmail)
                .otpHash(otpHash)
                .attempts(0)
                .verified(false)
                .expiresAt(LocalDateTime.now().plusMinutes(5))
                .build();

        when(emailVerificationRepository.findActiveVerificationByUserId(eq(testUserId), any(LocalDateTime.class)))
                .thenReturn(Optional.of(emailVerification));
        when(emailVerificationRepository.save(any(EmailVerification.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // Act & Assert
        CorporateEmailException exception = assertThrows(
                CorporateEmailException.class,
                () -> emailVerificationService.verifyEmailOTP(testUserId, wrongOtp)
        );

        assertEquals("INVALID_OTP", exception.getErrorCode());
        assertTrue(exception.getMessage().contains("attempts remaining"));
        verify(emailVerificationRepository, times(1)).save(any(EmailVerification.class));
    }

    @Test
    void testVerifyEmailOTP_NoActiveVerification() {
        // Arrange
        String otp = "123456";
        when(emailVerificationRepository.findActiveVerificationByUserId(eq(testUserId), any(LocalDateTime.class)))
                .thenReturn(Optional.empty());

        // Act & Assert
        CorporateEmailException exception = assertThrows(
                CorporateEmailException.class,
                () -> emailVerificationService.verifyEmailOTP(testUserId, otp)
        );

        assertEquals("NO_ACTIVE_VERIFICATION", exception.getErrorCode());
    }

    @Test
    void testVerifyEmailOTP_AlreadyVerified() {
        // Arrange
        String otp = "123456";
        String otpHash = hashOTP(otp);

        EmailVerification emailVerification = EmailVerification.builder()
                .verificationId(UUID.randomUUID())
                .userId(testUserId)
                .corporateEmail(testEmail)
                .otpHash(otpHash)
                .attempts(0)
                .verified(true) // Already verified
                .expiresAt(LocalDateTime.now().plusMinutes(5))
                .build();

        when(emailVerificationRepository.findActiveVerificationByUserId(eq(testUserId), any(LocalDateTime.class)))
                .thenReturn(Optional.of(emailVerification));

        // Act & Assert
        CorporateEmailException exception = assertThrows(
                CorporateEmailException.class,
                () -> emailVerificationService.verifyEmailOTP(testUserId, otp)
        );

        assertEquals("ALREADY_VERIFIED", exception.getErrorCode());
    }

    @Test
    void testVerifyEmailOTP_Expired() {
        // Arrange
        String otp = "123456";
        String otpHash = hashOTP(otp);

        EmailVerification emailVerification = EmailVerification.builder()
                .verificationId(UUID.randomUUID())
                .userId(testUserId)
                .corporateEmail(testEmail)
                .otpHash(otpHash)
                .attempts(0)
                .verified(false)
                .expiresAt(LocalDateTime.now().minusMinutes(5)) // Expired
                .build();

        when(emailVerificationRepository.findActiveVerificationByUserId(eq(testUserId), any(LocalDateTime.class)))
                .thenReturn(Optional.of(emailVerification));

        // Act & Assert
        CorporateEmailException exception = assertThrows(
                CorporateEmailException.class,
                () -> emailVerificationService.verifyEmailOTP(testUserId, otp)
        );

        assertEquals("OTP_EXPIRED", exception.getErrorCode());
        verify(emailVerificationRepository, times(1)).delete(emailVerification);
    }

    @Test
    void testVerifyEmailOTP_MaxAttemptsExceeded() {
        // Arrange
        String otp = "123456";
        String otpHash = hashOTP(otp);

        EmailVerification emailVerification = EmailVerification.builder()
                .verificationId(UUID.randomUUID())
                .userId(testUserId)
                .corporateEmail(testEmail)
                .otpHash(otpHash)
                .attempts(3) // Max attempts reached
                .verified(false)
                .expiresAt(LocalDateTime.now().plusMinutes(5))
                .build();

        when(emailVerificationRepository.findActiveVerificationByUserId(eq(testUserId), any(LocalDateTime.class)))
                .thenReturn(Optional.of(emailVerification));

        // Act & Assert
        CorporateEmailException exception = assertThrows(
                CorporateEmailException.class,
                () -> emailVerificationService.verifyEmailOTP(testUserId, otp)
        );

        assertEquals("MAX_ATTEMPTS_EXCEEDED", exception.getErrorCode());
        verify(emailVerificationRepository, times(1)).delete(emailVerification);
    }

    // ========== Resend OTP Tests ==========

    @Test
    void testResendEmailOTP_Success() {
        // Arrange
        EmailVerification existingVerification = EmailVerification.builder()
                .verificationId(UUID.randomUUID())
                .userId(testUserId)
                .corporateEmail(testEmail)
                .expiresAt(LocalDateTime.now().plusMinutes(5))
                .build();

        when(emailVerificationRepository.findActiveVerificationByUserId(eq(testUserId), any(LocalDateTime.class)))
                .thenReturn(Optional.of(existingVerification))
                .thenReturn(Optional.empty()); // After deletion
        when(userAccountRepository.findById(testUserId)).thenReturn(Optional.of(testUserAccount));
        when(userAccountRepository.existsByCorporateEmail(testEmail)).thenReturn(false);
        when(emailVerificationRepository.save(any(EmailVerification.class)))
                .thenAnswer(invocation -> {
                    EmailVerification ev = invocation.getArgument(0);
                    // Simulate @PrePersist
                    if (ev.getExpiresAt() == null) {
                        ev.setExpiresAt(LocalDateTime.now().plusMinutes(10));
                    }
                    return ev;
                });

        // Act
        VerificationResponse response = emailVerificationService.resendEmailOTP(testUserId);

        // Assert
        assertNotNull(response);
        assertTrue(response.isOtpSent());
        verify(emailVerificationRepository, times(1)).delete(existingVerification);
        verify(emailVerificationRepository, times(1)).save(any(EmailVerification.class));
    }

    @Test
    void testResendEmailOTP_NoActiveVerification() {
        // Arrange
        when(emailVerificationRepository.findActiveVerificationByUserId(eq(testUserId), any(LocalDateTime.class)))
                .thenReturn(Optional.empty());

        // Act & Assert
        CorporateEmailException exception = assertThrows(
                CorporateEmailException.class,
                () -> emailVerificationService.resendEmailOTP(testUserId)
        );

        assertEquals("NO_ACTIVE_VERIFICATION", exception.getErrorCode());
    }

    // ========== One Email Per Account Enforcement Tests ==========

    @Test
    void testOneEmailPerAccount_PreventsDuplicateEmail() {
        // Arrange
        String duplicateEmail = "duplicate@company.com";
        when(userAccountRepository.findById(testUserId)).thenReturn(Optional.of(testUserAccount));
        when(userAccountRepository.existsByCorporateEmail(duplicateEmail)).thenReturn(true);

        // Act & Assert
        CorporateEmailException exception = assertThrows(
                CorporateEmailException.class,
                () -> emailVerificationService.sendEmailOTP(testUserId, duplicateEmail)
        );

        assertEquals("EMAIL_ALREADY_EXISTS", exception.getErrorCode());
        assertTrue(exception.getMessage().contains("already registered"));
    }

    @Test
    void testOneEmailPerAccount_AllowsSameUserToUpdateEmail() {
        // This test verifies that the same user can update their email
        // by first removing the old one and adding a new one
        
        // Arrange
        String newEmail = "newemail@company.com";
        
        // Rebuild testUserAccount with existing email
        UserAccount userWithEmail = UserAccount.builder()
                .userId(testUserId)
                .phoneNumber(testPhoneNumber)
                .phoneVerified(true)
                .corporateEmail("oldemail@company.com")
                .emailVerified(true)
                .accountStatus(AccountStatus.ACTIVE)
                .build();
        
        when(userAccountRepository.findById(testUserId)).thenReturn(Optional.of(userWithEmail));
        when(userAccountRepository.existsByCorporateEmail(newEmail)).thenReturn(false);
        when(emailVerificationRepository.findActiveVerificationByUserId(eq(testUserId), any(LocalDateTime.class)))
                .thenReturn(Optional.empty());
        when(emailVerificationRepository.save(any(EmailVerification.class)))
                .thenAnswer(invocation -> {
                    EmailVerification ev = invocation.getArgument(0);
                    // Simulate @PrePersist
                    if (ev.getExpiresAt() == null) {
                        ev.setExpiresAt(LocalDateTime.now().plusMinutes(10));
                    }
                    return ev;
                });

        // Act
        VerificationResponse response = emailVerificationService.sendEmailOTP(testUserId, newEmail);

        // Assert
        assertNotNull(response);
        assertTrue(response.isOtpSent());
        verify(emailVerificationRepository, times(1)).save(any(EmailVerification.class));
    }

    // ========== Email Change Functionality Tests ==========

    @Test
    void testInitiateEmailUpdate_Success() {
        // Arrange
        String newEmail = "newemail@company.com";
        String mobileOtp = "123456";
        
        // Rebuild testUserAccount with email set
        testUserAccount = UserAccount.builder()
                .userId(testUserId)
                .phoneNumber(testPhoneNumber)
                .phoneVerified(true)
                .corporateEmail("oldemail@company.com")
                .emailVerified(true)
                .accountStatus(AccountStatus.ACTIVE)
                .build();

        when(userAccountRepository.findById(testUserId)).thenReturn(Optional.of(testUserAccount));
        when(otpService.verifyMobileOTP(testPhoneNumber, mobileOtp)).thenReturn(true);
        when(userAccountRepository.existsByCorporateEmail(newEmail)).thenReturn(false);
        when(emailChangeAuditLogRepository.save(any())).thenAnswer(invocation -> {
            EmailChangeAuditLog log = invocation.getArgument(0);
            // Simulate database ID generation
            log.setAuditId(UUID.randomUUID());
            return log;
        });
        when(userAccountRepository.save(any(UserAccount.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(emailVerificationRepository.findActiveVerificationByUserId(eq(testUserId), any(LocalDateTime.class)))
                .thenReturn(Optional.empty());
        when(emailVerificationRepository.save(any(EmailVerification.class)))
                .thenAnswer(invocation -> {
                    EmailVerification ev = invocation.getArgument(0);
                    // Simulate @PrePersist
                    if (ev.getExpiresAt() == null) {
                        ev.setExpiresAt(LocalDateTime.now().plusMinutes(10));
                    }
                    return ev;
                });

        // Act
        VerificationResponse response = emailVerificationService.initiateEmailUpdate(
                testUserId,
                mobileOtp,
                newEmail,
                "Changed companies",
                "192.168.1.1",
                "Mozilla/5.0"
        );

        // Assert
        assertNotNull(response);
        assertTrue(response.isOtpSent());
        assertNotNull(response.getAuditLogId()); // Verify audit log ID is set
        verify(otpService, times(1)).verifyMobileOTP(testPhoneNumber, mobileOtp);
        verify(emailChangeAuditLogRepository, times(1)).save(any());
        verify(emailVerificationRepository, times(1)).deleteByUserId(testUserId);
        verify(userAccountRepository, times(1)).save(any(UserAccount.class));
    }

    @Test
    void testInitiateEmailUpdate_InvalidMobileOTP() {
        // Arrange
        String newEmail = "newemail@company.com";
        String mobileOtp = "123456";

        when(userAccountRepository.findById(testUserId)).thenReturn(Optional.of(testUserAccount));
        when(otpService.verifyMobileOTP(testPhoneNumber, mobileOtp)).thenReturn(false);

        // Act & Assert
        CorporateEmailException exception = assertThrows(
                CorporateEmailException.class,
                () -> emailVerificationService.initiateEmailUpdate(
                        testUserId,
                        mobileOtp,
                        newEmail,
                        "Changed companies",
                        "192.168.1.1",
                        "Mozilla/5.0"
                )
        );

        assertEquals("INVALID_MOBILE_OTP", exception.getErrorCode());
        verify(emailChangeAuditLogRepository, never()).save(any());
    }

    @Test
    void testRemoveCorporateEmail_Success() {
        // Arrange
        String mobileOtp = "123456";
        
        // Rebuild testUserAccount with email set
        UserAccount userWithEmail = UserAccount.builder()
                .userId(testUserId)
                .phoneNumber(testPhoneNumber)
                .phoneVerified(true)
                .corporateEmail("oldemail@company.com")
                .emailVerified(true)
                .accountStatus(AccountStatus.ACTIVE)
                .build();

        when(userAccountRepository.findById(testUserId)).thenReturn(Optional.of(userWithEmail));
        when(otpService.verifyMobileOTP(testPhoneNumber, mobileOtp)).thenReturn(true);
        when(emailChangeAuditLogRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(userAccountRepository.save(any(UserAccount.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        emailVerificationService.removeCorporateEmail(
                testUserId,
                mobileOtp,
                "Left company",
                "192.168.1.1",
                "Mozilla/5.0"
        );

        // Assert
        verify(otpService, times(1)).verifyMobileOTP(testPhoneNumber, mobileOtp);
        verify(emailChangeAuditLogRepository, times(1)).save(any());
        verify(emailVerificationRepository, times(1)).deleteByUserId(testUserId);
        verify(userAccountRepository, times(1)).save(any(UserAccount.class));
    }

    @Test
    void testRemoveCorporateEmail_InvalidMobileOTP() {
        // Arrange
        String mobileOtp = "123456";

        when(userAccountRepository.findById(testUserId)).thenReturn(Optional.of(testUserAccount));
        when(otpService.verifyMobileOTP(testPhoneNumber, mobileOtp)).thenReturn(false);

        // Act & Assert
        CorporateEmailException exception = assertThrows(
                CorporateEmailException.class,
                () -> emailVerificationService.removeCorporateEmail(
                        testUserId,
                        mobileOtp,
                        "Left company",
                        "192.168.1.1",
                        "Mozilla/5.0"
                )
        );

        assertEquals("INVALID_MOBILE_OTP", exception.getErrorCode());
        verify(emailChangeAuditLogRepository, never()).save(any());
    }

    /**
     * Helper method to hash OTP (same logic as in EmailVerificationService).
     */
    private String hashOTP(String otp) {
        try {
            java.security.MessageDigest digest = java.security.MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(otp.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            return java.util.Base64.getEncoder().encodeToString(hash);
        } catch (java.security.NoSuchAlgorithmException e) {
            throw new RuntimeException("Failed to hash OTP", e);
        }
    }
}
