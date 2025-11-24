package com.officemate.shared.dto;

import com.officemate.shared.validation.PhoneNumber;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request DTO for user registration.
 * Contains phone number for mobile registration.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RegisterRequest {
    
    /**
     * Phone number for registration (E.164 format recommended)
     */
    @NotBlank(message = "Phone number is required")
    @PhoneNumber(internationalOnly = true, message = "Phone number must be in international format (e.g., +1234567890)")
    private String phoneNumber;
}
