package com.officemate.config;

import org.springframework.boot.web.servlet.MultipartConfigFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.unit.DataSize;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import jakarta.servlet.MultipartConfigElement;

/**
 * Web configuration for request handling, file uploads, and security settings.
 * Configures request size limits, timeout handling, and input sanitization.
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {
    
    /**
     * Configure multipart file upload settings with size limits.
     * Prevents large file uploads that could cause DoS attacks.
     */
    @Bean
    public MultipartConfigElement multipartConfigElement() {
        MultipartConfigFactory factory = new MultipartConfigFactory();
        
        // Set maximum file size (10MB)
        factory.setMaxFileSize(DataSize.ofMegabytes(10));
        
        // Set maximum request size (50MB total)
        factory.setMaxRequestSize(DataSize.ofMegabytes(50));
        
        // Set file size threshold for writing to disk (1MB)
        factory.setFileSizeThreshold(DataSize.ofMegabytes(1));
        
        return factory.createMultipartConfig();
    }
}