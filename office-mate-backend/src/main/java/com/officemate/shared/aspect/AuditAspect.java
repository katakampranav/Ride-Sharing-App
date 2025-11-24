package com.officemate.shared.aspect;

import com.officemate.shared.service.AuditService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;

import java.lang.reflect.Field;
import java.util.UUID;

/**
 * Aspect for automatically logging profile changes and entity operations
 */
@Aspect
@Component
@RequiredArgsConstructor
@Slf4j
public class AuditAspect {

    private final AuditService auditService;

    /**
     * Log profile service method calls that modify data
     */
    @AfterReturning(pointcut = "execution(* com.officemate.modules.profile.service.*Service.create*(..))", returning = "result")
    public void logProfileCreation(JoinPoint joinPoint, Object result) {
        try {
            String methodName = joinPoint.getSignature().getName();
            String serviceName = joinPoint.getTarget().getClass().getSimpleName();
            Object[] args = joinPoint.getArgs();
            
            // Extract user ID from the first argument (typically UUID userId)
            UUID userId = extractUserIdFromArgs(args);
            if (userId != null) {
                String entityType = determineEntityType(serviceName, methodName);
                String entityId = extractEntityId(result);
                
                auditService.logEntityCreation(userId, entityType, entityId, 
                        "Profile creation via " + serviceName + "." + methodName);
            }
        } catch (Exception e) {
            log.error("Failed to log profile creation: {}", e.getMessage(), e);
        }
    }

    /**
     * Log profile service method calls that update data
     */
    @AfterReturning(pointcut = "execution(* com.officemate.modules.profile.service.*Service.update*(..))", returning = "result")
    public void logProfileUpdate(JoinPoint joinPoint, Object result) {
        try {
            String methodName = joinPoint.getSignature().getName();
            String serviceName = joinPoint.getTarget().getClass().getSimpleName();
            Object[] args = joinPoint.getArgs();
            
            // Extract user ID from the first argument
            UUID userId = extractUserIdFromArgs(args);
            if (userId != null) {
                String entityType = determineEntityType(serviceName, methodName);
                String entityId = extractEntityId(result);
                
                auditService.logBulkProfileChange(userId, entityType, entityId, 
                        "Profile update via " + serviceName + "." + methodName,
                        "Profile modification through service layer");
            }
        } catch (Exception e) {
            log.error("Failed to log profile update: {}", e.getMessage(), e);
        }
    }

    /**
     * Log authentication service method calls
     */
    @AfterReturning(pointcut = "execution(* com.officemate.modules.auth.service.*Service.verify*(..))", returning = "result")
    public void logAuthenticationAction(JoinPoint joinPoint, Object result) {
        try {
            String methodName = joinPoint.getSignature().getName();
            String serviceName = joinPoint.getTarget().getClass().getSimpleName();
            Object[] args = joinPoint.getArgs();
            
            UUID userId = extractUserIdFromArgs(args);
            if (userId != null) {
                auditService.logBulkProfileChange(userId, "UserAccount", userId.toString(), 
                        "Authentication action: " + methodName,
                        "Verification status change via " + serviceName);
            }
        } catch (Exception e) {
            log.error("Failed to log authentication action: {}", e.getMessage(), e);
        }
    }

    /**
     * Log wallet service operations
     */
    @AfterReturning(pointcut = "execution(* com.officemate.modules.wallet.service.*Service.*(..))", returning = "result")
    public void logWalletOperation(JoinPoint joinPoint, Object result) {
        try {
            String methodName = joinPoint.getSignature().getName();
            String serviceName = joinPoint.getTarget().getClass().getSimpleName();
            Object[] args = joinPoint.getArgs();
            
            UUID userId = extractUserIdFromArgs(args);
            if (userId != null && (methodName.startsWith("create") || methodName.startsWith("update") || 
                                  methodName.startsWith("add") || methodName.startsWith("enable"))) {
                auditService.logBulkProfileChange(userId, "Wallet", userId.toString(), 
                        "Wallet operation: " + methodName,
                        "Wallet modification via " + serviceName);
            }
        } catch (Exception e) {
            log.error("Failed to log wallet operation: {}", e.getMessage(), e);
        }
    }

    /**
     * Extract user ID from method arguments
     */
    private UUID extractUserIdFromArgs(Object[] args) {
        if (args != null && args.length > 0) {
            for (Object arg : args) {
                if (arg instanceof UUID) {
                    return (UUID) arg;
                }
                // Check if it's a DTO with userId field
                if (arg != null) {
                    try {
                        Field userIdField = arg.getClass().getDeclaredField("userId");
                        userIdField.setAccessible(true);
                        Object userIdValue = userIdField.get(arg);
                        if (userIdValue instanceof UUID) {
                            return (UUID) userIdValue;
                        }
                    } catch (Exception e) {
                        // Field doesn't exist or not accessible, continue
                    }
                }
            }
        }
        return null;
    }

    /**
     * Extract entity ID from the result object
     */
    private String extractEntityId(Object result) {
        if (result == null) {
            return "UNKNOWN";
        }
        
        try {
            // Try to get userId field from response
            Field userIdField = result.getClass().getDeclaredField("userId");
            userIdField.setAccessible(true);
            Object userIdValue = userIdField.get(result);
            if (userIdValue instanceof UUID) {
                return userIdValue.toString();
            }
        } catch (Exception e) {
            // Field doesn't exist, try other common ID fields
        }
        
        try {
            // Try to get id field from response
            Field idField = result.getClass().getDeclaredField("id");
            idField.setAccessible(true);
            Object idValue = idField.get(result);
            if (idValue != null) {
                return idValue.toString();
            }
        } catch (Exception e) {
            // Field doesn't exist
        }
        
        return "UNKNOWN";
    }

    /**
     * Determine entity type based on service name and method
     */
    private String determineEntityType(String serviceName, String methodName) {
        if (serviceName.contains("UserProfile")) {
            return "UserProfile";
        } else if (serviceName.contains("DriverProfile")) {
            return "DriverProfile";
        } else if (serviceName.contains("RiderProfile")) {
            return "RiderProfile";
        } else if (serviceName.contains("Wallet")) {
            return "Wallet";
        } else if (serviceName.contains("Auth")) {
            return "UserAccount";
        } else {
            return serviceName.replace("Service", "");
        }
    }
}