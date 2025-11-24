package com.officemate.modules.wallet.controller;

import com.officemate.modules.wallet.service.WalletService;
import com.officemate.shared.dto.AutoReloadRequest;
import com.officemate.shared.dto.BankAccountRequest;
import com.officemate.shared.dto.WalletRequest;
import com.officemate.shared.dto.WalletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/**
 * REST controller for wallet management operations.
 * Handles wallet status retrieval, payment method management, auto-reload configuration,
 * and bank account linking for driver earnings withdrawal.
 * All endpoints require both mobile and email verification.
 */
@RestController
@RequestMapping("/users/{userId}/wallet")
@RequiredArgsConstructor
@Slf4j
public class WalletController {

    private final WalletService walletService;

    /**
     * Get wallet status for a user.
     * Returns wallet balance, payment methods, and configuration.
     * 
     * GET /users/{userId}/wallet
     * 
     * @param userId The user's unique identifier
     * @return WalletResponse containing wallet information
     */
    @GetMapping
    @PreAuthorize("hasAuthority('EMAIL_VERIFIED') and (#userId == authentication.name or hasRole('ADMIN'))")
    public ResponseEntity<WalletResponse> getWalletStatus(@PathVariable String userId) {
        log.info("Get wallet status request for user: {}", userId);
        
        try {
            UUID userUuid = UUID.fromString(userId);
            WalletResponse response = walletService.getWalletStatus(userUuid);
            log.info("Successfully retrieved wallet status for user: {}", userId);
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            log.warn("Invalid user ID format: {}", userId);
            throw e;
        }
    }

    /**
     * Add a payment method to the user's wallet.
     * Supports credit cards, bank accounts, and UPI.
     * 
     * POST /users/{userId}/wallet/payment-methods
     * 
     * @param userId The user's unique identifier
     * @param request WalletRequest containing payment method details
     * @return WalletResponse with updated wallet information
     */
    @PostMapping("/payment-methods")
    @PreAuthorize("hasAuthority('EMAIL_VERIFIED') and (#userId == authentication.name or hasRole('ADMIN'))")
    public ResponseEntity<WalletResponse> addPaymentMethod(
            @PathVariable String userId,
            @Valid @RequestBody WalletRequest request) {
        
        log.info("Add payment method request for user: {}, type: {}", userId, request.getMethodType());
        
        try {
            UUID userUuid = UUID.fromString(userId);
            WalletResponse response = walletService.addPaymentMethod(userUuid, request);
            log.info("Successfully added payment method for user: {}", userId);
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (IllegalArgumentException | IllegalStateException e) {
            log.warn("Payment method addition failed for user {}: {}", userId, e.getMessage());
            throw e;
        }
    }

    /**
     * Configure auto-reload settings for the user's wallet.
     * Enables or disables auto-reload with specified threshold and amount.
     * 
     * PUT /users/{userId}/wallet/auto-reload
     * 
     * @param userId The user's unique identifier
     * @param request AutoReloadRequest containing auto-reload configuration
     * @return WalletResponse with updated wallet information
     */
    @PutMapping("/auto-reload")
    @PreAuthorize("hasAuthority('EMAIL_VERIFIED') and (#userId == authentication.name or hasRole('ADMIN'))")
    public ResponseEntity<WalletResponse> configureAutoReload(
            @PathVariable String userId,
            @Valid @RequestBody AutoReloadRequest request) {
        
        log.info("Configure auto-reload request for user: {}, enabled: {}", userId, request.isEnabled());
        
        try {
            UUID userUuid = UUID.fromString(userId);
            WalletResponse response;
            
            if (request.isEnabled()) {
                response = walletService.enableAutoReload(
                    userUuid,
                    request.getThreshold(),
                    request.getAmount()
                );
                log.info("Successfully enabled auto-reload for user: {}", userId);
            } else {
                response = walletService.disableAutoReload(userUuid);
                log.info("Successfully disabled auto-reload for user: {}", userId);
            }
            
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException | IllegalStateException e) {
            log.warn("Auto-reload configuration failed for user {}: {}", userId, e.getMessage());
            throw e;
        }
    }

    /**
     * Link a bank account to the user's wallet.
     * Used for driver earnings withdrawal and QR code payments.
     * 
     * POST /users/{userId}/wallet/bank-account
     * 
     * @param userId The user's unique identifier
     * @param request BankAccountRequest containing bank account details
     * @return WalletResponse with updated wallet information
     */
    @PostMapping("/bank-account")
    @PreAuthorize("hasAuthority('EMAIL_VERIFIED') and (#userId == authentication.name or hasRole('ADMIN'))")
    public ResponseEntity<WalletResponse> linkBankAccount(
            @PathVariable String userId,
            @Valid @RequestBody BankAccountRequest request) {
        
        log.info("Link bank account request for user: {}", userId);
        
        try {
            UUID userUuid = UUID.fromString(userId);
            WalletResponse response = walletService.linkBankAccount(
                userUuid,
                request.getAccountNumber(),
                request.getBankName(),
                request.getIfscCode()
            );
            log.info("Successfully linked bank account for user: {}", userId);
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (IllegalArgumentException | IllegalStateException e) {
            log.warn("Bank account linking failed for user {}: {}", userId, e.getMessage());
            throw e;
        }
    }
}
