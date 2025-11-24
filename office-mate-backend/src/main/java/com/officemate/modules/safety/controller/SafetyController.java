package com.officemate.modules.safety.controller;

import com.officemate.modules.safety.entity.EmergencyContact;
import com.officemate.modules.safety.entity.FamilySharingContact;
import com.officemate.modules.safety.entity.SOSAlert;
import com.officemate.modules.safety.entity.LocationShare;
import com.officemate.modules.safety.service.SafetyService;
import com.officemate.shared.dto.*;
import com.officemate.shared.dto.SafetySettingsRequest.EmergencyContactDTO;
import com.officemate.shared.dto.SafetySettingsRequest.FamilySharingContactDTO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * REST controller for safety features management.
 * Handles emergency contacts, family sharing, and safety settings.
 * All endpoints require authentication and email verification.
 */
@RestController
@RequestMapping("/users/{userId}")
@RequiredArgsConstructor
@Slf4j
public class SafetyController {

    private final SafetyService safetyService;

    /**
     * Add a new emergency contact for the user.
     * Requires both mobile and email verification.
     * 
     * POST /users/{userId}/emergency-contacts
     * 
     * @param userId The user's unique identifier
     * @param contactDTO Emergency contact information
     * @return EmergencyContactResponse with created contact details
     */
    @PostMapping("/emergency-contacts")
    @PreAuthorize("hasAuthority('EMAIL_VERIFIED') and (#userId == authentication.name or hasRole('ADMIN'))")
    public ResponseEntity<EmergencyContactResponse> addEmergencyContact(
            @PathVariable String userId,
            @Valid @RequestBody EmergencyContactDTO contactDTO) {
        
        log.info("Add emergency contact request for user: {}", userId);
        
        try {
            UUID userUuid = UUID.fromString(userId);
            EmergencyContact contact = safetyService.addEmergencyContact(userUuid, contactDTO);
            EmergencyContactResponse response = buildEmergencyContactResponse(contact);
            log.info("Successfully added emergency contact for user: {}", userId);
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (IllegalArgumentException e) {
            log.warn("Emergency contact creation failed for user {}: {}", userId, e.getMessage());
            throw e;
        }
    }

    /**
     * Get all emergency contacts for the user.
     * 
     * GET /users/{userId}/emergency-contacts
     * 
     * @param userId The user's unique identifier
     * @return List of EmergencyContactResponse
     */
    @GetMapping("/emergency-contacts")
    @PreAuthorize("hasAuthority('EMAIL_VERIFIED') and (#userId == authentication.name or hasRole('ADMIN'))")
    public ResponseEntity<List<EmergencyContactResponse>> getEmergencyContacts(@PathVariable String userId) {
        log.info("Get emergency contacts request for user: {}", userId);
        
        try {
            UUID userUuid = UUID.fromString(userId);
            List<EmergencyContact> contacts = safetyService.getEmergencyContacts(userUuid);
            List<EmergencyContactResponse> responses = contacts.stream()
                    .map(this::buildEmergencyContactResponse)
                    .collect(Collectors.toList());
            log.info("Successfully retrieved {} emergency contacts for user: {}", responses.size(), userId);
            return ResponseEntity.ok(responses);
        } catch (IllegalArgumentException e) {
            log.warn("Invalid user ID format: {}", userId);
            throw e;
        }
    }

    /**
     * Update an existing emergency contact.
     * 
     * PUT /users/{userId}/emergency-contacts/{contactId}
     * 
     * @param userId The user's unique identifier
     * @param contactId The emergency contact's unique identifier
     * @param contactDTO Updated emergency contact information
     * @return EmergencyContactResponse with updated contact details
     */
    @PutMapping("/emergency-contacts/{contactId}")
    @PreAuthorize("hasAuthority('EMAIL_VERIFIED') and (#userId == authentication.name or hasRole('ADMIN'))")
    public ResponseEntity<EmergencyContactResponse> updateEmergencyContact(
            @PathVariable String userId,
            @PathVariable String contactId,
            @Valid @RequestBody EmergencyContactDTO contactDTO) {
        
        log.info("Update emergency contact {} request for user: {}", contactId, userId);
        
        try {
            UUID userUuid = UUID.fromString(userId);
            UUID contactUuid = UUID.fromString(contactId);
            EmergencyContact contact = safetyService.updateEmergencyContact(userUuid, contactUuid, contactDTO);
            EmergencyContactResponse response = buildEmergencyContactResponse(contact);
            log.info("Successfully updated emergency contact {} for user: {}", contactId, userId);
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            log.warn("Emergency contact update failed for user {}: {}", userId, e.getMessage());
            throw e;
        }
    }

    /**
     * Delete an emergency contact.
     * 
     * DELETE /users/{userId}/emergency-contacts/{contactId}
     * 
     * @param userId The user's unique identifier
     * @param contactId The emergency contact's unique identifier
     * @return 204 No Content on success
     */
    @DeleteMapping("/emergency-contacts/{contactId}")
    @PreAuthorize("hasAuthority('EMAIL_VERIFIED') and (#userId == authentication.name or hasRole('ADMIN'))")
    public ResponseEntity<Void> deleteEmergencyContact(
            @PathVariable String userId,
            @PathVariable String contactId) {
        
        log.info("Delete emergency contact {} request for user: {}", contactId, userId);
        
        try {
            UUID userUuid = UUID.fromString(userId);
            UUID contactUuid = UUID.fromString(contactId);
            safetyService.deleteEmergencyContact(userUuid, contactUuid);
            log.info("Successfully deleted emergency contact {} for user: {}", contactId, userId);
            return ResponseEntity.noContent().build();
        } catch (IllegalArgumentException e) {
            log.warn("Emergency contact deletion failed for user {}: {}", userId, e.getMessage());
            throw e;
        }
    }

    /**
     * Add a new family sharing contact for the user.
     * Requires both mobile and email verification.
     * 
     * POST /users/{userId}/family-sharing
     * 
     * @param userId The user's unique identifier
     * @param contactDTO Family sharing contact information
     * @return FamilySharingContactResponse with created contact details
     */
    @PostMapping("/family-sharing")
    @PreAuthorize("hasAuthority('EMAIL_VERIFIED') and (#userId == authentication.name or hasRole('ADMIN'))")
    public ResponseEntity<FamilySharingContactResponse> addFamilySharingContact(
            @PathVariable String userId,
            @Valid @RequestBody FamilySharingContactDTO contactDTO) {
        
        log.info("Add family sharing contact request for user: {}", userId);
        
        try {
            UUID userUuid = UUID.fromString(userId);
            FamilySharingContact contact = safetyService.addFamilySharingContact(userUuid, contactDTO);
            FamilySharingContactResponse response = buildFamilySharingContactResponse(contact);
            log.info("Successfully added family sharing contact for user: {}", userId);
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (IllegalArgumentException e) {
            log.warn("Family sharing contact creation failed for user {}: {}", userId, e.getMessage());
            throw e;
        }
    }

    /**
     * Get all family sharing contacts for the user.
     * 
     * GET /users/{userId}/family-sharing
     * 
     * @param userId The user's unique identifier
     * @return List of FamilySharingContactResponse
     */
    @GetMapping("/family-sharing")
    @PreAuthorize("hasAuthority('EMAIL_VERIFIED') and (#userId == authentication.name or hasRole('ADMIN'))")
    public ResponseEntity<List<FamilySharingContactResponse>> getFamilySharingContacts(@PathVariable String userId) {
        log.info("Get family sharing contacts request for user: {}", userId);
        
        try {
            UUID userUuid = UUID.fromString(userId);
            List<FamilySharingContact> contacts = safetyService.getFamilySharingContacts(userUuid);
            List<FamilySharingContactResponse> responses = contacts.stream()
                    .map(this::buildFamilySharingContactResponse)
                    .collect(Collectors.toList());
            log.info("Successfully retrieved {} family sharing contacts for user: {}", responses.size(), userId);
            return ResponseEntity.ok(responses);
        } catch (IllegalArgumentException e) {
            log.warn("Invalid user ID format: {}", userId);
            throw e;
        }
    }

    /**
     * Update an existing family sharing contact.
     * 
     * PUT /users/{userId}/family-sharing/{sharingId}
     * 
     * @param userId The user's unique identifier
     * @param sharingId The family sharing contact's unique identifier
     * @param contactDTO Updated family sharing contact information
     * @return FamilySharingContactResponse with updated contact details
     */
    @PutMapping("/family-sharing/{sharingId}")
    @PreAuthorize("hasAuthority('EMAIL_VERIFIED') and (#userId == authentication.name or hasRole('ADMIN'))")
    public ResponseEntity<FamilySharingContactResponse> updateFamilySharingContact(
            @PathVariable String userId,
            @PathVariable String sharingId,
            @Valid @RequestBody FamilySharingContactDTO contactDTO) {
        
        log.info("Update family sharing contact {} request for user: {}", sharingId, userId);
        
        try {
            UUID userUuid = UUID.fromString(userId);
            UUID sharingUuid = UUID.fromString(sharingId);
            FamilySharingContact contact = safetyService.updateFamilySharingContact(userUuid, sharingUuid, contactDTO);
            FamilySharingContactResponse response = buildFamilySharingContactResponse(contact);
            log.info("Successfully updated family sharing contact {} for user: {}", sharingId, userId);
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            log.warn("Family sharing contact update failed for user {}: {}", userId, e.getMessage());
            throw e;
        }
    }

    /**
     * Delete a family sharing contact.
     * 
     * DELETE /users/{userId}/family-sharing/{sharingId}
     * 
     * @param userId The user's unique identifier
     * @param sharingId The family sharing contact's unique identifier
     * @return 204 No Content on success
     */
    @DeleteMapping("/family-sharing/{sharingId}")
    @PreAuthorize("hasAuthority('EMAIL_VERIFIED') and (#userId == authentication.name or hasRole('ADMIN'))")
    public ResponseEntity<Void> deleteFamilySharingContact(
            @PathVariable String userId,
            @PathVariable String sharingId) {
        
        log.info("Delete family sharing contact {} request for user: {}", sharingId, userId);
        
        try {
            UUID userUuid = UUID.fromString(userId);
            UUID sharingUuid = UUID.fromString(sharingId);
            safetyService.deleteFamilySharingContact(userUuid, sharingUuid);
            log.info("Successfully deleted family sharing contact {} for user: {}", sharingId, userId);
            return ResponseEntity.noContent().build();
        } catch (IllegalArgumentException e) {
            log.warn("Family sharing contact deletion failed for user {}: {}", userId, e.getMessage());
            throw e;
        }
    }

    /**
     * Get comprehensive safety settings for the user.
     * Includes emergency contacts, family sharing contacts, and safety status.
     * 
     * GET /users/{userId}/safety-settings
     * 
     * @param userId The user's unique identifier
     * @return SafetySettingsResponse with all safety information
     */
    @GetMapping("/safety-settings")
    @PreAuthorize("hasAuthority('EMAIL_VERIFIED') and (#userId == authentication.name or hasRole('ADMIN'))")
    public ResponseEntity<SafetySettingsResponse> getSafetySettings(@PathVariable String userId) {
        log.info("Get safety settings request for user: {}", userId);
        
        try {
            UUID userUuid = UUID.fromString(userId);
            
            // Get emergency contacts
            List<EmergencyContact> emergencyContacts = safetyService.getEmergencyContacts(userUuid);
            List<EmergencyContactResponse> emergencyContactResponses = emergencyContacts.stream()
                    .map(this::buildEmergencyContactResponse)
                    .collect(Collectors.toList());
            
            // Get family sharing contacts
            List<FamilySharingContact> familyContacts = safetyService.getFamilySharingContacts(userUuid);
            List<FamilySharingContactResponse> familyContactResponses = familyContacts.stream()
                    .map(this::buildFamilySharingContactResponse)
                    .collect(Collectors.toList());
            
            // Check for active SOS alert
            SOSAlert activeSOSAlert = safetyService.getActiveSOSAlert(userUuid);
            boolean hasActiveSOSAlert = activeSOSAlert != null;
            
            // Check for active location sharing
            LocationShare activeLocationShare = safetyService.getActiveLocationShare(userUuid);
            boolean locationSharingActive = activeLocationShare != null;
            
            SafetySettingsResponse response = SafetySettingsResponse.builder()
                    .emergencyContacts(emergencyContactResponses)
                    .familySharingContacts(familyContactResponses)
                    .hasActiveSOSAlert(hasActiveSOSAlert)
                    .locationSharingActive(locationSharingActive)
                    .build();
            
            log.info("Successfully retrieved safety settings for user: {}", userId);
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            log.warn("Invalid user ID format: {}", userId);
            throw e;
        }
    }

    /**
     * Update safety settings for the user.
     * Allows bulk update of emergency contacts and family sharing contacts.
     * 
     * PUT /users/{userId}/safety-settings
     * 
     * @param userId The user's unique identifier
     * @param request SafetySettingsRequest with updated settings
     * @return SafetySettingsResponse with updated safety information
     */
    @PutMapping("/safety-settings")
    @PreAuthorize("hasAuthority('EMAIL_VERIFIED') and (#userId == authentication.name or hasRole('ADMIN'))")
    public ResponseEntity<SafetySettingsResponse> updateSafetySettings(
            @PathVariable String userId,
            @Valid @RequestBody SafetySettingsRequest request) {
        
        log.info("Update safety settings request for user: {}", userId);
        
        try {
            UUID userUuid = UUID.fromString(userId);
            
            // Delete all existing contacts if new ones are provided
            if (request.getEmergencyContacts() != null && !request.getEmergencyContacts().isEmpty()) {
                safetyService.deleteAllEmergencyContacts(userUuid);
                
                // Add new emergency contacts
                for (EmergencyContactDTO contactDTO : request.getEmergencyContacts()) {
                    safetyService.addEmergencyContact(userUuid, contactDTO);
                }
            }
            
            if (request.getFamilySharingContacts() != null && !request.getFamilySharingContacts().isEmpty()) {
                safetyService.deleteAllFamilySharingContacts(userUuid);
                
                // Add new family sharing contacts
                for (FamilySharingContactDTO contactDTO : request.getFamilySharingContacts()) {
                    safetyService.addFamilySharingContact(userUuid, contactDTO);
                }
            }
            
            // Return updated safety settings
            log.info("Successfully updated safety settings for user: {}", userId);
            return getSafetySettings(userId);
        } catch (IllegalArgumentException e) {
            log.warn("Safety settings update failed for user {}: {}", userId, e.getMessage());
            throw e;
        }
    }

    /**
     * Build EmergencyContactResponse from EmergencyContact entity.
     * 
     * @param contact The emergency contact entity
     * @return EmergencyContactResponse DTO
     */
    private EmergencyContactResponse buildEmergencyContactResponse(EmergencyContact contact) {
        return EmergencyContactResponse.builder()
                .contactId(contact.getContactId())
                .name(contact.getName())
                .phoneNumber(contact.getPhoneNumber())
                .relationship(contact.getRelationship())
                .isPrimary(contact.isPrimaryContact())
                .createdAt(contact.getCreatedAt())
                .build();
    }

    /**
     * Build FamilySharingContactResponse from FamilySharingContact entity.
     * 
     * @param contact The family sharing contact entity
     * @return FamilySharingContactResponse DTO
     */
    private FamilySharingContactResponse buildFamilySharingContactResponse(FamilySharingContact contact) {
        return FamilySharingContactResponse.builder()
                .sharingId(contact.getSharingId())
                .name(contact.getName())
                .phoneNumber(contact.getPhoneNumber())
                .email(contact.getEmail())
                .receiveRideUpdates(contact.shouldReceiveRideUpdates())
                .createdAt(contact.getCreatedAt())
                .build();
    }
}
