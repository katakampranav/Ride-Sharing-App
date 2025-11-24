package com.officemate.shared.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Response DTO for safety settings including emergency contacts
 * and family sharing configuration.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SafetySettingsResponse {
    
    /**
     * List of emergency contacts
     */
    private List<EmergencyContactResponse> emergencyContacts;
    
    /**
     * List of family sharing contacts
     */
    private List<FamilySharingContactResponse> familySharingContacts;
    
    /**
     * Flag indicating if user has any active SOS alerts
     */
    private boolean hasActiveSOSAlert;
    
    /**
     * Flag indicating if location sharing is currently active
     */
    private boolean locationSharingActive;
}
