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
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.regex.Pattern;

/**
 * Service for managing safety features including emergency contacts,
 * family sharing, and SOS functionality.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class SafetyService {

    private final EmergencyContactRepository emergencyContactRepository;
    private final FamilySharingContactRepository familySharingContactRepository;
    private final SOSAlertRepository sosAlertRepository;
    private final LocationShareRepository locationShareRepository;

    // Phone number validation pattern (E.164 format)
    private static final Pattern PHONE_PATTERN = Pattern.compile("^\\+?[1-9]\\d{1,14}$");
    
    // Email validation pattern
    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");
    
    // Maximum number of emergency contacts per user
    private static final int MAX_EMERGENCY_CONTACTS = 5;
    
    // Maximum number of family sharing contacts per user
    private static final int MAX_FAMILY_SHARING_CONTACTS = 10;

    /**
     * Add a new emergency contact for a user
     * 
     * @param userId the user's ID
     * @param contactDTO the emergency contact data
     * @return the created emergency contact
     * @throws SafetyException if validation fails or limits are exceeded
     */
    @Transactional
    public EmergencyContact addEmergencyContact(UUID userId, EmergencyContactDTO contactDTO) {
        log.info("Adding emergency contact for user: {}", userId);
        
        // Validate input
        validateEmergencyContactData(contactDTO);
        
        // Check if user has reached the maximum number of contacts
        long contactCount = emergencyContactRepository.countByUserId(userId);
        if (contactCount >= MAX_EMERGENCY_CONTACTS) {
            throw new SafetyException(
                String.format("Maximum number of emergency contacts (%d) reached", MAX_EMERGENCY_CONTACTS)
            );
        }
        
        // If this contact is marked as primary, unmark any existing primary contact
        if (contactDTO.isPrimary()) {
            handlePrimaryContactDesignation(userId);
        }
        
        // Create and save the emergency contact
        EmergencyContact contact = EmergencyContact.builder()
                .userId(userId)
                .name(contactDTO.getName())
                .phoneNumber(contactDTO.getPhoneNumber())
                .relationship(contactDTO.getRelationship())
                .isPrimary(contactDTO.isPrimary())
                .build();
        
        EmergencyContact savedContact = emergencyContactRepository.save(contact);
        log.info("Emergency contact created with ID: {} for user: {}", savedContact.getContactId(), userId);
        
        return savedContact;
    }

    /**
     * Get all emergency contacts for a user
     * 
     * @param userId the user's ID
     * @return list of emergency contacts
     */
    @Transactional(readOnly = true)
    public List<EmergencyContact> getEmergencyContacts(UUID userId) {
        log.debug("Retrieving emergency contacts for user: {}", userId);
        return emergencyContactRepository.findByUserId(userId);
    }

    /**
     * Get the primary emergency contact for a user
     * 
     * @param userId the user's ID
     * @return the primary emergency contact, or null if none exists
     */
    @Transactional(readOnly = true)
    public EmergencyContact getPrimaryEmergencyContact(UUID userId) {
        log.debug("Retrieving primary emergency contact for user: {}", userId);
        return emergencyContactRepository.findByUserIdAndIsPrimaryTrue(userId)
                .orElse(null);
    }

    /**
     * Update an existing emergency contact
     * 
     * @param userId the user's ID
     * @param contactId the contact's ID
     * @param contactDTO the updated contact data
     * @return the updated emergency contact
     * @throws SafetyException if contact not found or validation fails
     */
    @Transactional
    public EmergencyContact updateEmergencyContact(UUID userId, UUID contactId, EmergencyContactDTO contactDTO) {
        log.info("Updating emergency contact {} for user: {}", contactId, userId);
        
        // Validate input
        validateEmergencyContactData(contactDTO);
        
        // Find the contact and verify ownership
        EmergencyContact contact = emergencyContactRepository.findByContactIdAndUserId(contactId, userId)
                .orElseThrow(() -> new SafetyException("Emergency contact not found"));
        
        // If changing to primary, unmark any existing primary contact
        if (contactDTO.isPrimary() && !contact.isPrimaryContact()) {
            handlePrimaryContactDesignation(userId);
        }
        
        // Update contact details
        contact.setName(contactDTO.getName());
        contact.setPhoneNumber(contactDTO.getPhoneNumber());
        contact.setRelationship(contactDTO.getRelationship());
        contact.setIsPrimary(contactDTO.isPrimary());
        
        EmergencyContact updatedContact = emergencyContactRepository.save(contact);
        log.info("Emergency contact {} updated for user: {}", contactId, userId);
        
        return updatedContact;
    }

    /**
     * Delete an emergency contact
     * 
     * @param userId the user's ID
     * @param contactId the contact's ID
     * @throws SafetyException if contact not found
     */
    @Transactional
    public void deleteEmergencyContact(UUID userId, UUID contactId) {
        log.info("Deleting emergency contact {} for user: {}", contactId, userId);
        
        // Find the contact and verify ownership
        EmergencyContact contact = emergencyContactRepository.findByContactIdAndUserId(contactId, userId)
                .orElseThrow(() -> new SafetyException("Emergency contact not found"));
        
        emergencyContactRepository.delete(contact);
        log.info("Emergency contact {} deleted for user: {}", contactId, userId);
    }

    /**
     * Set a contact as the primary emergency contact
     * 
     * @param userId the user's ID
     * @param contactId the contact's ID to set as primary
     * @throws SafetyException if contact not found
     */
    @Transactional
    public void setPrimaryEmergencyContact(UUID userId, UUID contactId) {
        log.info("Setting emergency contact {} as primary for user: {}", contactId, userId);
        
        // Find the contact and verify ownership
        EmergencyContact contact = emergencyContactRepository.findByContactIdAndUserId(contactId, userId)
                .orElseThrow(() -> new SafetyException("Emergency contact not found"));
        
        // Unmark all existing primary contacts
        handlePrimaryContactDesignation(userId);
        
        // Mark this contact as primary
        contact.markAsPrimary();
        emergencyContactRepository.save(contact);
        
        log.info("Emergency contact {} set as primary for user: {}", contactId, userId);
    }

    /**
     * Delete all emergency contacts for a user
     * 
     * @param userId the user's ID
     */
    @Transactional
    public void deleteAllEmergencyContacts(UUID userId) {
        log.info("Deleting all emergency contacts for user: {}", userId);
        emergencyContactRepository.deleteByUserId(userId);
        log.info("All emergency contacts deleted for user: {}", userId);
    }

    /**
     * Validate emergency contact data
     * 
     * @param contactDTO the contact data to validate
     * @throws SafetyException if validation fails
     */
    private void validateEmergencyContactData(EmergencyContactDTO contactDTO) {
        if (contactDTO.getName() == null || contactDTO.getName().trim().isEmpty()) {
            throw new SafetyException("Emergency contact name is required");
        }
        
        if (contactDTO.getPhoneNumber() == null || contactDTO.getPhoneNumber().trim().isEmpty()) {
            throw new SafetyException("Emergency contact phone number is required");
        }
        
        if (!PHONE_PATTERN.matcher(contactDTO.getPhoneNumber()).matches()) {
            throw new SafetyException("Invalid phone number format");
        }
    }

    /**
     * Handle primary contact designation by unmarking all existing primary contacts
     * 
     * @param userId the user's ID
     */
    private void handlePrimaryContactDesignation(UUID userId) {
        if (emergencyContactRepository.existsByUserIdAndIsPrimaryTrue(userId)) {
            log.debug("Unmarking existing primary contact for user: {}", userId);
            emergencyContactRepository.unmarkAllAsPrimaryForUser(userId);
        }
    }

    // ==================== Family Sharing Contact Management ====================

    /**
     * Add a new family sharing contact for a user
     * 
     * @param userId the user's ID
     * @param contactDTO the family sharing contact data
     * @return the created family sharing contact
     * @throws SafetyException if validation fails or limits are exceeded
     */
    @Transactional
    public FamilySharingContact addFamilySharingContact(UUID userId, FamilySharingContactDTO contactDTO) {
        log.info("Adding family sharing contact for user: {}", userId);
        
        // Validate input
        validateFamilySharingContactData(contactDTO);
        
        // Check if user has reached the maximum number of family sharing contacts
        long contactCount = familySharingContactRepository.countByUserId(userId);
        if (contactCount >= MAX_FAMILY_SHARING_CONTACTS) {
            throw new SafetyException(
                String.format("Maximum number of family sharing contacts (%d) reached", MAX_FAMILY_SHARING_CONTACTS)
            );
        }
        
        // Create and save the family sharing contact
        FamilySharingContact contact = FamilySharingContact.builder()
                .userId(userId)
                .name(contactDTO.getName())
                .phoneNumber(contactDTO.getPhoneNumber())
                .email(contactDTO.getEmail())
                .receiveRideUpdates(contactDTO.isReceiveRideUpdates())
                .build();
        
        FamilySharingContact savedContact = familySharingContactRepository.save(contact);
        log.info("Family sharing contact created with ID: {} for user: {}", savedContact.getSharingId(), userId);
        
        return savedContact;
    }

    /**
     * Get all family sharing contacts for a user
     * 
     * @param userId the user's ID
     * @return list of family sharing contacts
     */
    @Transactional(readOnly = true)
    public List<FamilySharingContact> getFamilySharingContacts(UUID userId) {
        log.debug("Retrieving family sharing contacts for user: {}", userId);
        return familySharingContactRepository.findByUserId(userId);
    }

    /**
     * Get all family sharing contacts that have ride updates enabled for a user
     * 
     * @param userId the user's ID
     * @return list of family sharing contacts with ride updates enabled
     */
    @Transactional(readOnly = true)
    public List<FamilySharingContact> getFamilySharingContactsWithRideUpdates(UUID userId) {
        log.debug("Retrieving family sharing contacts with ride updates enabled for user: {}", userId);
        return familySharingContactRepository.findByUserIdAndReceiveRideUpdatesTrue(userId);
    }

    /**
     * Update an existing family sharing contact
     * 
     * @param userId the user's ID
     * @param sharingId the sharing contact's ID
     * @param contactDTO the updated contact data
     * @return the updated family sharing contact
     * @throws SafetyException if contact not found or validation fails
     */
    @Transactional
    public FamilySharingContact updateFamilySharingContact(UUID userId, UUID sharingId, FamilySharingContactDTO contactDTO) {
        log.info("Updating family sharing contact {} for user: {}", sharingId, userId);
        
        // Validate input
        validateFamilySharingContactData(contactDTO);
        
        // Find the contact and verify ownership
        FamilySharingContact contact = familySharingContactRepository.findBySharingIdAndUserId(sharingId, userId)
                .orElseThrow(() -> new SafetyException("Family sharing contact not found"));
        
        // Update contact details
        contact.setName(contactDTO.getName());
        contact.setPhoneNumber(contactDTO.getPhoneNumber());
        contact.setEmail(contactDTO.getEmail());
        contact.setReceiveRideUpdates(contactDTO.isReceiveRideUpdates());
        
        FamilySharingContact updatedContact = familySharingContactRepository.save(contact);
        log.info("Family sharing contact {} updated for user: {}", sharingId, userId);
        
        return updatedContact;
    }

    /**
     * Delete a family sharing contact
     * 
     * @param userId the user's ID
     * @param sharingId the sharing contact's ID
     * @throws SafetyException if contact not found
     */
    @Transactional
    public void deleteFamilySharingContact(UUID userId, UUID sharingId) {
        log.info("Deleting family sharing contact {} for user: {}", sharingId, userId);
        
        // Find the contact and verify ownership
        FamilySharingContact contact = familySharingContactRepository.findBySharingIdAndUserId(sharingId, userId)
                .orElseThrow(() -> new SafetyException("Family sharing contact not found"));
        
        familySharingContactRepository.delete(contact);
        log.info("Family sharing contact {} deleted for user: {}", sharingId, userId);
    }

    /**
     * Enable ride updates for a family sharing contact
     * 
     * @param userId the user's ID
     * @param sharingId the sharing contact's ID
     * @throws SafetyException if contact not found
     */
    @Transactional
    public void enableRideUpdatesForFamilyContact(UUID userId, UUID sharingId) {
        log.info("Enabling ride updates for family sharing contact {} for user: {}", sharingId, userId);
        
        // Find the contact and verify ownership
        FamilySharingContact contact = familySharingContactRepository.findBySharingIdAndUserId(sharingId, userId)
                .orElseThrow(() -> new SafetyException("Family sharing contact not found"));
        
        contact.enableRideUpdates();
        familySharingContactRepository.save(contact);
        
        log.info("Ride updates enabled for family sharing contact {} for user: {}", sharingId, userId);
    }

    /**
     * Disable ride updates for a family sharing contact
     * 
     * @param userId the user's ID
     * @param sharingId the sharing contact's ID
     * @throws SafetyException if contact not found
     */
    @Transactional
    public void disableRideUpdatesForFamilyContact(UUID userId, UUID sharingId) {
        log.info("Disabling ride updates for family sharing contact {} for user: {}", sharingId, userId);
        
        // Find the contact and verify ownership
        FamilySharingContact contact = familySharingContactRepository.findBySharingIdAndUserId(sharingId, userId)
                .orElseThrow(() -> new SafetyException("Family sharing contact not found"));
        
        contact.disableRideUpdates();
        familySharingContactRepository.save(contact);
        
        log.info("Ride updates disabled for family sharing contact {} for user: {}", sharingId, userId);
    }

    /**
     * Delete all family sharing contacts for a user
     * 
     * @param userId the user's ID
     */
    @Transactional
    public void deleteAllFamilySharingContacts(UUID userId) {
        log.info("Deleting all family sharing contacts for user: {}", userId);
        familySharingContactRepository.deleteByUserId(userId);
        log.info("All family sharing contacts deleted for user: {}", userId);
    }

    /**
     * Validate family sharing contact data
     * 
     * @param contactDTO the contact data to validate
     * @throws SafetyException if validation fails
     */
    private void validateFamilySharingContactData(FamilySharingContactDTO contactDTO) {
        if (contactDTO.getName() == null || contactDTO.getName().trim().isEmpty()) {
            throw new SafetyException("Family contact name is required");
        }
        
        // At least one contact method (phone or email) must be provided
        boolean hasPhone = contactDTO.getPhoneNumber() != null && !contactDTO.getPhoneNumber().trim().isEmpty();
        boolean hasEmail = contactDTO.getEmail() != null && !contactDTO.getEmail().trim().isEmpty();
        
        if (!hasPhone && !hasEmail) {
            throw new SafetyException("At least one contact method (phone number or email) is required");
        }
        
        // Validate phone number format if provided
        if (hasPhone && !PHONE_PATTERN.matcher(contactDTO.getPhoneNumber()).matches()) {
            throw new SafetyException("Invalid phone number format");
        }
        
        // Validate email format if provided
        if (hasEmail && !EMAIL_PATTERN.matcher(contactDTO.getEmail()).matches()) {
            throw new SafetyException("Invalid email format");
        }
    }

    // ==================== SOS Emergency Alert Management ====================

    /**
     * Trigger an SOS emergency alert
     * 
     * @param userId the user's ID
     * @param rideId the ride ID (optional)
     * @param latitude the current latitude
     * @param longitude the current longitude
     * @param message optional message from the user
     * @return the created SOS alert
     * @throws SafetyException if validation fails
     */
    @Transactional
    public SOSAlert triggerSOSAlert(UUID userId, UUID rideId, Double latitude, Double longitude, String message) {
        log.warn("SOS ALERT TRIGGERED by user: {} at location: ({}, {})", userId, latitude, longitude);
        
        // Validate coordinates
        if (latitude == null || longitude == null) {
            throw new SafetyException("Location coordinates are required for SOS alert");
        }
        
        if (latitude < -90 || latitude > 90) {
            throw new SafetyException("Invalid latitude value");
        }
        
        if (longitude < -180 || longitude > 180) {
            throw new SafetyException("Invalid longitude value");
        }
        
        // Check if user already has an active SOS alert
        if (sosAlertRepository.existsByUserIdAndStatus(userId, SOSStatus.ACTIVE)) {
            log.warn("User {} already has an active SOS alert", userId);
            throw new SafetyException("An active SOS alert already exists for this user");
        }
        
        // Create the SOS alert
        SOSAlert alert = SOSAlert.builder()
                .userId(userId)
                .rideId(rideId)
                .latitude(latitude)
                .longitude(longitude)
                .message(message)
                .status(SOSStatus.ACTIVE)
                .build();
        
        SOSAlert savedAlert = sosAlertRepository.save(alert);
        log.info("SOS alert created with ID: {} for user: {}", savedAlert.getAlertId(), userId);
        
        // Trigger emergency contact notifications (placeholder for future implementation)
        notifyEmergencyContacts(userId, savedAlert);
        
        // Start automatic location sharing if not already active
        startAutomaticLocationSharing(userId, rideId, latitude, longitude);
        
        return savedAlert;
    }

    /**
     * Get all SOS alerts for a user
     * 
     * @param userId the user's ID
     * @return list of SOS alerts
     */
    @Transactional(readOnly = true)
    public List<SOSAlert> getSOSAlerts(UUID userId) {
        log.debug("Retrieving SOS alerts for user: {}", userId);
        return sosAlertRepository.findByUserId(userId);
    }

    /**
     * Get the active SOS alert for a user
     * 
     * @param userId the user's ID
     * @return the active SOS alert, or null if none exists
     */
    @Transactional(readOnly = true)
    public SOSAlert getActiveSOSAlert(UUID userId) {
        log.debug("Retrieving active SOS alert for user: {}", userId);
        return sosAlertRepository.findFirstByUserIdAndStatusOrderByCreatedAtDesc(userId, SOSStatus.ACTIVE)
                .orElse(null);
    }

    /**
     * Resolve an SOS alert
     * 
     * @param userId the user's ID
     * @param alertId the alert's ID
     * @param resolvedBy the user ID who resolved the alert
     * @param notes resolution notes
     * @return the resolved SOS alert
     * @throws SafetyException if alert not found
     */
    @Transactional
    public SOSAlert resolveSOSAlert(UUID userId, UUID alertId, UUID resolvedBy, String notes) {
        log.info("Resolving SOS alert {} for user: {}", alertId, userId);
        
        // Find the alert and verify ownership
        SOSAlert alert = sosAlertRepository.findByAlertIdAndUserId(alertId, userId)
                .orElseThrow(() -> new SafetyException("SOS alert not found"));
        
        if (!alert.isActive()) {
            throw new SafetyException("SOS alert is not active");
        }
        
        // Resolve the alert
        alert.resolve(resolvedBy, notes);
        SOSAlert resolvedAlert = sosAlertRepository.save(alert);
        
        log.info("SOS alert {} resolved for user: {}", alertId, userId);
        
        return resolvedAlert;
    }

    /**
     * Cancel an SOS alert (user-initiated)
     * 
     * @param userId the user's ID
     * @param alertId the alert's ID
     * @return the cancelled SOS alert
     * @throws SafetyException if alert not found
     */
    @Transactional
    public SOSAlert cancelSOSAlert(UUID userId, UUID alertId) {
        log.info("Cancelling SOS alert {} for user: {}", alertId, userId);
        
        // Find the alert and verify ownership
        SOSAlert alert = sosAlertRepository.findByAlertIdAndUserId(alertId, userId)
                .orElseThrow(() -> new SafetyException("SOS alert not found"));
        
        if (!alert.isActive()) {
            throw new SafetyException("SOS alert is not active");
        }
        
        // Cancel the alert
        alert.cancel();
        SOSAlert cancelledAlert = sosAlertRepository.save(alert);
        
        log.info("SOS alert {} cancelled by user: {}", alertId, userId);
        
        return cancelledAlert;
    }

    /**
     * Notify emergency contacts about an SOS alert (placeholder)
     * This is a foundation method that will be enhanced with actual notification logic
     * 
     * @param userId the user's ID
     * @param alert the SOS alert
     */
    private void notifyEmergencyContacts(UUID userId, SOSAlert alert) {
        log.info("Notifying emergency contacts for user: {} about SOS alert: {}", userId, alert.getAlertId());
        
        // Get all emergency contacts for the user
        List<EmergencyContact> contacts = emergencyContactRepository.findByUserId(userId);
        
        if (contacts.isEmpty()) {
            log.warn("No emergency contacts found for user: {}", userId);
            return;
        }
        
        // Placeholder: In a real implementation, this would:
        // 1. Send SMS notifications via AWS SNS
        // 2. Send push notifications to mobile apps
        // 3. Send email notifications via AWS SES
        // 4. Include location link and alert details
        
        log.info("Emergency contact notification placeholder executed for {} contacts", contacts.size());
    }

    // ==================== Real-time Location Sharing Management ====================

    /**
     * Start a location sharing session
     * 
     * @param userId the user's ID
     * @param rideId the ride ID (optional)
     * @param latitude the current latitude
     * @param longitude the current longitude
     * @return the created location share
     * @throws SafetyException if validation fails
     */
    @Transactional
    public LocationShare startLocationSharing(UUID userId, UUID rideId, Double latitude, Double longitude) {
        log.info("Starting location sharing for user: {}", userId);
        
        // Validate coordinates
        if (latitude == null || longitude == null) {
            throw new SafetyException("Location coordinates are required");
        }
        
        if (latitude < -90 || latitude > 90) {
            throw new SafetyException("Invalid latitude value");
        }
        
        if (longitude < -180 || longitude > 180) {
            throw new SafetyException("Invalid longitude value");
        }
        
        // Check if user already has an active location share
        Optional<LocationShare> existingShare = locationShareRepository
                .findFirstByUserIdAndIsActiveOrderByCreatedAtDesc(userId, true);
        
        if (existingShare.isPresent()) {
            log.info("User {} already has an active location share, updating it", userId);
            LocationShare share = existingShare.get();
            share.updateLocation(latitude, longitude);
            return locationShareRepository.save(share);
        }
        
        // Generate a unique share token
        String shareToken = generateShareToken();
        
        // Create the location share
        LocationShare share = LocationShare.builder()
                .userId(userId)
                .rideId(rideId)
                .currentLatitude(latitude)
                .currentLongitude(longitude)
                .shareToken(shareToken)
                .isActive(true)
                .build();
        
        LocationShare savedShare = locationShareRepository.save(share);
        log.info("Location sharing started with ID: {} for user: {}", savedShare.getShareId(), userId);
        
        // Notify family sharing contacts (placeholder)
        notifyFamilyContacts(userId, savedShare);
        
        return savedShare;
    }

    /**
     * Update location in an active sharing session
     * 
     * @param userId the user's ID
     * @param shareId the share ID
     * @param latitude the new latitude
     * @param longitude the new longitude
     * @return the updated location share
     * @throws SafetyException if share not found or validation fails
     */
    @Transactional
    public LocationShare updateSharedLocation(UUID userId, UUID shareId, Double latitude, Double longitude) {
        log.debug("Updating shared location for user: {}, share: {}", userId, shareId);
        
        // Validate coordinates
        if (latitude == null || longitude == null) {
            throw new SafetyException("Location coordinates are required");
        }
        
        // Find the share and verify ownership
        LocationShare share = locationShareRepository.findByShareIdAndUserId(shareId, userId)
                .orElseThrow(() -> new SafetyException("Location share not found"));
        
        if (!share.isCurrentlyActive()) {
            throw new SafetyException("Location share is not active");
        }
        
        // Update the location
        share.updateLocation(latitude, longitude);
        LocationShare updatedShare = locationShareRepository.save(share);
        
        log.debug("Location updated for share: {}", shareId);
        
        return updatedShare;
    }

    /**
     * Get active location share for a user
     * 
     * @param userId the user's ID
     * @return the active location share, or null if none exists
     */
    @Transactional(readOnly = true)
    public LocationShare getActiveLocationShare(UUID userId) {
        log.debug("Retrieving active location share for user: {}", userId);
        return locationShareRepository.findFirstByUserIdAndIsActiveOrderByCreatedAtDesc(userId, true)
                .orElse(null);
    }

    /**
     * Get location share by token (for family members to view)
     * 
     * @param shareToken the share token
     * @return the location share
     * @throws SafetyException if share not found
     */
    @Transactional(readOnly = true)
    public LocationShare getLocationShareByToken(String shareToken) {
        log.debug("Retrieving location share by token");
        return locationShareRepository.findByShareToken(shareToken)
                .orElseThrow(() -> new SafetyException("Location share not found"));
    }

    /**
     * End a location sharing session
     * 
     * @param userId the user's ID
     * @param shareId the share ID
     * @return the ended location share
     * @throws SafetyException if share not found
     */
    @Transactional
    public LocationShare endLocationSharing(UUID userId, UUID shareId) {
        log.info("Ending location sharing for user: {}, share: {}", userId, shareId);
        
        // Find the share and verify ownership
        LocationShare share = locationShareRepository.findByShareIdAndUserId(shareId, userId)
                .orElseThrow(() -> new SafetyException("Location share not found"));
        
        if (!share.isCurrentlyActive()) {
            throw new SafetyException("Location share is not active");
        }
        
        // End the sharing
        share.endSharing();
        LocationShare endedShare = locationShareRepository.save(share);
        
        log.info("Location sharing ended for share: {}", shareId);
        
        return endedShare;
    }

    /**
     * End all active location shares for a user
     * 
     * @param userId the user's ID
     */
    @Transactional
    public void endAllLocationSharing(UUID userId) {
        log.info("Ending all location sharing for user: {}", userId);
        locationShareRepository.endAllActiveSharesForUser(userId);
        log.info("All location sharing ended for user: {}", userId);
    }

    /**
     * Start automatic location sharing when SOS is triggered
     * 
     * @param userId the user's ID
     * @param rideId the ride ID (optional)
     * @param latitude the current latitude
     * @param longitude the current longitude
     */
    private void startAutomaticLocationSharing(UUID userId, UUID rideId, Double latitude, Double longitude) {
        try {
            // Check if location sharing is already active
            if (!locationShareRepository.existsByUserIdAndIsActive(userId, true)) {
                startLocationSharing(userId, rideId, latitude, longitude);
                log.info("Automatic location sharing started for SOS alert");
            } else {
                log.info("Location sharing already active for user: {}", userId);
            }
        } catch (Exception e) {
            log.error("Failed to start automatic location sharing for user: {}", userId, e);
            // Don't fail the SOS alert if location sharing fails
        }
    }

    /**
     * Notify family sharing contacts about location sharing (placeholder)
     * This is a foundation method that will be enhanced with actual notification logic
     * 
     * @param userId the user's ID
     * @param share the location share
     */
    private void notifyFamilyContacts(UUID userId, LocationShare share) {
        log.info("Notifying family contacts for user: {} about location share: {}", userId, share.getShareId());
        
        // Get all family sharing contacts with ride updates enabled
        List<FamilySharingContact> contacts = familySharingContactRepository
                .findByUserIdAndReceiveRideUpdatesTrue(userId);
        
        if (contacts.isEmpty()) {
            log.debug("No family contacts with ride updates enabled for user: {}", userId);
            return;
        }
        
        // Placeholder: In a real implementation, this would:
        // 1. Send SMS notifications with share link via AWS SNS
        // 2. Send email notifications with share link via AWS SES
        // 3. Send push notifications to mobile apps
        // 4. Include the share token/URL for real-time tracking
        
        log.info("Family contact notification placeholder executed for {} contacts", contacts.size());
    }

    /**
     * Generate a unique share token for location sharing
     * 
     * @return a unique share token
     */
    private String generateShareToken() {
        return UUID.randomUUID().toString().replace("-", "");
    }
}
