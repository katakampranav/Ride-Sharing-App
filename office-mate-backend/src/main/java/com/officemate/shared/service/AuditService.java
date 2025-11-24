package com.officemate.shared.service;

import com.officemate.shared.entity.AuditLog;
import com.officemate.shared.repository.AuditLogRepository;
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
 * Service for managing audit trails and logging profile changes
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AuditService {

    private final AuditLogRepository auditLogRepository;

    /**
     * Log a profile change event
     */
    @Async
    public void logProfileChange(UUID userId, String entityType, String entityId, 
                               String fieldName, String oldValue, String newValue, String reason) {
        try {
            HttpServletRequest request = getCurrentRequest();
            String sessionId = getCurrentSessionId();
            
            AuditLog auditLog = AuditLog.builder()
                .userId(userId)
                .entityType(entityType)
                .entityId(entityId)
                .action("UPDATE")
                .fieldName(fieldName)
                .oldValue(oldValue)
                .newValue(newValue)
                .ipAddress(getClientIpAddress(request))
                .userAgent(getUserAgent(request))
                .sessionId(sessionId)
                .reason(reason)
                .timestamp(LocalDateTime.now())
                .build();

            auditLogRepository.save(auditLog);
            
            // Log to audit logger with structured data
            MDC.put("userId", userId.toString());
            MDC.put("entityType", entityType);
            MDC.put("entityId", entityId);
            MDC.put("action", "UPDATE");
            MDC.put("fieldName", fieldName);
            MDC.put("sessionId", sessionId);
            
            log.info("Profile change logged: {} field '{}' changed from '{}' to '{}' for user {}", 
                    entityType, fieldName, oldValue, newValue, userId);
            
            MDC.clear();
            
        } catch (Exception e) {
            log.error("Failed to log profile change for user {}: {}", userId, e.getMessage(), e);
        }
    }

    /**
     * Log entity creation
     */
    @Async
    public void logEntityCreation(UUID userId, String entityType, String entityId, String reason) {
        try {
            HttpServletRequest request = getCurrentRequest();
            String sessionId = getCurrentSessionId();
            
            AuditLog auditLog = AuditLog.builder()
                .userId(userId)
                .entityType(entityType)
                .entityId(entityId)
                .action("CREATE")
                .ipAddress(getClientIpAddress(request))
                .userAgent(getUserAgent(request))
                .sessionId(sessionId)
                .reason(reason)
                .timestamp(LocalDateTime.now())
                .build();

            auditLogRepository.save(auditLog);
            
            // Log to audit logger
            MDC.put("userId", userId.toString());
            MDC.put("entityType", entityType);
            MDC.put("entityId", entityId);
            MDC.put("action", "CREATE");
            MDC.put("sessionId", sessionId);
            
            log.info("Entity creation logged: {} created with ID {} for user {}", 
                    entityType, entityId, userId);
            
            MDC.clear();
            
        } catch (Exception e) {
            log.error("Failed to log entity creation for user {}: {}", userId, e.getMessage(), e);
        }
    }

    /**
     * Log entity deletion
     */
    @Async
    public void logEntityDeletion(UUID userId, String entityType, String entityId, String reason) {
        try {
            HttpServletRequest request = getCurrentRequest();
            String sessionId = getCurrentSessionId();
            
            AuditLog auditLog = AuditLog.builder()
                .userId(userId)
                .entityType(entityType)
                .entityId(entityId)
                .action("DELETE")
                .ipAddress(getClientIpAddress(request))
                .userAgent(getUserAgent(request))
                .sessionId(sessionId)
                .reason(reason)
                .timestamp(LocalDateTime.now())
                .build();

            auditLogRepository.save(auditLog);
            
            // Log to audit logger
            MDC.put("userId", userId.toString());
            MDC.put("entityType", entityType);
            MDC.put("entityId", entityId);
            MDC.put("action", "DELETE");
            MDC.put("sessionId", sessionId);
            
            log.info("Entity deletion logged: {} with ID {} deleted for user {}", 
                    entityType, entityId, userId);
            
            MDC.clear();
            
        } catch (Exception e) {
            log.error("Failed to log entity deletion for user {}: {}", userId, e.getMessage(), e);
        }
    }

    /**
     * Log bulk profile changes
     */
    @Async
    public void logBulkProfileChange(UUID userId, String entityType, String entityId, 
                                   String changesDescription, String reason) {
        try {
            HttpServletRequest request = getCurrentRequest();
            String sessionId = getCurrentSessionId();
            
            AuditLog auditLog = AuditLog.builder()
                .userId(userId)
                .entityType(entityType)
                .entityId(entityId)
                .action("BULK_UPDATE")
                .newValue(changesDescription)
                .ipAddress(getClientIpAddress(request))
                .userAgent(getUserAgent(request))
                .sessionId(sessionId)
                .reason(reason)
                .timestamp(LocalDateTime.now())
                .build();

            auditLogRepository.save(auditLog);
            
            // Log to audit logger
            MDC.put("userId", userId.toString());
            MDC.put("entityType", entityType);
            MDC.put("entityId", entityId);
            MDC.put("action", "BULK_UPDATE");
            MDC.put("sessionId", sessionId);
            
            log.info("Bulk profile change logged: {} bulk update for user {}: {}", 
                    entityType, userId, changesDescription);
            
            MDC.clear();
            
        } catch (Exception e) {
            log.error("Failed to log bulk profile change for user {}: {}", userId, e.getMessage(), e);
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