package com.officemate.modules.wallet.repository;

import com.officemate.modules.wallet.entity.QRCode;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

/**
 * Repository interface for QRCode entity.
 * Provides database operations for QR code management.
 */
@Repository
public interface QRCodeRepository extends JpaRepository<QRCode, UUID> {

    /**
     * Finds an active QR code by wallet ID
     * 
     * @param walletId Wallet ID
     * @return Optional containing the QR code if found
     */
    @Query("SELECT qr FROM QRCode qr WHERE qr.wallet.walletId = :walletId AND qr.isActive = true")
    Optional<QRCode> findActiveByWalletWalletId(@Param("walletId") UUID walletId);

    /**
     * Finds a QR code by QR code data
     * 
     * @param qrCodeData QR code data string
     * @return Optional containing the QR code if found
     */
    @Query("SELECT qr FROM QRCode qr WHERE qr.qrCodeData = :qrCodeData")
    Optional<QRCode> findByQrCodeData(@Param("qrCodeData") String qrCodeData);

    /**
     * Checks if a wallet has an active QR code
     * 
     * @param walletId Wallet ID
     * @return true if wallet has an active QR code
     */
    @Query("SELECT CASE WHEN COUNT(qr) > 0 THEN true ELSE false END FROM QRCode qr WHERE qr.wallet.walletId = :walletId AND qr.isActive = true")
    boolean existsActiveByWalletWalletId(@Param("walletId") UUID walletId);

    /**
     * Deactivates all QR codes for a wallet
     * 
     * @param walletId Wallet ID
     */
    @Query("UPDATE QRCode qr SET qr.isActive = false WHERE qr.wallet.walletId = :walletId")
    void deactivateAllByWalletWalletId(@Param("walletId") UUID walletId);
}
