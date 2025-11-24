package com.officemate.config.security;

import com.officemate.shared.exception.ProfileAccessException;
import com.officemate.shared.validation.RequireEmailVerification;
import com.officemate.shared.validation.RequireFullVerification;
import com.officemate.shared.validation.RequireMobileVerification;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.lang.reflect.Method;

/**
 * Aspect for enforcing verification requirements on methods.
 * Checks mobile and email verification status before allowing method execution.
 */
@Aspect
@Component
@RequiredArgsConstructor
@Slf4j
public class VerificationAspect {

    /**
     * Enforce mobile verification requirement.
     */
    @Before("@annotation(com.officemate.shared.validation.RequireMobileVerification)")
    public void checkMobileVerification(JoinPoint joinPoint) {
        HttpServletRequest request = getCurrentRequest();
        Boolean mobileVerified = (Boolean) request.getAttribute("mobileVerified");
        Boolean emailVerified = (Boolean) request.getAttribute("emailVerified");
        
        if (mobileVerified == null || !mobileVerified) {
            String message = getAnnotationMessage(joinPoint, RequireMobileVerification.class);
            log.warn("Mobile verification required for method: {}", joinPoint.getSignature().getName());
            throw new ProfileAccessException(
                    message, 
                    mobileVerified != null && mobileVerified, 
                    emailVerified != null && emailVerified
            );
        }
    }

    /**
     * Enforce email verification requirement.
     */
    @Before("@annotation(com.officemate.shared.validation.RequireEmailVerification)")
    public void checkEmailVerification(JoinPoint joinPoint) {
        HttpServletRequest request = getCurrentRequest();
        Boolean mobileVerified = (Boolean) request.getAttribute("mobileVerified");
        Boolean emailVerified = (Boolean) request.getAttribute("emailVerified");
        
        if (emailVerified == null || !emailVerified) {
            String message = getAnnotationMessage(joinPoint, RequireEmailVerification.class);
            log.warn("Email verification required for method: {}", joinPoint.getSignature().getName());
            throw new ProfileAccessException(
                    message, 
                    mobileVerified != null && mobileVerified, 
                    emailVerified != null && emailVerified
            );
        }
    }

    /**
     * Enforce full verification requirement (both mobile and email).
     */
    @Before("@annotation(com.officemate.shared.validation.RequireFullVerification)")
    public void checkFullVerification(JoinPoint joinPoint) {
        HttpServletRequest request = getCurrentRequest();
        Boolean mobileVerified = (Boolean) request.getAttribute("mobileVerified");
        Boolean emailVerified = (Boolean) request.getAttribute("emailVerified");
        
        if (mobileVerified == null || !mobileVerified || emailVerified == null || !emailVerified) {
            String message = getAnnotationMessage(joinPoint, RequireFullVerification.class);
            log.warn("Full verification required for method: {}", joinPoint.getSignature().getName());
            throw new ProfileAccessException(
                    message, 
                    mobileVerified != null && mobileVerified, 
                    emailVerified != null && emailVerified
            );
        }
    }

    /**
     * Get current HTTP request from context.
     */
    private HttpServletRequest getCurrentRequest() {
        ServletRequestAttributes attributes = 
                (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attributes == null) {
            throw new IllegalStateException("No request context available");
        }
        return attributes.getRequest();
    }

    /**
     * Extract custom message from annotation.
     */
    private <T> String getAnnotationMessage(JoinPoint joinPoint, Class<T> annotationClass) {
        try {
            MethodSignature signature = (MethodSignature) joinPoint.getSignature();
            Method method = signature.getMethod();
            
            if (annotationClass == RequireMobileVerification.class) {
                RequireMobileVerification annotation = method.getAnnotation(RequireMobileVerification.class);
                return annotation != null ? annotation.message() : "Mobile verification required";
            } else if (annotationClass == RequireEmailVerification.class) {
                RequireEmailVerification annotation = method.getAnnotation(RequireEmailVerification.class);
                return annotation != null ? annotation.message() : "Email verification required";
            } else if (annotationClass == RequireFullVerification.class) {
                RequireFullVerification annotation = method.getAnnotation(RequireFullVerification.class);
                return annotation != null ? annotation.message() : "Full verification required";
            }
        } catch (Exception e) {
            log.error("Error extracting annotation message", e);
        }
        return "Verification required";
    }
}
