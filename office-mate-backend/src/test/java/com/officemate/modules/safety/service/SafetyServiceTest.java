package com.officemate.modules.safety.service;

import com.officemate.modules.safety.entity.EmergencyContact;
import com.officemate.modules.safety.entity.FamilySharingContact;
import com.officemate.modules.safety.entity.LocationShare;
import com.officemate.modules.safety.entity.SOSAlert;
import com.officemate.modules.safety.entity.SOSAlert.SOSStatus;
import com.officemate.modules.safety.repository.EmergencyContactRepository;
import com.officemate.modules.safety.repository.FamilySharingContactRepository;
import com.officemate.modules.safety.repository.LocationShareRepository;
import com.officemate.modules.safety.repository.SOSAlertRepository;
import com.officemate.shared.dto.SafetySettingsRequest.EmergencyContactDTO;
import com.officemate.shared.dto.SafetySettingsRequest.FamilySharingContactDTO;
import com.officemate.shared.exception.SafetyException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit tests for SafetyService.
 * Tests emergency contact management, family sharing functionality, and SOS system integration.
 */
@ExtendWith(MockitoExtension.class)
class SafetyServiceTest {

    @Mock
    private EmergencyContactRepository emergencyContactRepository;

    @Mock
    private FamilySharingContactRepository familySharingContactRepository;

    @Mock
    private SOSAlertRepository sosAlertRepository;

    @Mock
    private LocationShareRepository locationShareRepository;

    @InjectMocks
    private SafetyService safetyService;

    private UUID testUserId;
    private UUID testContactId;
    private UUID testSharingId;
    private UUID testAlertId;
    private UUID testShareId;
    private EmergencyContact testEmergencyContact;
    private FamilySharingContact testFamilySharingContact;
    private SOSAlert testSOSAlert;
    private LocationShare testLocationShare;

    @BeforeEach
    void setUp() {
        testUserId = UUID.randomUUID();
        testContactId = UUID.randomUUID();
        testSharingId = UUID.randomUUID();
        testAlertId = UUID.randomUUID();
        testShareId = UUID.randomUUID();

        testEmergencyContact = EmergencyContact.builder()
                .contactId(testContactId)
                .userId(testUserId)
                .name("John Doe")
                .phoneNumber("+1234567890")
                .relationship("Spouse")
                .isPrimary(false)
                .build();

        testFamilySharingContact = FamilySharingContact.builder()
                .sharingId(testSharingId)
                .userId(testUserId)
                .name("Jane Doe")
                .phoneNumber("+1234567891")
                .email("jane@example.com")
                .receiveRideUpdates(true)
                .build();

        testSOSAlert = SOSAlert.builder()
                .alertId(testAlertId)
                .userId(testUserId)
                .latitude(37.7749)
                .longitude(-122.4194)
                .status(SOSStatus.ACTIVE)
                .build();

        testLocationShare = LocationShare.builder()
                .shareId(testShareId)
                .userId(testUserId)
                .currentLatitude(37.7749)
                .currentLongitude(-122.4194)
                .shareToken("test-token-123")
                .isActive(true)
                .build();
    }

    // ========== Emergency Contact Management Tests ==========

    @Test
    void testAddEmergencyContact_Success() {
        // Arrange
        EmergencyContactDTO contactDTO = EmergencyContactDTO.builder()
                .name("John Doe")
                .phoneNumber("+1234567890")
                .relationship("Spouse")
                .isPrimary(false)
                .build();

        when(emergencyContactRepository.countByUserId(testUserId)).thenReturn(0L);
        when(emergencyContactRepository.save(any(EmergencyContact.class))).thenReturn(testEmergencyContact);

        // Act
        EmergencyContact result = safetyService.addEmergencyContact(testUserId, contactDTO);

        // Assert
        assertNotNull(result);
        assertEquals(testContactId, result.getContactId());
        assertEquals("John Doe", result.getName());
        verify(emergencyContactRepository).countByUserId(testUserId);
        verify(emergencyContactRepository).save(any(EmergencyContact.class));
    }

