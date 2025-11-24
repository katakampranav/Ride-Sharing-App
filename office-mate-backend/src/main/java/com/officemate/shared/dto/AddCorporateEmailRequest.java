package com.officemate.shared.dto;

import com.officemate.shared.validation.CorporateEmail;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request DTO for adding corporate email to user account.
 * Contains the corporate email address to be verified.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AddCorporateEmailRequest {
    
    /**
     * Corporate email address to add and verify
     */
    @NotBlank(message = "Corporate email is required")
    @CorporateEmail(message = "Please provide a valid corporate email address")
    private String corporateEmail;
}
