package com.officemate.modules.safety.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * JPA Entity representing an emergency contact in the PostgreSQL database.
 * Emergency contacts can be notified during SOS situations and ride emergencies.
 */
@Entity
@Table(
    name = "emergency_contacts",
    indexes = {
        @Index(name = "idx_emergency_contacts_user", columnList = "user_id"),
        @Index(name = "idx_emergency_contacts_primary", columnList = "user_id, is_primary")
    }
)
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EmergencyContact {

    /**
     * Unique identifier for the emergency contact (UUID)
     */
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "contact_id", updatable = false, nullable = false)
    private UUID contactId;

    /**
     * Reference to the user account this emergency contact belongs to
     */
    @Column(name = "user_id", nullable = false)
    private UUID userId;

    /**
     * Emergency contact's full name
     */
    @Column(name = "name", nullable = false, length = 100)
    private String name;

    /**
     * Emergency contact's phone number
     */
    @Column(name = "phone_number", nullable = false, length = 20)
    private String phoneNumber;

    /**
     * Relationship to the user (e.g., "Spouse", "Parent", "Sibling", "Friend")
     */
    @Column(name = "relationship", length = 50)
    private String relationship;

    /**
     * Flag indicating if this is the primary emergency contact
     * Only one contact per user should be marked as primary
     */
    @Column(name = "is_primary", nullable = false)
    @Builder.Default
    private Boolean isPrimary = false;

    /**
     * Timestamp when the emergency contact was created
     */
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    /**
     * Marks this contact as the primary emergency contact
     */
    public void markAsPrimary() {
        this.isPrimary = true;
    }

    /**
     * Removes the primary designation from this contact
     */
    public void unmarkAsPrimary() {
        this.isPrimary = false;
    }

    /**
     * Checks if this is the primary emergency contact
     * 
     * @return true if this is the primary contact
     */
    public boolean isPrimaryContact() {
        return isPrimary != null && isPrimary;
    }
}
