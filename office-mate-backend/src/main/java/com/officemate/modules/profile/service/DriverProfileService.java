package com.officemate.modules.profile.service;

import com.officemate.modules.auth.entity.UserAccount;
import com.officemate.modules.auth.repository.UserAccountRepository;
import com.officemate.modules.profile.entity.DriverProfile;
import com.officemate.modules.profile.entity.UserProfile;
import com.officemate.modules.profile.repository.DriverProfileRepository;
import com.officemate.modules.profile.repository.UserProfileRepository;
import com.officemate.shared.dto.DriverProfileRequest;
import com.officemate.shared.dto.VehicleInfoDTO;
import com.officemate.shared.exception.ProfileAccessException;
import jakarta.persistence.EntityNotFoundException;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.UUID;

/**
 * Service for managing driver profiles.
 * Handles driver profile creation, updates, vehicle information management,
 * license verification, and route preferences storage.
 * Requires both mobile and email verification before driver profile creation.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class DriverProfileService {

    private final DriverProfileRepository driverProfileRepository;
    private final UserProfileRepository userProfileRepository;
    private final UserAccountRepository userAccountRepository;
    private final RoutePreferencesService routePreferencesService;
    private final LicenseVerificationService licenseVerificationService;

    /**
     * Creates a driver profile with vehicle information and route preferences.
     * Requires both mobile and email verification.
     * 
     * @param userId The user's unique identifier
     * @param request Driver profile request containing license, vehicle, and route information
     * @return Created DriverProfile entity
     * @throws ProfileAccessException if user is not fully verified
     * @throws EntityNotFoundException if user profile not found
     * @throws IllegalStateException if driver profile already exists
     */
    @Transactional
    public DriverProfile createDriverProfile(UUID userId, DriverProfileRequest request) {
        log.info("Creating driver profile for user: {}", userId);

        // Verify user has both mobile and email verified
        UserAccount userAccount = userAccountRepository.findById(userId)
            .orElseThrow(() -> new EntityNotFoundException("User account not found: " + userId));

        if (!userAccount.isFullyVerified()) {
            log.warn("Driver profile creation denied for user {} - verification incomplete. Mobile: {}, Email: {}",
                userId, userAccount.getPhoneVerified(), userAccount.getEmailVerified());
            throw new ProfileAccessException(
                "Both mobile and email verification required before creating driver profile",
                userAccount.getPhoneVerified(),
                userAccount.getEmailVerified()
            );
        }

        // Check if user profile exists
        UserProfile userProfile = userProfileRepository.findById(userId)
            .orElseThrow(() -> new EntityNotFoundException("User profile not found: " + userId));

        // Check if driver profile already exists
        if (driverProfileRepository.existsByDriverId(userId)) {
            log.warn("Driver profile already exists for user: {}", userId);
            throw new IllegalStateException("Driver profile already exists for user: " + userId);
        }

        // Check if license number is already registered
        if (driverProfileRepository.existsByLicenseNumber(request.getLicenseNumber())) {
            log.warn("License number already registered: {}", request.getLicenseNumber());
            throw new IllegalStateException("License number already registered: " + request.getLicenseNumber());
        }

        // Validate license expiry date
        if (request.getLicenseExpiry().isBefore(LocalDate.now())) {
            log.warn("License expiry date is in the past: {}", request.getLicenseExpiry());
            throw new IllegalArgumentException("License expiry date must be in the future");
        }

        // Validate vehicle capacity for vehicle type
        VehicleInfoDTO vehicle = request.getVehicle();
        validateVehicleCapacity(vehicle);

        // Create driver profile
        DriverProfile driverProfile = DriverProfile.builder()
            .driverId(userId)
            .userProfile(userProfile)
            .licenseNumber(request.getLicenseNumber())
            .licenseExpiry(request.getLicenseExpiry())
            .licenseVerified(false)
            .maxDetourDistance(request.getMaxDetourDistance())
            .vehicleType(vehicle.getVehicleType())
            .vehicleMake(vehicle.getMake())
            .vehicleModel(vehicle.getModel())
            .vehicleYear(vehicle.getYear())
            .licensePlate(vehicle.getLicensePlate())
            .vehicleCapacity(vehicle.getCapacity())
            .fuelType(vehicle.getFuelType())
            .build();

        DriverProfile savedProfile = driverProfileRepository.save(driverProfile);
        log.info("Successfully created driver profile for user: {}", userId);

        // Store route preferences in DynamoDB if provided
        if (request.getRoutePreferences() != null) {
            routePreferencesService.saveDriverRoutePreferences(userId, request.getRoutePreferences());
            log.info("Saved route preferences for driver: {}", userId);
        }

        // Initiate license verification (placeholder for external service integration)
        licenseVerificationService.initiateLicenseVerification(userId, request.getLicenseNumber());

        return savedProfile;
    }

    /**
     * Updates driver profile information including vehicle details and route preferences.
     * 
     * @param userId The user's unique identifier
     * @param request Updated driver profile information
     * @return Updated DriverProfile entity
     * @throws EntityNotFoundException if driver profile not found
     */
    @Transactional
    public DriverProfile updateDriverProfile(UUID userId, DriverProfileRequest request) {
        log.info("Updating driver profile for user: {}", userId);

        DriverProfile driverProfile = driverProfileRepository.findById(userId)
            .orElseThrow(() -> new EntityNotFoundException("Driver profile not found for user: " + userId));

        boolean licenseChanged = false;

        // Update license information if provided
        if (request.getLicenseNumber() != null && !request.getLicenseNumber().equals(driverProfile.getLicenseNumber())) {
            // Check if new license number is already registered
            if (driverProfileRepository.existsByLicenseNumber(request.getLicenseNumber())) {
                log.warn("License number already registered: {}", request.getLicenseNumber());
                throw new IllegalStateException("License number already registered: " + request.getLicenseNumber());
            }
            driverProfile.setLicenseNumber(request.getLicenseNumber());
            licenseChanged = true;
        }

        if (request.getLicenseExpiry() != null) {
            if (request.getLicenseExpiry().isBefore(LocalDate.now())) {
                throw new IllegalArgumentException("License expiry date must be in the future");
            }
            driverProfile.setLicenseExpiry(request.getLicenseExpiry());
            licenseChanged = true;
        }

        // If license information changed, unverify and re-initiate verification
        if (licenseChanged) {
            driverProfile.unverifyLicense();
            licenseVerificationService.initiateLicenseVerification(userId, driverProfile.getLicenseNumber());
            log.info("License information changed for user {}, re-verification initiated", userId);
        }

        // Update vehicle information if provided
        if (request.getVehicle() != null) {
            VehicleInfoDTO vehicle = request.getVehicle();
            validateVehicleCapacity(vehicle);

            driverProfile.setVehicleType(vehicle.getVehicleType());
            driverProfile.setVehicleMake(vehicle.getMake());
            driverProfile.setVehicleModel(vehicle.getModel());
            driverProfile.setVehicleYear(vehicle.getYear());
            driverProfile.setLicensePlate(vehicle.getLicensePlate());
            driverProfile.setVehicleCapacity(vehicle.getCapacity());
            driverProfile.setFuelType(vehicle.getFuelType());
        }

        // Update max detour distance if provided
        if (request.getMaxDetourDistance() != null) {
            driverProfile.setMaxDetourDistance(request.getMaxDetourDistance());
        }

        DriverProfile updatedProfile = driverProfileRepository.save(driverProfile);
        log.info("Successfully updated driver profile for user: {}", userId);

        // Update route preferences in DynamoDB if provided
        if (request.getRoutePreferences() != null) {
            routePreferencesService.updateDriverRoutePreferences(userId, request.getRoutePreferences());
            log.info("Updated route preferences for driver: {}", userId);
        }

        return updatedProfile;
    }

    /**
     * Retrieves a driver profile by user ID.
     * 
     * @param userId The user's unique identifier
     * @return DriverProfile entity
     * @throws EntityNotFoundException if driver profile not found
     */
    @Transactional
    public DriverProfile getDriverProfile(UUID userId) {
        log.debug("Retrieving driver profile for user: {}", userId);

        return driverProfileRepository.findByIdWithUserProfile(userId)
            .orElseThrow(() -> new EntityNotFoundException("Driver profile not found for user: " + userId));
    }

    /**
     * Checks if a driver profile exists for the given user.
     * 
     * @param userId The user's unique identifier
     * @return true if driver profile exists
     */
    public boolean driverProfileExists(UUID userId) {
        return driverProfileRepository.existsByDriverId(userId);
    }

    /**
     * Marks a driver's license as verified.
     * This method is called by the license verification service after successful verification.
     * 
     * @param userId The user's unique identifier
     * @throws EntityNotFoundException if driver profile not found
     */
    @Transactional
    public void verifyDriverLicense(UUID userId) {
        log.info("Verifying license for driver: {}", userId);

        DriverProfile driverProfile = driverProfileRepository.findById(userId)
            .orElseThrow(() -> new EntityNotFoundException("Driver profile not found for user: " + userId));

        driverProfile.verifyLicense();
        driverProfileRepository.save(driverProfile);

        log.info("Successfully verified license for driver: {}", userId);
    }

    /**
     * Deletes a driver profile.
     * 
     * @param userId The user's unique identifier
     * @throws EntityNotFoundException if driver profile not found
     */
    @Transactional
    public void deleteDriverProfile(UUID userId) {
        log.info("Deleting driver profile for user: {}", userId);

        DriverProfile driverProfile = driverProfileRepository.findById(userId)
            .orElseThrow(() -> new EntityNotFoundException("Driver profile not found for user: " + userId));

        driverProfileRepository.delete(driverProfile);

        // Delete route preferences from DynamoDB
        routePreferencesService.deleteDriverRoutePreferences(userId);

        log.info("Successfully deleted driver profile for user: {}", userId);
    }

    /**
     * Validates vehicle capacity based on vehicle type.
     * 
     * @param vehicle Vehicle information DTO
     * @throws IllegalArgumentException if capacity is invalid for vehicle type
     */
    private void validateVehicleCapacity(VehicleInfoDTO vehicle) {
        int capacity = vehicle.getCapacity();
        
        switch (vehicle.getVehicleType()) {
            case CAR:
                if (capacity < 1 || capacity > 7) {
                    throw new IllegalArgumentException("Car capacity must be between 1 and 7 passengers");
                }
                break;
            case MOTORCYCLE:
            case SCOOTER:
            case BICYCLE:
                if (capacity < 1 || capacity > 2) {
                    throw new IllegalArgumentException(
                        vehicle.getVehicleType() + " capacity must be between 1 and 2 passengers"
                    );
                }
                break;
            default:
                throw new IllegalArgumentException("Unknown vehicle type: " + vehicle.getVehicleType());
        }
    }
}
