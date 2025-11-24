package com.officemate.shared.dto;

import com.officemate.shared.enums.GenderPreference;
import com.officemate.shared.enums.VehicleType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import java.util.List;

/**
 * Request DTO for creating or updating a rider profile.
 * Contains rider-specific preferences including gender and vehicle type preferences.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RiderProfileRequest {
    
    /**
     * Route preferences for the rider
     */
    @NotNull(message = "Route preferences are required")
    @Valid
    private RoutePreferencesDTO routePreferences;
    
    /**
     * Gender preference for ride matching
     * Options: FEMALE_ONLY, MALE_SINGLE_FEMALE, MALE_ALL_FEMALE, NO_PREFERENCE
     */
    private GenderPreference genderPreference;
    
    /**
     * Preferred vehicle types for rides
     * Can include: CAR, MOTORCYCLE, SCOOTER, BICYCLE
     */
    private List<VehicleType> vehicleTypePreferences;
    
    /**
     * List of favorite driver user IDs
     */
    private List<String> favoriteDrivers;
}
