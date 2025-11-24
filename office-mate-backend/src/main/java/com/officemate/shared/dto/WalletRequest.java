package com.officemate.shared.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import java.math.BigDecimal;
import java.util.Map;

/**
 * Request DTO for wallet operations including payment method management
 * and auto-reload configuration.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WalletRequest {
    
    /**
     * Type of wallet operation or payment method
     * Options: CREDIT_CARD, BANK_ACCOUNT, UPI
     */
    @NotBlank(message = "Payment method type is required")
    private String methodType;
    
    /**
     * Payment method identifier (masked card number, account number, UPI ID)
     */
    @NotBlank(message = "Payment method identifier is required")
    private String identifier;
    
    /**
     * Flag to set this payment method as primary
     */
    private boolean isPrimary;
    
    /**
     * Auto-reload threshold amount
     * When wallet balance falls below this, auto-reload is triggered
     */
    @DecimalMin(value = "0.0", inclusive = false, message = "Auto-reload threshold must be greater than 0")
    private BigDecimal autoReloadThreshold;
    
    /**
     * Auto-reload amount to add when threshold is reached
     */
    @DecimalMin(value = "0.0", inclusive = false, message = "Auto-reload amount must be greater than 0")
    private BigDecimal autoReloadAmount;
    
    /**
     * Flag to enable or disable auto-reload
     */
    private boolean autoReloadEnabled;
    
    /**
     * Additional metadata for the payment method
     * Can include card expiry, bank name, etc.
     */
    private Map<String, String> metadata;
}
