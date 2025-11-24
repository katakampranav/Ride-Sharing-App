# Safety Module

## Overview
The Safety module manages emergency contacts, family ride sharing, and safety features for the corporate ride-sharing application.

## Responsibilities
- Emergency contact management
- Family ride sharing setup and notifications
- SOS emergency functionality
- Real-time location sharing
- Emergency alert processing
- Safety preferences management

## Package Structure
```
safety/
├── entity/          # JPA entities (EmergencyContact, FamilySharingContact)
├── repository/      # Spring Data JPA repositories
├── service/         # Business logic for safety features
└── controller/      # REST API endpoints for safety operations
```

## Key Components
- **SafetyService**: Core safety operations and emergency contact management
- **EmergencyAlertService**: SOS and emergency alert processing
- **FamilySharingService**: Family contact and ride sharing notifications

## Related Requirements
- Requirement 9.1: Emergency contact management
- Requirement 9.2: Family member designation for ride sharing
- Requirement 9.3: SOS emergency button functionality
- Requirement 9.4: Real-time ride tracking sharing
- Requirement 9.5: Emergency alert capabilities
