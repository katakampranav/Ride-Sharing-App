package com.officemate.modules.auth.service;

import com.officemate.config.JwtConfig;
import com.officemate.modules.auth.entity.RevokedToken;
import com.officemate.modules.auth.entity.SessionMetadata;
import com.officemate.modules.auth.entity.UserAccount;
import com.officemate.modules.auth.entity.UserSession;
import com.officemate.modules.auth.repository.RevokedTokenRepository;
import com.officemate.modules.auth.repository.SessionMetadataRepository;
import com.officemate.modules.auth.repository.UserSessionRepository;
import com.officemate.shared.dto.DeviceInfo;
import com.officemate.shared.dto.SessionTokens;
import com.officemate.shared.dto.TokenValidation;
import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import io.jsonwebtoken.security.SignatureException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;

/**
 * Service for managing user sessions and JWT tokens with RSA signing.
 * Handles token generation, validation, refresh, revocation, and session lifecycle.
 * Supports permission tracking for mobile and email verification status.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class SessionManagementService {

    private final JwtConfig jwtConfig;
    private final UserSessionRepository sessionRepository;
    private final SessionMetadataRepository sessionMetadataRepository;
    private final RevokedTokenRepository revokedTokenRepository;

    /**
     * Create a new session with JWT tokens for a user.
     * Stores session information in Redis and generates access and refresh tokens.
     *
     * @param userAccount the user account
     * @param deviceInfo device information for session tracking
     * @return SessionTokens containing access token, refresh token, and session info
     */
    @Transactional
    public SessionTokens createSession(UserAccount userAccount, DeviceInfo deviceInfo) {
        String sessionId = UUID.randomUUID().toString();
        String userId = userAccount.getUserId().toString();
        
        // Calculate expiration times
        LocalDateTime accessExpiration = LocalDateTime.now()
                .plusSeconds(jwtConfig.getExpiration() / 1000);
        LocalDateTime refreshExpiration = LocalDateTime.now()
                .plusSeconds(jwtConfig.getRefreshExpiration() / 1000);
        
        // Build permissions list based on verification status
        List<String> permissions = buildPermissions(userAccount);
        
        // Generate tokens
        String accessToken = generateAccessToken(userAccount, sessionId);
        String refreshToken = generateRefreshToken(userAccount, sessionId);
        
        // Create and save session
        UserSession session = UserSession.builder()
                .sessionId(sessionId)
                .userId(userId)
                .deviceType(deviceInfo != null ? deviceInfo.getDeviceType() : "UNKNOWN")
                .deviceId(deviceInfo != null ? deviceInfo.getDeviceId() : "UNKNOWN")
                .appVersion(deviceInfo != null ? deviceInfo.getAppVersion() : "UNKNOWN")
                .permissions(permissions)
                .createdAt(LocalDateTime.now())
                .lastAccessAt(LocalDateTime.now())
                .expiresAt(refreshExpiration)
                .refreshToken(refreshToken)
                .mobileVerified(userAccount.getPhoneVerified())
                .emailVerified(userAccount.getEmailVerified())
                .ttl(jwtConfig.getRefreshExpiration() / 1000)
                .build();
        
        sessionRepository.save(session);
        
        // Create session metadata in PostgreSQL for audit trail
        SessionMetadata metadata = SessionMetadata.builder()
                .sessionId(sessionId)
                .userId(userAccount.getUserId())
                .deviceType(deviceInfo != null ? deviceInfo.getDeviceType() : "UNKNOWN")
                .deviceId(deviceInfo != null ? deviceInfo.getDeviceId() : "UNKNOWN")
                .appVersion(deviceInfo != null ? deviceInfo.getAppVersion() : "UNKNOWN")
                .ipAddress(deviceInfo != null ? deviceInfo.getIpAddress() : null)
                .userAgent(deviceInfo != null ? deviceInfo.getUserAgent() : null)
                .mobileVerified(userAccount.getPhoneVerified())
                .emailVerified(userAccount.getEmailVerified())
                .build();
        
        sessionMetadataRepository.save(metadata);
        
        log.info("Created new session {} for user {} on device {}", 
                sessionId, userId, deviceInfo != null ? deviceInfo.getDeviceType() : "UNKNOWN");
        
        return SessionTokens.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .expiresAt(accessExpiration)
                .sessionId(sessionId)
                .userId(userId)
                .build();
    }

    /**
     * Generate JWT access token with verification status and permissions.
     * Uses RSA signing if configured, otherwise falls back to HMAC.
     *
     * @param userAccount the user account
     * @param sessionId the session ID
     * @return JWT access token string
     */
    public String generateAccessToken(UserAccount userAccount, String sessionId) {
        String userId = userAccount.getUserId().toString();
        Date now = new Date();
        Date expirationDate = Date.from(
            LocalDateTime.now()
                .plusSeconds(jwtConfig.getExpiration() / 1000)
                .atZone(ZoneId.systemDefault())
                .toInstant()
        );
        
        // Build claims
        Map<String, Object> claims = new HashMap<>();
        claims.put("userId", userId);
        claims.put("sessionId", sessionId);
        claims.put("mobileVerified", userAccount.getPhoneVerified());
        claims.put("emailVerified", userAccount.getEmailVerified());
        claims.put("accountStatus", userAccount.getAccountStatus().name());
        claims.put("permissions", buildPermissions(userAccount));
        claims.put("tokenType", "ACCESS");
        
        log.debug("Generating access token for user: {} session: {}", userId, sessionId);
        
        return Jwts.builder()
                .claims(claims)
                .subject(userId)
                .issuer(jwtConfig.getIssuer())
                .issuedAt(now)
                .expiration(expirationDate)
                .id(UUID.randomUUID().toString()) // jti claim for revocation
                .signWith(getSigningKey())
                .compact();
    }

    /**
     * Generate JWT refresh token.
     * Used to obtain new access tokens without re-authentication.
     *
     * @param userAccount the user account
     * @param sessionId the session ID
     * @return JWT refresh token string
     */
    public String generateRefreshToken(UserAccount userAccount, String sessionId) {
        String userId = userAccount.getUserId().toString();
        Date now = new Date();
        Date expirationDate = Date.from(
            LocalDateTime.now()
                .plusSeconds(jwtConfig.getRefreshExpiration() / 1000)
                .atZone(ZoneId.systemDefault())
                .toInstant()
        );
        
        Map<String, Object> claims = new HashMap<>();
        claims.put("userId", userId);
        claims.put("sessionId", sessionId);
        claims.put("tokenType", "REFRESH");
        
        log.debug("Generating refresh token for user: {} session: {}", userId, sessionId);
        
        return Jwts.builder()
                .claims(claims)
                .subject(userId)
                .issuer(jwtConfig.getIssuer())
                .issuedAt(now)
                .expiration(expirationDate)
                .id(UUID.randomUUID().toString()) // jti claim for revocation
                .signWith(getSigningKey())
                .compact();
    }

    /**
     * Validate JWT token and extract claims.
     * Checks token signature, expiration, and revocation status.
     *
     * @param token the JWT token to validate
     * @return TokenValidation object with validation results
     */
    public TokenValidation validateToken(String token) {
        try {
            Claims claims = parseToken(token);
            
            // Check if token is revoked
            String tokenId = claims.getId();
            if (tokenId != null && revokedTokenRepository.existsById(tokenId)) {
                log.warn("Token {} has been revoked", tokenId);
                return TokenValidation.builder()
                        .valid(false)
                        .errorMessage("Token has been revoked")
                        .build();
            }
            
            // Extract information from claims
            String userId = claims.getSubject();
            String sessionId = claims.get("sessionId", String.class);
            Boolean mobileVerified = claims.get("mobileVerified", Boolean.class);
            Boolean emailVerified = claims.get("emailVerified", Boolean.class);
            
            @SuppressWarnings("unchecked")
            List<String> permissions = claims.get("permissions", List.class);
            
            // Update session last access time
            if (sessionId != null) {
                updateSessionAccess(sessionId);
            }
            
            return TokenValidation.builder()
                    .valid(true)
                    .userId(userId)
                    .sessionId(sessionId)
                    .mobileVerified(mobileVerified != null && mobileVerified)
                    .emailVerified(emailVerified != null && emailVerified)
                    .permissions(permissions != null ? permissions : Collections.emptyList())
                    .build();
            
        } catch (ExpiredJwtException e) {
            log.debug("Token has expired: {}", e.getMessage());
            return TokenValidation.builder()
                    .valid(false)
                    .errorMessage("Token has expired")
                    .build();
        } catch (SignatureException e) {
            log.warn("Invalid token signature: {}", e.getMessage());
            return TokenValidation.builder()
                    .valid(false)
                    .errorMessage("Invalid token signature")
                    .build();
        } catch (MalformedJwtException e) {
            log.warn("Malformed token: {}", e.getMessage());
            return TokenValidation.builder()
                    .valid(false)
                    .errorMessage("Malformed token")
                    .build();
        } catch (Exception e) {
            log.error("Token validation error", e);
            return TokenValidation.builder()
                    .valid(false)
                    .errorMessage("Token validation failed: " + e.getMessage())
                    .build();
        }
    }

    /**
     * Refresh session and generate new access token.
     * Validates refresh token and creates new access token with updated permissions.
     *
     * @param refreshToken the refresh token
     * @param userAccount updated user account (for permission refresh)
     * @return new SessionTokens with fresh access token
     */
    @Transactional
    public SessionTokens refreshSession(String refreshToken, UserAccount userAccount) {
        try {
            Claims claims = parseToken(refreshToken);
            
            // Verify it's a refresh token
            String tokenType = claims.get("tokenType", String.class);
            if (!"REFRESH".equals(tokenType)) {
                throw new IllegalArgumentException("Invalid token type for refresh");
            }
            
            // Check if token is revoked
            String tokenId = claims.getId();
            if (tokenId != null && revokedTokenRepository.existsById(tokenId)) {
                throw new IllegalArgumentException("Refresh token has been revoked");
            }
            
            String sessionId = claims.get("sessionId", String.class);
            
            // Verify session exists
            Optional<UserSession> sessionOpt = sessionRepository.findById(sessionId);
            if (sessionOpt.isEmpty()) {
                throw new IllegalArgumentException("Session not found");
            }
            
            UserSession session = sessionOpt.get();
            
            // Update session with new verification status
            session.setMobileVerified(userAccount.getPhoneVerified());
            session.setEmailVerified(userAccount.getEmailVerified());
            session.setPermissions(buildPermissions(userAccount));
            session.updateLastAccess();
            sessionRepository.save(session);
            
            // Generate new access token
            String newAccessToken = generateAccessToken(userAccount, sessionId);
            LocalDateTime accessExpiration = LocalDateTime.now()
                    .plusSeconds(jwtConfig.getExpiration() / 1000);
            
            log.info("Refreshed session {} for user {}", sessionId, userAccount.getUserId());
            
            return SessionTokens.builder()
                    .accessToken(newAccessToken)
                    .refreshToken(refreshToken) // Keep same refresh token
                    .expiresAt(accessExpiration)
                    .sessionId(sessionId)
                    .userId(userAccount.getUserId().toString())
                    .build();
            
        } catch (JwtException e) {
            log.error("Failed to refresh session: {}", e.getMessage());
            throw new IllegalArgumentException("Invalid refresh token", e);
        }
    }

    /**
     * Revoke a specific session by session ID.
     * Adds tokens to blacklist and removes session from Redis.
     *
     * @param sessionId the session ID to revoke
     */
    @Transactional
    public void revokeSession(String sessionId) {
        Optional<UserSession> sessionOpt = sessionRepository.findById(sessionId);
        if (sessionOpt.isPresent()) {
            UserSession session = sessionOpt.get();
            
            // Add refresh token to revoked list
            revokeTokenById(session.getRefreshToken(), session.getUserId(), "Session revoked");
            
            // Update metadata in PostgreSQL
            sessionMetadataRepository.findBySessionId(sessionId)
                .ifPresent(metadata -> {
                    metadata.endSession("USER_LOGOUT");
                    sessionMetadataRepository.save(metadata);
                });
            
            // Delete session from Redis
            sessionRepository.deleteById(sessionId);
            
            log.info("Revoked session {} for user {}", sessionId, session.getUserId());
        }
    }

    /**
     * Revoke all sessions for a specific user.
     * Used for security events like password change or account compromise.
     *
     * @param userId the user ID
     */
    @Transactional
    public void revokeAllSessions(String userId) {
        List<UserSession> sessions = sessionRepository.findByUserId(userId);
        
        for (UserSession session : sessions) {
            // Add refresh token to revoked list
            revokeTokenById(session.getRefreshToken(), userId, "All sessions revoked");
            
            // Update metadata in PostgreSQL
            sessionMetadataRepository.findBySessionId(session.getSessionId())
                .ifPresent(metadata -> {
                    metadata.endSession("SECURITY_EVENT");
                    sessionMetadataRepository.save(metadata);
                });
        }
        
        // Delete all sessions from Redis
        sessionRepository.deleteByUserId(userId);
        
        log.info("Revoked all {} sessions for user {}", sessions.size(), userId);
    }

    /**
     * Revoke a specific token by adding it to the blacklist.
     *
     * @param token the JWT token to revoke
     * @param userId the user ID
     * @param reason reason for revocation
     */
    public void revokeTokenById(String token, String userId, String reason) {
        try {
            Claims claims = parseToken(token);
            String tokenId = claims.getId();
            
            if (tokenId != null) {
                // Calculate TTL based on token expiration
                Date expiration = claims.getExpiration();
                long ttl = (expiration.getTime() - System.currentTimeMillis()) / 1000;
                
                if (ttl > 0) {
                    RevokedToken revokedToken = RevokedToken.builder()
                            .tokenId(tokenId)
                            .userId(userId)
                            .revokedAt(LocalDateTime.now())
                            .reason(reason)
                            .ttl(ttl)
                            .build();
                    
                    revokedTokenRepository.save(revokedToken);
                    log.info("Revoked token {} for user {}: {}", tokenId, userId, reason);
                }
            }
        } catch (Exception e) {
            log.error("Failed to revoke token", e);
        }
    }

    /**
     * Get all active sessions for a user.
     *
     * @param userId the user ID
     * @return list of active sessions
     */
    public List<UserSession> getUserSessions(String userId) {
        return sessionRepository.findByUserId(userId);
    }

    /**
     * Parse JWT token and extract claims.
     *
     * @param token the JWT token
     * @return Claims object
     * @throws JwtException if token is invalid
     */
    private Claims parseToken(String token) throws JwtException {
        if (jwtConfig.isRsaEnabled()) {
            return Jwts.parser()
                    .verifyWith(jwtConfig.getPublicKey())
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
        } else {
            byte[] keyBytes = jwtConfig.getSecret().getBytes(StandardCharsets.UTF_8);
            SecretKey key = Keys.hmacShaKeyFor(keyBytes);
            return Jwts.parser()
                    .verifyWith(key)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
        }
    }

    /**
     * Update session last access timestamp.
     *
     * @param sessionId the session ID
     */
    private void updateSessionAccess(String sessionId) {
        try {
            // Update Redis session
            Optional<UserSession> sessionOpt = sessionRepository.findById(sessionId);
            if (sessionOpt.isPresent()) {
                UserSession session = sessionOpt.get();
                session.updateLastAccess();
                sessionRepository.save(session);
                
                // Update PostgreSQL metadata (async to avoid blocking)
                sessionMetadataRepository.findBySessionId(sessionId)
                    .ifPresent(metadata -> {
                        metadata.updateActivity();
                        sessionMetadataRepository.save(metadata);
                    });
            }
        } catch (Exception e) {
            log.debug("Failed to update session access time: {}", e.getMessage());
        }
    }

    /**
     * Build permissions list based on user verification status.
     *
     * @param userAccount the user account
     * @return list of permission strings
     */
    private List<String> buildPermissions(UserAccount userAccount) {
        List<String> permissions = new ArrayList<>();
        
        if (userAccount.getPhoneVerified()) {
            permissions.add("MOBILE_VERIFIED");
        }
        
        if (userAccount.getEmailVerified()) {
            permissions.add("EMAIL_VERIFIED");
        }
        
        if (userAccount.isFullyVerified()) {
            permissions.add("FULLY_VERIFIED");
            permissions.add("ACCESS_RIDE_FEATURES");
        }
        
        if (userAccount.isActive()) {
            permissions.add("ACCOUNT_ACTIVE");
        }
        
        return permissions;
    }

    /**
     * Get signing key for JWT token generation.
     * Uses RSA private key if configured, otherwise HMAC secret.
     *
     * @return Key for signing
     */
    private Key getSigningKey() {
        if (jwtConfig.isRsaEnabled()) {
            return jwtConfig.getPrivateKey();
        } else {
            byte[] keyBytes = jwtConfig.getSecret().getBytes(StandardCharsets.UTF_8);
            return Keys.hmacShaKeyFor(keyBytes);
        }
    }

    /**
     * Get verification key for JWT token validation.
     * Uses RSA public key if configured, otherwise HMAC secret.
     *
     * @return Key for verification
     */
    private Key getVerificationKey() {
        if (jwtConfig.isRsaEnabled()) {
            return jwtConfig.getPublicKey();
        } else {
            byte[] keyBytes = jwtConfig.getSecret().getBytes(StandardCharsets.UTF_8);
            return Keys.hmacShaKeyFor(keyBytes);
        }
    }

    /**
     * Calculate access token expiration time.
     *
     * @return LocalDateTime when access token expires
     */
    public LocalDateTime getAccessTokenExpiration() {
        return LocalDateTime.now().plusSeconds(jwtConfig.getExpiration() / 1000);
    }

    /**
     * Calculate refresh token expiration time.
     *
     * @return LocalDateTime when refresh token expires
     */
    public LocalDateTime getRefreshTokenExpiration() {
        return LocalDateTime.now().plusSeconds(jwtConfig.getRefreshExpiration() / 1000);
    }
    
    /**
     * Get all active sessions with metadata for a user.
     * Combines Redis session data with PostgreSQL metadata.
     *
     * @param userId the user ID
     * @return list of session metadata
     */
    public List<SessionMetadata> getUserSessionsWithMetadata(String userId) {
        try {
            java.util.UUID userUuid = java.util.UUID.fromString(userId);
            return sessionMetadataRepository.findByUserIdAndActiveTrue(userUuid);
        } catch (IllegalArgumentException e) {
            log.error("Invalid user ID format: {}", userId);
            return Collections.emptyList();
        }
    }
    
    /**
     * Get session count for a user.
     *
     * @param userId the user ID
     * @return number of active sessions
     */
    public long getActiveSessionCount(String userId) {
        try {
            java.util.UUID userUuid = java.util.UUID.fromString(userId);
            return sessionMetadataRepository.countByUserIdAndActiveTrue(userUuid);
        } catch (IllegalArgumentException e) {
            log.error("Invalid user ID format: {}", userId);
            return 0;
        }
    }
    
    /**
     * Revoke sessions for a specific device.
     * Useful when a device is lost or compromised.
     *
     * @param userId the user ID
     * @param deviceId the device ID to revoke
     */
    @Transactional
    public void revokeDeviceSessions(String userId, String deviceId) {
        List<UserSession> sessions = sessionRepository.findByUserId(userId);
        int revokedCount = 0;
        
        for (UserSession session : sessions) {
            if (deviceId.equals(session.getDeviceId())) {
                // Add refresh token to revoked list
                revokeTokenById(session.getRefreshToken(), userId, "Device revoked");
                
                // Update metadata
                sessionMetadataRepository.findBySessionId(session.getSessionId())
                    .ifPresent(metadata -> {
                        metadata.endSession("DEVICE_REVOKED");
                        sessionMetadataRepository.save(metadata);
                    });
                
                // Delete from Redis
                sessionRepository.deleteById(session.getSessionId());
                revokedCount++;
            }
        }
        
        log.info("Revoked {} sessions for device {} of user {}", revokedCount, deviceId, userId);
    }
    
    /**
     * Get session history for a user.
     * Returns both active and ended sessions.
     *
     * @param userId the user ID
     * @return list of session metadata ordered by creation date
     */
    public List<SessionMetadata> getSessionHistory(String userId) {
        try {
            java.util.UUID userUuid = java.util.UUID.fromString(userId);
            return sessionMetadataRepository.findByUserIdOrderByCreatedAtDesc(userUuid);
        } catch (IllegalArgumentException e) {
            log.error("Invalid user ID format: {}", userId);
            return Collections.emptyList();
        }
    }
    
    /**
     * Check if a session is still active.
     *
     * @param sessionId the session ID
     * @return true if session is active, false otherwise
     */
    public boolean isSessionActive(String sessionId) {
        return sessionRepository.existsById(sessionId);
    }
}
