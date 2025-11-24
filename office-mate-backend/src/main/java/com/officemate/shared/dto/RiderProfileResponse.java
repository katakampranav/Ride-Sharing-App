package com.officemate.shared.dto;

import com.officemate.shared.enums.GenderPreference;
import com.officemate.shared.enums.VehicleType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Response DTO for rider profile information.
 * Contains rider-specific preferences and route information.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RiderProfileResponse {
    
    /**
     * Unique rider identifier (same as user ID)
     */
    private String riderId;
    
    /**
     * Gender preference for ride matching
     */
    private GenderPreference genderPreference;
    
    /**
     * Preferred vehicle types for rides
     */
    private List<VehicleType> vehicleTypePreferences;
    
    /**
     * List of favorite driver user IDs
     */
    private List<String> favoriteDrivers;
    
    /**
     * Route preferences (if available)
     */
    private RoutePreferencesDTO routePreferences;
    
    /**
     * Flag indicating if rider has gender preference set
     */
    private boolean hasGenderPreference;
    
    /**
     * Profile creation timestamp
     */
    private LocalDateTime createdAt;
    
    /**
     * Last profile update timestamp
     */
    private LocalDateTime updatedAt;
}
