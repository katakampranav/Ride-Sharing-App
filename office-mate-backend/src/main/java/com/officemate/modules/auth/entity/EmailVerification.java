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
 * JPA Entity representing an email verification record in the PostgreSQL database.
 * This entity stores OTP-based corporate email verification information with expiry.
 * Records expire after 10 minutes and are automatically cleaned up.
 */
@Entity
@Table(
    name = "email_verifications",
    indexes = {
        @Index(name = "idx_email_verifications_user", columnList = "user_id"),
        @Index(name = "idx_email_verifications_expires", columnList = "expires_at")
    }
)
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EmailVerification {

    /**
     * Unique identifier for the email verification record (UUID)
     */
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "verification_id", updatable = false, nullable = false)
    private UUID verificationId;

    /**
     * Reference to the user account attempting email verification
     */
    @Column(name = "user_id", nullable = false)
    private UUID userId;

    /**
     * Corporate email address being verified
     */
    @Column(name = "corporate_email", nullable = false, length = 255)
    private String corporateEmail;

    /**
     * Hashed OTP for security (never store plain text OTP)
     */
    @Column(name = "otp_hash", nullable = false, length = 255)
    private String otpHash;

    /**
     * Number of verification attempts made
     */
    @Column(name = "attempts", nullable = false)
    @Builder.Default
    private Integer attempts = 0;

    /**
     * Flag indicating if the email has been successfully verified
     */
    @Column(name = "verified", nullable = false)
    @Builder.Default
    private Boolean verified = false;

    /**
     * Timestamp when the verification record was created
     */
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    /**
     * Timestamp when the verification record expires (10 minutes from creation)
     */
    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;

    /**
     * Pre-persist callback to set expiry timestamp to 10 minutes from creation
     */
    @PrePersist
    protected void onCreate() {
        if (expiresAt == null) {
            expiresAt = LocalDateTime.now().plusMinutes(10);
        }
    }

    /**
     * Increments the attempt counter
     */
    public void incrementAttempts() {
        this.attempts++;
    }

    /**
     * Marks the email as verified
     */
    public void markAsVerified() {
        this.verified = true;
    }

    /**
     * Checks if the verification record has expired
     * 
     * @return true if the current time is after the expiry timestamp
     */
    public boolean isExpired() {
        return LocalDateTime.now().isAfter(expiresAt);
    }

    /**
     * Checks if the maximum number of attempts has been reached
     * 
     * @param maxAttempts the maximum allowed attempts
     * @return true if attempts have reached or exceeded the limit
     */
    public boolean hasExceededAttempts(int maxAttempts) {
        return attempts >= maxAttempts;
    }
}
