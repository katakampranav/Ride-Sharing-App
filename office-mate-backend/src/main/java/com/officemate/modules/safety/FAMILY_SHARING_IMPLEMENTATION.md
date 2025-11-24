# Family Sharing Contact Implementation

## Overview
This document describes the implementation of the family sharing functionality for the safety module, which allows users to designate family members who can receive ride updates and track rides in real-time.

## Components Implemented

### 1. FamilySharingContact Entity
**Location:** `src/main/java/com/officemate/modules/safety/entity/FamilySharingContact.java`

PostgreSQL entity representing family sharing contacts with the following fields:
- `sharingId` (UUID): Unique identifier
- `userId` (UUID): Reference to the user account
- `name` (String): Family member's full name
- `phoneNumber` (String, optional): Family member's phone number
- `email` (String, optional): Family member's email address
- `receiveRideUpdates` (Boolean): Flag for ride update notifications (default: true)
- `createdAt` (LocalDateTime): Timestamp of creation

**Database Table:** `family_sharing_contacts`

**Key Methods:**
- `enableRideUpdates()`: Enable ride update notifications
- `disableRideUpdates()`: Disable ride update notifications
- `shouldReceiveRideUpdates()`: Check if contact should receive updates

### 2. FamilySharingContactRepository
**Location:** `src/main/java/com/officemate/modules/safety/repository/FamilySharingContactRepository.java`

Spring Data JPA repository providing database operations:
- `findByUserId(UUID userId)`: Get all family sharing contacts for a user
- `findByUserIdAndReceiveRideUpdatesTrue(UUID userId)`: Get contacts with ride updates enabled
- `countByUserId(UUID userId)`: Count family sharing contacts for a user
- `deleteByUserId(UUID userId)`: Delete all family sharing contacts for a user
- `findBySharingIdAndUserId(UUID sharingId, UUID userId)`: Find specific contact with ownership verification

### 3. SafetyService Updates
**Location:** `src/main/java/com/officemate/modules/safety/service/SafetyService.java`

Added family sharing management methods:

#### Public Methods:
- `addFamilySharingContact(UUID userId, FamilySharingContactDTO contactDTO)`: Add new family sharing contact
- `getFamilySharingContacts(UUID userId)`: Get all family sharing contacts
- `getFamilySharingContactsWithRideUpdates(UUID userId)`: Get contacts with ride updates enabled
- `updateFamilySharingContact(UUID userId, UUID sharingId, FamilySharingContactDTO contactDTO)`: Update existing contact
- `deleteFamilySharingContact(UUID userId, UUID sharingId)`: Delete a family sharing contact
- `enableRideUpdatesForFamilyContact(UUID userId, UUID sharingId)`: Enable ride updates for a contact
- `disableRideUpdatesForFamilyContact(UUID userId, UUID sharingId)`: Disable ride updates for a contact
- `deleteAllFamilySharingContacts(UUID userId)`: Delete all family sharing contacts for a user

#### Validation:
- Maximum 10 family sharing contacts per user
- At least one contact method (phone or email) required
- Phone number validation using E.164 format
- Email validation using standard email pattern

## Features

### Contact Management
- Users can add up to 10 family members for ride sharing
- Each contact requires a name and at least one contact method (phone or email)
- Both phone and email are optional, but at least one must be provided
- Contacts can be updated or deleted individually

### Ride Update Notifications
- Each contact has a `receiveRideUpdates` flag (default: true)
- Users can enable/disable ride updates for individual contacts
- Service provides method to retrieve only contacts with ride updates enabled
- This allows selective notification of family members during rides

### Security
- All operations verify user ownership of contacts
- Repository methods include userId in queries to prevent unauthorized access
- Transactional operations ensure data consistency

## Integration with Requirements

This implementation satisfies the following requirements from the design document:

**Requirement 9.2:** "THE Safety_System SHALL allow designation of family members for ride sharing notifications"
- ✅ Implemented through `addFamilySharingContact()` method
- ✅ Supports multiple family members (up to 10)

**Requirement 9.4:** "THE Safety_System SHALL enable real-time ride tracking sharing with designated contacts"
- ✅ Foundation implemented with `receiveRideUpdates` flag
- ✅ Service method to retrieve contacts with ride updates enabled
- ✅ Ready for integration with real-time tracking system

## Database Schema

```sql
CREATE TABLE family_sharing_contacts (
    sharing_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES user_accounts(user_id),
    name VARCHAR(100) NOT NULL,
    phone_number VARCHAR(20),
    email VARCHAR(255),
    receive_ride_updates BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_family_sharing_user ON family_sharing_contacts(user_id);
```

## Usage Example

```java
// Add a family sharing contact
FamilySharingContactDTO contactDTO = FamilySharingContactDTO.builder()
    .name("Jane Doe")
    .phoneNumber("+1234567890")
    .email("jane@example.com")
    .receiveRideUpdates(true)
    .build();

FamilySharingContact contact = safetyService.addFamilySharingContact(userId, contactDTO);

// Get all contacts with ride updates enabled
List<FamilySharingContact> activeContacts = 
    safetyService.getFamilySharingContactsWithRideUpdates(userId);

// Disable ride updates for a specific contact
safetyService.disableRideUpdatesForFamilyContact(userId, sharingId);
```

## Next Steps

To complete the family sharing functionality:
1. Create REST API endpoints for family sharing contact management
2. Integrate with real-time ride tracking system
3. Implement notification service to send ride updates to family contacts
4. Add unit tests for family sharing service methods
5. Add integration tests for complete workflow

## Notes

- The implementation follows the same patterns as emergency contact management
- Email and phone validation patterns are consistent across the safety module
- The service is transaction-safe and includes proper error handling
- All methods include logging for debugging and audit purposes
