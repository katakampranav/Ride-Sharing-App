package com.officemate.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.web.client.RestTemplate;

/**
 * Configuration class for security enhancement features.
 * Provides beans for CAPTCHA service, pattern monitoring, and other security components.
 */
@Configuration
@EnableAsync
@EnableScheduling
public class SecurityEnhancementConfig {

    /**
     * RestTemplate bean for external service calls (CAPTCHA verification).
     */
    @Bean
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }
}