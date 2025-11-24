package com.officemate.shared.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Custom validation annotation for text sanitization and XSS prevention.
 * Validates and sanitizes text input to prevent XSS attacks and malicious content.
 */
@Documented
@Constraint(validatedBy = SafeTextValidator.class)
@Target({ElementType.FIELD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
public @interface SafeText {
    String message() default "Text contains invalid or potentially harmful content";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
    
    /**
     * Maximum allowed length for the text
     */
    int maxLength() default 255;
    
    /**
     * Whether to allow HTML tags (default: false for security)
     */
    boolean allowHtml() default false;
    
    /**
     * Whether to allow special characters
     */
    boolean allowSpecialChars() default true;
    
    /**
     * Pattern for allowed characters (if specified, overrides other settings)
     */
    String allowedPattern() default "";
}