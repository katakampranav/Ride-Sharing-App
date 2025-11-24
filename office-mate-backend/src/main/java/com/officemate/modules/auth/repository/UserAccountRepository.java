package com.officemate.modules.auth.repository;

import com.officemate.modules.auth.entity.UserAccount;
import com.officemate.shared.enums.AccountStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

/**
 * Repository interface for UserAccount entity.
 * Provides database access methods for user account operations.
 */
@Repository
public interface UserAccountRepository extends JpaRepository<UserAccount, UUID> {

    /**
     * Find a user account by phone number
     * 
     * @param phoneNumber the phone number to search for
     * @return Optional containing the user account if found
     */
    Optional<UserAccount> findByPhoneNumber(String phoneNumber);

    /**
     * Find a user account by corporate email
     * 
     * @param corporateEmail the corporate email to search for
     * @return Optional containing the user account if found
     */
    Optional<UserAccount> findByCorporateEmail(String corporateEmail);

    /**
     * Check if a phone number already exists in the system
     * 
     * @param phoneNumber the phone number to check
     * @return true if the phone number exists
     */
    boolean existsByPhoneNumber(String phoneNumber);

    /**
     * Check if a corporate email already exists in the system
     * 
     * @param corporateEmail the corporate email to check
     * @return true if the corporate email exists
     */
    boolean existsByCorporateEmail(String corporateEmail);

    /**
     * Find all user accounts with a specific account status
     * 
     * @param accountStatus the account status to filter by
     * @return list of user accounts with the specified status
     */
    @Query("SELECT ua FROM UserAccount ua WHERE ua.accountStatus = :status")
    java.util.List<UserAccount> findByAccountStatus(@Param("status") AccountStatus accountStatus);

    /**
     * Find all fully verified user accounts (phone and email verified)
     * 
     * @return list of fully verified user accounts
     */
    @Query("SELECT ua FROM UserAccount ua WHERE ua.phoneVerified = true AND ua.emailVerified = true")
    java.util.List<UserAccount> findFullyVerifiedAccounts();

    /**
     * Find user accounts pending email verification
     * 
     * @return list of accounts with verified phone but unverified email
     */
    @Query("SELECT ua FROM UserAccount ua WHERE ua.phoneVerified = true AND ua.emailVerified = false")
    java.util.List<UserAccount> findPendingEmailVerification();
}
