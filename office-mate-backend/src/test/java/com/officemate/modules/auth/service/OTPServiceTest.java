package com.officemate.modules.auth.service;

import com.officemate.modules.auth.entity.OTPRecord;
import com.officemate.modules.auth.repository.OTPRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.mockito.Mockito.lenient;

/**
 * Unit tests for OTPService.
 * Tests OTP generation, verification, hashing, and rate limiting.
 */
@ExtendWith(MockitoExtension.class)
class OTPServiceTest {

    @Mock
    private OTPRepository otpRepository;

    @Mock
    private RedisTemplate<String, String> redisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    @InjectMocks
    private OTPService otpService;

    @BeforeEach
    void setUp() {
        // Set configuration values using reflection
        ReflectionTestUtils.setField(otpService, "otpLength", 6);
        ReflectionTestUtils.setField(otpService, "otpExpirationMinutes", 5);
        ReflectionTestUtils.setField(otpService, "maxAttempts", 3);
        ReflectionTestUtils.setField(otpService, "otpRequestsPerHour", 5);

        // Mock RedisTemplate operations with lenient stubbing
        lenient().when(redisTemplate.opsForValue()).thenReturn(valueOperations);
    }

    @Test
    void testGenerateMobileOTP_Success() {
        // Arrange
        String phoneNumber = "+1234567890";
        when(valueOperations.get(anyString())).thenReturn(null); // No rate limit
        when(valueOperations.increment(anyString())).thenReturn(1L);
        when(otpRepository.save(any(OTPRecord.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        String otp = otpService.generateMobileOTP(phoneNumber);

        // Assert
        assertNotNull(otp);
        assertEquals(6, otp.length());
        assertTrue(otp.matches("\\d{6}")); // Should be 6 digits
        verify(otpRepository, times(1)).save(any(OTPRecord.class));
        verify(valueOperations, times(1)).increment(anyString());
    }

    @Test
    void testGenerateEmailOTP_Success() {
        // Arrange
        String email = "user@company.com";
        when(valueOperations.get(anyString())).thenReturn(null); // No rate limit
        when(valueOperations.increment(anyString())).thenReturn(1L);
        when(otpRepository.save(any(OTPRecord.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        String otp = otpService.generateEmailOTP(email);

        // Assert
        assertNotNull(otp);
        assertEquals(6, otp.length());
        assertTrue(otp.matches("\\d{6}")); // Should be 6 digits
        verify(otpRepository, times(1)).save(any(OTPRecord.class));
        verify(valueOperations, times(1)).increment(anyString());
    }

    @Test
    void testGenerateMobileOTP_RateLimitExceeded() {
        // Arrange
        String phoneNumber = "+1234567890";
        when(valueOperations.get(anyString())).thenReturn("5"); // Rate limit reached

        // Act & Assert
        IllegalStateException exception = assertThrows(IllegalStateException.class, () -> {
            otpService.generateMobileOTP(phoneNumber);
        });

        assertTrue(exception.getMessage().contains("Too many OTP requests"));
        verify(otpRepository, never()).save(any(OTPRecord.class));
    }

    @Test
    void testVerifyMobileOTP_Success() {
        // Arrange
        String phoneNumber = "+1234567890";
        String otp = "123456";
        String key = "phone:" + phoneNumber;

        // Create a mock OTP record with the same hash
        OTPRecord otpRecord = OTPRecord.builder()
                .key(key)
                .otpHash(hashOTP(otp)) // Use same hash
                .attempts(0)
                .createdAt(LocalDateTime.now())
                .expiresAt(LocalDateTime.now().plusMinutes(5))
                .verified(false)
                .type(OTPRecord.OTPType.MOBILE)
                .build();

        when(otpRepository.findByKey(key)).thenReturn(Optional.of(otpRecord));
        when(otpRepository.save(any(OTPRecord.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        boolean result = otpService.verifyMobileOTP(phoneNumber, otp);

        // Assert
        assertTrue(result);
        verify(otpRepository, times(1)).save(any(OTPRecord.class));
    }

    @Test
    void testVerifyMobileOTP_InvalidOTP() {
        // Arrange
        String phoneNumber = "+1234567890";
        String correctOtp = "123456";
        String wrongOtp = "654321";
        String key = "phone:" + phoneNumber;

        OTPRecord otpRecord = OTPRecord.builder()
                .key(key)
                .otpHash(hashOTP(correctOtp))
                .attempts(0)
                .createdAt(LocalDateTime.now())
                .expiresAt(LocalDateTime.now().plusMinutes(5))
                .verified(false)
                .type(OTPRecord.OTPType.MOBILE)
                .build();

        when(otpRepository.findByKey(key)).thenReturn(Optional.of(otpRecord));
        when(otpRepository.save(any(OTPRecord.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        boolean result = otpService.verifyMobileOTP(phoneNumber, wrongOtp);

        // Assert
        assertFalse(result);
        verify(otpRepository, times(1)).save(any(OTPRecord.class));
    }

    @Test
    void testVerifyMobileOTP_NotFound() {
        // Arrange
        String phoneNumber = "+1234567890";
        String otp = "123456";
        String key = "phone:" + phoneNumber;

        when(otpRepository.findByKey(key)).thenReturn(Optional.empty());

        // Act & Assert
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            otpService.verifyMobileOTP(phoneNumber, otp);
        });

        assertTrue(exception.getMessage().contains("OTP not found or expired"));
    }

    @Test
    void testVerifyMobileOTP_Expired() {
        // Arrange
        String phoneNumber = "+1234567890";
        String otp = "123456";
        String key = "phone:" + phoneNumber;

        OTPRecord otpRecord = OTPRecord.builder()
                .key(key)
                .otpHash(hashOTP(otp))
                .attempts(0)
                .createdAt(LocalDateTime.now().minusMinutes(10))
                .expiresAt(LocalDateTime.now().minusMinutes(5)) // Expired
                .verified(false)
                .type(OTPRecord.OTPType.MOBILE)
                .build();

        when(otpRepository.findByKey(key)).thenReturn(Optional.of(otpRecord));

        // Act & Assert
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            otpService.verifyMobileOTP(phoneNumber, otp);
        });

        assertTrue(exception.getMessage().contains("OTP expired"));
        verify(otpRepository, times(1)).delete(otpRecord);
    }

    @Test
    void testVerifyMobileOTP_MaxAttemptsReached() {
        // Arrange
        String phoneNumber = "+1234567890";
        String otp = "123456";
        String key = "phone:" + phoneNumber;

        OTPRecord otpRecord = OTPRecord.builder()
                .key(key)
                .otpHash(hashOTP(otp))
                .attempts(3) // Max attempts reached
                .createdAt(LocalDateTime.now())
                .expiresAt(LocalDateTime.now().plusMinutes(5))
                .verified(false)
                .type(OTPRecord.OTPType.MOBILE)
                .build();

        when(otpRepository.findByKey(key)).thenReturn(Optional.of(otpRecord));

        // Act & Assert
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            otpService.verifyMobileOTP(phoneNumber, otp);
        });

        assertTrue(exception.getMessage().contains("Maximum verification attempts exceeded"));
        verify(otpRepository, times(1)).delete(otpRecord);
    }

    @Test
    void testVerifyMobileOTP_AlreadyVerified() {
        // Arrange
        String phoneNumber = "+1234567890";
        String otp = "123456";
        String key = "phone:" + phoneNumber;

        OTPRecord otpRecord = OTPRecord.builder()
                .key(key)
                .otpHash(hashOTP(otp))
                .attempts(0)
                .createdAt(LocalDateTime.now())
                .expiresAt(LocalDateTime.now().plusMinutes(5))
                .verified(true) // Already verified
                .type(OTPRecord.OTPType.MOBILE)
                .build();

        when(otpRepository.findByKey(key)).thenReturn(Optional.of(otpRecord));

        // Act & Assert
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            otpService.verifyMobileOTP(phoneNumber, otp);
        });

        assertTrue(exception.getMessage().contains("OTP already used"));
    }

    @Test
    void testDeleteMobileOTP() {
        // Arrange
        String phoneNumber = "+1234567890";

        // Act
        otpService.deleteMobileOTP(phoneNumber);

        // Assert
        verify(otpRepository, times(1)).deleteByKey("phone:" + phoneNumber);
    }

    @Test
    void testHasValidOTP_True() {
        // Arrange
        String phoneNumber = "+1234567890";
        String key = "phone:" + phoneNumber;

        OTPRecord otpRecord = OTPRecord.builder()
                .key(key)
                .otpHash("hash")
                .attempts(0)
                .createdAt(LocalDateTime.now())
                .expiresAt(LocalDateTime.now().plusMinutes(5))
                .verified(false)
                .type(OTPRecord.OTPType.MOBILE)
                .build();

        when(otpRepository.findByKey(key)).thenReturn(Optional.of(otpRecord));

        // Act
        boolean result = otpService.hasValidOTP(phoneNumber);

        // Assert
        assertTrue(result);
    }

    @Test
    void testHasValidOTP_False_Expired() {
        // Arrange
        String phoneNumber = "+1234567890";
        String key = "phone:" + phoneNumber;

        OTPRecord otpRecord = OTPRecord.builder()
                .key(key)
                .otpHash("hash")
                .attempts(0)
                .createdAt(LocalDateTime.now().minusMinutes(10))
                .expiresAt(LocalDateTime.now().minusMinutes(5)) // Expired
                .verified(false)
                .type(OTPRecord.OTPType.MOBILE)
                .build();

        when(otpRepository.findByKey(key)).thenReturn(Optional.of(otpRecord));

        // Act
        boolean result = otpService.hasValidOTP(phoneNumber);

        // Assert
        assertFalse(result);
    }

    /**
     * Helper method to hash OTP (same logic as in OTPService).
     */
    private String hashOTP(String otp) {
        try {
            java.security.MessageDigest digest = java.security.MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(otp.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            return java.util.Base64.getEncoder().encodeToString(hash);
        } catch (java.security.NoSuchAlgorithmException e) {
            throw new RuntimeException("Failed to hash OTP", e);
        }
    }
}
