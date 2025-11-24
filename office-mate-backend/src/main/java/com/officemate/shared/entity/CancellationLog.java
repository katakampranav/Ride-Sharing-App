package com.officemate.shared.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Entity for tracking ride cancellations and enforcing suspension policies
 * Tracks driver cancellations to enforce 3-month suspension after 5 cancellations per month
 */
@Entity
@Table(name = "cancellation_logs", indexes = {
    @Index(name = "idx_cancellation_user_id", columnList = "user_id"),
    @Index(name = "idx_cancellation_timestamp", columnList = "timestamp"),
    @Index(name = "idx_cancellation_month_year", columnList = "cancellation_month, cancellation_year"),
    @Index(name = "idx_cancellation_type", columnList = "cancellation_type")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CancellationLog {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "cancellation_id")
    private UUID cancellationId;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "ride_id")
    private UUID rideId;

    @Column(name = "cancellation_type", nullable = false, length = 20)
    private String cancellationType; // DRIVER, RIDER

    @Column(name = "cancellation_reason", length = 200)
    private String cancellationReason;

    @Column(name = "timestamp", nullable = false)
    private LocalDateTime timestamp;

    @Column(name = "cancellation_month", nullable = false)
    private Integer cancellationMonth;

    @Column(name = "cancellation_year", nullable = false)
    private Integer cancellationYear;

    @Column(name = "minutes_before_ride")
    private Integer minutesBeforeRide;

    @Column(name = "penalty_applied", nullable = false)
    @Builder.Default
    private Boolean penaltyApplied = false;

    @Column(name = "penalty_type", length = 50)
    private String penaltyType; // WARNING, SUSPENSION

    @Column(name = "penalty_duration_days")
    private Integer penaltyDurationDays;

    @Column(name = "penalty_start_date")
    private LocalDateTime penaltyStartDate;

    @Column(name = "penalty_end_date")
    private LocalDateTime penaltyEndDate;

    @Column(name = "ip_address", length = 45)
    private String ipAddress;

    @Column(name = "user_agent", length = 500)
    private String userAgent;

    @Column(name = "additional_notes", columnDefinition = "TEXT")
    private String additionalNotes;

    @PrePersist
    protected void onCreate() {
        if (timestamp == null) {
            timestamp = LocalDateTime.now();
            cancellationMonth = timestamp.getMonthValue();
            cancellationYear = timestamp.getYear();
        }
        if (penaltyApplied == null) {
            penaltyApplied = false;
        }
    }
}