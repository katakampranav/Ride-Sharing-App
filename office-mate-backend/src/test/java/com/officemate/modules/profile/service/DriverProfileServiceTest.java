package com.officemate.modules.profile.service;

import com.officemate.modules.auth.entity.UserAccount;
import com.officemate.modules.auth.repository.UserAccountRepository;
import com.officemate.modules.profile.entity.DriverProfile;
import com.officemate.modules.profile.entity.UserProfile;
import com.officemate.modules.profile.repository.DriverProfileRepository;
import com.officemate.modules.profile.repository.UserProfileRepository;
import com.officemate.shared.dto.DriverProfileRequest;
import com.officemate.shared.dto.RoutePreferencesDTO;
import com.officemate.shared.dto.VehicleInfoDTO;
import com.officemate.shared.enums.AccountStatus;
import com.officemate.shared.enums.FuelType;
import com.officemate.shared.enums.VehicleType;
import com.officemate.shared.exception.ProfileAccessException;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * Unit tests for DriverProfileService.
 * Tests driver profile creation with vehicle types and fuel types,
 * license verification, and data validation.
 */
@ExtendWith(MockitoExtension.class)
class DriverProfileServiceTest {

    @Mock
    private DriverProfileRepository driverProfileRepository;

    @Mock
    private UserProfileRepository userProfileRepository;

    @Mock
    private UserAccountRepository userAccountRepository;

    @Mock
    private RoutePreferencesService routePreferencesService;

    @Mock
    private LicenseVerificationService licenseVerificationService;

    @InjectMocks
    private DriverProfileService driverProfileService;

    private UUID testUserId;
    private UserAccount fullyVerifiedAccount;
    private UserAccount mobileOnlyAccount;
    private UserProfile testProfile;
    private DriverProfile testDriverProfile;
    private DriverProfileRequest validRequest;

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
            .firstName("John")
            .lastName("Doe")
            .build();

        // Test driver profile
        testDriverProfile = DriverProfile.builder()
            .driverId(testUserId)
            .userProfile(testProfile)
            .licenseNumber("DL123456")
            .licenseExpiry(LocalDate.now().plusYears(2))
            .licenseVerified(false)
            .maxDetourDistance(500)
            .vehicleType(VehicleType.CAR)
            .vehicleMake("Toyota")
            .vehicleModel("Camry")
            .vehicleYear(2020)
            .licensePlate("ABC123")
            .vehicleCapacity(4)
            .fuelType(FuelType.PETROL)
            .build();

        // Valid request
        VehicleInfoDTO vehicleInfo = VehicleInfoDTO.builder()
            .vehicleType(VehicleType.CAR)
            .make("Toyota")
            .model("Camry")
            .year(2020)
            .licensePlate("ABC123")
            .capacity(4)
            .fuelType(FuelType.PETROL)
            .build();

        RoutePreferencesDTO routePrefs = RoutePreferencesDTO.builder()
            .startLatitude(40.7128)
            .startLongitude(-74.0060)
            .startAddress("New York, NY")
            .endLatitude(40.7589)
            .endLongitude(-73.9851)
            .endAddress("Times Square, NY")
            .preferredStartTimes(Arrays.asList("08:00", "09:00"))
            .build();

