package com.officemate.shared.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Response DTO for emergency contact information.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EmergencyContactResponse {
    
    /**
     * Unique identifier for the emergency contact
     */
    private UUID contactId;
    
    /**
     * Contact's full name
     */
    private String name;
    
    /**
     * Contact's phone number
     */
    private String phoneNumber;
    
    /**
     * Relationship to the user
     */
    private String relationship;
    
    /**
     * Flag indicating if this is the primary emergency contact
     */
    private boolean isPrimary;
    
    /**
     * Timestamp when the contact was created
     */
    private LocalDateTime createdAt;
}
