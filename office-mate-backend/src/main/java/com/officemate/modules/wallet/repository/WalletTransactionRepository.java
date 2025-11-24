package com.officemate.modules.wallet.repository;

import com.officemate.modules.wallet.entity.WalletTransaction;
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
 * Repository interface for WalletTransaction entity.
 * Provides database operations for wallet transaction management.
 */
@Repository
public interface WalletTransactionRepository extends JpaRepository<WalletTransaction, UUID> {

    /**
     * Finds all transactions for a wallet
     * 
     * @param walletId Wallet ID
     * @param pageable Pagination information
     * @return Page of transactions
     */
    @Query("SELECT wt FROM WalletTransaction wt WHERE wt.wallet.walletId = :walletId ORDER BY wt.createdAt DESC")
    Page<WalletTransaction> findByWalletWalletId(@Param("walletId") UUID walletId, Pageable pageable);

    /**
     * Finds all transactions for a wallet by type
     * 
     * @param walletId Wallet ID
     * @param transactionType Transaction type
     * @param pageable Pagination information
     * @return Page of transactions
     */
    @Query("SELECT wt FROM WalletTransaction wt WHERE wt.wallet.walletId = :walletId AND wt.transactionType = :transactionType ORDER BY wt.createdAt DESC")
    Page<WalletTransaction> findByWalletWalletIdAndTransactionType(
        @Param("walletId") UUID walletId, 
        @Param("transactionType") String transactionType, 
        Pageable pageable
    );

    /**
     * Finds all transactions for a wallet within a date range
     * 
     * @param walletId Wallet ID
     * @param startDate Start date
     * @param endDate End date
     * @param pageable Pagination information
     * @return Page of transactions
     */
    @Query("SELECT wt FROM WalletTransaction wt WHERE wt.wallet.walletId = :walletId AND wt.createdAt BETWEEN :startDate AND :endDate ORDER BY wt.createdAt DESC")
    Page<WalletTransaction> findByWalletWalletIdAndCreatedAtBetween(
        @Param("walletId") UUID walletId,
        @Param("startDate") LocalDateTime startDate,
        @Param("endDate") LocalDateTime endDate,
        Pageable pageable
    );

    /**
     * Finds recent transactions for a wallet
     * 
     * @param walletId Wallet ID
     * @param limit Maximum number of transactions
     * @return List of recent transactions
     */
    @Query("SELECT wt FROM WalletTransaction wt WHERE wt.wallet.walletId = :walletId ORDER BY wt.createdAt DESC LIMIT :limit")
    List<WalletTransaction> findRecentTransactions(@Param("walletId") UUID walletId, @Param("limit") int limit);

    /**
     * Counts pending transactions for a wallet
     * 
     * @param walletId Wallet ID
     * @return Count of pending transactions
     */
    @Query("SELECT COUNT(wt) FROM WalletTransaction wt WHERE wt.wallet.walletId = :walletId AND wt.status = 'PENDING'")
    long countPendingTransactions(@Param("walletId") UUID walletId);
}
