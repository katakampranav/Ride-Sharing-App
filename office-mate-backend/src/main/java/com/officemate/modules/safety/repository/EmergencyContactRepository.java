package com.officemate.modules.safety.repository;

import com.officemate.modules.safety.entity.EmergencyContact;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository interface for EmergencyContact entity.
 * Provides database operations for emergency contact management.
 */
@Repository
public interface EmergencyContactRepository extends JpaRepository<EmergencyContact, UUID> {

    /**
     * Find all emergency contacts for a specific user
     * 
     * @param userId the user's ID
     * @return list of emergency contacts
     */
    List<EmergencyContact> findByUserId(UUID userId);

    /**
     * Find the primary emergency contact for a specific user
     * 
     * @param userId the user's ID
     * @return optional containing the primary contact if exists
     */
    Optional<EmergencyContact> findByUserIdAndIsPrimaryTrue(UUID userId);

    /**
     * Count the number of emergency contacts for a specific user
     * 
     * @param userId the user's ID
     * @return count of emergency contacts
     */
    long countByUserId(UUID userId);

    /**
     * Check if a user has a primary emergency contact
     * 
     * @param userId the user's ID
     * @return true if a primary contact exists
     */
    boolean existsByUserIdAndIsPrimaryTrue(UUID userId);

    /**
     * Delete all emergency contacts for a specific user
     * 
     * @param userId the user's ID
     */
    void deleteByUserId(UUID userId);

    /**
     * Unmark all contacts as primary for a specific user
     * This is used before setting a new primary contact
     * 
     * @param userId the user's ID
     */
    @Modifying
    @Query("UPDATE EmergencyContact ec SET ec.isPrimary = false WHERE ec.userId = :userId")
    void unmarkAllAsPrimaryForUser(@Param("userId") UUID userId);

    /**
     * Find a specific emergency contact by contact ID and user ID
     * This ensures users can only access their own contacts
     * 
     * @param contactId the contact's ID
     * @param userId the user's ID
     * @return optional containing the contact if exists
     */
    Optional<EmergencyContact> findByContactIdAndUserId(UUID contactId, UUID userId);
}
