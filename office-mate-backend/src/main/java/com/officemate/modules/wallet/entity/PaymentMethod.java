package com.officemate.modules.wallet.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * JPA Entity representing a payment method in the PostgreSQL database.
 * Supports multiple payment types: credit cards, bank accounts, and UPI.
 */
@Entity
@Table(
    name = "payment_methods",
    indexes = {
        @Index(name = "idx_payment_methods_wallet", columnList = "wallet_id"),
        @Index(name = "idx_payment_methods_primary", columnList = "wallet_id, is_primary")
    }
)
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PaymentMethod {

    /**
     * Unique identifier for the payment method
     */
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "method_id", updatable = false, nullable = false)
    private UUID methodId;

    /**
     * Many-to-one relationship with Wallet
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "wallet_id", nullable = false)
    @NotNull(message = "Wallet is required")
    private Wallet wallet;

    /**
     * Payment method type (CREDIT_CARD, BANK_ACCOUNT, UPI)
     */
    @NotBlank(message = "Payment method type is required")
    @Column(name = "method_type", nullable = false, length = 20)
    private String methodType;

    /**
     * Payment method identifier (masked card number, account number, UPI ID)
     */
    @NotBlank(message = "Payment method identifier is required")
    @Column(name = "identifier", nullable = false, length = 255)
    private String identifier;

    /**
     * Flag indicating if this is the primary payment method
     */
    @Column(name = "is_primary", nullable = false)
    @Builder.Default
    private Boolean isPrimary = false;

    /**
     * Flag indicating if this payment method is verified
     */
    @Column(name = "is_verified", nullable = false)
    @Builder.Default
    private Boolean isVerified = false;

    /**
     * Additional metadata for the payment method (JSON)
     * Can include card expiry, bank name, UPI provider, etc.
     */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "metadata", columnDefinition = "jsonb")
    @Builder.Default
    private Map<String, String> metadata = new HashMap<>();

    /**
     * Timestamp when the payment method was created
     */
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    /**
     * Marks this payment method as primary
     * Note: Caller should ensure only one primary method per wallet
     */
    public void markAsPrimary() {
        this.isPrimary = true;
    }

    /**
     * Removes primary status from this payment method
     */
    public void unmarkAsPrimary() {
        this.isPrimary = false;
    }

    /**
     * Marks this payment method as verified
     */
    public void verify() {
        this.isVerified = true;
    }

    /**
     * Marks this payment method as unverified
     */
    public void unverify() {
        this.isVerified = false;
    }

    /**
     * Adds or updates metadata
     * 
     * @param key Metadata key
     * @param value Metadata value
     */
    public void addMetadata(String key, String value) {
        if (this.metadata == null) {
            this.metadata = new HashMap<>();
        }
        this.metadata.put(key, value);
    }

    /**
     * Gets metadata value by key
     * 
     * @param key Metadata key
     * @return Metadata value or null if not found
     */
    public String getMetadata(String key) {
        return this.metadata != null ? this.metadata.get(key) : null;
    }

    /**
     * Checks if this is a credit card payment method
     * 
     * @return true if method type is CREDIT_CARD
     */
    public boolean isCreditCard() {
        return "CREDIT_CARD".equals(methodType);
    }

    /**
     * Checks if this is a bank account payment method
     * 
     * @return true if method type is BANK_ACCOUNT
     */
    public boolean isBankAccount() {
        return "BANK_ACCOUNT".equals(methodType);
    }

    /**
     * Checks if this is a UPI payment method
     * 
     * @return true if method type is UPI
     */
    public boolean isUPI() {
        return "UPI".equals(methodType);
    }

    /**
     * Gets a masked version of the identifier for display
     * 
     * @return Masked identifier
     */
    public String getMaskedIdentifier() {
        if (identifier == null || identifier.length() <= 4) {
            return identifier;
        }
        
        if (isCreditCard()) {
            // For credit cards: **** **** **** 1234
            String lastFour = identifier.substring(identifier.length() - 4);
            return "**** **** **** " + lastFour;
        } else if (isBankAccount()) {
            // For bank accounts: ******1234
            String lastFour = identifier.substring(identifier.length() - 4);
            return "******" + lastFour;
        } else {
            // For UPI and others: show as is (already masked or safe to show)
            return identifier;
        }
    }
}
