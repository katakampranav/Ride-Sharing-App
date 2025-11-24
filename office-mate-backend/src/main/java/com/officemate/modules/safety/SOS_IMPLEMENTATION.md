# SOS Emergency System Implementation

## Overview

This document describes the implementation of the SOS emergency system foundation for the ride-sharing application. The system provides emergency alert functionality and real-time location sharing capabilities to enhance user safety during rides.

## Components Implemented

### 1. Database Entities

#### SOSAlert Entity
- **Table**: `sos_alerts`
- **Purpose**: Stores SOS emergency alerts triggered by users
- **Key Fields**:
  - `alert_id`: Unique identifier (UUID)
  - `user_id`: User who triggered the alert
  - `ride_id`: Associated ride (optional)
  - `status`: Alert status (ACTIVE, RESOLVED, CANCELLED)
  - `latitude`, `longitude`: Location coordinates
  - `message`: Optional user message
  - `created_at`, `resolved_at`: Timestamps
  - `resolved_by`, `resolution_notes`: Resolution tracking

#### LocationShare Entity
- **Table**: `location_shares`
- **Purpose**: Manages real-time location sharing sessions
- **Key Fields**:
  - `share_id`: Unique identifier (UUID)
  - `user_id`: User sharing location
  - `ride_id`: Associated ride (optional)
  - `is_active`: Active status flag
  - `current_latitude`, `current_longitude`: Current location
  - `last_location_update`: Last update timestamp
  - `share_token`: Unique token for accessing the share
  - `created_at`, `updated_at`, `ended_at`: Timestamps

### 2. Repository Interfaces

#### SOSAlertRepository
Provides database operations for SOS alerts:
- Find alerts by user, status, ride
- Find active alerts
- Count and check existence of alerts
- Time-range queries for reporting

#### LocationShareRepository
Provides database operations for location sharing:
- Find shares by user, token, ride
- Find active shares
- Update location coordinates
- End sharing sessions
- Bulk operations for user cleanup

### 3. DTOs (Data Transfer Objects)

#### Request DTOs
- **SOSRequest**: Trigger SOS alert with location and optional message
- **LocationShareRequest**: Start/update location sharing with coordinates

#### Response DTOs
- **SOSResponse**: SOS alert details with notification status
- **LocationShareResponse**: Location share details with shareable URL

### 4. Service Layer Methods

#### SOS Alert Management
- `triggerSOSAlert()`: Create new SOS alert with validation
- `getSOSAlerts()`: Retrieve all alerts for a user
- `getActiveSOSAlert()`: Get current active alert
- `resolveSOSAlert()`: Mark alert as resolved (admin/support)
- `cancelSOSAlert()`: User-initiated cancellation
- `notifyEmergencyContacts()`: Placeholder for notifications

#### Location Sharing Management
- `startLocationSharing()`: Begin location sharing session
- `updateSharedLocation()`: Update location in active session
- `getActiveLocationShare()`: Get current active share
- `getLocationShareByToken()`: Access share via token (for family)
- `endLocationSharing()`: End specific sharing session
- `endAllLocationSharing()`: End all active shares for user
- `notifyFamilyContacts()`: Placeholder for notifications

## Key Features

### 1. SOS Emergency Button Functionality
- Users can trigger SOS alerts with current location
- Validates location coordinates (latitude: -90 to 90, longitude: -180 to 180)
- Prevents duplicate active alerts per user
- Associates alerts with rides when applicable
- Tracks alert lifecycle (active → resolved/cancelled)

### 2. Emergency Alert Processing
- Creates persistent alert records in database
- Captures location, timestamp, and optional user message
- Supports resolution tracking (who resolved, when, notes)
- Allows user-initiated cancellation
- Foundation for notification system integration

### 3. Real-time Location Sharing
- Generates unique shareable tokens for each session
- Tracks location updates with timestamps
- Supports multiple sharing sessions per user
- Associates with rides for ride-specific tracking
- Automatic sharing when SOS is triggered

### 4. Emergency Contact Notification System
- Placeholder methods for future notification integration
- Retrieves emergency contacts from database
- Foundation for SMS, email, and push notifications
- Includes location links in notifications (future)
- Notifies family sharing contacts with ride updates

## Integration Points

### With Emergency Contacts
- SOS alerts automatically notify all emergency contacts
- Primary contact receives priority notification (future)
- Includes alert location and details

### With Family Sharing
- Location sharing notifies family members with ride updates enabled
- Provides shareable token/URL for real-time tracking
- Automatic notification when sharing starts

### With Ride System (Future)
- Associates SOS alerts with active rides
- Links location sharing to ride sessions
- Enables ride-specific emergency response

