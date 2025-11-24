package com.officemate.shared.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.List;

/**
 * DTO for route preferences used by both drivers and riders.
 * Contains start/end coordinates, addresses, and preferred travel times.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RoutePreferencesDTO {
    
    /**
     * Starting point latitude
     */
    @NotNull(message = "Start latitude is required")
    @DecimalMin(value = "-90.0", message = "Latitude must be between -90 and 90")
    @DecimalMax(value = "90.0", message = "Latitude must be between -90 and 90")
    private Double startLatitude;
    
    /**
     * Starting point longitude
     */
    @NotNull(message = "Start longitude is required")
    @DecimalMin(value = "-180.0", message = "Longitude must be between -180 and 180")
    @DecimalMax(value = "180.0", message = "Longitude must be between -180 and 180")
    private Double startLongitude;
    
    /**
     * Human-readable starting address
     */
    @NotBlank(message = "Start address is required")
    private String startAddress;
    
    /**
     * Destination latitude
     */
    @NotNull(message = "End latitude is required")
    @DecimalMin(value = "-90.0", message = "Latitude must be between -90 and 90")
    @DecimalMax(value = "90.0", message = "Latitude must be between -90 and 90")
    private Double endLatitude;
    
    /**
     * Destination longitude
     */
    @NotNull(message = "End longitude is required")
    @DecimalMin(value = "-180.0", message = "Longitude must be between -180 and 180")
    @DecimalMax(value = "180.0", message = "Longitude must be between -180 and 180")
    private Double endLongitude;
    
    /**
     * Human-readable destination address
     */
    @NotBlank(message = "End address is required")
    private String endAddress;
    
    /**
     * Preferred start times in HH:mm format (e.g., "08:30", "17:00")
     */
    @NotEmpty(message = "At least one preferred start time is required")
    private List<String> preferredStartTimes;
    
    /**
     * Flag indicating if this route is currently active
     */
    @lombok.Builder.Default
    private boolean isActive = true;
}
