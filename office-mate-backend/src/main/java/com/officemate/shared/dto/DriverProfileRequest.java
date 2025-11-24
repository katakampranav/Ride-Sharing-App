package com.officemate.shared.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import java.time.LocalDate;

/**
 * Request DTO for creating or updating a driver profile.
 * Contains driver-specific information including license and vehicle details.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DriverProfileRequest {
    
    /**
     * Driver's license number
     */
    @NotBlank(message = "License number is required")
    @Pattern(regexp = "^[A-Z0-9-]+$", message = "License number must contain only uppercase letters, numbers, and hyphens")
    private String licenseNumber;
    
    /**
     * License expiry date
     */
    @NotNull(message = "License expiry date is required")
    private LocalDate licenseExpiry;
    
    /**
     * Vehicle information
     */
    @NotNull(message = "Vehicle information is required")
    @Valid
    private VehicleInfoDTO vehicle;
    
    /**
     * Maximum acceptable detour distance in meters (up to 500m)
     */
    @NotNull(message = "Maximum detour distance is required")
    @Min(value = 0, message = "Maximum detour distance must be at least 0 meters")
    @Max(value = 500, message = "Maximum detour distance must not exceed 500 meters")
    private Integer maxDetourDistance;
    
    /**
     * Route preferences for the driver
     */
    @Valid
    private RoutePreferencesDTO routePreferences;
}
