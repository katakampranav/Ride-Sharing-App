package com.officemate.shared.service;

import com.officemate.shared.entity.CancellationLog;
import com.officemate.shared.repository.CancellationLogRepository;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Service for tracking ride cancellations and enforcing suspension policies
 * Implements the 3-month suspension policy after 5 driver cancellations per month
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class CancellationTrackingService {

    private final CancellationLogRepository cancellationLogRepository;
    private static final int MAX_DRIVER_CANCELLATIONS_PER_MONTH = 5;
    private static final int SUSPENSION_DURATION_DAYS = 90; // 3 months

    /**
     * Log a ride cancellation and check for suspension policy enforcement
     */
    @Async
    public void logCancellation(UUID userId, UUID rideId, String cancellationType, 
                              String reason, Integer minutesBeforeRide, String additionalNotes) {
        try {
            HttpServletRequest request = getCurrentRequest();
            LocalDateTime now = LocalDateTime.now();
            
            CancellationLog cancellationLog = CancellationLog.builder()
                .userId(userId)
                .rideId(rideId)
                .cancellationType(cancellationType)
                .cancellationReason(reason)
                .timestamp(now)
                .cancellationMonth(now.getMonthValue())
                .cancellationYear(now.getYear())
                .minutesBeforeRide(minutesBeforeRide)
                .ipAddress(getClientIpAddress(request))
                .userAgent(getUserAgent(request))
                .additionalNotes(additionalNotes)
                .build();

            cancellationLogRepository.save(cancellationLog);
            
            // Log the cancellation event
            MDC.put("userId", userId.toString());
            MDC.put("rideId", rideId != null ? rideId.toString() : "UNKNOWN");
            MDC.put("cancellationType", cancellationType);
            MDC.put("reason", reason);
            MDC.put("minutesBeforeRide", minutesBeforeRide != null ? minutesBeforeRide.toString() : "UNKNOWN");
            
            log.info("Ride cancellation logged: {} cancellation by user {} for ride {} - Reason: {}", 
                    cancellationType, userId, rideId, reason);
            
            // Check for suspension policy enforcement if it's a driver cancellation
            if ("DRIVER".equals(cancellationType)) {
                checkAndEnforceSuspensionPolicy(userId, now.getMonthValue(), now.getYear());
            }
            
            MDC.clear();
            
        } catch (Exception e) {
            log.error("Failed to log cancellation for user {}: {}", userId, e.getMessage(), e);
        }
    }

    /**
     * Check if a user is currently suspended
     */
    public boolean isUserSuspended(UUID userId) {
        return cancellationLogRepository.isUserSuspended(userId, LocalDateTime.now());
    }

    /**
     * Get active suspension details for a user
     */
    public CancellationLog getActiveSuspension(UUID userId) {
        List<CancellationLog> activeSuspensions = cancellationLogRepository.findActiveSuspensions(LocalDateTime.now());
        return activeSuspensions.stream()
                .filter(suspension -> suspension.getUserId().equals(userId))
                .findFirst()
                .orElse(null);
    }

    /**
     * Get driver cancellation count for current month
     */
    public long getDriverCancellationCountThisMonth(UUID userId) {
        LocalDateTime now = LocalDateTime.now();
        return cancellationLogRepository.countDriverCancellationsInMonth(userId, now.getMonthValue(), now.getYear());
    }

    /**
     * Get driver cancellation count for a specific month
     */
    public long getDriverCancellationCount(UUID userId, int month, int year) {
        return cancellationLogRepository.countDriverCancellationsInMonth(userId, month, year);
    }

    /**
     * Check and enforce suspension policy for driver cancellations
     */
    private void checkAndEnforceSuspensionPolicy(UUID userId, int month, int year) {
        try {
            long cancellationCount = cancellationLogRepository.countDriverCancellationsInMonth(userId, month, year);
            
            MDC.put("userId", userId.toString());
            MDC.put("cancellationCount", String.valueOf(cancellationCount));
            MDC.put("month", String.valueOf(month));
            MDC.put("year", String.valueOf(year));
            
            if (cancellationCount >= MAX_DRIVER_CANCELLATIONS_PER_MONTH) {
                // Check if suspension has already been applied for this month
                List<CancellationLog> monthCancellations = cancellationLogRepository
                        .findDriverCancellationsInMonth(userId, month, year);
                
                boolean suspensionAlreadyApplied = monthCancellations.stream()
                        .anyMatch(c -> c.getPenaltyApplied() && "SUSPENSION".equals(c.getPenaltyType()));
                
                if (!suspensionAlreadyApplied) {
                    applySuspension(userId, cancellationCount, month, year);
                }
            } else if (cancellationCount >= 3) {
                // Issue warning for users approaching the limit
                issueWarning(userId, cancellationCount, month, year);
            }
            
            MDC.clear();
            
        } catch (Exception e) {
            log.error("Failed to check suspension policy for user {}: {}", userId, e.getMessage(), e);
        }
    }

    /**
     * Apply suspension to a user
     */
    private void applySuspension(UUID userId, long cancellationCount, int month, int year) {
        try {
            LocalDateTime now = LocalDateTime.now();
            LocalDateTime suspensionEnd = now.plusDays(SUSPENSION_DURATION_DAYS);
            
            // Create a suspension record
            CancellationLog suspensionLog = CancellationLog.builder()
                .userId(userId)
                .cancellationType("DRIVER")
                .cancellationReason("AUTOMATIC_SUSPENSION")
                .timestamp(now)
                .cancellationMonth(month)
                .cancellationYear(year)
                .penaltyApplied(true)
                .penaltyType("SUSPENSION")
                .penaltyDurationDays(SUSPENSION_DURATION_DAYS)
                .penaltyStartDate(now)
                .penaltyEndDate(suspensionEnd)
                .additionalNotes(String.format("Automatic suspension applied due to %d driver cancellations in %d/%d", 
                                              cancellationCount, month, year))
                .build();
            
            cancellationLogRepository.save(suspensionLog);
            
            MDC.put("userId", userId.toString());
            MDC.put("suspensionEndDate", suspensionEnd.toString());
            MDC.put("cancellationCount", String.valueOf(cancellationCount));
            
            log.error("SUSPENSION APPLIED: User {} suspended for {} days due to {} driver cancellations in {}/{}", 
                     userId, SUSPENSION_DURATION_DAYS, cancellationCount, month, year);
            
            MDC.clear();
            
        } catch (Exception e) {
            log.error("Failed to apply suspension for user {}: {}", userId, e.getMessage(), e);
        }
    }

    /**
     * Issue warning to user approaching cancellation limit
     */
    private void issueWarning(UUID userId, long cancellationCount, int month, int year) {
        try {
            LocalDateTime now = LocalDateTime.now();
            
            CancellationLog warningLog = CancellationLog.builder()
                .userId(userId)
                .cancellationType("DRIVER")
                .cancellationReason("WARNING_ISSUED")
                .timestamp(now)
                .cancellationMonth(month)
                .cancellationYear(year)
                .penaltyApplied(true)
                .penaltyType("WARNING")
                .additionalNotes(String.format("Warning issued: %d/%d driver cancellations in %d/%d", 
                                              cancellationCount, MAX_DRIVER_CANCELLATIONS_PER_MONTH, month, year))
                .build();
            
            cancellationLogRepository.save(warningLog);
            
            MDC.put("userId", userId.toString());
            MDC.put("cancellationCount", String.valueOf(cancellationCount));
            MDC.put("remainingCancellations", String.valueOf(MAX_DRIVER_CANCELLATIONS_PER_MONTH - cancellationCount));
            
            log.warn("WARNING ISSUED: User {} has {} driver cancellations in {}/{}. {} more will result in suspension", 
                    userId, cancellationCount, month, year, MAX_DRIVER_CANCELLATIONS_PER_MONTH - cancellationCount);
            
            MDC.clear();
            
        } catch (Exception e) {
            log.error("Failed to issue warning for user {}: {}", userId, e.getMessage(), e);
        }
    }

    private HttpServletRequest getCurrentRequest() {
        try {
            ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.currentRequestAttributes();
            return attributes.getRequest();
        } catch (Exception e) {
            return null;
        }
    }

    private String getClientIpAddress(HttpServletRequest request) {
        if (request == null) return "UNKNOWN";
        
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            return xForwardedFor.split(",")[0].trim();
        }
        
        String xRealIp = request.getHeader("X-Real-IP");
        if (xRealIp != null && !xRealIp.isEmpty()) {
            return xRealIp;
        }
        
        return request.getRemoteAddr();
    }

    private String getUserAgent(HttpServletRequest request) {
        if (request == null) return "UNKNOWN";
        return request.getHeader("User-Agent");
    }
}