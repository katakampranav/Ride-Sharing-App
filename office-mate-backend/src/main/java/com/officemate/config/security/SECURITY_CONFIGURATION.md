# Spring Security Configuration

## Overview

This document describes the comprehensive Spring Security configuration implemented for the OfficeMate ride-sharing application. The security setup includes JWT-based authentication, CORS configuration, rate limiting, and permission-based access control.

## Components

### 1. SecurityConfig

Main Spring Security configuration class that sets up the security filter chain.

**Features:**
- JWT-based stateless authentication
- CORS configuration for cross-origin requests
- Rate limiting using Redis
- Method-level security with verification requirements
- Permission-based access control
- Custom exception handlers

**Public Endpoints (No Authentication Required):**
- `/auth/register` - User registration
- `/auth/login` - User login
- `/auth/verify-mobile-otp` - Mobile OTP verification
- `/auth/resend-otp` - Resend OTP
- `/health` - Health check
- `/actuator/health` - Actuator health endpoint
- `/actuator/info` - Actuator info endpoint

**Protected Endpoints (Authentication Required):**
- `/auth/add-corporate-email` - Add corporate email
- `/auth/verify-email-otp` - Verify email OTP
- `/auth/resend-email-otp` - Resend email OTP
- `/auth/update-corporate-email` - Update corporate email
- `/users/**` - All user-related endpoints

### 2. JwtAuthenticationFilter

Filter that intercepts requests and validates JWT tokens.

**Responsibilities:**
- Extract JWT token from Authorization header
- Validate token using SessionManagementService
- Set authentication context in Spring Security
- Add user information to request attributes

**Request Attributes Set:**
- `userId` - User ID from token
- `sessionId` - Session ID from token
- `mobileVerified` - Mobile verification status
- `emailVerified` - Email verification status

### 3. RateLimitingFilter

Filter that enforces rate limiting on API requests using Redis.

**Rate Limits:**
- OTP requests: 5 per hour per identifier
- Login attempts: 10 per hour per identifier
- API requests: 60 per minute per user

**Configuration Properties:**
```yaml
app:
  rate-limit:
    enabled: true
    otp-requests-per-hour: 5
    login-attempts-per-hour: 10
    max-requests-per-minute: 60
```

### 4. RateLimitingService

Service that implements rate limiting logic using Redis counters.

**Methods:**
- `isAllowed(key, maxRequests, duration)` - Check if request is allowed
- `isOtpRequestAllowed(phoneNumber, maxRequests)` - Check OTP request limit
- `isLoginAttemptAllowed(identifier, maxAttempts)` - Check login attempt limit
- `isApiRequestAllowed(userId, maxRequests)` - Check API request limit
- `resetRateLimit(key)` - Reset rate limit for a key
- `getRemainingRequests(key, maxRequests)` - Get remaining requests
- `getTimeUntilReset(key)` - Get time until rate limit resets

### 5. VerificationAspect

AOP aspect that enforces verification requirements on methods.

**Annotations:**
- `@RequireMobileVerification` - Requires mobile verification
- `@RequireEmailVerification` - Requires email verification
- `@RequireFullVerification` - Requires both mobile and email verification

**Usage Example:**
```java
@RequireFullVerification(message = "Profile creation requires full verification")
public ProfileResponse createProfile(String userId, ProfileData data) {
    // Method implementation
}
```

### 6. CorsConfig

CORS (Cross-Origin Resource Sharing) configuration.

**Configuration Properties:**
```yaml
app:
  cors:
    allowed-origins: http://localhost:3000,http://localhost:8080
    allowed-methods: GET,POST,PUT,DELETE,OPTIONS
    allowed-headers: *
    exposed-headers: Authorization,Content-Type
    allow-credentials: true
    max-age: 3600
```

### 7. CustomAuthenticationEntryPoint

Handles authentication failures and returns JSON error responses.

**Response Format:**
```json
{
  "error": "Unauthorized",
  "message": "Authentication required. Please provide a valid access token.",
  "status": 401,
  "timestamp": "2024-01-15T10:30:00",
  "path": "/users/profile"
}
```

### 8. CustomAccessDeniedHandler

