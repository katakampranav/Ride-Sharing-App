package com.officemate.modules.safety.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * JPA Entity representing a real-time location sharing session in the PostgreSQL database.
 * Location shares allow users to share their real-time location with emergency contacts and family.
 */
@Entity
@Table(
    name = "location_shares",
    indexes = {
        @Index(name = "idx_location_shares_user", columnList = "user_id"),
        @Index(name = "idx_location_shares_ride", columnList = "ride_id"),
        @Index(name = "idx_location_shares_status", columnList = "is_active")
    }
)
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LocationShare {

    /**
     * Unique identifier for the location share session (UUID)
     */
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "share_id", updatable = false, nullable = false)
    private UUID shareId;

    /**
     * Reference to the user sharing their location
     */
    @Column(name = "user_id", nullable = false)
    private UUID userId;

    /**
     * Ride ID associated with this location share (if applicable)
     */
    @Column(name = "ride_id")
    private UUID rideId;

    /**
     * Flag indicating if location sharing is currently active
     */
    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private Boolean isActive = true;

    /**
     * Current latitude coordinate
     */
    @Column(name = "current_latitude")
    private Double currentLatitude;

    /**
     * Current longitude coordinate
     */
    @Column(name = "current_longitude")
    private Double currentLongitude;

    /**
     * Timestamp of the last location update
     */
    @Column(name = "last_location_update")
    private LocalDateTime lastLocationUpdate;

    /**
     * Timestamp when the location share session was created
     */
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    /**
     * Timestamp when the location share session was last updated
     */
    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    /**
     * Timestamp when the location share session ended
     */
    @Column(name = "ended_at")
    private LocalDateTime endedAt;

    /**
     * Shareable token for accessing the location share
     */
    @Column(name = "share_token", unique = true, length = 100)
    private String shareToken;

    /**
     * Update the current location
     * 
     * @param latitude the new latitude
     * @param longitude the new longitude
     */
    public void updateLocation(Double latitude, Double longitude) {
        this.currentLatitude = latitude;
        this.currentLongitude = longitude;
        this.lastLocationUpdate = LocalDateTime.now();
    }

    /**
     * End the location sharing session
     */
    public void endSharing() {
        this.isActive = false;
        this.endedAt = LocalDateTime.now();
    }

    /**
     * Check if location sharing is currently active
     * 
     * @return true if location sharing is active
     */
    public boolean isCurrentlyActive() {
        return isActive != null && isActive;
    }
}
