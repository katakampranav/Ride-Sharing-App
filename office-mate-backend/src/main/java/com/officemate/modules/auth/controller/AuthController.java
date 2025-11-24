package com.officemate.modules.auth.controller;

import com.officemate.modules.auth.entity.UserAccount;
import com.officemate.modules.auth.repository.UserAccountRepository;
import com.officemate.modules.auth.service.EmailVerificationService;
import com.officemate.modules.auth.service.MobileAuthService;
import com.officemate.modules.auth.service.SessionManagementService;
import com.officemate.shared.dto.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * REST controller for authentication operations.
 * Handles user registration, login, OTP verification, token refresh, and logout.
 */
@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
@Slf4j
public class AuthController {

    private final MobileAuthService mobileAuthService;
    private final SessionManagementService sessionManagementService;
    private final UserAccountRepository userAccountRepository;
    private final EmailVerificationService emailVerificationService;

    /**
     * Register a new user with mobile number.
     * Sends OTP to the provided phone number.
     * 
     * POST /auth/register
     * 
     * @param request RegisterRequest containing phone number
     * @return RegistrationResponse with user ID and OTP status
     */
    @PostMapping("/register")
    public ResponseEntity<RegistrationResponse> register(@Valid @RequestBody RegisterRequest request) {
        log.info("Registration request received for phone number");
        
        try {
            RegistrationResponse response = mobileAuthService.registerUser(request.getPhoneNumber());
            log.info("Registration successful for user: {}", response.getUserId());
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (IllegalArgumentException e) {
            log.warn("Registration failed: {}", e.getMessage());
            throw e;
        }
    }

    /**
     * Verify mobile OTP during registration.
     * Completes the registration process and returns authentication tokens.
     * 
     * POST /auth/verify-mobile-otp
     * 
     * @param request VerifyOTPRequest containing phone number and OTP
     * @return AuthResponse with JWT tokens and verification status
     */
    @PostMapping("/verify-mobile-otp")
    public ResponseEntity<AuthResponse> verifyMobileOTP(@Valid @RequestBody VerifyOTPRequest request) {
        log.info("Mobile OTP verification request received");
        
        try {
            AuthResponse response = mobileAuthService.verifyOTP(
                request.getPhoneNumber(), 
                request.getOtp()
            );
            log.info("Mobile OTP verification successful for user: {}", response.getUserId());
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            log.warn("Mobile OTP verification failed: {}", e.getMessage());
            throw e;
        }
    }

    /**
     * Login with mobile number.
     * Sends OTP to the registered phone number.
     * 
     * POST /auth/login
     * 
     * @param request LoginRequest containing phone number
     * @return RegistrationResponse with OTP status
     */
    @PostMapping("/login")
    public ResponseEntity<RegistrationResponse> login(@Valid @RequestBody LoginRequest request) {
        log.info("Login request received");
        
        try {
            RegistrationResponse response = mobileAuthService.loginUser(request.getPhoneNumber());
            log.info("Login OTP sent for user: {}", response.getUserId());
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            log.warn("Login failed: {}", e.getMessage());
            throw e;
        }
    }

    /**
     * Verify login OTP.
     * Completes the login process and returns authentication tokens.
     * 
     * POST /auth/verify-login-otp
     * 
     * @param request VerifyOTPRequest containing phone number and OTP
     * @return AuthResponse with JWT tokens and verification status
     */
    @PostMapping("/verify-login-otp")
    public ResponseEntity<AuthResponse> verifyLoginOTP(@Valid @RequestBody VerifyOTPRequest request) {
        log.info("Login OTP verification request received");
        
        try {
            AuthResponse response = mobileAuthService.verifyLoginOTP(
                request.getPhoneNumber(), 
                request.getOtp()
            );
            log.info("Login OTP verification successful for user: {}", response.getUserId());
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            log.warn("Login OTP verification failed: {}", e.getMessage());
            throw e;
        }
    }

    /**
     * Refresh access token using refresh token.
     * Generates a new access token without requiring re-authentication.
     * 
     * POST /auth/refresh
     * 
     * @param request RefreshTokenRequest containing refresh token
     * @return SessionTokens with new access token
     */
    @PostMapping("/refresh")
    public ResponseEntity<SessionTokens> refresh(@Valid @RequestBody RefreshTokenRequest request) {
        log.info("Token refresh request received");
        
        try {
            // Validate refresh token and extract user ID
            TokenValidation validation = sessionManagementService.validateToken(request.getRefreshToken());
            
            if (!validation.isValid()) {
                log.warn("Invalid refresh token: {}", validation.getErrorMessage());
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
            }
            
            // Get user account for updated permissions
            UUID userId = UUID.fromString(validation.getUserId());
            UserAccount userAccount = userAccountRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
            
            // Refresh session with updated permissions
            SessionTokens tokens = sessionManagementService.refreshSession(
                request.getRefreshToken(), 
                userAccount
            );
            
            log.info("Token refresh successful for user: {}", userId);
            return ResponseEntity.ok(tokens);
        } catch (IllegalArgumentException e) {
            log.warn("Token refresh failed: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
    }

    /**
     * Logout and terminate session.
     * Revokes the current session and invalidates tokens.
     * 
     * POST /auth/logout
     * 
     * @param request HttpServletRequest to extract session information
     * @return Success message
     */
    @PostMapping("/logout")
    public ResponseEntity<Map<String, String>> logout(HttpServletRequest request) {
        log.info("Logout request received");
        
        try {
            // Get authentication from security context
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            
            if (authentication != null && authentication.isAuthenticated()) {
                // Extract session ID from authentication details
                Object details = authentication.getDetails();
                String sessionId = null;
                
                if (details instanceof Map) {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> detailsMap = (Map<String, Object>) details;
                    sessionId = (String) detailsMap.get("sessionId");
                }
                
                // Revoke session if session ID is available
                if (sessionId != null) {
                    sessionManagementService.revokeSession(sessionId);
                    log.info("Session revoked successfully: {}", sessionId);
                } else {
                    log.warn("No session ID found in authentication details");
                }
            }
            
            Map<String, String> response = new HashMap<>();
            response.put("message", "Logout successful");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Logout failed", e);
            Map<String, String> response = new HashMap<>();
            response.put("message", "Logout completed with warnings");
            return ResponseEntity.ok(response);
        }
    }

    /**
     * Add corporate email to user account.
     * Sends OTP to the provided corporate email for verification.
     * 
     * POST /auth/add-corporate-email
     * 
     * @param request AddCorporateEmailRequest containing corporate email
     * @return VerificationResponse with OTP delivery status
     */
    @PostMapping("/add-corporate-email")
    public ResponseEntity<VerificationResponse> addCorporateEmail(@Valid @RequestBody AddCorporateEmailRequest request) {
        log.info("Add corporate email request received");
        
        try {
            // Get authenticated user ID from security context
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            
            if (authentication == null || !authentication.isAuthenticated()) {
                log.warn("Unauthenticated request to add corporate email");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
            }
            
            // Extract user ID from authentication principal
            String userId = authentication.getName();
            
            // Send email OTP
            VerificationResponse response = emailVerificationService.sendEmailOTP(
                UUID.fromString(userId), 
                request.getCorporateEmail()
            );
            
            log.info("Corporate email OTP sent successfully for user: {}", userId);
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            log.warn("Add corporate email failed: {}", e.getMessage());
            throw e;
        }
    }

    /**
     * Verify email OTP and complete corporate email verification.
     * Updates user account with verified corporate email.
     * 
     * POST /auth/verify-email-otp
     * 
     * @param request VerifyEmailOtpRequest containing OTP
     * @return VerificationResponse with verification result
     */
    @PostMapping("/verify-email-otp")
    public ResponseEntity<VerificationResponse> verifyEmailOTP(@Valid @RequestBody VerifyEmailOtpRequest request) {
        log.info("Email OTP verification request received");
        
        try {
            // Get authenticated user ID from security context
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            
            if (authentication == null || !authentication.isAuthenticated()) {
                log.warn("Unauthenticated request to verify email OTP");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
            }
            
            // Extract user ID from authentication principal
            String userId = authentication.getName();
            
            // Verify email OTP
            VerificationResponse response = emailVerificationService.verifyEmailOTP(
                UUID.fromString(userId), 
                request.getOtp()
            );
            
            log.info("Email OTP verification successful for user: {}", userId);
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            log.warn("Email OTP verification failed: {}", e.getMessage());
            throw e;
        }
    }

    /**
     * Resend email OTP for corporate email verification.
     * Generates and sends a new OTP to the pending corporate email.
     * 
     * POST /auth/resend-email-otp
     * 
     * @return VerificationResponse with OTP delivery status
     */
    @PostMapping("/resend-email-otp")
    public ResponseEntity<VerificationResponse> resendEmailOTP() {
        log.info("Resend email OTP request received");
        
        try {
            // Get authenticated user ID from security context
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            
            if (authentication == null || !authentication.isAuthenticated()) {
                log.warn("Unauthenticated request to resend email OTP");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
            }
            
            // Extract user ID from authentication principal
            String userId = authentication.getName();
            
            // Resend email OTP
            VerificationResponse response = emailVerificationService.resendEmailOTP(
                UUID.fromString(userId)
            );
            
            log.info("Email OTP resent successfully for user: {}", userId);
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            log.warn("Resend email OTP failed: {}", e.getMessage());
            throw e;
        }
    }

    /**
     * Update corporate email (for company changes).
     * Requires mobile OTP verification for security.
     * Step 1: Verifies mobile OTP and sends OTP to new email.
     * Step 2: Client must call verify-email-otp to complete the update.
     * 
     * POST /auth/update-corporate-email
     * 
     * @param request UpdateCorporateEmailRequest containing mobile OTP and new email
     * @param httpRequest HttpServletRequest for audit trail (IP, user agent)
     * @return VerificationResponse with OTP delivery status for new email
     */
    @PostMapping("/update-corporate-email")
    public ResponseEntity<VerificationResponse> updateCorporateEmail(
            @Valid @RequestBody UpdateCorporateEmailRequest request,
            HttpServletRequest httpRequest) {
        
        log.info("Update corporate email request received");
        
        try {
            // Get authenticated user ID from security context
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            
            if (authentication == null || !authentication.isAuthenticated()) {
                log.warn("Unauthenticated request to update corporate email");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
            }
            
            // Extract user ID from authentication principal
            String userId = authentication.getName();
            
            // Extract client information for audit trail
            String ipAddress = getClientIpAddress(httpRequest);
            String userAgent = httpRequest.getHeader("User-Agent");
            
            // Initiate email update workflow
            VerificationResponse response = emailVerificationService.initiateEmailUpdate(
                UUID.fromString(userId),
                request.getMobileOtp(),
                request.getNewCorporateEmail(),
                request.getChangeReason(),
                ipAddress,
                userAgent
            );
            
            log.info("Corporate email update initiated for user: {}", userId);
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            log.warn("Update corporate email failed: {}", e.getMessage());
            throw e;
        }
    }

    /**
     * Health check endpoint for authentication service.
     * 
     * GET /auth/health
     * 
     * @return Health status
     */
    @GetMapping("/health")
    public ResponseEntity<Map<String, String>> health() {
        Map<String, String> response = new HashMap<>();
        response.put("status", "UP");
        response.put("service", "authentication");
        return ResponseEntity.ok(response);
    }

    /**
     * Extract client IP address from HTTP request.
     * Checks X-Forwarded-For header for proxy scenarios.
     * 
     * @param request HttpServletRequest
     * @return Client IP address
     */
    private String getClientIpAddress(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            // X-Forwarded-For can contain multiple IPs, take the first one
            return xForwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
