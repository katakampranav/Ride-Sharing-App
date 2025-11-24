package com.officemate.config.security;

import com.officemate.shared.validation.RequireFullVerification;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * REST controller for security management operations.
 * Provides endpoints for managing rate limits, account lockouts, and CAPTCHA challenges.
 */
@RestController
@RequestMapping("/security")
@RequiredArgsConstructor
@Slf4j
public class SecurityManagementController {

    private final RateLimitingService rateLimitingService;
    private final CaptchaService captchaService;
    private final AuthenticationPatternMonitoringService patternMonitoringService;

    /**
     * Check if CAPTCHA is required for a user.
     */
    @PostMapping("/captcha/check")
    public ResponseEntity<Map<String, Object>> checkCaptchaRequirement(
            @RequestBody Map<String, String> request) {
        
        String identifier = request.get("identifier");
        String ipAddress = request.get("ipAddress");
        
        Map<String, Object> response = new HashMap<>();
        
        try {
            boolean requireCaptcha = rateLimitingService.shouldRequireCaptcha(identifier, ipAddress);
            response.put("required", requireCaptcha);
            
            if (requireCaptcha && !captchaService.hasCaptchaBypass(identifier)) {
                Map<String, Object> challenge = captchaService.generateCaptchaChallenge(identifier);
                response.putAll(challenge);
            } else {
                response.put("message", "CAPTCHA not required or bypass active");
            }
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("Error checking CAPTCHA requirement: {}", e.getMessage(), e);
            response.put("error", "Unable to check CAPTCHA requirement");
            return ResponseEntity.internalServerError().body(response);
        }
    }

    /**
     * Verify CAPTCHA response.
     */
    @PostMapping("/captcha/verify")
    public ResponseEntity<Map<String, Object>> verifyCaptcha(
            @RequestBody Map<String, String> request,
            @RequestHeader(value = "X-Forwarded-For", required = false) String xForwardedFor,
            @RequestHeader(value = "X-Real-IP", required = false) String xRealIp,
            jakarta.servlet.http.HttpServletRequest httpRequest) {
        
        String challengeId = request.get("challengeId");
        String captchaResponse = request.get("captchaResponse");
        
        // Get client IP
        String clientIp = xForwardedFor != null ? xForwardedFor.split(",")[0].trim() :
                         xRealIp != null ? xRealIp : httpRequest.getRemoteAddr();
        
        Map<String, Object> response = new HashMap<>();
        
        try {
            boolean isValid = captchaService.verifyCaptcha(challengeId, captchaResponse, clientIp);
            
            response.put("valid", isValid);
            response.put("message", isValid ? "CAPTCHA verification successful" : "CAPTCHA verification failed");
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("Error verifying CAPTCHA: {}", e.getMessage(), e);
            response.put("valid", false);
            response.put("error", "CAPTCHA verification error");
            return ResponseEntity.internalServerError().body(response);
        }
    }

    /**
     * Check account lockout status.
     */
    @GetMapping("/lockout/status/{identifier}")
    public ResponseEntity<Map<String, Object>> checkLockoutStatus(@PathVariable String identifier) {
        Map<String, Object> response = new HashMap<>();
        
        try {
            boolean isLocked = rateLimitingService.isAccountLocked(identifier);
            response.put("locked", isLocked);
            
            if (isLocked) {
                LocalDateTime lockoutEnd = rateLimitingService.getLockoutEndTime(identifier);
                if (lockoutEnd != null) {
                    response.put("lockedUntil", lockoutEnd.toString());
                }
                response.put("message", "Account is temporarily locked");
            } else {
                response.put("message", "Account is not locked");
            }
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("Error checking lockout status: {}", e.getMessage(), e);
            response.put("error", "Unable to check lockout status");
            return ResponseEntity.internalServerError().body(response);
        }
    }

    /**
     * Get authentication risk assessment for a user.
     */
    @GetMapping("/risk-assessment/{userId}")
    @RequireFullVerification
    public ResponseEntity<Map<String, Object>> getRiskAssessment(@PathVariable String userId) {
        try {
            Map<String, Object> assessment = patternMonitoringService.getAuthenticationRiskAssessment(userId);
            return ResponseEntity.ok(assessment);
            
        } catch (Exception e) {
            log.error("Error getting risk assessment: {}", e.getMessage(), e);
            Map<String, Object> response = new HashMap<>();
            response.put("error", "Unable to retrieve risk assessment");
            return ResponseEntity.internalServerError().body(response);
        }
    }

    /**
     * Admin endpoint to unlock an account.
     */
    @PostMapping("/admin/unlock")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> unlockAccount(
            @RequestBody Map<String, String> request) {
        
        String identifier = request.get("identifier");
        Map<String, Object> response = new HashMap<>();
        
        try {
            rateLimitingService.unlockAccount(identifier);
            response.put("success", true);
            response.put("message", "Account unlocked successfully");
            
            log.info("Account unlocked by admin: {}", identifier);
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("Error unlocking account: {}", e.getMessage(), e);
            response.put("success", false);
            response.put("error", "Unable to unlock account");
            return ResponseEntity.internalServerError().body(response);
        }
    }

    /**
     * Admin endpoint to reset rate limits.
     */
    @PostMapping("/admin/reset-rate-limit")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> resetRateLimit(
            @RequestBody Map<String, String> request) {
        
        String identifier = request.get("identifier");
        Map<String, Object> response = new HashMap<>();
        
        try {
            rateLimitingService.resetRateLimit(identifier);
            rateLimitingService.clearFailedAttempts(identifier);
            
            response.put("success", true);
            response.put("message", "Rate limit reset successfully");
            
            log.info("Rate limit reset by admin for: {}", identifier);
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("Error resetting rate limit: {}", e.getMessage(), e);
            response.put("success", false);
            response.put("error", "Unable to reset rate limit");
            return ResponseEntity.internalServerError().body(response);
        }
    }

    /**
     * Get rate limit status for an identifier.
     */
    @GetMapping("/rate-limit/status/{identifier}")
    public ResponseEntity<Map<String, Object>> getRateLimitStatus(@PathVariable String identifier) {
        Map<String, Object> response = new HashMap<>();
        
        try {
            // Check various rate limits
            boolean otpAllowed = rateLimitingService.isOtpRequestAllowed(identifier, 5);
            boolean loginAllowed = rateLimitingService.isLoginAttemptAllowed(identifier, 10);
            boolean apiAllowed = rateLimitingService.isApiRequestAllowed(identifier, 60);
            
            response.put("otpRequestsAllowed", otpAllowed);
            response.put("loginAttemptsAllowed", loginAllowed);
            response.put("apiRequestsAllowed", apiAllowed);
            
            // Get remaining requests
            response.put("remainingOtpRequests", rateLimitingService.getRemainingRequests("otp_limit:" + identifier, 5));
            response.put("remainingLoginAttempts", rateLimitingService.getRemainingRequests("login_limit:" + identifier, 10));
            response.put("remainingApiRequests", rateLimitingService.getRemainingRequests("api:" + identifier, 60));
            
            // Get time until reset
            response.put("otpResetTime", rateLimitingService.getTimeUntilReset("otp_limit:" + identifier));
            response.put("loginResetTime", rateLimitingService.getTimeUntilReset("login_limit:" + identifier));
            response.put("apiResetTime", rateLimitingService.getTimeUntilReset("api:" + identifier));
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("Error getting rate limit status: {}", e.getMessage(), e);
            response.put("error", "Unable to retrieve rate limit status");
            return ResponseEntity.internalServerError().body(response);
        }
    }
}