    @Test
    void testAddEmergencyContact_MaxLimitReached_ThrowsException() {
        // Arrange
        EmergencyContactDTO contactDTO = EmergencyContactDTO.builder()
                .name("John Doe")
                .phoneNumber("+1234567890")
                .build();

        when(emergencyContactRepository.countByUserId(testUserId)).thenReturn(5L);

        // Act & Assert
        SafetyException exception = assertThrows(
                SafetyException.class,
                () -> safetyService.addEmergencyContact(testUserId, contactDTO)
        );

        assertTrue(exception.getMessage().contains("Maximum number of emergency contacts"));
        verify(emergencyContactRepository, never()).save(any(EmergencyContact.class));
    }

    @Test
    void testAddEmergencyContact_InvalidPhoneNumber_ThrowsException() {
        // Arrange
        EmergencyContactDTO contactDTO = EmergencyContactDTO.builder()
                .name("John Doe")
                .phoneNumber("invalid")
                .build();

        // Act & Assert
        SafetyException exception = assertThrows(
                SafetyException.class,
                () -> safetyService.addEmergencyContact(testUserId, contactDTO)
        );

        assertTrue(exception.getMessage().contains("Invalid phone number format"));
        verify(emergencyContactRepository, never()).save(any(EmergencyContact.class));
    }

    @Test
    void testAddEmergencyContact_EmptyName_ThrowsException() {
        // Arrange
        EmergencyContactDTO contactDTO = EmergencyContactDTO.builder()
                .name("")
                .phoneNumber("+1234567890")
                .build();

        // Act & Assert
        SafetyException exception = assertThrows(
                SafetyException.class,
                () -> safetyService.addEmergencyContact(testUserId, contactDTO)
        );

        assertTrue(exception.getMessage().contains("name is required"));
        verify(emergencyContactRepository, never()).save(any(EmergencyContact.class));
    }

    @Test
    void testAddEmergencyContact_AsPrimary_UnmarksExistingPrimary() {
        // Arrange
        EmergencyContactDTO contactDTO = EmergencyContactDTO.builder()
                .name("John Doe")
                .phoneNumber("+1234567890")
                .isPrimary(true)
                .build();

        when(emergencyContactRepository.countByUserId(testUserId)).thenReturn(1L);
        when(emergencyContactRepository.existsByUserIdAndIsPrimaryTrue(testUserId)).thenReturn(true);
        when(emergencyContactRepository.save(any(EmergencyContact.class))).thenReturn(testEmergencyContact);

        // Act
        safetyService.addEmergencyContact(testUserId, contactDTO);

        // Assert
        verify(emergencyContactRepository).unmarkAllAsPrimaryForUser(testUserId);
        verify(emergencyContactRepository).save(any(EmergencyContact.class));
    }

    @Test
    void testGetEmergencyContacts_Success() {
        // Arrange
        List<EmergencyContact> contacts = Arrays.asList(testEmergencyContact);
        when(emergencyContactRepository.findByUserId(testUserId)).thenReturn(contacts);

        // Act
        List<EmergencyContact> result = safetyService.getEmergencyContacts(testUserId);

        // Assert
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(testContactId, result.get(0).getContactId());
        verify(emergencyContactRepository).findByUserId(testUserId);
    }

    @Test
    void testGetPrimaryEmergencyContact_Success() {
        // Arrange
        testEmergencyContact.markAsPrimary();
        when(emergencyContactRepository.findByUserIdAndIsPrimaryTrue(testUserId))
                .thenReturn(Optional.of(testEmergencyContact));

        // Act
        EmergencyContact result = safetyService.getPrimaryEmergencyContact(testUserId);

        // Assert
        assertNotNull(result);
        assertTrue(result.isPrimaryContact());
        verify(emergencyContactRepository).findByUserIdAndIsPrimaryTrue(testUserId);
    }

    @Test
    void testUpdateEmergencyContact_Success() {
        // Arrange
        EmergencyContactDTO contactDTO = EmergencyContactDTO.builder()
                .name("Updated Name")
                .phoneNumber("+9876543210")
                .relationship("Friend")
                .isPrimary(false)
                .build();

        when(emergencyContactRepository.findByContactIdAndUserId(testContactId, testUserId))
                .thenReturn(Optional.of(testEmergencyContact));
        when(emergencyContactRepository.save(any(EmergencyContact.class))).thenReturn(testEmergencyContact);

        // Act
        EmergencyContact result = safetyService.updateEmergencyContact(testUserId, testContactId, contactDTO);

        // Assert
        assertNotNull(result);
        verify(emergencyContactRepository).save(any(EmergencyContact.class));
    }

