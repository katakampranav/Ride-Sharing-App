package com.officemate.shared.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Response DTO for SOS emergency alert operations.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SOSResponse {

    /**
     * Alert ID
     */
    private UUID alertId;

    /**
     * User ID who triggered the alert
     */
    private UUID userId;

    /**
     * Ride ID associated with the alert
     */
    private UUID rideId;

    /**
     * Current status of the alert
     */
    private String status;

    /**
     * Latitude coordinate
     */
    private Double latitude;

    /**
     * Longitude coordinate
     */
    private Double longitude;

    /**
     * Alert message
     */
    private String message;

    /**
     * Timestamp when the alert was created
     */
    private LocalDateTime createdAt;

    /**
     * Timestamp when the alert was resolved
     */
    private LocalDateTime resolvedAt;

    /**
     * Number of emergency contacts notified
     */
    private Integer contactsNotified;

    /**
     * Flag indicating if notifications were sent successfully
     */
    private Boolean notificationsSent;
}
