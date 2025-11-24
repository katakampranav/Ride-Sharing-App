# Authentication REST Controller

## Overview
The `AuthController` provides REST endpoints for user authentication operations including registration, login, OTP verification, token refresh, and logout.

## Implemented Endpoints

### 1. POST /auth/register
Register a new user with mobile number.

**Request Body:**
```json
{
  "phoneNumber": "+919876543210"
}
```

**Response (201 Created):**
```json
{
  "userId": "uuid",
  "otpSent": true,
  "expiresAt": "2024-01-01T12:05:00",
  "maskedPhoneNumber": "****3210"
}
```

### 2. POST /auth/verify-mobile-otp
Verify mobile OTP during registration.

**Request Body:**
```json
{
  "phoneNumber": "+919876543210",
  "otp": "123456"
}
```

**Response (200 OK):**
```json
{
  "accessToken": "jwt-access-token",
  "refreshToken": "jwt-refresh-token",
  "userId": "uuid",
  "mobileVerified": true,
  "emailVerified": false,
  "profileComplete": false,
  "expiresAt": "2024-01-01T13:00:00"
}
```

### 3. POST /auth/login
Login with mobile number (sends OTP).

**Request Body:**
```json
{
  "phoneNumber": "+919876543210"
}
```

**Response (200 OK):**
```json
{
  "userId": "uuid",
  "otpSent": true,
  "expiresAt": "2024-01-01T12:05:00",
  "maskedPhoneNumber": "****3210"
}
```

### 4. POST /auth/verify-login-otp
Verify login OTP and get authentication tokens.

**Request Body:**
```json
{
  "phoneNumber": "+919876543210",
  "otp": "123456"
}
```

**Response (200 OK):**
```json
{
  "accessToken": "jwt-access-token",
  "refreshToken": "jwt-refresh-token",
  "userId": "uuid",
  "mobileVerified": true,
  "emailVerified": true,
  "profileComplete": true,
  "expiresAt": "2024-01-01T13:00:00"
}
```

### 5. POST /auth/refresh
Refresh access token using refresh token.

**Request Body:**
```json
{
  "refreshToken": "jwt-refresh-token"
}
```

**Response (200 OK):**
```json
{
  "accessToken": "new-jwt-access-token",
  "refreshToken": "jwt-refresh-token",
  "expiresAt": "2024-01-01T14:00:00",
  "sessionId": "session-uuid",
  "userId": "user-uuid"
}
```

**Error Response (401 Unauthorized):**
- Invalid or expired refresh token

### 6. POST /auth/logout
Logout and terminate current session.

**Headers:**
```
Authorization: Bearer <access-token>
```

**Response (200 OK):**
```json
{
  "message": "Logout successful"
}
```

### 7. GET /auth/health
Health check endpoint for authentication service.

**Response (200 OK):**
```json
{
  "status": "UP",
  "service": "authentication"
}
```

## Error Handling

All endpoints return appropriate HTTP status codes:
- **200 OK**: Successful operation
- **201 Created**: Resource created successfully (registration)
- **400 Bad Request**: Invalid input or validation errors
- **401 Unauthorized**: Authentication failed or invalid token
- **500 Internal Server Error**: Server-side errors

Error responses include descriptive messages from the GlobalExceptionHandler.

## Security

- All endpoints except `/auth/health` require proper request validation
- Phone numbers are validated using libphonenumber library
- OTPs are hashed and stored in Redis with 5-minute expiration
- JWT tokens are signed with RSA keys (if configured) or HMAC
- Session management tracks device information and supports multi-device sessions
- Logout revokes the current session and invalidates tokens

## Dependencies

- `MobileAuthService`: Handles phone registration and OTP verification
- `SessionManagementService`: Manages JWT tokens and session lifecycle
- `UserAccountRepository`: Accesses user account data from PostgreSQL

## Requirements Satisfied

This implementation satisfies the following requirements from the specification:
- **Requirement 1.1**: Mobile number registration with OTP verification
- **Requirement 1.4**: User account creation and management
- **Requirement 2.1**: Login with mobile number and OTP
- **Requirement 2.4**: JWT token generation and session management

## Next Steps

- Implement Spring Security configuration (Task 9.1)
- Add rate limiting for OTP requests
- Integrate AWS SNS for SMS delivery
- Add comprehensive integration tests
