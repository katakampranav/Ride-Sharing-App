package com.officemate.shared.service;

import com.officemate.shared.entity.AuditLog;
import com.officemate.shared.repository.AuditLogRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit tests for AuditService
 */
@ExtendWith(MockitoExtension.class)
class AuditServiceTest {

    @Mock
    private AuditLogRepository auditLogRepository;

    @InjectMocks
    private AuditService auditService;

    private UUID testUserId;

    @BeforeEach
    void setUp() {
        testUserId = UUID.randomUUID();
    }

    @Test
    void testLogProfileChange() {
        // Given
        String entityType = "UserProfile";
        String entityId = testUserId.toString();
        String fieldName = "firstName";
        String oldValue = "John";
        String newValue = "Jane";
        String reason = "Profile update";

        // When
        auditService.logProfileChange(testUserId, entityType, entityId, fieldName, oldValue, newValue, reason);

        // Then
        ArgumentCaptor<AuditLog> auditLogCaptor = ArgumentCaptor.forClass(AuditLog.class);
        verify(auditLogRepository, timeout(1000)).save(auditLogCaptor.capture());

        AuditLog capturedLog = auditLogCaptor.getValue();
        assertEquals(testUserId, capturedLog.getUserId());
        assertEquals(entityType, capturedLog.getEntityType());
        assertEquals(entityId, capturedLog.getEntityId());
        assertEquals("UPDATE", capturedLog.getAction());
        assertEquals(fieldName, capturedLog.getFieldName());
        assertEquals(oldValue, capturedLog.getOldValue());
        assertEquals(newValue, capturedLog.getNewValue());
        assertEquals(reason, capturedLog.getReason());
        assertNotNull(capturedLog.getTimestamp());
    }

    @Test
    void testLogEntityCreation() {
        // Given
        String entityType = "DriverProfile";
        String entityId = testUserId.toString();
        String reason = "Driver profile creation";

        // When
        auditService.logEntityCreation(testUserId, entityType, entityId, reason);

        // Then
        ArgumentCaptor<AuditLog> auditLogCaptor = ArgumentCaptor.forClass(AuditLog.class);
        verify(auditLogRepository, timeout(1000)).save(auditLogCaptor.capture());

        AuditLog capturedLog = auditLogCaptor.getValue();
        assertEquals(testUserId, capturedLog.getUserId());
        assertEquals(entityType, capturedLog.getEntityType());
        assertEquals(entityId, capturedLog.getEntityId());
        assertEquals("CREATE", capturedLog.getAction());
        assertEquals(reason, capturedLog.getReason());
        assertNotNull(capturedLog.getTimestamp());
    }

    @Test
    void testLogEntityDeletion() {
        // Given
        String entityType = "RiderProfile";
        String entityId = testUserId.toString();
        String reason = "Profile deletion";

        // When
        auditService.logEntityDeletion(testUserId, entityType, entityId, reason);

        // Then
        ArgumentCaptor<AuditLog> auditLogCaptor = ArgumentCaptor.forClass(AuditLog.class);
        verify(auditLogRepository, timeout(1000)).save(auditLogCaptor.capture());

        AuditLog capturedLog = auditLogCaptor.getValue();
        assertEquals(testUserId, capturedLog.getUserId());
        assertEquals(entityType, capturedLog.getEntityType());
        assertEquals(entityId, capturedLog.getEntityId());
        assertEquals("DELETE", capturedLog.getAction());
        assertEquals(reason, capturedLog.getReason());
        assertNotNull(capturedLog.getTimestamp());
    }

    @Test
    void testLogBulkProfileChange() {
        // Given
        String entityType = "UserProfile";
        String entityId = testUserId.toString();
        String changesDescription = "Updated multiple fields: firstName, lastName, dateOfBirth";
        String reason = "Bulk profile update";

        // When
        auditService.logBulkProfileChange(testUserId, entityType, entityId, changesDescription, reason);

        // Then
        ArgumentCaptor<AuditLog> auditLogCaptor = ArgumentCaptor.forClass(AuditLog.class);
        verify(auditLogRepository, timeout(1000)).save(auditLogCaptor.capture());

        AuditLog capturedLog = auditLogCaptor.getValue();
        assertEquals(testUserId, capturedLog.getUserId());
        assertEquals(entityType, capturedLog.getEntityType());
        assertEquals(entityId, capturedLog.getEntityId());
        assertEquals("BULK_UPDATE", capturedLog.getAction());
        assertEquals(changesDescription, capturedLog.getNewValue());
        assertEquals(reason, capturedLog.getReason());
        assertNotNull(capturedLog.getTimestamp());
    }

    @Test
    void testLogProfileChangeHandlesException() {
        // Given
        when(auditLogRepository.save(any(AuditLog.class))).thenThrow(new RuntimeException("Database error"));

        // When & Then - should not throw exception
        assertDoesNotThrow(() -> {
            auditService.logProfileChange(testUserId, "UserProfile", testUserId.toString(), 
                    "firstName", "John", "Jane", "Test update");
        });

        // Verify that save was attempted
        verify(auditLogRepository, timeout(1000)).save(any(AuditLog.class));
    }
}