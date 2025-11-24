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
 * JPA Entity representing a family sharing contact in the PostgreSQL database.
 * Family sharing contacts receive ride updates and can track user's rides in real-time.
 */
@Entity
@Table(
    name = "family_sharing_contacts",
    indexes = {
        @Index(name = "idx_family_sharing_user", columnList = "user_id")
    }
)
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FamilySharingContact {

    /**
     * Unique identifier for the family sharing contact (UUID)
     */
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "sharing_id", updatable = false, nullable = false)
    private UUID sharingId;

    /**
     * Reference to the user account this family sharing contact belongs to
     */
    @Column(name = "user_id", nullable = false)
    private UUID userId;

    /**
     * Family member's full name
     */
    @Column(name = "name", nullable = false, length = 100)
    private String name;

    /**
     * Family member's phone number (optional)
     */
    @Column(name = "phone_number", length = 20)
    private String phoneNumber;

    /**
     * Family member's email address (optional)
     */
    @Column(name = "email", length = 255)
    private String email;

    /**
     * Flag indicating if this contact should receive ride update notifications
     */
    @Column(name = "receive_ride_updates", nullable = false)
    @Builder.Default
    private Boolean receiveRideUpdates = true;

    /**
     * Timestamp when the family sharing contact was created
     */
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    /**
     * Enable ride update notifications for this contact
     */
    public void enableRideUpdates() {
        this.receiveRideUpdates = true;
    }

    /**
     * Disable ride update notifications for this contact
     */
    public void disableRideUpdates() {
        this.receiveRideUpdates = false;
    }

    /**
     * Check if this contact should receive ride updates
     * 
     * @return true if ride updates are enabled
     */
    public boolean shouldReceiveRideUpdates() {
        return receiveRideUpdates != null && receiveRideUpdates;
    }
}
