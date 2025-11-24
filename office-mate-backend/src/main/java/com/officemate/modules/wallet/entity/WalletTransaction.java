package com.officemate.modules.wallet.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * JPA Entity representing a wallet transaction in the PostgreSQL database.
 * Tracks all wallet operations including deposits, withdrawals, and auto-reloads.
 */
@Entity
@Table(
    name = "wallet_transactions",
    indexes = {
        @Index(name = "idx_wallet_transactions_wallet", columnList = "wallet_id"),
        @Index(name = "idx_wallet_transactions_type", columnList = "transaction_type"),
        @Index(name = "idx_wallet_transactions_created", columnList = "created_at")
    }
)
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WalletTransaction {

    /**
     * Unique identifier for the transaction
     */
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "transaction_id", updatable = false, nullable = false)
    private UUID transactionId;

    /**
     * Many-to-one relationship with Wallet
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "wallet_id", nullable = false)
    @NotNull(message = "Wallet is required")
    private Wallet wallet;

    /**
     * Transaction type (DEPOSIT, WITHDRAWAL, AUTO_RELOAD, RIDE_PAYMENT, RIDE_EARNING, QR_PAYMENT)
     */
    @NotBlank(message = "Transaction type is required")
    @Column(name = "transaction_type", nullable = false, length = 20)
    private String transactionType;

    /**
     * Transaction amount
     */
    @DecimalMin(value = "0.00", inclusive = false, message = "Transaction amount must be greater than 0")
    @Column(name = "amount", nullable = false, precision = 10, scale = 2)
    private BigDecimal amount;

    /**
     * Balance after transaction
     */
    @Column(name = "balance_after", nullable = false, precision = 10, scale = 2)
    private BigDecimal balanceAfter;

    /**
     * Transaction status (PENDING, COMPLETED, FAILED, CANCELLED)
     */
    @NotBlank(message = "Transaction status is required")
    @Column(name = "status", nullable = false, length = 20)
    @Builder.Default
    private String status = "PENDING";

    /**
     * Payment method used for the transaction
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "payment_method_id")
    private PaymentMethod paymentMethod;

    /**
     * Description or notes about the transaction
     */
    @Column(name = "description", length = 500)
    private String description;

    /**
     * Reference ID from external payment gateway
     */
    @Column(name = "external_reference_id", length = 255)
    private String externalReferenceId;

    /**
     * Timestamp when the transaction was created
     */
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    /**
     * Marks transaction as completed
     */
    public void markAsCompleted() {
        this.status = "COMPLETED";
    }

    /**
     * Marks transaction as failed
     */
    public void markAsFailed() {
        this.status = "FAILED";
    }

    /**
     * Marks transaction as cancelled
     */
    public void markAsCancelled() {
        this.status = "CANCELLED";
    }

    /**
     * Checks if transaction is completed
     * 
     * @return true if status is COMPLETED
     */
    public boolean isCompleted() {
        return "COMPLETED".equals(status);
    }

    /**
     * Checks if transaction is pending
     * 
     * @return true if status is PENDING
     */
    public boolean isPending() {
        return "PENDING".equals(status);
    }

    /**
     * Checks if transaction is an auto-reload
     * 
     * @return true if transaction type is AUTO_RELOAD
     */
    public boolean isAutoReload() {
        return "AUTO_RELOAD".equals(transactionType);
    }

    /**
     * Checks if transaction is a deposit
     * 
     * @return true if transaction type is DEPOSIT
     */
    public boolean isDeposit() {
        return "DEPOSIT".equals(transactionType);
    }

    /**
     * Checks if transaction is a withdrawal
     * 
     * @return true if transaction type is WITHDRAWAL
     */
    public boolean isWithdrawal() {
        return "WITHDRAWAL".equals(transactionType);
    }
}
