# EmailVerificationService

## Overview

The `EmailVerificationService` handles corporate email verification using OTP (One-Time Password). It provides functionality for:
- Sending OTP to corporate email addresses
- Verifying email OTP
- Managing corporate email updates (for company changes)
- Enforcing one corporate email per account

## Key Features

### 1. OTP-Based Verification
- Generates 6-digit numeric OTP (configurable)
- OTP expires after 10 minutes (configurable)
- Maximum 3 verification attempts (configurable)
- OTPs are hashed using SHA-256 before storage

### 2. Email Validation
- Validates email format using regex pattern
- Checks for duplicate emails across accounts
- Enforces one corporate email per account

### 3. Corporate Email Management
- Add corporate email with OTP verification
- Update corporate email (for company changes)
- Remove corporate email
- Maintains user data during email changes

## Configuration

Add these properties to `application.yml`:

```yaml
app:
  email:
    otp:
      length: 6                    # OTP length (default: 6)
      expiration-minutes: 10       # OTP expiration time (default: 10 minutes)
      max-attempts: 3              # Maximum verification attempts (default: 3)
```

## Usage Examples

### 1. Send Email OTP

```java
@Autowired
private EmailVerificationService emailVerificationService;

public void addCorporateEmail(UUID userId, String corporateEmail) {
    try {
        VerificationResponse response = emailVerificationService.sendEmailOTP(userId, corporateEmail);
        
        // Response contains:
        // - otpSent: true
        // - expiresAt: timestamp when OTP expires
        // - maskedEmail: masked email for display
        
        System.out.println("OTP sent to: " + response.getMaskedEmail());
        System.out.println("Expires at: " + response.getExpiresAt());
        
    } catch (CorporateEmailException e) {
        // Handle errors:
        // - INVALID_EMAIL_FORMAT: Email format is invalid
        // - USER_NOT_FOUND: User does not exist
        // - EMAIL_ALREADY_EXISTS: Email already registered to another account
        // - ACTIVE_VERIFICATION_EXISTS: Active verification already in progress
        
        System.err.println("Error: " + e.getMessage());
        System.err.println("Error code: " + e.getErrorCode());
    }
}
```

### 2. Verify Email OTP

```java
public void verifyEmail(UUID userId, String otp) {
    try {
        VerificationResponse response = emailVerificationService.verifyEmailOTP(userId, otp);
        
        if (response.isVerified()) {
            System.out.println("Email verified successfully!");
            // User's corporateEmail field is now updated
            // User's emailVerified flag is set to true
            // Account status may be updated to ACTIVE
        }
        
    } catch (CorporateEmailException e) {
        // Handle errors:
        // - NO_ACTIVE_VERIFICATION: No active verification found or OTP expired
        // - ALREADY_VERIFIED: Email already verified
        // - OTP_EXPIRED: OTP has expired
        // - MAX_ATTEMPTS_EXCEEDED: Too many failed attempts
        // - INVALID_OTP: OTP is incorrect (includes remaining attempts)
        
        System.err.println("Error: " + e.getMessage());
    }
}
```

### 3. Resend Email OTP

```java
public void resendOTP(UUID userId) {
    try {
        VerificationResponse response = emailVerificationService.resendEmailOTP(userId);
        
        System.out.println("New OTP sent to: " + response.getMaskedEmail());
        
    } catch (CorporateEmailException e) {
        // Handle errors:
        // - NO_ACTIVE_VERIFICATION: No active verification to resend
        
        System.err.println("Error: " + e.getMessage());
    }
}
```

### 4. Update Corporate Email (Company Change)

```java
public void changeCompany(UUID userId, String newCorporateEmail) {
    // NOTE: Caller must verify mobile OTP before calling this method
    
    try {
        VerificationResponse response = emailVerificationService.updateCorporateEmail(userId, newCorporateEmail);
        
        // Old email is removed
        // New OTP is sent to new email
        // User must verify new email with OTP
        
        System.out.println("OTP sent to new email: " + response.getMaskedEmail());
        
    } catch (CorporateEmailException e) {
        // Handle errors:
        // - INVALID_EMAIL_FORMAT: New email format is invalid
        // - USER_NOT_FOUND: User does not exist
        // - EMAIL_ALREADY_EXISTS: New email already registered to another account
        
        System.err.println("Error: " + e.getMessage());
    }
}
```

### 5. Remove Corporate Email

```java
public void removeEmail(UUID userId) {
    // NOTE: Caller must verify mobile OTP before calling this method
    
    try {
        emailVerificationService.removeCorporateEmail(userId);
        
        System.out.println("Corporate email removed successfully");
        
    } catch (CorporateEmailException e) {
        // Handle errors:
        // - USER_NOT_FOUND: User does not exist
        
        System.err.println("Error: " + e.getMessage());
    }
}
```

## Database Schema

The service uses the `email_verifications` table:

```sql
CREATE TABLE email_verifications (
    verification_id UUID PRIMARY KEY,
    user_id UUID NOT NULL,
    corporate_email VARCHAR(255) NOT NULL,
    otp_hash VARCHAR(255) NOT NULL,
    attempts INTEGER DEFAULT 0,
    verified BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP NOT NULL,
    expires_at TIMESTAMP NOT NULL
);

CREATE INDEX idx_email_verifications_user ON email_verifications(user_id);
CREATE INDEX idx_email_verifications_expires ON email_verifications(expires_at);
```

## Security Considerations

1. **OTP Hashing**: OTPs are hashed using SHA-256 before storage. Plain text OTPs are never stored.

2. **Rate Limiting**: The service checks for active verifications to prevent spam.

3. **Attempt Limiting**: Maximum 3 verification attempts per OTP.

4. **Expiration**: OTPs expire after 10 minutes.

5. **Email Masking**: Emails are masked in logs for privacy.

6. **One Email Per Account**: Enforces that each corporate email can only be associated with one account.

## Error Handling

All errors are thrown as `CorporateEmailException` with specific error codes:

| Error Code | Description |
|------------|-------------|
| `INVALID_EMAIL_FORMAT` | Email format is invalid |
| `USER_NOT_FOUND` | User does not exist |
| `EMAIL_ALREADY_EXISTS` | Email already registered to another account |
| `ACTIVE_VERIFICATION_EXISTS` | Active verification already in progress |
| `NO_ACTIVE_VERIFICATION` | No active verification found or OTP expired |
| `ALREADY_VERIFIED` | Email already verified |
| `OTP_EXPIRED` | OTP has expired |
| `MAX_ATTEMPTS_EXCEEDED` | Too many failed verification attempts |
| `INVALID_OTP` | OTP is incorrect |

## Integration with UserAccount

When email verification succeeds, the service automatically:
1. Updates `UserAccount.corporateEmail` field
2. Sets `UserAccount.emailVerified` to `true`
3. Updates `UserAccount.accountStatus` to `ACTIVE` (if phone is also verified)

## Cleanup

Expired verification records are automatically cleaned up by the `EmailVerificationCleanupJob` scheduled task.

## TODO

- Integrate with AWS SES for actual email delivery
- Add email templates for OTP emails
- Implement rate limiting per email address
- Add audit logging for email changes

## Related Services

- `OTPService`: Handles mobile OTP verification
- `MobileAuthService`: Handles mobile authentication
- `UserAccountRepository`: Manages user accounts
- `EmailVerificationRepository`: Manages email verification records
