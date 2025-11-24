# UserProfileService Implementation

## Overview

The `UserProfileService` is a Spring service that manages user profile operations in the corporate ride-sharing application. It enforces verification requirements and provides comprehensive profile management capabilities.

## Key Features

### 1. Verification-Based Access Control
- **Requirement**: Both mobile and email verification must be completed before profile creation
- **Method**: `canAccessRideFeatures(UUID userId)` - Checks if user has full verification
- **Exception**: Throws `ProfileAccessException` if verification is incomplete

### 2. Profile Creation
- **Method**: `createBasicProfile(UUID userId, String firstName, String lastName, LocalDate dateOfBirth, String gender)`
- **Validation**: 
  - Checks mobile and email verification status
  - Prevents duplicate profile creation
  - Validates user account exists
- **Returns**: `ProfileResponse` with complete profile information

### 3. Profile Retrieval
- **Method**: `getProfile(UUID userId)`
- **Features**:
  - Eagerly loads UserAccount relationship
  - Includes driver/rider capability flags
  - Returns verification status
- **Returns**: `ProfileResponse` with all profile data

### 4. Profile Updates
- **Method**: `updateProfile(UUID userId, String firstName, String lastName, LocalDate dateOfBirth, String gender, String profileImageUrl)`
- **Features**:
  - Partial updates supported (null values ignored)
  - Maintains data integrity
  - Logs all update operations
- **Note**: Sensitive changes requiring re-authentication should be handled at controller layer

### 5. Profile Deletion
- **Method**: `deleteProfile(UUID userId)`
- **Features**:
  - Transactional operation
  - Maintains referential integrity
  - Logs deletion operations

## Verification Requirements

### Mobile and Email Verification
According to requirements 4.1, 5.1, and 8.1, users must complete both verifications before:
- Creating a user profile
- Adding driver capabilities
- Adding rider capabilities
- Accessing any ride-sharing features

### Verification Check Flow
```java
UserAccount userAccount = userAccountRepository.findById(userId);
boolean canAccess = userAccount.getPhoneVerified() && userAccount.getEmailVerified();
```

## ProfileResponse Structure

The service returns `ProfileResponse` DTOs containing:
- **Basic Info**: userId, firstName, lastName, phoneNumber, corporateEmail
- **Profile Details**: profileImageUrl, dateOfBirth, gender
- **Verification Status**: mobileVerified, emailVerified, accountStatus
- **Capabilities**: isDriver, isRider, canAccessRideFeatures
- **Timestamps**: createdAt, updatedAt

## MongoDB Compatibility

During the migration period, the service:
- Uses PostgreSQL for new UserProfile entities
- Maintains compatibility with existing MongoDB User model
- Provides consistent API regardless of underlying storage
- Supports gradual migration strategy

## Error Handling

### ProfileAccessException
Thrown when users attempt operations without proper verification:
```java
throw new ProfileAccessException(
    "Both mobile and email verification required before creating profile",
    userAccount.getPhoneVerified(),
    userAccount.getEmailVerified()
);
```

### EntityNotFoundException
Thrown when:
- User account not found
- Profile not found
- Invalid user ID provided

### IllegalStateException
Thrown when:
- Attempting to create duplicate profile
- Invalid state transitions

## Transaction Management

All write operations are annotated with `@Transactional`:
- `createBasicProfile()` - Creates profile atomically
- `updateProfile()` - Updates profile with rollback support
- `deleteProfile()` - Deletes profile with referential integrity

## Logging

The service uses SLF4J logging at multiple levels:
- **INFO**: Profile creation, updates, deletions
- **WARN**: Access denied, duplicate profiles
- **DEBUG**: Profile retrieval, verification checks

## Usage Examples

### Creating a Profile
```java
ProfileResponse profile = userProfileService.createBasicProfile(
    userId,
    "John",
    "Doe",
    LocalDate.of(1990, 1, 1),
    "MALE"
);
```

### Checking Access
```java
boolean canAccess = userProfileService.canAccessRideFeatures(userId);
if (!canAccess) {
    // Redirect to verification flow
}
```

### Updating a Profile
```java
ProfileResponse updated = userProfileService.updateProfile(
    userId,
    "Jane",      // firstName
    "Smith",     // lastName
    null,        // dateOfBirth (no change)
    "FEMALE",    // gender
    "https://example.com/profile.jpg" // profileImageUrl
);
```

### Retrieving a Profile
```java
ProfileResponse profile = userProfileService.getProfile(userId);
System.out.println("User: " + profile.getFirstName() + " " + profile.getLastName());
System.out.println("Can access rides: " + profile.isCanAccessRideFeatures());
```

## Dependencies

- **UserProfileRepository**: JPA repository for UserProfile entities
- **UserAccountRepository**: JPA repository for UserAccount entities
- **DriverProfileRepository**: Checks driver capability existence
- **RiderProfileRepository**: Checks rider capability existence

## Related Components

- **UserProfile Entity**: PostgreSQL entity for profile data
- **UserAccount Entity**: PostgreSQL entity for authentication data
- **ProfileResponse DTO**: Response data transfer object
- **ProfileAccessException**: Custom exception for access control

## Requirements Mapping

- **Requirement 4.1**: Profile creation with verification requirements
- **Requirement 5.1**: Rider profile support with verification
- **Requirement 8.1**: Profile management and updates

## Future Enhancements

1. Profile image upload and storage integration
2. Profile validation rules engine
3. Profile change audit logging
4. Profile search and filtering capabilities
5. Profile privacy settings
6. Profile completion percentage tracking
