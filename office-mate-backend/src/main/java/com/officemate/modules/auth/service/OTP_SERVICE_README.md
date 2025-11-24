# OTP Service - Redis Implementation

## Overview

The OTP Service has been migrated from MongoDB to Redis for improved performance and automatic TTL-based expiration. This service handles OTP generation, verification, hashing, and rate limiting for both mobile and email authentication.

## Key Features

### 1. Redis Storage with TTL
- OTPs are stored in Redis with automatic expiration (5 minutes by default)
- No manual cleanup required - Redis handles expiration automatically
- Configurable TTL via `app.otp.expiration-minutes` property

### 2. Secure OTP Hashing
- OTPs are hashed using SHA-256 before storage
- Plain text OTPs are never stored in the database
- Only hashed values are compared during verification

### 3. Rate Limiting
- Prevents abuse by limiting OTP requests per hour
- Uses Redis counters with automatic expiration
- Configurable via `app.rate-limit.otp-requests-per-hour` property
- Default: 5 requests per hour per identifier (phone/email)

### 4. Attempt Tracking
- Tracks failed verification attempts
- Maximum attempts configurable via `app.otp.max-attempts` property
- Default: 3 attempts before OTP is invalidated

### 5. Dual Support
- Supports both mobile (SMS) and email OTP workflows
- Separate key prefixes: `phone:` and `email:`
- Same verification logic for both types

## Configuration

Add these properties to `application.yml`:

```yaml
app:
  otp:
    length: 6                    # OTP length (default: 6 digits)
    expiration-minutes: 5        # OTP expiration time (default: 5 minutes)
    max-attempts: 3              # Maximum verification attempts (default: 3)
    
  rate-limit:
    otp-requests-per-hour: 5     # Maximum OTP requests per hour (default: 5)
```

## Usage Examples

### Generate Mobile OTP

```java
@Autowired
private OTPService otpService;

// Generate OTP for mobile number
String otp = otpService.generateMobileOTP("+1234567890");

// Send OTP via SMS (AWS SNS integration - to be implemented)
// smsService.sendOTP(phoneNumber, otp);
```

### Verify Mobile OTP

```java
// Verify OTP
boolean isValid = otpService.verifyMobileOTP("+1234567890", "123456");

if (isValid) {
    // OTP verified successfully
    // Mark user as verified
} else {
    // Invalid OTP
    int remainingAttempts = otpService.getRemainingAttempts("+1234567890");
    // Show error with remaining attempts
}
```

### Generate Email OTP

```java
// Generate OTP for email
String otp = otpService.generateEmailOTP("user@company.com");

// Send OTP via email (AWS SES integration - to be implemented)
// emailService.sendOTP(email, otp);
```

### Verify Email OTP

```java
// Verify email OTP
boolean isValid = otpService.verifyEmailOTP("user@company.com", "123456");

if (isValid) {
    // Email verified successfully
    // Update user account
}
```

### Check Valid OTP Exists

```java
// Check if user has a valid OTP
boolean hasValidOTP = otpService.hasValidOTP("+1234567890");

if (hasValidOTP) {
    // User can verify existing OTP
} else {
    // Need to generate new OTP
}
```

### Delete OTP After Verification

```java
// Delete OTP after successful verification (optional - auto-expires anyway)
otpService.deleteMobileOTP("+1234567890");
```

## Redis Data Structure

### OTP Record

```
Key: "otp:phone:+1234567890" or "otp:email:user@company.com"
Value: {
    "key": "phone:+1234567890",
    "otpHash": "hashed_otp_value",
    "attempts": 0,
    "createdAt": "2024-01-15T10:30:00",
    "expiresAt": "2024-01-15T10:35:00",
    "verified": false,
    "type": "MOBILE",
    "timeToLive": 300
}
TTL: 300 seconds (5 minutes)
```

### Rate Limit Counter

