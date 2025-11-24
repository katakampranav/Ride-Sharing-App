package com.officemate.shared.validation;

import com.google.i18n.phonenumbers.NumberParseException;
import com.google.i18n.phonenumbers.PhoneNumberUtil;
import com.google.i18n.phonenumbers.Phonenumber;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Validator for phone number validation using Google's libphonenumber library.
 * Provides comprehensive phone number validation including format and region checks.
 */
public class PhoneNumberValidator implements ConstraintValidator<PhoneNumber, String> {
    
    private static final Logger log = LoggerFactory.getLogger(PhoneNumberValidator.class);
    
    private PhoneNumberUtil phoneNumberUtil;
    private String defaultRegion;
    private boolean internationalOnly;
    
    @Override
    public void initialize(PhoneNumber constraintAnnotation) {
        this.phoneNumberUtil = PhoneNumberUtil.getInstance();
        this.defaultRegion = constraintAnnotation.defaultRegion();
        this.internationalOnly = constraintAnnotation.internationalOnly();
    }
    
    @Override
    public boolean isValid(String phoneNumber, ConstraintValidatorContext context) {
        if (phoneNumber == null || phoneNumber.trim().isEmpty()) {
            return false;
        }
        
        try {
            // Parse the phone number
            Phonenumber.PhoneNumber parsedNumber = phoneNumberUtil.parse(phoneNumber, defaultRegion);
            
            // Check if the number is valid
            boolean isValid = phoneNumberUtil.isValidNumber(parsedNumber);
            
            if (!isValid) {
                log.debug("Invalid phone number format: {}", phoneNumber);
                return false;
            }
            
            // If international only is required, check format
            if (internationalOnly) {
                String formattedNumber = phoneNumberUtil.format(parsedNumber, PhoneNumberUtil.PhoneNumberFormat.E164);
                if (!phoneNumber.equals(formattedNumber)) {
                    log.debug("Phone number not in international format: {}", phoneNumber);
                    context.disableDefaultConstraintViolation();
                    context.buildConstraintViolationWithTemplate("Phone number must be in international format (e.g., +1234567890)")
                           .addConstraintViolation();
                    return false;
                }
            }
            
            return true;
            
        } catch (NumberParseException e) {
            log.debug("Failed to parse phone number: {}, error: {}", phoneNumber, e.getMessage());
            return false;
        }
    }
}