    @Test
    void testUpdateEmergencyContact_NotFound_ThrowsException() {
        // Arrange
        EmergencyContactDTO contactDTO = EmergencyContactDTO.builder()
                .name("Updated Name")
                .phoneNumber("+9876543210")
                .build();

        when(emergencyContactRepository.findByContactIdAndUserId(testContactId, testUserId))
                .thenReturn(Optional.empty());

        // Act & Assert
        SafetyException exception = assertThrows(
                SafetyException.class,
                () -> safetyService.updateEmergencyContact(testUserId, testContactId, contactDTO)
        );

        assertTrue(exception.getMessage().contains("not found"));
        verify(emergencyContactRepository, never()).save(any(EmergencyContact.class));
    }

    @Test
    void testDeleteEmergencyContact_Success() {
        // Arrange
        when(emergencyContactRepository.findByContactIdAndUserId(testContactId, testUserId))
                .thenReturn(Optional.of(testEmergencyContact));

        // Act
        safetyService.deleteEmergencyContact(testUserId, testContactId);

        // Assert
        verify(emergencyContactRepository).delete(testEmergencyContact);
    }

    @Test
    void testSetPrimaryEmergencyContact_Success() {
        // Arrange
        when(emergencyContactRepository.findByContactIdAndUserId(testContactId, testUserId))
                .thenReturn(Optional.of(testEmergencyContact));
        when(emergencyContactRepository.existsByUserIdAndIsPrimaryTrue(testUserId)).thenReturn(true);
        when(emergencyContactRepository.save(any(EmergencyContact.class))).thenReturn(testEmergencyContact);

        // Act
        safetyService.setPrimaryEmergencyContact(testUserId, testContactId);

        // Assert
        verify(emergencyContactRepository).unmarkAllAsPrimaryForUser(testUserId);
        verify(emergencyContactRepository).save(any(EmergencyContact.class));
    }

    // ========== Family Sharing Contact Management Tests ==========

    @Test
    void testAddFamilySharingContact_Success() {
        // Arrange
        FamilySharingContactDTO contactDTO = FamilySharingContactDTO.builder()
                .name("Jane Doe")
                .phoneNumber("+1234567891")
                .email("jane@example.com")
                .receiveRideUpdates(true)
                .build();

        when(familySharingContactRepository.countByUserId(testUserId)).thenReturn(0L);
        when(familySharingContactRepository.save(any(FamilySharingContact.class)))
                .thenReturn(testFamilySharingContact);

        // Act
        FamilySharingContact result = safetyService.addFamilySharingContact(testUserId, contactDTO);

        // Assert
        assertNotNull(result);
        assertEquals(testSharingId, result.getSharingId());
        assertEquals("Jane Doe", result.getName());
        verify(familySharingContactRepository).save(any(FamilySharingContact.class));
    }

    @Test
    void testAddFamilySharingContact_MaxLimitReached_ThrowsException() {
        // Arrange
        FamilySharingContactDTO contactDTO = FamilySharingContactDTO.builder()
                .name("Jane Doe")
                .phoneNumber("+1234567891")
                .build();

        when(familySharingContactRepository.countByUserId(testUserId)).thenReturn(10L);

        // Act & Assert
        SafetyException exception = assertThrows(
                SafetyException.class,
                () -> safetyService.addFamilySharingContact(testUserId, contactDTO)
        );

        assertTrue(exception.getMessage().contains("Maximum number of family sharing contacts"));
        verify(familySharingContactRepository, never()).save(any(FamilySharingContact.class));
    }

    @Test
    void testAddFamilySharingContact_NoContactMethod_ThrowsException() {
        // Arrange
        FamilySharingContactDTO contactDTO = FamilySharingContactDTO.builder()
                .name("Jane Doe")
                .build();

        // Act & Assert
        SafetyException exception = assertThrows(
                SafetyException.class,
                () -> safetyService.addFamilySharingContact(testUserId, contactDTO)
        );

        assertTrue(exception.getMessage().contains("At least one contact method"));
        verify(familySharingContactRepository, never()).save(any(FamilySharingContact.class));
    }

