# RiderProfileService Implementation

## Overview

The `RiderProfileService` manages rider profiles in the corporate ride-sharing application. It handles rider profile creation, updates, gender preference management, vehicle type preference management, route preferences storage in DynamoDB, and favorite driver management.

## Key Features

### 1. Rider Profile Creation
- **Verification Required**: Both mobile and email verification must be completed before creating a rider profile
- **Gender Preferences**: Supports FEMALE_ONLY, MALE_SINGLE_FEMALE, MALE_ALL_FEMALE, NO_PREFERENCE
- **Vehicle Type Preferences**: Riders can specify preferred vehicle types (CAR, MOTORCYCLE, SCOOTER, BICYCLE)
- **Route Preferences**: Stores route information in DynamoDB with geospatial indexing
- **Favorite Drivers**: Maintains a list of favorite driver user IDs

### 2. Gender Preference Management
Riders can set and update gender preferences for ride matching:
- `FEMALE_ONLY`: Female riders can request female drivers only
- `MALE_SINGLE_FEMALE`: Male riders can prefer rides with single female passengers
- `MALE_ALL_FEMALE`: Male riders can prefer rides with all female passengers
- `NO_PREFERENCE`: No gender-based matching preference

### 3. Vehicle Type Preference Management
Riders can specify which vehicle types they prefer:
- Add vehicle type preferences
- Remove vehicle type preferences
- Support for multiple vehicle types: CAR, MOTORCYCLE, SCOOTER, BICYCLE

### 4. Route Preferences Management
- Stores route preferences in DynamoDB for efficient geospatial queries
- Supports HOME_TO_WORK and WORK_TO_HOME routes
- Includes start/end coordinates, addresses, and preferred travel times
- Integrates with `RoutePreferencesService` for DynamoDB operations

### 5. Favorite Driver Management
- Add drivers to favorites list
- Remove drivers from favorites list
- Stored as array of driver user IDs in PostgreSQL

## Service Methods

### Core Operations

#### `createRiderProfile(UUID userId, RiderProfileRequest request)`
Creates a new rider profile with preferences and route information.
- **Requires**: Both mobile and email verification
- **Validates**: User profile exists, rider profile doesn't already exist
- **Stores**: Profile in PostgreSQL, route preferences in DynamoDB
- **Returns**: `RiderProfileResponse`

#### `updateRiderProfile(UUID userId, RiderProfileRequest request)`
Updates an existing rider profile with new preferences.
- **Updates**: Gender preference, vehicle type preferences, favorite drivers
- **Optional**: Route preferences update in DynamoDB
- **Returns**: `RiderProfileResponse`

#### `getRiderProfile(UUID userId)`
Retrieves a rider profile with route preferences.
- **Fetches**: Profile from PostgreSQL, route preferences from DynamoDB
- **Returns**: `RiderProfileResponse` with complete information

### Preference Management

#### `updateGenderPreference(UUID userId, GenderPreference genderPreference)`
Updates only the gender preference for a rider.

#### `addVehicleTypePreference(UUID userId, VehicleType vehicleType)`
Adds a vehicle type to the rider's preferences.

#### `removeVehicleTypePreference(UUID userId, VehicleType vehicleType)`
Removes a vehicle type from the rider's preferences.

### Favorite Driver Management

#### `addFavoriteDriver(UUID userId, UUID driverId)`
Adds a driver to the rider's favorites list.

#### `removeFavoriteDriver(UUID userId, UUID driverId)`
Removes a driver from the rider's favorites list.

### Route Management

#### `updateRoutePreferences(UUID userId, RoutePreferencesDTO routePreferences)`
Updates route preferences in DynamoDB.

### Utility Methods

#### `riderProfileExists(UUID userId)`
Checks if a rider profile exists for the given user.

#### `deleteRiderProfile(UUID userId)`
Deletes a rider profile and associated route preferences.

## Data Storage

### PostgreSQL (RiderProfile Entity)
- Rider ID (UUID, primary key)
- Gender preference (enum)
- Vehicle type preferences (text array)
- Favorite drivers (text array)
- Timestamps (created_at, updated_at)

### DynamoDB (RoutePreference)
- User ID (partition key)
- Route type (sort key: HOME_TO_WORK, WORK_TO_HOME)
- Start/end coordinates and addresses
- Preferred start times
- Active status

## Integration Points

### Dependencies
- `RiderProfileRepository`: PostgreSQL repository for rider profiles
- `UserProfileRepository`: Validates user profile exists
- `UserAccountRepository`: Validates user verification status
- `RoutePreferencesService`: Manages route preferences in DynamoDB

### Exception Handling
- `ProfileAccessException`: Thrown when verification requirements not met
- `EntityNotFoundException`: Thrown when profile or user not found
- `IllegalStateException`: Thrown when rider profile already exists

## Usage Example

```java
@Autowired
private RiderProfileService riderProfileService;

// Create rider profile
RiderProfileRequest request = RiderProfileRequest.builder()
    .genderPreference(GenderPreference.FEMALE_ONLY)
    .vehicleTypePreferences(List.of(VehicleType.CAR, VehicleType.MOTORCYCLE))
    .routePreferences(routeDTO)
    .build();

RiderProfileResponse response = riderProfileService.createRiderProfile(userId, request);

// Update gender preference
riderProfileService.updateGenderPreference(userId, GenderPreference.NO_PREFERENCE);

// Add favorite driver
riderProfileService.addFavoriteDriver(userId, driverId);

// Add vehicle type preference
riderProfileService.addVehicleTypePreference(userId, VehicleType.SCOOTER);
```

## Requirements Satisfied

This implementation satisfies the following requirements from the design document:

- **Requirement 5.1**: Rider profile creation requiring mobile and email verification
- **Requirement 5.2**: Typical commute route with start and end geo-coordinates
- **Requirement 5.3**: Preferred travel times for commuting schedules
- **Requirement 5.4**: Female users can select female-only driver preferences
- **Requirement 5.5**: Male users can set female co-rider preferences

## Security Considerations

1. **Verification Enforcement**: All rider profile operations require both mobile and email verification
2. **Data Validation**: Input validation through DTOs with Jakarta validation annotations
3. **Access Control**: Users can only manage their own rider profiles
4. **Audit Logging**: All operations logged with SLF4J for audit trails

## Performance Optimizations

1. **DynamoDB Integration**: Route preferences stored in DynamoDB for efficient geospatial queries
2. **Lazy Loading**: UserProfile relationship loaded only when needed
3. **Batch Operations**: Vehicle type preferences stored as arrays for efficient updates
4. **Caching Ready**: Service methods designed to work with caching layers

## Future Enhancements

1. **Matching Algorithm Integration**: Connect with ride matching service
2. **Preference Analytics**: Track preference patterns for better matching
3. **Real-time Updates**: WebSocket support for live preference changes
4. **Advanced Filtering**: More granular vehicle and driver preferences
