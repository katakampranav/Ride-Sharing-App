# Auth Module

## Overview
The Auth module handles user authentication, registration, and corporate email verification for the corporate ride-sharing application.

## Responsibilities
- Mobile number registration and OTP verification
- User login and session management
- Corporate email verification workflow
- JWT token generation and validation
- Session lifecycle management
- OTP generation and delivery (SMS and email)

## Package Structure
```
auth/
├── entity/          # JPA entities (UserAccount, EmailVerification)
├── repository/      # Spring Data JPA repositories
├── service/         # Business logic for authentication and verification
└── controller/      # REST API endpoints for auth operations
```

## Key Components
- **MobileAuthService**: Phone number registration and OTP verification
- **CorporateEmailVerificationService**: Email verification workflow
- **SessionManagementService**: JWT token and session management
- **OTPService**: OTP generation, storage, and verification

## Related Requirements
- Requirement 1.1: Mobile number registration
- Requirement 1.4: User account creation
- Requirement 2.1: Mobile OTP login
- Requirement 2.4: Session management
- Requirement 3.1: Corporate email verification
- Requirement 3.2: Email OTP verification
- Requirement 7.1-7.5: Corporate email updates
