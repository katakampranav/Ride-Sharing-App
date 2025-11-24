package com.officemate.modules.profile.entity;

import com.officemate.shared.entity.AuditableEntity;
import com.officemate.shared.enums.FuelType;
import com.officemate.shared.enums.VehicleType;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.UUID;

/**
 * JPA Entity representing a driver profile in the PostgreSQL database.
 * Contains vehicle information, license details, and driver-specific preferences.
 * Requires both mobile and email verification before creation.
 */
@Entity
@Table(
    name = "driver_profiles",
    indexes = {
        @Index(name = "idx_driver_profiles_vehicle_type", columnList = "vehicle_type")
    }
)
@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DriverProfile extends AuditableEntity {

    /**
     * Unique identifier for the driver profile (same as UserProfile userId)
     */
    @Id
    @Column(name = "driver_id", updatable = false, nullable = false)
    private UUID driverId;

    /**
     * One-to-one relationship with UserProfile
     */
    @OneToOne(fetch = FetchType.LAZY)
    @MapsId
    @JoinColumn(name = "driver_id", nullable = false)
    private UserProfile userProfile;

    /**
     * Driver's license number
     */
    @NotBlank(message = "License number is required")
    @Size(max = 50, message = "License number must not exceed 50 characters")
    @Column(name = "license_number", nullable = false, length = 50)
    private String licenseNumber;

    /**
     * Driver's license expiry date
     */
    @NotNull(message = "License expiry date is required")
    @Future(message = "License expiry date must be in the future")
    @Column(name = "license_expiry", nullable = false)
    private LocalDate licenseExpiry;

    /**
     * Flag indicating if the license has been verified
     */
    @Column(name = "license_verified", nullable = false)
    @Builder.Default
    private Boolean licenseVerified = false;

    /**
     * Maximum detour distance the driver is willing to take (in meters, max 500)
     */
    @Min(value = 0, message = "Maximum detour distance must be at least 0 meters")
    @Max(value = 500, message = "Maximum detour distance must not exceed 500 meters")
    @Column(name = "max_detour_distance", nullable = false)
    @Builder.Default
    private Integer maxDetourDistance = 500;

    /**
     * Type of vehicle (CAR, MOTORCYCLE, SCOOTER, BICYCLE)
     */
    @NotNull(message = "Vehicle type is required")
    @Enumerated(EnumType.STRING)
    @Column(name = "vehicle_type", nullable = false, length = 20)
    private VehicleType vehicleType;

    /**
     * Vehicle make (e.g., Toyota, Honda)
     */
    @Size(max = 50, message = "Vehicle make must not exceed 50 characters")
    @Column(name = "vehicle_make", length = 50)
    private String vehicleMake;

    /**
     * Vehicle model (e.g., Camry, Civic)
     */
    @Size(max = 50, message = "Vehicle model must not exceed 50 characters")
    @Column(name = "vehicle_model", length = 50)
    private String vehicleModel;

    /**
     * Vehicle year
     */
    @Min(value = 1900, message = "Vehicle year must be 1900 or later")
    @Max(value = 2100, message = "Vehicle year must be 2100 or earlier")
    @Column(name = "vehicle_year")
    private Integer vehicleYear;

    /**
     * Vehicle license plate number
     */
    @Size(max = 20, message = "License plate must not exceed 20 characters")
    @Column(name = "license_plate", length = 20)
    private String licensePlate;

    /**
     * Vehicle passenger capacity
     * For cars: 1-7, For 2-wheelers: 1-2
     */
    @Min(value = 1, message = "Vehicle capacity must be at least 1")
    @Max(value = 7, message = "Vehicle capacity must not exceed 7")
    @Column(name = "vehicle_capacity")
    private Integer vehicleCapacity;

    /**
     * Vehicle fuel type (PETROL, DIESEL, ELECTRIC, HYBRID, CNG)
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "fuel_type", length = 20)
    private FuelType fuelType;



    /**
     * Checks if the license is currently valid (not expired)
     * 
     * @return true if license expiry date is in the future
     */
    public boolean isLicenseValid() {
        return licenseExpiry != null && licenseExpiry.isAfter(LocalDate.now());
    }

    /**
     * Checks if the driver profile is complete with all required information
     * 
     * @return true if all required fields are populated
     */
    public boolean isComplete() {
        return licenseNumber != null && !licenseNumber.isBlank()
            && licenseExpiry != null
            && vehicleType != null
            && vehicleCapacity != null;
    }

    /**
     * Validates vehicle capacity based on vehicle type
     * 
     * @return true if capacity is valid for the vehicle type
     */
    public boolean isCapacityValidForVehicleType() {
        if (vehicleType == null || vehicleCapacity == null) {
            return false;
        }
        
        return switch (vehicleType) {
            case CAR -> vehicleCapacity >= 1 && vehicleCapacity <= 7;
            case MOTORCYCLE, SCOOTER, BICYCLE -> vehicleCapacity >= 1 && vehicleCapacity <= 2;
        };
    }

    /**
     * Marks the license as verified
     */
    public void verifyLicense() {
        this.licenseVerified = true;
    }

    /**
     * Unmarks the license verification (e.g., when license information changes)
     */
    public void unverifyLicense() {
        this.licenseVerified = false;
    }
}
