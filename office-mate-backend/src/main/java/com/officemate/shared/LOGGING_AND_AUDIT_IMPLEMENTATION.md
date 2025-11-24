# Comprehensive Logging and Audit Trails Implementation

## Overview

This implementation provides comprehensive logging and audit trails for the OfficeMate ride-sharing application, including structured logging, security event tracking, profile change auditing, and cancellation tracking with suspension policy enforcement.

## Components Implemented

### 1. Structured Logging Configuration

**File**: `src/main/resources/logback-spring.xml`

- **JSON-structured logging** using Logstash encoder for better log aggregation
- **Separate log files** for different event types:
  - `officemate.log` - General application logs
  - `officemate-security.log` - Security events
  - `officemate-audit.log` - Audit trails
  - `officemate-cancellation.log` - Cancellation tracking
- **Async appenders** for better performance
- **Rolling file policies** with size and time-based rotation
- **Environment-specific configuration** (dev vs prod)

### 2. Audit Trail System

**Entities**:
- `AuditLog` - Tracks profile changes and entity operations
- `AuditableEntity` - Base class providing automatic audit fields

**Service**: `AuditService`
- Logs profile changes with old/new values
- Tracks entity creation, updates, and deletions
- Captures IP address, user agent, and session information
- Async processing for performance

**Features**:
- Automatic audit logging via AOP aspects
- User context tracking
- IP address and session tracking
- Structured audit data with timestamps

### 3. Security Event Logging

**Entity**: `SecurityEventLog`
- Tracks authentication events, failed attempts, suspicious activities
- Severity levels (LOW, MEDIUM, HIGH, CRITICAL)
- Resolution tracking for security incidents

**Service**: `SecurityEventService`
- Failed login attempts
- OTP verification failures
- Account lockouts
- Suspicious activity detection
- Rate limiting violations
- Token manipulation attempts

**Integration**:
- Spring Security event listeners
- Automatic security event capture
- Real-time threat detection

### 4. Cancellation Tracking and Suspension Policy

**Entity**: `CancellationLog`
- Tracks ride cancellations by drivers and riders
- Automatic penalty calculation
- Suspension policy enforcement

**Service**: `CancellationTrackingService`
- **Policy**: 5 driver cancellations per month = 3-month suspension
- Warning system at 3+ cancellations
- Automatic suspension application
- Active suspension checking

**Features**:
- Monthly cancellation counting
- Automatic penalty enforcement
- Warning notifications
- Suspension status tracking

### 5. Log Management and Monitoring

**Service**: `LogManagementService`
- **Scheduled cleanup** of old logs
- **Daily security reports** with metrics
- **Weekly cancellation reports**
- **Suspicious pattern monitoring**
- **Monthly audit summaries**

**Scheduled Jobs**:
- Daily log cleanup (2 AM)
- Daily security reports (8 AM)
- Weekly cancellation reports (Monday 9 AM)
- Hourly suspicious pattern monitoring
- Monthly audit summaries (1st of month, 10 AM)

### 6. Configuration and Infrastructure

**Files**:
- `AuditConfig.java` - JPA auditing configuration
- `AsyncConfig.java` - Async processing and AOP configuration
- `SecurityEventListener.java` - Spring Security event capture
- `AuditAspect.java` - Automatic audit logging via AOP

**Dependencies Added**:
- `net.logstash.logback:logstash-logback-encoder:7.4`
- `org.springframework:spring-aspects`

## Integration Examples

### 1. MobileAuthService Integration

```java
// Failed OTP verification logging
securityEventService.logFailedOtpVerification(userAccount.getUserId(), validatedPhoneNumber, 
        "MOBILE", "Invalid OTP provided. Remaining attempts: " + remainingAttempts);

// Successful login logging
securityEventService.logSuccessfulLogin(userAccount.getUserId(), validatedPhoneNumber);
```

### 2. Automatic Profile Change Auditing

Profile changes are automatically logged via AOP aspects when using service methods:
- `UserProfileService.updateProfile()`
- `DriverProfileService.updateDriverProfile()`
- `RiderProfileService.updateRiderProfile()`

### 3. Entity Auditing

All entities extending `AuditableEntity` automatically track:
- `createdAt` / `updatedAt` timestamps
- `createdBy` / `updatedBy` user information

## Logging Patterns

### Security Events
```json
{
  "timestamp": "2025-11-16T10:30:00.000Z",
  "level": "WARN",
  "logger": "com.officemate.security",
  "eventType": "LOGIN_FAILURE",
  "severity": "MEDIUM",
  "userId": "123e4567-e89b-12d3-a456-426614174000",
  "phoneNumber": "+1234567890",
  "ipAddress": "192.168.1.100",
  "message": "Failed login attempt: Invalid OTP"
}
```

### Audit Events
```json
{
  "timestamp": "2025-11-16T10:30:00.000Z",
  "level": "INFO",
  "logger": "com.officemate.audit",
  "userId": "123e4567-e89b-12d3-a456-426614174000",
  "entityType": "UserProfile",
  "action": "UPDATE",
  "fieldName": "firstName",
  "oldValue": "John",
  "newValue": "Jane",
  "message": "Profile change logged"
}
```

