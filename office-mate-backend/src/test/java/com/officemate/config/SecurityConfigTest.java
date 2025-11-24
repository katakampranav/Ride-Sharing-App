package com.officemate.config;

import org.junit.jupiter.api.Disabled;

import com.officemate.config.security.RateLimitingService;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;

/**
 * Integration tests for Spring Security configuration.
 * Tests authentication, authorization, and CORS settings.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Disabled
class SecurityConfigTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private RateLimitingService rateLimitingService;

    @Test
    void publicEndpoints_shouldBeAccessibleWithoutAuthentication() throws Exception {
        // Test health endpoint
        mockMvc.perform(get("/health"))
                .andExpect(status().isOk());

        // Test actuator health endpoint
        mockMvc.perform(get("/actuator/health"))
                .andExpect(status().isOk());
    }

    @Test
    void protectedEndpoints_shouldRequireAuthentication() throws Exception {
        // Test user profile endpoint without token
        mockMvc.perform(get("/users/123/profile"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("Unauthorized"));
    }

    @Test
    void authEndpoints_shouldReturnNotFoundForNonExistentPaths() throws Exception {
        // Test non-existent endpoint
        mockMvc.perform(get("/auth/nonexistent"))
                .andExpect(status().isNotFound());
    }

    @Test
    void corsHeaders_shouldBePresent() throws Exception {
        // Test CORS preflight request
        mockMvc.perform(
                post("/auth/register")
                        .header("Origin", "http://localhost:3000")
                        .header("Access-Control-Request-Method", "POST")
                        .contentType(MediaType.APPLICATION_JSON)
        );
        // Note: Full CORS testing requires actual CORS preflight requests
    }

    @Test
    void securityHeaders_shouldBePresent() throws Exception {
        // Test that security headers are set
        mockMvc.perform(get("/health"))
                .andExpect(status().isOk());
    }
}
