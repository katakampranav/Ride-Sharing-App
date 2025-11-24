# Profile Management REST Controller Implementation

## Overview
This document describes the implementation of comprehensive profile management REST controllers for the Office Mate ride-sharing application.

## Implementation Summary

### Created Files

1. **ProfileController.java**
   - Location: `src/main/java/com/officemate/modules/profile/controller/ProfileController.java`
   - Main REST controller for profile management operations

2. **ProfileUpdateRequest.java**
   - Location: `src/main/java/com/officemate/shared/dto/ProfileUpdateRequest.java`
   - DTO for basic profile update requests

3. **DriverProfileResponse.java**
   - Location: `src/main/java/com/officemate/shared/dto/DriverProfileResponse.java`
   - DTO for driver profile responses

## Implemented Endpoints

### 1. GET /users/{userId}/profile
**Purpose**: Retrieve user profile information

**Security**: 
- Requires `MOBILE_VERIFIED` authority
- User can only access their own profile (or admin)

**Response**: `ProfileResponse` containing:
- Basic profile information (name, phone, email)
- Verification status (mobile, email)
- Driver/rider capability flags
- Wallet information
- Account status

**Example**:
```bash
GET /users/123e4567-e89b-12d3-a456-426614174000/profile
Authorization: Bearer <token>
```

### 2. PUT /users/{userId}/profile
**Purpose**: Update basic user profile information

**Security**: 
- Requires `MOBILE_VERIFIED` authority
- User can only update their own profile (or admin)

**Request Body**: `ProfileUpdateRequest`
```json
{
  "firstName": "John",
  "lastName": "Doe",
  "dateOfBirth": "1990-01-15",
  "gender": "MALE",
  "profileImageUrl": "https://example.com/image.jpg"
}
```

**Response**: Updated `ProfileResponse`

**Notes**:
- All fields are optional
- Only provided fields will be updated

### 3. POST /users/{userId}/driver-profile
**Purpose**: Create driver profile with vehicle information

**Security**: 
- Requires `EMAIL_VERIFIED` authority (both mobile and email must be verified)
- User can only create their own driver profile (or admin)

**Request Body**: `DriverProfileRequest`
```json
{
  "licenseNumber": "DL-1234567890",
  "licenseExpiry": "2025-12-31",
  "maxDetourDistance": 500,
  "vehicle": {
    "vehicleType": "CAR",
    "make": "Toyota",
    "model": "Camry",
    "year": 2022,
    "licensePlate": "ABC-1234",
    "capacity": 4,
    "fuelType": "PETROL"
  },
  "routePreferences": {
    "startLatitude": 37.7749,
    "startLongitude": -122.4194,
    "startAddress": "123 Main St, San Francisco, CA",
    "endLatitude": 37.3382,
    "endLongitude": -121.8863,
    "endAddress": "456 Work Ave, San Jose, CA",
    "preferredStartTimes": ["08:00", "08:30"],
    "isActive": true
  }
}
```

**Response**: `DriverProfileResponse` (HTTP 201 Created)

**Validation**:
- License number must be unique
- License expiry must be in the future
- Vehicle capacity must match vehicle type (1-7 for cars, 1-2 for 2-wheelers)
- Max detour distance: 0-500 meters

### 4. PUT /users/{userId}/driver-profile
**Purpose**: Update driver profile information

**Security**: 
- Requires `EMAIL_VERIFIED` authority
- User can only update their own driver profile (or admin)

**Request Body**: `DriverProfileRequest` (same as POST)

**Response**: Updated `DriverProfileResponse`

**Notes**:
- Changing license information triggers re-verification
- All fields are optional for updates

### 5. POST /users/{userId}/rider-profile
**Purpose**: Create rider profile with preferences

**Security**: 
- Requires `EMAIL_VERIFIED` authority (both mobile and email must be verified)
- User can only create their own rider profile (or admin)

**Request Body**: `RiderProfileRequest`
```json
{
  "routePreferences": {
    "startLatitude": 37.7749,
    "startLongitude": -122.4194,
    "startAddress": "123 Main St, San Francisco, CA",
    "endLatitude": 37.3382,
    "endLongitude": -121.8863,
    "endAddress": "456 Work Ave, San Jose, CA",
    "preferredStartTimes": ["08:00", "08:30"],
    "isActive": true
  },
  "genderPreference": "NO_PREFERENCE",
  "vehicleTypePreferences": ["CAR", "MOTORCYCLE"],
  "favoriteDrivers": ["uuid1", "uuid2"]
}
```

**Response**: `RiderProfileResponse` (HTTP 201 Created)

**Gender Preference Options**:
- `FEMALE_ONLY`: Female riders prefer female drivers only
- `MALE_SINGLE_FEMALE`: Male riders prefer single female co-riders
- `MALE_ALL_FEMALE`: Male riders prefer all female co-riders
- `NO_PREFERENCE`: No gender preference

