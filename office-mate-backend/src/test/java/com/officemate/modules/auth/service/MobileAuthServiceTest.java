package com.officemate.modules.auth.service;

import com.officemate.modules.auth.entity.UserAccount;
import com.officemate.modules.auth.repository.UserAccountRepository;
import com.officemate.shared.dto.AuthResponse;
import com.officemate.shared.dto.RegistrationResponse;
import com.officemate.shared.dto.SessionTokens;
import com.officemate.shared.enums.AccountStatus;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.*;

import com.officemate.config.security.AuthenticationPatternMonitoringService;
import com.officemate.config.security.RateLimitingService;
import com.officemate.shared.service.SecurityEventService;

/**
 * Unit tests for MobileAuthService.
 * Tests phone number validation, registration, and OTP verification logic.
 */
@ExtendWith(MockitoExtension.class)
class MobileAuthServiceTest {

    @Mock
    private UserAccountRepository userAccountRepository;

    @Mock
    private OTPService otpService;

    @Mock
    private SessionManagementService sessionManagementService;

    @Mock
    private SecurityEventService securityEventService;

    @Mock
    private RateLimitingService rateLimitingService;

    @Mock
    private AuthenticationPatternMonitoringService patternMonitoringService;


    @InjectMocks
    private MobileAuthService mobileAuthService;

    private UserAccount testUserAccount;

    @BeforeEach
    void setUp() {
        testUserAccount = UserAccount.builder()
                .userId(UUID.randomUUID())
                .phoneNumber("+919876543210")
                .phoneVerified(false)
                .emailVerified(false)
                .accountStatus(AccountStatus.PENDING_EMAIL)
                .build();
    }

    @Test
    void testRegisterUser_ValidPhoneNumber_Success() {
        // Arrange
        String phoneNumber = "+919876543210";
        when(userAccountRepository.existsByPhoneNumber(phoneNumber)).thenReturn(false);
        when(userAccountRepository.save(any(UserAccount.class))).thenReturn(testUserAccount);
        when(rateLimitingService.isAccountLocked(anyString())).thenReturn(false);
        when(rateLimitingService.isOtpRequestAllowed(anyString(), anyInt())).thenReturn(true);
        when(otpService.generateMobileOTP(phoneNumber)).thenReturn("123456");

        // Act
        RegistrationResponse response = mobileAuthService.registerUser(phoneNumber);

        // Assert
        assertNotNull(response);
        assertNotNull(response.getUserId());
        assertTrue(response.isOtpSent());
        assertNotNull(response.getExpiresAt());
        assertNotNull(response.getMaskedPhoneNumber());
        verify(userAccountRepository).existsByPhoneNumber(phoneNumber);
        verify(userAccountRepository).save(any(UserAccount.class));
        verify(otpService).generateMobileOTP(phoneNumber);
    }

    @Test
    void testRegisterUser_PhoneNumberWithoutCountryCode_Success() {
        // Arrange
        String phoneNumber = "9876543210"; // Indian number without country code
        String expectedFormatted = "+919876543210";
        when(userAccountRepository.existsByPhoneNumber(expectedFormatted)).thenReturn(false);
        when(rateLimitingService.isAccountLocked(anyString())).thenReturn(false);
        when(rateLimitingService.isOtpRequestAllowed(anyString(), anyInt())).thenReturn(true);
        when(userAccountRepository.save(any(UserAccount.class))).thenReturn(testUserAccount);
        when(otpService.generateMobileOTP(expectedFormatted)).thenReturn("123456");

        // Act
        RegistrationResponse response = mobileAuthService.registerUser(phoneNumber);

        // Assert
        assertNotNull(response);
        assertTrue(response.isOtpSent());
        verify(userAccountRepository).existsByPhoneNumber(expectedFormatted);
        verify(otpService).generateMobileOTP(expectedFormatted);
    }

