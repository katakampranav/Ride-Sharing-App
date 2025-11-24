package com.officemate.infrastructure;

import com.officemate.modules.auth.entity.EmailVerification;
import com.officemate.modules.auth.entity.UserAccount;
import com.officemate.modules.auth.repository.EmailVerificationRepository;
import com.officemate.modules.auth.repository.UserAccountRepository;
import com.officemate.modules.profile.entity.DriverProfile;
import com.officemate.modules.profile.entity.RiderProfile;
import com.officemate.modules.profile.entity.UserProfile;
import com.officemate.modules.profile.repository.DriverProfileRepository;
import com.officemate.modules.profile.repository.RiderProfileRepository;
import com.officemate.modules.profile.repository.UserProfileRepository;
import com.officemate.modules.wallet.entity.Wallet;
import com.officemate.modules.wallet.repository.WalletRepository;
import com.officemate.shared.enums.AccountStatus;
import com.officemate.shared.enums.VehicleType;
import com.officemate.shared.enums.FuelType;
import com.officemate.shared.enums.GenderPreference;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Disabled;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests repository operations across all databases.
 * Verifies CRUD operations for all entity types.
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
@Disabled
class RepositoryOperationsTest {

    @Autowired
    private UserAccountRepository userAccountRepository;

    @Autowired
    private UserProfileRepository userProfileRepository;

    @Autowired
    private DriverProfileRepository driverProfileRepository;

    @Autowired
    private RiderProfileRepository riderProfileRepository;

    @Autowired
    private WalletRepository walletRepository;

    @Autowired
    private EmailVerificationRepository emailVerificationRepository;

    @Test
    void userAccountCrudOperations() {
        // Create
        UserAccount account = createTestUserAccount("+1234567890");
        UserAccount saved = userAccountRepository.save(account);
        assertThat(saved.getUserId()).isNotNull();

        // Read
        UserAccount found = userAccountRepository.findById(saved.getUserId()).orElse(null);
        assertThat(found).isNotNull();
        assertThat(found.getPhoneNumber()).isEqualTo("+1234567890");

        // Update
        found.setCorporateEmail("test@company.com");
        found.setEmailVerified(true);
        UserAccount updated = userAccountRepository.save(found);
        assertThat(updated.getCorporateEmail()).isEqualTo("test@company.com");
        assertThat(updated.getEmailVerified()).isTrue();

        // Delete
        userAccountRepository.delete(updated);
        assertThat(userAccountRepository.findById(saved.getUserId())).isEmpty();
    }

    @Test
    void userProfileCrudOperations() {
        // Create user account first
        UserAccount account = createTestUserAccount("+1234567891");
        account = userAccountRepository.save(account);

        // Create profile
        UserProfile profile = UserProfile.builder()
                .userId(account.getUserId())
                .userAccount(account)
                .firstName("John")
                .lastName("Doe")
                .gender("MALE")
                .build();

        UserProfile saved = userProfileRepository.save(profile);
        assertThat(saved.getUserId()).isEqualTo(account.getUserId());

        // Read
        UserProfile found = userProfileRepository.findById(saved.getUserId()).orElse(null);
        assertThat(found).isNotNull();
        assertThat(found.getFirstName()).isEqualTo("John");

        // Update
        found.setLastName("Smith");
        UserProfile updated = userProfileRepository.save(found);
        assertThat(updated.getLastName()).isEqualTo("Smith");
    }

    @Test
    void driverProfileCrudOperations() {
        // Create user account and profile
        UserAccount account = createTestUserAccount("+1234567892");
        account = userAccountRepository.save(account);

        UserProfile profile = createTestUserProfile(account);
        profile = userProfileRepository.save(profile);

        // Create driver profile
        DriverProfile driverProfile = DriverProfile.builder()
                .driverId(profile.getUserId())
                .userProfile(profile)
                .licenseNumber("DL123456")
                .licenseExpiry(LocalDate.now().plusYears(5))
                .licenseVerified(true)
                .maxDetourDistance(500)
                .vehicleType(VehicleType.CAR)
                .vehicleMake("Toyota")
                .vehicleModel("Camry")
                .vehicleYear(2022)
                .licensePlate("ABC123")
                .vehicleCapacity(4)
                .fuelType(FuelType.PETROL)
                .build();

        DriverProfile saved = driverProfileRepository.save(driverProfile);
        assertThat(saved.getDriverId()).isEqualTo(profile.getUserId());

        // Read
        DriverProfile found = driverProfileRepository.findById(saved.getDriverId()).orElse(null);
        assertThat(found).isNotNull();
        assertThat(found.getVehicleType()).isEqualTo(VehicleType.CAR);
        assertThat(found.getFuelType()).isEqualTo(FuelType.PETROL);

        // Update
        found.setVehicleModel("Corolla");
        DriverProfile updated = driverProfileRepository.save(found);
        assertThat(updated.getVehicleModel()).isEqualTo("Corolla");
    }