    @Test
    void testAddFamilySharingContact_InvalidEmail_ThrowsException() {
        // Arrange
        FamilySharingContactDTO contactDTO = FamilySharingContactDTO.builder()
                .name("Jane Doe")
                .email("invalid-email")
                .build();

        // Act & Assert
        SafetyException exception = assertThrows(
                SafetyException.class,
                () -> safetyService.addFamilySharingContact(testUserId, contactDTO)
        );

        assertTrue(exception.getMessage().contains("Invalid email format"));
        verify(familySharingContactRepository, never()).save(any(FamilySharingContact.class));
    }

    @Test
    void testGetFamilySharingContacts_Success() {
        // Arrange
        List<FamilySharingContact> contacts = Arrays.asList(testFamilySharingContact);
        when(familySharingContactRepository.findByUserId(testUserId)).thenReturn(contacts);

        // Act
        List<FamilySharingContact> result = safetyService.getFamilySharingContacts(testUserId);

        // Assert
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(testSharingId, result.get(0).getSharingId());
        verify(familySharingContactRepository).findByUserId(testUserId);
    }

    @Test
    void testGetFamilySharingContactsWithRideUpdates_Success() {
        // Arrange
        List<FamilySharingContact> contacts = Arrays.asList(testFamilySharingContact);
        when(familySharingContactRepository.findByUserIdAndReceiveRideUpdatesTrue(testUserId))
                .thenReturn(contacts);

        // Act
        List<FamilySharingContact> result = safetyService.getFamilySharingContactsWithRideUpdates(testUserId);

        // Assert
        assertNotNull(result);
        assertEquals(1, result.size());
        assertTrue(result.get(0).shouldReceiveRideUpdates());
        verify(familySharingContactRepository).findByUserIdAndReceiveRideUpdatesTrue(testUserId);
    }

    @Test
    void testUpdateFamilySharingContact_Success() {
        // Arrange
        FamilySharingContactDTO contactDTO = FamilySharingContactDTO.builder()
                .name("Updated Name")
                .phoneNumber("+9876543210")
                .email("updated@example.com")
                .receiveRideUpdates(false)
                .build();

        when(familySharingContactRepository.findBySharingIdAndUserId(testSharingId, testUserId))
                .thenReturn(Optional.of(testFamilySharingContact));
        when(familySharingContactRepository.save(any(FamilySharingContact.class)))
                .thenReturn(testFamilySharingContact);

        // Act
        FamilySharingContact result = safetyService.updateFamilySharingContact(testUserId, testSharingId, contactDTO);

        // Assert
        assertNotNull(result);
        verify(familySharingContactRepository).save(any(FamilySharingContact.class));
    }

    @Test
    void testDeleteFamilySharingContact_Success() {
        // Arrange
        when(familySharingContactRepository.findBySharingIdAndUserId(testSharingId, testUserId))
                .thenReturn(Optional.of(testFamilySharingContact));

        // Act
        safetyService.deleteFamilySharingContact(testUserId, testSharingId);

        // Assert
        verify(familySharingContactRepository).delete(testFamilySharingContact);
    }

    @Test
    void testEnableRideUpdatesForFamilyContact_Success() {
        // Arrange
        testFamilySharingContact.disableRideUpdates();
        when(familySharingContactRepository.findBySharingIdAndUserId(testSharingId, testUserId))
                .thenReturn(Optional.of(testFamilySharingContact));
        when(familySharingContactRepository.save(any(FamilySharingContact.class)))
                .thenReturn(testFamilySharingContact);

        // Act
        safetyService.enableRideUpdatesForFamilyContact(testUserId, testSharingId);

        // Assert
        verify(familySharingContactRepository).save(any(FamilySharingContact.class));
    }

    @Test
    void testDisableRideUpdatesForFamilyContact_Success() {
        // Arrange
        when(familySharingContactRepository.findBySharingIdAndUserId(testSharingId, testUserId))
                .thenReturn(Optional.of(testFamilySharingContact));
        when(familySharingContactRepository.save(any(FamilySharingContact.class)))
                .thenReturn(testFamilySharingContact);

        // Act
        safetyService.disableRideUpdatesForFamilyContact(testUserId, testSharingId);

        // Assert
        verify(familySharingContactRepository).save(any(FamilySharingContact.class));
    }

    // ========== SOS Emergency Alert Management Tests ==========

