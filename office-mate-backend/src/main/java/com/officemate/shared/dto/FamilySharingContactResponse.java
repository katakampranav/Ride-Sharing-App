package com.officemate.shared.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Response DTO for family sharing contact information.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FamilySharingContactResponse {
    
    /**
     * Unique identifier for the family sharing contact
     */
    private UUID sharingId;
    
    /**
     * Family member's full name
     */
    private String name;
    
    /**
     * Family member's phone number
     */
    private String phoneNumber;
    
    /**
     * Family member's email address
     */
    private String email;
    
    /**
     * Flag indicating if this contact receives ride updates
     */
    private boolean receiveRideUpdates;
    
    /**
     * Timestamp when the contact was created
     */
    private LocalDateTime createdAt;
}
