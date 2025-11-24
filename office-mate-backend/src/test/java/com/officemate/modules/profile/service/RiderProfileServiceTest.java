package com.officemate.modules.profile.service;

import com.officemate.modules.auth.entity.UserAccount;
import com.officemate.modules.auth.repository.UserAccountRepository;
import com.officemate.modules.profile.entity.RiderProfile;
import com.officemate.modules.profile.entity.UserProfile;
import com.officemate.modules.profile.repository.RiderProfileRepository;
import com.officemate.modules.profile.repository.UserProfileRepository;
import com.officemate.shared.dto.RiderProfileRequest;
import com.officemate.shared.dto.RoutePreferencesDTO;
import com.officemate.shared.enums.AccountStatus;
import com.officemate.shared.enums.GenderPreference;
import com.officemate.shared.enums.VehicleType;
import com.officemate.shared.exception.ProfileAccessException;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit tests for RiderProfileService.
 * Tests rider profile creation with gender and vehicle type preferences,
 * favorite driver management, and data validation.
 */
@ExtendWith(MockitoExtension.class)
class RiderProfileServiceTest {

    @Mock
    private RiderProfileRepository riderProfileRepository;

    @Mock
    private UserProfileRepository userProfileRepository;

    @Mock
    private UserAccountRepository userAccountRepository;

    @Mock
    private RoutePreferencesService routePreferencesService;

    @InjectMocks
    private RiderProfileService riderProfileService;

    private UUID testUserId;
    private UserAccount fullyVerifiedAccount;
    private UserAccount mobileOnlyAccount;
    private UserProfile testProfile;
    private RiderProfile testRiderProfile;
    private RiderProfileRequest validRequest;

    @BeforeEach
    void setUp() {
        testUserId = UUID.randomUUID();

        // Fully verified account
        fullyVerifiedAccount = UserAccount.builder()
            .userId(testUserId)
            .phoneNumber("+1234567890")
            .phoneVerified(true)
            .corporateEmail("user@company.com")
            .emailVerified(true)
            .accountStatus(AccountStatus.ACTIVE)
            .build();

        // Mobile only verified account
        mobileOnlyAccount = UserAccount.builder()
            .userId(testUserId)
            .phoneNumber("+1234567890")
            .phoneVerified(true)
            .emailVerified(false)
            .accountStatus(AccountStatus.PENDING_EMAIL)
            .build();

        // Test profile
        testProfile = UserProfile.builder()
            .userId(testUserId)
            .userAccount(fullyVerifiedAccount)
            .firstName("Jane")
            .lastName("Doe")
            .gender("FEMALE")
            .build();

        // Test rider profile
        testRiderProfile = RiderProfile.builder()
            .riderId(testUserId)
            .userProfile(testProfile)
            .genderPreference(GenderPreference.FEMALE_ONLY)
            .vehicleTypePreferences(Arrays.asList("CAR", "MOTORCYCLE"))
            .favoriteDrivers(new java.util.ArrayList<>())
            .build();

        // Valid request
        RoutePreferencesDTO routePrefs = RoutePreferencesDTO.builder()
            .startLatitude(40.7128)
            .startLongitude(-74.0060)
            .startAddress("New York, NY")
            .endLatitude(40.7589)
            .endLongitude(-73.9851)
            .endAddress("Times Square, NY")
            .preferredStartTimes(Arrays.asList("08:00", "09:00"))
            .build();

        validRequest = RiderProfileRequest.builder()
            .genderPreference(GenderPreference.FEMALE_ONLY)
            .vehicleTypePreferences(Arrays.asList(VehicleType.CAR, VehicleType.MOTORCYCLE))
            .routePreferences(routePrefs)
            .build();
    }

    // ========== Rider Profile Creation Tests ==========

    @Test
    void testCreateRiderProfile_FullyVerified_Success() {
        // Arrange
        when(userAccountRepository.findById(testUserId)).thenReturn(Optional.of(fullyVerifiedAccount));
        when(userProfileRepository.findById(testUserId)).thenReturn(Optional.of(testProfile));
        when(riderProfileRepository.existsByRiderId(testUserId)).thenReturn(false);
        when(riderProfileRepository.save(any(RiderProfile.class))).thenReturn(testRiderProfile);
        doNothing().when(routePreferencesService).saveRiderRoutePreferences(any(UUID.class), any(RoutePreferencesDTO.class));

        // Act
        var response = riderProfileService.createRiderProfile(testUserId, validRequest);

        // Assert
        assertNotNull(response);
        assertEquals(testUserId.toString(), response.getRiderId());
        assertEquals(GenderPreference.FEMALE_ONLY, response.getGenderPreference());
        assertNotNull(response.getVehicleTypePreferences());
        assertEquals(2, response.getVehicleTypePreferences().size());
        verify(riderProfileRepository).save(any(RiderProfile.class));
        verify(routePreferencesService).saveRiderRoutePreferences(any(UUID.class), any(RoutePreferencesDTO.class));
    }

