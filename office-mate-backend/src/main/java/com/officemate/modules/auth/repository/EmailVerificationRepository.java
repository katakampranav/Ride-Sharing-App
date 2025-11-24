package com.officemate.modules.auth.repository;

import com.officemate.modules.auth.entity.EmailVerification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository interface for EmailVerification entity.
 * Provides database access methods for email verification operations.
 */
@Repository
public interface EmailVerificationRepository extends JpaRepository<EmailVerification, UUID> {

    /**
     * Find the most recent email verification record for a user
     * 
     * @param userId the user ID to search for
     * @return Optional containing the most recent verification record if found
     */
    @Query("SELECT ev FROM EmailVerification ev WHERE ev.userId = :userId " +
           "ORDER BY ev.createdAt DESC LIMIT 1")
    Optional<EmailVerification> findLatestByUserId(@Param("userId") UUID userId);

    /**
     * Find all email verification records for a specific user
     * 
     * @param userId the user ID to search for
     * @return list of email verification records for the user
     */
    List<EmailVerification> findByUserId(UUID userId);

    /**
     * Find email verification record by user ID and corporate email
     * 
     * @param userId the user ID
     * @param corporateEmail the corporate email
     * @return Optional containing the verification record if found
     */
    Optional<EmailVerification> findByUserIdAndCorporateEmail(UUID userId, String corporateEmail);

    /**
     * Find all unverified email verification records for a user
     * 
     * @param userId the user ID
     * @return list of unverified email verification records
     */
    @Query("SELECT ev FROM EmailVerification ev WHERE ev.userId = :userId AND ev.verified = false")
    List<EmailVerification> findUnverifiedByUserId(@Param("userId") UUID userId);

    /**
     * Find all expired email verification records
     * 
     * @param currentTime the current timestamp
     * @return list of expired verification records
     */
    @Query("SELECT ev FROM EmailVerification ev WHERE ev.expiresAt < :currentTime AND ev.verified = false")
    List<EmailVerification> findExpiredRecords(@Param("currentTime") LocalDateTime currentTime);

    /**
     * Delete all expired email verification records (cleanup job)
     * 
     * @param currentTime the current timestamp
     * @return number of records deleted
     */
    @Modifying
    @Query("DELETE FROM EmailVerification ev WHERE ev.expiresAt < :currentTime AND ev.verified = false")
    int deleteExpiredRecords(@Param("currentTime") LocalDateTime currentTime);

    /**
     * Delete all email verification records for a specific user
     * 
     * @param userId the user ID
     */
    @Modifying
    @Query("DELETE FROM EmailVerification ev WHERE ev.userId = :userId")
    void deleteByUserId(@Param("userId") UUID userId);

    /**
     * Check if there are any active (non-expired, unverified) verification records for a user
     * 
     * @param userId the user ID
     * @param currentTime the current timestamp
     * @return true if active verification records exist
     */
    @Query("SELECT COUNT(ev) > 0 FROM EmailVerification ev " +
           "WHERE ev.userId = :userId AND ev.verified = false AND ev.expiresAt > :currentTime")
    boolean hasActiveVerification(@Param("userId") UUID userId, @Param("currentTime") LocalDateTime currentTime);

    /**
     * Find the most recent active (non-expired, unverified) verification record for a user
     * 
     * @param userId the user ID
     * @param currentTime the current timestamp
     * @return Optional containing the active verification record if found
     */
    @Query("SELECT ev FROM EmailVerification ev " +
           "WHERE ev.userId = :userId AND ev.verified = false AND ev.expiresAt > :currentTime " +
           "ORDER BY ev.createdAt DESC LIMIT 1")
    Optional<EmailVerification> findActiveVerificationByUserId(
        @Param("userId") UUID userId, 
        @Param("currentTime") LocalDateTime currentTime
    );
}
