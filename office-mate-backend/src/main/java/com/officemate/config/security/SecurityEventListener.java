package com.officemate.config.security;

import com.officemate.shared.service.SecurityEventService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.security.authentication.event.*;
import org.springframework.security.authorization.event.AuthorizationDeniedEvent;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.session.SessionFixationProtectionEvent;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * Spring Security event listener for capturing authentication and authorization events
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class SecurityEventListener {

    private final SecurityEventService securityEventService;

    /**
     * Handle successful authentication events
     */
    @EventListener
    public void handleAuthenticationSuccess(AuthenticationSuccessEvent event) {
        try {
            Authentication authentication = event.getAuthentication();
            String principal = authentication.getName();
            
            // Try to parse as UUID (user ID) or treat as phone number
            UUID userId = null;
            String phoneNumber = null;
            
            try {
                userId = UUID.fromString(principal);
            } catch (IllegalArgumentException e) {
                phoneNumber = principal;
            }
            
            securityEventService.logSuccessfulLogin(userId, phoneNumber);
            
        } catch (Exception e) {
            log.error("Failed to handle authentication success event: {}", e.getMessage(), e);
        }
    }

    /**
     * Handle authentication failure events
     */
    @EventListener
    public void handleAuthenticationFailure(AbstractAuthenticationFailureEvent event) {
        try {
            Authentication authentication = event.getAuthentication();
            String principal = authentication != null ? authentication.getName() : "UNKNOWN";
            Exception exception = event.getException();
            
            String reason = exception != null ? exception.getMessage() : "Unknown authentication failure";
            String eventType = event.getClass().getSimpleName();
            
            securityEventService.logFailedLogin(principal, eventType + ": " + reason, null);
            
        } catch (Exception e) {
            log.error("Failed to handle authentication failure event: {}", e.getMessage(), e);
        }
    }

    /**
     * Handle authorization denied events
     */
    @EventListener
    public void handleAuthorizationDenied(AuthorizationDeniedEvent event) {
        try {
            Authentication authentication = event.getAuthentication().get();
            String principal = authentication != null ? authentication.getName() : "UNKNOWN";
            
            UUID userId = null;
            String phoneNumber = null;
            
            try {
                userId = UUID.fromString(principal);
            } catch (IllegalArgumentException e) {
                phoneNumber = principal;
            }
            
            String resource = event.getAuthorizationDecision().toString();
            securityEventService.logSuspiciousActivity(userId, phoneNumber, null, 
                    "AUTHORIZATION_DENIED", 
                    "Access denied to resource: " + resource, 
                    "MEDIUM");
            
        } catch (Exception e) {
            log.error("Failed to handle authorization denied event: {}", e.getMessage(), e);
        }
    }

    /**
     * Handle interactive authentication success (e.g., form login)
     */
    @EventListener
    public void handleInteractiveAuthenticationSuccess(InteractiveAuthenticationSuccessEvent event) {
        try {
            Authentication authentication = event.getAuthentication();
            String principal = authentication.getName();
            
            UUID userId = null;
            String phoneNumber = null;
            
            try {
                userId = UUID.fromString(principal);
            } catch (IllegalArgumentException e) {
                phoneNumber = principal;
            }
            
            securityEventService.logSuccessfulLogin(userId, phoneNumber);
            
        } catch (Exception e) {
            log.error("Failed to handle interactive authentication success event: {}", e.getMessage(), e);
        }
    }

    /**
     * Handle session fixation protection events
     */
    @EventListener
    public void handleSessionFixation(SessionFixationProtectionEvent event) {
        try {
            Authentication authentication = event.getAuthentication();
            String principal = authentication != null ? authentication.getName() : "UNKNOWN";
            
            UUID userId = null;
            try {
                userId = UUID.fromString(principal);
            } catch (IllegalArgumentException e) {
                // Not a UUID, continue with null
            }
            
            securityEventService.logSuspiciousActivity(userId, null, null, 
                    "SESSION_FIXATION_PROTECTION", 
                    "Session fixation protection triggered for user: " + principal, 
                    "MEDIUM");
            
        } catch (Exception e) {
            log.error("Failed to handle session fixation event: {}", e.getMessage(), e);
        }
    }
}