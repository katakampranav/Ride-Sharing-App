package com.officemate.modules.auth.service;

import com.google.i18n.phonenumbers.NumberParseException;
import com.google.i18n.phonenumbers.PhoneNumberUtil;
import com.google.i18n.phonenumbers.Phonenumber.PhoneNumber;
import com.officemate.modules.auth.entity.UserAccount;
import com.officemate.modules.auth.repository.UserAccountRepository;
import com.officemate.shared.dto.AuthResponse;
import com.officemate.shared.dto.RegistrationResponse;
import com.officemate.shared.dto.SessionTokens;
import com.officemate.shared.enums.AccountStatus;
import com.officemate.shared.service.SecurityEventService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * Service for mobile authentication operations including registration and OTP verification.
 * Handles phone number validation using libphonenumber and PostgreSQL storage.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class MobileAuthService {

    private final UserAccountRepository userAccountRepository;
    private final OTPService otpService;
    private final SessionManagementService sessionManagementService;
    private final SecurityEventService securityEventService;
    private final com.officemate.config.security.RateLimitingService rateLimitingService;
    private final com.officemate.config.security.AuthenticationPatternMonitoringService patternMonitoringService;
    private final PhoneNumberUtil phoneNumberUtil = PhoneNumberUtil.getInstance();

    /**
     * Register a new user with phone number validation.
     * Creates a new UserAccount in PostgreSQL and initiates OTP verification.
     *
     * @param phoneNumber the phone number to register (E.164 format recommended)
     * @return RegistrationResponse with user ID and OTP status
     * @throws IllegalArgumentException if phone number is invalid or already exists
     */
    @Transactional
    public RegistrationResponse registerUser(String phoneNumber) {
        log.info("Starting registration for phone number: {}", maskPhoneNumber(phoneNumber));

        // Validate phone number format
        String validatedPhoneNumber = validateAndFormatPhoneNumber(phoneNumber);

        // Check if account is locked
        if (rateLimitingService.isAccountLocked(validatedPhoneNumber)) {
            log.warn("Registration blocked: Account locked for phone: {}", maskPhoneNumber(validatedPhoneNumber));
            throw new IllegalArgumentException("Account temporarily locked due to suspicious activity");
        }

        // Check rate limits for registration
        if (!rateLimitingService.isOtpRequestAllowed(validatedPhoneNumber, 5)) {
            log.warn("Registration blocked: Rate limit exceeded for phone: {}", maskPhoneNumber(validatedPhoneNumber));
            rateLimitingService.trackSuspiciousActivity(validatedPhoneNumber, "EXCESSIVE_REGISTRATION_ATTEMPTS", null);
            throw new IllegalArgumentException("Too many registration attempts. Please try again later.");
        }

        // Check for duplicate phone number
        if (userAccountRepository.existsByPhoneNumber(validatedPhoneNumber)) {
            log.warn("Registration failed: Phone number already exists: {}", maskPhoneNumber(validatedPhoneNumber));
            throw new IllegalArgumentException("Phone number already registered");
        }

        // Create new user account in PostgreSQL
        UserAccount userAccount = UserAccount.builder()
                .phoneNumber(validatedPhoneNumber)
                .phoneVerified(false)
                .emailVerified(false)
                .accountStatus(AccountStatus.PENDING_EMAIL)
                .build();

        UserAccount savedAccount = userAccountRepository.save(userAccount);
        log.info("User account created successfully with ID: {}", savedAccount.getUserId());

        // Generate OTP and store in Redis
        String otp = otpService.generateMobileOTP(validatedPhoneNumber);
        LocalDateTime otpExpiresAt = LocalDateTime.now().plusMinutes(5);

        // TODO: Send OTP via AWS SNS (will be implemented in later task)
        log.info("OTP generated for registration: {} (not sent yet - SMS integration pending)", maskPhoneNumber(validatedPhoneNumber));

        return RegistrationResponse.builder()
                .userId(savedAccount.getUserId().toString())
                .otpSent(true)
                .expiresAt(otpExpiresAt)
                .maskedPhoneNumber(maskPhoneNumber(validatedPhoneNumber))
                .build();
    }

    /**
     * Verify OTP and complete phone verification.
     * Updates the UserAccount to mark phone as verified.
     *
     * @param phoneNumber the phone number being verified
     * @param otp the OTP code to verify
     * @return AuthResponse with authentication tokens and verification status
     * @throws IllegalArgumentException if phone number or OTP is invalid
     */
    @Transactional
    public AuthResponse verifyOTP(String phoneNumber, String otp) {
        log.info("Verifying OTP for phone number: {}", maskPhoneNumber(phoneNumber));

        // Validate phone number format
        String validatedPhoneNumber = validateAndFormatPhoneNumber(phoneNumber);

        // Check if account is locked
        if (rateLimitingService.isAccountLocked(validatedPhoneNumber)) {
            log.warn("OTP verification blocked: Account locked for phone: {}", maskPhoneNumber(validatedPhoneNumber));
            throw new IllegalArgumentException("Account temporarily locked due to suspicious activity");
        }

        // Find user account
        UserAccount userAccount = userAccountRepository.findByPhoneNumber(validatedPhoneNumber)
                .orElseThrow(() -> new IllegalArgumentException("User account not found"));

        // Verify OTP from Redis
        boolean isValid = otpService.verifyMobileOTP(validatedPhoneNumber, otp);
        
        if (!isValid) {
            int remainingAttempts = otpService.getRemainingAttempts(validatedPhoneNumber);
            log.warn("Invalid OTP for phone: {}. Remaining attempts: {}", 
                    maskPhoneNumber(validatedPhoneNumber), remainingAttempts);
            
            // Record failed attempt and check for lockout
            boolean shouldLock = rateLimitingService.recordFailedAttempt(
                    validatedPhoneNumber, "OTP_VERIFICATION", userAccount.getUserId().toString());
            
            // Log security event for failed OTP verification
            securityEventService.logFailedOtpVerification(userAccount.getUserId(), validatedPhoneNumber, 
                    "MOBILE", "Invalid OTP provided. Remaining attempts: " + remainingAttempts);
            
            if (shouldLock) {
                throw new IllegalArgumentException("Account locked due to too many failed attempts");
            }
            
            throw new IllegalArgumentException("Invalid OTP. Remaining attempts: " + remainingAttempts);
        }

        // Clear failed attempts after successful verification
        rateLimitingService.clearFailedAttempts(validatedPhoneNumber);

        // Mark phone as verified and update last login
        userAccount.verifyPhone();
        userAccount.updateLastLogin();
        userAccountRepository.save(userAccount);

        // Delete OTP after successful verification
        otpService.deleteMobileOTP(validatedPhoneNumber);

        log.info("Phone verification successful for user: {}", userAccount.getUserId());

        // Log successful authentication
        securityEventService.logSuccessfulLogin(userAccount.getUserId(), validatedPhoneNumber);

        // Analyze authentication patterns for suspicious behavior
        patternMonitoringService.analyzeAuthenticationPattern(
                userAccount.getUserId().toString(), 
                validatedPhoneNumber, 
                getCurrentClientIp(), 
                getCurrentUserAgent(), 
                getCurrentDeviceInfo()
        );

        // Create session with JWT tokens
        SessionTokens tokens = sessionManagementService.createSession(userAccount, null);

        return AuthResponse.builder()
                .accessToken(tokens.getAccessToken())
                .refreshToken(tokens.getRefreshToken())
                .userId(userAccount.getUserId().toString())
                .mobileVerified(userAccount.getPhoneVerified())
                .emailVerified(userAccount.getEmailVerified())
                .profileComplete(false)
                .expiresAt(tokens.getExpiresAt())
                .build();
    }

    /**
     * Validate and format phone number using libphonenumber.
     * Attempts to parse with default region, then tries international format.
     *
     * @param phoneNumber the phone number to validate
     * @return validated phone number in E.164 format
     * @throws IllegalArgumentException if phone number is invalid
     */
    private String validateAndFormatPhoneNumber(String phoneNumber) {
        if (phoneNumber == null || phoneNumber.trim().isEmpty()) {
            throw new IllegalArgumentException("Phone number cannot be empty");
        }

        String trimmedNumber = phoneNumber.trim();

        try {
            PhoneNumber parsedNumber;

            // Try parsing with default region (India - IN)
            // This can be made configurable based on application requirements
            if (!trimmedNumber.startsWith("+")) {
                parsedNumber = phoneNumberUtil.parse(trimmedNumber, "IN");
            } else {
                // Parse as international format
                parsedNumber = phoneNumberUtil.parse(trimmedNumber, null);
            }

            // Validate the parsed number
            if (!phoneNumberUtil.isValidNumber(parsedNumber)) {
                throw new IllegalArgumentException("Invalid phone number");
            }

            // Format to E.164 standard (+[country code][number])
            String formattedNumber = phoneNumberUtil.format(parsedNumber, PhoneNumberUtil.PhoneNumberFormat.E164);
            log.debug("Phone number validated and formatted: {}", maskPhoneNumber(formattedNumber));

            return formattedNumber;

        } catch (NumberParseException e) {
            log.error("Phone number validation failed: {}", e.getMessage());
            throw new IllegalArgumentException("Invalid phone number format: " + e.getMessage());
        }
    }

    /**
     * Mask phone number for logging (shows only last 4 digits).
     *
     * @param phoneNumber the phone number to mask
     * @return masked phone number
     */
    private String maskPhoneNumber(String phoneNumber) {
        if (phoneNumber == null || phoneNumber.length() < 4) {
            return "****";
        }
        return "****" + phoneNumber.substring(phoneNumber.length() - 4);
    }

    /**
     * Login user with phone number and OTP.
     * Similar to registration but for existing users.
     *
     * @param phoneNumber the phone number to login with
     * @return RegistrationResponse with OTP status
     * @throws IllegalArgumentException if phone number is not registered
     */
    @Transactional(readOnly = true)
    public RegistrationResponse loginUser(String phoneNumber) {
        log.info("Login attempt for phone number: {}", maskPhoneNumber(phoneNumber));

        // Validate phone number format
        String validatedPhoneNumber = validateAndFormatPhoneNumber(phoneNumber);

        // Check if account is locked
        if (rateLimitingService.isAccountLocked(validatedPhoneNumber)) {
            log.warn("Login blocked: Account locked for phone: {}", maskPhoneNumber(validatedPhoneNumber));
            throw new IllegalArgumentException("Account temporarily locked due to suspicious activity");
        }

        // Check rate limits for login attempts
        if (!rateLimitingService.isLoginAttemptAllowed(validatedPhoneNumber, 10)) {
            log.warn("Login blocked: Rate limit exceeded for phone: {}", maskPhoneNumber(validatedPhoneNumber));
            rateLimitingService.trackSuspiciousActivity(validatedPhoneNumber, "EXCESSIVE_LOGIN_ATTEMPTS", null);
            throw new IllegalArgumentException("Too many login attempts. Please try again later.");
        }

        // Check if user exists
        UserAccount userAccount = userAccountRepository.findByPhoneNumber(validatedPhoneNumber)
                .orElseThrow(() -> new IllegalArgumentException("Phone number not registered"));

        // Check if account is suspended
        if (userAccount.getAccountStatus() == AccountStatus.SUSPENDED) {
            throw new IllegalArgumentException("Account is suspended");
        }

        // Generate OTP and store in Redis
        String otp = otpService.generateMobileOTP(validatedPhoneNumber);
        LocalDateTime otpExpiresAt = LocalDateTime.now().plusMinutes(5);

        // TODO: Send OTP via AWS SNS (will be implemented in later task)
        log.info("Login OTP generated for user: {} (not sent yet - SMS integration pending)", userAccount.getUserId());

        return RegistrationResponse.builder()
                .userId(userAccount.getUserId().toString())
                .otpSent(true)
                .expiresAt(otpExpiresAt)
                .maskedPhoneNumber(maskPhoneNumber(validatedPhoneNumber))
                .build();
    }

    /**
     * Verify login OTP and generate authentication tokens.
     * This method is used for login flow (existing users).
     *
     * @param phoneNumber the phone number being verified
     * @param otp the OTP code to verify
     * @return AuthResponse with JWT tokens and verification status
     * @throws IllegalArgumentException if phone number or OTP is invalid
     */
    @Transactional
    public AuthResponse verifyLoginOTP(String phoneNumber, String otp) {
        log.info("Verifying login OTP for phone number: {}", maskPhoneNumber(phoneNumber));

        // Validate phone number format
        String validatedPhoneNumber = validateAndFormatPhoneNumber(phoneNumber);

        // Check if account is locked
        if (rateLimitingService.isAccountLocked(validatedPhoneNumber)) {
            log.warn("Login OTP verification blocked: Account locked for phone: {}", maskPhoneNumber(validatedPhoneNumber));
            throw new IllegalArgumentException("Account temporarily locked due to suspicious activity");
        }

        // Find user account
        UserAccount userAccount = userAccountRepository.findByPhoneNumber(validatedPhoneNumber)
                .orElseThrow(() -> new IllegalArgumentException("User account not found"));

        // Check if account is suspended
        if (userAccount.getAccountStatus() == AccountStatus.SUSPENDED) {
            throw new IllegalArgumentException("Account is suspended");
        }

        // Verify OTP from Redis
        boolean isValid = otpService.verifyMobileOTP(validatedPhoneNumber, otp);
        
        if (!isValid) {
            int remainingAttempts = otpService.getRemainingAttempts(validatedPhoneNumber);
            log.warn("Invalid login OTP for phone: {}. Remaining attempts: {}", 
                    maskPhoneNumber(validatedPhoneNumber), remainingAttempts);
            
            // Record failed attempt and check for lockout
            boolean shouldLock = rateLimitingService.recordFailedAttempt(
                    validatedPhoneNumber, "LOGIN_OTP_VERIFICATION", userAccount.getUserId().toString());
            
            // Log security event for failed login OTP verification
            securityEventService.logFailedOtpVerification(userAccount.getUserId(), validatedPhoneNumber, 
                    "LOGIN", "Invalid login OTP provided. Remaining attempts: " + remainingAttempts);
            
            if (shouldLock) {
                throw new IllegalArgumentException("Account locked due to too many failed attempts");
            }
            
            throw new IllegalArgumentException("Invalid OTP. Remaining attempts: " + remainingAttempts);
        }

        // Clear failed attempts after successful verification
        rateLimitingService.clearFailedAttempts(validatedPhoneNumber);

        // Update last login timestamp
        userAccount.updateLastLogin();
        userAccountRepository.save(userAccount);

        // Delete OTP after successful verification
        otpService.deleteMobileOTP(validatedPhoneNumber);

        log.info("Login successful for user: {}. Mobile verified: {}, Email verified: {}", 
                userAccount.getUserId(), userAccount.getPhoneVerified(), userAccount.getEmailVerified());

        // Log successful login
        securityEventService.logSuccessfulLogin(userAccount.getUserId(), validatedPhoneNumber);

        // Analyze authentication patterns for suspicious behavior
        patternMonitoringService.analyzeAuthenticationPattern(
                userAccount.getUserId().toString(), 
                validatedPhoneNumber, 
                getCurrentClientIp(), 
                getCurrentUserAgent(), 
                getCurrentDeviceInfo()
        );

        // Create session with JWT tokens
        SessionTokens tokens = sessionManagementService.createSession(userAccount, null);

        return AuthResponse.builder()
                .accessToken(tokens.getAccessToken())
                .refreshToken(tokens.getRefreshToken())
                .userId(userAccount.getUserId().toString())
                .mobileVerified(userAccount.getPhoneVerified())
                .emailVerified(userAccount.getEmailVerified())
                .profileComplete(userAccount.isFullyVerified())
                .expiresAt(tokens.getExpiresAt())
                .build();
    }

    /**
     * Get current client IP address from request context.
     */
    private String getCurrentClientIp() {
        try {
            org.springframework.web.context.request.ServletRequestAttributes attributes = 
                    (org.springframework.web.context.request.ServletRequestAttributes) 
                    org.springframework.web.context.request.RequestContextHolder.currentRequestAttributes();
            jakarta.servlet.http.HttpServletRequest request = attributes.getRequest();
            
            String xForwardedFor = request.getHeader("X-Forwarded-For");
            if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
                return xForwardedFor.split(",")[0].trim();
            }
            
            String xRealIp = request.getHeader("X-Real-IP");
            if (xRealIp != null && !xRealIp.isEmpty()) {
                return xRealIp;
            }
            
            return request.getRemoteAddr();
        } catch (Exception e) {
            return "UNKNOWN";
        }
    }

    /**
     * Get current user agent from request context.
     */
    private String getCurrentUserAgent() {
        try {
            org.springframework.web.context.request.ServletRequestAttributes attributes = 
                    (org.springframework.web.context.request.ServletRequestAttributes) 
                    org.springframework.web.context.request.RequestContextHolder.currentRequestAttributes();
            jakarta.servlet.http.HttpServletRequest request = attributes.getRequest();
            return request.getHeader("User-Agent");
        } catch (Exception e) {
            return "UNKNOWN";
        }
    }

    /**
     * Get current device information from request context.
     */
    private java.util.Map<String, String> getCurrentDeviceInfo() {
        java.util.Map<String, String> deviceInfo = new java.util.HashMap<>();
        
        try {
            org.springframework.web.context.request.ServletRequestAttributes attributes = 
                    (org.springframework.web.context.request.ServletRequestAttributes) 
                    org.springframework.web.context.request.RequestContextHolder.currentRequestAttributes();
            jakarta.servlet.http.HttpServletRequest request = attributes.getRequest();
            
            deviceInfo.put("userAgent", request.getHeader("User-Agent"));
            deviceInfo.put("acceptLanguage", request.getHeader("Accept-Language"));
            deviceInfo.put("acceptEncoding", request.getHeader("Accept-Encoding"));
            deviceInfo.put("deviceType", request.getHeader("X-Device-Type"));
            deviceInfo.put("appVersion", request.getHeader("X-App-Version"));
            
        } catch (Exception e) {
            deviceInfo.put("error", "Unable to retrieve device info");
        }
        
        return deviceInfo;
    }
}
