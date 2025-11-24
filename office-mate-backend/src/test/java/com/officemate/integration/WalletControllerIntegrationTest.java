package com.officemate.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.officemate.modules.auth.entity.UserAccount;
import com.officemate.modules.auth.repository.UserAccountRepository;
import com.officemate.modules.wallet.entity.Wallet;
import com.officemate.modules.wallet.repository.WalletRepository;
import com.officemate.shared.dto.*;
import com.officemate.shared.enums.AccountStatus;
import com.officemate.shared.enums.PaymentMethodType;
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

import java.math.BigDecimal;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration tests for wallet management endpoints.
 * Tests wallet initialization, payment methods, and auto-reload functionality.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Disabled
class WalletControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserAccountRepository userAccountRepository;

    @Autowired
    private WalletRepository walletRepository;

    private UUID testUserId;
    private UserAccount testUser;

    @BeforeEach
    void setUp() {
        // Clean up test data
        walletRepository.deleteAll();
        userAccountRepository.deleteAll();

        // Create test user with full verification
        testUser = new UserAccount();
        testUser.setPhoneNumber("+1234567890");
        testUser.setPhoneVerified(true);
        testUser.setCorporateEmail("test@company.com");
        testUser.setEmailVerified(true);
        testUser.setAccountStatus(AccountStatus.ACTIVE);
        testUser = userAccountRepository.save(testUser);
        testUserId = testUser.getUserId();

        // Create wallet
        Wallet wallet = Wallet.builder()
                .userAccount(testUser)
                .balance(BigDecimal.valueOf(100.00))
                .autoReloadEnabled(false)
                .build();
        walletRepository.save(wallet);
    }

    @Test
    @WithMockUser(username = "testUserId", authorities = {"EMAIL_VERIFIED"})
    void testGetWalletStatus() throws Exception {
        mockMvc.perform(get("/users/" + testUserId + "/wallet"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.balance").value(100.00))
                .andExpect(jsonPath("$.autoReloadEnabled").value(false));
    }

    @Test
    @WithMockUser(username = "testUserId", authorities = {"EMAIL_VERIFIED"})
    void testAddPaymentMethod() throws Exception {
        WalletRequest walletRequest = new WalletRequest();
        walletRequest.setMethodType("CREDIT_CARD");
        walletRequest.setIdentifier("**** **** **** 1234");

        mockMvc.perform(post("/users/" + testUserId + "/wallet/payment-methods")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(walletRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.walletId").exists());
    }

    @Test
    @WithMockUser(username = "testUserId", authorities = {"EMAIL_VERIFIED"})
    void testEnableAutoReload() throws Exception {
        AutoReloadRequest autoReloadRequest = new AutoReloadRequest();
        autoReloadRequest.setEnabled(true);
        autoReloadRequest.setThreshold(BigDecimal.valueOf(50.00));
        autoReloadRequest.setAmount(BigDecimal.valueOf(100.00));

        mockMvc.perform(put("/users/" + testUserId + "/wallet/auto-reload")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(autoReloadRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.autoReloadEnabled").value(true));
    }

    @Test
    @WithMockUser(username = "testUserId", authorities = {"EMAIL_VERIFIED"})
    void testDisableAutoReload() throws Exception {
        AutoReloadRequest autoReloadRequest = new AutoReloadRequest();
        autoReloadRequest.setEnabled(false);

        mockMvc.perform(put("/users/" + testUserId + "/wallet/auto-reload")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(autoReloadRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.autoReloadEnabled").value(false));
    }

    @Test
    @WithMockUser(username = "testUserId", authorities = {"EMAIL_VERIFIED"})
    void testLinkBankAccount() throws Exception {
        BankAccountRequest bankRequest = new BankAccountRequest();
        bankRequest.setAccountNumber("1234567890");
        bankRequest.setBankName("Test Bank");
        bankRequest.setIfscCode("TEST0001234");

        mockMvc.perform(post("/users/" + testUserId + "/wallet/bank-account")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(bankRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.bankLinked").value(true));
    }

    @Test
    void testGetWalletWithoutAuthentication() throws Exception {
        mockMvc.perform(get("/users/" + testUserId + "/wallet"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(username = "testUserId", authorities = {"MOBILE_VERIFIED"})
    void testAddPaymentMethodWithoutEmailVerification() throws Exception {
        WalletRequest walletRequest = new WalletRequest();
        walletRequest.setMethodType("CREDIT_CARD");

        mockMvc.perform(post("/users/" + testUserId + "/wallet/payment-methods")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(walletRequest)))
                .andExpect(status().isForbidden());
    }
}
