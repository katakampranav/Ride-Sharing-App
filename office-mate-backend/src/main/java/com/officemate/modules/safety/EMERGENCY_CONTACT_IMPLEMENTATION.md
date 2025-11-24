# Emergency Contact Management Implementation

## Overview
This document describes the implementation of emergency contact management functionality for the safety module, completed as part of task 7.1.

## Components Implemented

### 1. Entity Layer
**File**: `entity/EmergencyContact.java`
- PostgreSQL JPA entity for storing emergency contact information
- Fields: contactId (UUID), userId, name, phoneNumber, relationship, isPrimary, createdAt
- Indexed on userId and (userId, isPrimary) for efficient queries
- Helper methods for managing primary contact designation

### 2. Repository Layer
**File**: `repository/EmergencyContactRepository.java`
- Spring Data JPA repository interface
- Custom query methods:
  - `findByUserId()` - Get all contacts for a user
  - `findByUserIdAndIsPrimaryTrue()` - Get primary contact
  - `countByUserId()` - Count contacts per user
  - `existsByUserIdAndIsPrimaryTrue()` - Check for primary contact
  - `unmarkAllAsPrimaryForUser()` - Batch update to unmark primary contacts
  - `findByContactIdAndUserId()` - Secure contact retrieval with ownership verification

### 3. Service Layer
**File**: `service/SafetyService.java`
- Core business logic for emergency contact management
- Features:
  - Add emergency contact with validation
  - Update existing contacts
  - Delete contacts
  - Set/unset primary contact designation
  - Automatic primary contact management (only one primary per user)
  - Maximum 5 contacts per user limit
  - Phone number validation (E.164 format)

### 4. Exception Handling
**File**: `shared/exception/SafetyException.java`
- Custom exception for safety-related errors
- Integrated with GlobalExceptionHandler for consistent error responses

**Updated**: `shared/exception/GlobalExceptionHandler.java`
- Added handler for SafetyException
- Returns 400 BAD_REQUEST with standardized error format

### 5. DTOs
**File**: `shared/dto/EmergencyContactResponse.java`
- Response DTO for emergency contact data
- Fields: contactId, name, phoneNumber, relationship, isPrimary, createdAt

**Existing**: `shared/dto/SafetySettingsRequest.java`
- Already contains EmergencyContactDTO nested class for requests

## Key Features

### Validation
- Phone number format validation using regex pattern (E.164)
- Required field validation (name, phoneNumber)
- Maximum contact limit enforcement (5 per user)

### Primary Contact Management
- Only one contact can be marked as primary per user
- Automatic unmarking of existing primary when setting a new one
- Transactional consistency for primary contact updates

### Security
- All operations verify user ownership of contacts
- Repository methods include userId in queries to prevent unauthorized access

### Database Schema
```sql
CREATE TABLE emergency_contacts (
    contact_id UUID PRIMARY KEY,
    user_id UUID NOT NULL,
    name VARCHAR(100) NOT NULL,
    phone_number VARCHAR(20) NOT NULL,
    relationship VARCHAR(50),
    is_primary BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP NOT NULL
);

CREATE INDEX idx_emergency_contacts_user ON emergency_contacts(user_id);
CREATE INDEX idx_emergency_contacts_primary ON emergency_contacts(user_id, is_primary);
```

## Requirements Satisfied
- ✅ Create PostgreSQL entity classes for emergency_contacts table
- ✅ Implement emergency contact addition and management logic
- ✅ Add primary contact designation functionality
- ✅ Create emergency contact validation and storage
- ✅ Requirements: 9.1, 9.2

## Testing Notes
- Unit tests should be created separately as per task 7.4
- Service layer uses @Transactional for data consistency
- Repository operations are tested through service layer integration tests

## Next Steps
- Task 7.2: Implement family sharing functionality
- Task 7.3: Create SOS emergency system foundation
- Task 7.4: Write unit tests for safety service
