package com.officemate.shared.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Entity for storing security-related events such as failed login attempts,
 * suspicious activities, and authentication events
 */
@Entity
@Table(name = "security_event_logs", indexes = {
    @Index(name = "idx_security_user_id", columnList = "user_id"),
    @Index(name = "idx_security_event_type", columnList = "event_type"),
    @Index(name = "idx_security_timestamp", columnList = "timestamp"),
    @Index(name = "idx_security_ip_address", columnList = "ip_address"),
    @Index(name = "idx_security_phone_number", columnList = "phone_number")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SecurityEventLog {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "event_id")
    private UUID eventId;

    @Column(name = "user_id")
    private UUID userId;

    @Column(name = "phone_number", length = 20)
    private String phoneNumber;

    @Column(name = "corporate_email", length = 255)
    private String corporateEmail;

    @Column(name = "event_type", nullable = false, length = 50)
    private String eventType; // LOGIN_SUCCESS, LOGIN_FAILURE, OTP_FAILURE, ACCOUNT_LOCKED, etc.

    @Column(name = "event_description", nullable = false, length = 500)
    private String eventDescription;

    @Column(name = "ip_address", length = 45)
    private String ipAddress;

    @Column(name = "user_agent", length = 500)
    private String userAgent;

    @Column(name = "session_id", length = 100)
    private String sessionId;

    @Column(name = "timestamp", nullable = false)
    private LocalDateTime timestamp;

    @Column(name = "severity", length = 20)
    private String severity; // LOW, MEDIUM, HIGH, CRITICAL

    @Column(name = "additional_data", columnDefinition = "TEXT")
    private String additionalData; // JSON string for additional context

    @Column(name = "resolved", nullable = false)
    @Builder.Default
    private Boolean resolved = false;

    @Column(name = "resolved_at")
    private LocalDateTime resolvedAt;

    @Column(name = "resolved_by", length = 50)
    private String resolvedBy;

    @PrePersist
    protected void onCreate() {
        if (timestamp == null) {
            timestamp = LocalDateTime.now();
        }
        if (resolved == null) {
            resolved = false;
        }
    }
}