package com.officemate.modules.auth.repository;

import com.officemate.modules.auth.entity.EmailChangeAuditLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Repository for EmailChangeAuditLog entity.
 * Provides data access methods for email change audit logs.
 */
@Repository
public interface EmailChangeAuditLogRepository extends JpaRepository<EmailChangeAuditLog, UUID> {

    /**
     * Find all audit logs for a specific user.
     *
     * @param userId the user ID
     * @return list of audit logs ordered by timestamp descending
     */
    List<EmailChangeAuditLog> findByUserIdOrderByChangedAtDesc(UUID userId);

    /**
     * Find audit logs for a user within a date range.
     *
     * @param userId the user ID
     * @param startDate start date
     * @param endDate end date
     * @return list of audit logs
     */
    @Query("SELECT e FROM EmailChangeAuditLog e WHERE e.userId = :userId " +
           "AND e.changedAt BETWEEN :startDate AND :endDate " +
           "ORDER BY e.changedAt DESC")
    List<EmailChangeAuditLog> findByUserIdAndDateRange(
        @Param("userId") UUID userId,
        @Param("startDate") LocalDateTime startDate,
        @Param("endDate") LocalDateTime endDate
    );

    /**
     * Find all audit logs for a specific email address.
     *
     * @param email the email address
     * @return list of audit logs
     */
    @Query("SELECT e FROM EmailChangeAuditLog e WHERE e.oldEmail = :email OR e.newEmail = :email " +
           "ORDER BY e.changedAt DESC")
    List<EmailChangeAuditLog> findByEmail(@Param("email") String email);

    /**
     * Count email changes for a user within a time period.
     *
     * @param userId the user ID
     * @param since the start date
     * @return count of changes
     */
    @Query("SELECT COUNT(e) FROM EmailChangeAuditLog e WHERE e.userId = :userId " +
           "AND e.changedAt >= :since AND e.status = 'COMPLETED'")
    long countCompletedChangesSince(@Param("userId") UUID userId, @Param("since") LocalDateTime since);
}
