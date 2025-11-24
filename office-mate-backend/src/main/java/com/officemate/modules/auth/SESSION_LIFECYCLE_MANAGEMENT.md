# Session Lifecycle Management Implementation

## Overview

This document describes the implementation of session lifecycle management with multi-device support for the OfficeMate authentication system. The implementation provides comprehensive session tracking, automatic cleanup, and persistent audit trails.

## Architecture

### Dual Storage Strategy

The session management system uses a dual storage approach:

1. **Redis (Primary Session Storage)**
   - Fast, in-memory storage for active sessions
   - Automatic TTL-based expiration
   - Optimized for high-frequency read/write operations
   - Entity: `UserSession`

2. **PostgreSQL (Persistent Metadata Storage)**
   - Long-term audit trail and session history
   - Multi-device session tracking
   - Security event logging
   - Entity: `SessionMetadata`

### Components

#### 1. UserSession (Redis Entity)
- **Location**: `com.officemate.modules.auth.entity.UserSession`
- **Storage**: Redis with `@RedisHash("user_sessions")`
- **TTL**: Automatic expiration based on refresh token lifetime
- **Purpose**: Active session tracking with fast access

**Key Fields**:
- `sessionId`: Unique session identifier (Redis key)
- `userId`: User ID (indexed for multi-session queries)
- `deviceType`, `deviceId`, `appVersion`: Device tracking
- `permissions`: List of granted permissions
- `refreshToken`: Associated refresh token
- `mobileVerified`, `emailVerified`: Verification status
- `ttl`: Time-to-live in seconds

#### 2. SessionMetadata (PostgreSQL Entity)
- **Location**: `com.officemate.modules.auth.entity.SessionMetadata`
- **Storage**: PostgreSQL table `session_metadata`
- **Purpose**: Persistent audit trail and session history

**Key Fields**:
- `metadataId`: Primary key (UUID)
- `sessionId`: Reference to Redis session
- `userId`: User ID (foreign key to user_accounts)
- `deviceType`, `deviceId`, `appVersion`: Device information
- `ipAddress`, `userAgent`: Network information
- `createdAt`, `lastActivityAt`, `endedAt`: Timestamps
- `terminationReason`: Why session ended
- `isActive`: Current status flag
- `mobileVerified`, `emailVerified`: Verification status at creation

#### 3. SessionManagementService
- **Location**: `com.officemate.modules.auth.service.SessionManagementService`
- **Purpose**: Core session management logic

**Key Methods**:
- `createSession()`: Create new session in both Redis and PostgreSQL
- `validateToken()`: Validate JWT and update last access time
- `refreshSession()`: Refresh access token and update permissions
- `revokeSession()`: Revoke specific session
- `revokeAllSessions()`: Revoke all user sessions (security event)
- `revokeDeviceSessions()`: Revoke sessions for specific device
- `getUserSessionsWithMetadata()`: Get active sessions with metadata
- `getSessionHistory()`: Get complete session history
- `isSessionActive()`: Check if session is still active

#### 4. SessionCleanupJob
- **Location**: `com.officemate.modules.auth.scheduled.SessionCleanupJob`
- **Purpose**: Automated session cleanup and maintenance

**Scheduled Tasks**:

1. **Expired Session Cleanup** (Hourly)
   - Cron: `0 0 * * * *` (every hour)
   - Removes expired sessions from Redis
   - Updates metadata with "EXPIRED" termination reason

2. **Inactive Session Cleanup** (Daily at 2 AM)
   - Cron: `0 0 2 * * *`
   - Removes sessions inactive for 30+ days
   - Updates metadata with "INACTIVE" termination reason

3. **Session Metadata Archival** (Weekly on Sunday at 3 AM)
   - Cron: `0 0 3 * * SUN`
   - Deletes metadata for sessions ended 90+ days ago
   - Maintains database size and performance

4. **Session State Synchronization** (Every 6 hours)
   - Cron: `0 0 */6 * * *`
   - Ensures consistency between Redis and PostgreSQL
   - Updates metadata for sessions expired in Redis

## Multi-Device Support

### Device Tracking

Each session tracks:
- Device type (IOS, ANDROID, WEB)
- Unique device identifier
- Application version
- IP address
- User agent string

### Multi-Session Management

Users can have multiple active sessions across different devices:

