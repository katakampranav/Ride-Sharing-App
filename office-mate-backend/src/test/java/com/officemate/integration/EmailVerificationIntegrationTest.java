package com.officemate.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.officemate.modules.auth.entity.UserAccount;
import com.officemate.modules.auth.repository.UserAccountRepository;
import com.officemate.shared.dto.AddCorporateEmailRequest;
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
 * Integration tests for corporate email verification workflow.
 * Tests email addition, OTP verification, and email update functionality.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Disabled
class EmailVerificationIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserAccountRepository userAccountRepository;

    private UUID testUserId;
    private UserAccount testUser;

    @BeforeEach
    void setUp() {
        // Clean up test data
        userAccountRepository.deleteAll();

        // Create test user with mobile verification only
        testUser = new UserAccount();
        testUser.setPhoneNumber("+1234567890");
        testUser.setPhoneVerified(true);
        testUser.setEmailVerified(false);
        testUser.setAccountStatus(AccountStatus.PENDING_EMAIL);
        testUser = userAccountRepository.save(testUser);
        testUserId = testUser.getUserId();
    }

    @Test
    @WithMockUser(username = "testUserId", authorities = {"MOBILE_VERIFIED"})
    void testAddCorporateEmail() throws Exception {
        AddCorporateEmailRequest request = new AddCorporateEmailRequest();
        request.setCorporateEmail("test@company.com");

        mockMvc.perform(post("/auth/add-corporate-email")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.otpSent").value(true))
                .andExpect(jsonPath("$.maskedEmail").exists());
    }

    @Test
    @WithMockUser(username = "testUserId", authorities = {"MOBILE_VERIFIED"})
    void testAddInvalidCorporateEmail() throws Exception {
        AddCorporateEmailRequest request = new AddCorporateEmailRequest();
        request.setCorporateEmail("invalid-email");

        mockMvc.perform(post("/auth/add-corporate-email")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void testAddCorporateEmailWithoutAuthentication() throws Exception {
        AddCorporateEmailRequest request = new AddCorporateEmailRequest();
        request.setCorporateEmail("test@company.com");

        mockMvc.perform(post("/auth/add-corporate-email")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(username = "testUserId", authorities = {"MOBILE_VERIFIED"})
    void testResendEmailOTP() throws Exception {
        // First add corporate email
        testUser.setCorporateEmail("test@company.com");
        userAccountRepository.save(testUser);

        mockMvc.perform(post("/auth/resend-email-otp"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.otpSent").value(true));
    }

    @Test
    @WithMockUser(username = "testUserId", authorities = {"MOBILE_VERIFIED"})
    void testAddDuplicateCorporateEmail() throws Exception {
        // Create another user with the same email
        UserAccount existingUser = new UserAccount();
        existingUser.setPhoneNumber("+9876543210");
        existingUser.setPhoneVerified(true);
        existingUser.setCorporateEmail("existing@company.com");
        existingUser.setEmailVerified(true);
        existingUser.setAccountStatus(AccountStatus.ACTIVE);
        userAccountRepository.save(existingUser);

        // Try to add the same email to test user
        AddCorporateEmailRequest request = new AddCorporateEmailRequest();
        request.setCorporateEmail("existing@company.com");

        mockMvc.perform(post("/auth/add-corporate-email")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }
}
