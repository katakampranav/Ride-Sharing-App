package com.officemate.shared.repository;

import com.officemate.shared.entity.CancellationLog;
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
 * Repository for managing cancellation log records and suspension policy enforcement
 */
@Repository
public interface CancellationLogRepository extends JpaRepository<CancellationLog, UUID> {

    /**
     * Find cancellations for a specific user
     */
    Page<CancellationLog> findByUserIdOrderByTimestampDesc(UUID userId, Pageable pageable);

    /**
     * Count driver cancellations for a user in a specific month and year
     */
    @Query("SELECT COUNT(c) FROM CancellationLog c WHERE c.userId = :userId " +
           "AND c.cancellationType = 'DRIVER' AND c.cancellationMonth = :month AND c.cancellationYear = :year")
    long countDriverCancellationsInMonth(@Param("userId") UUID userId, 
                                        @Param("month") Integer month, 
                                        @Param("year") Integer year);

    /**
     * Find all driver cancellations for a user in a specific month and year
     */
    @Query("SELECT c FROM CancellationLog c WHERE c.userId = :userId " +
           "AND c.cancellationType = 'DRIVER' AND c.cancellationMonth = :month AND c.cancellationYear = :year " +
           "ORDER BY c.timestamp DESC")
    List<CancellationLog> findDriverCancellationsInMonth(@Param("userId") UUID userId, 
                                                        @Param("month") Integer month, 
                                                        @Param("year") Integer year);

    /**
     * Find cancellations by type
     */
    Page<CancellationLog> findByCancellationTypeOrderByTimestampDesc(String cancellationType, Pageable pageable);

    /**
     * Find cancellations with penalties applied
     */
    Page<CancellationLog> findByPenaltyAppliedTrueOrderByTimestampDesc(Pageable pageable);

    /**
     * Find active suspensions (penalties that haven't ended yet)
     */
    @Query("SELECT c FROM CancellationLog c WHERE c.penaltyApplied = true " +
           "AND c.penaltyType = 'SUSPENSION' AND c.penaltyEndDate > :currentTime " +
           "ORDER BY c.penaltyEndDate ASC")
    List<CancellationLog> findActiveSuspensions(@Param("currentTime") LocalDateTime currentTime);

    /**
     * Check if a user is currently suspended
     */
    @Query("SELECT COUNT(c) > 0 FROM CancellationLog c WHERE c.userId = :userId " +
           "AND c.penaltyApplied = true AND c.penaltyType = 'SUSPENSION' " +
           "AND c.penaltyEndDate > :currentTime")
    boolean isUserSuspended(@Param("userId") UUID userId, @Param("currentTime") LocalDateTime currentTime);

    /**
     * Find cancellations within a date range
     */
    @Query("SELECT c FROM CancellationLog c WHERE c.timestamp BETWEEN :startDate AND :endDate ORDER BY c.timestamp DESC")
    Page<CancellationLog> findByTimestampBetween(@Param("startDate") LocalDateTime startDate, 
                                               @Param("endDate") LocalDateTime endDate, 
                                               Pageable pageable);

    /**
     * Find users who are approaching the cancellation limit
     */
    @Query("SELECT c.userId, COUNT(c) as cancellationCount FROM CancellationLog c " +
           "WHERE c.cancellationType = 'DRIVER' AND c.cancellationMonth = :month AND c.cancellationYear = :year " +
           "GROUP BY c.userId HAVING COUNT(c) >= :threshold")
    List<Object[]> findUsersApproachingCancellationLimit(@Param("month") Integer month, 
                                                        @Param("year") Integer year, 
                                                        @Param("threshold") Long threshold);

    /**
     * Count total cancellations for a user
     */
    long countByUserId(UUID userId);

    /**
     * Count cancellations by type for a user
     */
    long countByUserIdAndCancellationType(UUID userId, String cancellationType);
}