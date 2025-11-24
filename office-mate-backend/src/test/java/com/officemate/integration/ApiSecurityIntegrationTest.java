package com.officemate.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.officemate.modules.auth.entity.UserAccount;
import com.officemate.modules.auth.repository.UserAccountRepository;
import com.officemate.shared.dto.RegisterRequest;
import com.officemate.shared.enums.AccountStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration tests for API security and validation.
 * Tests authentication requirements, authorization, input validation, and error handling.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Disabled
class ApiSecurityIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserAccountRepository userAccountRepository;

    private UUID testUserId;

    @BeforeEach
    void setUp() {
        userAccountRepository.deleteAll();

        // Create test user
        UserAccount testUser = new UserAccount();
        testUser.setPhoneNumber("+1234567890");
        testUser.setPhoneVerified(true);
        testUser.setCorporateEmail("test@company.com");
        testUser.setEmailVerified(true);
        testUser.setAccountStatus(AccountStatus.ACTIVE);
        testUser = userAccountRepository.save(testUser);
        testUserId = testUser.getUserId();
    }

    @Test
    void testUnauthenticatedAccessToProtectedEndpoint() throws Exception {
        mockMvc.perform(get("/users/" + testUserId + "/profile"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(username = "differentUserId", authorities = {"MOBILE_VERIFIED"})
    void testAccessAnotherUserProfile() throws Exception {
        // User trying to access another user's profile should be forbidden
        mockMvc.perform(get("/users/" + testUserId + "/profile"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "testUserId", authorities = {"MOBILE_VERIFIED"})
    void testAccessEmailVerifiedEndpointWithoutEmailVerification() throws Exception {
        mockMvc.perform(post("/users/" + testUserId + "/profile/driver-profile")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void testInvalidJsonPayload() throws Exception {
        mockMvc.perform(post("/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{invalid json"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void testMissingRequiredFields() throws Exception {
        RegisterRequest request = new RegisterRequest();
        // phoneNumber is null

        mockMvc.perform(post("/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void testInvalidPhoneNumberFormat() throws Exception {
        RegisterRequest request = new RegisterRequest();
        request.setPhoneNumber("invalid");

        mockMvc.perform(post("/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void testInvalidUuidFormat() throws Exception {
        mockMvc.perform(get("/users/invalid-uuid/profile"))
                .andExpect(status().isUnauthorized()); // Will fail auth first
    }

    @Test
    @WithMockUser(username = "testUserId", authorities = {"MOBILE_VERIFIED"})
    void testSqlInjectionPrevention() throws Exception {
        String sqlInjection = "'; DROP TABLE users; --";
        
        mockMvc.perform(get("/users/" + sqlInjection + "/profile"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(username = "testUserId", authorities = {"MOBILE_VERIFIED"})
    void testXssPreventionInInput() throws Exception {
        String xssPayload = "<script>alert('xss')</script>";
        RegisterRequest request = new RegisterRequest();
        request.setPhoneNumber(xssPayload);

        mockMvc.perform(post("/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void testCorsHeaders() throws Exception {
        mockMvc.perform(options("/auth/health")
                .header("Origin", "http://localhost:3000")
                .header("Access-Control-Request-Method", "GET"))
                .andExpect(status().isOk());
    }

    @Test
    void testContentTypeValidation() throws Exception {
        mockMvc.perform(post("/auth/register")
                .contentType(MediaType.TEXT_PLAIN)
                .content("plain text"))
                .andExpect(status().isUnsupportedMediaType());
    }

    @Test
    @WithMockUser(username = "testUserId", authorities = {"MOBILE_VERIFIED", "EMAIL_VERIFIED"})
    void testValidAuthenticatedRequest() throws Exception {
        mockMvc.perform(get("/users/" + testUserId + "/profile"))
                .andExpect(status().isOk());
    }

    @Test
    void testPublicEndpointAccess() throws Exception {
        mockMvc.perform(get("/auth/health"))
                .andExpect(status().isOk());
    }

    @Test
    void testMethodNotAllowed() throws Exception {
        mockMvc.perform(delete("/auth/health"))
                .andExpect(status().isMethodNotAllowed());
    }

    @Test
    @WithMockUser(username = "testUserId", authorities = {"MOBILE_VERIFIED"})
    void testLargePayloadRejection() throws Exception {
        // Create a very large payload
        StringBuilder largePayload = new StringBuilder("{\"phoneNumber\":\"");
        for (int i = 0; i < 100000; i++) {
            largePayload.append("a");
        }
        largePayload.append("\"}");

        mockMvc.perform(post("/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(largePayload.toString()))
                .andExpect(status().isBadRequest());
    }
}
