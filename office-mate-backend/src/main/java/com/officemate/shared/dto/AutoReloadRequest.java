package com.officemate.shared.dto;

import jakarta.validation.constraints.DecimalMin;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * Request DTO for configuring wallet auto-reload settings.
 * Used to enable/disable auto-reload and set threshold and amount.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AutoReloadRequest {
    
    /**
     * Flag to enable or disable auto-reload
     */
    private boolean enabled;
    
    /**
     * Auto-reload threshold amount
     * When wallet balance falls below this, auto-reload is triggered
     */
    @DecimalMin(value = "0.0", inclusive = false, message = "Auto-reload threshold must be greater than 0")
    private BigDecimal threshold;
    
    /**
     * Auto-reload amount to add when threshold is reached
     */
    @DecimalMin(value = "0.0", inclusive = false, message = "Auto-reload amount must be greater than 0")
    private BigDecimal amount;
}
