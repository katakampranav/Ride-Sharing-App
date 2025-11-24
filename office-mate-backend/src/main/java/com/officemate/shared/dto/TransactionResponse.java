package com.officemate.shared.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * DTO for wallet transaction responses.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TransactionResponse {

    /**
     * Transaction ID
     */
    private String transactionId;

    /**
     * Wallet ID
     */
    private String walletId;

    /**
     * Transaction type (DEPOSIT, WITHDRAWAL, AUTO_RELOAD, etc.)
     */
    private String transactionType;

    /**
     * Transaction amount
     */
    private BigDecimal amount;

    /**
     * Balance after transaction
     */
    private BigDecimal balanceAfter;

    /**
     * Transaction status (PENDING, COMPLETED, FAILED, CANCELLED)
     */
    private String status;

    /**
     * Description or notes
     */
    private String description;

    /**
     * Payment method ID used
     */
    private String paymentMethodId;

    /**
     * External reference ID from payment gateway
     */
    private String externalReferenceId;

    /**
     * Transaction creation timestamp
     */
    private LocalDateTime createdAt;
}
