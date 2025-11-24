package com.officemate.shared.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request DTO for email OTP verification.
 * Contains the OTP code sent to the corporate email.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VerifyEmailOtpRequest {
    
    /**
     * OTP code sent to corporate email
     */
    @NotBlank(message = "OTP is required")
    private String otp;
}
