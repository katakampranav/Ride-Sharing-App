package com.officemate.modules.safety.repository;

import com.officemate.modules.safety.entity.FamilySharingContact;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository interface for FamilySharingContact entity.
 * Provides database operations for family sharing contact management.
 */
@Repository
public interface FamilySharingContactRepository extends JpaRepository<FamilySharingContact, UUID> {

    /**
     * Find all family sharing contacts for a specific user
     * 
     * @param userId the user's ID
     * @return list of family sharing contacts
     */
    List<FamilySharingContact> findByUserId(UUID userId);

    /**
     * Find all family sharing contacts that have ride updates enabled for a specific user
     * 
     * @param userId the user's ID
     * @return list of family sharing contacts with ride updates enabled
     */
    List<FamilySharingContact> findByUserIdAndReceiveRideUpdatesTrue(UUID userId);

    /**
     * Count the number of family sharing contacts for a specific user
     * 
     * @param userId the user's ID
     * @return count of family sharing contacts
     */
    long countByUserId(UUID userId);

    /**
     * Delete all family sharing contacts for a specific user
     * 
     * @param userId the user's ID
     */
    void deleteByUserId(UUID userId);

    /**
     * Find a specific family sharing contact by sharing ID and user ID
     * This ensures users can only access their own contacts
     * 
     * @param sharingId the sharing contact's ID
     * @param userId the user's ID
     * @return optional containing the contact if exists
     */
    Optional<FamilySharingContact> findBySharingIdAndUserId(UUID sharingId, UUID userId);
}
