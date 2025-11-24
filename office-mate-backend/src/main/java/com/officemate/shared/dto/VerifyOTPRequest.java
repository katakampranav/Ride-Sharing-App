package com.officemate.shared.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request DTO for OTP verification.
 * Contains phone number and OTP code.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VerifyOTPRequest {
    
    /**
     * Phone number being verified
     */
    @NotBlank(message = "Phone number is required")
    private String phoneNumber;
    
    /**
     * OTP code to verify
     */
    @NotBlank(message = "OTP is required")
    private String otp;
}