    @Test
    void testTriggerSOSAlert_Success() {
        // Arrange
        Double latitude = 37.7749;
        Double longitude = -122.4194;
        String message = "Emergency!";

        when(sosAlertRepository.existsByUserIdAndStatus(testUserId, SOSStatus.ACTIVE)).thenReturn(false);
        when(sosAlertRepository.save(any(SOSAlert.class))).thenReturn(testSOSAlert);
        when(emergencyContactRepository.findByUserId(testUserId)).thenReturn(Collections.emptyList());
        when(locationShareRepository.existsByUserIdAndIsActive(testUserId, true)).thenReturn(false);
        when(locationShareRepository.save(any(LocationShare.class))).thenReturn(testLocationShare);

        // Act
        SOSAlert result = safetyService.triggerSOSAlert(testUserId, null, latitude, longitude, message);

        // Assert
        assertNotNull(result);
        assertEquals(testAlertId, result.getAlertId());
        assertTrue(result.isActive());
        verify(sosAlertRepository).save(any(SOSAlert.class));
    }

    @Test
    void testTriggerSOSAlert_InvalidLatitude_ThrowsException() {
        // Arrange
        Double invalidLatitude = 100.0;
        Double longitude = -122.4194;

        // Act & Assert
        SafetyException exception = assertThrows(
                SafetyException.class,
                () -> safetyService.triggerSOSAlert(testUserId, null, invalidLatitude, longitude, null)
        );

        assertTrue(exception.getMessage().contains("Invalid latitude"));
        verify(sosAlertRepository, never()).save(any(SOSAlert.class));
    }

    @Test
    void testTriggerSOSAlert_InvalidLongitude_ThrowsException() {
        // Arrange
        Double latitude = 37.7749;
        Double invalidLongitude = 200.0;

        // Act & Assert
        SafetyException exception = assertThrows(
                SafetyException.class,
                () -> safetyService.triggerSOSAlert(testUserId, null, latitude, invalidLongitude, null)
        );

        assertTrue(exception.getMessage().contains("Invalid longitude"));
        verify(sosAlertRepository, never()).save(any(SOSAlert.class));
    }

    @Test
    void testTriggerSOSAlert_ActiveAlertExists_ThrowsException() {
        // Arrange
        Double latitude = 37.7749;
        Double longitude = -122.4194;

        when(sosAlertRepository.existsByUserIdAndStatus(testUserId, SOSStatus.ACTIVE)).thenReturn(true);

        // Act & Assert
        SafetyException exception = assertThrows(
                SafetyException.class,
                () -> safetyService.triggerSOSAlert(testUserId, null, latitude, longitude, null)
        );

        assertTrue(exception.getMessage().contains("active SOS alert already exists"));
        verify(sosAlertRepository, never()).save(any(SOSAlert.class));
    }

    @Test
    void testGetSOSAlerts_Success() {
        // Arrange
        List<SOSAlert> alerts = Arrays.asList(testSOSAlert);
        when(sosAlertRepository.findByUserId(testUserId)).thenReturn(alerts);

        // Act
        List<SOSAlert> result = safetyService.getSOSAlerts(testUserId);

        // Assert
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(testAlertId, result.get(0).getAlertId());
        verify(sosAlertRepository).findByUserId(testUserId);
    }

    @Test
    void testGetActiveSOSAlert_Success() {
        // Arrange
        when(sosAlertRepository.findFirstByUserIdAndStatusOrderByCreatedAtDesc(testUserId, SOSStatus.ACTIVE))
                .thenReturn(Optional.of(testSOSAlert));

        // Act
        SOSAlert result = safetyService.getActiveSOSAlert(testUserId);

        // Assert
        assertNotNull(result);
        assertTrue(result.isActive());
        verify(sosAlertRepository).findFirstByUserIdAndStatusOrderByCreatedAtDesc(testUserId, SOSStatus.ACTIVE);
    }

    @Test
    void testResolveSOSAlert_Success() {
        // Arrange
        UUID resolvedBy = UUID.randomUUID();
        String notes = "Resolved by support";

        when(sosAlertRepository.findByAlertIdAndUserId(testAlertId, testUserId))
                .thenReturn(Optional.of(testSOSAlert));
        when(sosAlertRepository.save(any(SOSAlert.class))).thenReturn(testSOSAlert);

        // Act
        SOSAlert result = safetyService.resolveSOSAlert(testUserId, testAlertId, resolvedBy, notes);

        // Assert
        assertNotNull(result);
        verify(sosAlertRepository).save(any(SOSAlert.class));
    }

