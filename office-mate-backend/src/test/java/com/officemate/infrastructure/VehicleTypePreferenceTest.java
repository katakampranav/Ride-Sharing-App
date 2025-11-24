package com.officemate.infrastructure;

import com.officemate.modules.auth.entity.UserAccount;
import com.officemate.modules.auth.repository.UserAccountRepository;
import com.officemate.modules.profile.entity.DriverProfile;
import com.officemate.modules.profile.entity.RiderProfile;
import com.officemate.modules.profile.entity.UserProfile;
import com.officemate.modules.profile.repository.DriverProfileRepository;
import com.officemate.modules.profile.repository.RiderProfileRepository;
import com.officemate.modules.profile.repository.UserProfileRepository;
import com.officemate.shared.enums.AccountStatus;
import com.officemate.shared.enums.FuelType;
import com.officemate.shared.enums.GenderPreference;
import com.officemate.shared.enums.VehicleType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Disabled;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests vehicle type and preference management.
 * Verifies that vehicle types, fuel types, and gender preferences work correctly.
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
@Disabled
class VehicleTypePreferenceTest {

    @Autowired
    private UserAccountRepository userAccountRepository;

    @Autowired
    private UserProfileRepository userProfileRepository;

    @Autowired
    private DriverProfileRepository driverProfileRepository;

    @Autowired
    private RiderProfileRepository riderProfileRepository;

    @Test
    void allVehicleTypesAreSupported() {
        for (VehicleType vehicleType : VehicleType.values()) {
            UserAccount account = createTestUserAccount("+123456789" + vehicleType.ordinal());
            account = userAccountRepository.save(account);

            UserProfile profile = createTestUserProfile(account);
            profile = userProfileRepository.save(profile);

            DriverProfile driverProfile = createTestDriverProfile(profile, vehicleType, FuelType.PETROL);
            DriverProfile saved = driverProfileRepository.save(driverProfile);

            assertThat(saved.getVehicleType()).isEqualTo(vehicleType);
        }
    }

    @Test
    void allFuelTypesAreSupported() {
        for (FuelType fuelType : FuelType.values()) {
            UserAccount account = createTestUserAccount("+223456789" + fuelType.ordinal());
            account = userAccountRepository.save(account);

            UserProfile profile = createTestUserProfile(account);
            profile = userProfileRepository.save(profile);

            DriverProfile driverProfile = createTestDriverProfile(profile, VehicleType.CAR, fuelType);
            DriverProfile saved = driverProfileRepository.save(driverProfile);

            assertThat(saved.getFuelType()).isEqualTo(fuelType);
        }
    }

    @Test
    void vehicleCapacityValidationForCars() {
        UserAccount account = createTestUserAccount("+3234567890");
        account = userAccountRepository.save(account);

        UserProfile profile = createTestUserProfile(account);
        profile = userProfileRepository.save(profile);

        DriverProfile driverProfile = createTestDriverProfile(profile, VehicleType.CAR, FuelType.PETROL);
        driverProfile.setVehicleCapacity(4);
        DriverProfile saved = driverProfileRepository.save(driverProfile);

        assertThat(saved.getVehicleCapacity()).isEqualTo(4);
        assertThat(saved.getVehicleCapacity()).isBetween(1, 7);
    }

    @Test
    void vehicleCapacityValidationForMotorcycles() {
        UserAccount account = createTestUserAccount("+4234567890");
        account = userAccountRepository.save(account);

        UserProfile profile = createTestUserProfile(account);
        profile = userProfileRepository.save(profile);

        DriverProfile driverProfile = createTestDriverProfile(profile, VehicleType.MOTORCYCLE, FuelType.PETROL);
        driverProfile.setVehicleCapacity(2);
        DriverProfile saved = driverProfileRepository.save(driverProfile);

        assertThat(saved.getVehicleCapacity()).isEqualTo(2);
        assertThat(saved.getVehicleCapacity()).isBetween(1, 2);
    }

    @Test
    void riderVehicleTypePreferencesAreStored() {
        UserAccount account = createTestUserAccount("+5234567890");
        account = userAccountRepository.save(account);

        UserProfile profile = createTestUserProfile(account);
        profile = userProfileRepository.save(profile);

        List<VehicleType> preferences = Arrays.asList(VehicleType.CAR, VehicleType.MOTORCYCLE, VehicleType.SCOOTER);
        RiderProfile riderProfile = createTestRiderProfile(profile, preferences);
        RiderProfile saved = riderProfileRepository.save(riderProfile);

        List<String> expectedPreferences = preferences.stream().map(VehicleType::name).toList();
        assertThat(saved.getVehicleTypePreferences()).containsExactlyElementsOf(expectedPreferences);
    }

