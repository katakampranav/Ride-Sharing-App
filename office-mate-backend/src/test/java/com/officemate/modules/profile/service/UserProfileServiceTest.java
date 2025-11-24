package com.officemate.modules.profile.service;

import com.officemate.modules.auth.entity.UserAccount;
import com.officemate.modules.auth.repository.UserAccountRepository;
import com.officemate.modules.profile.entity.UserProfile;
import com.officemate.modules.profile.repository.DriverProfileRepository;
import com.officemate.modules.profile.repository.RiderProfileRepository;
import com.officemate.modules.profile.repository.UserProfileRepository;
import com.officemate.modules.wallet.repository.WalletRepository;
import com.officemate.modules.wallet.repository.WalletRepository;
import com.officemate.shared.dto.ProfileResponse;
import com.officemate.shared.enums.AccountStatus;
import com.officemate.shared.exception.ProfileAccessException;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit tests for UserProfileService.
 * Tests profile creation with verification requirements, profile retrieval,
 * updates, and access control validation logic.
 */
@ExtendWith(MockitoExtension.class)
class UserProfileServiceTest {

    @Mock
    private UserProfileRepository userProfileRepository;

    @Mock
    private UserAccountRepository userAccountRepository;

    @Mock
    private DriverProfileRepository driverProfileRepository;

    @Mock
    private RiderProfileRepository riderProfileRepository;

    @Mock
    private WalletRepository walletRepository;

    @InjectMocks
    private UserProfileService userProfileService;

    private UUID testUserId;
    private UserAccount fullyVerifiedAccount;
    private UserAccount mobileOnlyAccount;
    private UserAccount emailOnlyAccount;
    private UserProfile testProfile;

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

        // Email only verified account (edge case)
        emailOnlyAccount = UserAccount.builder()
            .userId(testUserId)
            .phoneNumber("+1234567890")
            .phoneVerified(false)
            .corporateEmail("user@company.com")
            .emailVerified(true)
            .accountStatus(AccountStatus.PENDING_EMAIL)
            .build();