### 6. PUT /users/{userId}/rider-profile
**Purpose**: Update rider profile preferences

**Security**: 
- Requires `EMAIL_VERIFIED` authority
- User can only update their own rider profile (or admin)

**Request Body**: `RiderProfileRequest` (same as POST)

**Response**: Updated `RiderProfileResponse`

**Notes**:
- All fields are optional for updates
- Can update gender preferences, vehicle type preferences, and favorite drivers

## Security Implementation

### Authorization
All endpoints use Spring Security's `@PreAuthorize` annotation:

1. **Mobile Verified Endpoints** (GET, PUT profile):
   ```java
   @PreAuthorize("hasAuthority('MOBILE_VERIFIED') and (#userId == authentication.name or hasRole('ADMIN'))")
   ```

2. **Email Verified Endpoints** (Driver/Rider profiles):
   ```java
   @PreAuthorize("hasAuthority('EMAIL_VERIFIED') and (#userId == authentication.name or hasRole('ADMIN'))")
   ```

### Access Control
- Users can only access/modify their own profiles
- Admin role can access/modify any profile
- Path variable `userId` must match authenticated user's ID

## Error Handling

### Common Errors

1. **401 Unauthorized**
   - Missing or invalid JWT token
   - Insufficient permissions (not verified)

2. **403 Forbidden**
   - Attempting to access another user's profile
   - Missing required verification (mobile or email)

3. **404 Not Found**
   - User profile doesn't exist
   - Driver/rider profile doesn't exist

4. **400 Bad Request**
   - Invalid UUID format
   - Validation errors (invalid data)
   - License number already registered
   - License expired
   - Invalid vehicle capacity for vehicle type

5. **409 Conflict**
   - Driver/rider profile already exists

## Integration with Services

### UserProfileService
- `getProfile(UUID userId)`: Retrieve profile
- `updateProfile(...)`: Update basic profile fields

### DriverProfileService
- `createDriverProfile(UUID userId, DriverProfileRequest request)`: Create driver profile
- `updateDriverProfile(UUID userId, DriverProfileRequest request)`: Update driver profile

### RiderProfileService
- `createRiderProfile(UUID userId, RiderProfileRequest request)`: Create rider profile
- `updateRiderProfile(UUID userId, RiderProfileRequest request)`: Update rider profile

## Testing

### Manual Testing with cURL

1. **Get Profile**:
```bash
curl -X GET http://localhost:8080/users/{userId}/profile \
  -H "Authorization: Bearer <token>"
```

2. **Update Profile**:
```bash
curl -X PUT http://localhost:8080/users/{userId}/profile \
  -H "Authorization: Bearer <token>" \
  -H "Content-Type: application/json" \
  -d '{
    "firstName": "John",
    "lastName": "Doe"
  }'
```

3. **Create Driver Profile**:
```bash
curl -X POST http://localhost:8080/users/{userId}/driver-profile \
  -H "Authorization: Bearer <token>" \
  -H "Content-Type: application/json" \
  -d '{
    "licenseNumber": "DL-1234567890",
    "licenseExpiry": "2025-12-31",
    "maxDetourDistance": 500,
    "vehicle": {
      "vehicleType": "CAR",
      "make": "Toyota",
      "model": "Camry",
      "year": 2022,
      "licensePlate": "ABC-1234",
      "capacity": 4,
      "fuelType": "PETROL"
    }
  }'
```

4. **Create Rider Profile**:
```bash
curl -X POST http://localhost:8080/users/{userId}/rider-profile \
  -H "Authorization: Bearer <token>" \
  -H "Content-Type: application/json" \
  -d '{
    "routePreferences": {
      "startLatitude": 37.7749,
      "startLongitude": -122.4194,
      "startAddress": "123 Main St",
      "endLatitude": 37.3382,
      "endLongitude": -121.8863,
      "endAddress": "456 Work Ave",
      "preferredStartTimes": ["08:00"],
      "isActive": true
    },
    "genderPreference": "NO_PREFERENCE"
  }'
```

## Requirements Mapping

This implementation satisfies the following requirements from the specification:

- **Requirement 4.1**: Driver profile creation with vehicle type and fuel type support
- **Requirement 5.1**: Rider profile creation with gender and vehicle type preferences
- **Requirement 8.1**: Profile management with verification requirements

## Next Steps

1. Implement integration tests for all endpoints
2. Add API documentation with Swagger/OpenAPI
3. Implement rate limiting for profile updates
4. Add profile image upload functionality
5. Implement profile deletion endpoints (if needed)

## Notes

- All endpoints require authentication via JWT token
- Driver and rider profiles require email verification
- Route preferences are stored in DynamoDB (handled by services)
- Profile images should be uploaded to S3 (URL stored in profile)
- License verification is handled asynchronously by external service