    @Test
    void testRegisterUser_DuplicatePhoneNumber_ThrowsException() {
        // Arrange
        String phoneNumber = "+919876543210";
        when(userAccountRepository.existsByPhoneNumber(phoneNumber)).thenReturn(true);
        when(rateLimitingService.isAccountLocked(anyString())).thenReturn(false);
        when(rateLimitingService.isOtpRequestAllowed(anyString(), anyInt())).thenReturn(true);

        // Act & Assert
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> mobileAuthService.registerUser(phoneNumber)
        );
        assertEquals("Phone number already registered", exception.getMessage());
        verify(userAccountRepository).existsByPhoneNumber(phoneNumber);
        verify(userAccountRepository, never()).save(any(UserAccount.class));
    }

    @Test
    void testRegisterUser_InvalidPhoneNumber_ThrowsException() {
        // Arrange
        String invalidPhoneNumber = "123"; // Too short

        // Act & Assert
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> mobileAuthService.registerUser(invalidPhoneNumber)
        );
        assertTrue(exception.getMessage().contains("Invalid phone number"));
        verify(userAccountRepository, never()).existsByPhoneNumber(anyString());
        verify(userAccountRepository, never()).save(any(UserAccount.class));
    }

    @Test
    void testRegisterUser_EmptyPhoneNumber_ThrowsException() {
        // Arrange
        String emptyPhoneNumber = "";

        // Act & Assert
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> mobileAuthService.registerUser(emptyPhoneNumber)
        );
        assertEquals("Phone number cannot be empty", exception.getMessage());
    }

    @Test
    void testRegisterUser_NullPhoneNumber_ThrowsException() {
        // Act & Assert
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> mobileAuthService.registerUser(null)
        );
        assertEquals("Phone number cannot be empty", exception.getMessage());
    }

    @Test
    void testVerifyOTP_ValidOTP_Success() {
        // Arrange
        String phoneNumber = "+919876543210";
        String otp = "123456";
        String accessToken = "mock.access.token";
        String refreshToken = "mock.refresh.token";
        LocalDateTime expiresAt = LocalDateTime.now().plusHours(1);
        
        SessionTokens mockTokens = SessionTokens.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .expiresAt(expiresAt)
                .sessionId(UUID.randomUUID().toString())
                .userId(testUserAccount.getUserId().toString())
                .build();
        
        when(rateLimitingService.isAccountLocked(phoneNumber)).thenReturn(false);
        when(userAccountRepository.findByPhoneNumber(phoneNumber)).thenReturn(Optional.of(testUserAccount));
        when(userAccountRepository.save(any(UserAccount.class))).thenReturn(testUserAccount);
        when(otpService.verifyMobileOTP(phoneNumber, otp)).thenReturn(true);
        when(sessionManagementService.createSession(any(UserAccount.class), isNull())).thenReturn(mockTokens);

        // Act
        AuthResponse response = mobileAuthService.verifyOTP(phoneNumber, otp);

        // Assert
        assertNotNull(response);
        assertEquals(testUserAccount.getUserId().toString(), response.getUserId());
        assertEquals(accessToken, response.getAccessToken());
        assertEquals(refreshToken, response.getRefreshToken());
        assertTrue(response.isMobileVerified());
        assertFalse(response.isEmailVerified());
        assertFalse(response.isProfileComplete());
        assertNotNull(response.getExpiresAt());
        verify(userAccountRepository).findByPhoneNumber(phoneNumber);
        verify(userAccountRepository).save(any(UserAccount.class));
        verify(otpService).verifyMobileOTP(phoneNumber, otp);
        verify(otpService).deleteMobileOTP(phoneNumber);
        verify(sessionManagementService).createSession(any(UserAccount.class), isNull());
    }

    @Test
    void testVerifyOTP_UserNotFound_ThrowsException() {
        // Arrange
        String phoneNumber = "+919876543210";
        String otp = "123456";
        when(rateLimitingService.isAccountLocked(phoneNumber)).thenReturn(false);
        when(userAccountRepository.findByPhoneNumber(phoneNumber)).thenReturn(Optional.empty());

        // Act & Assert
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> mobileAuthService.verifyOTP(phoneNumber, otp)
        );
        assertEquals("User account not found", exception.getMessage());
        verify(userAccountRepository).findByPhoneNumber(phoneNumber);
        verify(userAccountRepository, never()).save(any(UserAccount.class));
    }

    @Test
    void testLoginUser_ExistingUser_Success() {
        // Arrange
        String phoneNumber = "+919876543210";
        when(userAccountRepository.findByPhoneNumber(phoneNumber)).thenReturn(Optional.of(testUserAccount));
        when(rateLimitingService.isAccountLocked(phoneNumber)).thenReturn(false);
        when(rateLimitingService.isLoginAttemptAllowed(phoneNumber, 10)).thenReturn(true);
        when(otpService.generateMobileOTP(phoneNumber)).thenReturn("123456");

        // Act
        RegistrationResponse response = mobileAuthService.loginUser(phoneNumber);

        // Assert
        assertNotNull(response);
        assertEquals(testUserAccount.getUserId().toString(), response.getUserId());
        assertTrue(response.isOtpSent());
        assertNotNull(response.getExpiresAt());
        verify(userAccountRepository).findByPhoneNumber(phoneNumber);
        verify(otpService).generateMobileOTP(phoneNumber);
    }

    @Test
    void testLoginUser_UserNotFound_ThrowsException() {
        // Arrange
        String phoneNumber = "+919876543210";
        when(userAccountRepository.findByPhoneNumber(phoneNumber)).thenReturn(Optional.empty());
        when(rateLimitingService.isAccountLocked(phoneNumber)).thenReturn(false);
        when(rateLimitingService.isLoginAttemptAllowed(phoneNumber, 10)).thenReturn(true);

        // Act & Assert
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> mobileAuthService.loginUser(phoneNumber)
        );
        assertEquals("Phone number not registered", exception.getMessage());
        verify(userAccountRepository).findByPhoneNumber(phoneNumber);
    }

    @Test
    void testLoginUser_SuspendedAccount_ThrowsException() {
        // Arrange
        String phoneNumber = "+919876543210";
        testUserAccount.suspend();
        when(rateLimitingService.isAccountLocked(phoneNumber)).thenReturn(false);
        when(rateLimitingService.isLoginAttemptAllowed(phoneNumber, 10)).thenReturn(true);
        when(userAccountRepository.findByPhoneNumber(phoneNumber)).thenReturn(Optional.of(testUserAccount));

        // Act & Assert
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> mobileAuthService.loginUser(phoneNumber)
        );
        assertEquals("Account is suspended", exception.getMessage());
        verify(userAccountRepository).findByPhoneNumber(phoneNumber);
    }

    @Test
    void testRegisterUser_USPhoneNumber_Success() {
        // Arrange
        String phoneNumber = "+12025551234"; // US number
        UserAccount usUserAccount = UserAccount.builder()
                .userId(UUID.randomUUID())
                .phoneNumber(phoneNumber)
                .phoneVerified(false)
                .emailVerified(false)
                .accountStatus(AccountStatus.PENDING_EMAIL)
                .build();
        when(userAccountRepository.existsByPhoneNumber(phoneNumber)).thenReturn(false);
        when(rateLimitingService.isAccountLocked(anyString())).thenReturn(false);
        when(rateLimitingService.isOtpRequestAllowed(anyString(), anyInt())).thenReturn(true);
        when(userAccountRepository.save(any(UserAccount.class))).thenReturn(usUserAccount);
        when(otpService.generateMobileOTP(phoneNumber)).thenReturn("123456");

        // Act
        RegistrationResponse response = mobileAuthService.registerUser(phoneNumber);

        // Assert
        assertNotNull(response);
        assertTrue(response.isOtpSent());
        verify(userAccountRepository).existsByPhoneNumber(phoneNumber);
        verify(otpService).generateMobileOTP(phoneNumber);
    }

    @Test
    void testVerifyOTP_InvalidOTP_ThrowsException() {
        // Arrange
        String phoneNumber = "+919876543210";
        String invalidOtp = "000000";
        when(rateLimitingService.isAccountLocked(phoneNumber)).thenReturn(false);
        when(userAccountRepository.findByPhoneNumber(phoneNumber)).thenReturn(Optional.of(testUserAccount));
        when(otpService.verifyMobileOTP(phoneNumber, invalidOtp)).thenReturn(false);
        when(otpService.getRemainingAttempts(phoneNumber)).thenReturn(2);

        // Act & Assert
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> mobileAuthService.verifyOTP(phoneNumber, invalidOtp)
        );
        assertTrue(exception.getMessage().contains("Invalid OTP"));
        assertTrue(exception.getMessage().contains("Remaining attempts: 2"));
        verify(userAccountRepository).findByPhoneNumber(phoneNumber);
        verify(otpService).verifyMobileOTP(phoneNumber, invalidOtp);
        verify(userAccountRepository, never()).save(any(UserAccount.class));
    }

    @Test
    void testVerifyOTP_ExpiredOTP_ThrowsException() {
        // Arrange
        String phoneNumber = "+919876543210";
        String expiredOtp = "123456";
        when(rateLimitingService.isAccountLocked(phoneNumber)).thenReturn(false);
        when(userAccountRepository.findByPhoneNumber(phoneNumber)).thenReturn(Optional.of(testUserAccount));
        when(otpService.verifyMobileOTP(phoneNumber, expiredOtp)).thenReturn(false);
        when(otpService.getRemainingAttempts(phoneNumber)).thenReturn(3);

        // Act & Assert
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> mobileAuthService.verifyOTP(phoneNumber, expiredOtp)
        );
        assertTrue(exception.getMessage().contains("Invalid OTP"));
        verify(otpService).verifyMobileOTP(phoneNumber, expiredOtp);
        verify(userAccountRepository, never()).save(any(UserAccount.class));
    }

    @Test
    void testRegisterUser_PostgreSQLStorage_Success() {
        // Arrange
        String phoneNumber = "+919876543210";
        when(userAccountRepository.existsByPhoneNumber(phoneNumber)).thenReturn(false);
        when(rateLimitingService.isAccountLocked(anyString())).thenReturn(false);
        when(rateLimitingService.isOtpRequestAllowed(anyString(), anyInt())).thenReturn(true);
        when(userAccountRepository.save(any(UserAccount.class))).thenAnswer(invocation -> {
            UserAccount account = invocation.getArgument(0);
            // Simulate PostgreSQL auto-generating UUID
            return UserAccount.builder()
                    .userId(UUID.randomUUID())
                    .phoneNumber(account.getPhoneNumber())
                    .phoneVerified(account.getPhoneVerified())
                    .emailVerified(account.getEmailVerified())
                    .accountStatus(account.getAccountStatus())
                    .build();
        });
        when(otpService.generateMobileOTP(phoneNumber)).thenReturn("123456");

        // Act
        RegistrationResponse response = mobileAuthService.registerUser(phoneNumber);

        // Assert
        assertNotNull(response);
        assertNotNull(response.getUserId());
        assertTrue(response.isOtpSent());
        verify(userAccountRepository).save(any(UserAccount.class));
        verify(userAccountRepository).existsByPhoneNumber(phoneNumber);
    }

    @Test
    void testVerifyOTP_UpdatesPhoneVerifiedFlag_Success() {
        // Arrange
        String phoneNumber = "+919876543210";
        String otp = "123456";
        String accessToken = "mock.access.token";
        String refreshToken = "mock.refresh.token";
        LocalDateTime expiresAt = LocalDateTime.now().plusHours(1);
        
        SessionTokens mockTokens = SessionTokens.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .expiresAt(expiresAt)
                .sessionId(UUID.randomUUID().toString())
                .userId(testUserAccount.getUserId().toString())
                .build();
        
        when(rateLimitingService.isAccountLocked(phoneNumber)).thenReturn(false);
        when(userAccountRepository.findByPhoneNumber(phoneNumber)).thenReturn(Optional.of(testUserAccount));
        when(otpService.verifyMobileOTP(phoneNumber, otp)).thenReturn(true);
        when(userAccountRepository.save(any(UserAccount.class))).thenAnswer(invocation -> {
            UserAccount account = invocation.getArgument(0);
            assertTrue(account.getPhoneVerified(), "Phone should be marked as verified");
            assertNotNull(account.getLastLoginAt(), "Last login should be updated");
            return account;
        });
        when(sessionManagementService.createSession(any(UserAccount.class), isNull())).thenReturn(mockTokens);

        // Act
        AuthResponse response = mobileAuthService.verifyOTP(phoneNumber, otp);

        // Assert
        assertNotNull(response);
        assertTrue(response.isMobileVerified());
        verify(userAccountRepository).save(any(UserAccount.class));
        verify(otpService).deleteMobileOTP(phoneNumber);
    }

    @Test
    void testLoginUser_InvalidPhoneFormat_ThrowsException() {
        // Arrange
        String invalidPhoneNumber = "abc123";

        // Act & Assert
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> mobileAuthService.loginUser(invalidPhoneNumber)
        );
        assertTrue(exception.getMessage().contains("Invalid phone number"));
        verify(userAccountRepository, never()).findByPhoneNumber(anyString());
    }

    @Test
    void testRegisterUser_PhoneNumberNormalization_Success() {
        // Arrange
        String phoneNumberWithSpaces = " +91 9876543210 ";
        String normalizedPhoneNumber = "+919876543210";
        when(rateLimitingService.isAccountLocked(anyString())).thenReturn(false);
        when(rateLimitingService.isOtpRequestAllowed(anyString(), anyInt())).thenReturn(true);
        when(userAccountRepository.existsByPhoneNumber(normalizedPhoneNumber)).thenReturn(false);
        when(userAccountRepository.save(any(UserAccount.class))).thenReturn(testUserAccount);
        when(otpService.generateMobileOTP(normalizedPhoneNumber)).thenReturn("123456");

        // Act
        RegistrationResponse response = mobileAuthService.registerUser(phoneNumberWithSpaces);

        // Assert
        assertNotNull(response);
        assertTrue(response.isOtpSent());
        verify(userAccountRepository).existsByPhoneNumber(normalizedPhoneNumber);
        verify(otpService).generateMobileOTP(normalizedPhoneNumber);
    }

    @Test
    void testVerifyOTP_DeletesOTPAfterSuccess() {
        // Arrange
        String phoneNumber = "+919876543210";
        String otp = "123456";
        String accessToken = "mock.access.token";
        String refreshToken = "mock.refresh.token";
        LocalDateTime expiresAt = LocalDateTime.now().plusHours(1);
        
        SessionTokens mockTokens = SessionTokens.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .expiresAt(expiresAt)
                .sessionId(UUID.randomUUID().toString())
                .userId(testUserAccount.getUserId().toString())
                .build();
        
        when(rateLimitingService.isAccountLocked(phoneNumber)).thenReturn(false);
        when(userAccountRepository.findByPhoneNumber(phoneNumber)).thenReturn(Optional.of(testUserAccount));
        when(otpService.verifyMobileOTP(phoneNumber, otp)).thenReturn(true);
        when(userAccountRepository.save(any(UserAccount.class))).thenReturn(testUserAccount);
        when(sessionManagementService.createSession(any(UserAccount.class), isNull())).thenReturn(mockTokens);

        // Act
        mobileAuthService.verifyOTP(phoneNumber, otp);

        // Assert
        verify(otpService).deleteMobileOTP(phoneNumber);
    }

    @Test
    void testRegisterUser_UKPhoneNumber_Success() {
        // Arrange
        String phoneNumber = "+447911123456"; // UK number
        UserAccount ukUserAccount = UserAccount.builder()
                .userId(UUID.randomUUID())
                .phoneNumber(phoneNumber)
                .phoneVerified(false)
                .emailVerified(false)
                .accountStatus(AccountStatus.PENDING_EMAIL)
                .build();
        when(userAccountRepository.existsByPhoneNumber(phoneNumber)).thenReturn(false);
        when(rateLimitingService.isAccountLocked(anyString())).thenReturn(false);
        when(rateLimitingService.isOtpRequestAllowed(anyString(), anyInt())).thenReturn(true);
        when(userAccountRepository.save(any(UserAccount.class))).thenReturn(ukUserAccount);
        when(otpService.generateMobileOTP(phoneNumber)).thenReturn("123456");

        // Act
        RegistrationResponse response = mobileAuthService.registerUser(phoneNumber);

        // Assert
        assertNotNull(response);
        assertTrue(response.isOtpSent());
        verify(userAccountRepository).existsByPhoneNumber(phoneNumber);
        verify(otpService).generateMobileOTP(phoneNumber);
    }

    @Test
    void testVerifyOTP_AccountStatusRemainsPendingEmail() {
        // Arrange
        String phoneNumber = "+919876543210";
        String otp = "123456";
        String accessToken = "mock.access.token";
        String refreshToken = "mock.refresh.token";
        LocalDateTime expiresAt = LocalDateTime.now().plusHours(1);
        
        SessionTokens mockTokens = SessionTokens.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .expiresAt(expiresAt)
                .sessionId(UUID.randomUUID().toString())
                .userId(testUserAccount.getUserId().toString())
                .build();
        
        when(rateLimitingService.isAccountLocked(phoneNumber)).thenReturn(false);
        when(userAccountRepository.findByPhoneNumber(phoneNumber)).thenReturn(Optional.of(testUserAccount));
        when(otpService.verifyMobileOTP(phoneNumber, otp)).thenReturn(true);
        when(userAccountRepository.save(any(UserAccount.class))).thenReturn(testUserAccount);
        when(sessionManagementService.createSession(any(UserAccount.class), isNull())).thenReturn(mockTokens);

        // Act
        AuthResponse response = mobileAuthService.verifyOTP(phoneNumber, otp);

        // Assert
        assertNotNull(response);
        assertTrue(response.isMobileVerified());
        assertFalse(response.isEmailVerified());
        assertFalse(response.isProfileComplete());
    }
}
