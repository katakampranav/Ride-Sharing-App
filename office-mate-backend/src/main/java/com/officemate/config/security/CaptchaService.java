package com.officemate.config.security;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Service for CAPTCHA integration and verification.
 * Provides CAPTCHA challenges for high-risk authentication scenarios.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class CaptchaService {

    private final RedisTemplate<String, String> redisTemplate;
    private final RestTemplate restTemplate;

    @Value("${app.captcha.enabled:true}")
    private boolean captchaEnabled;

    @Value("${app.captcha.site-key:}")
    private String captchaSiteKey;

    @Value("${app.captcha.secret-key:}")
    private String captchaSecretKey;

    @Value("${app.captcha.verify-url:https://www.google.com/recaptcha/api/siteverify}")
    private String captchaVerifyUrl;

    private static final String CAPTCHA_CHALLENGE_PREFIX = "captcha_challenge:";
    private static final String CAPTCHA_BYPASS_PREFIX = "captcha_bypass:";

    /**
     * Generate a CAPTCHA challenge for a user.
     *
     * @param identifier user identifier
     * @return CAPTCHA challenge data
     */
    public Map<String, Object> generateCaptchaChallenge(String identifier) {
        Map<String, Object> challenge = new HashMap<>();
        
        if (!captchaEnabled) {
            challenge.put("required", false);
            challenge.put("message", "CAPTCHA disabled in configuration");
            return challenge;
        }

        try {
            String challengeId = UUID.randomUUID().toString();
            String challengeKey = CAPTCHA_CHALLENGE_PREFIX + challengeId;
            
            // Store challenge in Redis with 10-minute expiry
            redisTemplate.opsForValue().set(challengeKey, identifier, Duration.ofMinutes(10));
            
            challenge.put("required", true);
            challenge.put("challengeId", challengeId);
            challenge.put("siteKey", captchaSiteKey);
            challenge.put("message", "Please complete the CAPTCHA verification");
            
            log.debug("Generated CAPTCHA challenge {} for identifier: {}", challengeId, identifier);
            
        } catch (Exception e) {
            log.error("Error generating CAPTCHA challenge for identifier: {}", identifier, e);
            challenge.put("required", false);
            challenge.put("error", "CAPTCHA service temporarily unavailable");
        }
        
        return challenge;
    }

    /**
     * Verify a CAPTCHA response.
     *
     * @param challengeId the challenge ID
     * @param captchaResponse the CAPTCHA response from client
     * @param clientIp client IP address
     * @return true if CAPTCHA is valid
     */
    public boolean verifyCaptcha(String challengeId, String captchaResponse, String clientIp) {
        if (!captchaEnabled) {
            log.debug("CAPTCHA verification skipped - disabled in configuration");
            return true;
        }

        try {
            String challengeKey = CAPTCHA_CHALLENGE_PREFIX + challengeId;
            String storedIdentifier = redisTemplate.opsForValue().get(challengeKey);
            
            if (storedIdentifier == null) {
                log.warn("CAPTCHA challenge not found or expired: {}", challengeId);
                return false;
            }
            
            // Verify with Google reCAPTCHA service
            boolean isValid = verifyWithRecaptchaService(captchaResponse, clientIp);
            
            if (isValid) {
                // Remove challenge after successful verification
                redisTemplate.delete(challengeKey);
                
                // Grant temporary bypass for successful verification (5 minutes)
                grantCaptchaBypass(storedIdentifier);
                
                log.info("CAPTCHA verification successful for challenge: {}", challengeId);
            } else {
                log.warn("CAPTCHA verification failed for challenge: {}", challengeId);
            }
            
            return isValid;
            
        } catch (Exception e) {
            log.error("Error verifying CAPTCHA for challenge: {}", challengeId, e);
            return false;
        }
    }

    /**
     * Check if user has a valid CAPTCHA bypass.
     *
     * @param identifier user identifier
     * @return true if user has valid bypass
     */
    public boolean hasCaptchaBypass(String identifier) {
        if (!captchaEnabled) {
            return true;
        }

        try {
            String bypassKey = CAPTCHA_BYPASS_PREFIX + identifier;
            String bypassValue = redisTemplate.opsForValue().get(bypassKey);
            return bypassValue != null;
            
        } catch (Exception e) {
            log.error("Error checking CAPTCHA bypass for identifier: {}", identifier, e);
            return false;
        }
    }

    /**
     * Grant temporary CAPTCHA bypass after successful verification.
     *
     * @param identifier user identifier
     */
    private void grantCaptchaBypass(String identifier) {
        try {
            String bypassKey = CAPTCHA_BYPASS_PREFIX + identifier;
            redisTemplate.opsForValue().set(bypassKey, "granted", Duration.ofMinutes(5));
            
            log.debug("Granted CAPTCHA bypass for identifier: {}", identifier);
            
        } catch (Exception e) {
            log.error("Error granting CAPTCHA bypass for identifier: {}", identifier, e);
        }
    }

    /**
     * Verify CAPTCHA response with Google reCAPTCHA service.
     *
     * @param captchaResponse the CAPTCHA response
     * @param clientIp client IP address
     * @return true if verification successful
     */
    private boolean verifyWithRecaptchaService(String captchaResponse, String clientIp) {
        try {
            Map<String, String> requestBody = new HashMap<>();
            requestBody.put("secret", captchaSecretKey);
            requestBody.put("response", captchaResponse);
            requestBody.put("remoteip", clientIp);
            
            @SuppressWarnings("unchecked")
            Map<String, Object> response = restTemplate.postForObject(
                    captchaVerifyUrl, requestBody, Map.class);
            
            if (response != null && response.containsKey("success")) {
                Boolean success = (Boolean) response.get("success");
                
                if (success != null && success) {
                    log.debug("reCAPTCHA verification successful");
                    return true;
                } else {
                    log.warn("reCAPTCHA verification failed: {}", response.get("error-codes"));
                }
            }
            
            return false;
            
        } catch (Exception e) {
            log.error("Error calling reCAPTCHA service: {}", e.getMessage(), e);
            return false;
        }
    }

    /**
     * Invalidate CAPTCHA bypass for security events.
     *
     * @param identifier user identifier
     */
    public void invalidateCaptchaBypass(String identifier) {
        try {
            String bypassKey = CAPTCHA_BYPASS_PREFIX + identifier;
            redisTemplate.delete(bypassKey);
            
            log.debug("Invalidated CAPTCHA bypass for identifier: {}", identifier);
            
        } catch (Exception e) {
            log.error("Error invalidating CAPTCHA bypass for identifier: {}", identifier, e);
        }
    }

    /**
     * Clean up expired CAPTCHA challenges (called by scheduled task).
     */
    public void cleanupExpiredChallenges() {
        try {
            // Redis TTL handles cleanup automatically, but we can log statistics
            log.debug("CAPTCHA cleanup task executed");
            
        } catch (Exception e) {
            log.error("Error during CAPTCHA cleanup: {}", e.getMessage(), e);
        }
    }
}