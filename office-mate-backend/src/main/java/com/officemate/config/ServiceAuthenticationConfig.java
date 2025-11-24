package com.officemate.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.security.SecureRandom;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

/**
 * Configuration for service-to-service authentication.
 * Provides API keys and authentication mechanisms for internal service communication.
 */
@Slf4j
@Configuration
public class ServiceAuthenticationConfig {

    @Value("${app.service-auth.enabled:true}")
    private boolean serviceAuthEnabled;

    @Value("${app.service-auth.api-key:}")
    private String configuredApiKey;

    /**
     * Password encoder for API key hashing
     */
    @Bean
    public PasswordEncoder servicePasswordEncoder() {
        return new BCryptPasswordEncoder(12);
    }

    /**
     * Generate or retrieve service API key
     */
    @Bean
    public String serviceApiKey() {
        if (configuredApiKey != null && !configuredApiKey.isEmpty()) {
            log.info("Using configured service API key");
            return configuredApiKey;
        }
        
        // Generate a secure random API key for development
        String generatedKey = generateSecureApiKey();
        log.warn("Generated temporary service API key: {} (Configure a permanent key in production)", 
                generatedKey.substring(0, 8) + "...");
        return generatedKey;
    }

    /**
     * Service authentication headers configuration
     */
    @Bean
    public Map<String, String> serviceAuthHeaders() {
        Map<String, String> headers = new HashMap<>();
        headers.put("X-Service-Auth", "true");
        headers.put("X-Service-Name", "officemate-backend");
        return headers;
    }

    /**
     * Generate a secure random API key
     */
    private String generateSecureApiKey() {
        SecureRandom random = new SecureRandom();
        byte[] bytes = new byte[32];
        random.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    /**
     * Check if service authentication is enabled
     */
    public boolean isServiceAuthEnabled() {
        return serviceAuthEnabled;
    }
}
