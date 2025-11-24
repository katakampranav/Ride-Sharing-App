package com.officemate.modules.auth.service;

import com.officemate.modules.auth.entity.EmailChangeAuditLog;
import com.officemate.modules.auth.entity.EmailVerification;
import com.officemate.modules.auth.entity.UserAccount;
import com.officemate.modules.auth.repository.EmailChangeAuditLogRepository;
import com.officemate.modules.auth.repository.EmailVerificationRepository;
import com.officemate.modules.auth.repository.UserAccountRepository;
import com.officemate.modules.wallet.service.WalletService;
import com.officemate.shared.dto.VerificationResponse;
import com.officemate.shared.exception.CorporateEmailException;
import com.officemate.shared.exception.WalletException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.List;
import java.util.UUID;
import java.util.regex.Pattern;

/**
 * Service for corporate email verification using OTP.
 * Handles email OTP generation, verification, and corporate email management.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class EmailVerificationService {

    private final EmailVerificationRepository emailVerificationRepository;
    private final UserAccountRepository userAccountRepository;
    private final EmailChangeAuditLogRepository emailChangeAuditLogRepository;
    private final OTPService otpService;
    private final WalletService walletService;
    private final SecureRandom secureRandom = new SecureRandom();

    @Value("${app.email.otp.length:6}")
    private int otpLength;

    @Value("${app.email.otp.expiration-minutes:10}")
    private int otpExpirationMinutes;

    @Value("${app.email.otp.max-attempts:3}")
    private int maxAttempts;

    // Basic email validation pattern
    private static final Pattern EMAIL_PATTERN = Pattern.compile(
        "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$"
    );

    /**
     * Send OTP to corporate email for verification.
     *
     * @param userId the user ID
     * @param corporateEmail the corporate email to verify
     * @return VerificationResponse with OTP delivery status
     * @throws CorporateEmailException if validation fails or email already exists
     */
    @Transactional
    public VerificationResponse sendEmailOTP(UUID userId, String corporateEmail) {
        log.info("Sending email OTP for user: {} to email: {}", userId, maskEmail(corporateEmail));

        // Validate email format
        if (!isValidEmail(corporateEmail)) {
            throw new CorporateEmailException("Invalid email format", "INVALID_EMAIL_FORMAT");
        }

        // Check if user exists
        userAccountRepository.findById(userId)
                .orElseThrow(() -> new CorporateEmailException("User not found", "USER_NOT_FOUND"));

        // Check if email already exists for another user
        if (userAccountRepository.existsByCorporateEmail(corporateEmail)) {
            throw new CorporateEmailException(
                "Corporate email already registered to another account", 
                "EMAIL_ALREADY_EXISTS"
            );
        }

        // Check if there's an active verification for this user
        emailVerificationRepository.findActiveVerificationByUserId(userId, LocalDateTime.now())
                .ifPresent(activeVerification -> {
                    throw new CorporateEmailException(
                        "Active verification already exists. Please wait for it to expire or use the existing OTP.",
                        "ACTIVE_VERIFICATION_EXISTS"
                    );
                });

        // Generate OTP
        String otp = generateOTP();
        String otpHash = hashOTP(otp);

        // Create email verification record
        EmailVerification emailVerification = EmailVerification.builder()
                .userId(userId)
                .corporateEmail(corporateEmail)
                .otpHash(otpHash)
                .attempts(0)
                .verified(false)
                .build();

        emailVerificationRepository.save(emailVerification);

        log.info("Email OTP generated and stored for user: {}", userId);

        // TODO: Send OTP via AWS SES (to be implemented in external service integration)
        // For now, log the OTP (REMOVE IN PRODUCTION)
        log.debug("Generated OTP for {}: {}", maskEmail(corporateEmail), otp);

        return VerificationResponse.builder()
                .otpSent(true)
                .expiresAt(emailVerification.getExpiresAt())
                .maskedEmail(maskEmail(corporateEmail))
                .verified(false)
                .userId(userId.toString())
                .build();
    }

    /**
     * Verify email OTP and update user account.
     *
     * @param userId the user ID
     * @param otp the OTP to verify
     * @return VerificationResponse with verification result
     * @throws CorporateEmailException if verification fails
     */
    @Transactional
    public VerificationResponse verifyEmailOTP(UUID userId, String otp) {
        log.info("Verifying email OTP for user: {}", userId);

        // Find active verification record
        EmailVerification emailVerification = emailVerificationRepository
                .findActiveVerificationByUserId(userId, LocalDateTime.now())
                .orElseThrow(() -> new CorporateEmailException(
                    "No active email verification found or OTP expired",
                    "NO_ACTIVE_VERIFICATION"
                ));

        // Check if already verified
        if (emailVerification.getVerified()) {
            throw new CorporateEmailException("Email already verified", "ALREADY_VERIFIED");
        }

        // Check if expired
        if (emailVerification.isExpired()) {
            emailVerificationRepository.delete(emailVerification);
            throw new CorporateEmailException("OTP expired", "OTP_EXPIRED");
        }

        // Check max attempts
        if (emailVerification.hasExceededAttempts(maxAttempts)) {
            emailVerificationRepository.delete(emailVerification);
            throw new CorporateEmailException(
                "Maximum verification attempts exceeded",
                "MAX_ATTEMPTS_EXCEEDED"
            );
        }

        // Verify OTP hash
        String otpHash = hashOTP(otp);
        boolean isValid = otpHash.equals(emailVerification.getOtpHash());

        if (!isValid) {
            // Increment attempts
            emailVerification.incrementAttempts();
            emailVerificationRepository.save(emailVerification);
            
            int remainingAttempts = maxAttempts - emailVerification.getAttempts();
            log.warn("Invalid OTP attempt for user: {}. Remaining attempts: {}", userId, remainingAttempts);
            
            throw new CorporateEmailException(
                String.format("Invalid OTP. %d attempts remaining.", remainingAttempts),
                "INVALID_OTP"
            );
        }

        // Mark as verified
        emailVerification.markAsVerified();
        emailVerificationRepository.save(emailVerification);

        // Update user account with verified corporate email
        UserAccount userAccount = userAccountRepository.findById(userId)
                .orElseThrow(() -> new CorporateEmailException("User not found", "USER_NOT_FOUND"));
        
        userAccount.setCorporateEmail(emailVerification.getCorporateEmail());
        userAccount.verifyEmail();
        userAccountRepository.save(userAccount);

        log.info("Email verified successfully for user: {}", userId);

        // Automatically initialize wallet after email verification completion
        // This ensures users can access ride features immediately after full verification
        try {
            walletService.initializeWallet(userId);
            log.info("Wallet automatically initialized for user: {} after email verification", userId);
        } catch (WalletException e) {
            // Wallet may already exist (e.g., from previous verification or manual creation)
            if ("WALLET_ALREADY_EXISTS".equals(e.getErrorCode())) {
                log.debug("Wallet already exists for user: {}, skipping automatic creation", userId);
            } else {
                // Log other wallet errors but don't fail the email verification
                log.warn("Failed to automatically initialize wallet for user: {} - {}", userId, e.getMessage());
            }
        } catch (Exception e) {
            // Log unexpected errors but don't fail the email verification
            log.error("Unexpected error during automatic wallet initialization for user: {}", userId, e);
        }

        return VerificationResponse.builder()
                .otpSent(false)
                .expiresAt(null)
                .maskedEmail(maskEmail(emailVerification.getCorporateEmail()))
                .verified(true)
                .userId(userId.toString())
                .build();
    }

    /**
     * Resend email OTP.
     *
     * @param userId the user ID
     * @return VerificationResponse with OTP delivery status
     * @throws CorporateEmailException if no active verification exists
     */
    @Transactional
    public VerificationResponse resendEmailOTP(UUID userId) {
        log.info("Resending email OTP for user: {}", userId);

        // Find active verification record
        EmailVerification existingVerification = emailVerificationRepository
                .findActiveVerificationByUserId(userId, LocalDateTime.now())
                .orElseThrow(() -> new CorporateEmailException(
                    "No active email verification found",
                    "NO_ACTIVE_VERIFICATION"
                ));

        // Delete existing verification
        emailVerificationRepository.delete(existingVerification);

        // Send new OTP
        return sendEmailOTP(userId, existingVerification.getCorporateEmail());
    }

    /**
     * Initiate corporate email update workflow (for company changes).
     * Step 1: Verify mobile OTP to authorize the email change.
     *
     * @param userId the user ID
     * @param mobileOtp the mobile OTP for verification
     * @param newCorporateEmail the new corporate email
     * @param changeReason optional reason for the change
     * @param ipAddress client IP address for audit
     * @param userAgent client user agent for audit
     * @return VerificationResponse with OTP delivery status for new email
     * @throws CorporateEmailException if validation or mobile OTP verification fails
     */
    @Transactional
    public VerificationResponse initiateEmailUpdate(
            UUID userId, 
            String mobileOtp, 
            String newCorporateEmail,
            String changeReason,
            String ipAddress,
            String userAgent) {
        
        log.info("Initiating email update for user: {} to: {}", userId, maskEmail(newCorporateEmail));

        // Validate new email format
        if (!isValidEmail(newCorporateEmail)) {
            throw new CorporateEmailException("Invalid email format", "INVALID_EMAIL_FORMAT");
        }

        // Check if user exists
        UserAccount userAccount = userAccountRepository.findById(userId)
                .orElseThrow(() -> new CorporateEmailException("User not found", "USER_NOT_FOUND"));

        // Verify mobile OTP first for security
        boolean mobileOtpValid = otpService.verifyMobileOTP(userAccount.getPhoneNumber(), mobileOtp);
        if (!mobileOtpValid) {
            log.warn("Mobile OTP verification failed for email update request from user: {}", userId);
            throw new CorporateEmailException("Invalid mobile OTP", "INVALID_MOBILE_OTP");
        }

        log.info("Mobile OTP verified successfully for user: {}", userId);

        // Check if new email already exists for another user
        if (userAccountRepository.existsByCorporateEmail(newCorporateEmail)) {
            throw new CorporateEmailException(
                "Corporate email already registered to another account",
                "EMAIL_ALREADY_EXISTS"
            );
        }

        // Create audit log entry
        String oldEmail = userAccount.getCorporateEmail();
        EmailChangeAuditLog.ChangeType changeType = (oldEmail == null) 
            ? EmailChangeAuditLog.ChangeType.ADDITION 
            : EmailChangeAuditLog.ChangeType.UPDATE;

        EmailChangeAuditLog auditLog = EmailChangeAuditLog.builder()
                .userId(userId)
                .oldEmail(oldEmail)
                .newEmail(newCorporateEmail)
                .changeType(changeType)
                .changeReason(changeReason)
                .ipAddress(ipAddress)
                .userAgent(userAgent)
                .mobileOtpVerified(true)
                .emailOtpVerified(false)
                .status(EmailChangeAuditLog.Status.MOBILE_VERIFIED)
                .build();

        emailChangeAuditLogRepository.save(auditLog);
        log.info("Audit log created for email change: {}", auditLog.getAuditId());

        // Remove old corporate email from user account
        userAccount.setCorporateEmail(null);
        userAccount.setEmailVerified(false);
        userAccountRepository.save(userAccount);

        // Delete any existing verification records for this user
        emailVerificationRepository.deleteByUserId(userId);

        log.info("Old corporate email removed for user: {}", userId);

        // Send OTP to new email
        VerificationResponse response = sendEmailOTP(userId, newCorporateEmail);
        
        // Store audit log ID in response for tracking
        response.setAuditLogId(auditLog.getAuditId().toString());
        
        return response;
    }

    /**
     * Complete email update by verifying the new email OTP.
     * Step 2: Verify the OTP sent to the new email address.
     * Also ensures wallet is initialized if not already present.
     *
     * @param userId the user ID
     * @param emailOtp the OTP sent to new email
     * @return VerificationResponse with completion status
     * @throws CorporateEmailException if verification fails
     */
    @Transactional
    public VerificationResponse completeEmailUpdate(UUID userId, String emailOtp) {
        log.info("Completing email update for user: {}", userId);

        // Verify email OTP (this will also trigger automatic wallet creation)
        VerificationResponse response = verifyEmailOTP(userId, emailOtp);

        // Find the most recent audit log for this user
        List<EmailChangeAuditLog> auditLogs = emailChangeAuditLogRepository
                .findByUserIdOrderByChangedAtDesc(userId);
        
        if (!auditLogs.isEmpty()) {
            EmailChangeAuditLog latestLog = auditLogs.get(0);
            if (latestLog.getStatus() == EmailChangeAuditLog.Status.MOBILE_VERIFIED ||
                latestLog.getStatus() == EmailChangeAuditLog.Status.EMAIL_VERIFIED) {
                
                latestLog.markEmailVerified();
                latestLog.markCompleted();
                latestLog.setNotes("Email update completed successfully");
                emailChangeAuditLogRepository.save(latestLog);
                
                log.info("Email update completed and audit log updated: {}", latestLog.getAuditId());
            }
        }

        return response;
    }

    /**
     * Update corporate email (legacy method - for backward compatibility).
     * Requires mobile OTP verification before calling this method.
     * 
     * @deprecated Use initiateEmailUpdate() and completeEmailUpdate() for better audit trail
     *
     * @param userId the user ID
     * @param newCorporateEmail the new corporate email
     * @return VerificationResponse with OTP delivery status
     * @throws CorporateEmailException if validation fails
     */
    @Deprecated
    @Transactional
    public VerificationResponse updateCorporateEmail(UUID userId, String newCorporateEmail) {
        log.info("Updating corporate email for user: {} to: {}", userId, maskEmail(newCorporateEmail));

        // Validate email format
        if (!isValidEmail(newCorporateEmail)) {
            throw new CorporateEmailException("Invalid email format", "INVALID_EMAIL_FORMAT");
        }

        // Check if user exists
        UserAccount userAccount = userAccountRepository.findById(userId)
                .orElseThrow(() -> new CorporateEmailException("User not found", "USER_NOT_FOUND"));

        // Check if new email already exists for another user
        if (userAccountRepository.existsByCorporateEmail(newCorporateEmail)) {
            throw new CorporateEmailException(
                "Corporate email already registered to another account",
                "EMAIL_ALREADY_EXISTS"
            );
        }

        // Create audit log entry (without mobile OTP verification in legacy method)
        String oldEmail = userAccount.getCorporateEmail();
        EmailChangeAuditLog.ChangeType changeType = (oldEmail == null) 
            ? EmailChangeAuditLog.ChangeType.ADDITION 
            : EmailChangeAuditLog.ChangeType.UPDATE;

        EmailChangeAuditLog auditLog = EmailChangeAuditLog.builder()
                .userId(userId)
                .oldEmail(oldEmail)
                .newEmail(newCorporateEmail)
                .changeType(changeType)
                .changeReason("Legacy update method")
                .mobileOtpVerified(false)
                .emailOtpVerified(false)
                .status(EmailChangeAuditLog.Status.INITIATED)
                .build();

        emailChangeAuditLogRepository.save(auditLog);

        // Remove old corporate email from user account
        userAccount.setCorporateEmail(null);
        userAccount.setEmailVerified(false);
        userAccountRepository.save(userAccount);

        // Delete any existing verification records for this user
        emailVerificationRepository.deleteByUserId(userId);

        log.info("Old corporate email removed for user: {}", userId);

        // Send OTP to new email
        return sendEmailOTP(userId, newCorporateEmail);
    }

    /**
     * Remove corporate email from user account.
     * Requires mobile OTP verification before calling this method.
     *
     * @param userId the user ID
     * @param mobileOtp the mobile OTP for verification
     * @param changeReason optional reason for removal
     * @param ipAddress client IP address for audit
     * @param userAgent client user agent for audit
     * @throws CorporateEmailException if user not found or mobile OTP invalid
     */
    @Transactional
    public void removeCorporateEmail(
            UUID userId, 
            String mobileOtp,
            String changeReason,
            String ipAddress,
            String userAgent) {
        
        log.info("Removing corporate email for user: {}", userId);

        // Check if user exists
        UserAccount userAccount = userAccountRepository.findById(userId)
                .orElseThrow(() -> new CorporateEmailException("User not found", "USER_NOT_FOUND"));

        // Verify mobile OTP first for security
        boolean mobileOtpValid = otpService.verifyMobileOTP(userAccount.getPhoneNumber(), mobileOtp);
        if (!mobileOtpValid) {
            log.warn("Mobile OTP verification failed for email removal request from user: {}", userId);
            throw new CorporateEmailException("Invalid mobile OTP", "INVALID_MOBILE_OTP");
        }

        log.info("Mobile OTP verified successfully for email removal: {}", userId);

        // Create audit log entry
        String oldEmail = userAccount.getCorporateEmail();
        EmailChangeAuditLog auditLog = EmailChangeAuditLog.builder()
                .userId(userId)
                .oldEmail(oldEmail)
                .newEmail(null)
                .changeType(EmailChangeAuditLog.ChangeType.REMOVAL)
                .changeReason(changeReason)
                .ipAddress(ipAddress)
                .userAgent(userAgent)
                .mobileOtpVerified(true)
                .emailOtpVerified(false)
                .status(EmailChangeAuditLog.Status.COMPLETED)
                .build();

        emailChangeAuditLogRepository.save(auditLog);
        log.info("Audit log created for email removal: {}", auditLog.getAuditId());

        // Remove corporate email
        userAccount.setCorporateEmail(null);
        userAccount.setEmailVerified(false);
        userAccountRepository.save(userAccount);

        // Delete any verification records
        emailVerificationRepository.deleteByUserId(userId);

        log.info("Corporate email removed successfully for user: {}", userId);
    }

    /**
     * Remove corporate email from user account (legacy method - for backward compatibility).
     * 
     * @deprecated Use removeCorporateEmail() with mobile OTP verification for better security
     *
     * @param userId the user ID
     * @throws CorporateEmailException if user not found
     */
    @Deprecated
    @Transactional
    public void removeCorporateEmailLegacy(UUID userId) {
        log.info("Removing corporate email for user: {} (legacy method)", userId);

        // Check if user exists
        UserAccount userAccount = userAccountRepository.findById(userId)
                .orElseThrow(() -> new CorporateEmailException("User not found", "USER_NOT_FOUND"));

        // Create audit log entry (without mobile OTP verification in legacy method)
        String oldEmail = userAccount.getCorporateEmail();
        EmailChangeAuditLog auditLog = EmailChangeAuditLog.builder()
                .userId(userId)
                .oldEmail(oldEmail)
                .newEmail(null)
                .changeType(EmailChangeAuditLog.ChangeType.REMOVAL)
                .changeReason("Legacy removal method")
                .mobileOtpVerified(false)
                .emailOtpVerified(false)
                .status(EmailChangeAuditLog.Status.COMPLETED)
                .build();

        emailChangeAuditLogRepository.save(auditLog);

        // Remove corporate email
        userAccount.setCorporateEmail(null);
        userAccount.setEmailVerified(false);
        userAccountRepository.save(userAccount);

        // Delete any verification records
        emailVerificationRepository.deleteByUserId(userId);

        log.info("Corporate email removed successfully for user: {}", userId);
    }

    /**
     * Generate random numeric OTP.
     *
     * @return OTP string
     */
    private String generateOTP() {
        StringBuilder otp = new StringBuilder();
        for (int i = 0; i < otpLength; i++) {
            otp.append(secureRandom.nextInt(10));
        }
        return otp.toString();
    }

    /**
     * Hash OTP using SHA-256 for secure storage.
     *
     * @param otp the plain text OTP
     * @return hashed OTP in Base64 format
     */
    private String hashOTP(String otp) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(otp.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(hash);
        } catch (NoSuchAlgorithmException e) {
            log.error("SHA-256 algorithm not available", e);
            throw new RuntimeException("Failed to hash OTP", e);
        }
    }

    /**
     * Validate email format.
     *
     * @param email the email to validate
     * @return true if valid, false otherwise
     */
    private boolean isValidEmail(String email) {
        return email != null && EMAIL_PATTERN.matcher(email).matches();
    }

    /**
     * Mask email for logging (shows only first 2 and last 2 characters before @).
     *
     * @param email the email to mask
     * @return masked email
     */
    private String maskEmail(String email) {
        if (email == null || !email.contains("@")) {
            return "****";
        }
        String[] parts = email.split("@");
        String localPart = parts[0];
        if (localPart.length() <= 4) {
            return "****@" + parts[1];
        }
        return localPart.substring(0, 2) + "****" + localPart.substring(localPart.length() - 2) + "@" + parts[1];
    }
}
