package com.officemate.config.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CaptchaServiceTest {

    @Mock
    private RedisTemplate<String, String> redisTemplate;

    @Mock
    private RestTemplate restTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    private CaptchaService captchaService;

    @BeforeEach
    void setUp() {
        captchaService = new CaptchaService(redisTemplate, restTemplate);
        
        // Set test configuration values
        ReflectionTestUtils.setField(captchaService, "captchaEnabled", true);
        ReflectionTestUtils.setField(captchaService, "captchaSiteKey", "test-site-key");
        ReflectionTestUtils.setField(captchaService, "captchaSecretKey", "test-secret-key");
        ReflectionTestUtils.setField(captchaService, "captchaVerifyUrl", "https://test-verify-url");
    }

    @Test
    void testGenerateCaptchaChallenge_WhenEnabled_ShouldReturnChallenge() {
        // Arrange
        String identifier = "test_user";
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);

        // Act
        Map<String, Object> result = captchaService.generateCaptchaChallenge(identifier);

        // Assert
        assertTrue((Boolean) result.get("required"));
        assertNotNull(result.get("challengeId"));
        assertEquals("test-site-key", result.get("siteKey"));
        assertEquals("Please complete the CAPTCHA verification", result.get("message"));
        
        verify(valueOperations).set(anyString(), eq(identifier), eq(Duration.ofMinutes(10)));
    }

    @Test
    void testGenerateCaptchaChallenge_WhenDisabled_ShouldReturnNotRequired() {
        // Arrange
        ReflectionTestUtils.setField(captchaService, "captchaEnabled", false);
        String identifier = "test_user";

        // Act
        Map<String, Object> result = captchaService.generateCaptchaChallenge(identifier);

        // Assert
        assertFalse((Boolean) result.get("required"));
        assertEquals("CAPTCHA disabled in configuration", result.get("message"));
    }

    @Test
    void testVerifyCaptcha_WhenDisabled_ShouldReturnTrue() {
        // Arrange
        ReflectionTestUtils.setField(captchaService, "captchaEnabled", false);
        String challengeId = "test-challenge";
        String captchaResponse = "test-response";
        String clientIp = "192.168.1.1";

        // Act
        boolean result = captchaService.verifyCaptcha(challengeId, captchaResponse, clientIp);

        // Assert
        assertTrue(result);
    }

    @Test
    void testVerifyCaptcha_ValidChallenge_ShouldReturnTrue() {
        // Arrange
        String challengeId = "test-challenge";
        String captchaResponse = "test-response";
        String clientIp = "192.168.1.1";
        String identifier = "test_user";

        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get("captcha_challenge:" + challengeId)).thenReturn(identifier);
        
        // Mock successful reCAPTCHA response
        Map<String, Object> recaptchaResponse = new HashMap<>();
        recaptchaResponse.put("success", true);
        when(restTemplate.postForObject(anyString(), any(), eq(Map.class))).thenReturn(recaptchaResponse);

        // Act
        boolean result = captchaService.verifyCaptcha(challengeId, captchaResponse, clientIp);

        // Assert
        assertTrue(result);
        verify(redisTemplate).delete("captcha_challenge:" + challengeId);
        verify(valueOperations).set("captcha_bypass:" + identifier, "granted", Duration.ofMinutes(5));
    }

    @Test
    void testVerifyCaptcha_InvalidChallenge_ShouldReturnFalse() {
        // Arrange
        String challengeId = "test-challenge";
        String captchaResponse = "test-response";
        String clientIp = "192.168.1.1";

        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get("captcha_challenge:" + challengeId)).thenReturn(null);

        // Act
        boolean result = captchaService.verifyCaptcha(challengeId, captchaResponse, clientIp);

        // Assert
        assertFalse(result);
    }

    @Test
    void testVerifyCaptcha_FailedRecaptchaVerification_ShouldReturnFalse() {
        // Arrange
        String challengeId = "test-challenge";
        String captchaResponse = "test-response";
        String clientIp = "192.168.1.1";
        String identifier = "test_user";

        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get("captcha_challenge:" + challengeId)).thenReturn(identifier);
        
        // Mock failed reCAPTCHA response
        Map<String, Object> recaptchaResponse = new HashMap<>();
        recaptchaResponse.put("success", false);
        when(restTemplate.postForObject(anyString(), any(), eq(Map.class))).thenReturn(recaptchaResponse);

        // Act
        boolean result = captchaService.verifyCaptcha(challengeId, captchaResponse, clientIp);

        // Assert
        assertFalse(result);
        verify(redisTemplate, never()).delete("captcha_challenge:" + challengeId);
    }

    @Test
    void testHasCaptchaBypass_WhenDisabled_ShouldReturnTrue() {
        // Arrange
        ReflectionTestUtils.setField(captchaService, "captchaEnabled", false);
        String identifier = "test_user";

        // Act
        boolean result = captchaService.hasCaptchaBypass(identifier);

        // Assert
        assertTrue(result);
    }

    @Test
    void testHasCaptchaBypass_WithValidBypass_ShouldReturnTrue() {
        // Arrange
        String identifier = "test_user";

        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get("captcha_bypass:" + identifier)).thenReturn("granted");

        // Act
        boolean result = captchaService.hasCaptchaBypass(identifier);

        // Assert
        assertTrue(result);
    }

    @Test
    void testHasCaptchaBypass_WithoutBypass_ShouldReturnFalse() {
        // Arrange
        String identifier = "test_user";

        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get("captcha_bypass:" + identifier)).thenReturn(null);

        // Act
        boolean result = captchaService.hasCaptchaBypass(identifier);

        // Assert
        assertFalse(result);
    }

    @Test
    void testInvalidateCaptchaBypass_ShouldDeleteBypass() {
        // Arrange
        String identifier = "test_user";

        // Act
        captchaService.invalidateCaptchaBypass(identifier);

        // Assert
        verify(redisTemplate).delete("captcha_bypass:" + identifier);
    }

    @Test
    void testCleanupExpiredChallenges_ShouldExecuteWithoutError() {
        // Act & Assert - should not throw exception
        assertDoesNotThrow(() -> captchaService.cleanupExpiredChallenges());
    }
}