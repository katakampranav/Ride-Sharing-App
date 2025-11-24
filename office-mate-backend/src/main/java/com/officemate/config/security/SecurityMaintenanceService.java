package com.officemate.config.security;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

/**
 * Service for scheduled security maintenance tasks.
 * Handles cleanup of expired data and monitoring of security metrics.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class SecurityMaintenanceService {

    private final CaptchaService captchaService;

    /**
     * Clean up expired CAPTCHA challenges.
     * Runs every hour to clean up expired challenges and maintain Redis performance.
     */
    @Scheduled(fixedRate = 3600000) // 1 hour
    public void cleanupExpiredChallenges() {
        try {
            log.debug("Starting scheduled cleanup of expired CAPTCHA challenges");
            captchaService.cleanupExpiredChallenges();
            log.debug("Completed cleanup of expired CAPTCHA challenges");
        } catch (Exception e) {
            log.error("Error during CAPTCHA cleanup: {}", e.getMessage(), e);
        }
    }

    /**
     * Log security metrics for monitoring.
     * Runs every 15 minutes to log security statistics.
     */
    @Scheduled(fixedRate = 900000) // 15 minutes
    public void logSecurityMetrics() {
        try {
            log.info("Security maintenance task executed - monitoring active");
            // Additional metrics logging can be added here
        } catch (Exception e) {
            log.error("Error during security metrics logging: {}", e.getMessage(), e);
        }
    }
}