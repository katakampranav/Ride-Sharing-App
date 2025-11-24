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
 * JPA Entity representing an SOS emergency alert in the PostgreSQL database.
 * SOS alerts are triggered during ride emergencies and notify emergency contacts.
 */
@Entity
@Table(
    name = "sos_alerts",
    indexes = {
        @Index(name = "idx_sos_alerts_user", columnList = "user_id"),
        @Index(name = "idx_sos_alerts_status", columnList = "status"),
        @Index(name = "idx_sos_alerts_created", columnList = "created_at")
    }
)
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SOSAlert {

    /**
     * Unique identifier for the SOS alert (UUID)
     */
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "alert_id", updatable = false, nullable = false)
    private UUID alertId;

    /**
     * Reference to the user who triggered the SOS alert
     */
    @Column(name = "user_id", nullable = false)
    private UUID userId;

    /**
     * Ride ID associated with this SOS alert (if applicable)
     */
    @Column(name = "ride_id")
    private UUID rideId;

    /**
     * Current status of the SOS alert
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    @Builder.Default
    private SOSStatus status = SOSStatus.ACTIVE;

    /**
     * Latitude coordinate where SOS was triggered
     */
    @Column(name = "latitude", nullable = false)
    private Double latitude;

    /**
     * Longitude coordinate where SOS was triggered
     */
    @Column(name = "longitude", nullable = false)
    private Double longitude;

    /**
     * Optional message or notes from the user
     */
    @Column(name = "message", length = 500)
    private String message;

    /**
     * Timestamp when the SOS alert was created
     */
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    /**
     * Timestamp when the SOS alert was resolved
     */
    @Column(name = "resolved_at")
    private LocalDateTime resolvedAt;

    /**
     * User ID of the person who resolved the alert (admin or support)
     */
    @Column(name = "resolved_by")
    private UUID resolvedBy;

    /**
     * Resolution notes
     */
    @Column(name = "resolution_notes", length = 1000)
    private String resolutionNotes;

    /**
     * Enum representing the status of an SOS alert
     */
    public enum SOSStatus {
        ACTIVE,      // Alert is active and being processed
        RESOLVED,    // Alert has been resolved
        CANCELLED    // Alert was cancelled by user
    }

    /**
     * Resolve the SOS alert
     * 
     * @param resolvedBy the user ID who resolved the alert
     * @param notes resolution notes
     */
    public void resolve(UUID resolvedBy, String notes) {
        this.status = SOSStatus.RESOLVED;
        this.resolvedAt = LocalDateTime.now();
        this.resolvedBy = resolvedBy;
        this.resolutionNotes = notes;
    }

    /**
     * Cancel the SOS alert
     */
    public void cancel() {
        this.status = SOSStatus.CANCELLED;
        this.resolvedAt = LocalDateTime.now();
    }

    /**
     * Check if the alert is active
     * 
     * @return true if the alert is active
     */
    public boolean isActive() {
        return status == SOSStatus.ACTIVE;
    }
}
