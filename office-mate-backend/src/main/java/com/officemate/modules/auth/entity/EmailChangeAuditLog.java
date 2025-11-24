package com.officemate.modules.auth.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * JPA Entity for auditing corporate email changes.
 * Tracks all email update operations for security and compliance.
 */
@Entity
@Table(
    name = "email_change_audit_logs",
    indexes = {
        @Index(name = "idx_email_audit_user", columnList = "user_id"),
        @Index(name = "idx_email_audit_timestamp", columnList = "changed_at")
    }
)
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EmailChangeAuditLog {

    /**
     * Unique identifier for the audit log entry
     */
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "audit_id", updatable = false, nullable = false)
    private UUID auditId;

    /**
     * User ID who performed the email change
     */
    @Column(name = "user_id", nullable = false)
    private UUID userId;

    /**
     * Previous corporate email (null if first email addition)
     */
    @Column(name = "old_email", length = 255)
    private String oldEmail;

    /**
     * New corporate email (null if email removal)
     */
    @Column(name = "new_email", length = 255)
    private String newEmail;

    /**
     * Type of change operation
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "change_type", nullable = false, length = 20)
    private ChangeType changeType;

    /**
     * Reason for the change (e.g., "Company change", "Correction")
     */
    @Column(name = "change_reason", length = 500)
    private String changeReason;

    /**
     * IP address from which the change was initiated
     */
    @Column(name = "ip_address", length = 45)
    private String ipAddress;

    /**
     * User agent string of the client
     */
    @Column(name = "user_agent", length = 500)
    private String userAgent;

    /**
     * Whether mobile OTP verification was completed
     */
    @Column(name = "mobile_otp_verified", nullable = false)
    @Builder.Default
    private Boolean mobileOtpVerified = false;

    /**
     * Whether new email OTP verification was completed
     */
    @Column(name = "email_otp_verified", nullable = false)
    @Builder.Default
    private Boolean emailOtpVerified = false;

    /**
     * Timestamp when the change was performed
     */
    @CreationTimestamp
    @Column(name = "changed_at", nullable = false, updatable = false)
    private LocalDateTime changedAt;

    /**
     * Status of the change operation
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    @Builder.Default
    private Status status = Status.INITIATED;

    /**
     * Additional notes or error messages
     */
    @Column(name = "notes", length = 1000)
    private String notes;

    /**
     * Enum for change types
     */
    public enum ChangeType {
        ADDITION,      // First time adding email
        UPDATE,        // Changing from one email to another
        REMOVAL        // Removing email
    }

    /**
     * Enum for change status
     */
    public enum Status {
        INITIATED,     // Change process started
        MOBILE_VERIFIED,  // Mobile OTP verified
        EMAIL_VERIFIED,   // Email OTP verified
        COMPLETED,     // Change successfully completed
        FAILED         // Change failed
    }

    /**
     * Mark the change as mobile verified
     */
    public void markMobileVerified() {
        this.mobileOtpVerified = true;
        this.status = Status.MOBILE_VERIFIED;
    }

    /**
     * Mark the change as email verified
     */
    public void markEmailVerified() {
        this.emailOtpVerified = true;
        this.status = Status.EMAIL_VERIFIED;
    }

    /**
     * Mark the change as completed
     */
    public void markCompleted() {
        this.status = Status.COMPLETED;
    }

    /**
     * Mark the change as failed with notes
     */
    public void markFailed(String errorMessage) {
        this.status = Status.FAILED;
        this.notes = errorMessage;
    }
}
