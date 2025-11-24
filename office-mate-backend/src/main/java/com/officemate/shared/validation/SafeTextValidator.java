package com.officemate.shared.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.regex.Pattern;

/**
 * Validator for safe text input to prevent XSS and injection attacks.
 * Sanitizes and validates text content for security.
 */
public class SafeTextValidator implements ConstraintValidator<SafeText, String> {
    
    private static final Logger log = LoggerFactory.getLogger(SafeTextValidator.class);
    
    // Common XSS patterns to detect
    private static final Pattern XSS_PATTERN = Pattern.compile(
        "(?i)<script[^>]*>.*?</script>|javascript:|on\\w+\\s*=|<iframe|<object|<embed|<link|<meta|<style",
        Pattern.CASE_INSENSITIVE | Pattern.DOTALL
    );
    
    // SQL injection patterns
    private static final Pattern SQL_INJECTION_PATTERN = Pattern.compile(
        "(?i)(union|select|insert|update|delete|drop|create|alter|exec|execute|sp_|xp_)\\s",
        Pattern.CASE_INSENSITIVE
    );
    
    // Basic alphanumeric with common punctuation
    private static final Pattern SAFE_CHARS_PATTERN = Pattern.compile(
        "^[a-zA-Z0-9\\s\\-_.,!?@#$%&*()+=\\[\\]{}|;:'\"/\\\\]*$"
    );
    
    private int maxLength;
    private boolean allowHtml;
    private boolean allowSpecialChars;
    private Pattern customPattern;
    
    @Override
    public void initialize(SafeText constraintAnnotation) {
        this.maxLength = constraintAnnotation.maxLength();
        this.allowHtml = constraintAnnotation.allowHtml();
        this.allowSpecialChars = constraintAnnotation.allowSpecialChars();
        
        if (!constraintAnnotation.allowedPattern().isEmpty()) {
            this.customPattern = Pattern.compile(constraintAnnotation.allowedPattern());
        }
    }
    
    @Override
    public boolean isValid(String text, ConstraintValidatorContext context) {
        if (text == null) {
            return true; // Let @NotNull handle null validation
        }
        
        // Check length
        if (text.length() > maxLength) {
            log.debug("Text exceeds maximum length: {} > {}", text.length(), maxLength);
            context.disableDefaultConstraintViolation();
            context.buildConstraintViolationWithTemplate("Text exceeds maximum length of " + maxLength + " characters")
                   .addConstraintViolation();
            return false;
        }
        
        // Check for XSS patterns if HTML is not allowed
        if (!allowHtml && XSS_PATTERN.matcher(text).find()) {
            log.warn("Potential XSS attempt detected in text input");
            context.disableDefaultConstraintViolation();
            context.buildConstraintViolationWithTemplate("Text contains potentially harmful content")
                   .addConstraintViolation();
            return false;
        }
        
        // Check for SQL injection patterns
        if (SQL_INJECTION_PATTERN.matcher(text).find()) {
            log.warn("Potential SQL injection attempt detected in text input");
            context.disableDefaultConstraintViolation();
            context.buildConstraintViolationWithTemplate("Text contains invalid content")
                   .addConstraintViolation();
            return false;
        }
        
        // Apply custom pattern if specified
        if (customPattern != null) {
            if (!customPattern.matcher(text).matches()) {
                log.debug("Text does not match custom pattern: {}", text);
                return false;
            }
        } else if (!allowSpecialChars) {
            // Use basic alphanumeric pattern if special chars not allowed
            Pattern basicPattern = Pattern.compile("^[a-zA-Z0-9\\s\\-_.]*$");
            if (!basicPattern.matcher(text).matches()) {
                log.debug("Text contains disallowed special characters: {}", text);
                context.disableDefaultConstraintViolation();
                context.buildConstraintViolationWithTemplate("Text contains invalid characters")
                       .addConstraintViolation();
                return false;
            }
        } else {
            // Check against safe characters pattern
            if (!SAFE_CHARS_PATTERN.matcher(text).matches()) {
                log.debug("Text contains potentially unsafe characters: {}", text);
                context.disableDefaultConstraintViolation();
                context.buildConstraintViolationWithTemplate("Text contains invalid characters")
                       .addConstraintViolation();
                return false;
            }
        }
        
        return true;
    }
}