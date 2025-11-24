package com.officemate.modules.profile.entity;

import com.officemate.shared.entity.AuditableEntity;
import com.officemate.shared.enums.GenderPreference;
import com.officemate.shared.enums.VehicleType;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * JPA Entity representing a rider profile in the PostgreSQL database.
 * Contains rider preferences including gender preferences and vehicle type preferences.
 * Requires both mobile and email verification before creation.
 */
@Entity
@Table(name = "rider_profiles")
@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RiderProfile extends AuditableEntity {

    /**
     * Unique identifier for the rider profile (same as UserProfile userId)
     */
    @Id
    @Column(name = "rider_id", updatable = false, nullable = false)
    private UUID riderId;

    /**
     * One-to-one relationship with UserProfile
     */
    @OneToOne(fetch = FetchType.LAZY)
    @MapsId
    @JoinColumn(name = "rider_id", nullable = false)
    private UserProfile userProfile;

    /**
     * Rider's gender preference for ride matching
     * (FEMALE_ONLY, MALE_SINGLE_FEMALE, MALE_ALL_FEMALE, NO_PREFERENCE)
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "gender_preference", length = 20)
    private GenderPreference genderPreference;

    /**
     * Rider's preferred vehicle types (array of VehicleType enums)
     * Stored as TEXT[] in PostgreSQL
     */
    @JdbcTypeCode(SqlTypes.ARRAY)
    @Column(name = "vehicle_type_preferences", columnDefinition = "text[]")
    @Builder.Default
    private List<String> vehicleTypePreferences = new ArrayList<>();

    /**
     * List of favorite driver user IDs
     * Stored as TEXT[] in PostgreSQL
     */
    @JdbcTypeCode(SqlTypes.ARRAY)
    @Column(name = "favorite_drivers", columnDefinition = "text[]")
    @Builder.Default
    private List<String> favoriteDrivers = new ArrayList<>();



    /**
     * Adds a vehicle type preference if not already present
     * 
     * @param vehicleType The vehicle type to add
     */
    public void addVehicleTypePreference(VehicleType vehicleType) {
        if (vehicleTypePreferences == null) {
            vehicleTypePreferences = new ArrayList<>();
        }
        String vehicleTypeStr = vehicleType.name();
        if (!vehicleTypePreferences.contains(vehicleTypeStr)) {
            vehicleTypePreferences.add(vehicleTypeStr);
        }
    }

    /**
     * Removes a vehicle type preference
     * 
     * @param vehicleType The vehicle type to remove
     */
    public void removeVehicleTypePreference(VehicleType vehicleType) {
        if (vehicleTypePreferences != null) {
            vehicleTypePreferences.remove(vehicleType.name());
        }
    }

    /**
     * Checks if a specific vehicle type is preferred
     * 
     * @param vehicleType The vehicle type to check
     * @return true if the vehicle type is in preferences
     */
    public boolean prefersVehicleType(VehicleType vehicleType) {
        return vehicleTypePreferences != null 
            && vehicleTypePreferences.contains(vehicleType.name());
    }

    /**
     * Adds a driver to favorites if not already present
     * 
     * @param driverId The driver's user ID
     */
    public void addFavoriteDriver(UUID driverId) {
        if (favoriteDrivers == null) {
            favoriteDrivers = new ArrayList<>();
        }
        String driverIdStr = driverId.toString();
        if (!favoriteDrivers.contains(driverIdStr)) {
            favoriteDrivers.add(driverIdStr);
        }
    }

    /**
     * Removes a driver from favorites
     * 
     * @param driverId The driver's user ID
     */
    public void removeFavoriteDriver(UUID driverId) {
        if (favoriteDrivers != null) {
            favoriteDrivers.remove(driverId.toString());
        }
    }

    /**
     * Checks if a driver is in favorites
     * 
     * @param driverId The driver's user ID
     * @return true if the driver is in favorites
     */
    public boolean isFavoriteDriver(UUID driverId) {
        return favoriteDrivers != null 
            && favoriteDrivers.contains(driverId.toString());
    }

    /**
     * Checks if the rider has any gender preference set
     * 
     * @return true if gender preference is not null and not NO_PREFERENCE
     */
    public boolean hasGenderPreference() {
        return genderPreference != null && genderPreference != GenderPreference.NO_PREFERENCE;
    }

    /**
     * Checks if the rider profile is complete with basic information
     * 
     * @return true if profile has been initialized
     */
    public boolean isComplete() {
        return true; // Rider profile is optional, so it's always considered complete
    }
}