```java
// Get all active sessions for a user
List<SessionMetadata> sessions = sessionManagementService
    .getUserSessionsWithMetadata(userId);

// Count active sessions
long sessionCount = sessionManagementService
    .getActiveSessionCount(userId);

// Revoke sessions for a specific device
sessionManagementService.revokeDeviceSessions(userId, deviceId);
```

### Session History

Complete session history is maintained in PostgreSQL:

```java
// Get session history (active and ended)
List<SessionMetadata> history = sessionManagementService
    .getSessionHistory(userId);
```

## Session Lifecycle

### 1. Session Creation

When a user logs in:

1. Generate session ID and JWT tokens
2. Create `UserSession` in Redis with TTL
3. Create `SessionMetadata` in PostgreSQL
4. Store device information and verification status
5. Return tokens to client

```java
SessionTokens tokens = sessionManagementService.createSession(
    userAccount, 
    deviceInfo
);
```

### 2. Session Activity

On each authenticated request:

1. Validate JWT token
2. Check if token is revoked
3. Update `lastAccessAt` in both Redis and PostgreSQL
4. Return validation result with permissions

```java
TokenValidation validation = sessionManagementService.validateToken(token);
```

### 3. Session Refresh

When access token expires:

1. Validate refresh token
2. Check session still exists in Redis
3. Generate new access token with updated permissions
4. Update session metadata
5. Return new tokens

```java
SessionTokens newTokens = sessionManagementService.refreshSession(
    refreshToken, 
    userAccount
);
```

### 4. Session Termination

Sessions can end in several ways:

**User Logout**:
```java
sessionManagementService.revokeSession(sessionId);
// Reason: "USER_LOGOUT"
```

**Security Event** (password change, account compromise):
```java
sessionManagementService.revokeAllSessions(userId);
// Reason: "SECURITY_EVENT"
```

**Device Revocation** (lost/stolen device):
```java
sessionManagementService.revokeDeviceSessions(userId, deviceId);
// Reason: "DEVICE_REVOKED"
```

**Automatic Expiration**:
- Redis TTL expires session automatically
- Cleanup job updates metadata
- Reason: "EXPIRED"

**Inactivity**:
- Cleanup job detects 30+ days of inactivity
- Removes session and updates metadata
- Reason: "INACTIVE"

## Session Expiration

### TTL Configuration

Session expiration is configured in `application.yml`:

```yaml
app:
  security:
    jwt:
      expiration: 3600000          # Access token: 1 hour
      refresh-expiration: 86400000 # Refresh token: 24 hours
```

### Automatic Cleanup

Redis handles automatic expiration via TTL:
- `@TimeToLive` annotation on `UserSession.ttl` field
- TTL set to refresh token expiration time
- Redis automatically removes expired entries

Scheduled cleanup provides additional safety:
- Hourly check for expired sessions
- Updates PostgreSQL metadata
- Ensures consistency

## Security Features

### Token Revocation

Revoked tokens are stored in Redis:
- Entity: `RevokedToken`
- TTL matches token expiration
- Checked on every token validation

### Session Revocation Reasons

- `USER_LOGOUT`: User explicitly logged out
- `SECURITY_EVENT`: Password change, account compromise
- `DEVICE_REVOKED`: Device lost or stolen
- `EXPIRED`: Token expired naturally
- `EXPIRED_IN_REDIS`: Session expired in Redis
- `INACTIVE`: No activity for 30+ days

### Audit Trail

All session events are logged in PostgreSQL:
- Session creation with device info
- Last activity timestamps
- Session termination with reason
- Complete session history per user

## Database Schema

### session_metadata Table

```sql
CREATE TABLE session_metadata (
    metadata_id UUID PRIMARY KEY,
    session_id VARCHAR(100) NOT NULL,
    user_id UUID NOT NULL,
    device_type VARCHAR(20),
    device_id VARCHAR(255),
    app_version VARCHAR(50),
    ip_address VARCHAR(45),
    user_agent VARCHAR(500),
    created_at TIMESTAMP NOT NULL,
    last_activity_at TIMESTAMP,
    ended_at TIMESTAMP,
    termination_reason VARCHAR(100),
    is_active BOOLEAN NOT NULL,
    mobile_verified BOOLEAN,
    email_verified BOOLEAN,
    FOREIGN KEY (user_id) REFERENCES user_accounts(user_id)
);
```

### Indexes