    @Test
    void testCreateRiderProfile_NotFullyVerified_ThrowsException() {
        // Arrange
        when(userAccountRepository.findById(testUserId)).thenReturn(Optional.of(mobileOnlyAccount));

        // Act & Assert
        ProfileAccessException exception = assertThrows(
            ProfileAccessException.class,
            () -> riderProfileService.createRiderProfile(testUserId, validRequest)
        );

        assertTrue(exception.getMessage().contains("Both mobile and email verification required"));
        verify(riderProfileRepository, never()).save(any(RiderProfile.class));
    }

    @Test
    void testCreateRiderProfile_AlreadyExists_ThrowsException() {
        // Arrange
        when(userAccountRepository.findById(testUserId)).thenReturn(Optional.of(fullyVerifiedAccount));
        when(userProfileRepository.findById(testUserId)).thenReturn(Optional.of(testProfile));
        when(riderProfileRepository.existsByRiderId(testUserId)).thenReturn(true);

        // Act & Assert
        IllegalStateException exception = assertThrows(
            IllegalStateException.class,
            () -> riderProfileService.createRiderProfile(testUserId, validRequest)
        );

        assertTrue(exception.getMessage().contains("Rider profile already exists"));
        verify(riderProfileRepository, never()).save(any(RiderProfile.class));
    }

    // ========== Gender Preference Tests ==========

    @Test
    void testCreateRiderProfile_FemaleOnlyPreference_Success() {
        // Arrange
        validRequest.setGenderPreference(GenderPreference.FEMALE_ONLY);

        when(userAccountRepository.findById(testUserId)).thenReturn(Optional.of(fullyVerifiedAccount));
        when(userProfileRepository.findById(testUserId)).thenReturn(Optional.of(testProfile));
        when(riderProfileRepository.existsByRiderId(testUserId)).thenReturn(false);
        when(riderProfileRepository.save(any(RiderProfile.class))).thenAnswer(invocation -> {
            RiderProfile saved = invocation.getArgument(0);
            assertEquals(GenderPreference.FEMALE_ONLY, saved.getGenderPreference());
            return testRiderProfile;
        });
        doNothing().when(routePreferencesService).saveRiderRoutePreferences(any(UUID.class), any(RoutePreferencesDTO.class));

        // Act
        var response = riderProfileService.createRiderProfile(testUserId, validRequest);

        // Assert
        assertNotNull(response);
        assertEquals(GenderPreference.FEMALE_ONLY, response.getGenderPreference());
        verify(riderProfileRepository).save(any(RiderProfile.class));
    }

    @Test
    void testCreateRiderProfile_MaleSingleFemalePreference_Success() {
        // Arrange
        validRequest.setGenderPreference(GenderPreference.MALE_SINGLE_FEMALE);

        when(userAccountRepository.findById(testUserId)).thenReturn(Optional.of(fullyVerifiedAccount));
        when(userProfileRepository.findById(testUserId)).thenReturn(Optional.of(testProfile));
        when(riderProfileRepository.existsByRiderId(testUserId)).thenReturn(false);
        when(riderProfileRepository.save(any(RiderProfile.class))).thenAnswer(invocation -> {
            RiderProfile saved = invocation.getArgument(0);
            assertEquals(GenderPreference.MALE_SINGLE_FEMALE, saved.getGenderPreference());
            return testRiderProfile;
        });
        doNothing().when(routePreferencesService).saveRiderRoutePreferences(any(UUID.class), any(RoutePreferencesDTO.class));

        // Act
        var response = riderProfileService.createRiderProfile(testUserId, validRequest);

        // Assert
        assertNotNull(response);
        verify(riderProfileRepository).save(any(RiderProfile.class));
    }

