package com.officemate.shared.validation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation to mark methods that require both mobile and email verification.
 * Used with aspect-oriented programming to enforce verification checks.
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface RequireFullVerification {
    String message() default "Both mobile and email verification required to access this feature";
}
