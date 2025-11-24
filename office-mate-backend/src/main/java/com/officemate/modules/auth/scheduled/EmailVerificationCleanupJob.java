package com.officemate.modules.auth.scheduled;

import com.officemate.modules.auth.repository.EmailVerificationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * Scheduled job to clean up expired email verification records.
 * Runs periodically to remove expired and unverified email verification records
 * from the database to prevent data accumulation.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class EmailVerificationCleanupJob {

    private final EmailVerificationRepository emailVerificationRepository;

    /**
     * Cleanup job that runs every hour to delete expired email verification records.
     * Only removes records that are both expired and unverified.
     * Verified records are kept for audit purposes.
     */
    @Scheduled(cron = "0 0 * * * *") // Run every hour at the top of the hour
    @Transactional
    public void cleanupExpiredVerifications() {
        log.info("Starting email verification cleanup job");
        
        try {
            LocalDateTime currentTime = LocalDateTime.now();
            int deletedCount = emailVerificationRepository.deleteExpiredRecords(currentTime);
            
            if (deletedCount > 0) {
                log.info("Email verification cleanup completed. Deleted {} expired records", deletedCount);
            } else {
                log.debug("Email verification cleanup completed. No expired records found");
            }
        } catch (Exception e) {
            log.error("Error during email verification cleanup job", e);
        }
    }

    /**
     * Additional cleanup job that runs daily to remove very old verified records
     * (older than 30 days) to maintain database hygiene.
     */
    @Scheduled(cron = "0 0 2 * * *") // Run daily at 2 AM
    @Transactional
    public void cleanupOldVerifiedRecords() {
        log.info("Starting cleanup of old verified email verification records");
        
        try {
            LocalDateTime thirtyDaysAgo = LocalDateTime.now().minusDays(30);
            
            // Custom query to delete old verified records
            int deletedCount = emailVerificationRepository
                .findAll()
                .stream()
                .filter(ev -> ev.getVerified() && ev.getCreatedAt().isBefore(thirtyDaysAgo))
                .mapToInt(ev -> {
                    emailVerificationRepository.delete(ev);
                    return 1;
                })
                .sum();
            
            if (deletedCount > 0) {
                log.info("Old verified records cleanup completed. Deleted {} records", deletedCount);
            } else {
                log.debug("Old verified records cleanup completed. No old records found");
            }
        } catch (Exception e) {
            log.error("Error during old verified records cleanup job", e);
        }
    }
}
