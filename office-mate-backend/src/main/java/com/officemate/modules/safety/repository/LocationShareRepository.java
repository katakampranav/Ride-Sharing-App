package com.officemate.modules.safety.repository;

import com.officemate.modules.safety.entity.LocationShare;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository interface for LocationShare entity operations.
 * Provides database access methods for real-time location sharing sessions.
 */
@Repository
public interface LocationShareRepository extends JpaRepository<LocationShare, UUID> {

    /**
     * Find all location shares for a specific user
     * 
     * @param userId the user's ID
     * @return list of location shares
     */
    List<LocationShare> findByUserId(UUID userId);

    /**
     * Find all active location shares for a specific user
     * 
     * @param userId the user's ID
     * @param isActive the active status
     * @return list of active location shares
     */
    List<LocationShare> findByUserIdAndIsActive(UUID userId, Boolean isActive);

    /**
     * Find the most recent active location share for a user
     * 
     * @param userId the user's ID
     * @param isActive the active status
     * @return optional location share
     */
    Optional<LocationShare> findFirstByUserIdAndIsActiveOrderByCreatedAtDesc(UUID userId, Boolean isActive);

    /**
     * Find location share by share ID and user ID
     * 
     * @param shareId the share's ID
     * @param userId the user's ID
     * @return optional location share
     */
    Optional<LocationShare> findByShareIdAndUserId(UUID shareId, UUID userId);

    /**
     * Find location share by share token
     * 
     * @param shareToken the share token
     * @return optional location share
     */
    Optional<LocationShare> findByShareToken(String shareToken);

    /**
     * Find all location shares for a specific ride
     * 
     * @param rideId the ride's ID
     * @return list of location shares
     */
    List<LocationShare> findByRideId(UUID rideId);

    /**
     * Find active location share for a specific ride
     * 
     * @param rideId the ride's ID
     * @param isActive the active status
     * @return optional location share
     */
    Optional<LocationShare> findByRideIdAndIsActive(UUID rideId, Boolean isActive);

    /**
     * Count active location shares for a user
     * 
     * @param userId the user's ID
     * @param isActive the active status
     * @return count of active shares
     */
    long countByUserIdAndIsActive(UUID userId, Boolean isActive);

    /**
     * Check if user has any active location shares
     * 
     * @param userId the user's ID
     * @param isActive the active status
     * @return true if user has active shares
     */
    boolean existsByUserIdAndIsActive(UUID userId, Boolean isActive);

    /**
     * End all active location shares for a user
     * 
     * @param userId the user's ID
     */
    @Modifying
    @Query("UPDATE LocationShare l SET l.isActive = false, l.endedAt = CURRENT_TIMESTAMP WHERE l.userId = :userId AND l.isActive = true")
    void endAllActiveSharesForUser(@Param("userId") UUID userId);

    /**
     * Delete all location shares for a user
     * 
     * @param userId the user's ID
     */
    void deleteByUserId(UUID userId);
}
