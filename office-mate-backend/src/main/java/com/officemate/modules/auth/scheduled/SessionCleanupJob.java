package com.officemate.modules.auth.scheduled;

import com.officemate.modules.auth.entity.SessionMetadata;
import com.officemate.modules.auth.entity.UserSession;
import com.officemate.modules.auth.repository.SessionMetadataRepository;
import com.officemate.modules.auth.repository.UserSessionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Scheduled job for cleaning up expired and inactive sessions.
 * Runs periodically to maintain session hygiene and free up resources.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class SessionCleanupJob {
    
    private final UserSessionRepository userSessionRepository;
    private final SessionMetadataRepository sessionMetadataRepository;
    
    /**
     * Clean up expired sessions from Redis.
     * Redis TTL handles automatic expiration, but this provides additional cleanup.
     * Runs every hour.
     */
    @Scheduled(cron = "0 0 * * * *") // Every hour at minute 0
    @Transactional
    public void cleanupExpiredSessions() {
        log.info("Starting expired session cleanup job");
        
        try {
            // Get all sessions from Redis
            Iterable<UserSession> allSessions = userSessionRepository.findAll();
            int expiredCount = 0;
            
            for (UserSession session : allSessions) {
                // Check if session has expired
                if (session.getExpiresAt() != null && 
                    session.getExpiresAt().isBefore(LocalDateTime.now())) {
                    
                    // Update metadata in PostgreSQL
                    sessionMetadataRepository.findBySessionId(session.getSessionId())
                        .ifPresent(metadata -> {
                            metadata.endSession("EXPIRED");
                            sessionMetadataRepository.save(metadata);
                        });
                    
                    // Delete from Redis
                    userSessionRepository.deleteById(session.getSessionId());
                    expiredCount++;
                    
                    log.debug("Cleaned up expired session: {}", session.getSessionId());
                }
            }
            
            log.info("Expired session cleanup completed. Removed {} sessions", expiredCount);
            
        } catch (Exception e) {
            log.error("Error during expired session cleanup", e);
        }
    }
    
    /**
     * Clean up inactive sessions that haven't been accessed recently.
     * Marks sessions as inactive if no activity for 30 days.
     * Runs daily at 2 AM.
     */
    @Scheduled(cron = "0 0 2 * * *") // Daily at 2 AM
    @Transactional
    public void cleanupInactiveSessions() {
        log.info("Starting inactive session cleanup job");
        
        try {
            // Find sessions inactive for more than 30 days
            LocalDateTime inactivityThreshold = LocalDateTime.now().minusDays(30);
            List<SessionMetadata> inactiveSessions = 
                sessionMetadataRepository.findInactiveSessions(inactivityThreshold);
            
            int cleanedCount = 0;
            for (SessionMetadata metadata : inactiveSessions) {
                // End session in metadata
                metadata.endSession("INACTIVE");
                sessionMetadataRepository.save(metadata);
                
                // Remove from Redis if still exists
                userSessionRepository.deleteById(metadata.getSessionId());
                cleanedCount++;
                
                log.debug("Cleaned up inactive session: {}", metadata.getSessionId());
            }
            
            log.info("Inactive session cleanup completed. Removed {} sessions", cleanedCount);
            
        } catch (Exception e) {
            log.error("Error during inactive session cleanup", e);
        }
    }
    
    /**
     * Archive old session metadata records.
     * Deletes metadata for sessions that ended more than 90 days ago.
     * Runs weekly on Sunday at 3 AM.
     */
    @Scheduled(cron = "0 0 3 * * SUN") // Weekly on Sunday at 3 AM
    @Transactional
    public void archiveOldSessionMetadata() {
        log.info("Starting old session metadata archival job");
        
        try {
            // Delete metadata for sessions that ended more than 90 days ago
            LocalDateTime archiveThreshold = LocalDateTime.now().minusDays(90);
            int deletedCount = sessionMetadataRepository.deleteOldInactiveSessions(archiveThreshold);
            
            log.info("Session metadata archival completed. Deleted {} old records", deletedCount);
            
        } catch (Exception e) {
            log.error("Error during session metadata archival", e);
        }
    }
    
    /**
     * Synchronize Redis sessions with PostgreSQL metadata.
     * Ensures consistency between Redis and PostgreSQL.
     * Runs every 6 hours.
     */
    @Scheduled(cron = "0 0 */6 * * *") // Every 6 hours
    @Transactional
    public void synchronizeSessionState() {
        log.info("Starting session state synchronization job");
        
        try {
            // Get all active metadata records
            List<SessionMetadata> activeMetadata = sessionMetadataRepository
                .findSessionsInTimeRange(
                    LocalDateTime.now().minusDays(7),
                    LocalDateTime.now()
                );
            
            int syncedCount = 0;
            for (SessionMetadata metadata : activeMetadata) {
                if (metadata.isActive()) {
                    // Check if session still exists in Redis
                    boolean existsInRedis = userSessionRepository
                        .existsById(metadata.getSessionId());
                    
                    if (!existsInRedis) {
                        // Session expired in Redis but metadata still active
                        metadata.endSession("EXPIRED_IN_REDIS");
                        sessionMetadataRepository.save(metadata);
                        syncedCount++;
                        
                        log.debug("Synchronized metadata for expired session: {}", 
                            metadata.getSessionId());
                    }
                }
            }
            
            log.info("Session state synchronization completed. Synced {} records", syncedCount);
            
        } catch (Exception e) {
            log.error("Error during session state synchronization", e);
        }
    }
}
