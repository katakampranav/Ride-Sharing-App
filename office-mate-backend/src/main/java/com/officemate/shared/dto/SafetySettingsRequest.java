package com.officemate.shared.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import java.util.List;

/**
 * Request DTO for safety settings including emergency contacts
 * and family sharing configuration.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SafetySettingsRequest {
    
    /**
     * List of emergency contacts
     */
    @Valid
    private List<EmergencyContactDTO> emergencyContacts;
    
    /**
     * List of family members for ride sharing
     */
    @Valid
    private List<FamilySharingContactDTO> familySharingContacts;
    
    /**
     * DTO for emergency contact information
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class EmergencyContactDTO {
        
        /**
         * Contact's full name
         */
        @NotBlank(message = "Emergency contact name is required")
        private String name;
        
        /**
         * Contact's phone number
         */
        @NotBlank(message = "Emergency contact phone number is required")
        @Pattern(regexp = "^\\+?[1-9]\\d{1,14}$", message = "Invalid phone number format")
        private String phoneNumber;
        
        /**
         * Relationship to the user
         */
        private String relationship;
        
        /**
         * Flag to mark this as the primary emergency contact
         */
        private boolean isPrimary;
    }
    
    /**
     * DTO for family sharing contact information
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class FamilySharingContactDTO {
        
        /**
         * Family member's full name
         */
        @NotBlank(message = "Family contact name is required")
        private String name;
        
        /**
         * Family member's phone number
         */
        @Pattern(regexp = "^\\+?[1-9]\\d{1,14}$", message = "Invalid phone number format")
        private String phoneNumber;
        
        /**
         * Family member's email address
         */
        @Email(message = "Invalid email format")
        private String email;
        
        /**
         * Flag to enable ride update notifications for this contact
         */
        @lombok.Builder.Default
        private boolean receiveRideUpdates = true;
    }
}