## Security Considerations

### Location Data
- Validates all coordinate inputs
- Stores location with timestamps for audit trail
- Restricts access to location shares via tokens
- Users can only access their own alerts and shares

### Access Control
- Users can only trigger/cancel their own SOS alerts
- Admin/support can resolve alerts with tracking
- Family members access location via secure tokens
- All operations logged for audit purposes

## Future Enhancements

### Notification Integration
The placeholder methods `notifyEmergencyContacts()` and `notifyFamilyContacts()` will be enhanced to:
1. Send SMS via AWS SNS to emergency contacts
2. Send emails via AWS SES with location links
3. Send push notifications to mobile apps
4. Include real-time location tracking URLs
5. Provide one-click response options

### Real-time Updates
- WebSocket integration for live location updates
- Push notifications for location changes
- Automatic location polling during active alerts
- Battery-efficient location tracking

### Advanced Features
- Geofencing for automatic alerts
- Silent alarm mode
- Audio/video recording during emergencies
- Integration with local emergency services
- Panic button hardware integration

## Database Schema

### SOS Alerts Table
```sql
CREATE TABLE sos_alerts (
    alert_id UUID PRIMARY KEY,
    user_id UUID NOT NULL,
    ride_id UUID,
    status VARCHAR(20) NOT NULL,
    latitude DOUBLE PRECISION NOT NULL,
    longitude DOUBLE PRECISION NOT NULL,
    message VARCHAR(500),
    created_at TIMESTAMP NOT NULL,
    resolved_at TIMESTAMP,
    resolved_by UUID,
    resolution_notes VARCHAR(1000)
);

CREATE INDEX idx_sos_alerts_user ON sos_alerts(user_id);
CREATE INDEX idx_sos_alerts_status ON sos_alerts(status);
CREATE INDEX idx_sos_alerts_created ON sos_alerts(created_at);
```

### Location Shares Table
```sql
CREATE TABLE location_shares (
    share_id UUID PRIMARY KEY,
    user_id UUID NOT NULL,
    ride_id UUID,
    is_active BOOLEAN NOT NULL,
    current_latitude DOUBLE PRECISION,
    current_longitude DOUBLE PRECISION,
    last_location_update TIMESTAMP,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    ended_at TIMESTAMP,
    share_token VARCHAR(100) UNIQUE
);

CREATE INDEX idx_location_shares_user ON location_shares(user_id);
CREATE INDEX idx_location_shares_ride ON location_shares(ride_id);
CREATE INDEX idx_location_shares_status ON location_shares(is_active);
```

## Testing Recommendations

### Unit Tests
- Test SOS alert creation with valid/invalid coordinates
- Test duplicate alert prevention
- Test alert resolution and cancellation
- Test location sharing session lifecycle
- Test location update validation
- Test share token generation uniqueness

### Integration Tests
- Test SOS alert with emergency contact retrieval
- Test location sharing with family contact retrieval
- Test automatic location sharing on SOS trigger
- Test concurrent location updates
- Test share token access

### End-to-End Tests
- Test complete SOS flow from trigger to resolution
- Test location sharing from start to end
- Test family member accessing shared location
- Test SOS cancellation by user
- Test multiple simultaneous alerts (different users)

## API Endpoints (Future Implementation)

The following REST endpoints will be implemented in task 9.6:

### SOS Alerts
- `POST /users/{userId}/sos` - Trigger SOS alert
- `GET /users/{userId}/sos` - Get all SOS alerts
- `GET /users/{userId}/sos/active` - Get active SOS alert
- `POST /users/{userId}/sos/{alertId}/cancel` - Cancel SOS alert
- `POST /admin/sos/{alertId}/resolve` - Resolve SOS alert (admin)

### Location Sharing
- `POST /users/{userId}/location-share` - Start location sharing
- `PUT /users/{userId}/location-share/{shareId}` - Update location
- `GET /users/{userId}/location-share/active` - Get active share
- `GET /location-share/{token}` - Access shared location (public)
- `DELETE /users/{userId}/location-share/{shareId}` - End sharing

## Requirements Satisfied

This implementation satisfies the following requirements from the specification:

- **Requirement 9.3**: SOS emergency button functionality during active rides
- **Requirement 9.4**: Real-time ride tracking sharing with designated contacts
- **Requirement 9.5**: Emergency alert capabilities for immediate assistance

## Notes

- This is a **foundation implementation** providing core functionality
- Notification systems are placeholder methods ready for integration
- Real-time updates will require WebSocket implementation
- Location tracking should be optimized for battery efficiency
- All operations are logged for security and audit purposes
