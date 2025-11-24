package com.officemate.modules.auth.repository;

import com.officemate.modules.auth.entity.SessionMetadata;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository for managing session metadata in PostgreSQL.
 * Provides persistent storage for session history and audit trails.
 */
@Repository
public interface SessionMetadataRepository extends JpaRepository<SessionMetadata, UUID> {
    
    /**
     * Find session metadata by session ID.
     *
     * @param sessionId the session ID
     * @return optional session metadata
     */
    Optional<SessionMetadata> findBySessionId(String sessionId);
    
    /**
     * Find all active sessions for a user.
     *
     * @param userId the user ID
     * @return list of active session metadata
     */
    List<SessionMetadata> findByUserIdAndActiveTrue(UUID userId);
    
    /**
     * Find all sessions (active and inactive) for a user.
     *
     * @param userId the user ID
     * @return list of all session metadata
     */
    List<SessionMetadata> findByUserIdOrderByCreatedAtDesc(UUID userId);
    
    /**
     * Find sessions by device ID.
     *
     * @param deviceId the device ID
     * @return list of session metadata
     */
    List<SessionMetadata> findByDeviceIdOrderByCreatedAtDesc(String deviceId);
    
    /**
     * Count active sessions for a user.
     *
     * @param userId the user ID
     * @return count of active sessions
     */
    long countByUserIdAndActiveTrue(UUID userId);
    
    /**
     * Find sessions that have been inactive for a specified duration.
     *
     * @param threshold the inactivity threshold
     * @return list of inactive session metadata
     */
    @Query("SELECT sm FROM SessionMetadata sm WHERE sm.active = true AND sm.lastActivityAt < :threshold")
    List<SessionMetadata> findInactiveSessions(@Param("threshold") LocalDateTime threshold);
    
    /**
     * Find sessions created within a time range.
     *
     * @param startTime start of time range
     * @param endTime end of time range
     * @return list of session metadata
     */
    @Query("SELECT sm FROM SessionMetadata sm WHERE sm.createdAt BETWEEN :startTime AND :endTime ORDER BY sm.createdAt DESC")
    List<SessionMetadata> findSessionsInTimeRange(
        @Param("startTime") LocalDateTime startTime,
        @Param("endTime") LocalDateTime endTime
    );
    
    /**
     * Delete old session metadata records.
     *
     * @param threshold the date threshold
     * @return number of deleted records
     */
    @Query("DELETE FROM SessionMetadata sm WHERE sm.active = false AND sm.endedAt < :threshold")
    int deleteOldInactiveSessions(@Param("threshold") LocalDateTime threshold);
}
