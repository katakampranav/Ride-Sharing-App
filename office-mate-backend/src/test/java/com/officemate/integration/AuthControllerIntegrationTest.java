package com.officemate.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.officemate.modules.auth.entity.UserAccount;
import com.officemate.modules.auth.repository.UserAccountRepository;
import com.officemate.shared.dto.*;
import com.officemate.shared.enums.AccountStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for authentication endpoints.
 * Tests complete authentication flows including registration, OTP verification, and login.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Disabled
class AuthControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserAccountRepository userAccountRepository;

    private String testPhoneNumber;

    @BeforeEach
    void setUp() {
        // Clean up test data
        userAccountRepository.deleteAll();
        testPhoneNumber = "+1234567890";
    }

    @Test
    void testCompleteRegistrationFlow() throws Exception {
        // Step 1: Register with phone number
        RegisterRequest registerRequest = new RegisterRequest();
        registerRequest.setPhoneNumber(testPhoneNumber);

        MvcResult registerResult = mockMvc.perform(post("/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(registerRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.userId").exists())
                .andExpect(jsonPath("$.otpSent").value(true))
                .andReturn();

        String registerResponse = registerResult.getResponse().getContentAsString();
        RegistrationResponse registrationResponse = objectMapper.readValue(registerResponse, RegistrationResponse.class);
        
        assertNotNull(registrationResponse.getUserId());
        assertTrue(registrationResponse.isOtpSent());

        // Verify user account was created
        UserAccount userAccount = userAccountRepository.findByPhoneNumber(testPhoneNumber).orElse(null);
        assertNotNull(userAccount);
        assertFalse(userAccount.getPhoneVerified());
    }

    @Test
    void testLoginFlow() throws Exception {
        // Create a verified user first
        UserAccount user = new UserAccount();
        user.setPhoneNumber(testPhoneNumber);
        user.setPhoneVerified(true);
        user.setAccountStatus(AccountStatus.PENDING_EMAIL);
        userAccountRepository.save(user);

        // Step 1: Login with phone number
        LoginRequest loginRequest = new LoginRequest();
        loginRequest.setPhoneNumber(testPhoneNumber);

        mockMvc.perform(post("/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.otpSent").value(true));
    }

    @Test
    void testRegistrationWithInvalidPhoneNumber() throws Exception {
        RegisterRequest registerRequest = new RegisterRequest();
        registerRequest.setPhoneNumber("invalid");

        mockMvc.perform(post("/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(registerRequest)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void testDuplicateRegistration() throws Exception {
        // Create existing user
        UserAccount existingUser = new UserAccount();
        existingUser.setPhoneNumber(testPhoneNumber);
        existingUser.setPhoneVerified(true);
        existingUser.setAccountStatus(AccountStatus.ACTIVE);
        userAccountRepository.save(existingUser);

        // Try to register again
        RegisterRequest registerRequest = new RegisterRequest();
        registerRequest.setPhoneNumber(testPhoneNumber);

        mockMvc.perform(post("/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(registerRequest)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void testHealthEndpoint() throws Exception {
        mockMvc.perform(get("/auth/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"))
                .andExpect(jsonPath("$.service").value("authentication"));
    }
}
