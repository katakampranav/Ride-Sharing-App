package com.officemate.modules.auth.service;

import com.officemate.config.JwtConfig;
import com.officemate.modules.auth.entity.SessionMetadata;
import com.officemate.modules.auth.entity.UserAccount;
import com.officemate.modules.auth.entity.UserSession;
import com.officemate.modules.auth.repository.RevokedTokenRepository;
import com.officemate.modules.auth.repository.SessionMetadataRepository;
import com.officemate.modules.auth.repository.UserSessionRepository;
import com.officemate.shared.dto.DeviceInfo;
import com.officemate.shared.dto.SessionTokens;
import com.officemate.shared.dto.TokenValidation;
import com.officemate.shared.enums.AccountStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * Unit tests for SessionManagementService.
 * Tests JWT token generation, validation, session lifecycle, and multi-device management.
 */
@ExtendWith(MockitoExtension.class)
class SessionManagementServiceTest {

    @Mock
    private UserSessionRepository sessionRepository;

    @Mock
    private SessionMetadataRepository sessionMetadataRepository;

    @Mock
    private RevokedTokenRepository revokedTokenRepository;

    private JwtConfig jwtConfig;
    private SessionManagementService sessionManagementService;
    private UserAccount testUserAccount;
    private DeviceInfo testDeviceInfo;

    @BeforeEach
    void setUp() {
        // Create real JWT config for testing (HMAC mode)
        jwtConfig = new JwtConfig();
        ReflectionTestUtils.setField(jwtConfig, "secret", "test-secret-key-for-testing-only-must-be-at-least-256-bits-long");
        ReflectionTestUtils.setField(jwtConfig, "expiration", 3600000L); // 1 hour
        ReflectionTestUtils.setField(jwtConfig, "refreshExpiration", 86400000L); // 24 hours
        ReflectionTestUtils.setField(jwtConfig, "algorithm", "HS256"); // Use HMAC for testing
        ReflectionTestUtils.setField(jwtConfig, "issuer", "officemate-test");

        // Create service with mocked repositories
        sessionManagementService = new SessionManagementService(
            jwtConfig, 
            sessionRepository, 
            sessionMetadataRepository,
            revokedTokenRepository
        );

        // Create test user account
        testUserAccount = UserAccount.builder()
                .userId(UUID.randomUUID())
                .phoneNumber("+919876543210")
                .phoneVerified(true)
                .emailVerified(false)
                .accountStatus(AccountStatus.PENDING_EMAIL)
                .build();
        
        // Create test device info
        testDeviceInfo = DeviceInfo.builder()
                .deviceType("ANDROID")
                .deviceId("test-device-123")
                .appVersion("1.0.0")
                .ipAddress("192.168.1.1")
                .userAgent("OfficeMate/1.0.0")
                .build();
    }

    // ========== JWT Token Generation Tests ==========
    