    @Test
    void riderProfileCrudOperations() {
        // Create user account and profile
        UserAccount account = createTestUserAccount("+1234567893");
        account = userAccountRepository.save(account);

        UserProfile profile = createTestUserProfile(account);
        profile = userProfileRepository.save(profile);

        // Create rider profile
        RiderProfile riderProfile = RiderProfile.builder()
                .riderId(profile.getUserId())
                .userProfile(profile)
                .genderPreference(GenderPreference.NO_PREFERENCE)
                .vehicleTypePreferences(Arrays.asList(VehicleType.CAR.name(), VehicleType.MOTORCYCLE.name()))
                .build();

        RiderProfile saved = riderProfileRepository.save(riderProfile);
        assertThat(saved.getRiderId()).isEqualTo(profile.getUserId());

        // Read
        RiderProfile found = riderProfileRepository.findById(saved.getRiderId()).orElse(null);
        assertThat(found).isNotNull();
        assertThat(found.getGenderPreference()).isEqualTo(GenderPreference.NO_PREFERENCE);
        assertThat(found.getVehicleTypePreferences()).containsExactly(VehicleType.CAR.name(), VehicleType.MOTORCYCLE.name());

        // Update
        found.setGenderPreference(GenderPreference.FEMALE_ONLY);
        RiderProfile updated = riderProfileRepository.save(found);
        assertThat(updated.getGenderPreference()).isEqualTo(GenderPreference.FEMALE_ONLY);
    }

    @Test
    void walletCrudOperations() {
        // Create user account
        UserAccount account = createTestUserAccount("+1234567894");
        account = userAccountRepository.save(account);

        // Create wallet
        Wallet wallet = Wallet.builder()
                .userAccount(account)
                .balance(BigDecimal.valueOf(100.00))
                .autoReloadEnabled(true)
                .autoReloadThreshold(BigDecimal.valueOf(50.00))
                .autoReloadAmount(BigDecimal.valueOf(100.00))
                .bankLinked(false)
                .build();

        Wallet saved = walletRepository.save(wallet);
        assertThat(saved.getWalletId()).isNotNull();

        // Read
        Wallet found = walletRepository.findByUserAccountUserId(account.getUserId()).orElse(null);
        assertThat(found).isNotNull();
        assertThat(found.getBalance()).isEqualByComparingTo(BigDecimal.valueOf(100.00));

        // Update
        found.setBalance(BigDecimal.valueOf(150.00));
        Wallet updated = walletRepository.save(found);
        assertThat(updated.getBalance()).isEqualByComparingTo(BigDecimal.valueOf(150.00));
    }

    @Test
    void emailVerificationCrudOperations() {
        // Create user account
        UserAccount account = createTestUserAccount("+1234567895");
        account = userAccountRepository.save(account);

        // Create email verification
        EmailVerification verification = EmailVerification.builder()
                .userId(account.getUserId())
                .corporateEmail("test@company.com")
                .otpHash("hashed-otp")
                .attempts(0)
                .verified(false)
                .expiresAt(LocalDateTime.now().plusMinutes(10))
                .build();

        EmailVerification saved = emailVerificationRepository.save(verification);
        assertThat(saved.getVerificationId()).isNotNull();

        // Read
        EmailVerification found = emailVerificationRepository.findById(saved.getVerificationId()).orElse(null);
        assertThat(found).isNotNull();
        assertThat(found.getCorporateEmail()).isEqualTo("test@company.com");

        // Update
        found.setVerified(true);
        EmailVerification updated = emailVerificationRepository.save(found);
        assertThat(updated.getVerified()).isTrue();
    }

    private UserAccount createTestUserAccount(String phoneNumber) {
        return UserAccount.builder()
                .phoneNumber(phoneNumber)
                .phoneVerified(true)
                .accountStatus(AccountStatus.ACTIVE)
                .build();
    }

    private UserProfile createTestUserProfile(UserAccount account) {
        return UserProfile.builder()
                .userId(account.getUserId())
                .userAccount(account)
                .firstName("Test")
                .lastName("User")
                .gender("MALE")
                .build();
    }
}
