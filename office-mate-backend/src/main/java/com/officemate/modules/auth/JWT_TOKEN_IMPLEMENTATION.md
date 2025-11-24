# JWT Token Generation and Validation Implementation

## Overview

This implementation provides comprehensive JWT token generation and validation with RSA signing, permission tracking, token refresh, and Redis-based token revocation. It supports multi-device session management and verification-based access control.

## Components

### 1. SessionManagementService

**Location**: `com.officemate.modules.auth.service.SessionManagementService`

**Key Features**:
- JWT token generation with RSA or HMAC signing
- Token validation with revocation checking
- Session lifecycle management
- Permission tracking based on verification status
- Token refresh functionality
- Multi-device session support

**Main Methods**:
```java
// Create new session with tokens
SessionTokens createSession(UserAccount userAccount, DeviceInfo deviceInfo)

// Generate access token
String generateAccessToken(UserAccount userAccount, String sessionId)

// Generate refresh token
String generateRefreshToken(UserAccount userAccount, String sessionId)

// Validate token
TokenValidation validateToken(String token)

// Refresh session
SessionTokens refreshSession(String refreshToken, UserAccount userAccount)

// Revoke session
void revokeSession(String sessionId)

// Revoke all user sessions
void revokeAllSessions(String userId)
```

### 2. JWT Configuration

**Location**: `com.officemate.config.JwtConfig`

**Configuration Properties** (application.yml):
```yaml
app:
  security:
    jwt:
      secret: ${JWT_SECRET:changeme-this-is-a-development-secret-key-only}
      expiration: ${JWT_EXPIRATION:3600000}  # 1 hour in milliseconds
      refresh-expiration: ${JWT_REFRESH_EXPIRATION:86400000}  # 24 hours
      algorithm: RS256  # RS256 for RSA, HS256 for HMAC
      issuer: officemate
      rsa-private-key: ${JWT_RSA_PRIVATE_KEY:}  # Base64 encoded
      rsa-public-key: ${JWT_RSA_PUBLIC_KEY:}    # Base64 encoded
```

**Features**:
- Supports both RSA (RS256) and HMAC (HS256) signing
- Auto-generates RSA key pair if not configured
- Configurable token expiration times
- Custom issuer claim

### 3. Redis Entities

#### UserSession
**Location**: `com.officemate.modules.auth.entity.UserSession`

Stores active session information in Redis with TTL:
- Session ID
- User ID
- Device information
- Permissions list
- Verification status
- Refresh token
- Timestamps

#### RevokedToken
**Location**: `com.officemate.modules.auth.entity.RevokedToken`

Stores revoked tokens in Redis for blacklisting:
- Token ID (jti claim)
- User ID
- Revocation timestamp
- Revocation reason
- TTL matching token expiration

### 4. Spring Security Integration

#### JwtAuthenticationFilter
**Location**: `com.officemate.config.security.JwtAuthenticationFilter`

Spring Security filter that:
- Extracts JWT from Authorization header
- Validates token
- Sets authentication context
- Adds user info to request attributes

#### SecurityConfig
**Location**: `com.officemate.config.SecurityConfig`

Configures:
- Stateless session management
- Public endpoints (no authentication required)
- Protected endpoints (authentication required)
- JWT filter integration

### 5. Verification Annotations

Custom annotations for permission-based access control:

#### @RequireMobileVerification
Requires mobile number to be verified

#### @RequireEmailVerification
Requires corporate email to be verified

#### @RequireFullVerification
Requires both mobile and email verification

**Usage Example**:
```java
@RequireFullVerification(message = "Profile setup requires full verification")
public ProfileResponse createProfile(String userId, ProfileData data) {
    // Method implementation
}
```

#### VerificationAspect
**Location**: `com.officemate.config.security.VerificationAspect`

AOP aspect that enforces verification requirements by checking request attributes set by JWT filter.

## Token Structure

### Access Token Claims
```json
{
  "sub": "user-uuid",
  "iss": "officemate",
  "iat": 1234567890,
  "exp": 1234571490,
  "jti": "token-uuid",
  "userId": "user-uuid",
  "sessionId": "session-uuid",
  "mobileVerified": true,
  "emailVerified": true,
  "accountStatus": "ACTIVE",
  "permissions": [
    "MOBILE_VERIFIED",
    "EMAIL_VERIFIED",
    "FULLY_VERIFIED",
    "ACCESS_RIDE_FEATURES",
    "ACCOUNT_ACTIVE"
  ],
  "tokenType": "ACCESS"
}
```

### Refresh Token Claims
```json
{
  "sub": "user-uuid",
  "iss": "officemate",
  "iat": 1234567890,
  "exp": 1234654290,
  "jti": "token-uuid",
  "userId": "user-uuid",
  "sessionId": "session-uuid",
  "tokenType": "REFRESH"
}
```

