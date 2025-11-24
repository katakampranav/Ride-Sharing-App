package com.officemate.shared.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * DTO for QR code responses.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class QRCodeResponse {

    /**
     * QR code ID
     */
    private String qrCodeId;

    /**
     * Wallet ID
     */
    private String walletId;

    /**
     * QR code data (encoded string)
     */
    private String qrCodeData;

    /**
     * QR code image URL
     */
    private String qrCodeImageUrl;

    /**
     * Flag indicating if QR code is active
     */
    private Boolean isActive;

    /**
     * QR code creation timestamp
     */
    private LocalDateTime createdAt;

    /**
     * QR code last update timestamp
     */
    private LocalDateTime updatedAt;
}
