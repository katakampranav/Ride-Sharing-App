# Profile Entities Implementation

## Overview

This document describes the PostgreSQL JPA entities created for comprehensive user profile management in the OfficeMate ride-sharing application.

## Entities Created

### 1. UserProfile Entity
**Location**: `com.officemate.modules.profile.entity.UserProfile`

**Purpose**: Stores basic profile information for all users (both drivers and riders).

**Key Features**:
- One-to-one relationship with `UserAccount` (uses `@MapsId` for shared primary key)
- Requires both mobile and email verification before creation
- Contains basic user information: first name, last name, profile image, date of birth, gender
- Includes validation annotations for data integrity
- Provides utility methods: `getFullName()`, `isComplete()`

**Database Table**: `user_profiles`

**Relationships**:
- `UserAccount` (1:1) - Parent relationship
- `DriverProfile` (1:0..1) - Optional driver capability
- `RiderProfile` (1:0..1) - Optional rider capability

### 2. DriverProfile Entity
**Location**: `com.officemate.modules.profile.entity.DriverProfile`

**Purpose**: Stores driver-specific information including vehicle details and license information.

**Key Features**:
- One-to-one relationship with `UserProfile` (uses `@MapsId` for shared primary key)
- Vehicle type support: CAR, MOTORCYCLE, SCOOTER, BICYCLE
- Fuel type support: PETROL, DIESEL, ELECTRIC, HYBRID, CNG
- License verification tracking
- Maximum detour distance configuration (0-500 meters)
- Vehicle capacity validation based on vehicle type
- Comprehensive vehicle information: make, model, year, license plate

**Database Table**: `driver_profiles`

**Indexes**:
- `idx_driver_profiles_vehicle_type` on `vehicle_type` column

**Validation Rules**:
- License expiry must be in the future
- Max detour distance: 0-500 meters
- Vehicle capacity: 1-7 for cars, 1-2 for 2-wheelers
- Vehicle year: 1900-2100

**Utility Methods**:
- `isLicenseValid()` - Checks if license is not expired
- `isComplete()` - Validates all required fields are populated
- `isCapacityValidForVehicleType()` - Validates capacity matches vehicle type
- `verifyLicense()` / `unverifyLicense()` - License verification management

### 3. RiderProfile Entity
**Location**: `com.officemate.modules.profile.entity.RiderProfile`

**Purpose**: Stores rider-specific preferences including gender preferences and vehicle type preferences.

**Key Features**:
- One-to-one relationship with `UserProfile` (uses `@MapsId` for shared primary key)
- Gender preference support: FEMALE_ONLY, MALE_SINGLE_FEMALE, MALE_ALL_FEMALE, NO_PREFERENCE
- Vehicle type preferences (array of preferred vehicle types)
- Favorite drivers list (array of driver user IDs)
- PostgreSQL array support using `@JdbcTypeCode(SqlTypes.ARRAY)`

**Database Table**: `rider_profiles`

**Array Columns**:
- `vehicle_type_preferences` (TEXT[]) - Stores preferred vehicle types
- `favorite_drivers` (TEXT[]) - Stores favorite driver UUIDs

**Utility Methods**:
- `addVehicleTypePreference()` / `removeVehicleTypePreference()` - Manage vehicle preferences
- `prefersVehicleType()` - Check if vehicle type is preferred
- `addFavoriteDriver()` / `removeFavoriteDriver()` - Manage favorite drivers
- `isFavoriteDriver()` - Check if driver is favorited
- `hasGenderPreference()` - Check if gender preference is set

## Repositories Created

### 1. UserProfileRepository
**Location**: `com.officemate.modules.profile.repository.UserProfileRepository`

**Key Methods**:
- `findByIdWithUserAccount()` - Eager load UserAccount relationship
- `existsByUserId()` - Check profile existence
- `findByPhoneNumber()` - Find profile by phone number
- `findByCorporateEmail()` - Find profile by corporate email

### 2. DriverProfileRepository
**Location**: `com.officemate.modules.profile.repository.DriverProfileRepository`

**Key Methods**:
- `findByIdWithUserProfile()` - Eager load UserProfile relationship
- `findByVehicleType()` - Filter by vehicle type
- `findVerifiedDriversByVehicleType()` - Find verified drivers by vehicle type
- `findDriversWithExpiredLicenses()` - Find drivers with expired licenses
- `findDriversWithExpiringLicenses()` - Find drivers with licenses expiring soon
- `findByLicenseNumber()` - Find driver by license number
- `existsByLicenseNumber()` - Check if license number exists
- `findAllVerifiedDrivers()` / `findAllUnverifiedDrivers()` - Filter by verification status