## Permission System

Permissions are automatically assigned based on user verification status:

| Permission | Condition |
|-----------|-----------|
| MOBILE_VERIFIED | Phone number verified |
| EMAIL_VERIFIED | Corporate email verified |
| FULLY_VERIFIED | Both phone and email verified |
| ACCESS_RIDE_FEATURES | Fully verified |
| ACCOUNT_ACTIVE | Account status is ACTIVE |

## Token Lifecycle

### 1. Session Creation
```
User Login → Create Session → Generate Tokens → Store in Redis → Return to Client
```

### 2. Token Validation
```
Request → Extract Token → Parse Claims → Check Revocation → Verify Signature → Set Auth Context
```

### 3. Token Refresh
```
Refresh Token → Validate → Check Session → Generate New Access Token → Update Session
```

### 4. Token Revocation
```
Revoke Request → Add to Blacklist → Delete Session → Prevent Future Use
```

## Security Features

### 1. RSA Signing
- 2048-bit RSA key pairs
- Public key for verification
- Private key for signing
- Secure key storage

### 2. Token Revocation
- Redis-based blacklist
- TTL matches token expiration
- Automatic cleanup
- Session-level revocation

### 3. Multi-Device Support
- Track device information
- Multiple active sessions per user
- Device-specific revocation
- Session management

### 4. Permission Tracking
- Verification-based permissions
- Dynamic permission updates
- Token refresh updates permissions
- Fine-grained access control

## Usage Examples

### Creating a Session
```java
@Autowired
private SessionManagementService sessionService;

public AuthResponse login(UserAccount userAccount, DeviceInfo deviceInfo) {
    SessionTokens tokens = sessionService.createSession(userAccount, deviceInfo);
    
    return AuthResponse.builder()
            .accessToken(tokens.getAccessToken())
            .refreshToken(tokens.getRefreshToken())
            .userId(tokens.getUserId())
            .expiresAt(tokens.getExpiresAt())
            .build();
}
```

### Validating a Token
```java
public void validateRequest(String token) {
    TokenValidation validation = sessionService.validateToken(token);
    
    if (!validation.isValid()) {
        throw new UnauthorizedException(validation.getErrorMessage());
    }
    
    // Use validation data
    String userId = validation.getUserId();
    List<String> permissions = validation.getPermissions();
}
```

### Refreshing a Token
```java
public SessionTokens refresh(String refreshToken, UserAccount userAccount) {
    return sessionService.refreshSession(refreshToken, userAccount);
}
```

### Revoking Sessions
```java
// Revoke specific session
sessionService.revokeSession(sessionId);

// Revoke all user sessions (e.g., on password change)
sessionService.revokeAllSessions(userId);
```

### Using Verification Annotations
```java
@RestController
@RequestMapping("/api/profile")
public class ProfileController {
    
    @PostMapping
    @RequireFullVerification(message = "Complete verification to create profile")
    public ProfileResponse createProfile(@RequestBody ProfileRequest request) {
        // Only accessible if both mobile and email are verified
        return profileService.createProfile(request);
    }
    
    @GetMapping
    @RequireMobileVerification
    public ProfileResponse getProfile() {
        // Only accessible if mobile is verified
        return profileService.getProfile();
    }
}
```

## Configuration

### Development (HMAC)
```yaml
app:
  security:
    jwt:
      secret: dev-secret-key-change-in-production
      algorithm: HS256
      expiration: 3600000
      refresh-expiration: 86400000
```

### Production (RSA)
```yaml
app:
  security:
    jwt:
      algorithm: RS256
      expiration: 3600000
      refresh-expiration: 86400000
      rsa-private-key: ${JWT_RSA_PRIVATE_KEY}
      rsa-public-key: ${JWT_RSA_PUBLIC_KEY}
      issuer: officemate
```

## Testing

The implementation includes comprehensive unit tests covering:
- Token generation and validation
- Permission tracking
- Token refresh
- Session management
- Token revocation
- Multi-device support

## Requirements Satisfied

✅ **Requirement 2.4**: JWT token generation with verification status tracking
✅ **Requirement 8.3**: Session management with multi-device support
✅ RSA key signing for enhanced security
✅ Permission-based access control
✅ Token refresh functionality with security checks
✅ Redis-based token revocation and blacklisting
✅ Spring Security filter integration
✅ Verification annotations for method-level security

## Notes

- RSA keys are auto-generated if not configured (development only)
- Store RSA keys securely in production (environment variables or secrets manager)
- Token revocation uses Redis TTL for automatic cleanup
- Sessions are stored in Redis with automatic expiration
- Permissions are dynamically updated on token refresh
- Multi-device sessions allow users to be logged in on multiple devices simultaneously