        // Test profile
        testProfile = UserProfile.builder()
            .userId(testUserId)
            .userAccount(fullyVerifiedAccount)
            .firstName("John")
            .lastName("Doe")
            .dateOfBirth(LocalDate.of(1990, 1, 1))
            .gender("MALE")
            .build();
    }

    // ========== Profile Creation Tests ==========

    @Test
    void testCreateBasicProfile_FullyVerified_Success() {
        // Arrange
        when(userAccountRepository.findById(testUserId)).thenReturn(Optional.of(fullyVerifiedAccount));
        when(userProfileRepository.existsByUserId(testUserId)).thenReturn(false);
        when(userProfileRepository.save(any(UserProfile.class))).thenReturn(testProfile);
        when(driverProfileRepository.existsByDriverId(testUserId)).thenReturn(false);
        when(riderProfileRepository.existsByRiderId(testUserId)).thenReturn(false);

        // Act
        ProfileResponse response = userProfileService.createBasicProfile(
            testUserId,
            "John",
            "Doe",
            LocalDate.of(1990, 1, 1),
            "MALE"
        );

        // Assert
        assertNotNull(response);
        assertEquals(testUserId.toString(), response.getUserId());
        assertEquals("John", response.getFirstName());
        assertEquals("Doe", response.getLastName());
        assertTrue(response.isMobileVerified());
        assertTrue(response.isEmailVerified());
        assertTrue(response.isCanAccessRideFeatures());
        verify(userProfileRepository).save(any(UserProfile.class));
    }

    @Test
    void testCreateBasicProfile_MobileOnlyVerified_ThrowsException() {
        // Arrange
        when(userAccountRepository.findById(testUserId)).thenReturn(Optional.of(mobileOnlyAccount));

        // Act & Assert
        ProfileAccessException exception = assertThrows(
            ProfileAccessException.class,
            () -> userProfileService.createBasicProfile(
                testUserId,
                "John",
                "Doe",
                LocalDate.of(1990, 1, 1),
                "MALE"
            )
        );

        assertTrue(exception.getMessage().contains("Both mobile and email verification required"));
        assertTrue(exception.isMobileVerified());
        assertFalse(exception.isEmailVerified());
        verify(userProfileRepository, never()).save(any(UserProfile.class));
    }

    @Test
    void testCreateBasicProfile_EmailOnlyVerified_ThrowsException() {
        // Arrange
        when(userAccountRepository.findById(testUserId)).thenReturn(Optional.of(emailOnlyAccount));

        // Act & Assert
        ProfileAccessException exception = assertThrows(
            ProfileAccessException.class,
            () -> userProfileService.createBasicProfile(
                testUserId,
                "John",
                "Doe",
                LocalDate.of(1990, 1, 1),
                "MALE"
            )
        );

        assertTrue(exception.getMessage().contains("Both mobile and email verification required"));
        assertFalse(exception.isMobileVerified());
        assertTrue(exception.isEmailVerified());
        verify(userProfileRepository, never()).save(any(UserProfile.class));
    }

    @Test
    void testCreateBasicProfile_UserNotFound_ThrowsException() {
        // Arrange
        when(userAccountRepository.findById(testUserId)).thenReturn(Optional.empty());

        // Act & Assert
        EntityNotFoundException exception = assertThrows(
            EntityNotFoundException.class,
            () -> userProfileService.createBasicProfile(
                testUserId,
                "John",
                "Doe",
                LocalDate.of(1990, 1, 1),
                "MALE"
            )
        );

        assertTrue(exception.getMessage().contains("User account not found"));
        verify(userProfileRepository, never()).save(any(UserProfile.class));
    }

    @Test
    void testCreateBasicProfile_ProfileAlreadyExists_ThrowsException() {
        // Arrange
        when(userAccountRepository.findById(testUserId)).thenReturn(Optional.of(fullyVerifiedAccount));
        when(userProfileRepository.existsByUserId(testUserId)).thenReturn(true);

        // Act & Assert
        IllegalStateException exception = assertThrows(
            IllegalStateException.class,
            () -> userProfileService.createBasicProfile(
                testUserId,
                "John",
                "Doe",
                LocalDate.of(1990, 1, 1),
                "MALE"
            )
        );

        assertTrue(exception.getMessage().contains("Profile already exists"));
        verify(userProfileRepository, never()).save(any(UserProfile.class));
    }

    // ========== Profile Retrieval Tests ==========

    @Test
    void testGetProfile_Success() {
        // Arrange
        when(userProfileRepository.findByIdWithUserAccount(testUserId)).thenReturn(Optional.of(testProfile));
        when(driverProfileRepository.existsByDriverId(testUserId)).thenReturn(false);
        when(riderProfileRepository.existsByRiderId(testUserId)).thenReturn(false);

        // Act
        ProfileResponse response = userProfileService.getProfile(testUserId);

        // Assert
        assertNotNull(response);
        assertEquals(testUserId.toString(), response.getUserId());
        assertEquals("John", response.getFirstName());
        assertEquals("Doe", response.getLastName());
        verify(userProfileRepository).findByIdWithUserAccount(testUserId);
    }

    @Test
    void testGetProfile_NotFound_ThrowsException() {
        // Arrange
        when(userProfileRepository.findByIdWithUserAccount(testUserId)).thenReturn(Optional.empty());

        // Act & Assert
        EntityNotFoundException exception = assertThrows(
            EntityNotFoundException.class,
            () -> userProfileService.getProfile(testUserId)
        );

        assertTrue(exception.getMessage().contains("Profile not found"));
    }

    // ========== Profile Update Tests ==========

    @Test
    void testUpdateProfile_Success() {
        // Arrange
        when(userProfileRepository.findByIdWithUserAccount(testUserId)).thenReturn(Optional.of(testProfile));
        when(userProfileRepository.save(any(UserProfile.class))).thenReturn(testProfile);
        when(driverProfileRepository.existsByDriverId(testUserId)).thenReturn(false);
        when(riderProfileRepository.existsByRiderId(testUserId)).thenReturn(false);

        // Act
        ProfileResponse response = userProfileService.updateProfile(
            testUserId,
            "Jane",
            "Smith",
            LocalDate.of(1992, 5, 15),
            "FEMALE",
            "https://example.com/profile.jpg"
        );

        // Assert
        assertNotNull(response);
        verify(userProfileRepository).save(any(UserProfile.class));
        verify(userProfileRepository).findByIdWithUserAccount(testUserId);
    }

    @Test
    void testUpdateProfile_PartialUpdate_Success() {
        // Arrange
        when(userProfileRepository.findByIdWithUserAccount(testUserId)).thenReturn(Optional.of(testProfile));
        when(userProfileRepository.save(any(UserProfile.class))).thenReturn(testProfile);
        when(driverProfileRepository.existsByDriverId(testUserId)).thenReturn(false);
        when(riderProfileRepository.existsByRiderId(testUserId)).thenReturn(false);

        // Act - Only update first name
        ProfileResponse response = userProfileService.updateProfile(
            testUserId,
            "Jane",
            null,
            null,
            null,
            null
        );

        // Assert
        assertNotNull(response);
        verify(userProfileRepository).save(any(UserProfile.class));
    }

    // ========== Access Control Tests ==========

    @Test
    void testCanAccessRideFeatures_FullyVerified_ReturnsTrue() {
        // Arrange
        when(userAccountRepository.findById(testUserId)).thenReturn(Optional.of(fullyVerifiedAccount));

        // Act
        boolean canAccess = userProfileService.canAccessRideFeatures(testUserId);

        // Assert
        assertTrue(canAccess);
        verify(userAccountRepository).findById(testUserId);
    }

    @Test
    void testCanAccessRideFeatures_MobileOnlyVerified_ReturnsFalse() {
        // Arrange
        when(userAccountRepository.findById(testUserId)).thenReturn(Optional.of(mobileOnlyAccount));

        // Act
        boolean canAccess = userProfileService.canAccessRideFeatures(testUserId);

        // Assert
        assertFalse(canAccess);
        verify(userAccountRepository).findById(testUserId);
    }

    @Test
    void testCanAccessRideFeatures_EmailOnlyVerified_ReturnsFalse() {
        // Arrange
        when(userAccountRepository.findById(testUserId)).thenReturn(Optional.of(emailOnlyAccount));

        // Act
        boolean canAccess = userProfileService.canAccessRideFeatures(testUserId);

        // Assert
        assertFalse(canAccess);
        verify(userAccountRepository).findById(testUserId);
    }

    @Test
    void testCanAccessRideFeatures_UserNotFound_ThrowsException() {
        // Arrange
        when(userAccountRepository.findById(testUserId)).thenReturn(Optional.empty());

        // Act & Assert
        EntityNotFoundException exception = assertThrows(
            EntityNotFoundException.class,
            () -> userProfileService.canAccessRideFeatures(testUserId)
        );

        assertTrue(exception.getMessage().contains("User account not found"));
    }

    // ========== Profile Existence Tests ==========

    @Test
    void testProfileExists_ReturnsTrue() {
        // Arrange
        when(userProfileRepository.existsByUserId(testUserId)).thenReturn(true);

        // Act
        boolean exists = userProfileService.profileExists(testUserId);

        // Assert
        assertTrue(exists);
        verify(userProfileRepository).existsByUserId(testUserId);
    }

    @Test
    void testProfileExists_ReturnsFalse() {
        // Arrange
        when(userProfileRepository.existsByUserId(testUserId)).thenReturn(false);

        // Act
        boolean exists = userProfileService.profileExists(testUserId);

        // Assert
        assertFalse(exists);
        verify(userProfileRepository).existsByUserId(testUserId);
    }

    // ========== Profile Deletion Tests ==========

    @Test
    void testDeleteProfile_Success() {
        // Arrange
        when(userProfileRepository.findById(testUserId)).thenReturn(Optional.of(testProfile));

        // Act
        userProfileService.deleteProfile(testUserId);

        // Assert
        verify(userProfileRepository).delete(testProfile);
    }

    @Test
    void testDeleteProfile_NotFound_ThrowsException() {
        // Arrange
        when(userProfileRepository.findById(testUserId)).thenReturn(Optional.empty());

        // Act & Assert
        EntityNotFoundException exception = assertThrows(
            EntityNotFoundException.class,
            () -> userProfileService.deleteProfile(testUserId)
        );

        assertTrue(exception.getMessage().contains("Profile not found"));
        verify(userProfileRepository, never()).delete(any(UserProfile.class));
    }

    // ========== Driver/Rider Capability Tests ==========

    @Test
    void testGetProfile_WithDriverCapability_Success() {
        // Arrange
        when(userProfileRepository.findByIdWithUserAccount(testUserId)).thenReturn(Optional.of(testProfile));
        when(driverProfileRepository.existsByDriverId(testUserId)).thenReturn(true);
        when(riderProfileRepository.existsByRiderId(testUserId)).thenReturn(false);

        // Act
        ProfileResponse response = userProfileService.getProfile(testUserId);

        // Assert
        assertNotNull(response);
        assertTrue(response.isDriver());
        assertFalse(response.isRider());
    }

    @Test
    void testGetProfile_WithRiderCapability_Success() {
        // Arrange
        when(userProfileRepository.findByIdWithUserAccount(testUserId)).thenReturn(Optional.of(testProfile));
        when(driverProfileRepository.existsByDriverId(testUserId)).thenReturn(false);
        when(riderProfileRepository.existsByRiderId(testUserId)).thenReturn(true);

        // Act
        ProfileResponse response = userProfileService.getProfile(testUserId);

        // Assert
        assertNotNull(response);
        assertFalse(response.isDriver());
        assertTrue(response.isRider());
    }

    @Test
    void testGetProfile_WithBothCapabilities_Success() {
        // Arrange
        when(userProfileRepository.findByIdWithUserAccount(testUserId)).thenReturn(Optional.of(testProfile));
        when(driverProfileRepository.existsByDriverId(testUserId)).thenReturn(true);
        when(riderProfileRepository.existsByRiderId(testUserId)).thenReturn(true);

        // Act
        ProfileResponse response = userProfileService.getProfile(testUserId);

        // Assert
        assertNotNull(response);
        assertTrue(response.isDriver());
        assertTrue(response.isRider());
    }
}
