package com.officemate.shared.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Custom validation annotation for vehicle type validation.
 * Validates vehicle type enum values and related constraints.
 */
@Documented
@Constraint(validatedBy = VehicleTypeValidator.class)
@Target({ElementType.FIELD, ElementType.PARAMETER, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface VehicleTypeValid {
    String message() default "Invalid vehicle type or configuration";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
    
    /**
     * Whether to validate capacity based on vehicle type
     */
    boolean validateCapacity() default true;
    
    /**
     * Whether to validate fuel type compatibility
     */
    boolean validateFuelType() default true;
}