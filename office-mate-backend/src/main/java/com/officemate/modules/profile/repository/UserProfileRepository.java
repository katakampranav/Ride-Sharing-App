package com.officemate.modules.profile.repository;

import com.officemate.modules.profile.entity.UserProfile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

/**
 * Spring Data JPA repository for UserProfile entity.
 * Provides CRUD operations and custom queries for user profile management.
 */
@Repository
public interface UserProfileRepository extends JpaRepository<UserProfile, UUID> {

    /**
     * Finds a user profile by user ID with UserAccount eagerly loaded
     * 
     * @param userId The user's unique identifier
     * @return Optional containing the user profile if found
     */
    @Query("SELECT up FROM UserProfile up JOIN FETCH up.userAccount WHERE up.userId = :userId")
    Optional<UserProfile> findByIdWithUserAccount(@Param("userId") UUID userId);

    /**
     * Checks if a user profile exists for the given user ID
     * 
     * @param userId The user's unique identifier
     * @return true if profile exists
     */
    boolean existsByUserId(UUID userId);

    /**
     * Finds a user profile by the associated user account's phone number
     * 
     * @param phoneNumber The phone number to search for
     * @return Optional containing the user profile if found
     */
    @Query("SELECT up FROM UserProfile up JOIN up.userAccount ua WHERE ua.phoneNumber = :phoneNumber")
    Optional<UserProfile> findByPhoneNumber(@Param("phoneNumber") String phoneNumber);

    /**
     * Finds a user profile by the associated user account's corporate email
     * 
     * @param corporateEmail The corporate email to search for
     * @return Optional containing the user profile if found
     */
    @Query("SELECT up FROM UserProfile up JOIN up.userAccount ua WHERE ua.corporateEmail = :corporateEmail")
    Optional<UserProfile> findByCorporateEmail(@Param("corporateEmail") String corporateEmail);
}
