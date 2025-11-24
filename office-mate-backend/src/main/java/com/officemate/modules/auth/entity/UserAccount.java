package com.officemate.modules.auth.entity;

import com.officemate.shared.entity.AuditableEntity;
import com.officemate.shared.enums.AccountStatus;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * JPA Entity representing a user account in the PostgreSQL database.
 * This entity stores core authentication and verification information.
 * Separate from MongoDB User model to support migration to PostgreSQL.
 */
@Entity
@Table(
    name = "user_accounts",
    indexes = {
        @Index(name = "idx_user_accounts_phone", columnList = "phone_number"),
        @Index(name = "idx_user_accounts_email", columnList = "corporate_email")
    }
)
@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserAccount extends AuditableEntity {

    /**
     * Unique identifier for the user account (UUID)
     */
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "user_id", updatable = false, nullable = false)
    private UUID userId;

    /**
     * User's phone number (unique, required for authentication)
     */
    @Column(name = "phone_number", unique = true, nullable = false, length = 20)
    private String phoneNumber;

    /**
     * Flag indicating if the phone number has been verified via OTP
     */
    @Column(name = "phone_verified", nullable = false)
    @Builder.Default
    private Boolean phoneVerified = false;

    /**
     * User's corporate email address (unique, optional until verified)
     */
    @Column(name = "corporate_email", unique = true, length = 255)
    private String corporateEmail;

    /**
     * Flag indicating if the corporate email has been verified via OTP
     */
    @Column(name = "email_verified", nullable = false)
    @Builder.Default
    private Boolean emailVerified = false;

    /**
     * Current status of the account (ACTIVE, SUSPENDED, PENDING_EMAIL)
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "account_status", nullable = false, length = 20)
    @Builder.Default
    private AccountStatus accountStatus = AccountStatus.PENDING_EMAIL;

    /**
     * Timestamp of the user's last login
     */
    @Column(name = "last_login_at")
    private LocalDateTime lastLoginAt;

    /**
     * Updates the last login timestamp to the current time
     */
    public void updateLastLogin() {
        this.lastLoginAt = LocalDateTime.now();
    }

    /**
     * Marks the phone number as verified
     */
    public void verifyPhone() {
        this.phoneVerified = true;
    }

    /**
     * Marks the corporate email as verified and updates account status
     */
    public void verifyEmail() {
        this.emailVerified = true;
        if (this.phoneVerified && this.accountStatus == AccountStatus.PENDING_EMAIL) {
            this.accountStatus = AccountStatus.ACTIVE;
        }
    }

    /**
     * Checks if the user has completed both phone and email verification
     * 
     * @return true if both phone and email are verified
     */
    public boolean isFullyVerified() {
        return phoneVerified && emailVerified;
    }

    /**
     * Checks if the account is active and fully verified
     * 
     * @return true if account status is ACTIVE
     */
    public boolean isActive() {
        return accountStatus == AccountStatus.ACTIVE;
    }

    /**
     * Suspends the account
     */
    public void suspend() {
        this.accountStatus = AccountStatus.SUSPENDED;
    }

    /**
     * Reactivates a suspended account (requires full verification)
     */
    public void reactivate() {
        if (isFullyVerified()) {
            this.accountStatus = AccountStatus.ACTIVE;
        }
    }
}
