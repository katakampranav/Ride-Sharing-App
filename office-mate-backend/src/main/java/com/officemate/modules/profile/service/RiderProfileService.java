package com.officemate.modules.profile.service;

import com.officemate.modules.auth.entity.UserAccount;
import com.officemate.modules.auth.repository.UserAccountRepository;
import com.officemate.modules.profile.entity.RiderProfile;
import com.officemate.modules.profile.entity.UserProfile;
import com.officemate.modules.profile.model.RoutePreference;
import com.officemate.modules.profile.repository.RiderProfileRepository;
import com.officemate.modules.profile.repository.UserProfileRepository;
import com.officemate.shared.dto.RiderProfileRequest;
import com.officemate.shared.dto.RiderProfileResponse;
import com.officemate.shared.dto.RoutePreferencesDTO;
import com.officemate.shared.enums.GenderPreference;
import com.officemate.shared.enums.VehicleType;
import com.officemate.shared.exception.ProfileAccessException;
import jakarta.persistence.EntityNotFoundException;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Service for managing rider profiles.
 * Handles rider profile creation, updates, gender preference management,
 * vehicle type preference management, route preferences, and favorite driver management.
 * Requires both mobile and email verification before rider profile creation.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class RiderProfileService {

    private final RiderProfileRepository riderProfileRepository;
    private final UserProfileRepository userProfileRepository;
    private final UserAccountRepository userAccountRepository;
    private final RoutePreferencesService routePreferencesService;

    /**
     * Creates a rider profile with preferences and route information.
     * Requires both mobile and email verification.
     * 
     * @param userId The user's unique identifier
     * @param request Rider profile request containing preferences and route information
     * @return Created RiderProfileResponse
     * @throws ProfileAccessException if user is not fully verified
     * @throws EntityNotFoundException if user profile not found
     * @throws IllegalStateException if rider profile already exists
     */
    @Transactional
    public RiderProfileResponse createRiderProfile(UUID userId, RiderProfileRequest request) {
        log.info("Creating rider profile for user: {}", userId);

        // Verify user has both mobile and email verified
        UserAccount userAccount = userAccountRepository.findById(userId)
            .orElseThrow(() -> new EntityNotFoundException("User account not found: " + userId));

        if (!userAccount.isFullyVerified()) {
            log.warn("Rider profile creation denied for user {} - verification incomplete. Mobile: {}, Email: {}",
                userId, userAccount.getPhoneVerified(), userAccount.getEmailVerified());
            throw new ProfileAccessException(
                "Both mobile and email verification required before creating rider profile",
                userAccount.getPhoneVerified(),
                userAccount.getEmailVerified()
            );
        }

        // Check if user profile exists
        UserProfile userProfile = userProfileRepository.findById(userId)
            .orElseThrow(() -> new EntityNotFoundException("User profile not found: " + userId));

        // Check if rider profile already exists
        if (riderProfileRepository.existsByRiderId(userId)) {
            log.warn("Rider profile already exists for user: {}", userId);
            throw new IllegalStateException("Rider profile already exists for user: " + userId);
        }

        // Create rider profile
        RiderProfile riderProfile = RiderProfile.builder()
            .riderId(userId)
            .userProfile(userProfile)
            .genderPreference(request.getGenderPreference())
            .vehicleTypePreferences(convertVehicleTypesToStrings(request.getVehicleTypePreferences()))
            .favoriteDrivers(request.getFavoriteDrivers() != null ? 
                new ArrayList<>(request.getFavoriteDrivers()) : new ArrayList<>())
            .build();

        RiderProfile savedProfile = riderProfileRepository.save(riderProfile);
        log.info("Successfully created rider profile for user: {}", userId);

        // Store route preferences in DynamoDB if provided
        if (request.getRoutePreferences() != null) {
            routePreferencesService.saveRiderRoutePreferences(userId, request.getRoutePreferences());
            log.info("Saved route preferences for rider: {}", userId);
        }

        return buildRiderProfileResponse(savedProfile, request.getRoutePreferences());
    }

    /**
     * Updates rider profile information including preferences and route information.
     * 
     * @param userId The user's unique identifier
     * @param request Updated rider profile information
     * @return Updated RiderProfileResponse
     * @throws EntityNotFoundException if rider profile not found
     */
    @Transactional
    public RiderProfileResponse updateRiderProfile(UUID userId, RiderProfileRequest request) {
        log.info("Updating rider profile for user: {}", userId);

        RiderProfile riderProfile = riderProfileRepository.findById(userId)
            .orElseThrow(() -> new EntityNotFoundException("Rider profile not found for user: " + userId));

        // Update gender preference if provided
        if (request.getGenderPreference() != null) {
            riderProfile.setGenderPreference(request.getGenderPreference());
        }

        // Update vehicle type preferences if provided
        if (request.getVehicleTypePreferences() != null) {
            riderProfile.setVehicleTypePreferences(
                convertVehicleTypesToStrings(request.getVehicleTypePreferences())
            );
        }

        // Update favorite drivers if provided
        if (request.getFavoriteDrivers() != null) {
            riderProfile.setFavoriteDrivers(new ArrayList<>(request.getFavoriteDrivers()));
        }

        RiderProfile updatedProfile = riderProfileRepository.save(riderProfile);
        log.info("Successfully updated rider profile for user: {}", userId);

        // Update route preferences in DynamoDB if provided
        RoutePreferencesDTO routePreferences = null;
        if (request.getRoutePreferences() != null) {
            routePreferencesService.updateRiderRoutePreferences(userId, request.getRoutePreferences());
            routePreferences = request.getRoutePreferences();
            log.info("Updated route preferences for rider: {}", userId);
        }

        return buildRiderProfileResponse(updatedProfile, routePreferences);
    }

    /**
     * Retrieves a rider profile by user ID.
     * 
     * @param userId The user's unique identifier
     * @return RiderProfileResponse
     * @throws EntityNotFoundException if rider profile not found
     */
    @Transactional
    public RiderProfileResponse getRiderProfile(UUID userId) {
        log.debug("Retrieving rider profile for user: {}", userId);

        RiderProfile riderProfile = riderProfileRepository.findByIdWithUserProfile(userId)
            .orElseThrow(() -> new EntityNotFoundException("Rider profile not found for user: " + userId));

        // Retrieve route preferences from DynamoDB
        RoutePreference homeToWork = routePreferencesService.getDriverRoutePreferences(userId, "HOME_TO_WORK");
        RoutePreferencesDTO routePreferences = homeToWork != null ? convertToRoutePreferencesDTO(homeToWork) : null;

        return buildRiderProfileResponse(riderProfile, routePreferences);
    }

    /**
     * Checks if a rider profile exists for the given user.
     * 
     * @param userId The user's unique identifier
     * @return true if rider profile exists
     */
    public boolean riderProfileExists(UUID userId) {
        return riderProfileRepository.existsByRiderId(userId);
    }

    /**
     * Updates gender preference for a rider.
     * 
     * @param userId The user's unique identifier
     * @param genderPreference New gender preference
     * @return Updated RiderProfileResponse
     * @throws EntityNotFoundException if rider profile not found
     */
    @Transactional
    public RiderProfileResponse updateGenderPreference(UUID userId, GenderPreference genderPreference) {
        log.info("Updating gender preference for rider: {} to {}", userId, genderPreference);

        RiderProfile riderProfile = riderProfileRepository.findById(userId)
            .orElseThrow(() -> new EntityNotFoundException("Rider profile not found for user: " + userId));

        riderProfile.setGenderPreference(genderPreference);
        RiderProfile updatedProfile = riderProfileRepository.save(riderProfile);

        log.info("Successfully updated gender preference for rider: {}", userId);
        return buildRiderProfileResponse(updatedProfile, null);
    }

    /**
     * Adds a vehicle type preference for a rider.
     * 
     * @param userId The user's unique identifier
     * @param vehicleType Vehicle type to add to preferences
     * @return Updated RiderProfileResponse
     * @throws EntityNotFoundException if rider profile not found
     */
    @Transactional
    public RiderProfileResponse addVehicleTypePreference(UUID userId, VehicleType vehicleType) {
        log.info("Adding vehicle type preference {} for rider: {}", vehicleType, userId);

        RiderProfile riderProfile = riderProfileRepository.findById(userId)
            .orElseThrow(() -> new EntityNotFoundException("Rider profile not found for user: " + userId));

        riderProfile.addVehicleTypePreference(vehicleType);
        RiderProfile updatedProfile = riderProfileRepository.save(riderProfile);

        log.info("Successfully added vehicle type preference for rider: {}", userId);
        return buildRiderProfileResponse(updatedProfile, null);
    }

    /**
     * Removes a vehicle type preference for a rider.
     * 
     * @param userId The user's unique identifier
     * @param vehicleType Vehicle type to remove from preferences
     * @return Updated RiderProfileResponse
     * @throws EntityNotFoundException if rider profile not found
     */
    @Transactional
    public RiderProfileResponse removeVehicleTypePreference(UUID userId, VehicleType vehicleType) {
        log.info("Removing vehicle type preference {} for rider: {}", vehicleType, userId);

        RiderProfile riderProfile = riderProfileRepository.findById(userId)
            .orElseThrow(() -> new EntityNotFoundException("Rider profile not found for user: " + userId));

        riderProfile.removeVehicleTypePreference(vehicleType);
        RiderProfile updatedProfile = riderProfileRepository.save(riderProfile);

        log.info("Successfully removed vehicle type preference for rider: {}", userId);
        return buildRiderProfileResponse(updatedProfile, null);
    }

    /**
     * Adds a driver to the rider's favorites list.
     * 
     * @param userId The rider's unique identifier
     * @param driverId The driver's user ID to add to favorites
     * @return Updated RiderProfileResponse
     * @throws EntityNotFoundException if rider profile not found
     */
    @Transactional
    public RiderProfileResponse addFavoriteDriver(UUID userId, UUID driverId) {
        log.info("Adding favorite driver {} for rider: {}", driverId, userId);

        RiderProfile riderProfile = riderProfileRepository.findById(userId)
            .orElseThrow(() -> new EntityNotFoundException("Rider profile not found for user: " + userId));

        riderProfile.addFavoriteDriver(driverId);
        RiderProfile updatedProfile = riderProfileRepository.save(riderProfile);

        log.info("Successfully added favorite driver for rider: {}", userId);
        return buildRiderProfileResponse(updatedProfile, null);
    }

    /**
     * Removes a driver from the rider's favorites list.
     * 
     * @param userId The rider's unique identifier
     * @param driverId The driver's user ID to remove from favorites
     * @return Updated RiderProfileResponse
     * @throws EntityNotFoundException if rider profile not found
     */
    @Transactional
    public RiderProfileResponse removeFavoriteDriver(UUID userId, UUID driverId) {
        log.info("Removing favorite driver {} for rider: {}", driverId, userId);

        RiderProfile riderProfile = riderProfileRepository.findById(userId)
            .orElseThrow(() -> new EntityNotFoundException("Rider profile not found for user: " + userId));

        riderProfile.removeFavoriteDriver(driverId);
        RiderProfile updatedProfile = riderProfileRepository.save(riderProfile);

        log.info("Successfully removed favorite driver for rider: {}", userId);
        return buildRiderProfileResponse(updatedProfile, null);
    }

    /**
     * Updates route preferences for a rider.
     * 
     * @param userId The rider's unique identifier
     * @param routePreferences New route preferences
     * @return Updated RiderProfileResponse
     * @throws EntityNotFoundException if rider profile not found
     */
    @Transactional
    public RiderProfileResponse updateRoutePreferences(UUID userId, RoutePreferencesDTO routePreferences) {
        log.info("Updating route preferences for rider: {}", userId);

        // Verify rider profile exists
        RiderProfile riderProfile = riderProfileRepository.findById(userId)
            .orElseThrow(() -> new EntityNotFoundException("Rider profile not found for user: " + userId));

        // Update route preferences in DynamoDB
        routePreferencesService.updateRiderRoutePreferences(userId, routePreferences);

        log.info("Successfully updated route preferences for rider: {}", userId);
        return buildRiderProfileResponse(riderProfile, routePreferences);
    }

    /**
     * Deletes a rider profile.
     * 
     * @param userId The user's unique identifier
     * @throws EntityNotFoundException if rider profile not found
     */
    @Transactional
    public void deleteRiderProfile(UUID userId) {
        log.info("Deleting rider profile for user: {}", userId);

        RiderProfile riderProfile = riderProfileRepository.findById(userId)
            .orElseThrow(() -> new EntityNotFoundException("Rider profile not found for user: " + userId));

        riderProfileRepository.delete(riderProfile);

        // Delete route preferences from DynamoDB
        routePreferencesService.deleteRiderRoutePreferences(userId);

        log.info("Successfully deleted rider profile for user: {}", userId);
    }

    /**
     * Builds a RiderProfileResponse DTO from RiderProfile entity.
     * 
     * @param riderProfile The rider profile entity
     * @param routePreferences Route preferences DTO (optional)
     * @return RiderProfileResponse DTO
     */
    private RiderProfileResponse buildRiderProfileResponse(RiderProfile riderProfile, 
                                                           RoutePreferencesDTO routePreferences) {
        return RiderProfileResponse.builder()
            .riderId(riderProfile.getRiderId().toString())
            .genderPreference(riderProfile.getGenderPreference())
            .vehicleTypePreferences(convertStringsToVehicleTypes(riderProfile.getVehicleTypePreferences()))
            .favoriteDrivers(riderProfile.getFavoriteDrivers())
            .routePreferences(routePreferences)
            .hasGenderPreference(riderProfile.hasGenderPreference())
            .createdAt(riderProfile.getCreatedAt())
            .updatedAt(riderProfile.getUpdatedAt())
            .build();
    }

    /**
     * Converts a list of VehicleType enums to strings for database storage.
     * 
     * @param vehicleTypes List of VehicleType enums
     * @return List of vehicle type strings
     */
    private List<String> convertVehicleTypesToStrings(List<VehicleType> vehicleTypes) {
        if (vehicleTypes == null) {
            return new ArrayList<>();
        }
        return vehicleTypes.stream()
            .map(VehicleType::name)
            .collect(Collectors.toList());
    }

    /**
     * Converts a list of vehicle type strings to VehicleType enums.
     * 
     * @param vehicleTypeStrings List of vehicle type strings
     * @return List of VehicleType enums
     */
    private List<VehicleType> convertStringsToVehicleTypes(List<String> vehicleTypeStrings) {
        if (vehicleTypeStrings == null) {
            return new ArrayList<>();
        }
        return vehicleTypeStrings.stream()
            .map(VehicleType::valueOf)
            .collect(Collectors.toList());
    }

    /**
     * Converts a RoutePreference entity to RoutePreferencesDTO.
     * 
     * @param routePreference RoutePreference entity from DynamoDB
     * @return RoutePreferencesDTO
     */
    private RoutePreferencesDTO convertToRoutePreferencesDTO(RoutePreference routePreference) {
        return RoutePreferencesDTO.builder()
            .startLatitude(routePreference.getStartLatitude())
            .startLongitude(routePreference.getStartLongitude())
            .startAddress(routePreference.getStartAddress())
            .endLatitude(routePreference.getEndLatitude())
            .endLongitude(routePreference.getEndLongitude())
            .endAddress(routePreference.getEndAddress())
            .preferredStartTimes(routePreference.getPreferredStartTimes())
            .isActive(routePreference.isActive())
            .build();
    }
}
