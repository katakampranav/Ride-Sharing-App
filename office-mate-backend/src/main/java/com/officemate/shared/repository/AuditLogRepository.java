package com.officemate.shared.repository;

import com.officemate.shared.entity.AuditLog;
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
 * Repository for managing audit log records
 */
@Repository
public interface AuditLogRepository extends JpaRepository<AuditLog, UUID> {

    /**
     * Find audit logs for a specific user
     */
    Page<AuditLog> findByUserIdOrderByTimestampDesc(UUID userId, Pageable pageable);

    /**
     * Find audit logs for a specific entity type
     */
    Page<AuditLog> findByEntityTypeOrderByTimestampDesc(String entityType, Pageable pageable);

    /**
     * Find audit logs for a specific entity
     */
    List<AuditLog> findByEntityTypeAndEntityIdOrderByTimestampDesc(String entityType, String entityId);

    /**
     * Find audit logs within a date range
     */
    @Query("SELECT a FROM AuditLog a WHERE a.timestamp BETWEEN :startDate AND :endDate ORDER BY a.timestamp DESC")
    Page<AuditLog> findByTimestampBetween(@Param("startDate") LocalDateTime startDate, 
                                         @Param("endDate") LocalDateTime endDate, 
                                         Pageable pageable);

    /**
     * Find audit logs for a specific user and entity type
     */
    Page<AuditLog> findByUserIdAndEntityTypeOrderByTimestampDesc(UUID userId, String entityType, Pageable pageable);

    /**
     * Count audit logs for a user within a time period
     */
    @Query("SELECT COUNT(a) FROM AuditLog a WHERE a.userId = :userId AND a.timestamp >= :since")
    long countByUserIdAndTimestampAfter(@Param("userId") UUID userId, @Param("since") LocalDateTime since);

    /**
     * Find recent profile changes for monitoring
     */
    @Query("SELECT a FROM AuditLog a WHERE a.entityType IN ('UserProfile', 'DriverProfile', 'RiderProfile') " +
           "AND a.timestamp >= :since ORDER BY a.timestamp DESC")
    List<AuditLog> findRecentProfileChanges(@Param("since") LocalDateTime since);
}