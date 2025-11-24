package com.officemate.shared.repository;

import com.officemate.shared.entity.SecurityEventLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Repository for managing security event log records
 */
@Repository
public interface SecurityEventLogRepository extends JpaRepository<SecurityEventLog, UUID> {

    /**
     * Find security events for a specific user
     */
    Page<SecurityEventLog> findByUserIdOrderByTimestampDesc(UUID userId, Pageable pageable);

    /**
     * Find security events by phone number (for tracking before user creation)
     */
    Page<SecurityEventLog> findByPhoneNumberOrderByTimestampDesc(String phoneNumber, Pageable pageable);

    /**
     * Find security events by event type
     */
    Page<SecurityEventLog> findByEventTypeOrderByTimestampDesc(String eventType, Pageable pageable);

    /**
     * Find security events by IP address
     */
    Page<SecurityEventLog> findByIpAddressOrderByTimestampDesc(String ipAddress, Pageable pageable);

    /**
     * Find unresolved security events
     */
    Page<SecurityEventLog> findByResolvedFalseOrderByTimestampDesc(Pageable pageable);

    /**
     * Find high severity security events
     */
    Page<SecurityEventLog> findBySeverityOrderByTimestampDesc(String severity, Pageable pageable);

    /**
     * Count failed login attempts for a phone number within a time period
     */
    @Query("SELECT COUNT(s) FROM SecurityEventLog s WHERE s.phoneNumber = :phoneNumber " +
           "AND s.eventType = 'LOGIN_FAILURE' AND s.timestamp >= :since")
    long countFailedLoginAttempts(@Param("phoneNumber") String phoneNumber, @Param("since") LocalDateTime since);

    /**
     * Count failed OTP attempts for a user within a time period
     */
    @Query("SELECT COUNT(s) FROM SecurityEventLog s WHERE s.userId = :userId " +
           "AND s.eventType = 'OTP_FAILURE' AND s.timestamp >= :since")
    long countFailedOtpAttempts(@Param("userId") UUID userId, @Param("since") LocalDateTime since);

    /**
     * Find recent suspicious activities
     */
    @Query("SELECT s FROM SecurityEventLog s WHERE s.severity IN ('HIGH', 'CRITICAL') " +
           "AND s.resolved = false AND s.timestamp >= :since ORDER BY s.timestamp DESC")
    List<SecurityEventLog> findRecentSuspiciousActivities(@Param("since") LocalDateTime since);

    /**
     * Find security events within a date range
     */
    @Query("SELECT s FROM SecurityEventLog s WHERE s.timestamp BETWEEN :startDate AND :endDate ORDER BY s.timestamp DESC")
    Page<SecurityEventLog> findByTimestampBetween(@Param("startDate") LocalDateTime startDate, 
                                                 @Param("endDate") LocalDateTime endDate, 
                                                 Pageable pageable);

    /**
     * Count security events by type within a time period
     */
    @Query("SELECT COUNT(s) FROM SecurityEventLog s WHERE s.eventType = :eventType AND s.timestamp >= :since")
    long countByEventTypeAndTimestampAfter(@Param("eventType") String eventType, @Param("since") LocalDateTime since);
}