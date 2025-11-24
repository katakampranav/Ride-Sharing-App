package com.officemate.modules.wallet.repository;

import com.officemate.modules.wallet.entity.Wallet;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

/**
 * Repository interface for Wallet entity.
 * Provides database operations for wallet management.
 */
@Repository
public interface WalletRepository extends JpaRepository<Wallet, UUID> {

    /**
     * Finds a wallet by user account ID
     * 
     * @param userId User account ID
     * @return Optional containing the wallet if found
     */
    @Query("SELECT w FROM Wallet w WHERE w.userAccount.userId = :userId")
    Optional<Wallet> findByUserAccountUserId(@Param("userId") UUID userId);

    /**
     * Checks if a wallet exists for a user account
     * 
     * @param userId User account ID
     * @return true if wallet exists
     */
    @Query("SELECT CASE WHEN COUNT(w) > 0 THEN true ELSE false END FROM Wallet w WHERE w.userAccount.userId = :userId")
    boolean existsByUserAccountUserId(@Param("userId") UUID userId);

    /**
     * Finds a wallet with payment methods eagerly loaded
     * 
     * @param userId User account ID
     * @return Optional containing the wallet with payment methods
     */
    @Query("SELECT w FROM Wallet w LEFT JOIN FETCH w.paymentMethods WHERE w.userAccount.userId = :userId")
    Optional<Wallet> findByUserAccountUserIdWithPaymentMethods(@Param("userId") UUID userId);
}
