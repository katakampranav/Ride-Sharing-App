package com.officemate.shared.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.regex.Pattern;

/**
 * Validator for license plate format.
 * Validates license plate format based on region and prevents malicious input.
 */
public class LicensePlateValidator implements ConstraintValidator<LicensePlate, String> {
    
    private static final Logger log = LoggerFactory.getLogger(LicensePlateValidator.class);
    
    // Common license plate patterns for different regions
    private static final Pattern US_PATTERN = Pattern.compile("^[A-Z0-9]{2,8}$");
    private static final Pattern GENERIC_PATTERN = Pattern.compile("^[A-Z0-9\\-\\s]{2,10}$");
    
    // Patterns to detect potential security issues
    private static final Pattern MALICIOUS_PATTERN = Pattern.compile(
        "(?i)(script|javascript|<|>|&|;|'|\"|\\\\|/\\*|\\*/|--|union|select|insert|update|delete)",
        Pattern.CASE_INSENSITIVE
    );
    
    private String region;
    private int minLength;
    private int maxLength;
    
    @Override
    public void initialize(LicensePlate constraintAnnotation) {
        this.region = constraintAnnotation.region();
        this.minLength = constraintAnnotation.minLength();
        this.maxLength = constraintAnnotation.maxLength();
    }
    
    @Override
    public boolean isValid(String licensePlate, ConstraintValidatorContext context) {
        if (licensePlate == null || licensePlate.trim().isEmpty()) {
            return false;
        }
        
        // Sanitize input - trim and convert to uppercase
        licensePlate = licensePlate.trim().toUpperCase();
        
        // Check length constraints
        if (licensePlate.length() < minLength || licensePlate.length() > maxLength) {
            log.debug("License plate length invalid: {} (min: {}, max: {})", 
                     licensePlate.length(), minLength, maxLength);
            context.disableDefaultConstraintViolation();
            context.buildConstraintViolationWithTemplate(
                String.format("License plate must be between %d and %d characters", minLength, maxLength)
            ).addConstraintViolation();
            return false;
        }
        
        // Check for malicious patterns
        if (MALICIOUS_PATTERN.matcher(licensePlate).find()) {
            log.warn("Potentially malicious license plate input detected: {}", licensePlate);
            context.disableDefaultConstraintViolation();
            context.buildConstraintViolationWithTemplate("License plate contains invalid characters")
                   .addConstraintViolation();
            return false;
        }
        
        // Validate based on region
        Pattern pattern = getPatternForRegion(region);
        if (!pattern.matcher(licensePlate).matches()) {
            log.debug("License plate format invalid for region {}: {}", region, licensePlate);
            context.disableDefaultConstraintViolation();
            context.buildConstraintViolationWithTemplate(
                "License plate format is invalid for region " + region
            ).addConstraintViolation();
            return false;
        }
        
        return true;
    }
    
    private Pattern getPatternForRegion(String region) {
        switch (region.toUpperCase()) {
            case "US":
            case "USA":
                return US_PATTERN;
            default:
                return GENERIC_PATTERN;
        }
    }
}