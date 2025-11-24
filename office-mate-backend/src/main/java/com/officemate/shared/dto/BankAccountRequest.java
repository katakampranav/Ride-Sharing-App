package com.officemate.shared.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request DTO for linking a bank account to a wallet.
 * Used for driver earnings withdrawal and QR code payments.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BankAccountRequest {
    
    /**
     * Bank account number
     */
    @NotBlank(message = "Account number is required")
    @Pattern(regexp = "[A-Za-z0-9]{5,20}", message = "Invalid account number format")
    private String accountNumber;
    
    /**
     * Bank name
     */
    @NotBlank(message = "Bank name is required")
    private String bankName;
    
    /**
     * IFSC code (for Indian banks)
     * Optional for international banks
     */
    @Pattern(regexp = "^[A-Z]{4}0[A-Z0-9]{6}$|^$", message = "Invalid IFSC code format")
    private String ifscCode;
}
