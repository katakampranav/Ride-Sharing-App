package com.officemate.modules.wallet.service;

import com.officemate.modules.auth.entity.UserAccount;
import com.officemate.modules.auth.repository.UserAccountRepository;
import com.officemate.modules.wallet.entity.PaymentMethod;
import com.officemate.modules.wallet.entity.Wallet;
import com.officemate.modules.wallet.repository.PaymentMethodRepository;
import com.officemate.modules.wallet.repository.WalletRepository;
import com.officemate.shared.dto.WalletRequest;
import com.officemate.shared.dto.WalletResponse;
import com.officemate.shared.exception.ProfileAccessException;
import com.officemate.shared.exception.WalletException;
import jakarta.persistence.EntityNotFoundException;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Service for managing user wallets and payment methods.
 * Handles wallet creation, payment method management, and auto-reload configuration.
 * Requires both mobile and email verification before wallet operations.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class WalletService {

    private final WalletRepository walletRepository;
    private final PaymentMethodRepository paymentMethodRepository;
    private final UserAccountRepository userAccountRepository;

    /**
     * Initializes a wallet for a user.
     * Requires both mobile and email verification.
     * 
     * @param userId The user's unique identifier
     * @return WalletResponse containing the initialized wallet information
     * @throws ProfileAccessException if user is not fully verified
     * @throws WalletException if wallet already exists
     */
    @Transactional
    public WalletResponse initializeWallet(UUID userId) {
        log.info("Initializing wallet for user: {}", userId);

        // Fetch user account and verify permissions
        UserAccount userAccount = userAccountRepository.findById(userId)
            .orElseThrow(() -> new EntityNotFoundException("User account not found: " + userId));

        // Check if both mobile and email are verified
        if (!userAccount.isFullyVerified()) {
            log.warn("Wallet initialization denied for user {} - verification incomplete. Mobile: {}, Email: {}", 
                userId, userAccount.getPhoneVerified(), userAccount.getEmailVerified());
            throw new ProfileAccessException(
                "Both mobile and email verification required before wallet initialization",
                userAccount.getPhoneVerified(),
                userAccount.getEmailVerified()
            );
        }

        // Check if wallet already exists
        if (walletRepository.existsByUserAccountUserId(userId)) {
            log.warn("Wallet already exists for user: {}", userId);
            throw new WalletException("Wallet already exists for user: " + userId, "WALLET_ALREADY_EXISTS");
        }

        // Create new wallet
        Wallet wallet = Wallet.builder()
            .userAccount(userAccount)
            .balance(BigDecimal.ZERO)
            .autoReloadEnabled(false)
            .bankLinked(false)
            .build();

        Wallet savedWallet = walletRepository.save(wallet);
        log.info("Successfully initialized wallet for user: {}", userId);

        return buildWalletResponse(savedWallet);
    }

    /**
     * Adds a payment method to a user's wallet.
     * Validates and stores the payment method with optional verification.
     * 
     * @param userId The user's unique identifier
     * @param request WalletRequest containing payment method details
     * @return WalletResponse containing updated wallet information
     * @throws EntityNotFoundException if wallet not found
     * @throws WalletException if payment method validation fails
     */
    @Transactional
    public WalletResponse addPaymentMethod(UUID userId, WalletRequest request) {
        log.info("Adding payment method for user: {}, type: {}", userId, request.getMethodType());

        // Fetch wallet
        Wallet wallet = walletRepository.findByUserAccountUserIdWithPaymentMethods(userId)
            .orElseThrow(() -> new EntityNotFoundException("Wallet not found for user: " + userId));

        // Validate payment method type
        validatePaymentMethodType(request.getMethodType());

        // Validate identifier format based on type
        validatePaymentMethodIdentifier(request.getMethodType(), request.getIdentifier());

        // Check if payment method already exists
        if (paymentMethodRepository.existsByWalletWalletIdAndIdentifier(wallet.getWalletId(), request.getIdentifier())) {
            log.warn("Payment method already exists for wallet: {}", wallet.getWalletId());
            throw new WalletException("Payment method already exists", "PAYMENT_METHOD_DUPLICATE");
        }

        // Create payment method
        PaymentMethod paymentMethod = PaymentMethod.builder()
            .wallet(wallet)
            .methodType(request.getMethodType())
            .identifier(request.getIdentifier())
            .isPrimary(request.isPrimary())
            .isVerified(false) // Initially unverified, requires verification workflow
            .metadata(request.getMetadata() != null ? request.getMetadata() : Map.of())
            .build();

        // If this is set as primary, unmark all other payment methods
        if (request.isPrimary()) {
            paymentMethodRepository.unmarkAllAsPrimaryForWallet(wallet.getWalletId());
        }

        wallet.addPaymentMethod(paymentMethod);
        Wallet savedWallet = walletRepository.save(wallet);

        log.info("Successfully added payment method for user: {}", userId);

        return buildWalletResponse(savedWallet);
    }

    /**
     * Enables auto-reload functionality for a user's wallet.
     * 
     * @param userId The user's unique identifier
     * @param threshold Threshold amount to trigger auto-reload
     * @param amount Amount to reload when threshold is reached
     * @return WalletResponse containing updated wallet information
     * @throws EntityNotFoundException if wallet not found
     * @throws WalletException if wallet has no verified payment method
     */
    @Transactional
    public WalletResponse enableAutoReload(UUID userId, BigDecimal threshold, BigDecimal amount) {
        log.info("Enabling auto-reload for user: {}, threshold: {}, amount: {}", userId, threshold, amount);

        // Fetch wallet
        Wallet wallet = walletRepository.findByUserAccountUserIdWithPaymentMethods(userId)
            .orElseThrow(() -> new EntityNotFoundException("Wallet not found for user: " + userId));

        // Validate that wallet has at least one verified payment method
        if (!wallet.hasVerifiedPaymentMethod()) {
            log.warn("Auto-reload denied for user {} - no verified payment method", userId);
            throw new WalletException("At least one verified payment method required for auto-reload", "NO_VERIFIED_PAYMENT_METHOD");
        }

        // Validate threshold and amount
        if (threshold.compareTo(BigDecimal.ZERO) <= 0) {
            throw new WalletException("Auto-reload threshold must be greater than 0", "INVALID_THRESHOLD");
        }
        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new WalletException("Auto-reload amount must be greater than 0", "INVALID_AMOUNT");
        }

        wallet.enableAutoReload(threshold, amount);
        Wallet savedWallet = walletRepository.save(wallet);

        log.info("Successfully enabled auto-reload for user: {}", userId);

        return buildWalletResponse(savedWallet);
    }

    /**
     * Disables auto-reload functionality for a user's wallet.
     * 
     * @param userId The user's unique identifier
     * @return WalletResponse containing updated wallet information
     * @throws EntityNotFoundException if wallet not found
     */
    @Transactional
    public WalletResponse disableAutoReload(UUID userId) {
        log.info("Disabling auto-reload for user: {}", userId);

        Wallet wallet = walletRepository.findByUserAccountUserId(userId)
            .orElseThrow(() -> new EntityNotFoundException("Wallet not found for user: " + userId));

        wallet.disableAutoReload();
        Wallet savedWallet = walletRepository.save(wallet);

        log.info("Successfully disabled auto-reload for user: {}", userId);

        return buildWalletResponse(savedWallet);
    }

    /**
     * Links a bank account to a user's wallet (for driver earnings withdrawal).
     * 
     * @param userId The user's unique identifier
     * @param accountNumber Bank account number
     * @param bankName Bank name
     * @param ifscCode IFSC code (for Indian banks)
     * @return WalletResponse containing updated wallet information
     * @throws EntityNotFoundException if wallet not found
     */
    @Transactional
    public WalletResponse linkBankAccount(UUID userId, String accountNumber, String bankName, String ifscCode) {
        log.info("Linking bank account for user: {}", userId);

        Wallet wallet = walletRepository.findByUserAccountUserId(userId)
            .orElseThrow(() -> new EntityNotFoundException("Wallet not found for user: " + userId));

        // Create a bank account payment method
        PaymentMethod bankPaymentMethod = PaymentMethod.builder()
            .wallet(wallet)
            .methodType("BANK_ACCOUNT")
            .identifier(accountNumber)
            .isPrimary(false)
            .isVerified(false) // Requires verification
            .metadata(Map.of(
                "bankName", bankName,
                "ifscCode", ifscCode != null ? ifscCode : ""
            ))
            .build();

        wallet.addPaymentMethod(bankPaymentMethod);
        wallet.linkBankAccount();
        Wallet savedWallet = walletRepository.save(wallet);

        log.info("Successfully linked bank account for user: {}", userId);

        return buildWalletResponse(savedWallet);
    }

    /**
     * Retrieves wallet status for a user.
     * 
     * @param userId The user's unique identifier
     * @return WalletResponse containing wallet information
     * @throws EntityNotFoundException if wallet not found
     */
    @Transactional
    public WalletResponse getWalletStatus(UUID userId) {
        log.debug("Retrieving wallet status for user: {}", userId);

        Wallet wallet = walletRepository.findByUserAccountUserIdWithPaymentMethods(userId)
            .orElseThrow(() -> new EntityNotFoundException("Wallet not found for user: " + userId));

        return buildWalletResponse(wallet);
    }

    /**
     * Verifies a payment method.
     * This would typically be called after external payment gateway verification.
     * 
     * @param userId The user's unique identifier
     * @param methodId Payment method ID
     * @return WalletResponse containing updated wallet information
     * @throws EntityNotFoundException if wallet or payment method not found
     */
    @Transactional
    public WalletResponse verifyPaymentMethod(UUID userId, UUID methodId) {
        log.info("Verifying payment method {} for user: {}", methodId, userId);

        Wallet wallet = walletRepository.findByUserAccountUserIdWithPaymentMethods(userId)
            .orElseThrow(() -> new EntityNotFoundException("Wallet not found for user: " + userId));

        PaymentMethod paymentMethod = wallet.getPaymentMethods().stream()
            .filter(pm -> pm.getMethodId().equals(methodId))
            .findFirst()
            .orElseThrow(() -> new EntityNotFoundException("Payment method not found: " + methodId));

        paymentMethod.verify();
        Wallet savedWallet = walletRepository.save(wallet);

        log.info("Successfully verified payment method {} for user: {}", methodId, userId);

        return buildWalletResponse(savedWallet);
    }

    /**
     * Sets a payment method as primary.
     * 
     * @param userId The user's unique identifier
     * @param methodId Payment method ID
     * @return WalletResponse containing updated wallet information
     * @throws EntityNotFoundException if wallet or payment method not found
     */
    @Transactional
    public WalletResponse setPrimaryPaymentMethod(UUID userId, UUID methodId) {
        log.info("Setting primary payment method {} for user: {}", methodId, userId);

        Wallet wallet = walletRepository.findByUserAccountUserIdWithPaymentMethods(userId)
            .orElseThrow(() -> new EntityNotFoundException("Wallet not found for user: " + userId));

        // Unmark all as primary
        paymentMethodRepository.unmarkAllAsPrimaryForWallet(wallet.getWalletId());

        // Mark the specified method as primary
        PaymentMethod paymentMethod = wallet.getPaymentMethods().stream()
            .filter(pm -> pm.getMethodId().equals(methodId))
            .findFirst()
            .orElseThrow(() -> new EntityNotFoundException("Payment method not found: " + methodId));

        paymentMethod.markAsPrimary();
        Wallet savedWallet = walletRepository.save(wallet);

        log.info("Successfully set primary payment method {} for user: {}", methodId, userId);

        return buildWalletResponse(savedWallet);
    }

    /**
     * Validates payment method type.
     * 
     * @param methodType Payment method type
     * @throws WalletException if type is invalid
     */
    private void validatePaymentMethodType(String methodType) {
        if (methodType == null || methodType.isBlank()) {
            throw new WalletException("Payment method type is required", "INVALID_METHOD_TYPE");
        }

        List<String> validTypes = List.of("CREDIT_CARD", "BANK_ACCOUNT", "UPI");
        if (!validTypes.contains(methodType)) {
            throw new WalletException("Invalid payment method type: " + methodType + ". Must be one of: " + validTypes, "INVALID_METHOD_TYPE");
        }
    }

    /**
     * Validates payment method identifier format based on type.
     * 
     * @param methodType Payment method type
     * @param identifier Payment method identifier
     * @throws WalletException if identifier format is invalid
     */
    private void validatePaymentMethodIdentifier(String methodType, String identifier) {
        if (identifier == null || identifier.isBlank()) {
            throw new WalletException("Payment method identifier is required", "INVALID_IDENTIFIER");
        }

        // Basic validation - in production, use more sophisticated validation
        switch (methodType) {
            case "CREDIT_CARD":
                // Credit card should be 13-19 digits
                if (!identifier.matches("\\d{13,19}")) {
                    throw new WalletException("Invalid credit card number format", "INVALID_IDENTIFIER");
                }
                break;
            case "BANK_ACCOUNT":
                // Bank account should be alphanumeric
                if (!identifier.matches("[A-Za-z0-9]{5,20}")) {
                    throw new WalletException("Invalid bank account number format", "INVALID_IDENTIFIER");
                }
                break;
            case "UPI":
                // UPI should be in format: user@provider
                if (!identifier.matches("[a-zA-Z0-9.\\-_]+@[a-zA-Z0-9]+")) {
                    throw new WalletException("Invalid UPI ID format", "INVALID_IDENTIFIER");
                }
                break;
        }
    }

    /**
     * Builds a WalletResponse DTO from Wallet entity.
     * 
     * @param wallet The wallet entity
     * @return WalletResponse DTO
     */
    private WalletResponse buildWalletResponse(Wallet wallet) {
        List<WalletResponse.PaymentMethodDTO> paymentMethodDTOs = wallet.getPaymentMethods().stream()
            .map(pm -> WalletResponse.PaymentMethodDTO.builder()
                .methodId(pm.getMethodId().toString())
                .methodType(pm.getMethodType())
                .maskedIdentifier(pm.getMaskedIdentifier())
                .isPrimary(pm.getIsPrimary())
                .isVerified(pm.getIsVerified())
                .createdAt(pm.getCreatedAt())
                .build())
            .collect(Collectors.toList());

        return WalletResponse.builder()
            .walletId(wallet.getWalletId().toString())
            .userId(wallet.getUserAccount().getUserId().toString())
            .balance(wallet.getBalance())
            .paymentMethods(paymentMethodDTOs)
            .autoReloadEnabled(wallet.getAutoReloadEnabled())
            .autoReloadThreshold(wallet.getAutoReloadThreshold())
            .autoReloadAmount(wallet.getAutoReloadAmount())
            .bankLinked(wallet.getBankLinked())
            .createdAt(wallet.getCreatedAt())
            .updatedAt(wallet.getUpdatedAt())
            .build();
    }
}
