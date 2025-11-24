package com.officemate.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.officemate.modules.auth.entity.UserAccount;
import com.officemate.modules.auth.repository.UserAccountRepository;
import com.officemate.modules.profile.entity.UserProfile;
import com.officemate.modules.profile.repository.UserProfileRepository;
import com.officemate.shared.dto.*;
import com.officemate.shared.enums.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration tests for profile management endpoints.
 * Tests profile creation, updates, and driver/rider profile management.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Disabled
class ProfileControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserAccountRepository userAccountRepository;

    @Autowired
    private UserProfileRepository userProfileRepository;

    private UUID testUserId;
    private UserAccount testUser;

    @BeforeEach
    void setUp() {
        // Clean up test data
        userProfileRepository.deleteAll();
        userAccountRepository.deleteAll();

        // Create test user with full verification
        testUser = new UserAccount();
        testUser.setPhoneNumber("+1234567890");
        testUser.setPhoneVerified(true);
        testUser.setCorporateEmail("test@company.com");
        testUser.setEmailVerified(true);
        testUser.setAccountStatus(AccountStatus.ACTIVE);
        testUser = userAccountRepository.save(testUser);
        testUserId = testUser.getUserId();

        // Create basic profile
        UserProfile profile = new UserProfile();
        profile.setUserId(testUserId);
        profile.setFirstName("Test");
        profile.setLastName("User");
        userProfileRepository.save(profile);
    }

    @Test
    @WithMockUser(username = "testUserId", authorities = {"MOBILE_VERIFIED", "EMAIL_VERIFIED"})
    void testGetProfile() throws Exception {
        mockMvc.perform(get("/users/" + testUserId + "/profile"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.firstName").value("Test"))
                .andExpect(jsonPath("$.lastName").value("User"));
    }

    @Test
    @WithMockUser(username = "testUserId", authorities = {"MOBILE_VERIFIED"})
    void testUpdateProfile() throws Exception {
        ProfileUpdateRequest updateRequest = new ProfileUpdateRequest();
        updateRequest.setFirstName("Updated");
        updateRequest.setLastName("Name");
        updateRequest.setDateOfBirth(LocalDate.of(1990, 1, 1));

        mockMvc.perform(put("/users/" + testUserId + "/profile")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.firstName").value("Updated"))
                .andExpect(jsonPath("$.lastName").value("Name"));
    }

    @Test
    @WithMockUser(username = "testUserId", authorities = {"EMAIL_VERIFIED"})
    void testCreateDriverProfile() throws Exception {
        DriverProfileRequest driverRequest = new DriverProfileRequest();
        driverRequest.setLicenseNumber("DL123456");
        driverRequest.setLicenseExpiry(LocalDate.now().plusYears(5));
        driverRequest.setMaxDetourDistance(500);
        
        VehicleInfoDTO vehicleInfo = new VehicleInfoDTO();
        vehicleInfo.setVehicleType(VehicleType.CAR);
        vehicleInfo.setMake("Toyota");
        vehicleInfo.setModel("Camry");
        vehicleInfo.setYear(2020);
        vehicleInfo.setLicensePlate("ABC123");
        vehicleInfo.setCapacity(4);
        vehicleInfo.setFuelType(FuelType.PETROL);
        driverRequest.setVehicle(vehicleInfo);

        mockMvc.perform(post("/users/" + testUserId + "/profile/driver-profile")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(driverRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.licenseNumber").value("DL123456"))
                .andExpect(jsonPath("$.vehicleType").value("CAR"))
                .andExpect(jsonPath("$.fuelType").value("PETROL"));
    }

    @Test
    @WithMockUser(username = "testUserId", authorities = {"EMAIL_VERIFIED"})
    void testCreateRiderProfile() throws Exception {
        RiderProfileRequest riderRequest = new RiderProfileRequest();
        riderRequest.setGenderPreference(GenderPreference.NO_PREFERENCE);
        riderRequest.setVehicleTypePreferences(Arrays.asList(VehicleType.CAR, VehicleType.MOTORCYCLE));

        mockMvc.perform(post("/users/" + testUserId + "/profile/rider-profile")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(riderRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.genderPreference").value("NO_PREFERENCE"));
    }

    @Test
    void testGetProfileWithoutAuthentication() throws Exception {
        mockMvc.perform(get("/users/" + testUserId + "/profile"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(username = "testUserId", authorities = {"MOBILE_VERIFIED"})
    void testCreateDriverProfileWithoutEmailVerification() throws Exception {
        DriverProfileRequest driverRequest = new DriverProfileRequest();
        driverRequest.setLicenseNumber("DL123456");

        mockMvc.perform(post("/users/" + testUserId + "/profile/driver-profile")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(driverRequest)))
                .andExpect(status().isForbidden());
    }
}