    @Test
    void allGenderPreferencesAreSupported() {
        int counter = 0;
        for (GenderPreference genderPreference : GenderPreference.values()) {
            UserAccount account = createTestUserAccount("+623456789" + counter++);
            account = userAccountRepository.save(account);

            UserProfile profile = createTestUserProfile(account);
            profile = userProfileRepository.save(profile);

            RiderProfile riderProfile = createTestRiderProfile(profile, Arrays.asList(VehicleType.CAR));
            riderProfile.setGenderPreference(genderPreference);
            RiderProfile saved = riderProfileRepository.save(riderProfile);

            assertThat(saved.getGenderPreference()).isEqualTo(genderPreference);
        }
    }

    @Test
    void driverProfileWithElectricVehicle() {
        UserAccount account = createTestUserAccount("+7234567890");
        account = userAccountRepository.save(account);

        UserProfile profile = createTestUserProfile(account);
        profile = userProfileRepository.save(profile);

        DriverProfile driverProfile = createTestDriverProfile(profile, VehicleType.CAR, FuelType.ELECTRIC);
        driverProfile.setVehicleMake("Tesla");
        driverProfile.setVehicleModel("Model 3");
        DriverProfile saved = driverProfileRepository.save(driverProfile);

        assertThat(saved.getFuelType()).isEqualTo(FuelType.ELECTRIC);
        assertThat(saved.getVehicleMake()).isEqualTo("Tesla");
    }

    @Test
    void driverProfileWithBicycle() {
        UserAccount account = createTestUserAccount("+8234567890");
        account = userAccountRepository.save(account);

        UserProfile profile = createTestUserProfile(account);
        profile = userProfileRepository.save(profile);

        DriverProfile driverProfile = createTestDriverProfile(profile, VehicleType.BICYCLE, null);
        driverProfile.setVehicleCapacity(1);
        DriverProfile saved = driverProfileRepository.save(driverProfile);

        assertThat(saved.getVehicleType()).isEqualTo(VehicleType.BICYCLE);
        assertThat(saved.getVehicleCapacity()).isEqualTo(1);
    }

    @Test
    void maxDetourDistanceIsConfigurable() {
        UserAccount account = createTestUserAccount("+9234567890");
        account = userAccountRepository.save(account);

        UserProfile profile = createTestUserProfile(account);
        profile = userProfileRepository.save(profile);

        DriverProfile driverProfile = createTestDriverProfile(profile, VehicleType.CAR, FuelType.PETROL);
        driverProfile.setMaxDetourDistance(300);
        DriverProfile saved = driverProfileRepository.save(driverProfile);

        assertThat(saved.getMaxDetourDistance()).isEqualTo(300);
        assertThat(saved.getMaxDetourDistance()).isLessThanOrEqualTo(500);
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

    private DriverProfile createTestDriverProfile(UserProfile profile, VehicleType vehicleType, FuelType fuelType) {
        return DriverProfile.builder()
                .driverId(profile.getUserId())
                .userProfile(profile)
                .licenseNumber("DL" + System.currentTimeMillis())
                .licenseExpiry(LocalDate.now().plusYears(5))
                .licenseVerified(true)
                .maxDetourDistance(500)
                .vehicleType(vehicleType)
                .vehicleMake("TestMake")
                .vehicleModel("TestModel")
                .vehicleYear(2022)
                .licensePlate("TEST" + System.currentTimeMillis() % 1000)
                .vehicleCapacity(4)
                .fuelType(fuelType)
                .build();
    }

    private RiderProfile createTestRiderProfile(UserProfile profile, List<VehicleType> vehicleTypePreferences) {
        List<String> vehicleTypeStrings = vehicleTypePreferences.stream()
                .map(VehicleType::name)
                .toList();
        
        return RiderProfile.builder()
                .riderId(profile.getUserId())
                .userProfile(profile)
                .genderPreference(GenderPreference.NO_PREFERENCE)
                .vehicleTypePreferences(vehicleTypeStrings)
                .build();
    }
}
