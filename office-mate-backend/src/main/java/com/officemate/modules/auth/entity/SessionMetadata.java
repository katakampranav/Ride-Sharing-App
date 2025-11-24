package com.officemate.modules.auth.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * PostgreSQL entity for storing session metadata and audit information.
 * Provides persistent storage for session history and security auditing.
 */
@Entity
@Table(name = "session_metadata", indexes = {
    @Index(name = "idx_session_metadata_user_id", columnList = "user_id"),
    @Index(name = "idx_session_metadata_session_id", columnList = "session_id"),
    @Index(name = "idx_session_metadata_created_at", columnList = "created_at"),
    @Index(name = "idx_session_metadata_ended_at", columnList = "ended_at")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SessionMetadata {
    
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "metadata_id")
    private UUID metadataId;
    
    /**
     * Session ID from Redis
     */
    @Column(name = "session_id", nullable = false, length = 100)
    private String sessionId;
    
    /**
     * User ID associated with this session
     */
    @Column(name = "user_id", nullable = false)
    private UUID userId;
    
    /**
     * Device type (IOS, ANDROID, WEB)
     */
    @Column(name = "device_type", length = 20)
    private String deviceType;
    
    /**
     * Unique device identifier
     */
    @Column(name = "device_id", length = 255)
    private String deviceId;
    
    /**
     * Application version
     */
    @Column(name = "app_version", length = 50)
    private String appVersion;
    
    /**
     * IP address when session was created
     */
    @Column(name = "ip_address", length = 45)
    private String ipAddress;
    
    /**
     * User agent string
     */
    @Column(name = "user_agent", length = 500)
    private String userAgent;
    
    /**
     * Timestamp when session was created
     */
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;
    
    /**
     * Timestamp of last activity
     */
    @Column(name = "last_activity_at")
    private LocalDateTime lastActivityAt;
    
    /**
     * Timestamp when session ended (logout or expiration)
     */
    @Column(name = "ended_at")
    private LocalDateTime endedAt;
    
    /**
     * Reason for session termination
     */
    @Column(name = "termination_reason", length = 100)
    private String terminationReason;
    
    /**
     * Flag indicating if session is currently active
     */
    @Column(name = "is_active", nullable = false)
    private boolean active;
    
    /**
     * Mobile verification status at session creation
     */
    @Column(name = "mobile_verified")
    private boolean mobileVerified;
    
    /**
     * Email verification status at session creation
     */
    @Column(name = "email_verified")
    private boolean emailVerified;
    
    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
        if (lastActivityAt == null) {
            lastActivityAt = LocalDateTime.now();
        }
        active = true;
    }
    
    /**
     * Mark session as ended
     */
    public void endSession(String reason) {
        this.endedAt = LocalDateTime.now();
        this.terminationReason = reason;
        this.active = false;
    }
    
    /**
     * Update last activity timestamp
     */
    public void updateActivity() {
        this.lastActivityAt = LocalDateTime.now();
    }
}