### Cancellation Events
```json
{
  "timestamp": "2025-11-16T10:30:00.000Z",
  "level": "ERROR",
  "logger": "com.officemate.cancellation",
  "userId": "123e4567-e89b-12d3-a456-426614174000",
  "cancellationCount": "5",
  "suspensionEndDate": "2025-02-16T10:30:00.000Z",
  "message": "SUSPENSION APPLIED: User suspended for 90 days due to 5 driver cancellations"
}
```

## Database Schema

### Audit Logs Table
```sql
CREATE TABLE audit_logs (
    audit_id UUID PRIMARY KEY,
    user_id UUID NOT NULL,
    entity_type VARCHAR(50) NOT NULL,
    entity_id VARCHAR(255) NOT NULL,
    action VARCHAR(20) NOT NULL,
    field_name VARCHAR(100),
    old_value TEXT,
    new_value TEXT,
    ip_address VARCHAR(45),
    user_agent VARCHAR(500),
    session_id VARCHAR(100),
    timestamp TIMESTAMP NOT NULL,
    reason VARCHAR(200)
);
```

### Security Event Logs Table
```sql
CREATE TABLE security_event_logs (
    event_id UUID PRIMARY KEY,
    user_id UUID,
    phone_number VARCHAR(20),
    corporate_email VARCHAR(255),
    event_type VARCHAR(50) NOT NULL,
    event_description VARCHAR(500) NOT NULL,
    ip_address VARCHAR(45),
    user_agent VARCHAR(500),
    session_id VARCHAR(100),
    timestamp TIMESTAMP NOT NULL,
    severity VARCHAR(20),
    additional_data TEXT,
    resolved BOOLEAN DEFAULT FALSE,
    resolved_at TIMESTAMP,
    resolved_by VARCHAR(50)
);
```

### Cancellation Logs Table
```sql
CREATE TABLE cancellation_logs (
    cancellation_id UUID PRIMARY KEY,
    user_id UUID NOT NULL,
    ride_id UUID,
    cancellation_type VARCHAR(20) NOT NULL,
    cancellation_reason VARCHAR(200),
    timestamp TIMESTAMP NOT NULL,
    cancellation_month INTEGER NOT NULL,
    cancellation_year INTEGER NOT NULL,
    minutes_before_ride INTEGER,
    penalty_applied BOOLEAN DEFAULT FALSE,
    penalty_type VARCHAR(50),
    penalty_duration_days INTEGER,
    penalty_start_date TIMESTAMP,
    penalty_end_date TIMESTAMP,
    ip_address VARCHAR(45),
    user_agent VARCHAR(500),
    additional_notes TEXT
);
```

## Monitoring and Alerting

### Daily Security Report Metrics
- Failed login attempts
- OTP verification failures
- Suspicious activities
- Account lockouts
- Unresolved high-severity events

### Weekly Cancellation Report Metrics
- Total cancellations
- Driver vs rider cancellations
- Active suspensions
- Users at risk (approaching cancellation limit)

### Hourly Monitoring Alerts
- High OTP failure rates (>50/hour)
- Multiple account lockouts (>10/hour)
- Unusual cancellation patterns (>100/hour)

## Testing

### Unit Tests
- `AuditServiceTest` - Tests audit logging functionality
- `SecurityEventServiceTest` - Tests security event logging
- Both test suites verify async logging and error handling

### Integration
- Automatic integration with existing authentication services
- AOP-based profile change tracking
- Spring Security event listener integration

## Configuration

### Application Properties
```yaml
logging:
  level:
    com.officemate.security: INFO
    com.officemate.audit: INFO
    com.officemate.cancellation: INFO
  pattern:
    console: "%d{yyyy-MM-dd HH:mm:ss} [%thread] %-5level [%X{userId:-SYSTEM}] [%X{sessionId:-NONE}] %logger{36} - %msg%n"
```

### Environment Variables
- `LOG_LEVEL` - Application log level
- `SECURITY_LOG_LEVEL` - Security event log level
- `SQL_LOG_LEVEL` - Database query log level

## Performance Considerations

1. **Async Processing**: All logging operations are asynchronous to avoid blocking main application threads
2. **Batch Processing**: Log entries are batched for better database performance
3. **Index Optimization**: Database indexes on frequently queried fields (user_id, timestamp, event_type)
4. **Log Rotation**: Automatic log file rotation to manage disk space
5. **Cleanup Jobs**: Scheduled cleanup of old log entries

## Security Considerations

1. **PII Protection**: Phone numbers and emails are masked in logs where appropriate
2. **Access Control**: Log files and database tables have restricted access
3. **Data Retention**: Configurable retention periods for different log types
4. **Encryption**: Log files can be encrypted at rest
5. **Audit Trail Integrity**: Audit logs are immutable once created

## Future Enhancements

1. **Real-time Alerting**: Integration with monitoring systems (Prometheus, Grafana)
2. **Machine Learning**: Anomaly detection for suspicious patterns
3. **Log Aggregation**: Integration with ELK stack or similar
4. **Compliance Reporting**: Automated compliance report generation
5. **Advanced Analytics**: Behavioral analysis and risk scoring