### 3. RiderProfileRepository
**Location**: `com.officemate.modules.profile.repository.RiderProfileRepository`

**Key Methods**:
- `findByIdWithUserProfile()` - Eager load UserProfile relationship
- `findByGenderPreference()` - Filter by gender preference
- `findByFavoriteDriver()` - Find riders who favorited a specific driver
- `findByVehicleTypePreference()` - Find riders who prefer a specific vehicle type
- `findAllWithGenderPreferences()` - Find riders with specific gender preferences
- `findAllWithNoGenderPreference()` - Find riders with no gender preference

## Entity Relationships

```
UserAccount (1) ←→ (1) UserProfile
                        ↓
                        ├─→ (0..1) DriverProfile
                        └─→ (0..1) RiderProfile
```

## Key Design Decisions

1. **Shared Primary Keys**: All profile entities use `@MapsId` to share the same UUID as the UserAccount, ensuring referential integrity and simplifying queries.

2. **Lazy Loading**: Relationships use `FetchType.LAZY` by default to avoid unnecessary database queries. Repositories provide explicit methods for eager loading when needed.

3. **Bean Validation**: Extensive use of Jakarta Bean Validation annotations (`@NotBlank`, `@Size`, `@Min`, `@Max`, `@Past`, `@Future`) for data integrity.

4. **PostgreSQL Arrays**: RiderProfile uses PostgreSQL's native array support for storing lists of vehicle type preferences and favorite drivers.

5. **Enum Storage**: Enums are stored as strings (`@Enumerated(EnumType.STRING)`) for better readability and maintainability.

6. **Audit Timestamps**: All entities include `@CreationTimestamp` and `@UpdateTimestamp` for automatic timestamp management.

7. **Business Logic Methods**: Entities include utility methods for common business logic operations, keeping the logic close to the data.

## Requirements Satisfied

This implementation satisfies the following requirements from the specification:

- **Requirement 4.1**: Driver profile with vehicle information and license verification
- **Requirement 5.1**: Rider profile with route and preference information
- **Requirement 8.1**: Profile management and updates

## Next Steps

The next task (4.2) will implement the `UserProfileService` that uses these entities to provide profile management functionality, including:
- Profile creation requiring both mobile and email verification
- Profile retrieval and update operations
- Access control based on verification status
- Integration with existing UserAccount entity

## Database Schema

The entities will create the following PostgreSQL tables:

```sql
-- user_profiles table
CREATE TABLE user_profiles (
    user_id UUID PRIMARY KEY,
    first_name VARCHAR(100) NOT NULL,
    last_name VARCHAR(100) NOT NULL,
    profile_image_url VARCHAR(500),
    date_of_birth DATE,
    gender VARCHAR(10),
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    FOREIGN KEY (user_id) REFERENCES user_accounts(user_id)
);

-- driver_profiles table
CREATE TABLE driver_profiles (
    driver_id UUID PRIMARY KEY,
    license_number VARCHAR(50) NOT NULL,
    license_expiry DATE NOT NULL,
    license_verified BOOLEAN NOT NULL DEFAULT FALSE,
    max_detour_distance INTEGER NOT NULL DEFAULT 500,
    vehicle_type VARCHAR(20) NOT NULL,
    vehicle_make VARCHAR(50),
    vehicle_model VARCHAR(50),
    vehicle_year INTEGER,
    license_plate VARCHAR(20),
    vehicle_capacity INTEGER,
    fuel_type VARCHAR(20),
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    FOREIGN KEY (driver_id) REFERENCES user_profiles(user_id)
);

CREATE INDEX idx_driver_profiles_vehicle_type ON driver_profiles(vehicle_type);

-- rider_profiles table
CREATE TABLE rider_profiles (
    rider_id UUID PRIMARY KEY,
    gender_preference VARCHAR(20),
    vehicle_type_preferences TEXT[],
    favorite_drivers TEXT[],
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    FOREIGN KEY (rider_id) REFERENCES user_profiles(user_id)
);
```

## Testing

Unit tests for these entities should be created in the next phase to verify:
- Entity creation and persistence
- Relationship mappings
- Validation constraints
- Utility method behavior
- Repository query methods
