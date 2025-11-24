package com.officemate.modules.auth.service;

import com.officemate.modules.auth.entity.OTPRecord;
import com.officemate.modules.auth.repository.OTPRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.concurrent.TimeUnit;

/**
 * Service for OTP generation, verification, and rate limiting using Redis.
 * Implements secure OTP handling with hashing and TTL-based expiration.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class OTPService {

    private final OTPRepository otpRepository;
    private final RedisTemplate<String, String> redisTemplate;
    private final SecureRandom secureRandom = new SecureRandom();

    @Value("${app.otp.length:6}")
    private int otpLength;

    @Value("${app.otp.expiration-minutes:5}")
    private int otpExpirationMinutes;

    @Value("${app.otp.max-attempts:3}")
    private int maxAttempts;

    @Value("${app.rate-limit.otp-requests-per-hour:5}")
    private int otpRequestsPerHour;

    private static final String RATE_LIMIT_PREFIX = "rate_limit:otp:";
    private static final String OTP_PHONE_PREFIX = "phone:";
    private static final String OTP_EMAIL_PREFIX = "email:";

    /**
     * Generate and store OTP for mobile number.
     *
     * @param phoneNumber the phone number to generate OTP for
     * @return the generated OTP (plain text, to be sent via SMS)
     * @throws IllegalStateException if rate limit exceeded
     */
    public String generateMobileOTP(String phoneNumber) {
        log.info("Generating mobile OTP for: {}", maskIdentifier(phoneNumber));

        // Check rate limiting
        checkRateLimit(phoneNumber);

        // Generate OTP
        String otp = generateOTP();
        String key = OTP_PHONE_PREFIX + phoneNumber;

        // Hash OTP before storing
        String otpHash = hashOTP(otp);

        // Create OTP record with TTL
        OTPRecord otpRecord = OTPRecord.builder()
                .key(key)
                .otpHash(otpHash)
                .attempts(0)
                .createdAt(LocalDateTime.now())
                .expiresAt(LocalDateTime.now().plusMinutes(otpExpirationMinutes))
                .verified(false)
                .type(OTPRecord.OTPType.MOBILE)
                .timeToLive((long) (otpExpirationMinutes * 60)) // Convert to seconds
                .build();

        // Save to Redis (will auto-expire after TTL)
        otpRepository.save(otpRecord);

        // Increment rate limit counter
        incrementRateLimit(phoneNumber);

        log.info("Mobile OTP generated and stored in Redis with TTL: {} minutes", otpExpirationMinutes);

        return otp;
    }

    /**
     * Generate and store OTP for email.
     *
     * @param email the email to generate OTP for
     * @return the generated OTP (plain text, to be sent via email)
     * @throws IllegalStateException if rate limit exceeded
     */
    public String generateEmailOTP(String email) {
        log.info("Generating email OTP for: {}", maskIdentifier(email));

        // Check rate limiting
        checkRateLimit(email);

        // Generate OTP
        String otp = generateOTP();
        String key = OTP_EMAIL_PREFIX + email;

        // Hash OTP before storing
        String otpHash = hashOTP(otp);

        // Create OTP record with TTL
        OTPRecord otpRecord = OTPRecord.builder()
                .key(key)
                .otpHash(otpHash)
                .attempts(0)
                .createdAt(LocalDateTime.now())
                .expiresAt(LocalDateTime.now().plusMinutes(otpExpirationMinutes))
                .verified(false)
                .type(OTPRecord.OTPType.EMAIL)
                .timeToLive((long) (otpExpirationMinutes * 60)) // Convert to seconds
                .build();

        // Save to Redis (will auto-expire after TTL)
        otpRepository.save(otpRecord);

        // Increment rate limit counter
        incrementRateLimit(email);

        log.info("Email OTP generated and stored in Redis with TTL: {} minutes", otpExpirationMinutes);

        return otp;
    }

    /**
     * Verify mobile OTP.
     *
     * @param phoneNumber the phone number to verify
     * @param otp the OTP to verify
     * @return true if OTP is valid, false otherwise
     * @throws IllegalArgumentException if OTP not found or expired
     */
    public boolean verifyMobileOTP(String phoneNumber, String otp) {
        log.info("Verifying mobile OTP for: {}", maskIdentifier(phoneNumber));

        String key = OTP_PHONE_PREFIX + phoneNumber;
        return verifyOTP(key, otp);
    }

    /**
     * Verify email OTP.
     *
     * @param email the email to verify
     * @param otp the OTP to verify
     * @return true if OTP is valid, false otherwise
     * @throws IllegalArgumentException if OTP not found or expired
     */
    public boolean verifyEmailOTP(String email, String otp) {
        log.info("Verifying email OTP for: {}", maskIdentifier(email));

        String key = OTP_EMAIL_PREFIX + email;
        return verifyOTP(key, otp);
    }

    /**
     * Internal method to verify OTP.
     *
     * @param key the Redis key
     * @param otp the OTP to verify
     * @return true if valid, false otherwise
     */
    private boolean verifyOTP(String key, String otp) {
        // Find OTP record
        OTPRecord otpRecord = otpRepository.findByKey(key)
                .orElseThrow(() -> new IllegalArgumentException("OTP not found or expired"));

        // Check if already verified
        if (otpRecord.getVerified()) {
            log.warn("OTP already verified for key: {}", maskIdentifier(key));
            throw new IllegalArgumentException("OTP already used");
        }

        // Check if expired
        if (otpRecord.isExpired()) {
            log.warn("OTP expired for key: {}", maskIdentifier(key));
            otpRepository.delete(otpRecord);
            throw new IllegalArgumentException("OTP expired");
        }

        // Check max attempts
        if (otpRecord.isMaxAttemptsReached(maxAttempts)) {
            log.warn("Max OTP attempts reached for key: {}", maskIdentifier(key));
            otpRepository.delete(otpRecord);
            throw new IllegalArgumentException("Maximum verification attempts exceeded");
        }

        // Verify OTP hash
        String otpHash = hashOTP(otp);
        boolean isValid = otpHash.equals(otpRecord.getOtpHash());

        if (isValid) {
            // Mark as verified
            otpRecord.setVerified(true);
            otpRepository.save(otpRecord);
            log.info("OTP verified successfully for key: {}", maskIdentifier(key));
        } else {
            // Increment attempts
            otpRecord.incrementAttempts();
            otpRepository.save(otpRecord);
            log.warn("Invalid OTP attempt {} of {} for key: {}", 
                    otpRecord.getAttempts(), maxAttempts, maskIdentifier(key));
        }

        return isValid;
    }

    /**
     * Delete OTP record (used after successful verification or manual cleanup).
     *
     * @param phoneNumber the phone number
     */
    public void deleteMobileOTP(String phoneNumber) {
        String key = OTP_PHONE_PREFIX + phoneNumber;
        otpRepository.deleteByKey(key);
        log.info("Mobile OTP deleted for: {}", maskIdentifier(phoneNumber));
    }

    /**
     * Delete email OTP record.
     *
     * @param email the email
     */
    public void deleteEmailOTP(String email) {
        String key = OTP_EMAIL_PREFIX + email;
        otpRepository.deleteByKey(key);
        log.info("Email OTP deleted for: {}", maskIdentifier(email));
    }

    /**
     * Generate random numeric OTP.
     *
     * @return OTP string
     */
    private String generateOTP() {
        StringBuilder otp = new StringBuilder();
        for (int i = 0; i < otpLength; i++) {
            otp.append(secureRandom.nextInt(10));
        }
        return otp.toString();
    }

    /**
     * Hash OTP using SHA-256 for secure storage.
     *
     * @param otp the plain text OTP
     * @return hashed OTP in Base64 format
     */
    private String hashOTP(String otp) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(otp.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(hash);
        } catch (NoSuchAlgorithmException e) {
            log.error("SHA-256 algorithm not available", e);
            throw new RuntimeException("Failed to hash OTP", e);
        }
    }

    /**
     * Check rate limiting for OTP requests.
     *
     * @param identifier phone number or email
     * @throws IllegalStateException if rate limit exceeded
     */
    private void checkRateLimit(String identifier) {
        String rateLimitKey = RATE_LIMIT_PREFIX + identifier;
        String countStr = redisTemplate.opsForValue().get(rateLimitKey);
        
        if (countStr != null) {
            int count = Integer.parseInt(countStr);
            if (count >= otpRequestsPerHour) {
                log.warn("Rate limit exceeded for: {}", maskIdentifier(identifier));
                throw new IllegalStateException("Too many OTP requests. Please try again later.");
            }
        }
    }

    /**
     * Increment rate limit counter.
     *
     * @param identifier phone number or email
     */
    private void incrementRateLimit(String identifier) {
        String rateLimitKey = RATE_LIMIT_PREFIX + identifier;
        Long count = redisTemplate.opsForValue().increment(rateLimitKey);
        
        // Set expiry on first request (1 hour)
        if (count != null && count == 1) {
            redisTemplate.expire(rateLimitKey, 1, TimeUnit.HOURS);
        }
    }

    /**
     * Mask identifier for logging (shows only last 4 characters).
     *
     * @param identifier phone number or email
     * @return masked identifier
     */
    private String maskIdentifier(String identifier) {
        if (identifier == null || identifier.length() < 4) {
            return "****";
        }
        return "****" + identifier.substring(identifier.length() - 4);
    }

    /**
     * Get remaining OTP attempts.
     *
     * @param phoneNumber the phone number
     * @return remaining attempts
     */
    public int getRemainingAttempts(String phoneNumber) {
        String key = OTP_PHONE_PREFIX + phoneNumber;
        return otpRepository.findByKey(key)
                .map(record -> maxAttempts - (record.getAttempts() != null ? record.getAttempts() : 0))
                .orElse(maxAttempts);
    }

    /**
     * Check if OTP exists and is valid.
     *
     * @param phoneNumber the phone number
     * @return true if OTP exists and not expired
     */
    public boolean hasValidOTP(String phoneNumber) {
        String key = OTP_PHONE_PREFIX + phoneNumber;
        return otpRepository.findByKey(key)
                .map(record -> !record.isExpired() && !record.getVerified())
                .orElse(false);
    }
}
