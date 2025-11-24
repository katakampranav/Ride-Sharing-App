package com.officemate.modules.safety.repository;

import com.officemate.modules.safety.entity.SOSAlert;
import com.officemate.modules.safety.entity.SOSAlert.SOSStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository interface for SOSAlert entity operations.
 * Provides database access methods for SOS emergency alerts.
 */
@Repository
public interface SOSAlertRepository extends JpaRepository<SOSAlert, UUID> {

    /**
     * Find all SOS alerts for a specific user
     * 
     * @param userId the user's ID
     * @return list of SOS alerts
     */
    List<SOSAlert> findByUserId(UUID userId);

    /**
     * Find all active SOS alerts for a specific user
     * 
     * @param userId the user's ID
     * @param status the alert status
     * @return list of active SOS alerts
     */
    List<SOSAlert> findByUserIdAndStatus(UUID userId, SOSStatus status);

    /**
     * Find the most recent active SOS alert for a user
     * 
     * @param userId the user's ID
     * @param status the alert status
     * @return optional SOS alert
     */
    Optional<SOSAlert> findFirstByUserIdAndStatusOrderByCreatedAtDesc(UUID userId, SOSStatus status);

    /**
     * Find SOS alert by alert ID and user ID
     * 
     * @param alertId the alert's ID
     * @param userId the user's ID
     * @return optional SOS alert
     */
    Optional<SOSAlert> findByAlertIdAndUserId(UUID alertId, UUID userId);

    /**
     * Find all SOS alerts for a specific ride
     * 
     * @param rideId the ride's ID
     * @return list of SOS alerts
     */
    List<SOSAlert> findByRideId(UUID rideId);

    /**
     * Find all active SOS alerts
     * 
     * @param status the alert status
     * @return list of active SOS alerts
     */
    List<SOSAlert> findByStatus(SOSStatus status);

    /**
     * Find all SOS alerts created within a time range
     * 
     * @param startTime the start time
     * @param endTime the end time
     * @return list of SOS alerts
     */
    @Query("SELECT s FROM SOSAlert s WHERE s.createdAt BETWEEN :startTime AND :endTime ORDER BY s.createdAt DESC")
    List<SOSAlert> findAlertsInTimeRange(@Param("startTime") LocalDateTime startTime, @Param("endTime") LocalDateTime endTime);

    /**
     * Count active SOS alerts for a user
     * 
     * @param userId the user's ID
     * @param status the alert status
     * @return count of active alerts
     */
    long countByUserIdAndStatus(UUID userId, SOSStatus status);

    /**
     * Check if user has any active SOS alerts
     * 
     * @param userId the user's ID
     * @param status the alert status
     * @return true if user has active alerts
     */
    boolean existsByUserIdAndStatus(UUID userId, SOSStatus status);

    /**
     * Delete all SOS alerts for a user
     * 
     * @param userId the user's ID
     */
    void deleteByUserId(UUID userId);
}