Handles authorization failures and returns JSON error responses.

**Response Format:**
```json
{
  "error": "Forbidden",
  "message": "You do not have permission to access this resource.",
  "status": 403,
  "timestamp": "2024-01-15T10:30:00",
  "path": "/admin/users"
}
```

## Security Headers

The following security headers are configured:

- **Content-Type-Options**: Prevents MIME type sniffing
- **XSS-Protection**: Enables XSS protection
- **Cache-Control**: Controls caching behavior
- **HSTS**: HTTP Strict Transport Security (1 year, includeSubDomains)
- **Frame-Options**: Prevents clickjacking (DENY)

## Permission-Based Access Control

The system uses a permission-based access control model with the following permissions:

- `MOBILE_VERIFIED` - User has verified mobile number
- `EMAIL_VERIFIED` - User has verified corporate email
- `PROFILE_COMPLETE` - User has completed profile setup
- `DRIVER_APPROVED` - User is approved as a driver
- `RIDER_APPROVED` - User is approved as a rider

Permissions are stored in JWT tokens and checked by the VerificationAspect.

## Rate Limiting Strategy

Rate limiting is implemented using Redis counters with TTL (Time To Live):

1. **OTP Requests**: Limited to prevent SMS/email abuse
2. **Login Attempts**: Limited to prevent brute force attacks
3. **API Requests**: Limited to prevent API abuse

Rate limits are enforced at the filter level before authentication, ensuring that even unauthenticated requests are rate-limited.

## Configuration

### Environment Variables

```bash
# JWT Configuration
JWT_SECRET=your-secret-key-here
JWT_EXPIRATION=3600000
JWT_REFRESH_EXPIRATION=86400000

# Rate Limiting
RATE_LIMIT_ENABLED=true
RATE_LIMIT_OTP=5
RATE_LIMIT_LOGIN=10
RATE_LIMIT_API=60

# CORS Configuration
CORS_ALLOWED_ORIGINS=http://localhost:3000,https://app.officemate.com
CORS_ALLOWED_METHODS=GET,POST,PUT,DELETE,OPTIONS
CORS_ALLOW_CREDENTIALS=true
```

### Application Properties

See `application.yml` for complete configuration options.

## Testing

### Testing Authentication

```bash
# Register user
curl -X POST http://localhost:8080/auth/register \
  -H "Content-Type: application/json" \
  -d '{"phoneNumber": "+1234567890"}'

# Login with OTP
curl -X POST http://localhost:8080/auth/login \
  -H "Content-Type: application/json" \
  -d '{"phoneNumber": "+1234567890", "otp": "123456"}'

# Access protected endpoint
curl -X GET http://localhost:8080/users/profile \
  -H "Authorization: Bearer <access-token>"
```

### Testing Rate Limiting

```bash
# Test OTP rate limit (should fail after 5 requests)
for i in {1..6}; do
  curl -X POST http://localhost:8080/auth/resend-otp \
    -H "Content-Type: application/json" \
    -d '{"phoneNumber": "+1234567890"}'
done
```

## Security Best Practices

1. **Never log sensitive information** (passwords, OTPs, tokens)
2. **Always use HTTPS in production**
3. **Rotate JWT signing keys regularly**
4. **Monitor rate limit violations**
5. **Implement account lockout after multiple failed attempts**
6. **Use strong password policies** (if implementing password auth)
7. **Validate all input data**
8. **Keep dependencies up to date**

## Troubleshooting

### Common Issues

1. **CORS errors**: Check `allowed-origins` configuration
2. **Rate limit exceeded**: Check Redis connection and rate limit settings
3. **Authentication failures**: Verify JWT token is valid and not expired
4. **Permission denied**: Check user verification status and permissions

### Debugging

Enable debug logging for security:

```yaml
logging:
  level:
    org.springframework.security: DEBUG
    com.officemate.config.security: DEBUG
```

## Future Enhancements

1. **OAuth2 Integration**: Support for social login
2. **Two-Factor Authentication**: Additional security layer
3. **IP Whitelisting**: Restrict access by IP address
4. **Geolocation-based Access**: Location-based security
5. **Advanced Threat Detection**: ML-based anomaly detection
