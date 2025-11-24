package com.officemate.modules.profile.controller;

import com.officemate.modules.profile.entity.DriverProfile;
import com.officemate.modules.profile.service.DriverProfileService;
import com.officemate.modules.profile.service.RiderProfileService;
import com.officemate.modules.profile.service.UserProfileService;
import com.officemate.shared.dto.*;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/**
 * REST controller for user profile management operations.
 * Handles profile retrieval, updates, and driver/rider profile management.
 * All endpoints require authentication and most require email verification.
 */
@RestController
@RequestMapping("/users/{userId}/profile")
@RequiredArgsConstructor
@Slf4j
public class ProfileController {

    private final UserProfileService userProfileService;
    private final DriverProfileService driverProfileService;
    private final RiderProfileService riderProfileService;

    /**
     * Get user profile by user ID.
     * Returns basic profile information along with driver/rider status.
     * 
     * GET /users/{userId}/profile
     * 
     * @param userId The user's unique identifier
     * @return ProfileResponse containing profile information
     */
    @GetMapping
    @PreAuthorize("hasAuthority('MOBILE_VERIFIED') and (#userId == authentication.name or hasRole('ADMIN'))")
    public ResponseEntity<ProfileResponse> getProfile(@PathVariable String userId) {
        log.info("Get profile request for user: {}", userId);
        
        try {
            UUID userUuid = UUID.fromString(userId);
            ProfileResponse response = userProfileService.getProfile(userUuid);
            log.info("Successfully retrieved profile for user: {}", userId);
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            log.warn("Invalid user ID format: {}", userId);
            throw e;
        }
    }

    /**
     * Update basic user profile information.
     * All fields are optional - only provided fields will be updated.
     * 
     * PUT /users/{userId}/profile
     * 
     * @param userId The user's unique identifier
     * @param request ProfileUpdateRequest containing fields to update
     * @return ProfileResponse with updated profile information
     */
    @PutMapping
    @PreAuthorize("hasAuthority('MOBILE_VERIFIED') and (#userId == authentication.name or hasRole('ADMIN'))")
    public ResponseEntity<ProfileResponse> updateProfile(
            @PathVariable String userId,
            @Valid @RequestBody ProfileUpdateRequest request) {
        
        log.info("Update profile request for user: {}", userId);
        
        try {
            UUID userUuid = UUID.fromString(userId);
            ProfileResponse response = userProfileService.updateProfile(
                userUuid,
                request.getFirstName(),
                request.getLastName(),
                request.getDateOfBirth(),
                request.getGender(),
                request.getProfileImageUrl()
            );
            log.info("Successfully updated profile for user: {}", userId);
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            log.warn("Profile update failed for user {}: {}", userId, e.getMessage());
            throw e;
        }
    }

