package com.officemate.shared.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * DTO for wallet operation requests (deposits, withdrawals).
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WalletOperationRequest {

    /**
     * Amount for the operation
     */
    @NotNull(message = "Amount is required")
    @DecimalMin(value = "0.01", message = "Amount must be greater than 0")
    private BigDecimal amount;

    /**
     * Payment method ID (optional, uses primary if not specified)
     */
    private UUID paymentMethodId;

    /**
     * Description or notes about the operation
     */
    private String description;
}