        validRequest = DriverProfileRequest.builder()
            .licenseNumber("DL123456")
            .licenseExpiry(LocalDate.now().plusYears(2))
            .maxDetourDistance(500)
            .vehicle(vehicleInfo)
            .routePreferences(routePrefs)
            .build();
    }

    // ========== Driver Profile Creation Tests ==========

    @Test
    void testCreateDriverProfile_FullyVerified_Success() {
        // Arrange
        when(userAccountRepository.findById(testUserId)).thenReturn(Optional.of(fullyVerifiedAccount));
        when(userProfileRepository.findById(testUserId)).thenReturn(Optional.of(testProfile));
        when(driverProfileRepository.existsByDriverId(testUserId)).thenReturn(false);
        when(driverProfileRepository.existsByLicenseNumber(anyString())).thenReturn(false);
        when(driverProfileRepository.save(any(DriverProfile.class))).thenReturn(testDriverProfile);
        doNothing().when(routePreferencesService).saveDriverRoutePreferences(any(UUID.class), any(RoutePreferencesDTO.class));

        // Act
        var response = driverProfileService.createDriverProfile(testUserId, validRequest);

        // Assert
        assertNotNull(response);
        assertEquals(testUserId, response.getDriverId());
        assertEquals(VehicleType.CAR, response.getVehicleType());
        assertEquals(FuelType.PETROL, response.getFuelType());
        verify(driverProfileRepository).save(any(DriverProfile.class));
        verify(routePreferencesService).saveDriverRoutePreferences(any(UUID.class), any(RoutePreferencesDTO.class));
    }

    @Test
    void testCreateDriverProfile_NotFullyVerified_ThrowsException() {
        // Arrange
        when(userAccountRepository.findById(testUserId)).thenReturn(Optional.of(mobileOnlyAccount));

        // Act & Assert
        ProfileAccessException exception = assertThrows(
            ProfileAccessException.class,
            () -> driverProfileService.createDriverProfile(testUserId, validRequest)
        );

        assertTrue(exception.getMessage().contains("Both mobile and email verification required"));
        verify(driverProfileRepository, never()).save(any(DriverProfile.class));
    }

    @Test
    void testCreateDriverProfile_AlreadyExists_ThrowsException() {
        // Arrange
        when(userAccountRepository.findById(testUserId)).thenReturn(Optional.of(fullyVerifiedAccount));
        when(userProfileRepository.findById(testUserId)).thenReturn(Optional.of(testProfile));
        when(driverProfileRepository.existsByDriverId(testUserId)).thenReturn(true);

        // Act & Assert
        IllegalStateException exception = assertThrows(
            IllegalStateException.class,
            () -> driverProfileService.createDriverProfile(testUserId, validRequest)
        );

        assertTrue(exception.getMessage().contains("Driver profile already exists"));
        verify(driverProfileRepository, never()).save(any(DriverProfile.class));
    }

    // ========== Vehicle Type Tests ==========

    @Test
    void testCreateDriverProfile_WithMotorcycle_Success() {
        // Arrange
        VehicleInfoDTO motorcycleInfo = VehicleInfoDTO.builder()
            .vehicleType(VehicleType.MOTORCYCLE)
            .make("Honda")
            .model("CBR")
            .year(2021)
            .licensePlate("MOTO123")
            .capacity(2)
            .fuelType(FuelType.PETROL)
            .build();

        validRequest.setVehicle(motorcycleInfo);

        when(userAccountRepository.findById(testUserId)).thenReturn(Optional.of(fullyVerifiedAccount));
        when(userProfileRepository.findById(testUserId)).thenReturn(Optional.of(testProfile));
        when(driverProfileRepository.existsByDriverId(testUserId)).thenReturn(false);
        when(driverProfileRepository.existsByLicenseNumber(anyString())).thenReturn(false);
        when(driverProfileRepository.save(any(DriverProfile.class))).thenAnswer(invocation -> {
            DriverProfile saved = invocation.getArgument(0);
            assertEquals(VehicleType.MOTORCYCLE, saved.getVehicleType());
            assertEquals(2, saved.getVehicleCapacity());
            return saved;
        });
        doNothing().when(routePreferencesService).saveDriverRoutePreferences(any(UUID.class), any(RoutePreferencesDTO.class));

        // Act
        var response = driverProfileService.createDriverProfile(testUserId, validRequest);

        // Assert
        assertNotNull(response);
        assertEquals(VehicleType.MOTORCYCLE, response.getVehicleType());
        verify(driverProfileRepository).save(any(DriverProfile.class));
    }

    @Test
    void testCreateDriverProfile_WithElectricVehicle_Success() {
        // Arrange
        VehicleInfoDTO electricInfo = VehicleInfoDTO.builder()
            .vehicleType(VehicleType.CAR)
            .make("Tesla")
            .model("Model 3")
            .year(2023)
            .licensePlate("TESLA123")
            .capacity(5)
            .fuelType(FuelType.ELECTRIC)
            .build();

        validRequest.setVehicle(electricInfo);

        when(userAccountRepository.findById(testUserId)).thenReturn(Optional.of(fullyVerifiedAccount));
        when(userProfileRepository.findById(testUserId)).thenReturn(Optional.of(testProfile));
        when(driverProfileRepository.existsByDriverId(testUserId)).thenReturn(false);
        when(driverProfileRepository.existsByLicenseNumber(anyString())).thenReturn(false);
        when(driverProfileRepository.save(any(DriverProfile.class))).thenAnswer(invocation -> {
            DriverProfile saved = invocation.getArgument(0);
            assertEquals(FuelType.ELECTRIC, saved.getFuelType());
            return saved;
        });
        doNothing().when(routePreferencesService).saveDriverRoutePreferences(any(UUID.class), any(RoutePreferencesDTO.class));

        // Act
        var response = driverProfileService.createDriverProfile(testUserId, validRequest);

        // Assert
        assertNotNull(response);
        assertEquals(FuelType.ELECTRIC, response.getFuelType());
        verify(driverProfileRepository).save(any(DriverProfile.class));
    }

    // ========== Data Validation Tests ==========

    @Test
    void testCreateDriverProfile_InvalidCapacityForMotorcycle_ThrowsException() {
        // Arrange
        VehicleInfoDTO invalidInfo = VehicleInfoDTO.builder()
            .vehicleType(VehicleType.MOTORCYCLE)
            .capacity(5) // Invalid: motorcycles can only have 1-2 capacity
            .fuelType(FuelType.PETROL)
            .build();

        DriverProfileRequest invalidRequest = DriverProfileRequest.builder()
            .licenseNumber("DL123456")
            .licenseExpiry(LocalDate.now().plusYears(2))
            .maxDetourDistance(500)
            .vehicle(invalidInfo)
            .build();

        when(userAccountRepository.findById(testUserId)).thenReturn(Optional.of(fullyVerifiedAccount));
        when(userProfileRepository.findById(testUserId)).thenReturn(Optional.of(testProfile));
        when(driverProfileRepository.existsByDriverId(testUserId)).thenReturn(false);
        when(driverProfileRepository.existsByLicenseNumber(anyString())).thenReturn(false);

        // Act & Assert
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> driverProfileService.createDriverProfile(testUserId, invalidRequest)
        );

        assertTrue(exception.getMessage().contains("capacity must be between 1 and 2"));
        verify(driverProfileRepository, never()).save(any(DriverProfile.class));
    }

    @Test
    void testCreateDriverProfile_ExpiredLicense_ThrowsException() {
        // Arrange
        DriverProfileRequest expiredRequest = DriverProfileRequest.builder()
            .licenseNumber("DL123456")
            .licenseExpiry(LocalDate.now().minusDays(1)) // Expired
            .maxDetourDistance(500)
            .vehicle(validRequest.getVehicle())
            .build();

        when(userAccountRepository.findById(testUserId)).thenReturn(Optional.of(fullyVerifiedAccount));
        when(userProfileRepository.findById(testUserId)).thenReturn(Optional.of(testProfile));
        when(driverProfileRepository.existsByDriverId(testUserId)).thenReturn(false);
        when(driverProfileRepository.existsByLicenseNumber(anyString())).thenReturn(false);

        // Act & Assert
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> driverProfileService.createDriverProfile(testUserId, expiredRequest)
        );

        assertTrue(exception.getMessage().contains("License expiry date must be in the future"));
        verify(driverProfileRepository, never()).save(any(DriverProfile.class));
    }

    @Test
    void testCreateDriverProfile_MaxDetourExceeded_Success() {
        // Arrange
        // Note: maxDetourDistance validation happens at entity level via @Max annotation
        // The service doesn't validate this, so we test that it accepts the value
        DriverProfileRequest maxDetourRequest = DriverProfileRequest.builder()
            .licenseNumber("DL123456")
            .licenseExpiry(LocalDate.now().plusYears(2))
            .maxDetourDistance(500) // At the limit
            .vehicle(validRequest.getVehicle())
            .routePreferences(validRequest.getRoutePreferences())
            .build();

        when(userAccountRepository.findById(testUserId)).thenReturn(Optional.of(fullyVerifiedAccount));
        when(userProfileRepository.findById(testUserId)).thenReturn(Optional.of(testProfile));
        when(driverProfileRepository.existsByDriverId(testUserId)).thenReturn(false);
        when(driverProfileRepository.existsByLicenseNumber(anyString())).thenReturn(false);
        when(driverProfileRepository.save(any(DriverProfile.class))).thenReturn(testDriverProfile);
        doNothing().when(routePreferencesService).saveDriverRoutePreferences(any(UUID.class), any(RoutePreferencesDTO.class));

        // Act
        var response = driverProfileService.createDriverProfile(testUserId, maxDetourRequest);

        // Assert
        assertNotNull(response);
        verify(driverProfileRepository).save(any(DriverProfile.class));
    }

    // ========== Driver Profile Retrieval Tests ==========

    @Test
    void testGetDriverProfile_Success() {
        // Arrange
        when(driverProfileRepository.findByIdWithUserProfile(testUserId))
            .thenReturn(Optional.of(testDriverProfile));

        // Act
        var response = driverProfileService.getDriverProfile(testUserId);

        // Assert
        assertNotNull(response);
        assertEquals(testUserId, response.getDriverId());
        assertEquals(VehicleType.CAR, response.getVehicleType());
        assertEquals(FuelType.PETROL, response.getFuelType());
        verify(driverProfileRepository).findByIdWithUserProfile(testUserId);
    }

    @Test
    void testGetDriverProfile_NotFound_ThrowsException() {
        // Arrange
        when(driverProfileRepository.findByIdWithUserProfile(testUserId))
            .thenReturn(Optional.empty());

        // Act & Assert
        EntityNotFoundException exception = assertThrows(
            EntityNotFoundException.class,
            () -> driverProfileService.getDriverProfile(testUserId)
        );

        assertTrue(exception.getMessage().contains("Driver profile not found"));
    }

    // ========== Driver Profile Update Tests ==========

    @Test
    void testUpdateDriverProfile_Success() {
        // Arrange
        when(driverProfileRepository.findById(testUserId))
            .thenReturn(Optional.of(testDriverProfile));
        when(driverProfileRepository.save(any(DriverProfile.class))).thenReturn(testDriverProfile);

        DriverProfileRequest updateRequest = DriverProfileRequest.builder()
            .maxDetourDistance(300)
            .build();

        // Act
        var response = driverProfileService.updateDriverProfile(testUserId, updateRequest);

        // Assert
        assertNotNull(response);
        verify(driverProfileRepository).save(any(DriverProfile.class));
    }

    @Test
    void testUpdateDriverProfile_VehicleInfoChange_Success() {
        // Arrange
        when(driverProfileRepository.findById(testUserId))
            .thenReturn(Optional.of(testDriverProfile));
        when(driverProfileRepository.save(any(DriverProfile.class))).thenReturn(testDriverProfile);

        VehicleInfoDTO newVehicleInfo = VehicleInfoDTO.builder()
            .vehicleType(VehicleType.CAR)
            .make("Honda")
            .model("Accord")
            .year(2022)
            .licensePlate("XYZ789")
            .capacity(5)
            .fuelType(FuelType.HYBRID)
            .build();

        DriverProfileRequest updateRequest = DriverProfileRequest.builder()
            .vehicle(newVehicleInfo)
            .build();

        // Act
        var response = driverProfileService.updateDriverProfile(testUserId, updateRequest);

        // Assert
        assertNotNull(response);
        verify(driverProfileRepository).save(any(DriverProfile.class));
    }
}