```
Key: "rate_limit:otp:+1234567890"
Value: "3"
TTL: 3600 seconds (1 hour)
```

## Error Handling

### Rate Limit Exceeded
```java
try {
    String otp = otpService.generateMobileOTP(phoneNumber);
} catch (IllegalStateException e) {
    // "Too many OTP requests. Please try again later."
}
```

### OTP Not Found or Expired
```java
try {
    boolean isValid = otpService.verifyMobileOTP(phoneNumber, otp);
} catch (IllegalArgumentException e) {
    // "OTP not found or expired"
}
```

### Maximum Attempts Exceeded
```java
try {
    boolean isValid = otpService.verifyMobileOTP(phoneNumber, otp);
} catch (IllegalArgumentException e) {
    // "Maximum verification attempts exceeded"
}
```

### OTP Already Used
```java
try {
    boolean isValid = otpService.verifyMobileOTP(phoneNumber, otp);
} catch (IllegalArgumentException e) {
    // "OTP already used"
}
```

## Security Considerations

1. **OTP Hashing**: All OTPs are hashed using SHA-256 before storage
2. **Rate Limiting**: Prevents brute force attacks with configurable limits
3. **Attempt Tracking**: Limits verification attempts to prevent guessing
4. **Automatic Expiration**: OTPs expire after 5 minutes (configurable)
5. **One-Time Use**: OTPs are marked as verified and cannot be reused
6. **Secure Random**: Uses `SecureRandom` for OTP generation

## Integration with MobileAuthService

The OTP Service is integrated with `MobileAuthService` for phone verification:

```java
@Service
public class MobileAuthService {
    
    private final OTPService otpService;
    
    public RegistrationResponse registerUser(String phoneNumber) {
        // ... validation logic ...
        
        // Generate and store OTP in Redis
        String otp = otpService.generateMobileOTP(phoneNumber);
        
        // TODO: Send OTP via AWS SNS
        
        return response;
    }
    
    public AuthResponse verifyOTP(String phoneNumber, String otp) {
        // Verify OTP from Redis
        boolean isValid = otpService.verifyMobileOTP(phoneNumber, otp);
        
        if (!isValid) {
            int remainingAttempts = otpService.getRemainingAttempts(phoneNumber);
            throw new IllegalArgumentException("Invalid OTP. Remaining attempts: " + remainingAttempts);
        }
        
        // Mark phone as verified
        // Delete OTP after successful verification
        otpService.deleteMobileOTP(phoneNumber);
        
        return response;
    }
}
```

## Testing

Unit tests are available in `OTPServiceTest.java` covering:
- OTP generation for mobile and email
- OTP verification (success and failure cases)
- Rate limiting
- Expiration handling
- Attempt tracking
- Edge cases (already verified, not found, etc.)

Run tests:
```bash
./gradlew test --tests "com.officemate.modules.auth.service.OTPServiceTest"
```

## Migration Notes

### Changes from MongoDB Implementation

1. **Storage**: Migrated from MongoDB to Redis
2. **Expiration**: Now uses Redis TTL instead of manual cleanup
3. **Performance**: Faster read/write operations with Redis
4. **Annotations**: Removed MongoDB annotations (`@Document`, `@Id`)
5. **Added**: Redis-specific annotations (`@RedisHash`, `@TimeToLive`)
6. **Repository**: Changed from `MongoRepository` to `CrudRepository`

### Breaking Changes

- OTP records are no longer persisted long-term (auto-expire after TTL)
- Repository interface changed from MongoDB to Redis
- Key format changed to include prefix (`phone:` or `email:`)

## Future Enhancements

1. **AWS SNS Integration**: Send OTPs via SMS
2. **AWS SES Integration**: Send OTPs via email
3. **OTP Templates**: Customizable OTP message templates
4. **Multi-language Support**: Localized OTP messages
5. **Analytics**: Track OTP success/failure rates
6. **Admin Dashboard**: Monitor OTP usage and rate limits
