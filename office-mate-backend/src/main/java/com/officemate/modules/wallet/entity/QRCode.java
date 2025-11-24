package com.officemate.modules.wallet.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * JPA Entity representing a QR code for driver payments in the PostgreSQL database.
 * Drivers can generate QR codes for riders to scan and make payments.
 */
@Entity
@Table(
    name = "qr_codes",
    indexes = {
        @Index(name = "idx_qr_codes_wallet", columnList = "wallet_id"),
        @Index(name = "idx_qr_codes_code", columnList = "qr_code_data", unique = true)
    }
)
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class QRCode {

    /**
     * Unique identifier for the QR code
     */
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "qr_code_id", updatable = false, nullable = false)
    private UUID qrCodeId;

    /**
     * Many-to-one relationship with Wallet (driver's wallet)
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "wallet_id", nullable = false)
    @NotNull(message = "Wallet is required")
    private Wallet wallet;

    /**
     * QR code data (encoded string that can be scanned)
     */
    @NotBlank(message = "QR code data is required")
    @Column(name = "qr_code_data", nullable = false, unique = true, length = 500)
    private String qrCodeData;

    /**
     * QR code image URL (stored in S3 or similar)
     */
    @Column(name = "qr_code_image_url", length = 500)
    private String qrCodeImageUrl;

    /**
     * Flag indicating if QR code is active
     */
    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private Boolean isActive = true;

    /**
     * Timestamp when the QR code was created
     */
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    /**
     * Timestamp when the QR code was last updated
     */
    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    /**
     * Activates the QR code
     */
    public void activate() {
        this.isActive = true;
    }

    /**
     * Deactivates the QR code
     */
    public void deactivate() {
        this.isActive = false;
    }

    /**
     * Checks if QR code is active
     * 
     * @return true if QR code is active
     */
    public boolean isActive() {
        return Boolean.TRUE.equals(isActive);
    }
}
