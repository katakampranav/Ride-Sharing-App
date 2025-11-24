package com.officemate.shared.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Response DTO for location sharing operations.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LocationShareResponse {

    /**
     * Share ID
     */
    private UUID shareId;

    /**
     * User ID sharing the location
     */
    private UUID userId;

    /**
     * Ride ID associated with the share
     */
    private UUID rideId;

    /**
     * Flag indicating if sharing is active
     */
    private Boolean isActive;

    /**
     * Current latitude coordinate
     */
    private Double currentLatitude;

    /**
     * Current longitude coordinate
     */
    private Double currentLongitude;

    /**
     * Timestamp of last location update
     */
    private LocalDateTime lastLocationUpdate;

    /**
     * Timestamp when sharing was created
     */
    private LocalDateTime createdAt;

    /**
     * Shareable token for accessing the location
     */
    private String shareToken;

    /**
     * Shareable URL for accessing the location
     */
    private String shareUrl;
}
