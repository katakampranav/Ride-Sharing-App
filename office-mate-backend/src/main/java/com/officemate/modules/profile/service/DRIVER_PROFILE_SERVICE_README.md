# Driver Profile Service Implementation

## Overview

The Driver Profile Service manages driver profiles with comprehensive vehicle information, license verification, and route preferences storage. This implementation fulfills task 4.3 of the user authentication and registration specification.

## Components

### 1. DriverProfileService

**Location:** `com.officemate.modules.profile.service.DriverProfileService`

**Responsibilities:**
- Create and update driver profiles with vehicle type and fuel type support
- Manage comprehensive vehicle information (make, model, year, capacity, license plate)
- Validate vehicle capacity based on vehicle type
- Integrate with license verification service
- Coordinate route preferences storage in DynamoDB
- Enforce maximum detour distance configuration (up to 0.5 km)

**Key Methods:**
- `createDriverProfile(UUID userId, DriverProfileRequest request)` - Creates a new driver profile with verification checks
- `updateDriverProfile(UUID userId, DriverProfileRequest request)` - Updates existing driver profile
- `getDriverProfile(UUID userId)` - Retrieves driver profile by user ID
- `verifyDriverLicense(UUID userId)` - Marks driver's license as verified
- `deleteDriverProfile(UUID userId)` - Deletes driver profile and associated data
- `validateVehicleCapacity(VehicleInfoDTO vehicle)` - Validates capacity based on vehicle type

**Verification Requirements:**
- Both mobile and email verification required before driver profile creation
- Throws `ProfileAccessException` if verification incomplete
- User profile must exist before creating driver profile

**Vehicle Capacity Validation:**
- **CAR**: 1-7 passengers
- **MOTORCYCLE/SCOOTER/BICYCLE**: 1-2 passengers

### 2. RoutePreferencesService

**Location:** `com.officemate.modules.profile.service.RoutePreferencesService`

**Responsibilities:**
- Store and retrieve route preferences in DynamoDB
- Support geospatial indexing for route matching
- Manage both HOME_TO_WORK and WORK_TO_HOME routes
- Handle route preference updates and deletions

**Key Methods:**
- `saveDriverRoutePreferences(UUID userId, RoutePreferencesDTO routeDTO)` - Saves route preferences
- `updateDriverRoutePreferences(UUID userId, RoutePreferencesDTO routeDTO)` - Updates route preferences
- `getDriverRoutePreferences(UUID userId, String routeType)` - Retrieves specific route
- `getAllDriverRoutePreferences(UUID userId)` - Retrieves all routes for a user
- `deleteDriverRoutePreferences(UUID userId)` - Deletes all route preferences

**DynamoDB Structure:**
- **Table Name:** `{prefix}_route_preferences`
- **Partition Key:** userId (String)
- **Sort Key:** routeType (HOME_TO_WORK or WORK_TO_HOME)
- **Attributes:** Coordinates, addresses, preferred times, active status

**Route Types:**
- `HOME_TO_WORK` - Morning commute route
- `WORK_TO_HOME` - Evening return route (automatically created with reversed coordinates)

### 3. RoutePreference Model

**Location:** `com.officemate.modules.profile.model.RoutePreference`

**DynamoDB Bean Attributes:**
- `userId` - User's unique identifier (partition key)
- `routeType` - Route type (sort key)
- `startLatitude/startLongitude` - Starting coordinates
- `startAddress` - Human-readable start address
- `endLatitude/endLongitude` - Destination coordinates
- `endAddress` - Human-readable end address
- `preferredStartTimes` - List of preferred departure times (HH:mm format)
- `isActive` - Route active status
- `createdAt/updatedAt` - Timestamps

### 4. LicenseVerificationService

**Location:** `com.officemate.modules.profile.service.LicenseVerificationService`

**Responsibilities:**
- Placeholder for external license verification integration
- Initiate license verification process
- Check verification status
- Handle verification callbacks

**Integration Points (Placeholder):**
- Government DMV/RTO API integration
- Third-party verification services (Checkr, Onfido)
- Document upload and manual review workflow
- OCR-based license scanning

**Key Methods:**
- `initiateLicenseVerification(UUID userId, String licenseNumber)` - Starts verification
- `checkVerificationStatus(UUID userId)` - Checks current status
- `handleVerificationCallback(UUID userId, String verificationResult)` - Processes results

## Data Flow

### Driver Profile Creation Flow

1. **Verification Check**
   - Validate user has both mobile and email verified
   - Check if user profile exists
   - Verify driver profile doesn't already exist

