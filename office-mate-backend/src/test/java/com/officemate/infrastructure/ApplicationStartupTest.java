package com.officemate.infrastructure;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests Spring Boot application startup and configuration.
 * Verifies that all beans are properly configured and the application context loads successfully.
 */
@SpringBootTest
@ActiveProfiles("test")
@Disabled
class ApplicationStartupTest {

    @Autowired
    private ApplicationContext applicationContext;

    @Test
    void contextLoads() {
        assertThat(applicationContext).isNotNull();
    }

    @Test
    void allRequiredBeansArePresent() {
        // Verify core service beans
        assertThat(applicationContext.containsBean("mobileAuthService")).isTrue();
        assertThat(applicationContext.containsBean("corporateEmailVerificationService")).isTrue();
        assertThat(applicationContext.containsBean("userProfileService")).isTrue();
        assertThat(applicationContext.containsBean("driverProfileService")).isTrue();
        assertThat(applicationContext.containsBean("riderProfileService")).isTrue();
        assertThat(applicationContext.containsBean("walletService")).isTrue();
        assertThat(applicationContext.containsBean("safetyService")).isTrue();
        assertThat(applicationContext.containsBean("sessionManagementService")).isTrue();
        assertThat(applicationContext.containsBean("otpService")).isTrue();
    }

    @Test
    void securityBeansAreConfigured() {
        assertThat(applicationContext.containsBean("jwtTokenProvider")).isTrue();
        assertThat(applicationContext.containsBean("rateLimitingService")).isTrue();
        assertThat(applicationContext.containsBean("captchaService")).isTrue();
        assertThat(applicationContext.containsBean("auditService")).isTrue();
        assertThat(applicationContext.containsBean("securityEventService")).isTrue();
    }

    @Test
    void repositoryBeansAreConfigured() {
        assertThat(applicationContext.containsBean("userAccountRepository")).isTrue();
        assertThat(applicationContext.containsBean("userProfileRepository")).isTrue();
        assertThat(applicationContext.containsBean("driverProfileRepository")).isTrue();
        assertThat(applicationContext.containsBean("riderProfileRepository")).isTrue();
        assertThat(applicationContext.containsBean("walletRepository")).isTrue();
        assertThat(applicationContext.containsBean("emailVerificationRepository")).isTrue();
    }
}
