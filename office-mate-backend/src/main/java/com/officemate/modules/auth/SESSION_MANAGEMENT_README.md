# Session Management Service

## Overview

The SessionManagementService handles JWT token generation and validation for user authentication. It stores verification status (mobile and email) in JWT tokens as custom claims, enabling permission-based access control throughout the application.

## Features

### JWT Token Generation
- **Access Tokens**: Short-lived tokens (1 hour default) for API authentication
- **Refresh Tokens**: Long-lived tokens (24 hours default) for obtaining new access tokens
- **Verification Status**: Includes `mobileVerified` and `emailVerified` flags in token claims
- **Account Status**: Includes current account status (ACTIVE, SUSPENDED, PENDING_EMAIL)

### Token Validation
- **Signature Verification**: Uses HMAC-SHA with configurable secret key
- **Expiration Checking**: Automatically validates token expiration
- **Claims Extraction**: Provides methods to extract user ID and verification status

## Configuration

JWT settings are configured in `application.yml`:

```yaml
app:
  security:
    jwt:
      secret: ${JWT_SECRET:changeme-this-is-a-development-secret-key-only}
      expiration: ${JWT_EXPIRATION:3600000}  # 1 hour in milliseconds
      refresh-expiration: ${JWT_REFRESH_EXPIRATION:86400000}  # 24 hours in milliseconds
```

## Usage

### Generating Tokens

```java
@Autowired
private SessionManagementService sessionManagementService;

// Generate access token
String accessToken = sessionManagementService.generateAccessToken(userAccount);

// Generate refresh token
String refreshToken = sessionManagementService.generateRefreshToken(userAccount);
```

### Validating Tokens

```java
// Validate and extract claims
Claims claims = sessionManagementService.validateToken(token);

// Extract user ID
String userId = sessionManagementService.extractUserId(token);

// Check verification status
boolean mobileVerified = sessionManagementService.isMobileVerified(token);
boolean emailVerified = sessionManagementService.isEmailVerified(token);
```

## Token Structure

### Access Token Claims
```json
{
  "userId": "uuid-string",
  "mobileVerified": true,
  "emailVerified": false,
  "accountStatus": "PENDING_EMAIL",
  "tokenType": "ACCESS",
  "sub": "uuid-string",
  "iat": 1234567890,
  "exp": 1234571490
}
```

### Refresh Token Claims
```json
{
  "userId": "uuid-string",
  "tokenType": "REFRESH",
  "sub": "uuid-string",
  "iat": 1234567890,
  "exp": 1234654290
}
```

## Integration with MobileAuthService

The SessionManagementService is integrated into the authentication flow:

1. **Registration Flow**: After OTP verification, generates tokens with `mobileVerified=true`
2. **Login Flow**: Generates tokens with current verification status from database
3. **Token Refresh**: Will be implemented in future tasks

## Security Considerations

- **Secret Key**: Must be at least 256 bits (32 characters) for HMAC-SHA256
- **Token Storage**: Tokens should be stored securely on client side (HttpOnly cookies recommended)
- **Token Expiration**: Short-lived access tokens minimize security risk
- **Signature Verification**: All tokens are cryptographically signed and verified

## Future Enhancements

- Token revocation and blacklisting (using Redis)
- Multi-device session management
- Session metadata storage in PostgreSQL
- Token refresh endpoint implementation
- Rate limiting for token generation

## Testing

Comprehensive unit tests are available in `SessionManagementServiceTest.java`:
- Token generation tests
- Token validation tests
- Verification status extraction tests
- Expiration time calculation tests

Run tests with:
```bash
./gradlew test --tests "com.officemate.modules.auth.service.SessionManagementServiceTest"
```

## Dependencies

- `io.jsonwebtoken:jjwt-api:0.12.3` - JWT API
- `io.jsonwebtoken:jjwt-impl:0.12.3` - JWT implementation
- `io.jsonwebtoken:jjwt-jackson:0.12.3` - JSON processing for JWT

## Related Components

- **MobileAuthService**: Uses SessionManagementService for token generation
- **JwtConfig**: Configuration properties for JWT settings
- **UserAccount**: Entity containing verification status stored in tokens