    @Test
    void testCreateRiderProfile_NoPreference_Success() {
        // Arrange
        validRequest.setGenderPreference(GenderPreference.NO_PREFERENCE);

        when(userAccountRepository.findById(testUserId)).thenReturn(Optional.of(fullyVerifiedAccount));
        when(userProfileRepository.findById(testUserId)).thenReturn(Optional.of(testProfile));
        when(riderProfileRepository.existsByRiderId(testUserId)).thenReturn(false);
        when(riderProfileRepository.save(any(RiderProfile.class))).thenAnswer(invocation -> {
            RiderProfile saved = invocation.getArgument(0);
            assertEquals(GenderPreference.NO_PREFERENCE, saved.getGenderPreference());
            return testRiderProfile;
        });
        doNothing().when(routePreferencesService).saveRiderRoutePreferences(any(UUID.class), any(RoutePreferencesDTO.class));

        // Act
        var response = riderProfileService.createRiderProfile(testUserId, validRequest);

        // Assert
        assertNotNull(response);
        verify(riderProfileRepository).save(any(RiderProfile.class));
    }

    // ========== Vehicle Type Preference Tests ==========

    @Test
    void testCreateRiderProfile_MultipleVehicleTypes_Success() {
        // Arrange
        List<VehicleType> vehicleTypes = Arrays.asList(
            VehicleType.CAR, 
            VehicleType.MOTORCYCLE, 
            VehicleType.SCOOTER
        );
        validRequest.setVehicleTypePreferences(vehicleTypes);

        when(userAccountRepository.findById(testUserId)).thenReturn(Optional.of(fullyVerifiedAccount));
        when(userProfileRepository.findById(testUserId)).thenReturn(Optional.of(testProfile));
        when(riderProfileRepository.existsByRiderId(testUserId)).thenReturn(false);
        when(riderProfileRepository.save(any(RiderProfile.class))).thenAnswer(invocation -> {
            RiderProfile saved = invocation.getArgument(0);
            assertEquals(3, saved.getVehicleTypePreferences().size());
            return testRiderProfile;
        });
        doNothing().when(routePreferencesService).saveRiderRoutePreferences(any(UUID.class), any(RoutePreferencesDTO.class));

        // Act
        var response = riderProfileService.createRiderProfile(testUserId, validRequest);

        // Assert
        assertNotNull(response);
        assertEquals(2, response.getVehicleTypePreferences().size());
        verify(riderProfileRepository).save(any(RiderProfile.class));
    }

    @Test
    void testCreateRiderProfile_SingleVehicleType_Success() {
        // Arrange
        validRequest.setVehicleTypePreferences(Arrays.asList(VehicleType.CAR));

        when(userAccountRepository.findById(testUserId)).thenReturn(Optional.of(fullyVerifiedAccount));
        when(userProfileRepository.findById(testUserId)).thenReturn(Optional.of(testProfile));
        when(riderProfileRepository.existsByRiderId(testUserId)).thenReturn(false);
        when(riderProfileRepository.save(any(RiderProfile.class))).thenAnswer(invocation -> {
            RiderProfile saved = invocation.getArgument(0);
            assertEquals(1, saved.getVehicleTypePreferences().size());
            return testRiderProfile;
        });
        doNothing().when(routePreferencesService).saveRiderRoutePreferences(any(UUID.class), any(RoutePreferencesDTO.class));

        // Act
        var response = riderProfileService.createRiderProfile(testUserId, validRequest);

        // Assert
        assertNotNull(response);
        verify(riderProfileRepository).save(any(RiderProfile.class));
    }

    @Test
    void testCreateRiderProfile_NoVehicleTypePreference_Success() {
        // Arrange
        validRequest.setVehicleTypePreferences(null);

        when(userAccountRepository.findById(testUserId)).thenReturn(Optional.of(fullyVerifiedAccount));
        when(userProfileRepository.findById(testUserId)).thenReturn(Optional.of(testProfile));
        when(riderProfileRepository.existsByRiderId(testUserId)).thenReturn(false);
        when(riderProfileRepository.save(any(RiderProfile.class))).thenReturn(testRiderProfile);
        doNothing().when(routePreferencesService).saveRiderRoutePreferences(any(UUID.class), any(RoutePreferencesDTO.class));

        // Act
        var response = riderProfileService.createRiderProfile(testUserId, validRequest);

        // Assert
        assertNotNull(response);
        verify(riderProfileRepository).save(any(RiderProfile.class));
    }

    // ========== Rider Profile Retrieval Tests ==========

    @Test
    void testGetRiderProfile_Success() {
        // Arrange
        when(riderProfileRepository.findByIdWithUserProfile(testUserId))
            .thenReturn(Optional.of(testRiderProfile));
        when(routePreferencesService.getDriverRoutePreferences(testUserId, "HOME_TO_WORK"))
            .thenReturn(null);

        // Act
        var response = riderProfileService.getRiderProfile(testUserId);

        // Assert
        assertNotNull(response);
        assertEquals(testUserId.toString(), response.getRiderId());
        assertEquals(GenderPreference.FEMALE_ONLY, response.getGenderPreference());
        verify(riderProfileRepository).findByIdWithUserProfile(testUserId);
    }

