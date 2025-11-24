package com.officemate.modules.wallet.service;

import com.officemate.modules.wallet.entity.QRCode;
import com.officemate.modules.wallet.entity.Wallet;
import com.officemate.modules.wallet.repository.QRCodeRepository;
import com.officemate.modules.wallet.repository.WalletRepository;
import com.officemate.shared.dto.QRCodeResponse;
import com.officemate.shared.exception.WalletException;
import jakarta.persistence.EntityNotFoundException;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Base64;
import java.util.UUID;

/**
 * Service for managing QR code payments for drivers.
 * Handles QR code generation, activation, and payment processing.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class QRCodeService {

    private final QRCodeRepository qrCodeRepository;
    private final WalletRepository walletRepository;

    /**
     * Generates a QR code for a driver's wallet.
     * 
     * @param userId User ID (driver)
     * @return QRCodeResponse containing QR code details
     * @throws EntityNotFoundException if wallet not found
     * @throws WalletException if QR code generation fails
     */
    @Transactional
    public QRCodeResponse generateQRCode(UUID userId) {
        log.info("Generating QR code for user: {}", userId);

        Wallet wallet = walletRepository.findByUserAccountUserId(userId)
            .orElseThrow(() -> new EntityNotFoundException("Wallet not found for user: " + userId));

        // Check if wallet has bank linked (required for drivers)
        if (!wallet.getBankLinked()) {
            throw new WalletException("Bank account must be linked to generate QR code", "BANK_NOT_LINKED");
        }

        // Deactivate any existing active QR codes
        qrCodeRepository.deactivateAllByWalletWalletId(wallet.getWalletId());

        // Generate unique QR code data
        String qrCodeData = generateQRCodeData(wallet.getWalletId(), userId);

        // Create QR code entity
        QRCode qrCode = QRCode.builder()
            .wallet(wallet)
            .qrCodeData(qrCodeData)
            .isActive(true)
            .build();

        QRCode savedQRCode = qrCodeRepository.save(qrCode);

        log.info("Successfully generated QR code for user: {}, QR code ID: {}", userId, savedQRCode.getQrCodeId());

        return buildQRCodeResponse(savedQRCode);
    }

    /**
     * Gets the active QR code for a driver's wallet.
     * 
     * @param userId User ID (driver)
     * @return QRCodeResponse containing QR code details
     * @throws EntityNotFoundException if wallet or QR code not found
     */
    @Transactional
    public QRCodeResponse getActiveQRCode(UUID userId) {
        log.debug("Retrieving active QR code for user: {}", userId);

        Wallet wallet = walletRepository.findByUserAccountUserId(userId)
            .orElseThrow(() -> new EntityNotFoundException("Wallet not found for user: " + userId));

        QRCode qrCode = qrCodeRepository.findActiveByWalletWalletId(wallet.getWalletId())
            .orElseThrow(() -> new EntityNotFoundException("No active QR code found for user: " + userId));

        return buildQRCodeResponse(qrCode);
    }

    /**
     * Deactivates a QR code.
     * 
     * @param userId User ID (driver)
     * @param qrCodeId QR code ID
     * @return QRCodeResponse containing updated QR code details
     * @throws EntityNotFoundException if QR code not found
     */
    @Transactional
    public QRCodeResponse deactivateQRCode(UUID userId, UUID qrCodeId) {
        log.info("Deactivating QR code {} for user: {}", qrCodeId, userId);

        Wallet wallet = walletRepository.findByUserAccountUserId(userId)
            .orElseThrow(() -> new EntityNotFoundException("Wallet not found for user: " + userId));

        QRCode qrCode = qrCodeRepository.findById(qrCodeId)
            .orElseThrow(() -> new EntityNotFoundException("QR code not found: " + qrCodeId));

        // Verify QR code belongs to user's wallet
        if (!qrCode.getWallet().getWalletId().equals(wallet.getWalletId())) {
            throw new WalletException("QR code does not belong to user", "UNAUTHORIZED_QR_CODE");
        }

        qrCode.deactivate();
        QRCode savedQRCode = qrCodeRepository.save(qrCode);

        log.info("Successfully deactivated QR code {} for user: {}", qrCodeId, userId);

        return buildQRCodeResponse(savedQRCode);
    }

    /**
     * Validates a QR code for payment.
     * 
     * @param qrCodeData QR code data string
     * @return QRCodeResponse containing QR code details
     * @throws EntityNotFoundException if QR code not found
     * @throws WalletException if QR code is not active
     */
    @Transactional
    public QRCodeResponse validateQRCode(String qrCodeData) {
        log.debug("Validating QR code");

        QRCode qrCode = qrCodeRepository.findByQrCodeData(qrCodeData)
            .orElseThrow(() -> new EntityNotFoundException("QR code not found"));

        if (!qrCode.isActive()) {
            throw new WalletException("QR code is not active", "QR_CODE_INACTIVE");
        }

        return buildQRCodeResponse(qrCode);
    }

    /**
     * Regenerates a QR code for a driver's wallet.
     * 
     * @param userId User ID (driver)
     * @return QRCodeResponse containing new QR code details
     * @throws EntityNotFoundException if wallet not found
     */
    @Transactional
    public QRCodeResponse regenerateQRCode(UUID userId) {
        log.info("Regenerating QR code for user: {}", userId);

        // Deactivate existing QR codes
        Wallet wallet = walletRepository.findByUserAccountUserId(userId)
            .orElseThrow(() -> new EntityNotFoundException("Wallet not found for user: " + userId));

        qrCodeRepository.deactivateAllByWalletWalletId(wallet.getWalletId());

        // Generate new QR code
        return generateQRCode(userId);
    }

    /**
     * Generates unique QR code data.
     * 
     * @param walletId Wallet ID
     * @param userId User ID
     * @return QR code data string
     */
    private String generateQRCodeData(UUID walletId, UUID userId) {
        // Generate a unique identifier combining wallet ID, user ID, and timestamp
        String rawData = String.format("WALLET:%s:USER:%s:TS:%d", 
            walletId.toString(), 
            userId.toString(), 
            System.currentTimeMillis()
        );

        // Encode to Base64 for QR code compatibility
        return Base64.getEncoder().encodeToString(rawData.getBytes());
    }

    /**
     * Builds a QRCodeResponse DTO from QRCode entity.
     * 
     * @param qrCode The QR code entity
     * @return QRCodeResponse DTO
     */
    private QRCodeResponse buildQRCodeResponse(QRCode qrCode) {
        return QRCodeResponse.builder()
            .qrCodeId(qrCode.getQrCodeId().toString())
            .walletId(qrCode.getWallet().getWalletId().toString())
            .qrCodeData(qrCode.getQrCodeData())
            .qrCodeImageUrl(qrCode.getQrCodeImageUrl())
            .isActive(qrCode.getIsActive())
            .createdAt(qrCode.getCreatedAt())
            .updatedAt(qrCode.getUpdatedAt())
            .build();
    }
}
