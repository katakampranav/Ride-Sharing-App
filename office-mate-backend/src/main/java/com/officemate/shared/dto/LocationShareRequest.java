package com.officemate.shared.dto;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

/**
 * Request DTO for starting or updating a location sharing session.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LocationShareRequest {

    /**
     * Ride ID associated with the location share (optional)
     */
    private UUID rideId;

    /**
     * Current latitude coordinate
     */
    @NotNull(message = "Latitude is required")
    private Double latitude;

    /**
     * Current longitude coordinate
     */
    @NotNull(message = "Longitude is required")
    private Double longitude;
}
