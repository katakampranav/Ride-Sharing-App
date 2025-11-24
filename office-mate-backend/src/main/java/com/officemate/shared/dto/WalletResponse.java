package com.officemate.shared.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Response DTO for wallet information.
 * Contains wallet balance, payment methods, and configuration.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WalletResponse {
    
    /**
     * Unique wallet identifier
     */
    private String walletId;
    
    /**
     * User ID associated with this wallet
     */
    private String userId;
    
    /**
     * Current wallet balance
     */
    private BigDecimal balance;
    
    /**
     * List of payment methods
     */
    private List<PaymentMethodDTO> paymentMethods;
    
    /**
     * Flag indicating if auto-reload is enabled
     */
    private boolean autoReloadEnabled;
    
    /**
     * Auto-reload threshold amount
     */
    private BigDecimal autoReloadThreshold;
    
    /**
     * Auto-reload amount
     */
    private BigDecimal autoReloadAmount;
    
    /**
     * Flag indicating if bank account is linked
     */
    private boolean bankLinked;
    
    /**
     * Wallet creation timestamp
     */
    private LocalDateTime createdAt;
    
    /**
     * Last wallet update timestamp
     */
    private LocalDateTime updatedAt;
    
    /**
     * DTO for payment method information
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PaymentMethodDTO {
        
        /**
         * Payment method identifier
         */
        private String methodId;
        
        /**
         * Payment method type
         */
        private String methodType;
        
        /**
         * Masked identifier (e.g., **** **** **** 1234)
         */
        private String maskedIdentifier;
        
        /**
         * Flag indicating if this is the primary payment method
         */
        private boolean isPrimary;
        
        /**
         * Flag indicating if this payment method is verified
         */
        private boolean isVerified;
        
        /**
         * Payment method creation timestamp
         */
        private LocalDateTime createdAt;
    }
}
