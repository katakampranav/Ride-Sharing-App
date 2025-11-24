package com.officemate.modules.wallet.service;

import com.officemate.modules.wallet.entity.PaymentMethod;
import com.officemate.modules.wallet.entity.Wallet;
import com.officemate.modules.wallet.entity.WalletTransaction;
import com.officemate.modules.wallet.repository.WalletRepository;
import com.officemate.modules.wallet.repository.WalletTransactionRepository;
import com.officemate.shared.dto.TransactionResponse;
import com.officemate.shared.dto.WalletOperationRequest;
import com.officemate.shared.exception.WalletException;
import jakarta.persistence.EntityNotFoundException;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Service for wallet operations including balance management, transactions, and auto-reload.
 * Handles deposits, withdrawals, and transaction history.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class WalletOperationsService {

    private final WalletRepository walletRepository;
    private final WalletTransactionRepository transactionRepository;

    /**
     * Adds funds to a wallet (deposit operation).
     * 
     * @param userId User ID
     * @param request Wallet operation request
     * @return TransactionResponse containing transaction details
     * @throws EntityNotFoundException if wallet not found
     * @throws WalletException if operation fails
     */
    @Transactional
    public TransactionResponse addFunds(UUID userId, WalletOperationRequest request) {
        log.info("Adding funds to wallet for user: {}, amount: {}", userId, request.getAmount());

        Wallet wallet = walletRepository.findByUserAccountUserIdWithPaymentMethods(userId)
            .orElseThrow(() -> new EntityNotFoundException("Wallet not found for user: " + userId));

        // Validate amount
        if (request.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new WalletException("Deposit amount must be greater than 0", "INVALID_AMOUNT");
        }

        // Get payment method if specified
        PaymentMethod paymentMethod = null;
        if (request.getPaymentMethodId() != null) {
            paymentMethod = wallet.getPaymentMethods().stream()
                .filter(pm -> pm.getMethodId().equals(request.getPaymentMethodId()))
                .findFirst()
                .orElseThrow(() -> new WalletException("Payment method not found", "PAYMENT_METHOD_NOT_FOUND"));

            if (!paymentMethod.getIsVerified()) {
                throw new WalletException("Payment method not verified", "PAYMENT_METHOD_NOT_VERIFIED");
            }
        }

        // Create transaction record
        WalletTransaction transaction = WalletTransaction.builder()
            .wallet(wallet)
            .transactionType("DEPOSIT")
            .amount(request.getAmount())
            .balanceAfter(wallet.getBalance().add(request.getAmount()))
            .status("PENDING")
            .paymentMethod(paymentMethod)
            .description(request.getDescription() != null ? request.getDescription() : "Wallet deposit")
            .build();

        // Add funds to wallet
        wallet.addFunds(request.getAmount());

        // Mark transaction as completed
        transaction.markAsCompleted();

        // Save transaction
        WalletTransaction savedTransaction = transactionRepository.save(transaction);
        walletRepository.save(wallet);

        log.info("Successfully added funds to wallet for user: {}, transaction: {}", userId, savedTransaction.getTransactionId());

        return buildTransactionResponse(savedTransaction);
    }

    /**
     * Withdraws funds from a wallet (for driver earnings).
     * 
     * @param userId User ID
     * @param request Wallet operation request
     * @return TransactionResponse containing transaction details
     * @throws EntityNotFoundException if wallet not found
     * @throws WalletException if operation fails
     */
    @Transactional
    public TransactionResponse withdrawFunds(UUID userId, WalletOperationRequest request) {
        log.info("Withdrawing funds from wallet for user: {}, amount: {}", userId, request.getAmount());

        Wallet wallet = walletRepository.findByUserAccountUserIdWithPaymentMethods(userId)
            .orElseThrow(() -> new EntityNotFoundException("Wallet not found for user: " + userId));

        // Validate amount
        if (request.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new WalletException("Withdrawal amount must be greater than 0", "INVALID_AMOUNT");
        }

        // Check if wallet has sufficient balance
        if (wallet.getBalance().compareTo(request.getAmount()) < 0) {
            throw new WalletException("Insufficient balance", "INSUFFICIENT_BALANCE");
        }

        // Check if bank account is linked
        if (!wallet.getBankLinked()) {
            throw new WalletException("Bank account must be linked for withdrawals", "BANK_NOT_LINKED");
        }

        // Create transaction record
        WalletTransaction transaction = WalletTransaction.builder()
            .wallet(wallet)
            .transactionType("WITHDRAWAL")
            .amount(request.getAmount())
            .balanceAfter(wallet.getBalance().subtract(request.getAmount()))
            .status("PENDING")
            .description(request.getDescription() != null ? request.getDescription() : "Wallet withdrawal")
            .build();

        // Deduct funds from wallet
        boolean success = wallet.deductFunds(request.getAmount());
        if (!success) {
            throw new WalletException("Failed to deduct funds", "DEDUCTION_FAILED");
        }

        // Mark transaction as completed
        transaction.markAsCompleted();

        // Save transaction
        WalletTransaction savedTransaction = transactionRepository.save(transaction);
        walletRepository.save(wallet);

        log.info("Successfully withdrew funds from wallet for user: {}, transaction: {}", userId, savedTransaction.getTransactionId());

        return buildTransactionResponse(savedTransaction);
    }

    /**
     * Processes auto-reload for a wallet when balance falls below threshold.
     * 
     * @param userId User ID
     * @return TransactionResponse containing transaction details, or null if auto-reload not triggered
     * @throws EntityNotFoundException if wallet not found
     */
    @Transactional
    public TransactionResponse processAutoReload(UUID userId) {
        log.info("Processing auto-reload for user: {}", userId);

        Wallet wallet = walletRepository.findByUserAccountUserIdWithPaymentMethods(userId)
            .orElseThrow(() -> new EntityNotFoundException("Wallet not found for user: " + userId));

        // Check if auto-reload should be triggered
        if (!wallet.shouldTriggerAutoReload()) {
            log.debug("Auto-reload not triggered for user: {}", userId);
            return null;
        }

        // Get primary payment method
        PaymentMethod primaryPaymentMethod = wallet.getPrimaryPaymentMethod();
        if (primaryPaymentMethod == null || !primaryPaymentMethod.getIsVerified()) {
            log.warn("Auto-reload failed for user {} - no verified primary payment method", userId);
            throw new WalletException("No verified primary payment method for auto-reload", "NO_PRIMARY_PAYMENT_METHOD");
        }

        BigDecimal reloadAmount = wallet.getAutoReloadAmount();

        // Create auto-reload transaction
        WalletTransaction transaction = WalletTransaction.builder()
            .wallet(wallet)
            .transactionType("AUTO_RELOAD")
            .amount(reloadAmount)
            .balanceAfter(wallet.getBalance().add(reloadAmount))
            .status("PENDING")
            .paymentMethod(primaryPaymentMethod)
            .description("Auto-reload triggered at threshold: " + wallet.getAutoReloadThreshold())
            .build();

        // Add funds to wallet
        wallet.addFunds(reloadAmount);

        // Mark transaction as completed
        transaction.markAsCompleted();

        // Save transaction
        WalletTransaction savedTransaction = transactionRepository.save(transaction);
        walletRepository.save(wallet);

        log.info("Successfully processed auto-reload for user: {}, transaction: {}", userId, savedTransaction.getTransactionId());

        return buildTransactionResponse(savedTransaction);
    }

    /**
     * Gets transaction history for a wallet.
     * 
     * @param userId User ID
     * @param pageable Pagination information
     * @return Page of transaction responses
     * @throws EntityNotFoundException if wallet not found
     */
    @Transactional
    public Page<TransactionResponse> getTransactionHistory(UUID userId, Pageable pageable) {
        log.debug("Retrieving transaction history for user: {}", userId);

        Wallet wallet = walletRepository.findByUserAccountUserId(userId)
            .orElseThrow(() -> new EntityNotFoundException("Wallet not found for user: " + userId));

        Page<WalletTransaction> transactions = transactionRepository.findByWalletWalletId(wallet.getWalletId(), pageable);

        return transactions.map(this::buildTransactionResponse);
    }

    /**
     * Gets transaction history by type for a wallet.
     * 
     * @param userId User ID
     * @param transactionType Transaction type
     * @param pageable Pagination information
     * @return Page of transaction responses
     * @throws EntityNotFoundException if wallet not found
     */
    @Transactional
    public Page<TransactionResponse> getTransactionHistoryByType(UUID userId, String transactionType, Pageable pageable) {
        log.debug("Retrieving transaction history by type for user: {}, type: {}", userId, transactionType);

        Wallet wallet = walletRepository.findByUserAccountUserId(userId)
            .orElseThrow(() -> new EntityNotFoundException("Wallet not found for user: " + userId));

        Page<WalletTransaction> transactions = transactionRepository.findByWalletWalletIdAndTransactionType(
            wallet.getWalletId(), transactionType, pageable
        );

        return transactions.map(this::buildTransactionResponse);
    }

    /**
     * Gets recent transactions for a wallet.
     * 
     * @param userId User ID
     * @param limit Maximum number of transactions
     * @return List of transaction responses
     * @throws EntityNotFoundException if wallet not found
     */
    @Transactional
    public List<TransactionResponse> getRecentTransactions(UUID userId, int limit) {
        log.debug("Retrieving recent transactions for user: {}, limit: {}", userId, limit);

        Wallet wallet = walletRepository.findByUserAccountUserId(userId)
            .orElseThrow(() -> new EntityNotFoundException("Wallet not found for user: " + userId));

        List<WalletTransaction> transactions = transactionRepository.findRecentTransactions(wallet.getWalletId(), limit);

        return transactions.stream()
            .map(this::buildTransactionResponse)
            .collect(Collectors.toList());
    }

    /**
     * Gets transaction history within a date range.
     * 
     * @param userId User ID
     * @param startDate Start date
     * @param endDate End date
     * @param pageable Pagination information
     * @return Page of transaction responses
     * @throws EntityNotFoundException if wallet not found
     */
    @Transactional
    public Page<TransactionResponse> getTransactionHistoryByDateRange(
        UUID userId, 
        LocalDateTime startDate, 
        LocalDateTime endDate, 
        Pageable pageable
    ) {
        log.debug("Retrieving transaction history by date range for user: {}", userId);

        Wallet wallet = walletRepository.findByUserAccountUserId(userId)
            .orElseThrow(() -> new EntityNotFoundException("Wallet not found for user: " + userId));

        Page<WalletTransaction> transactions = transactionRepository.findByWalletWalletIdAndCreatedAtBetween(
            wallet.getWalletId(), startDate, endDate, pageable
        );

        return transactions.map(this::buildTransactionResponse);
    }

    /**
     * Builds a TransactionResponse DTO from WalletTransaction entity.
     * 
     * @param transaction The transaction entity
     * @return TransactionResponse DTO
     */
    private TransactionResponse buildTransactionResponse(WalletTransaction transaction) {
        return TransactionResponse.builder()
            .transactionId(transaction.getTransactionId().toString())
            .walletId(transaction.getWallet().getWalletId().toString())
            .transactionType(transaction.getTransactionType())
            .amount(transaction.getAmount())
            .balanceAfter(transaction.getBalanceAfter())
            .status(transaction.getStatus())
            .description(transaction.getDescription())
            .paymentMethodId(transaction.getPaymentMethod() != null ? 
                transaction.getPaymentMethod().getMethodId().toString() : null)
            .externalReferenceId(transaction.getExternalReferenceId())
            .createdAt(transaction.getCreatedAt())
            .build();
    }
}
