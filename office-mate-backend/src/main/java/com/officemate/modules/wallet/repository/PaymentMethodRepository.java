package com.officemate.modules.wallet.repository;

import com.officemate.modules.wallet.entity.PaymentMethod;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository interface for PaymentMethod entity.
 * Provides database operations for payment method management.
 */
@Repository
public interface PaymentMethodRepository extends JpaRepository<PaymentMethod, UUID> {

    /**
     * Finds all payment methods for a wallet
     * 
     * @param walletId Wallet ID
     * @return List of payment methods
     */
    @Query("SELECT pm FROM PaymentMethod pm WHERE pm.wallet.walletId = :walletId")
    List<PaymentMethod> findByWalletWalletId(@Param("walletId") UUID walletId);

    /**
     * Finds the primary payment method for a wallet
     * 
     * @param walletId Wallet ID
     * @return Optional containing the primary payment method if found
     */
    @Query("SELECT pm FROM PaymentMethod pm WHERE pm.wallet.walletId = :walletId AND pm.isPrimary = true")
    Optional<PaymentMethod> findPrimaryByWalletWalletId(@Param("walletId") UUID walletId);

    /**
     * Finds all verified payment methods for a wallet
     * 
     * @param walletId Wallet ID
     * @return List of verified payment methods
     */
    @Query("SELECT pm FROM PaymentMethod pm WHERE pm.wallet.walletId = :walletId AND pm.isVerified = true")
    List<PaymentMethod> findVerifiedByWalletWalletId(@Param("walletId") UUID walletId);

    /**
     * Unmarks all payment methods as primary for a wallet
     * Used when setting a new primary payment method
     * 
     * @param walletId Wallet ID
     */
    @Modifying
    @Query("UPDATE PaymentMethod pm SET pm.isPrimary = false WHERE pm.wallet.walletId = :walletId")
    void unmarkAllAsPrimaryForWallet(@Param("walletId") UUID walletId);

    /**
     * Checks if a payment method exists for a wallet with the given identifier
     * 
     * @param walletId Wallet ID
     * @param identifier Payment method identifier
     * @return true if payment method exists
     */
    @Query("SELECT CASE WHEN COUNT(pm) > 0 THEN true ELSE false END FROM PaymentMethod pm WHERE pm.wallet.walletId = :walletId AND pm.identifier = :identifier")
    boolean existsByWalletWalletIdAndIdentifier(@Param("walletId") UUID walletId, @Param("identifier") String identifier);

    /**
     * Counts payment methods for a wallet
     * 
     * @param walletId Wallet ID
     * @return Number of payment methods
     */
    @Query("SELECT COUNT(pm) FROM PaymentMethod pm WHERE pm.wallet.walletId = :walletId")
    long countByWalletWalletId(@Param("walletId") UUID walletId);
}
