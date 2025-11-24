package com.officemate.config.security;

import com.officemate.shared.service.SecurityEventService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.TimeUnit;

/**
 * Service for monitoring unusual authentication patterns and detecting suspicious behavior.
 * Tracks login patterns, device changes, location changes, and timing anomalies.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AuthenticationPatternMonitoringService {

    private final RedisTemplate<String, String> redisTemplate;
    private final SecurityEventService securityEventService;
    private final RateLimitingService rateLimitingService;

    @Value("${app.security.pattern-monitoring.enabled:true}")
    private boolean patternMonitoringEnabled;

    @Value("${app.security.pattern-monitoring.unusual-hour-threshold:2}")
    private int unusualHourThreshold;

    @Value("${app.security.pattern-monitoring.device-change-threshold:3}")
    private int deviceChangeThreshold;

    @Value("${app.security.pattern-monitoring.location-change-threshold:5}")
    private int locationChangeThreshold;

    private static final String LOGIN_PATTERN_PREFIX = "login_pattern:";
    private static final String DEVICE_HISTORY_PREFIX = "device_history:";
    private static final String LOCATION_HISTORY_PREFIX = "location_history:";
    private static final String TIME_PATTERN_PREFIX = "time_pattern:";
    private static final String VELOCITY_CHECK_PREFIX = "velocity_check:";

    /**
     * Analyze authentication attempt for unusual patterns.
     *
     * @param userId user ID
     * @param phoneNumber phone number
     * @param ipAddress client IP address
     * @param userAgent user agent string
     * @param deviceInfo device information
     * @return analysis result with risk score and recommendations
     */
    @Async
    public void analyzeAuthenticationPattern(String userId, String phoneNumber, String ipAddress, 
                                           String userAgent, Map<String, String> deviceInfo) {
        if (!patternMonitoringEnabled) {
            return;
        }

        try {
            LocalDateTime now = LocalDateTime.now();
            int riskScore = 0;
            List<String> suspiciousIndicators = new ArrayList<>();

            // Analyze time-based patterns
            riskScore += analyzeTimePattern(userId, now, suspiciousIndicators);

            // Analyze device patterns
            riskScore += analyzeDevicePattern(userId, userAgent, deviceInfo, suspiciousIndicators);

            // Analyze location patterns
            riskScore += analyzeLocationPattern(userId, ipAddress, suspiciousIndicators);

            // Analyze velocity (rapid successive attempts)
            riskScore += analyzeVelocityPattern(userId, ipAddress, suspiciousIndicators);

            // Record the authentication attempt
            recordAuthenticationAttempt(userId, now, ipAddress, userAgent);

            // Take action based on risk score
            if (riskScore >= 8) {
                handleHighRiskAuthentication(userId, phoneNumber, riskScore, suspiciousIndicators);
            } else if (riskScore >= 5) {
                handleMediumRiskAuthentication(userId, phoneNumber, riskScore, suspiciousIndicators);
            } else if (riskScore >= 3) {
                handleLowRiskAuthentication(userId, phoneNumber, riskScore, suspiciousIndicators);
            }

            log.debug("Authentication pattern analysis for user {}: risk score {}, indicators: {}", 
                     userId, riskScore, suspiciousIndicators);

        } catch (Exception e) {
            log.error("Error analyzing authentication pattern for user {}: {}", userId, e.getMessage(), e);
        }
    }

    /**
     * Analyze time-based authentication patterns.
     */
    private int analyzeTimePattern(String userId, LocalDateTime now, List<String> indicators) {
        try {
            String timePatternKey = TIME_PATTERN_PREFIX + userId;
            int hour = now.getHour();
            int riskScore = 0;

            // Get historical login hours
            Set<String> loginHours = redisTemplate.opsForSet().members(timePatternKey);
            
            if (loginHours != null && !loginHours.isEmpty()) {
                // Check if current hour is unusual
                String currentHour = String.valueOf(hour);
                if (!loginHours.contains(currentHour)) {
                    // Check if it's significantly different from usual pattern
                    boolean isUnusualTime = true;
                    for (String loginHour : loginHours) {
                        int historicalHour = Integer.parseInt(loginHour);
                        int timeDiff = Math.abs(hour - historicalHour);
                        if (timeDiff <= unusualHourThreshold || timeDiff >= (24 - unusualHourThreshold)) {
                            isUnusualTime = false;
                            break;
                        }
                    }
                    
                    if (isUnusualTime) {
                        riskScore += 2;
                        indicators.add("UNUSUAL_LOGIN_TIME");
                    }
                }
            }

            // Store current hour (keep last 30 days of data)
            redisTemplate.opsForSet().add(timePatternKey, String.valueOf(hour));
            redisTemplate.expire(timePatternKey, Duration.ofDays(30));

            return riskScore;

        } catch (Exception e) {
            log.error("Error analyzing time pattern for user {}: {}", userId, e.getMessage(), e);
            return 0;
        }
    }

    /**
     * Analyze device-based authentication patterns.
     */
    private int analyzeDevicePattern(String userId, String userAgent, Map<String, String> deviceInfo, 
                                   List<String> indicators) {
        try {
            String deviceHistoryKey = DEVICE_HISTORY_PREFIX + userId;
            int riskScore = 0;

            // Create device fingerprint
            String deviceFingerprint = createDeviceFingerprint(userAgent, deviceInfo);

            // Check if device is known
            Set<String> knownDevices = redisTemplate.opsForSet().members(deviceHistoryKey);
            
            if (knownDevices != null && !knownDevices.isEmpty()) {
                if (!knownDevices.contains(deviceFingerprint)) {
                    riskScore += 3;
                    indicators.add("NEW_DEVICE");
                    
                    // Check if too many new devices recently
                    if (knownDevices.size() >= deviceChangeThreshold) {
                        riskScore += 2;
                        indicators.add("FREQUENT_DEVICE_CHANGES");
                    }
                }
            }

            // Store device fingerprint (keep last 10 devices)
            redisTemplate.opsForSet().add(deviceHistoryKey, deviceFingerprint);
            
            // Trim to keep only recent devices
            if (knownDevices != null && knownDevices.size() > 10) {
                // Remove oldest entries (simplified approach)
                redisTemplate.expire(deviceHistoryKey, Duration.ofDays(90));
            } else {
                redisTemplate.expire(deviceHistoryKey, Duration.ofDays(90));
            }

            return riskScore;

        } catch (Exception e) {
            log.error("Error analyzing device pattern for user {}: {}", userId, e.getMessage(), e);
            return 0;
        }
    }

    /**
     * Analyze location-based authentication patterns.
     */
    private int analyzeLocationPattern(String userId, String ipAddress, List<String> indicators) {
        try {
            String locationHistoryKey = LOCATION_HISTORY_PREFIX + userId;
            int riskScore = 0;

            // Get IP geolocation (simplified - using IP prefix as location indicator)
            String locationIndicator = getLocationIndicator(ipAddress);

            // Check if location is known
            Set<String> knownLocations = redisTemplate.opsForSet().members(locationHistoryKey);
            
            if (knownLocations != null && !knownLocations.isEmpty()) {
                if (!knownLocations.contains(locationIndicator)) {
                    riskScore += 2;
                    indicators.add("NEW_LOCATION");
                    
                    // Check if too many location changes
                    if (knownLocations.size() >= locationChangeThreshold) {
                        riskScore += 3;
                        indicators.add("FREQUENT_LOCATION_CHANGES");
                    }
                }
            }

            // Store location indicator
            redisTemplate.opsForSet().add(locationHistoryKey, locationIndicator);
            redisTemplate.expire(locationHistoryKey, Duration.ofDays(60));

            return riskScore;

        } catch (Exception e) {
            log.error("Error analyzing location pattern for user {}: {}", userId, e.getMessage(), e);
            return 0;
        }
    }

    /**
     * Analyze velocity patterns (rapid successive attempts).
     */
    private int analyzeVelocityPattern(String userId, String ipAddress, List<String> indicators) {
        try {
            String velocityKey = VELOCITY_CHECK_PREFIX + userId;
            int riskScore = 0;

            // Check recent authentication attempts
            Long attemptCount = redisTemplate.opsForValue().increment(velocityKey);
            
            if (attemptCount == null) {
                return 0;
            }

            // Set expiry on first attempt (5-minute window)
            if (attemptCount == 1) {
                redisTemplate.expire(velocityKey, Duration.ofMinutes(5));
            }

            // Check for rapid attempts
            if (attemptCount > 3) {
                riskScore += 4;
                indicators.add("RAPID_AUTHENTICATION_ATTEMPTS");
            } else if (attemptCount > 2) {
                riskScore += 2;
                indicators.add("MULTIPLE_QUICK_ATTEMPTS");
            }

            return riskScore;

        } catch (Exception e) {
            log.error("Error analyzing velocity pattern for user {}: {}", userId, e.getMessage(), e);
            return 0;
        }
    }

    /**
     * Record authentication attempt for pattern analysis.
     */
    private void recordAuthenticationAttempt(String userId, LocalDateTime timestamp, 
                                           String ipAddress, String userAgent) {
        try {
            String patternKey = LOGIN_PATTERN_PREFIX + userId;
            String attemptData = timestamp.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME) + 
                               "|" + ipAddress + "|" + userAgent;
            
            // Store recent attempts (last 100)
            redisTemplate.opsForList().leftPush(patternKey, attemptData);
            redisTemplate.opsForList().trim(patternKey, 0, 99);
            redisTemplate.expire(patternKey, Duration.ofDays(30));

        } catch (Exception e) {
            log.error("Error recording authentication attempt for user {}: {}", userId, e.getMessage(), e);
        }
    }

    /**
     * Handle high-risk authentication attempts.
     */
    private void handleHighRiskAuthentication(String userId, String phoneNumber, int riskScore, 
                                            List<String> indicators) {
        try {
            // Lock account temporarily
            rateLimitingService.lockAccount(phoneNumber, 
                    "High-risk authentication pattern detected (score: " + riskScore + ")", userId);

            // Log critical security event
            securityEventService.logSuspiciousActivity(
                    UUID.fromString(userId), phoneNumber, null,
                    "HIGH_RISK_AUTHENTICATION_PATTERN",
                    "Risk score: " + riskScore + ", Indicators: " + String.join(", ", indicators),
                    "CRITICAL"
            );

            log.error("HIGH RISK authentication pattern detected for user {}: score {}, indicators: {}", 
                     userId, riskScore, indicators);

        } catch (Exception e) {
            log.error("Error handling high-risk authentication for user {}: {}", userId, e.getMessage(), e);
        }
    }

    /**
     * Handle medium-risk authentication attempts.
     */
    private void handleMediumRiskAuthentication(String userId, String phoneNumber, int riskScore, 
                                              List<String> indicators) {
        try {
            // Track suspicious activity
            rateLimitingService.trackSuspiciousActivity(phoneNumber, "MEDIUM_RISK_PATTERN", userId);

            // Log security event
            securityEventService.logSuspiciousActivity(
                    UUID.fromString(userId), phoneNumber, null,
                    "MEDIUM_RISK_AUTHENTICATION_PATTERN",
                    "Risk score: " + riskScore + ", Indicators: " + String.join(", ", indicators),
                    "HIGH"
            );

            log.warn("MEDIUM RISK authentication pattern detected for user {}: score {}, indicators: {}", 
                    userId, riskScore, indicators);

        } catch (Exception e) {
            log.error("Error handling medium-risk authentication for user {}: {}", userId, e.getMessage(), e);
        }
    }

    /**
     * Handle low-risk authentication attempts.
     */
    private void handleLowRiskAuthentication(String userId, String phoneNumber, int riskScore, 
                                           List<String> indicators) {
        try {
            // Log informational security event
            securityEventService.logSuspiciousActivity(
                    UUID.fromString(userId), phoneNumber, null,
                    "LOW_RISK_AUTHENTICATION_PATTERN",
                    "Risk score: " + riskScore + ", Indicators: " + String.join(", ", indicators),
                    "MEDIUM"
            );

            log.info("LOW RISK authentication pattern detected for user {}: score {}, indicators: {}", 
                    userId, riskScore, indicators);

        } catch (Exception e) {
            log.error("Error handling low-risk authentication for user {}: {}", userId, e.getMessage(), e);
        }
    }

    /**
     * Create device fingerprint from user agent and device info.
     */
    private String createDeviceFingerprint(String userAgent, Map<String, String> deviceInfo) {
        StringBuilder fingerprint = new StringBuilder();
        
        if (userAgent != null) {
            fingerprint.append(userAgent.hashCode());
        }
        
        if (deviceInfo != null) {
            deviceInfo.entrySet().stream()
                    .sorted(Map.Entry.comparingByKey())
                    .forEach(entry -> fingerprint.append("|").append(entry.getValue()));
        }
        
        return fingerprint.toString();
    }

    /**
     * Get location indicator from IP address (simplified geolocation).
     */
    private String getLocationIndicator(String ipAddress) {
        if (ipAddress == null) {
            return "UNKNOWN";
        }
        
        // Simplified approach - use first two octets as location indicator
        String[] parts = ipAddress.split("\\.");
        if (parts.length >= 2) {
            return parts[0] + "." + parts[1] + ".x.x";
        }
        
        return ipAddress;
    }

    /**
     * Get authentication risk assessment for a user.
     *
     * @param userId user ID
     * @return risk assessment data
     */
    public Map<String, Object> getAuthenticationRiskAssessment(String userId) {
        Map<String, Object> assessment = new HashMap<>();
        
        try {
            // Get recent authentication patterns
            String patternKey = LOGIN_PATTERN_PREFIX + userId;
            List<String> recentAttempts = redisTemplate.opsForList().range(patternKey, 0, 9);
            
            assessment.put("recentAttemptCount", recentAttempts != null ? recentAttempts.size() : 0);
            assessment.put("patternMonitoringEnabled", patternMonitoringEnabled);
            
            // Get device count
            String deviceHistoryKey = DEVICE_HISTORY_PREFIX + userId;
            Long deviceCount = redisTemplate.opsForSet().size(deviceHistoryKey);
            assessment.put("knownDeviceCount", deviceCount != null ? deviceCount : 0);
            
            // Get location count
            String locationHistoryKey = LOCATION_HISTORY_PREFIX + userId;
            Long locationCount = redisTemplate.opsForSet().size(locationHistoryKey);
            assessment.put("knownLocationCount", locationCount != null ? locationCount : 0);
            
        } catch (Exception e) {
            log.error("Error getting risk assessment for user {}: {}", userId, e.getMessage(), e);
            assessment.put("error", "Unable to retrieve risk assessment");
        }
        
        return assessment;
    }
}