    @Test
    void testResolveSOSAlert_NotActive_ThrowsException() {
        // Arrange
        testSOSAlert.cancel();
        UUID resolvedBy = UUID.randomUUID();

        when(sosAlertRepository.findByAlertIdAndUserId(testAlertId, testUserId))
                .thenReturn(Optional.of(testSOSAlert));

        // Act & Assert
        SafetyException exception = assertThrows(
                SafetyException.class,
                () -> safetyService.resolveSOSAlert(testUserId, testAlertId, resolvedBy, "notes")
        );

        assertTrue(exception.getMessage().contains("not active"));
        verify(sosAlertRepository, never()).save(any(SOSAlert.class));
    }

    @Test
    void testCancelSOSAlert_Success() {
        // Arrange
        when(sosAlertRepository.findByAlertIdAndUserId(testAlertId, testUserId))
                .thenReturn(Optional.of(testSOSAlert));
        when(sosAlertRepository.save(any(SOSAlert.class))).thenReturn(testSOSAlert);

        // Act
        SOSAlert result = safetyService.cancelSOSAlert(testUserId, testAlertId);

        // Assert
        assertNotNull(result);
        verify(sosAlertRepository).save(any(SOSAlert.class));
    }

    // ========== Location Sharing Tests ==========

    @Test
    void testStartLocationSharing_Success() {
        // Arrange
        Double latitude = 37.7749;
        Double longitude = -122.4194;

        when(locationShareRepository.findFirstByUserIdAndIsActiveOrderByCreatedAtDesc(testUserId, true))
                .thenReturn(Optional.empty());
        when(locationShareRepository.save(any(LocationShare.class))).thenReturn(testLocationShare);
        when(familySharingContactRepository.findByUserIdAndReceiveRideUpdatesTrue(testUserId))
                .thenReturn(Collections.emptyList());

        // Act
        LocationShare result = safetyService.startLocationSharing(testUserId, null, latitude, longitude);

        // Assert
        assertNotNull(result);
        assertEquals(testShareId, result.getShareId());
        verify(locationShareRepository).save(any(LocationShare.class));
    }

    @Test
    void testStartLocationSharing_InvalidCoordinates_ThrowsException() {
        // Arrange
        Double invalidLatitude = null;
        Double longitude = -122.4194;

        // Act & Assert
        SafetyException exception = assertThrows(
                SafetyException.class,
                () -> safetyService.startLocationSharing(testUserId, null, invalidLatitude, longitude)
        );

        assertTrue(exception.getMessage().contains("coordinates are required"));
        verify(locationShareRepository, never()).save(any(LocationShare.class));
    }

    @Test
    void testUpdateSharedLocation_Success() {
        // Arrange
        Double newLatitude = 37.8;
        Double newLongitude = -122.5;

        when(locationShareRepository.findByShareIdAndUserId(testShareId, testUserId))
                .thenReturn(Optional.of(testLocationShare));
        when(locationShareRepository.save(any(LocationShare.class))).thenReturn(testLocationShare);

        // Act
        LocationShare result = safetyService.updateSharedLocation(testUserId, testShareId, newLatitude, newLongitude);

        // Assert
        assertNotNull(result);
        verify(locationShareRepository).save(any(LocationShare.class));
    }

    @Test
    void testGetActiveLocationShare_Success() {
        // Arrange
        when(locationShareRepository.findFirstByUserIdAndIsActiveOrderByCreatedAtDesc(testUserId, true))
                .thenReturn(Optional.of(testLocationShare));

        // Act
        LocationShare result = safetyService.getActiveLocationShare(testUserId);

        // Assert
        assertNotNull(result);
        assertEquals(testShareId, result.getShareId());
        verify(locationShareRepository).findFirstByUserIdAndIsActiveOrderByCreatedAtDesc(testUserId, true);
    }

    @Test
    void testEndLocationSharing_Success() {
        // Arrange
        when(locationShareRepository.findByShareIdAndUserId(testShareId, testUserId))
                .thenReturn(Optional.of(testLocationShare));
        when(locationShareRepository.save(any(LocationShare.class))).thenReturn(testLocationShare);

        // Act
        LocationShare result = safetyService.endLocationSharing(testUserId, testShareId);

        // Assert
        assertNotNull(result);
        verify(locationShareRepository).save(any(LocationShare.class));
    }
}