2. **License Validation**
   - Check license number uniqueness
   - Validate license expiry date is in future
   - Initiate license verification process

3. **Vehicle Validation**
   - Validate vehicle capacity for vehicle type
   - Store vehicle information (type, fuel, make, model, year, plate, capacity)

4. **Profile Creation**
   - Create DriverProfile entity in PostgreSQL
   - Set license verification status to false
   - Configure maximum detour distance (default: 500m)

5. **Route Preferences Storage**
   - Store HOME_TO_WORK route in DynamoDB
   - Automatically create WORK_TO_HOME route (reversed)
   - Include geospatial coordinates for matching

### Driver Profile Update Flow

1. **License Update Handling**
   - Check if license number changed
   - Validate new license number uniqueness
   - Unverify license and re-initiate verification

2. **Vehicle Update**
   - Validate new vehicle capacity
   - Update vehicle information

3. **Route Preferences Update**
   - Update both HOME_TO_WORK and WORK_TO_HOME routes
   - Maintain geospatial indexing

## Requirements Mapping

This implementation addresses the following requirements from the specification:

### Requirement 4.2
- Driver profile requires typical commute route with start/end geo-coordinates
- Implemented via RoutePreferencesService with DynamoDB storage

### Requirement 4.3
- Maximum acceptable detour distance up to 0.5 km (500 meters)
- Validated and stored in DriverProfile entity

### Requirement 4.4
- Vehicle information captured: make, model, license plate, capacity
- Vehicle type support: CAR, MOTORCYCLE, SCOOTER, BICYCLE
- Fuel type support: PETROL, DIESEL, ELECTRIC, HYBRID, CNG
- Capacity validation based on vehicle type

### Requirement 4.5
- Driver's license information required
- License verification integration placeholder
- License re-verification on information changes

## Configuration

### Application Properties Required

```yaml
aws:
  region: us-east-1
  dynamodb:
    endpoint: http://localhost:4566  # For local development
    table-prefix: dev  # Environment-specific prefix
```

### DynamoDB Table Setup

The route preferences table should be created with:
- Table name: `{prefix}_route_preferences`
- Partition key: `userId` (String)
- Sort key: `routeType` (String)
- Geospatial index for location-based queries (future enhancement)

## Error Handling

### ProfileAccessException
Thrown when user attempts to create driver profile without full verification:
- Contains mobile and email verification status
- Provides clear error message to user

### EntityNotFoundException
Thrown when:
- User account not found
- User profile not found
- Driver profile not found

### IllegalStateException
Thrown when:
- Driver profile already exists
- License number already registered

### IllegalArgumentException
Thrown when:
- License expiry date is in the past
- Vehicle capacity invalid for vehicle type

### DynamoDbException
Wrapped in RuntimeException when:
- DynamoDB operations fail
- Route preferences cannot be saved/retrieved

## Testing Considerations

### Unit Tests Should Cover:
1. Driver profile creation with verification checks
2. Vehicle capacity validation for all vehicle types
3. License number uniqueness validation
4. Route preferences storage and retrieval
5. Profile update with license re-verification
6. Error handling for all exception scenarios

### Integration Tests Should Cover:
1. End-to-end driver profile creation flow
2. DynamoDB route preferences storage
3. License verification service integration
4. Profile update workflows

## Future Enhancements

1. **Geospatial Indexing**
   - Implement DynamoDB geospatial queries
   - Enable proximity-based driver matching
   - Support 0.5km radius searches

2. **License Verification Integration**
   - Connect to government DMV/RTO APIs
   - Implement document upload workflow
   - Add OCR-based license scanning

3. **Real-time Updates**
   - WebSocket notifications for verification status
   - Live route preference updates
   - Driver availability tracking

4. **Analytics**
   - Track popular routes
   - Vehicle type distribution
   - Verification success rates

## Dependencies

- Spring Boot 3.x
- Spring Data JPA (PostgreSQL)
- AWS SDK for DynamoDB Enhanced Client
- Lombok for boilerplate reduction
- Jakarta Validation for input validation

## Related Files

- `DriverProfile.java` - JPA entity for driver profiles
- `DriverProfileRepository.java` - Spring Data JPA repository
- `DriverProfileRequest.java` - Request DTO
- `VehicleInfoDTO.java` - Vehicle information DTO
- `RoutePreferencesDTO.java` - Route preferences DTO
- `VehicleType.java` - Vehicle type enum
- `FuelType.java` - Fuel type enum
