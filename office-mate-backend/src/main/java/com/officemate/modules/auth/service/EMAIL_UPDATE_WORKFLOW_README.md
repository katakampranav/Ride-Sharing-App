# Email Update Workflow for Company Changes

## Overview

This document describes the secure email update workflow implemented for users who change companies. The workflow ensures data preservation, security through multi-factor verification, and comprehensive audit logging.

## Key Features

1. **Mobile OTP Verification**: Requires mobile OTP verification before any email changes
2. **Email OTP Verification**: Requires verification of the new email address
3. **Data Preservation**: All user data, wallet balance, and ride history are preserved during email changes
4. **Audit Logging**: Complete audit trail of all email change operations
5. **Security**: Multi-step verification process prevents unauthorized email changes

## Workflow Steps

### 1. Email Update (Company Change)

When a user changes companies and needs to update their corporate email:

#### Step 1: Initiate Email Update with Mobile OTP

```java
VerificationResponse response = emailVerificationService.initiateEmailUpdate(
    userId,
    mobileOtp,           // OTP sent to user's registered mobile number
    newCorporateEmail,   // New corporate email address
    "Changed companies", // Reason for change
    ipAddress,           // Client IP for audit
    userAgent            // Client user agent for audit
);
```

**Process:**
- Validates new email format
- Verifies mobile OTP for security authorization
- Checks if new email already exists for another user
- Creates audit log entry with MOBILE_VERIFIED status
- Removes old corporate email from user account
- Sends OTP to new email address
- Returns verification response with audit log ID

#### Step 2: Complete Email Update with Email OTP

```java
VerificationResponse response = emailVerificationService.completeEmailUpdate(
    userId,
    emailOtp  // OTP sent to new email address
);
```

**Process:**
- Verifies OTP sent to new email address
- Updates user account with new verified email
- Updates audit log to COMPLETED status
- Returns verification response

### 2. Email Removal

When a user needs to remove their corporate email:

```java
emailVerificationService.removeCorporateEmail(
    userId,
    mobileOtp,           // OTP sent to user's registered mobile number
    "Leaving company",   // Reason for removal
    ipAddress,           // Client IP for audit
    userAgent            // Client user agent for audit
);
```

**Process:**
- Verifies mobile OTP for security authorization
- Creates audit log entry with REMOVAL type
- Removes corporate email from user account
- Deletes any pending verification records

## Data Preservation

During email updates, the following data is preserved:

- User profile information (name, phone number, etc.)
- Wallet balance and payment methods
- Ride history (as driver and rider)
- Driver profile (vehicle information, license)
- Rider profile (preferences, favorite drivers)
- Emergency contacts and safety settings
- Session tokens remain valid

**Only the corporate email is changed** - all other user data remains intact.

## Audit Logging

Every email change operation is logged in the `email_change_audit_logs` table with:

- User ID
- Old email address
- New email address
- Change type (ADDITION, UPDATE, REMOVAL)
- Change reason
- IP address and user agent
- Mobile OTP verification status
- Email OTP verification status
- Timestamp
- Operation status (INITIATED, MOBILE_VERIFIED, EMAIL_VERIFIED, COMPLETED, FAILED)
- Additional notes

### Audit Log Status Flow

```
INITIATED → MOBILE_VERIFIED → EMAIL_VERIFIED → COMPLETED
                                              ↓
                                           FAILED
```

## Security Considerations

1. **Multi-Factor Verification**: Both mobile and email OTP required
2. **Rate Limiting**: OTP requests are rate-limited (5 per hour by default)
3. **OTP Expiration**: OTPs expire after 5 minutes
4. **Attempt Limiting**: Maximum 3 OTP verification attempts
5. **Audit Trail**: Complete logging of all operations
6. **IP Tracking**: Client IP and user agent logged for security analysis

## Error Handling

### Common Errors

