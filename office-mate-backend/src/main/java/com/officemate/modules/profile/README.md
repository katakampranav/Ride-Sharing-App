# Profile Module

## Overview
The Profile module manages comprehensive user profiles with driver and rider capabilities for the corporate ride-sharing application.

## Responsibilities
- User profile creation and management
- Driver profile management with vehicle information and license verification
- Rider profile management with route and gender preferences
- Profile access control based on verification status
- Vehicle information storage and validation

## Package Structure
```
profile/
├── entity/          # JPA entities (UserProfile, DriverProfile, RiderProfile)
├── repository/      # Spring Data JPA repositories
├── service/         # Business logic and profile management services
└── controller/      # REST API endpoints for profile operations
```

## Key Components
- **UserProfileService**: Core profile management operations
- **DriverProfileService**: Driver-specific profile and vehicle management
- **RiderProfileService**: Rider-specific profile and preference management

## Related Requirements
- Requirement 4.1: Driver profile with vehicle and route information
- Requirement 5.1: Rider profile with preferences
- Requirement 8.1: Profile updates and management
