package com.officemate.shared.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Validator for corporate email addresses.
 * Ensures email is valid format and not from personal email providers.
 */
public class CorporateEmailValidator implements ConstraintValidator<CorporateEmail, String> {
    
    private static final Logger log = LoggerFactory.getLogger(CorporateEmailValidator.class);
    
    private static final Pattern EMAIL_PATTERN = Pattern.compile(
        "^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}$"
    );
    
    private Set<String> blockedDomains;
    private int minDomainLength;
    
    @Override
    public void initialize(CorporateEmail constraintAnnotation) {
        this.blockedDomains = new HashSet<>(Arrays.asList(constraintAnnotation.blockedDomains()));
        this.minDomainLength = constraintAnnotation.minDomainLength();
    }
    
    @Override
    public boolean isValid(String email, ConstraintValidatorContext context) {
        if (email == null || email.trim().isEmpty()) {
            return false;
        }
        
        // Sanitize input - trim and convert to lowercase
        email = email.trim().toLowerCase();
        
        // Check basic email format
        if (!EMAIL_PATTERN.matcher(email).matches()) {
            log.debug("Invalid email format: {}", email);
            return false;
        }
        
        // Extract domain
        String domain = email.substring(email.indexOf('@') + 1);
        
        // Check if domain is blocked (personal email provider)
        if (blockedDomains.contains(domain)) {
            log.debug("Blocked personal email domain: {}", domain);
            context.disableDefaultConstraintViolation();
            context.buildConstraintViolationWithTemplate("Personal email addresses are not allowed. Please use your corporate email.")
                   .addConstraintViolation();
            return false;
        }
        
        // Check minimum domain length
        if (domain.length() < minDomainLength) {
            log.debug("Domain too short: {}", domain);
            context.disableDefaultConstraintViolation();
            context.buildConstraintViolationWithTemplate("Invalid corporate domain")
                   .addConstraintViolation();
            return false;
        }
        
        // Additional corporate domain validation
        if (!isValidCorporateDomain(domain)) {
            log.debug("Invalid corporate domain: {}", domain);
            context.disableDefaultConstraintViolation();
            context.buildConstraintViolationWithTemplate("Please use a valid corporate email address")
                   .addConstraintViolation();
            return false;
        }
        
        return true;
    }
    
    /**
     * Additional validation for corporate domains.
     * Checks for suspicious patterns that might indicate non-corporate domains.
     */
    private boolean isValidCorporateDomain(String domain) {
        // Check for suspicious patterns
        if (domain.contains("temp") || domain.contains("disposable") || 
            domain.contains("fake") || domain.contains("test")) {
            return false;
        }
        
        // Must have at least one dot and proper TLD
        String[] parts = domain.split("\\.");
        if (parts.length < 2) {
            return false;
        }
        
        // TLD should be at least 2 characters
        String tld = parts[parts.length - 1];
        return tld.length() >= 2 && tld.matches("[a-zA-Z]+");
    }
}