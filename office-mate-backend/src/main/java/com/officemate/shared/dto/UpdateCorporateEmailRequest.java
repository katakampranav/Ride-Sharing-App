package com.officemate.shared.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request DTO for updating corporate email (company change scenario).
 * Requires mobile OTP verification for security.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateCorporateEmailRequest {
    
    /**
     * Mobile OTP for verification (security requirement)
     */
    @NotBlank(message = "Mobile OTP is required")
    private String mobileOtp;
    
    /**
     * New corporate email address
     */
    @NotBlank(message = "New corporate email is required")
    @Email(message = "Invalid email format")
    private String newCorporateEmail;
    
    /**
     * Optional reason for email change
     */
    private String changeReason;
}
