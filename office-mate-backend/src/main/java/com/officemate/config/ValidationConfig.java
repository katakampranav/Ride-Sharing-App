package com.officemate.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;
import org.springframework.validation.beanvalidation.MethodValidationPostProcessor;

import jakarta.validation.Validator;

/**
 * Configuration for Bean Validation (JSR-303/JSR-380).
 * Enables method-level validation and configures validation settings.
 */
@Configuration
public class ValidationConfig {
    
    /**
     * Configure the validator factory bean.
     * This enables custom validators and validation messages.
     */
    @Bean
    public LocalValidatorFactoryBean validator() {
        LocalValidatorFactoryBean validatorFactoryBean = new LocalValidatorFactoryBean();
        // Additional configuration can be added here if needed
        return validatorFactoryBean;
    }
    
    /**
     * Enable method-level validation.
     * This allows validation annotations on method parameters and return values.
     */
    @Bean
    public MethodValidationPostProcessor methodValidationPostProcessor() {
        MethodValidationPostProcessor processor = new MethodValidationPostProcessor();
        processor.setValidator(validator());
        return processor;
    }
    
    /**
     * Provide the validator instance for dependency injection.
     */
    @Bean
    public Validator validatorInstance() {
        return validator();
    }
}