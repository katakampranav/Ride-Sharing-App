package com.officemate.shared.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Custom validation annotation for corporate email validation.
 * Validates email format and ensures it's from a corporate domain.
 */
@Documented
@Constraint(validatedBy = CorporateEmailValidator.class)
@Target({ElementType.FIELD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
public @interface CorporateEmail {
    String message() default "Invalid corporate email format";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
    
    /**
     * List of blocked domains (personal email providers)
     */
    String[] blockedDomains() default {
        "gmail.com", "yahoo.com", "hotmail.com", "outlook.com", 
        "aol.com", "icloud.com", "protonmail.com", "mail.com"
    };
    
    /**
     * Minimum domain length to be considered corporate
     */
    int minDomainLength() default 4;
}