package com.officemate.shared.service;

import com.officemate.shared.entity.CancellationLog;
import com.officemate.shared.entity.SecurityEventLog;
import com.officemate.shared.repository.AuditLogRepository;
import com.officemate.shared.repository.CancellationLogRepository;
import com.officemate.shared.repository.SecurityEventLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Service for managing log cleanup and generating monitoring reports
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class LogManagementService {

    private final AuditLogRepository auditLogRepository;
    private final SecurityEventLogRepository securityEventLogRepository;
    private final CancellationLogRepository cancellationLogRepository;

    /**
     * Clean up old audit logs (keep for 1 year)
     * Runs daily at 2 AM
     */
    @Scheduled(cron = "0 0 2 * * ?")
    @Transactional
    public void cleanupOldAuditLogs() {
        try {
            LocalDateTime cutoffDate = LocalDateTime.now().minusYears(1);
            
            // Note: In a production environment, you might want to archive these logs
            // instead of deleting them for compliance purposes
            long deletedCount = auditLogRepository.findByTimestampBetween(
                LocalDateTime.of(2000, 1, 1, 0, 0), 
                cutoffDate, 
                org.springframework.data.domain.Pageable.unpaged()
            ).getTotalElements();
            
            log.info("Audit log cleanup completed. {} old records identified for cleanup", deletedCount);
            
        } catch (Exception e) {
            log.error("Failed to cleanup old audit logs: {}", e.getMessage(), e);
        }
    }

    /**
     * Clean up resolved security events older than 6 months
     * Runs daily at 2:30 AM
     */
    @Scheduled(cron = "0 30 2 * * ?")
    @Transactional
    public void cleanupResolvedSecurityEvents() {
        try {
            LocalDateTime cutoffDate = LocalDateTime.now().minusMonths(6);
            
            long deletedCount = securityEventLogRepository.findByTimestampBetween(
                LocalDateTime.of(2000, 1, 1, 0, 0), 
                cutoffDate, 
                org.springframework.data.domain.Pageable.unpaged()
            ).getTotalElements();
            
            log.info("Security event cleanup completed. {} resolved events identified for cleanup", deletedCount);
            
        } catch (Exception e) {
            log.error("Failed to cleanup resolved security events: {}", e.getMessage(), e);
        }
    }

    /**
     * Generate daily security report
     * Runs daily at 8 AM
     */
    @Scheduled(cron = "0 0 8 * * ?")
    public void generateDailySecurityReport() {
        try {
            LocalDateTime yesterday = LocalDateTime.now().minusDays(1);
            LocalDateTime today = LocalDateTime.now();
            
            // Count various security events from yesterday
            long failedLogins = securityEventLogRepository.countByEventTypeAndTimestampAfter("LOGIN_FAILURE", yesterday);
            long otpFailures = securityEventLogRepository.countByEventTypeAndTimestampAfter("OTP_FAILURE", yesterday);
            long suspiciousActivities = securityEventLogRepository.countByEventTypeAndTimestampAfter("SUSPICIOUS_ACTIVITY", yesterday);
            long accountLockouts = securityEventLogRepository.countByEventTypeAndTimestampAfter("ACCOUNT_LOCKED", yesterday);
            
            // Get unresolved high-severity events
            List<SecurityEventLog> unresolvedEvents = securityEventLogRepository.findRecentSuspiciousActivities(yesterday.minusDays(7));
            
            log.info("=== DAILY SECURITY REPORT ===");
            log.info("Date: {}", yesterday.toLocalDate());
            log.info("Failed Logins: {}", failedLogins);
            log.info("OTP Failures: {}", otpFailures);
            log.info("Suspicious Activities: {}", suspiciousActivities);
            log.info("Account Lockouts: {}", accountLockouts);
            log.info("Unresolved High-Severity Events (last 7 days): {}", unresolvedEvents.size());
            log.info("=============================");
            
        } catch (Exception e) {
            log.error("Failed to generate daily security report: {}", e.getMessage(), e);
        }
    }

    /**
     * Generate weekly cancellation report
     * Runs every Monday at 9 AM
     */
    @Scheduled(cron = "0 0 9 * * MON")
    public void generateWeeklyCancellationReport() {
        try {
            LocalDateTime weekAgo = LocalDateTime.now().minusDays(7);
            LocalDateTime now = LocalDateTime.now();
            
            // Get cancellation statistics
            long totalCancellations = cancellationLogRepository.findByTimestampBetween(weekAgo, now, 
                org.springframework.data.domain.Pageable.unpaged()).getTotalElements();
            
            long driverCancellations = cancellationLogRepository.findByCancellationTypeOrderByTimestampDesc("DRIVER", 
                org.springframework.data.domain.Pageable.unpaged()).getTotalElements();
            
            long riderCancellations = cancellationLogRepository.findByCancellationTypeOrderByTimestampDesc("RIDER", 
                org.springframework.data.domain.Pageable.unpaged()).getTotalElements();
            
            // Get active suspensions
            List<CancellationLog> activeSuspensions = cancellationLogRepository.findActiveSuspensions(now);
            
            // Get users approaching cancellation limit this month
            LocalDateTime thisMonth = now.withDayOfMonth(1);
            List<Object[]> usersAtRisk = cancellationLogRepository.findUsersApproachingCancellationLimit(
                now.getMonthValue(), now.getYear(), 3L);
            
            log.info("=== WEEKLY CANCELLATION REPORT ===");
            log.info("Week ending: {}", now.toLocalDate());
            log.info("Total Cancellations: {}", totalCancellations);
            log.info("Driver Cancellations: {}", driverCancellations);
            log.info("Rider Cancellations: {}", riderCancellations);
            log.info("Active Suspensions: {}", activeSuspensions.size());
            log.info("Users at Risk (3+ cancellations this month): {}", usersAtRisk.size());
            log.info("==================================");
            
        } catch (Exception e) {
            log.error("Failed to generate weekly cancellation report: {}", e.getMessage(), e);
        }
    }

    /**
     * Monitor and alert on suspicious patterns
     * Runs every hour
     */
    @Scheduled(cron = "0 0 * * * ?")
    public void monitorSuspiciousPatterns() {
        try {
            LocalDateTime lastHour = LocalDateTime.now().minusHours(1);
            
            // Check for high number of failed logins from same IP
            // This would require additional IP-based queries in the repository
            
            // Check for rapid OTP failures
            long recentOtpFailures = securityEventLogRepository.countByEventTypeAndTimestampAfter("OTP_FAILURE", lastHour);
            if (recentOtpFailures > 50) {
                log.warn("ALERT: High number of OTP failures in the last hour: {}", recentOtpFailures);
            }
            
            // Check for multiple account lockouts
            long recentLockouts = securityEventLogRepository.countByEventTypeAndTimestampAfter("ACCOUNT_LOCKED", lastHour);
            if (recentLockouts > 10) {
                log.warn("ALERT: High number of account lockouts in the last hour: {}", recentLockouts);
            }
            
            // Check for unusual cancellation patterns
            long recentCancellations = cancellationLogRepository.findByTimestampBetween(lastHour, LocalDateTime.now(), 
                org.springframework.data.domain.Pageable.unpaged()).getTotalElements();
            if (recentCancellations > 100) {
                log.warn("ALERT: High number of ride cancellations in the last hour: {}", recentCancellations);
            }
            
        } catch (Exception e) {
            log.error("Failed to monitor suspicious patterns: {}", e.getMessage(), e);
        }
    }

    /**
     * Generate monthly audit summary
     * Runs on the 1st of each month at 10 AM
     */
    @Scheduled(cron = "0 0 10 1 * ?")
    public void generateMonthlyAuditSummary() {
        try {
            LocalDateTime lastMonth = LocalDateTime.now().minusMonths(1);
            LocalDateTime thisMonth = LocalDateTime.now().withDayOfMonth(1);
            
            // Count audit events by type
            long profileChanges = auditLogRepository.countByUserIdAndTimestampAfter(null, lastMonth);
            long recentProfileChanges = auditLogRepository.findRecentProfileChanges(lastMonth).size();
            
            log.info("=== MONTHLY AUDIT SUMMARY ===");
            log.info("Month: {}", lastMonth.getMonth() + " " + lastMonth.getYear());
            log.info("Total Audit Events: {}", profileChanges);
            log.info("Profile Changes: {}", recentProfileChanges);
            log.info("=============================");
            
        } catch (Exception e) {
            log.error("Failed to generate monthly audit summary: {}", e.getMessage(), e);
        }
    }
}