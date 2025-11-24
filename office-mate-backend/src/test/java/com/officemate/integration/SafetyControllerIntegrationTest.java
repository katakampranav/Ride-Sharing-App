package com.officemate.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.officemate.modules.auth.entity.UserAccount;
import com.officemate.modules.auth.repository.UserAccountRepository;
import com.officemate.modules.safety.entity.EmergencyContact;
import com.officemate.modules.safety.entity.FamilySharingContact;
import com.officemate.modules.safety.repository.EmergencyContactRepository;
import com.officemate.modules.safety.repository.FamilySharingContactRepository;
import com.officemate.shared.dto.SafetySettingsRequest;
import com.officemate.shared.enums.AccountStatus;
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

import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration tests for safety features endpoints.
 * Tests emergency contacts, family sharing, and safety settings management.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Disabled
class SafetyControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserAccountRepository userAccountRepository;

    @Autowired
    private EmergencyContactRepository emergencyContactRepository;

    @Autowired
    private FamilySharingContactRepository familySharingContactRepository;

    private UUID testUserId;
    private UserAccount testUser;

    @BeforeEach
    void setUp() {
        // Clean up test data
        emergencyContactRepository.deleteAll();
        familySharingContactRepository.deleteAll();
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
    }

    @Test
    @WithMockUser(username = "testUserId", authorities = {"EMAIL_VERIFIED"})
    void testAddEmergencyContact() throws Exception {
        SafetySettingsRequest.EmergencyContactDTO contactDTO = new SafetySettingsRequest.EmergencyContactDTO();
        contactDTO.setName("John Doe");
        contactDTO.setPhoneNumber("+1987654321");
        contactDTO.setRelationship("Brother");
        contactDTO.setPrimary(true);

        mockMvc.perform(post("/users/" + testUserId + "/emergency-contacts")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(contactDTO)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("John Doe"))
                .andExpect(jsonPath("$.phoneNumber").value("+1987654321"))
                .andExpect(jsonPath("$.isPrimary").value(true));
    }

    @Test
    @WithMockUser(username = "testUserId", authorities = {"EMAIL_VERIFIED"})
    void testGetEmergencyContacts() throws Exception {
        // Create test emergency contact
        EmergencyContact contact = EmergencyContact.builder()
                .userId(testUserId)
                .name("Jane Doe")
                .phoneNumber("+1555555555")
                .relationship("Sister")
                .isPrimary(false)
                .build();
        emergencyContactRepository.save(contact);

        mockMvc.perform(get("/users/" + testUserId + "/emergency-contacts"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("Jane Doe"))
                .andExpect(jsonPath("$[0].phoneNumber").value("+1555555555"));
    }

    @Test
    @WithMockUser(username = "testUserId", authorities = {"EMAIL_VERIFIED"})
    void testUpdateEmergencyContact() throws Exception {
        // Create test emergency contact
        EmergencyContact contact = EmergencyContact.builder()
                .userId(testUserId)
                .name("Old Name")
                .phoneNumber("+1111111111")
                .relationship("Friend")
                .isPrimary(false)
                .build();
        contact = emergencyContactRepository.save(contact);

        SafetySettingsRequest.EmergencyContactDTO updateDTO = new SafetySettingsRequest.EmergencyContactDTO();
        updateDTO.setName("Updated Name");
        updateDTO.setPhoneNumber("+1222222222");
        updateDTO.setRelationship("Spouse");
        updateDTO.setPrimary(true);

        mockMvc.perform(put("/users/" + testUserId + "/emergency-contacts/" + contact.getContactId())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updateDTO)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Updated Name"));
    }

    @Test
    @WithMockUser(username = "testUserId", authorities = {"EMAIL_VERIFIED"})
    void testDeleteEmergencyContact() throws Exception {
        // Create test emergency contact
        EmergencyContact contact = EmergencyContact.builder()
                .userId(testUserId)
                .name("To Delete")
                .phoneNumber("+1333333333")
                .relationship("Colleague")
                .isPrimary(false)
                .build();
        contact = emergencyContactRepository.save(contact);

        mockMvc.perform(delete("/users/" + testUserId + "/emergency-contacts/" + contact.getContactId()))
                .andExpect(status().isNoContent());
    }

    @Test
    @WithMockUser(username = "testUserId", authorities = {"EMAIL_VERIFIED"})
    void testAddFamilySharingContact() throws Exception {
        SafetySettingsRequest.FamilySharingContactDTO contactDTO = new SafetySettingsRequest.FamilySharingContactDTO();
        contactDTO.setName("Family Member");
        contactDTO.setPhoneNumber("+1444444444");
        contactDTO.setEmail("family@example.com");
        contactDTO.setReceiveRideUpdates(true);

        mockMvc.perform(post("/users/" + testUserId + "/family-sharing")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(contactDTO)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("Family Member"))
                .andExpect(jsonPath("$.receiveRideUpdates").value(true));
    }

    @Test
    @WithMockUser(username = "testUserId", authorities = {"EMAIL_VERIFIED"})
    void testGetFamilySharingContacts() throws Exception {
        // Create test family sharing contact
        FamilySharingContact contact = new FamilySharingContact();
        contact.setUserId(testUserId);
        contact.setName("Parent");
        contact.setPhoneNumber("+1666666666");
        contact.setEmail("parent@example.com");
        contact.setReceiveRideUpdates(true);
        familySharingContactRepository.save(contact);

        mockMvc.perform(get("/users/" + testUserId + "/family-sharing"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("Parent"))
                .andExpect(jsonPath("$[0].email").value("parent@example.com"));
    }

    @Test
    @WithMockUser(username = "testUserId", authorities = {"EMAIL_VERIFIED"})
    void testGetSafetySettings() throws Exception {
        // Create test contacts
        EmergencyContact emergencyContact = EmergencyContact.builder()
                .userId(testUserId)
                .name("Emergency")
                .phoneNumber("+1777777777")
                .relationship("Parent")
                .isPrimary(true)
                .build();
        emergencyContactRepository.save(emergencyContact);

        FamilySharingContact familyContact = new FamilySharingContact();
        familyContact.setUserId(testUserId);
        familyContact.setName("Family");
        familyContact.setPhoneNumber("+1888888888");
        familyContact.setEmail("family@test.com");
        familyContact.setReceiveRideUpdates(true);
        familySharingContactRepository.save(familyContact);

        mockMvc.perform(get("/users/" + testUserId + "/safety-settings"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.emergencyContacts").isArray())
                .andExpect(jsonPath("$.familySharingContacts").isArray())
                .andExpect(jsonPath("$.hasActiveSOSAlert").exists())
                .andExpect(jsonPath("$.locationSharingActive").exists());
    }

    @Test
    void testAddEmergencyContactWithoutAuthentication() throws Exception {
        SafetySettingsRequest.EmergencyContactDTO contactDTO = new SafetySettingsRequest.EmergencyContactDTO();
        contactDTO.setName("Test");

        mockMvc.perform(post("/users/" + testUserId + "/emergency-contacts")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(contactDTO)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(username = "testUserId", authorities = {"MOBILE_VERIFIED"})
    void testAddEmergencyContactWithoutEmailVerification() throws Exception {
        SafetySettingsRequest.EmergencyContactDTO contactDTO = new SafetySettingsRequest.EmergencyContactDTO();
        contactDTO.setName("Test");

        mockMvc.perform(post("/users/" + testUserId + "/emergency-contacts")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(contactDTO)))
                .andExpect(status().isForbidden());
    }
}