    @Test
    void testCreateSession_Success() {
        // Arrange
        when(sessionRepository.save(any(UserSession.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(sessionMetadataRepository.save(any(SessionMetadata.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        SessionTokens tokens = sessionManagementService.createSession(testUserAccount, testDeviceInfo);

        // Assert
        assertNotNull(tokens);
        assertNotNull(tokens.getAccessToken());
        assertNotNull(tokens.getRefreshToken());
        assertNotNull(tokens.getSessionId());
        assertNotNull(tokens.getExpiresAt());
        assertEquals(testUserAccount.getUserId().toString(), tokens.getUserId());
        assertEquals(3, tokens.getAccessToken().split("\\.").length); // JWT has 3 parts
        verify(sessionRepository).save(any(UserSession.class));
        verify(sessionMetadataRepository).save(any(SessionMetadata.class));
    }

    @Test
    void testCreateSession_WithNullDeviceInfo_Success() {
        // Arrange
        when(sessionRepository.save(any(UserSession.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(sessionMetadataRepository.save(any(SessionMetadata.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        SessionTokens tokens = sessionManagementService.createSession(testUserAccount, null);

        // Assert
        assertNotNull(tokens);
        assertNotNull(tokens.getAccessToken());
        assertNotNull(tokens.getRefreshToken());
        verify(sessionRepository).save(any(UserSession.class));
    }

    @Test
    void testGenerateAccessToken_IncludesPermissions() {
        // Arrange
        when(sessionRepository.save(any(UserSession.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(sessionMetadataRepository.save(any(SessionMetadata.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(revokedTokenRepository.existsById(anyString())).thenReturn(false);

        // Act
        SessionTokens tokens = sessionManagementService.createSession(testUserAccount, testDeviceInfo);
        TokenValidation validation = sessionManagementService.validateToken(tokens.getAccessToken());

        // Assert
        assertTrue(validation.isValid());
        assertNotNull(validation.getPermissions());
        assertTrue(validation.getPermissions().contains("MOBILE_VERIFIED"));
        assertFalse(validation.getPermissions().contains("EMAIL_VERIFIED"));
    }

    @Test
    void testGenerateAccessToken_WithFullVerification_IncludesAllPermissions() {
        // Arrange
        testUserAccount.verifyEmail(); // This also sets account to ACTIVE
        when(sessionRepository.save(any(UserSession.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(sessionMetadataRepository.save(any(SessionMetadata.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(revokedTokenRepository.existsById(anyString())).thenReturn(false);

        // Act
        SessionTokens tokens = sessionManagementService.createSession(testUserAccount, testDeviceInfo);
        TokenValidation validation = sessionManagementService.validateToken(tokens.getAccessToken());

        // Assert
        assertTrue(validation.isValid());
        assertTrue(validation.isMobileVerified());
        assertTrue(validation.isEmailVerified());
        assertTrue(validation.getPermissions().contains("MOBILE_VERIFIED"));
        assertTrue(validation.getPermissions().contains("EMAIL_VERIFIED"));
        assertTrue(validation.getPermissions().contains("FULLY_VERIFIED"));
        assertTrue(validation.getPermissions().contains("ACCESS_RIDE_FEATURES"));
        assertTrue(validation.getPermissions().contains("ACCOUNT_ACTIVE"));
    }

    // ========== Token Validation Tests ==========
    
    @Test
    void testValidateToken_ValidToken_Success() {
        // Arrange
        when(sessionRepository.save(any(UserSession.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(sessionMetadataRepository.save(any(SessionMetadata.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(revokedTokenRepository.existsById(anyString())).thenReturn(false);
        when(sessionRepository.findById(anyString())).thenReturn(Optional.empty());

        SessionTokens tokens = sessionManagementService.createSession(testUserAccount, testDeviceInfo);

        // Act
        TokenValidation validation = sessionManagementService.validateToken(tokens.getAccessToken());

        // Assert
        assertTrue(validation.isValid());
        assertEquals(testUserAccount.getUserId().toString(), validation.getUserId());
        assertTrue(validation.isMobileVerified());
        assertFalse(validation.isEmailVerified());
        assertNotNull(validation.getPermissions());
        assertTrue(validation.getPermissions().contains("MOBILE_VERIFIED"));
    }

    @Test
    void testValidateToken_RevokedToken_ReturnsFalse() {
        // Arrange
        when(sessionRepository.save(any(UserSession.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(sessionMetadataRepository.save(any(SessionMetadata.class))).thenAnswer(invocation -> invocation.getArgument(0));
        SessionTokens tokens = sessionManagementService.createSession(testUserAccount, testDeviceInfo);
        
        when(revokedTokenRepository.existsById(anyString())).thenReturn(true);

        // Act
        TokenValidation validation = sessionManagementService.validateToken(tokens.getAccessToken());

        // Assert
        assertFalse(validation.isValid());
        assertEquals("Token has been revoked", validation.getErrorMessage());
    }

    @Test
    void testValidateToken_InvalidToken_ReturnsFalse() {
        // Act
        TokenValidation validation = sessionManagementService.validateToken("invalid.token.here");

        // Assert
        assertFalse(validation.isValid());
        assertNotNull(validation.getErrorMessage());
    }

    @Test
    void testValidateToken_MalformedToken_ReturnsFalse() {
        // Act
        TokenValidation validation = sessionManagementService.validateToken("not-a-jwt-token");

        // Assert
        assertFalse(validation.isValid());
        assertTrue(validation.getErrorMessage().contains("Malformed token"));
    }

    // ========== Session Lifecycle Tests ==========
    
    @Test
    void testRefreshSession_ValidRefreshToken_Success() {
        // Arrange
        when(sessionRepository.save(any(UserSession.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(sessionMetadataRepository.save(any(SessionMetadata.class))).thenAnswer(invocation -> invocation.getArgument(0));
        SessionTokens originalTokens = sessionManagementService.createSession(testUserAccount, testDeviceInfo);
        
        UserSession mockSession = UserSession.builder()
                .sessionId(originalTokens.getSessionId())
                .userId(testUserAccount.getUserId().toString())
                .refreshToken(originalTokens.getRefreshToken())
                .mobileVerified(true)
                .emailVerified(false)
                .build();
        
        when(sessionRepository.findById(originalTokens.getSessionId())).thenReturn(Optional.of(mockSession));
        when(revokedTokenRepository.existsById(anyString())).thenReturn(false);

        // Act
        SessionTokens refreshedTokens = sessionManagementService.refreshSession(
                originalTokens.getRefreshToken(), 
                testUserAccount
        );

        // Assert
        assertNotNull(refreshedTokens);
        assertNotNull(refreshedTokens.getAccessToken());
        assertEquals(originalTokens.getRefreshToken(), refreshedTokens.getRefreshToken());
        assertEquals(originalTokens.getSessionId(), refreshedTokens.getSessionId());
        verify(sessionRepository, atLeastOnce()).save(any(UserSession.class));
    }

    @Test
    void testRefreshSession_UpdatesVerificationStatus() {
        // Arrange
        when(sessionRepository.save(any(UserSession.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(sessionMetadataRepository.save(any(SessionMetadata.class))).thenAnswer(invocation -> invocation.getArgument(0));
        SessionTokens originalTokens = sessionManagementService.createSession(testUserAccount, testDeviceInfo);
        
        // User verifies email after session creation
        testUserAccount.verifyEmail();
        
        UserSession mockSession = UserSession.builder()
                .sessionId(originalTokens.getSessionId())
                .userId(testUserAccount.getUserId().toString())
                .refreshToken(originalTokens.getRefreshToken())
                .mobileVerified(true)
                .emailVerified(false)
                .build();
        
        when(sessionRepository.findById(originalTokens.getSessionId())).thenReturn(Optional.of(mockSession));
        when(revokedTokenRepository.existsById(anyString())).thenReturn(false);

        // Act
        SessionTokens refreshedTokens = sessionManagementService.refreshSession(
                originalTokens.getRefreshToken(), 
                testUserAccount
        );

        // Assert
        assertNotNull(refreshedTokens);
        TokenValidation validation = sessionManagementService.validateToken(refreshedTokens.getAccessToken());
        assertTrue(validation.isEmailVerified());
    }

    @Test
    void testRevokeSession_Success() {
        // Arrange
        String sessionId = UUID.randomUUID().toString();
        UserSession mockSession = UserSession.builder()
                .sessionId(sessionId)
                .userId(testUserAccount.getUserId().toString())
                .refreshToken("mock.refresh.token")
                .build();
        
        when(sessionRepository.findById(sessionId)).thenReturn(Optional.of(mockSession));
        when(sessionMetadataRepository.findBySessionId(sessionId)).thenReturn(Optional.empty());

        // Act
        sessionManagementService.revokeSession(sessionId);

        // Assert
        verify(sessionRepository).deleteById(sessionId);
    }

    @Test
    void testRevokeAllSessions_Success() {
        // Arrange
        String userId = testUserAccount.getUserId().toString();
        UserSession session1 = UserSession.builder()
                .sessionId("session-1")
                .userId(userId)
                .refreshToken("refresh.token.1")
                .build();
        UserSession session2 = UserSession.builder()
                .sessionId("session-2")
                .userId(userId)
                .refreshToken("refresh.token.2")
                .build();
        
        when(sessionRepository.findByUserId(userId)).thenReturn(Arrays.asList(session1, session2));
        when(sessionMetadataRepository.findBySessionId(anyString())).thenReturn(Optional.empty());

        // Act
        sessionManagementService.revokeAllSessions(userId);

        // Assert
        verify(sessionRepository).deleteByUserId(userId);
        verify(sessionRepository).findByUserId(userId);
    }

    @Test
    void testIsSessionActive_ActiveSession_ReturnsTrue() {
        // Arrange
        String sessionId = "active-session-id";
        when(sessionRepository.existsById(sessionId)).thenReturn(true);

        // Act
        boolean isActive = sessionManagementService.isSessionActive(sessionId);

        // Assert
        assertTrue(isActive);
    }

    @Test
    void testIsSessionActive_InactiveSession_ReturnsFalse() {
        // Arrange
        String sessionId = "inactive-session-id";
        when(sessionRepository.existsById(sessionId)).thenReturn(false);

        // Act
        boolean isActive = sessionManagementService.isSessionActive(sessionId);

        // Assert
        assertFalse(isActive);
    }

    // ========== Multi-Device Session Management Tests ==========
    
    @Test
    void testGetUserSessions_ReturnsAllSessions() {
        // Arrange
        String userId = testUserAccount.getUserId().toString();
        UserSession session1 = UserSession.builder()
                .sessionId("session-1")
                .userId(userId)
                .deviceType("ANDROID")
                .build();
        UserSession session2 = UserSession.builder()
                .sessionId("session-2")
                .userId(userId)
                .deviceType("IOS")
                .build();
        
        when(sessionRepository.findByUserId(userId)).thenReturn(Arrays.asList(session1, session2));

        // Act
        List<UserSession> sessions = sessionManagementService.getUserSessions(userId);

        // Assert
        assertEquals(2, sessions.size());
        verify(sessionRepository).findByUserId(userId);
    }

    @Test
    void testRevokeDeviceSessions_RevokesOnlySpecificDevice() {
        // Arrange
        String userId = testUserAccount.getUserId().toString();
        String deviceIdToRevoke = "device-to-revoke";
        
        UserSession session1 = UserSession.builder()
                .sessionId("session-1")
                .userId(userId)
                .deviceId(deviceIdToRevoke)
                .refreshToken("refresh.token.1")
                .build();
        UserSession session2 = UserSession.builder()
                .sessionId("session-2")
                .userId(userId)
                .deviceId("other-device")
                .refreshToken("refresh.token.2")
                .build();
        
        when(sessionRepository.findByUserId(userId)).thenReturn(Arrays.asList(session1, session2));
        when(sessionMetadataRepository.findBySessionId(anyString())).thenReturn(Optional.empty());

        // Act
        sessionManagementService.revokeDeviceSessions(userId, deviceIdToRevoke);

        // Assert
        verify(sessionRepository).deleteById("session-1");
        verify(sessionRepository, never()).deleteById("session-2");
    }

    @Test
    void testGetActiveSessionCount_ReturnsCorrectCount() {
        // Arrange
        UUID userId = testUserAccount.getUserId();
        when(sessionMetadataRepository.countByUserIdAndActiveTrue(userId)).thenReturn(3L);

        // Act
        long count = sessionManagementService.getActiveSessionCount(userId.toString());

        // Assert
        assertEquals(3L, count);
    }

    @Test
    void testGetSessionHistory_ReturnsAllSessions() {
        // Arrange
        UUID userId = testUserAccount.getUserId();
        SessionMetadata metadata1 = SessionMetadata.builder()
                .sessionId("session-1")
                .userId(userId)
                .active(true)
                .build();
        SessionMetadata metadata2 = SessionMetadata.builder()
                .sessionId("session-2")
                .userId(userId)
                .active(false)
                .build();
        
        when(sessionMetadataRepository.findByUserIdOrderByCreatedAtDesc(userId))
                .thenReturn(Arrays.asList(metadata1, metadata2));

        // Act
        List<SessionMetadata> history = sessionManagementService.getSessionHistory(userId.toString());

        // Assert
        assertEquals(2, history.size());
    }

    // ========== Token Expiration Tests ==========
    
    @Test
    void testGetAccessTokenExpiration_ReturnsValidTime() {
        // Act
        var expiration = sessionManagementService.getAccessTokenExpiration();

        // Assert
        assertNotNull(expiration);
        assertTrue(expiration.isAfter(java.time.LocalDateTime.now()));
    }

    @Test
    void testGetRefreshTokenExpiration_ReturnsValidTime() {
        // Act
        var expiration = sessionManagementService.getRefreshTokenExpiration();

        // Assert
        assertNotNull(expiration);
        assertTrue(expiration.isAfter(java.time.LocalDateTime.now()));
        assertTrue(expiration.isAfter(sessionManagementService.getAccessTokenExpiration()));
    }

    // ========== Session Cleanup Tests ==========
    
    @Test
    void testRevokeSession_UpdatesMetadata() {
        // Arrange
        String sessionId = UUID.randomUUID().toString();
        UserSession mockSession = UserSession.builder()
                .sessionId(sessionId)
                .userId(testUserAccount.getUserId().toString())
                .refreshToken("mock.refresh.token")
                .build();
        
        SessionMetadata mockMetadata = SessionMetadata.builder()
                .sessionId(sessionId)
                .userId(testUserAccount.getUserId())
                .active(true)
                .build();
        
        when(sessionRepository.findById(sessionId)).thenReturn(Optional.of(mockSession));
        when(sessionMetadataRepository.findBySessionId(sessionId)).thenReturn(Optional.of(mockMetadata));

        // Act
        sessionManagementService.revokeSession(sessionId);

        // Assert
        verify(sessionMetadataRepository).save(any(SessionMetadata.class));
        verify(sessionRepository).deleteById(sessionId);
    }

    @Test
    void testGetUserSessions_EmptyList_WhenNoSessions() {
        // Arrange
        String userId = testUserAccount.getUserId().toString();
        when(sessionRepository.findByUserId(userId)).thenReturn(Collections.emptyList());

        // Act
        List<UserSession> sessions = sessionManagementService.getUserSessions(userId);

        // Assert
        assertTrue(sessions.isEmpty());
    }
}
