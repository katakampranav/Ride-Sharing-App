package com.officemate.shared.service;

import com.officemate.shared.entity.SecurityEventLog;
import com.officemate.shared.repository.SecurityEventLogRepository;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.scheduling.annotation.Async;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Service for logging security events and failed authentication attempts
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class SecurityEventService {

    private final SecurityEventLogRepository securityEventLogRepository;

    /**
     * Log a failed login attempt
     */
    @Async
    public void logFailedLogin(String phoneNumber, String reason, String additionalData) {
        logSecurityEvent(null, phoneNumber, null, "LOGIN_FAILURE", 
                        "Failed login attempt: " + reason, "MEDIUM", additionalData);
    }

    /**
     * Log a successful login
     */
    @Async
    public void logSuccessfulLogin(UUID userId, String phoneNumber) {
        logSecurityEvent(userId, phoneNumber, null, "LOGIN_SUCCESS", 
                        "Successful login", "LOW", null);
    }

    /**
     * Log a failed OTP verification
     */
    @Async
    public void logFailedOtpVerification(UUID userId, String phoneNumber, String otpType, String reason) {
        logSecurityEvent(userId, phoneNumber, null, "OTP_FAILURE", 
                        "Failed OTP verification (" + otpType + "): " + reason, "MEDIUM", null);
    }

    /**
     * Log account lockout
     */
    @Async
    public void logAccountLockout(UUID userId, String phoneNumber, String reason) {
        logSecurityEvent(userId, phoneNumber, null, "ACCOUNT_LOCKED", 
                        "Account locked: " + reason, "HIGH", null);
    }

    /**
     * Log suspicious activity
     */
    @Async
    public void logSuspiciousActivity(UUID userId, String phoneNumber, String corporateEmail, 
                                    String activityType, String description, String severity) {
        logSecurityEvent(userId, phoneNumber, corporateEmail, "SUSPICIOUS_ACTIVITY", 
                        activityType + ": " + description, severity, null);
    }

    /**
     * Log email verification failure
     */
    @Async
    public void logEmailVerificationFailure(UUID userId, String corporateEmail, String reason) {
        logSecurityEvent(userId, null, corporateEmail, "EMAIL_VERIFICATION_FAILURE", 
                        "Email verification failed: " + reason, "MEDIUM", null);
    }

    /**
     * Log password reset attempt
     */
    @Async
    public void logPasswordResetAttempt(String phoneNumber, String reason) {
        logSecurityEvent(null, phoneNumber, null, "PASSWORD_RESET_ATTEMPT", 
                        "Password reset attempt: " + reason, "LOW", null);
    }

    /**
     * Log session hijacking attempt
     */
    @Async
    public void logSessionHijackingAttempt(UUID userId, String sessionId, String reason) {
        logSecurityEvent(userId, null, null, "SESSION_HIJACKING", 
                        "Potential session hijacking: " + reason, "CRITICAL", 
                        "sessionId=" + sessionId);
    }

    /**
     * Log rate limiting violation
     */
    @Async
    public void logRateLimitViolation(String phoneNumber, String endpoint, String reason) {
        logSecurityEvent(null, phoneNumber, null, "RATE_LIMIT_VIOLATION", 
                        "Rate limit exceeded for " + endpoint + ": " + reason, "MEDIUM", null);
    }

    /**
     * Log token manipulation attempt
     */
    @Async
    public void logTokenManipulation(UUID userId, String tokenType, String reason) {
        logSecurityEvent(userId, null, null, "TOKEN_MANIPULATION", 
                        "Token manipulation detected (" + tokenType + "): " + reason, "HIGH", null);
    }

    /**
     * Generic method to log security events
     */
    private void logSecurityEvent(UUID userId, String phoneNumber, String corporateEmail, 
                                String eventType, String description, String severity, String additionalData) {
        try {
            HttpServletRequest request = getCurrentRequest();
            String sessionId = getCurrentSessionId();
            
            SecurityEventLog securityEvent = SecurityEventLog.builder()
                .userId(userId)
                .phoneNumber(phoneNumber)
                .corporateEmail(corporateEmail)
                .eventType(eventType)
                .eventDescription(description)
                .ipAddress(getClientIpAddress(request))
                .userAgent(getUserAgent(request))
                .sessionId(sessionId)
                .severity(severity)
                .additionalData(additionalData)
                .timestamp(LocalDateTime.now())
                .build();

            securityEventLogRepository.save(securityEvent);
            
            // Log to security logger with structured data
            MDC.put("eventType", eventType);
            MDC.put("severity", severity);
            MDC.put("userId", userId != null ? userId.toString() : "UNKNOWN");
            MDC.put("phoneNumber", phoneNumber != null ? phoneNumber : "UNKNOWN");
            MDC.put("sessionId", sessionId);
            MDC.put("ipAddress", getClientIpAddress(request));
            
            if ("CRITICAL".equals(severity) || "HIGH".equals(severity)) {
                log.error("Security event: {} - {}", eventType, description);
            } else if ("MEDIUM".equals(severity)) {
                log.warn("Security event: {} - {}", eventType, description);
            } else {
                log.info("Security event: {} - {}", eventType, description);
            }
            
            MDC.clear();
            
        } catch (Exception e) {
            log.error("Failed to log security event {}: {}", eventType, e.getMessage(), e);
        }
    }

    /**
     * Mark a security event as resolved
     */
    public void resolveSecurityEvent(UUID eventId, String resolvedBy) {
        try {
            securityEventLogRepository.findById(eventId).ifPresent(event -> {
                event.setResolved(true);
                event.setResolvedAt(LocalDateTime.now());
                event.setResolvedBy(resolvedBy);
                securityEventLogRepository.save(event);
                
                log.info("Security event {} resolved by {}", eventId, resolvedBy);
            });
        } catch (Exception e) {
            log.error("Failed to resolve security event {}: {}", eventId, e.getMessage(), e);
        }
    }

    private HttpServletRequest getCurrentRequest() {
        try {
            ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.currentRequestAttributes();
            return attributes.getRequest();
        } catch (Exception e) {
            return null;
        }
    }

    private String getCurrentSessionId() {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication != null && authentication.getDetails() instanceof String) {
                return (String) authentication.getDetails();
            }
        } catch (Exception e) {
            // Ignore
        }
        return "UNKNOWN";
    }

    private String getClientIpAddress(HttpServletRequest request) {
        if (request == null) return "UNKNOWN";
        
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            return xForwardedFor.split(",")[0].trim();
        }
        
        String xRealIp = request.getHeader("X-Real-IP");
        if (xRealIp != null && !xRealIp.isEmpty()) {
            return xRealIp;
        }
        
        return request.getRemoteAddr();
    }

    private String getUserAgent(HttpServletRequest request) {
        if (request == null) return "UNKNOWN";
        return request.getHeader("User-Agent");
    }
}