package com.officemate.shared.dto;

import com.officemate.shared.enums.FuelType;
import com.officemate.shared.enums.VehicleType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Response DTO for driver profile information.
 * Contains driver-specific information including license and vehicle details.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DriverProfileResponse {
    
    /**
     * Unique driver identifier (same as user ID)
     */
    private String driverId;
    
    /**
     * Driver's license number
     */
    private String licenseNumber;
    
    /**
     * License expiry date
     */
    private LocalDate licenseExpiry;
    
    /**
     * License verification status
     */
    private boolean licenseVerified;
    
    /**
     * Maximum acceptable detour distance in meters
     */
    private Integer maxDetourDistance;
    
    /**
     * Vehicle type (CAR, MOTORCYCLE, SCOOTER, BICYCLE)
     */
    private VehicleType vehicleType;
    
    /**
     * Vehicle make
     */
    private String vehicleMake;
    
    /**
     * Vehicle model
     */
    private String vehicleModel;
    
    /**
     * Vehicle year
     */
    private Integer vehicleYear;
    
    /**
     * Vehicle license plate
     */
    private String licensePlate;
    
    /**
     * Vehicle passenger capacity
     */
    private Integer vehicleCapacity;
    
    /**
     * Fuel type (PETROL, DIESEL, ELECTRIC, HYBRID, CNG)
     */
    private FuelType fuelType;
    
    /**
     * Route preferences (if available)
     */
    private RoutePreferencesDTO routePreferences;
    
    /**
     * Profile creation timestamp
     */
    private LocalDateTime createdAt;
    
    /**
     * Last profile update timestamp
     */
    private LocalDateTime updatedAt;
}
