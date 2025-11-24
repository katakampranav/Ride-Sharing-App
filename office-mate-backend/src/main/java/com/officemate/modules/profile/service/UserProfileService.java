package com.officemate.modules.profile.service;

import com.officemate.modules.auth.entity.UserAccount;
import com.officemate.modules.auth.repository.UserAccountRepository;
import com.officemate.modules.profile.entity.UserProfile;
import com.officemate.modules.profile.repository.DriverProfileRepository;
import com.officemate.modules.profile.repository.RiderProfileRepository;
import com.officemate.modules.profile.repository.UserProfileRepository;
import com.officemate.modules.wallet.entity.Wallet;
import com.officemate.modules.wallet.repository.WalletRepository;
import com.officemate.shared.dto.ProfileResponse;
import com.officemate.shared.exception.ProfileAccessException;
import jakarta.persistence.EntityNotFoundException;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

/**
 * Service for managing user profiles.
 * Handles profile creation, retrieval, and updates with verification requirements.
 * Maintains compatibility with existing MongoDB User model during migration.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class UserProfileService {

    private final UserProfileRepository userProfileRepository;
    private final UserAccountRepository userAccountRepository;
    private final DriverProfileRepository driverProfileRepository;
    private final RiderProfileRepository riderProfileRepository;
    private final WalletRepository walletRepository;

    /**
     * Creates a basic user profile.
     * Requires both mobile and email verification before profile creation.
     * 
     * @param userId The user's unique identifier
     * @param firstName User's first name
     * @param lastName User's last name
     * @param dateOfBirth User's date of birth (optional)
     * @param gender User's gender (optional)
     * @return ProfileResponse containing the created profile information
     * @throws ProfileAccessException if user is not fully verified
     * @throws EntityNotFoundException if user account not found
     */
    @Transactional
    public ProfileResponse createBasicProfile(UUID userId, String firstName, String lastName, 
                                             LocalDate dateOfBirth, String gender) {
        log.info("Creating basic profile for user: {}", userId);

        // Fetch user account and verify permissions
        UserAccount userAccount = userAccountRepository.findById(userId)
            .orElseThrow(() -> new EntityNotFoundException("User account not found: " + userId));

        // Check if both mobile and email are verified
        if (!canAccessRideFeatures(userId)) {
            log.warn("Profile creation denied for user {} - verification incomplete. Mobile: {}, Email: {}", 
                userId, userAccount.getPhoneVerified(), userAccount.getEmailVerified());
            throw new ProfileAccessException(
                "Both mobile and email verification required before creating profile",
                userAccount.getPhoneVerified(),
                userAccount.getEmailVerified()
            );
        }

        // Check if profile already exists
        if (userProfileRepository.existsByUserId(userId)) {
            log.warn("Profile already exists for user: {}", userId);
            throw new IllegalStateException("Profile already exists for user: " + userId);
        }

        // Create new profile
        UserProfile profile = UserProfile.builder()
            .userId(userId)
            .userAccount(userAccount)
            .firstName(firstName)
            .lastName(lastName)
            .dateOfBirth(dateOfBirth)
            .gender(gender)
            .build();

        UserProfile savedProfile = userProfileRepository.save(profile);
        log.info("Successfully created profile for user: {}", userId);

        return buildProfileResponse(savedProfile, userAccount);
    }

    /**
     * Retrieves a user profile by user ID.
     * 
     * @param userId The user's unique identifier
     * @return ProfileResponse containing the profile information
     * @throws EntityNotFoundException if profile not found
     */
    @Transactional
    public ProfileResponse getProfile(UUID userId) {
        log.debug("Retrieving profile for user: {}", userId);

        UserProfile profile = userProfileRepository.findByIdWithUserAccount(userId)
            .orElseThrow(() -> new EntityNotFoundException("Profile not found for user: " + userId));

        return buildProfileResponse(profile, profile.getUserAccount());
    }

    /**
     * Updates a user profile.
     * Sensitive changes may require re-authentication (handled by controller layer).
     * 
     * @param userId The user's unique identifier
     * @param firstName Updated first name (optional)
     * @param lastName Updated last name (optional)
     * @param dateOfBirth Updated date of birth (optional)
     * @param gender Updated gender (optional)
     * @param profileImageUrl Updated profile image URL (optional)
     * @return ProfileResponse containing the updated profile information
     * @throws EntityNotFoundException if profile not found
     */
    @Transactional
    public ProfileResponse updateProfile(UUID userId, String firstName, String lastName,
                                        LocalDate dateOfBirth, String gender, String profileImageUrl) {
        log.info("Updating profile for user: {}", userId);

        UserProfile profile = userProfileRepository.findByIdWithUserAccount(userId)
            .orElseThrow(() -> new EntityNotFoundException("Profile not found for user: " + userId));

        // Update fields if provided
        if (firstName != null && !firstName.isBlank()) {
            profile.setFirstName(firstName);
        }
        if (lastName != null && !lastName.isBlank()) {
            profile.setLastName(lastName);
        }
        if (dateOfBirth != null) {
            profile.setDateOfBirth(dateOfBirth);
        }
        if (gender != null) {
            profile.setGender(gender);
        }
        if (profileImageUrl != null) {
            profile.setProfileImageUrl(profileImageUrl);
        }

        UserProfile updatedProfile = userProfileRepository.save(profile);
        log.info("Successfully updated profile for user: {}", userId);

        return buildProfileResponse(updatedProfile, profile.getUserAccount());
    }

    /**
     * Checks if a user can access ride features.
     * Requires both mobile and email verification.
     * 
     * @param userId The user's unique identifier
     * @return true if user has both mobile and email verified
     */
    public boolean canAccessRideFeatures(UUID userId) {
        log.debug("Checking ride feature access for user: {}", userId);

        UserAccount userAccount = userAccountRepository.findById(userId)
            .orElseThrow(() -> new EntityNotFoundException("User account not found: " + userId));

        boolean canAccess = userAccount.isFullyVerified();
        log.debug("User {} ride feature access: {} (mobile: {}, email: {})", 
            userId, canAccess, userAccount.getPhoneVerified(), userAccount.getEmailVerified());

        return canAccess;
    }

    /**
     * Checks if a user profile exists.
     * 
     * @param userId The user's unique identifier
     * @return true if profile exists
     */
    public boolean profileExists(UUID userId) {
        return userProfileRepository.existsByUserId(userId);
    }

    /**
     * Deletes a user profile.
     * This is a soft operation that maintains data integrity.
     * 
     * @param userId The user's unique identifier
     * @throws EntityNotFoundException if profile not found
     */
    @Transactional
    public void deleteProfile(UUID userId) {
        log.info("Deleting profile for user: {}", userId);

        UserProfile profile = userProfileRepository.findById(userId)
            .orElseThrow(() -> new EntityNotFoundException("Profile not found for user: " + userId));

        userProfileRepository.delete(profile);
        log.info("Successfully deleted profile for user: {}", userId);
    }

    /**
     * Builds a ProfileResponse DTO from UserProfile and UserAccount entities.
     * Includes driver and rider capability flags and wallet status.
     * 
     * @param profile The user profile entity
     * @param userAccount The user account entity
     * @return ProfileResponse DTO
     */
    private ProfileResponse buildProfileResponse(UserProfile profile, UserAccount userAccount) {
        boolean isDriver = driverProfileRepository.existsByDriverId(profile.getUserId());
        boolean isRider = riderProfileRepository.existsByRiderId(profile.getUserId());
        boolean canAccess = userAccount.isFullyVerified();

        // Fetch wallet information
        Optional<Wallet> walletOpt = walletRepository.findByUserAccountUserId(profile.getUserId());
        boolean hasWallet = walletOpt.isPresent();
        String walletId = walletOpt.map(w -> w.getWalletId().toString()).orElse(null);
        BigDecimal walletBalance = walletOpt.map(Wallet::getBalance).orElse(null);

        return ProfileResponse.builder()
            .userId(profile.getUserId().toString())
            .firstName(profile.getFirstName())
            .lastName(profile.getLastName())
            .phoneNumber(userAccount.getPhoneNumber())
            .corporateEmail(userAccount.getCorporateEmail())
            .profileImageUrl(profile.getProfileImageUrl())
            .dateOfBirth(profile.getDateOfBirth())
            .gender(profile.getGender())
            .mobileVerified(userAccount.getPhoneVerified())
            .emailVerified(userAccount.getEmailVerified())
            .accountStatus(userAccount.getAccountStatus())
            .isDriver(isDriver)
            .isRider(isRider)
            .canAccessRideFeatures(canAccess)
            .hasWallet(hasWallet)
            .walletId(walletId)
            .walletBalance(walletBalance)
            .createdAt(profile.getCreatedAt())
            .updatedAt(profile.getUpdatedAt())
            .build();
    }
}