    /**
     * Create driver profile with vehicle information and route preferences.
     * Requires both mobile and email verification.
     * 
     * POST /users/{userId}/driver-profile
     * 
     * @param userId The user's unique identifier
     * @param request DriverProfileRequest containing driver information
     * @return DriverProfileResponse with created driver profile
     */
    @PostMapping("/driver-profile")
    @PreAuthorize("hasAuthority('EMAIL_VERIFIED') and (#userId == authentication.name or hasRole('ADMIN'))")
    public ResponseEntity<DriverProfileResponse> createDriverProfile(
            @PathVariable String userId,
            @Valid @RequestBody DriverProfileRequest request) {
        
        log.info("Create driver profile request for user: {}", userId);
        
        try {
            UUID userUuid = UUID.fromString(userId);
            DriverProfile driverProfile = driverProfileService.createDriverProfile(userUuid, request);
            DriverProfileResponse response = buildDriverProfileResponse(driverProfile, request.getRoutePreferences());
            log.info("Successfully created driver profile for user: {}", userId);
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (IllegalArgumentException | IllegalStateException e) {
            log.warn("Driver profile creation failed for user {}: {}", userId, e.getMessage());
            throw e;
        }
    }

    /**
     * Update driver profile information.
     * Can update vehicle details, license information, and route preferences.
     * 
     * PUT /users/{userId}/driver-profile
     * 
     * @param userId The user's unique identifier
     * @param request DriverProfileRequest containing updated information
     * @return DriverProfileResponse with updated driver profile
     */
    @PutMapping("/driver-profile")
    @PreAuthorize("hasAuthority('EMAIL_VERIFIED') and (#userId == authentication.name or hasRole('ADMIN'))")
    public ResponseEntity<DriverProfileResponse> updateDriverProfile(
            @PathVariable String userId,
            @Valid @RequestBody DriverProfileRequest request) {
        
        log.info("Update driver profile request for user: {}", userId);
        
        try {
            UUID userUuid = UUID.fromString(userId);
            DriverProfile driverProfile = driverProfileService.updateDriverProfile(userUuid, request);
            DriverProfileResponse response = buildDriverProfileResponse(driverProfile, request.getRoutePreferences());
            log.info("Successfully updated driver profile for user: {}", userId);
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException | IllegalStateException e) {
            log.warn("Driver profile update failed for user {}: {}", userId, e.getMessage());
            throw e;
        }
    }

    /**
     * Create rider profile with preferences and route information.
     * Requires both mobile and email verification.
     * 
     * POST /users/{userId}/rider-profile
     * 
     * @param userId The user's unique identifier
     * @param request RiderProfileRequest containing rider preferences
     * @return RiderProfileResponse with created rider profile
     */
    @PostMapping("/rider-profile")
    @PreAuthorize("hasAuthority('EMAIL_VERIFIED') and (#userId == authentication.name or hasRole('ADMIN'))")
    public ResponseEntity<RiderProfileResponse> createRiderProfile(
            @PathVariable String userId,
            @Valid @RequestBody RiderProfileRequest request) {
        
        log.info("Create rider profile request for user: {}", userId);
        
        try {
            UUID userUuid = UUID.fromString(userId);
            RiderProfileResponse response = riderProfileService.createRiderProfile(userUuid, request);
            log.info("Successfully created rider profile for user: {}", userId);
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (IllegalArgumentException | IllegalStateException e) {
            log.warn("Rider profile creation failed for user {}: {}", userId, e.getMessage());
            throw e;
        }
    }

    /**
     * Update rider profile preferences and route information.
     * Can update gender preferences, vehicle type preferences, and favorite drivers.
     * 
     * PUT /users/{userId}/rider-profile
     * 
     * @param userId The user's unique identifier
     * @param request RiderProfileRequest containing updated preferences
     * @return RiderProfileResponse with updated rider profile
     */
    @PutMapping("/rider-profile")
    @PreAuthorize("hasAuthority('EMAIL_VERIFIED') and (#userId == authentication.name or hasRole('ADMIN'))")
    public ResponseEntity<RiderProfileResponse> updateRiderProfile(
            @PathVariable String userId,
            @Valid @RequestBody RiderProfileRequest request) {
        
        log.info("Update rider profile request for user: {}", userId);
        
        try {
            UUID userUuid = UUID.fromString(userId);
            RiderProfileResponse response = riderProfileService.updateRiderProfile(userUuid, request);
            log.info("Successfully updated rider profile for user: {}", userId);
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException | IllegalStateException e) {
            log.warn("Rider profile update failed for user {}: {}", userId, e.getMessage());
            throw e;
        }
    }

    /**
     * Builds a DriverProfileResponse DTO from DriverProfile entity.
     * 
     * @param driverProfile The driver profile entity
     * @param routePreferences Route preferences DTO (optional)
     * @return DriverProfileResponse DTO
     */
    private DriverProfileResponse buildDriverProfileResponse(DriverProfile driverProfile, 
                                                             RoutePreferencesDTO routePreferences) {
        return DriverProfileResponse.builder()
            .driverId(driverProfile.getDriverId().toString())
            .licenseNumber(driverProfile.getLicenseNumber())
            .licenseExpiry(driverProfile.getLicenseExpiry())
            .licenseVerified(driverProfile.getLicenseVerified())
            .maxDetourDistance(driverProfile.getMaxDetourDistance())
            .vehicleType(driverProfile.getVehicleType())
            .vehicleMake(driverProfile.getVehicleMake())
            .vehicleModel(driverProfile.getVehicleModel())
            .vehicleYear(driverProfile.getVehicleYear())
            .licensePlate(driverProfile.getLicensePlate())
            .vehicleCapacity(driverProfile.getVehicleCapacity())
            .fuelType(driverProfile.getFuelType())
            .routePreferences(routePreferences)
            .createdAt(driverProfile.getCreatedAt())
            .updatedAt(driverProfile.getUpdatedAt())
            .build();
    }
}
