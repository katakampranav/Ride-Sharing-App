package com.officemate.modules.profile.repository;

import com.officemate.modules.profile.entity.DriverProfile;
import com.officemate.shared.enums.VehicleType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Spring Data JPA repository for DriverProfile entity.
 * Provides CRUD operations and custom queries for driver profile management.
 */
@Repository
public interface DriverProfileRepository extends JpaRepository<DriverProfile, UUID> {

    /**
     * Finds a driver profile by driver ID with UserProfile eagerly loaded
     * 
     * @param driverId The driver's unique identifier
     * @return Optional containing the driver profile if found
     */
    @Query("SELECT dp FROM DriverProfile dp JOIN FETCH dp.userProfile WHERE dp.driverId = :driverId")
    Optional<DriverProfile> findByIdWithUserProfile(@Param("driverId") UUID driverId);

    /**
     * Checks if a driver profile exists for the given user ID
     * 
     * @param driverId The driver's unique identifier
     * @return true if driver profile exists
     */
    boolean existsByDriverId(UUID driverId);

    /**
     * Finds all driver profiles by vehicle type
     * 
     * @param vehicleType The vehicle type to filter by
     * @return List of driver profiles with the specified vehicle type
     */
    List<DriverProfile> findByVehicleType(VehicleType vehicleType);

    /**
     * Finds all verified driver profiles by vehicle type
     * 
     * @param vehicleType The vehicle type to filter by
     * @return List of verified driver profiles with the specified vehicle type
     */
    @Query("SELECT dp FROM DriverProfile dp WHERE dp.vehicleType = :vehicleType AND dp.licenseVerified = true")
    List<DriverProfile> findVerifiedDriversByVehicleType(@Param("vehicleType") VehicleType vehicleType);

    /**
     * Finds all driver profiles with expired licenses
     * 
     * @param currentDate The current date to compare against
     * @return List of driver profiles with expired licenses
     */
    @Query("SELECT dp FROM DriverProfile dp WHERE dp.licenseExpiry < :currentDate")
    List<DriverProfile> findDriversWithExpiredLicenses(@Param("currentDate") LocalDate currentDate);

    /**
     * Finds all driver profiles with licenses expiring soon
     * 
     * @param currentDate The current date
     * @param expiryDate The date to check expiry against
     * @return List of driver profiles with licenses expiring between current date and expiry date
     */
    @Query("SELECT dp FROM DriverProfile dp WHERE dp.licenseExpiry BETWEEN :currentDate AND :expiryDate")
    List<DriverProfile> findDriversWithExpiringLicenses(
        @Param("currentDate") LocalDate currentDate,
        @Param("expiryDate") LocalDate expiryDate
    );

    /**
     * Finds a driver profile by license number
     * 
     * @param licenseNumber The license number to search for
     * @return Optional containing the driver profile if found
     */
    Optional<DriverProfile> findByLicenseNumber(String licenseNumber);

    /**
     * Checks if a license number is already registered
     * 
     * @param licenseNumber The license number to check
     * @return true if license number exists
     */
    boolean existsByLicenseNumber(String licenseNumber);

    /**
     * Finds all driver profiles that are verified
     * 
     * @return List of verified driver profiles
     */
    @Query("SELECT dp FROM DriverProfile dp WHERE dp.licenseVerified = true")
    List<DriverProfile> findAllVerifiedDrivers();

    /**
     * Finds all driver profiles that are not verified
     * 
     * @return List of unverified driver profiles
     */
    @Query("SELECT dp FROM DriverProfile dp WHERE dp.licenseVerified = false")
    List<DriverProfile> findAllUnverifiedDrivers();
}
