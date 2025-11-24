package com.officemate.shared.dto;

import com.officemate.shared.enums.FuelType;
import com.officemate.shared.enums.VehicleType;
import com.officemate.shared.validation.LicensePlate;
import com.officemate.shared.validation.SafeText;
import com.officemate.shared.validation.VehicleTypeValid;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * DTO for vehicle information used in driver profiles.
 * Contains comprehensive vehicle details including type, fuel, and specifications.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@VehicleTypeValid(validateCapacity = true, validateFuelType = true)
public class VehicleInfoDTO {
    
    /**
     * Type of vehicle (CAR, MOTORCYCLE, SCOOTER, BICYCLE)
     */
    @NotNull(message = "Vehicle type is required")
    private VehicleType vehicleType;
    
    /**
     * Fuel type of the vehicle (PETROL, DIESEL, ELECTRIC, HYBRID, CNG)
     */
    @NotNull(message = "Fuel type is required")
    private FuelType fuelType;
    
    /**
     * Vehicle manufacturer/make
     */
    @NotBlank(message = "Vehicle make is required")
    @SafeText(maxLength = 50, allowSpecialChars = false, message = "Vehicle make contains invalid characters")
    private String make;
    
    /**
     * Vehicle model name
     */
    @NotBlank(message = "Vehicle model is required")
    @SafeText(maxLength = 50, allowSpecialChars = false, message = "Vehicle model contains invalid characters")
    private String model;
    
    /**
     * Year of manufacture
     */
    @NotNull(message = "Vehicle year is required")
    @Min(value = 1990, message = "Vehicle year must be 1990 or later")
    @Max(value = 2030, message = "Vehicle year must be 2030 or earlier")
    private Integer year;
    
    /**
     * Vehicle license plate number
     */
    @NotBlank(message = "License plate is required")
    @LicensePlate(region = "US", minLength = 2, maxLength = 10)
    private String licensePlate;
    
    /**
     * Maximum passenger capacity
     * For cars: 1-7 passengers
     * For two-wheelers: 1-2 passengers
     */
    @NotNull(message = "Vehicle capacity is required")
    @Min(value = 1, message = "Capacity must be at least 1")
    @Max(value = 7, message = "Capacity must not exceed 7")
    private Integer capacity;
}