    @Test
    void testGetRiderProfile_NotFound_ThrowsException() {
        // Arrange
        when(riderProfileRepository.findByIdWithUserProfile(testUserId))
            .thenReturn(Optional.empty());

        // Act & Assert
        EntityNotFoundException exception = assertThrows(
            EntityNotFoundException.class,
            () -> riderProfileService.getRiderProfile(testUserId)
        );

        assertTrue(exception.getMessage().contains("Rider profile not found"));
    }

    // ========== Rider Profile Update Tests ==========

    @Test
    void testUpdateRiderProfile_GenderPreferenceChange_Success() {
        // Arrange
        when(riderProfileRepository.findById(testUserId))
            .thenReturn(Optional.of(testRiderProfile));
        when(riderProfileRepository.save(any(RiderProfile.class))).thenReturn(testRiderProfile);

        RiderProfileRequest updateRequest = RiderProfileRequest.builder()
            .genderPreference(GenderPreference.NO_PREFERENCE)
            .build();

        // Act
        var response = riderProfileService.updateRiderProfile(testUserId, updateRequest);

        // Assert
        assertNotNull(response);
        verify(riderProfileRepository).save(any(RiderProfile.class));
    }

    @Test
    void testUpdateRiderProfile_VehicleTypePreferenceChange_Success() {
        // Arrange
        when(riderProfileRepository.findById(testUserId))
            .thenReturn(Optional.of(testRiderProfile));
        when(riderProfileRepository.save(any(RiderProfile.class))).thenReturn(testRiderProfile);

        RiderProfileRequest updateRequest = RiderProfileRequest.builder()
            .vehicleTypePreferences(Arrays.asList(VehicleType.CAR))
            .build();

        // Act
        var response = riderProfileService.updateRiderProfile(testUserId, updateRequest);

        // Assert
        assertNotNull(response);
        verify(riderProfileRepository).save(any(RiderProfile.class));
    }

    // ========== Favorite Driver Tests ==========

    @Test
    void testAddFavoriteDriver_Success() {
        // Arrange
        UUID driverId = UUID.randomUUID();
        when(riderProfileRepository.findById(testUserId))
            .thenReturn(Optional.of(testRiderProfile));
        when(riderProfileRepository.save(any(RiderProfile.class))).thenReturn(testRiderProfile);

        // Act
        var response = riderProfileService.addFavoriteDriver(testUserId, driverId);

        // Assert
        assertNotNull(response);
        verify(riderProfileRepository).save(any(RiderProfile.class));
    }

    @Test
    void testRemoveFavoriteDriver_Success() {
        // Arrange
        UUID driverId = UUID.randomUUID();
        testRiderProfile.addFavoriteDriver(driverId);
        
        when(riderProfileRepository.findById(testUserId))
            .thenReturn(Optional.of(testRiderProfile));
        when(riderProfileRepository.save(any(RiderProfile.class))).thenReturn(testRiderProfile);

        // Act
        var response = riderProfileService.removeFavoriteDriver(testUserId, driverId);

        // Assert
        assertNotNull(response);
        verify(riderProfileRepository).save(any(RiderProfile.class));
    }

    @Test
    void testAddFavoriteDriver_RiderNotFound_ThrowsException() {
        // Arrange
        UUID driverId = UUID.randomUUID();
        when(riderProfileRepository.findById(testUserId))
            .thenReturn(Optional.empty());

        // Act & Assert
        EntityNotFoundException exception = assertThrows(
            EntityNotFoundException.class,
            () -> riderProfileService.addFavoriteDriver(testUserId, driverId)
        );

        assertTrue(exception.getMessage().contains("Rider profile not found"));
        verify(riderProfileRepository, never()).save(any(RiderProfile.class));
    }

    // ========== Access Control Tests ==========

    @Test
    void testRiderProfileExists_ReturnsTrue() {
        // Arrange
        when(riderProfileRepository.existsByRiderId(testUserId)).thenReturn(true);

        // Act
        boolean exists = riderProfileService.riderProfileExists(testUserId);

        // Assert
        assertTrue(exists);
        verify(riderProfileRepository).existsByRiderId(testUserId);
    }

    @Test
    void testRiderProfileExists_ReturnsFalse() {
        // Arrange
        when(riderProfileRepository.existsByRiderId(testUserId)).thenReturn(false);

        // Act
        boolean exists = riderProfileService.riderProfileExists(testUserId);

        // Assert
        assertFalse(exists);
        verify(riderProfileRepository).existsByRiderId(testUserId);
    }
}
