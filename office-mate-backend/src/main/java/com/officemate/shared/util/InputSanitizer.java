package com.officemate.shared.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.regex.Pattern;

/**
 * Utility class for input sanitization and XSS prevention.
 * Provides methods to clean and validate user input for security.
 */
@Component
public class InputSanitizer {
    
    private static final Logger log = LoggerFactory.getLogger(InputSanitizer.class);
    
    // XSS patterns to remove or escape
    private static final Pattern XSS_SCRIPT_PATTERN = Pattern.compile(
        "(?i)<script[^>]*>.*?</script>", Pattern.DOTALL
    );
    
    private static final Pattern XSS_JAVASCRIPT_PATTERN = Pattern.compile(
        "(?i)javascript:", Pattern.CASE_INSENSITIVE
    );
    
    private static final Pattern XSS_EVENT_PATTERN = Pattern.compile(
        "(?i)on\\w+\\s*=", Pattern.CASE_INSENSITIVE
    );
    
    private static final Pattern XSS_TAG_PATTERN = Pattern.compile(
        "(?i)<(iframe|object|embed|link|meta|style|form|input)[^>]*>", 
        Pattern.CASE_INSENSITIVE
    );
    
    // SQL injection patterns
    private static final Pattern SQL_INJECTION_PATTERN = Pattern.compile(
        "(?i)(union|select|insert|update|delete|drop|create|alter|exec|execute|sp_|xp_)\\s+",
        Pattern.CASE_INSENSITIVE
    );
    
    // HTML entities for escaping
    private static final String[][] HTML_ENTITIES = {
        {"&", "&amp;"},
        {"<", "&lt;"},
        {">", "&gt;"},
        {"\"", "&quot;"},
        {"'", "&#x27;"},
        {"/", "&#x2F;"}
    };
    
    /**
     * Sanitizes text input by removing potentially harmful content.
     * 
     * @param input the input text to sanitize
     * @return sanitized text
     */
    public String sanitizeText(String input) {
        if (input == null) {
            return null;
        }
        
        String sanitized = input.trim();
        
        // Remove XSS patterns
        sanitized = XSS_SCRIPT_PATTERN.matcher(sanitized).replaceAll("");
        sanitized = XSS_JAVASCRIPT_PATTERN.matcher(sanitized).replaceAll("");
        sanitized = XSS_EVENT_PATTERN.matcher(sanitized).replaceAll("");
        sanitized = XSS_TAG_PATTERN.matcher(sanitized).replaceAll("");
        
        // Log if content was modified
        if (!input.equals(sanitized)) {
            log.warn("Input sanitization performed - potentially malicious content removed");
        }
        
        return sanitized;
    }
    
    /**
     * Escapes HTML entities in text to prevent XSS.
     * 
     * @param input the input text to escape
     * @return HTML-escaped text
     */
    public String escapeHtml(String input) {
        if (input == null) {
            return null;
        }
        
        String escaped = input;
        for (String[] entity : HTML_ENTITIES) {
            escaped = escaped.replace(entity[0], entity[1]);
        }
        
        return escaped;
    }
    
    /**
     * Sanitizes text for safe database storage.
     * Removes SQL injection patterns and escapes special characters.
     * 
     * @param input the input text to sanitize
     * @return sanitized text safe for database operations
     */
    public String sanitizeForDatabase(String input) {
        if (input == null) {
            return null;
        }
        
        String sanitized = input.trim();
        
        // Check for SQL injection patterns
        if (SQL_INJECTION_PATTERN.matcher(sanitized).find()) {
            log.warn("Potential SQL injection attempt detected and blocked");
            throw new IllegalArgumentException("Input contains potentially harmful content");
        }
        
        // Escape single quotes for SQL safety (though we use parameterized queries)
        sanitized = sanitized.replace("'", "''");
        
        return sanitized;
    }
    
    /**
     * Sanitizes phone number input.
     * Removes non-numeric characters except + and spaces.
     * 
     * @param phoneNumber the phone number to sanitize
     * @return sanitized phone number
     */
    public String sanitizePhoneNumber(String phoneNumber) {
        if (phoneNumber == null) {
            return null;
        }
        
        // Remove all characters except digits, +, -, (, ), and spaces
        String sanitized = phoneNumber.replaceAll("[^0-9+\\-()\\s]", "");
        
        return sanitized.trim();
    }
    
    /**
     * Sanitizes email address input.
     * Converts to lowercase and trims whitespace.
     * 
     * @param email the email address to sanitize
     * @return sanitized email address
     */
    public String sanitizeEmail(String email) {
        if (email == null) {
            return null;
        }
        
        return email.trim().toLowerCase();
    }
    
    /**
     * Sanitizes license plate input.
     * Converts to uppercase and removes invalid characters.
     * 
     * @param licensePlate the license plate to sanitize
     * @return sanitized license plate
     */
    public String sanitizeLicensePlate(String licensePlate) {
        if (licensePlate == null) {
            return null;
        }
        
        // Keep only alphanumeric characters and hyphens
        String sanitized = licensePlate.replaceAll("[^A-Za-z0-9\\-]", "");
        
        return sanitized.toUpperCase().trim();
    }
    
    /**
     * Checks if text contains potentially malicious content.
     * 
     * @param input the input text to check
     * @return true if content appears malicious
     */
    public boolean containsMaliciousContent(String input) {
        if (input == null) {
            return false;
        }
        
        return XSS_SCRIPT_PATTERN.matcher(input).find() ||
               XSS_JAVASCRIPT_PATTERN.matcher(input).find() ||
               XSS_EVENT_PATTERN.matcher(input).find() ||
               XSS_TAG_PATTERN.matcher(input).find() ||
               SQL_INJECTION_PATTERN.matcher(input).find();
    }
    
    /**
     * Validates that text contains only safe characters.
     * 
     * @param input the input text to validate
     * @param allowSpecialChars whether to allow special characters
     * @return true if text is safe
     */
    public boolean isSafeText(String input, boolean allowSpecialChars) {
        if (input == null) {
            return true;
        }
        
        if (containsMaliciousContent(input)) {
            return false;
        }
        
        if (allowSpecialChars) {
            // Allow alphanumeric and common punctuation
            return input.matches("^[a-zA-Z0-9\\s\\-_.,!?@#$%&*()+=\\[\\]{}|;:'\"/\\\\]*$");
        } else {
            // Only alphanumeric, spaces, hyphens, and underscores
            return input.matches("^[a-zA-Z0-9\\s\\-_.]*$");
        }
    }
}