- `INVALID_EMAIL_FORMAT`: New email format is invalid
- `EMAIL_ALREADY_EXISTS`: New email is already registered to another account
- `INVALID_MOBILE_OTP`: Mobile OTP verification failed
- `INVALID_OTP`: Email OTP verification failed
- `USER_NOT_FOUND`: User account not found
- `OTP_EXPIRED`: OTP has expired
- `MAX_ATTEMPTS_EXCEEDED`: Too many failed OTP attempts

## API Usage Examples

### Complete Email Update Flow

```java
// Step 1: User requests mobile OTP
String mobileOtp = otpService.generateMobileOTP(userPhoneNumber);
// Send OTP via SMS...

// Step 2: User submits mobile OTP and new email
VerificationResponse step1Response = emailVerificationService.initiateEmailUpdate(
    userId,
    mobileOtp,
    "newuser@newcompany.com",
    "Changed companies from OldCorp to NewCorp",
    request.getRemoteAddr(),
    request.getHeader("User-Agent")
);

// Step 3: System sends email OTP to new address
// User receives OTP at newuser@newcompany.com

// Step 4: User submits email OTP
VerificationResponse step2Response = emailVerificationService.completeEmailUpdate(
    userId,
    emailOtp
);

// Email update complete - user can now login with new email
```

### Email Removal Flow

```java
// Step 1: User requests mobile OTP
String mobileOtp = otpService.generateMobileOTP(userPhoneNumber);
// Send OTP via SMS...

// Step 2: User submits mobile OTP to remove email
emailVerificationService.removeCorporateEmail(
    userId,
    mobileOtp,
    "Left company",
    request.getRemoteAddr(),
    request.getHeader("User-Agent")
);

// Email removed - user can add new email later
```

## Database Schema

### email_change_audit_logs Table

```sql
CREATE TABLE email_change_audit_logs (
    audit_id UUID PRIMARY KEY,
    user_id UUID NOT NULL,
    old_email VARCHAR(255),
    new_email VARCHAR(255),
    change_type VARCHAR(20) NOT NULL,
    change_reason VARCHAR(500),
    ip_address VARCHAR(45),
    user_agent VARCHAR(500),
    mobile_otp_verified BOOLEAN NOT NULL DEFAULT FALSE,
    email_otp_verified BOOLEAN NOT NULL DEFAULT FALSE,
    changed_at TIMESTAMP NOT NULL,
    status VARCHAR(20) NOT NULL,
    notes VARCHAR(1000)
);

CREATE INDEX idx_email_audit_user ON email_change_audit_logs(user_id);
CREATE INDEX idx_email_audit_timestamp ON email_change_audit_logs(changed_at);
```

## Configuration

### Application Properties

```properties
# OTP Configuration
app.otp.length=6
app.otp.expiration-minutes=5
app.otp.max-attempts=3

# Email OTP Configuration
app.email.otp.length=6
app.email.otp.expiration-minutes=10
app.email.otp.max-attempts=3

# Rate Limiting
app.rate-limit.otp-requests-per-hour=5
```

## Testing

### Unit Tests

Test coverage includes:
- Mobile OTP verification for email updates
- Email OTP verification for new email
- Audit log creation and updates
- Data preservation during email changes
- Error handling for invalid OTPs
- Rate limiting enforcement

### Integration Tests

Test coverage includes:
- Complete email update workflow
- Email removal workflow
- Concurrent email change attempts
- Database transaction rollback on failures

## Backward Compatibility

Legacy methods are provided for backward compatibility:

- `updateCorporateEmail(UUID userId, String newEmail)` - @Deprecated
- `removeCorporateEmailLegacy(UUID userId)` - @Deprecated

These methods do not require mobile OTP verification and should be migrated to the new secure methods.

## Future Enhancements

1. Email change cooldown period (e.g., limit to 1 change per month)
2. Admin approval for suspicious email changes
3. Email notification to old email address when changed
4. SMS notification when email is changed
5. Automatic suspension if multiple failed attempts detected
