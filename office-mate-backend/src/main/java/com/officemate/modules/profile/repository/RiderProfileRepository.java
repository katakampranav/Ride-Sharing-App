package com.officemate.modules.profile.repository;

import com.officemate.modules.profile.entity.RiderProfile;
import com.officemate.shared.enums.GenderPreference;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Spring Data JPA repository for RiderProfile entity.
 * Provides CRUD operations and custom queries for rider profile management.
 */
@Repository
public interface RiderProfileRepository extends JpaRepository<RiderProfile, UUID> {

    /**
     * Finds a rider profile by rider ID with UserProfile eagerly loaded
     * 
     * @param riderId The rider's unique identifier
     * @return Optional containing the rider profile if found
     */
    @Query("SELECT rp FROM RiderProfile rp JOIN FETCH rp.userProfile WHERE rp.riderId = :riderId")
    Optional<RiderProfile> findByIdWithUserProfile(@Param("riderId") UUID riderId);

    /**
     * Checks if a rider profile exists for the given user ID
     * 
     * @param riderId The rider's unique identifier
     * @return true if rider profile exists
     */
    boolean existsByRiderId(UUID riderId);

    /**
     * Finds all rider profiles by gender preference
     * 
     * @param genderPreference The gender preference to filter by
     * @return List of rider profiles with the specified gender preference
     */
    List<RiderProfile> findByGenderPreference(GenderPreference genderPreference);

    /**
     * Finds all rider profiles that have a specific driver in their favorites
     * 
     * @param driverId The driver's user ID to search for
     * @return List of rider profiles that have favorited the driver
     */
    @Query("SELECT rp FROM RiderProfile rp WHERE :driverId = ANY(rp.favoriteDrivers)")
    List<RiderProfile> findByFavoriteDriver(@Param("driverId") String driverId);

    /**
     * Finds all rider profiles that prefer a specific vehicle type
     * 
     * @param vehicleType The vehicle type to search for
     * @return List of rider profiles that prefer the vehicle type
     */
    @Query("SELECT rp FROM RiderProfile rp WHERE :vehicleType = ANY(rp.vehicleTypePreferences)")
    List<RiderProfile> findByVehicleTypePreference(@Param("vehicleType") String vehicleType);

    /**
     * Finds all rider profiles with gender preferences (excluding NO_PREFERENCE)
     * 
     * @return List of rider profiles with specific gender preferences
     */
    @Query("SELECT rp FROM RiderProfile rp WHERE rp.genderPreference IS NOT NULL AND rp.genderPreference != 'NO_PREFERENCE'")
    List<RiderProfile> findAllWithGenderPreferences();

    /**
     * Finds all rider profiles with no gender preference
     * 
     * @return List of rider profiles with no gender preference
     */
    @Query("SELECT rp FROM RiderProfile rp WHERE rp.genderPreference IS NULL OR rp.genderPreference = 'NO_PREFERENCE'")
    List<RiderProfile> findAllWithNoGenderPreference();
}
