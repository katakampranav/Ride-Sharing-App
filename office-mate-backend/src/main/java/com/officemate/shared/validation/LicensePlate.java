package com.officemate.shared.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Custom validation annotation for license plate validation.
 * Validates license plate format and prevents malicious input.
 */
@Documented
@Constraint(validatedBy = LicensePlateValidator.class)
@Target({ElementType.FIELD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
public @interface LicensePlate {
    String message() default "Invalid license plate format";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
    
    /**
     * Country/region for license plate format (default: US)
     */
    String region() default "US";
    
    /**
     * Minimum length for license plate
     */
    int minLength() default 2;
    
    /**
     * Maximum length for license plate
     */
    int maxLength() default 10;
}