- `idx_session_metadata_user_id`: Fast user session lookup
- `idx_session_metadata_session_id`: Fast session lookup
- `idx_session_metadata_created_at`: Time-based queries
- `idx_session_metadata_ended_at`: Cleanup queries
- `idx_session_metadata_device_id`: Device-based queries
- `idx_session_metadata_active`: Active session queries

## Configuration

### Enable Scheduling

Scheduling is enabled in the main application class:

```java
@SpringBootApplication
@EnableScheduling
public class OfficemateApplication {
    // ...
}
```

### Redis Configuration

Redis is configured in `application.yml`:

```yaml
spring:
  data:
    redis:
      host: ${REDIS_HOST:localhost}
      port: ${REDIS_PORT:6379}
      password: ${REDIS_PASSWORD:}
      database: ${REDIS_DATABASE:0}
      timeout: ${REDIS_TIMEOUT:2000}
```

## Usage Examples

### Create Session on Login

```java
@Autowired
private SessionManagementService sessionManagementService;

public AuthResponse login(String phoneNumber, String otp, DeviceInfo deviceInfo) {
    // Verify OTP and get user account
    UserAccount userAccount = verifyAndGetUser(phoneNumber, otp);
    
    // Create session
    SessionTokens tokens = sessionManagementService.createSession(
        userAccount, 
        deviceInfo
    );
    
    return AuthResponse.builder()
        .accessToken(tokens.getAccessToken())
        .refreshToken(tokens.getRefreshToken())
        .userId(tokens.getUserId())
        .build();
}
```

### Validate Token on Request

```java
public void validateRequest(String token) {
    TokenValidation validation = sessionManagementService.validateToken(token);
    
    if (!validation.isValid()) {
        throw new UnauthorizedException(validation.getErrorMessage());
    }
    
    if (!validation.isEmailVerified()) {
        throw new ForbiddenException("Email verification required");
    }
}
```

### Logout User

```java
public void logout(String sessionId) {
    sessionManagementService.revokeSession(sessionId);
}
```

### Handle Security Event

```java
public void handlePasswordChange(String userId) {
    // Revoke all sessions to force re-login
    sessionManagementService.revokeAllSessions(userId);
}
```

### Get User's Active Sessions

```java
public List<SessionMetadata> getUserDevices(String userId) {
    return sessionManagementService.getUserSessionsWithMetadata(userId);
}
```

### Revoke Lost Device

```java
public void reportLostDevice(String userId, String deviceId) {
    sessionManagementService.revokeDeviceSessions(userId, deviceId);
}
```

## Monitoring and Maintenance

### Metrics to Monitor

1. **Active Session Count**: Track total active sessions
2. **Sessions Per User**: Average and max sessions per user
3. **Session Duration**: Average session lifetime
4. **Cleanup Job Performance**: Execution time and records processed
5. **Redis Memory Usage**: Monitor session storage size

### Maintenance Tasks

1. **Regular Cleanup**: Automated via scheduled jobs
2. **Metadata Archival**: Old sessions deleted after 90 days
3. **Consistency Checks**: Sync job runs every 6 hours
4. **Performance Tuning**: Monitor and adjust TTL values

## Testing

### Unit Tests

Test session lifecycle operations:
- Session creation
- Token validation
- Session refresh
- Session revocation
- Multi-device management

### Integration Tests

Test with actual Redis and PostgreSQL:
- End-to-end session flows
- Cleanup job execution
- Consistency between stores
- Concurrent session handling

## Requirements Satisfied

This implementation satisfies the following requirements from task 8.2:

✅ **Session storage and retrieval using Spring Data Redis**
- `UserSession` entity with `@RedisHash`
- `UserSessionRepository` with Redis operations
- Automatic TTL-based expiration

✅ **Session expiration and cleanup logic with Redis TTL**
- `@TimeToLive` annotation on UserSession
- Scheduled cleanup jobs (hourly, daily, weekly)
- Automatic Redis expiration

✅ **Device tracking and multi-device support**
- Device information in both Redis and PostgreSQL
- Multi-session queries per user
- Device-specific session revocation

✅ **Session revocation for security events**
- Individual session revocation
- All sessions revocation
- Device-specific revocation
- Token blacklisting

✅ **Session metadata storage in PostgreSQL**
- `SessionMetadata` entity with JPA
- Complete audit trail
- Session history tracking
- Indexed queries for performance

## Related Requirements

- **Requirement 2.4**: JWT token generation and validation with permissions
- **Requirement 2.5**: Session lifecycle management
- **Requirement 8.3**: Multi-device session support
