package com.officemate.shared.dto;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

/**
 * Request DTO for triggering an SOS emergency alert.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SOSRequest {

    /**
     * Ride ID associated with the SOS alert (optional)
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

    /**
     * Optional message or notes from the user
     */
    private String message;
}
