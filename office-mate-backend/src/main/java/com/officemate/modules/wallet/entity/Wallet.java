package com.officemate.modules.wallet.entity;

import com.officemate.modules.auth.entity.UserAccount;
import jakarta.persistence.*;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * JPA Entity representing a user's wallet in the PostgreSQL database.
 * Manages payment methods, balance, and auto-reload settings.
 * Requires both mobile and email verification before creation.
 */
@Entity
@Table(name = "wallets")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Wallet {

    /**
     * Unique identifier for the wallet
     */
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "wallet_id", updatable = false, nullable = false)
    private UUID walletId;

    /**
     * One-to-one relationship with UserAccount
     */
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    @NotNull(message = "User account is required")
    private UserAccount userAccount;

    /**
     * Current wallet balance
     */
    @DecimalMin(value = "0.00", message = "Balance cannot be negative")
    @Column(name = "balance", nullable = false, precision = 10, scale = 2)
    @Builder.Default
    private BigDecimal balance = BigDecimal.ZERO;

    /**
     * Flag indicating if auto-reload is enabled
     */
    @Column(name = "auto_reload_enabled", nullable = false)
    @Builder.Default
    private Boolean autoReloadEnabled = false;

    /**
     * Auto-reload threshold amount
     * When balance falls below this, auto-reload is triggered
     */
    @DecimalMin(value = "0.00", inclusive = false, message = "Auto-reload threshold must be greater than 0")
    @Column(name = "auto_reload_threshold", precision = 10, scale = 2)
    private BigDecimal autoReloadThreshold;

    /**
     * Auto-reload amount to add when threshold is reached
     */
    @DecimalMin(value = "0.00", inclusive = false, message = "Auto-reload amount must be greater than 0")
    @Column(name = "auto_reload_amount", precision = 10, scale = 2)
    private BigDecimal autoReloadAmount;

    /**
     * Flag indicating if bank account is linked (for driver earnings withdrawal)
     */
    @Column(name = "bank_linked", nullable = false)
    @Builder.Default
    private Boolean bankLinked = false;

    /**
     * One-to-many relationship with PaymentMethod
     */
    @OneToMany(mappedBy = "wallet", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @Builder.Default
    private List<PaymentMethod> paymentMethods = new ArrayList<>();

    /**
     * Timestamp when the wallet was created
     */
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    /**
     * Timestamp when the wallet was last updated
     */
    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    /**
     * Adds a payment method to the wallet
     * 
     * @param paymentMethod Payment method to add
     */
    public void addPaymentMethod(PaymentMethod paymentMethod) {
        paymentMethods.add(paymentMethod);
        paymentMethod.setWallet(this);
    }

    /**
     * Removes a payment method from the wallet
     * 
     * @param paymentMethod Payment method to remove
     */
    public void removePaymentMethod(PaymentMethod paymentMethod) {
        paymentMethods.remove(paymentMethod);
    }

    /**
     * Enables auto-reload with specified threshold and amount
     * 
     * @param threshold Threshold amount
     * @param amount Reload amount
     */
    public void enableAutoReload(BigDecimal threshold, BigDecimal amount) {
        this.autoReloadThreshold = threshold;
        this.autoReloadAmount = amount;
        this.autoReloadEnabled = true;
    }

    /**
     * Disables auto-reload
     */
    public void disableAutoReload() {
        this.autoReloadEnabled = false;
    }

    /**
     * Links a bank account to the wallet
     */
    public void linkBankAccount() {
        this.bankLinked = true;
    }

    /**
     * Unlinks the bank account from the wallet
     */
    public void unlinkBankAccount() {
        this.bankLinked = false;
    }

    /**
     * Adds funds to the wallet balance
     * 
     * @param amount Amount to add
     */
    public void addFunds(BigDecimal amount) {
        if (amount.compareTo(BigDecimal.ZERO) > 0) {
            this.balance = this.balance.add(amount);
        }
    }

    /**
     * Deducts funds from the wallet balance
     * 
     * @param amount Amount to deduct
     * @return true if deduction was successful, false if insufficient balance
     */
    public boolean deductFunds(BigDecimal amount) {
        if (amount.compareTo(BigDecimal.ZERO) > 0 && this.balance.compareTo(amount) >= 0) {
            this.balance = this.balance.subtract(amount);
            return true;
        }
        return false;
    }

    /**
     * Checks if auto-reload should be triggered
     * 
     * @return true if balance is below threshold and auto-reload is enabled
     */
    public boolean shouldTriggerAutoReload() {
        return autoReloadEnabled 
            && autoReloadThreshold != null 
            && balance.compareTo(autoReloadThreshold) < 0;
    }

    /**
     * Gets the primary payment method
     * 
     * @return Primary payment method or null if none exists
     */
    public PaymentMethod getPrimaryPaymentMethod() {
        return paymentMethods.stream()
            .filter(pm -> Boolean.TRUE.equals(pm.getIsPrimary()))
            .findFirst()
            .orElse(null);
    }

    /**
     * Checks if wallet has at least one verified payment method
     * 
     * @return true if at least one payment method is verified
     */
    public boolean hasVerifiedPaymentMethod() {
        return paymentMethods.stream()
            .anyMatch(pm -> Boolean.TRUE.equals(pm.getIsVerified()));
    }
}
