# Technical Specifications: Corporate Ride-Sharing Application

## 1. Introduction

### 1.1 Purpose
This Technical Specifications document provides detailed technical requirements, specifications, and implementation guidelines for the Corporate Ride-Sharing Application. It serves as a bridge between the high-level design and implementation, offering developers concrete specifications for implementation.

### 1.2 Scope
This document covers API specifications, database schemas, integration specifications, performance requirements, and security implementations. It provides the technical foundation for all components outlined in the Business Requirements Document and High-Level Design.

### 1.3 References
- Business Requirements Document v1.0
- High-Level Design Document v1.0
- AWS Technical Architecture v1.0
- Data Flow Diagrams v1.0
- UI/UX Wireframes v1.0
- User Flow Document v1.0
- Implementation Plan v1.0
- Test Plan v1.0

## 2. API Specifications

### 2.1 API Design Principles

The Corporate Ride-Sharing Application will implement a RESTful API architecture with the following principles:
- Resource-oriented design
- Standard HTTP methods and response codes
- JWT-based authentication
- Versioned endpoints
- Comprehensive error handling
- Rate limiting for security

### 2.2 API Authentication and Authorization

#### 2.2.1 Authentication Endpoints

**Corporate SSO Authentication**
```
POST /api/v1/auth/corporate-sso
Request:
{
 "email": "string", // Corporate email address
 "redirectUrl": "string" // URL to redirect after successful authentication
}
Response:
{
 "authUrl": "string", // URL to SSO provider
 "state": "string" // State parameter for validation
}
```

**OTP Verification**
```
POST /api/v1/auth/verify-otp
Request:
{
 "email": "string", // Corporate email address
 "otp": "string" // One-time password
}
Response:
{
 "accessToken": "string",
 "refreshToken": "string",
 "expiresIn": "number"
}
```

**Token Refresh**
```
POST /api/v1/auth/refresh
Request:
{
 "refreshToken": "string"
}
Response:
{
 "accessToken": "string",
 "refreshToken": "string",
 "expiresIn": "number"
}
```

### 2.3 User Management APIs

#### 2.3.1 Driver Registration

**Register Driver**
```
POST /api/v1/users/drivers
Request:
{
 "startLocation": {
 "latitude": "number",
 "longitude": "number",
 "address": "string"
 },
 "endLocation": {
 "latitude": "number",
 "longitude": "number",
 "address": "string"
 },
 "maxDetourDistance": "number", // in kilometers
 "vehicle": {
 "make": "string",
 "model": "string",
 "color": "string",
 "licensePlate": "string",
 "capacity": "number"
 },
 "drivingLicense": {
 "number": "string",
 "expiryDate": "string" // ISO 8601 format
 },
 "genderPreferences": {
 "acceptOnlyFemaleRiders": "boolean",
 "acceptOneFemaleSeparately": "boolean"
 }
}
Response:
{
 "userId": "string",
 "driverId": "string",
 "status": "string", // "PENDING_VERIFICATION", "ACTIVE"
 "createdAt": "string" // ISO 8601 format
}
```

#### 2.3.2 Rider Registration

**Register Rider**
```
POST /api/v1/users/riders
Request:
{
 "startLocation": {
 "latitude": "number",
 "longitude": "number",
 "address": "string"
 },
 "endLocation": {
 "latitude": "number",
 "longitude": "number",
 "address": "string"
 },
 "preferredTravelTimes": {
 "morningDepartureTime": "string", // ISO 8601 time format
 "eveningDepartureTime": "string" // ISO 8601 time format
 },
 "genderPreferences": {
 "preferFemaleDrivers": "boolean"
 }
}
Response:
{
 "userId": "string",
 "riderId": "string",
 "status": "string", // "ACTIVE"
 "createdAt": "string" // ISO 8601 format
}
```

### 2.4 Ride Management APIs

#### 2.4.1 Driver Ride Offering

**Create Ride Offering**
```
POST /api/v1/rides/offerings
Request:
{
 "departureTime": "string", // ISO 8601 format
 "startLocation": { // Optional, default to profile
 "latitude": "number",
 "longitude": "number",
 "address": "string"
 },
 "endLocation": { // Optional, default to profile
 "latitude": "number",
 "longitude": "number",
 "address": "string"
 },
 "availableSeats": "number",
 "maxDetourDistance": "number" // in kilometers, optional
}
Response:
{
 "offeringId": "string",
 "status": "string", // "OPEN", "CLOSED", "IN_PROGRESS", "COMPLETED", "CANCELLED"
 "departureTime": "string", // ISO 8601 format
 "createdAt": "string" // ISO 8601 format
}
```

**Update Ride Offering**
```
PUT /api/v1/rides/offerings/{offeringId}
Request:
{
 "departureTime": "string", // ISO 8601 format
 "availableSeats": "number",
 "maxDetourDistance": "number", // in kilometers
 "status": "string" // "OPEN", "CLOSED"
}
Response:
{
 "offeringId": "string",
 "status": "string",
 "updatedAt": "string" // ISO 8601 format
}
```

#### 2.4.2 Rider Ride Booking

**Search Available Rides**
```
GET /api/v1/rides/search?departureTime={time}&startLat={latitude}&startLng={longitude}&endLat={latitude}&endLng={longitude}&radius={meters}
Response:
{
 "rides": [
 {
 "offeringId": "string",
 "driverId": "string",
 "driverName": "string",
 "driverRating": "number",
 "vehicleDetails": {
 "make": "string",
 "model": "string",
 "color": "string",
 "licensePlate": "string"
 },
 "departureTime": "string", // ISO 8601 format
 "estimatedArrivalTime": "string", // ISO 8601 format
 "pickupDistance": "number", // Distance from rider's start in meters
 "dropoffDistance": "number", // Distance to rider's destination in meters
 "estimatedFare": "number",
 "availableSeats": "number"
 }
 ],
 "totalResults": "number",
 "page": "number",
 "pageSize": "number"
}
```

**Book a Ride**
```
POST /api/v1/rides/bookings
Request:
{
 "offeringId": "string",
 "pickupLocation": {
 "latitude": "number",
 "longitude": "number",
 "address": "string"
 },
 "dropoffLocation": {
 "latitude": "number",
 "longitude": "number",
 "address": "string"
 }
}
Response:
{
 "bookingId": "string",
 "status": "string", // "PENDING", "CONFIRMED", "CANCELLED"
 "offeringId": "string",
 "pickupLocation": {
 "latitude": "number",
 "longitude": "number",
 "address": "string"
 },
 "dropoffLocation": {
 "latitude": "number",
 "longitude": "number",
 "address": "string"
 },
 "estimatedFare": "number",
 "createdAt": "string" // ISO 8601 format
}
```

### 2.5 Ride Execution APIs

#### 2.5.1 Driver Ride Management

**Start Ride**
```
POST /api/v1/rides/offerings/{offeringId}/start
Response:
{
 "offeringId": "string",
 "status": "string", // "IN_PROGRESS"
 "startTime": "string", // ISO 8601 format
 "riders": [
 {
 "riderId": "string",
 "riderName": "string",
 "bookingId": "string",
 "pickupLocation": {
 "latitude": "number",
 "longitude": "number",
 "address": "string"
 },
 "dropoffLocation": {
 "latitude": "number",
 "longitude": "number",
 "address": "string"
 },
 "status": "string" // "WAITING", "PICKED_UP", "DROPPED_OFF", "NO_SHOW"
 }
 ]
}
```

**Mark Rider Pickup**
```
POST /api/v1/rides/offerings/{offeringId}/bookings/{bookingId}/pickup
Response:
{
 "bookingId": "string",
 "status": "string", // "PICKED_UP"
 "pickupTime": "string" // ISO 8601 format
}
```

**Mark Rider No-Show**
```
POST /api/v1/rides/offerings/{offeringId}/bookings/{bookingId}/no-show
Response:
{
 "bookingId": "string",
 "status": "string", // "NO_SHOW"
 "noShowTime": "string", // ISO 8601 format
 "noShowFee": "number"
}
```

**Mark Rider Dropoff**
```
POST /api/v1/rides/offerings/{offeringId}/bookings/{bookingId}/dropoff
Response:
{
 "bookingId": "string",
 "status": "string", // "COMPLETED"
 "dropoffTime": "string", // ISO 8601 format
 "finalFare": "number"
}
```

**Complete Ride**
```
POST /api/v1/rides/offerings/{offeringId}/complete
Response:
{
 "offeringId": "string",
 "status": "string", // "COMPLETED"
 "completionTime": "string", // ISO 8601 format
 "totalFare": "number",
 "totalDistance": "number", // in kilometers
 "totalDuration": "number" // in minutes
}
```

#### 2.5.2 Real-time Location Updates

**Update Driver Location**
```
POST /api/v1/rides/offerings/{offeringId}/location
Request:
{
 "latitude": "number",
 "longitude": "number",
 "timestamp": "string" // ISO 8601 format
}
Response:
{
 "offeringId": "string",
 "received": "boolean"
}
```

**Get Driver Location**
```
GET /api/v1/rides/offerings/{offeringId}/location
Response:
{
 "offeringId": "string",
 "driverId": "string",
 "location": {
 "latitude": "number",
 "longitude": "number"
 },
 "timestamp": "string", // ISO 8601 format
 "estimatedArrival": {
 "pickupEta": "string", // ISO 8601 format
 "destinationEta": "string" // ISO 8601 format
 }
}
```

### 2.6 Payment APIs

#### 2.6.1 Wallet Management

**Get Wallet Balance**
```
GET /api/v1/payments/wallet
Response:
{
 "balance": "number",
 "currency": "string" // e.g., "INR"
}
```

**Add Funds to Wallet**
```
POST /api/v1/payments/wallet/add
Request:
{
 "amount": "number",
 "paymentMethod": "string", // "CREDIT_CARD", "BANK_ACCOUNT", "UPI"
 "paymentDetails": {
 // Payment method specific details
 }
}
Response:
{
 "transactionId": "string",
 "status": "string", // "PENDING", "COMPLETED", "FAILED"
 "amount": "number",
 "newBalance": "number",
 "transactionTime": "string" // ISO 8601 format
}
```

**Transfer to Bank Account**
```
POST /api/v1/payments/wallet/transfer
Request:
{
 "amount": "number",
 "bankAccount": {
 "accountNumber": "string",
 "ifscCode": "string",
 "accountName": "string"
 }
}
Response:
{
 "transferId": "string",
 "status": "string", // "INITIATED", "COMPLETED", "FAILED"
 "amount": "number",
 "estimatedCompletionTime": "string" // ISO 8601 format
}
```

#### 2.6.2 Fare Calculation

**Calculate Estimated Fare**
```
POST /api/v1/payments/calculate-fare
Request:
{
 "pickupLocation": {
 "latitude": "number",
 "longitude": "number"
 },
 "dropoffLocation": {
 "latitude": "number",
 "longitude": "number"
 },
 "time": "string", // ISO 8601 format
 "detourDistance": "number" // in kilometers
}
Response:
{
 "baseFare": "number",
 "distanceFare": "number",
 "timeFare": "number",
 "detourFare": "number",
 "peakTimeFactor": "number",
 "weatherFactor": "number",
 "trafficFactor": "number",
 "totalFare": "number",
 "currency": "string" // e.g., "INR"
}
```

### 2.7 Feedback and Rating APIs

**Submit Driver Rating**
```
POST /api/v1/feedback/drivers/{driverId}
Request:
{
 "bookingId": "string",
 "rating": "number", // 1-5
 "comments": "string",
 "isFavorite": "boolean"
}
Response:
{
 "feedbackId": "string",
 "status": "string" // "SUBMITTED"
}
```

**Submit Rider Rating**
```
POST /api/v1/feedback/riders/{riderId}
Request:
{
 "bookingId": "string",
 "rating": "number", // 1-5
 "comments": "string",
 "behaviors": [
 "string" // e.g., "PUNCTUAL", "POLITE", "DISRUPTIVE"
 ]
}
Response:
{
 "feedbackId": "string",
 "status": "string" // "SUBMITTED"
}
```

**Report Incident**
```
POST /api/v1/feedback/incidents
Request:
{
 "bookingId": "string",
 "incidentType": "string", // "SAFETY", "BEHAVIOR", "VEHICLE", "OTHER"
 "description": "string",
 "severity": "string", // "LOW", "MEDIUM", "HIGH", "CRITICAL"
 "attachments": [
 {
 "fileName": "string",
 "fileType": "string",
 "fileContent": "string" // base64 encoded
 }
 ]
}
Response:
{
 "incidentId": "string",
 "status": "string", // "REPORTED", "UNDER_INVESTIGATION", "RESOLVED"
 "expectedResolutionTime": "string" // ISO 8601 format
}
```

### 2.8 Safety Feature APIs

**Trigger SOS Alert**
```
POST /api/v1/safety/sos
Request:
{
 "bookingId": "string",
 "location": {
 "latitude": "number",
 "longitude": "number"
 },
 "alertType": "string" // "EMERGENCY", "SECURITY_CONCERN"
}
Response:
{
 "sosId": "string",
 "status": "string", // "TRIGGERED"
 "expectedResponseTime": "string" // ISO 8601 format
}
```

**Share Ride with Contact**
```
POST /api/v1/safety/share-ride
Request:
{
 "bookingId": "string",
 "contactName": "string",
 "contactPhone": "string",
 "contactEmail": "string",
 "shareLocation": "boolean",
 "shareDriverDetails": "boolean",
 "shareEta": "boolean"
}
Response:
{
 "shareId": "string",
 "trackingUrl": "string",
 "expiresAt": "string" // ISO 8601 format
}
```

## 3. Database Schema Definitions

### 3.1 User Schema

#### 3.1.1 Users Table
```sql
CREATE TABLE users (
 user_id VARCHAR(36) PRIMARY KEY,
 corporate_email VARCHAR(100) UNIQUE NOT NULL,
 phone_number VARCHAR(20) UNIQUE,
 first_name VARCHAR(50) NOT NULL,
 last_name VARCHAR(50) NOT NULL,
 gender ENUM('MALE', 'FEMALE', 'OTHER', 'PREFER_NOT_TO_SAY') NOT NULL,
 profile_image_url VARCHAR(255),
 department VARCHAR(100),
 employee_id VARCHAR(50),
 status ENUM('ACTIVE', 'INACTIVE', 'SUSPENDED', 'DELETED') NOT NULL DEFAULT 'ACTIVE',
 created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
 updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
 last_login TIMESTAMP,
 auth_provider ENUM('CORPORATE_SSO', 'EMAIL_PASSWORD') NOT NULL,
 INDEX idx_corporate_email (corporate_email),
 INDEX idx_status (status)
);
```

#### 3.1.2 Drivers Table
```sql
CREATE TABLE drivers (
 driver_id VARCHAR(36) PRIMARY KEY,
 user_id VARCHAR(36) NOT NULL,
 license_number VARCHAR(50) NOT NULL,
 license_expiry_date DATE NOT NULL,
 license_verification_status ENUM('PENDING', 'VERIFIED', 'REJECTED') NOT NULL,
 max_detour_distance DECIMAL(5,2) NOT NULL DEFAULT 0.5,
 total_rides INT NOT NULL DEFAULT 0,
 average_rating DECIMAL(3,2),
 status ENUM('ACTIVE', 'INACTIVE', 'SUSPENDED') NOT NULL DEFAULT 'ACTIVE',
 gender_preference_female_only BOOLEAN NOT NULL DEFAULT FALSE,
 created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
 updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
 FOREIGN KEY (user_id) REFERENCES users(user_id),
 INDEX idx_status (status)
);
```

#### 3.1.3 Vehicles Table
```sql
CREATE TABLE vehicles (
 vehicle_id VARCHAR(36) PRIMARY KEY,
 driver_id VARCHAR(36) NOT NULL,
 make VARCHAR(50) NOT NULL,
 model VARCHAR(50) NOT NULL,
 year INT NOT NULL,
 color VARCHAR(30) NOT NULL,
 license_plate VARCHAR(20) NOT NULL,
 capacity INT NOT NULL,
 is_active BOOLEAN NOT NULL DEFAULT TRUE,
 created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
 updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
 FOREIGN KEY (driver_id) REFERENCES drivers(driver_id),
 INDEX idx_license_plate (license_plate)
);
```

#### 3.1.4 Riders Table
```sql
CREATE TABLE riders (
 rider_id VARCHAR(36) PRIMARY KEY,
 user_id VARCHAR(36) NOT NULL,
 preferred_morning_time TIME,
 preferred_evening_time TIME,
 total_rides INT NOT NULL DEFAULT 0,
 average_rating DECIMAL(3,2),
 status ENUM('ACTIVE', 'INACTIVE', 'SUSPENDED') NOT NULL DEFAULT 'ACTIVE',
 gender_preference_female_drivers BOOLEAN NOT NULL DEFAULT FALSE,
 created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
 updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
 FOREIGN KEY (user_id) REFERENCES users(user_id),
 INDEX idx_status (status)
);
```

### 3.2 Location and Routes Schema

#### 3.2.1 Locations Table
```sql
CREATE TABLE locations (
 location_id VARCHAR(36) PRIMARY KEY,
 latitude DECIMAL(10,8) NOT NULL,
 longitude DECIMAL(11,8) NOT NULL,
 address TEXT,
 address_type ENUM('HOME', 'WORK', 'OTHER') NOT NULL,
 user_id VARCHAR(36) NOT NULL,
 is_default BOOLEAN NOT NULL DEFAULT FALSE,
 created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
 updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
 FOREIGN KEY (user_id) REFERENCES users(user_id),
 INDEX idx_user_location (user_id, address_type),
 SPATIAL INDEX idx_spatial (latitude, longitude)
);
```

#### 3.2.2 Driver Routes Table
```sql
CREATE TABLE driver_routes (
 route_id VARCHAR(36) PRIMARY KEY,
 driver_id VARCHAR(36) NOT NULL,
 start_location_id VARCHAR(36) NOT NULL,
 end_location_id VARCHAR(36) NOT NULL,
 is_active BOOLEAN NOT NULL DEFAULT TRUE,
 created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
 updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
 FOREIGN KEY (driver_id) REFERENCES drivers(driver_id),
 FOREIGN KEY (start_location_id) REFERENCES locations(location_id),
 FOREIGN KEY (end_location_id) REFERENCES locations(location_id),
 INDEX idx_driver_active (driver_id, is_active)
);
```

### 3.3 Ride Management Schema

#### 3.3.1 Ride Offerings Table
```sql
CREATE TABLE ride_offerings (
 offering_id VARCHAR(36) PRIMARY KEY,
 driver_id VARCHAR(36) NOT NULL,
 vehicle_id VARCHAR(36) NOT NULL,
 departure_time DATETIME NOT NULL,
 start_location_id VARCHAR(36) NOT NULL,
 end_location_id VARCHAR(36) NOT NULL,
 available_seats INT NOT NULL,
 max_detour_distance DECIMAL(5,2) NOT NULL,
 status ENUM('SCHEDULED', 'IN_PROGRESS', 'COMPLETED', 'CANCELLED') NOT NULL,
 cancellation_reason TEXT,
 created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
 updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
 FOREIGN KEY (driver_id) REFERENCES drivers(driver_id),
 FOREIGN KEY (vehicle_id) REFERENCES vehicles(vehicle_id),
 FOREIGN KEY (start_location_id) REFERENCES locations(location_id),
 FOREIGN KEY (end_location_id) REFERENCES locations(location_id),
 INDEX idx_departure_time (departure_time),
 INDEX idx_status (status)
);
```

#### 3.3.2 Ride Bookings Table
```sql
CREATE TABLE ride_bookings (
 booking_id VARCHAR(36) PRIMARY KEY,
 offering_id VARCHAR(36) NOT NULL,
 rider_id VARCHAR(36) NOT NULL,
 pickup_location_id VARCHAR(36) NOT NULL,
 dropoff_location_id VARCHAR(36) NOT NULL,
 status ENUM('PENDING', 'CONFIRMED', 'PICKED_UP', 'COMPLETED', 'CANCELLED', 'NO_SHOW') NOT NULL,
 estimated_pickup_time DATETIME,
 actual_pickup_time DATETIME,
 estimated_dropoff_time DATETIME,
 actual_dropoff_time DATETIME,
 waiting_start_time DATETIME,
 estimated_fare DECIMAL(10,2),
 final_fare DECIMAL(10,2),
 cancellation_reason TEXT,
 cancellation_fee DECIMAL(10,2),
 no_show_fee DECIMAL(10,2),
 created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
 updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
 FOREIGN KEY (offering_id) REFERENCES ride_offerings(offering_id),
 FOREIGN KEY (rider_id) REFERENCES riders(rider_id),
 FOREIGN KEY (pickup_location_id) REFERENCES locations(location_id),
 FOREIGN KEY (dropoff_location_id) REFERENCES locations(location_id),
 INDEX idx_offering_id (offering_id),
 INDEX idx_rider_id (rider_id),
 INDEX idx_status (status)
);
```

#### 3.3.3 Location Tracking Table
```sql
CREATE TABLE location_tracking (
 tracking_id VARCHAR(36) PRIMARY KEY,
 offering_id VARCHAR(36) NOT NULL,
 latitude DECIMAL(10,8) NOT NULL,
 longitude DECIMAL(11,8) NOT NULL,
 timestamp DATETIME NOT NULL,
 speed DECIMAL(5,2),
 heading INT,
 created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
 FOREIGN KEY (offering_id) REFERENCES ride_offerings(offering_id),
 INDEX idx_offering_timestamp (offering_id, timestamp),
 SPATIAL INDEX idx_spatial (latitude, longitude)
);
```

### 3.4 Payment Schema

#### 3.4.1 Wallets Table
```sql
CREATE TABLE wallets (
 wallet_id VARCHAR(36) PRIMARY KEY,
 user_id VARCHAR(36) NOT NULL UNIQUE,
 balance DECIMAL(10,2) NOT NULL DEFAULT 0.00,
 currency VARCHAR(3) NOT NULL DEFAULT 'INR',
 status ENUM('ACTIVE', 'FROZEN', 'CLOSED') NOT NULL DEFAULT 'ACTIVE',
 created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
 updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
 FOREIGN KEY (user_id) REFERENCES users(user_id),
 INDEX idx_user_id (user_id),
 INDEX idx_status (status)
);
```

#### 3.4.2 Payment Methods Table
```sql
CREATE TABLE payment_methods (
 payment_method_id VARCHAR(36) PRIMARY KEY,
 user_id VARCHAR(36) NOT NULL,
 payment_type ENUM('CREDIT_CARD', 'DEBIT_CARD', 'UPI', 'BANK_ACCOUNT', 'WALLET') NOT NULL,
 is_default BOOLEAN NOT NULL DEFAULT FALSE,
 status ENUM('ACTIVE', 'EXPIRED', 'REMOVED') NOT NULL DEFAULT 'ACTIVE',
 last_four_digits VARCHAR(4),
 expiry_date DATE,
 card_brand VARCHAR(20),
 bank_name VARCHAR(100),
 account_holder_name VARCHAR(100),
 created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
 updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
 FOREIGN KEY (user_id) REFERENCES users(user_id),
 INDEX idx_user_payment (user_id, payment_type, status)
);
```

#### 3.4.3 Transactions Table
```sql
CREATE TABLE transactions (
 transaction_id VARCHAR(36) PRIMARY KEY,
 wallet_id VARCHAR(36) NOT NULL,
 amount DECIMAL(10,2) NOT NULL,
 currency VARCHAR(3) NOT NULL DEFAULT 'INR',
 transaction_type ENUM('CREDIT', 'DEBIT', 'REFUND', 'FEE') NOT NULL,
 status ENUM('PENDING', 'COMPLETED', 'FAILED', 'REVERSED') NOT NULL,
 reference_id VARCHAR(36),
 reference_type ENUM('BOOKING', 'TOPUP', 'WITHDRAWAL', 'CANCELLATION', 'NO_SHOW', 'ADJUSTMENT'),
 payment_method_id VARCHAR(36),
 description TEXT,
 created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
 updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
 FOREIGN KEY (wallet_id) REFERENCES wallets(wallet_id),
 FOREIGN KEY (payment_method_id) REFERENCES payment_methods(payment_method_id),
 INDEX idx_wallet_id (wallet_id),
 INDEX idx_reference (reference_type, reference_id),
 INDEX idx_created_at (created_at)
);
```

### 3.5 Feedback and Rating Schema

#### 3.5.1 Driver Ratings Table
```sql
CREATE TABLE driver_ratings (
 rating_id VARCHAR(36) PRIMARY KEY,
 booking_id VARCHAR(36) NOT NULL UNIQUE,
 driver_id VARCHAR(36) NOT NULL,
 rider_id VARCHAR(36) NOT NULL,
 rating TINYINT NOT NULL CHECK (rating BETWEEN 1 AND 5),
 comments TEXT,
 is_favorite BOOLEAN NOT NULL DEFAULT FALSE,
 created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
 FOREIGN KEY (booking_id) REFERENCES ride_bookings(booking_id),
 FOREIGN KEY (driver_id) REFERENCES drivers(driver_id),
 FOREIGN KEY (rider_id) REFERENCES riders(rider_id),
 INDEX idx_driver_id (driver_id),
 INDEX idx_rider_id (rider_id)
);
```

#### 3.5.2 Rider Ratings Table
```sql
CREATE TABLE rider_ratings (
 rating_id VARCHAR(36) PRIMARY KEY,
 booking_id VARCHAR(36) NOT NULL UNIQUE,
 rider_id VARCHAR(36) NOT NULL,
 driver_id VARCHAR(36) NOT NULL,
 rating TINYINT NOT NULL CHECK (rating BETWEEN 1 AND 5),
 comments TEXT,
 behavior_tags JSON,
 created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
 FOREIGN KEY (booking_id) REFERENCES ride_bookings(booking_id),
 FOREIGN KEY (rider_id) REFERENCES riders(rider_id),
 FOREIGN KEY (driver_id) REFERENCES drivers(driver_id),
 INDEX idx_rider_id (rider_id),
 INDEX idx_driver_id (driver_id)
);
```

#### 3.5.3 Incident Reports Table
```sql
CREATE TABLE incident_reports (
 incident_id VARCHAR(36) PRIMARY KEY,
 booking_id VARCHAR(36) NOT NULL,
 reported_by_user_id VARCHAR(36) NOT NULL,
 reported_against_user_id VARCHAR(36),
 incident_type ENUM('SAFETY', 'BEHAVIOR', 'VEHICLE', 'DAMAGE', 'OTHER') NOT NULL,
 severity ENUM('LOW', 'MEDIUM', 'HIGH', 'CRITICAL') NOT NULL,
 description TEXT NOT NULL,
 status ENUM('REPORTED', 'UNDER_INVESTIGATION', 'RESOLVED', 'DISMISSED') NOT NULL DEFAULT 'REPORTED',
 resolution_notes TEXT,
 investigation_owner VARCHAR(100),
 created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
 updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
 FOREIGN KEY (booking_id) REFERENCES ride_bookings(booking_id),
 FOREIGN KEY (reported_by_user_id) REFERENCES users(user_id),
 FOREIGN KEY (reported_against_user_id) REFERENCES users(user_id),
 INDEX idx_status (status),
 INDEX idx_booking_id (booking_id),
 INDEX idx_severity (severity)
);
```

### 3.6 Safety Feature Schema

#### 3.6.1 SOS Alerts Table
```sql
CREATE TABLE sos_alerts (
 sos_id VARCHAR(36) PRIMARY KEY,
 user_id VARCHAR(36) NOT NULL,
 booking_id VARCHAR(36) NOT NULL,
 latitude DECIMAL(10,8) NOT NULL,
 longitude DECIMAL(11,8) NOT NULL,
 alert_type ENUM('EMERGENCY', 'SECURITY_CONCERN') NOT NULL,
 status ENUM('TRIGGERED', 'ACKNOWLEDGED', 'RESOLVED', 'FALSE_ALARM') NOT NULL DEFAULT 'TRIGGERED',
 resolved_by VARCHAR(100),
 resolution_notes TEXT,
 created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
 updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
 FOREIGN KEY (user_id) REFERENCES users(user_id),
 FOREIGN KEY (booking_id) REFERENCES ride_bookings(booking_id),
 INDEX idx_status (status),
 INDEX idx_user_id (user_id),
 SPATIAL INDEX idx_spatial (latitude, longitude)
);
```

#### 3.6.2 Ride Shares Table
```sql
CREATE TABLE ride_shares (
 share_id VARCHAR(36) PRIMARY KEY,
 booking_id VARCHAR(36) NOT NULL,
 user_id VARCHAR(36) NOT NULL,
 contact_name VARCHAR(100) NOT NULL,
 contact_phone VARCHAR(20),
 contact_email VARCHAR(100),
 tracking_code VARCHAR(10) NOT NULL,
 share_location BOOLEAN NOT NULL DEFAULT TRUE,
 share_driver_details BOOLEAN NOT NULL DEFAULT TRUE,
 share_eta BOOLEAN NOT NULL DEFAULT TRUE,
 expires_at DATETIME NOT NULL,
 created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
 FOREIGN KEY (booking_id) REFERENCES ride_bookings(booking_id),
 FOREIGN KEY (user_id) REFERENCES users(user_id),
 INDEX idx_tracking_code (tracking_code),
 INDEX idx_expires_at (expires_at)
);
```

### 3.7 System Management Schema

#### 3.7.1 System Parameters Table
```sql
CREATE TABLE system_parameters (
 param_id VARCHAR(36) PRIMARY KEY,
 param_name VARCHAR(50) NOT NULL UNIQUE,
 param_value TEXT NOT NULL,
 description TEXT,
 data_type ENUM('STRING', 'INTEGER', 'DECIMAL', 'BOOLEAN', 'JSON') NOT NULL,
 is_encrypted BOOLEAN NOT NULL DEFAULT FALSE,
 created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
 updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
 INDEX idx_param_name (param_name)
);
```

#### 3.7.2 Audit Logs Table
```sql
CREATE TABLE audit_logs (
 log_id VARCHAR(36) PRIMARY KEY,
 user_id VARCHAR(36),
 action VARCHAR(100) NOT NULL,
 entity_type VARCHAR(50) NOT NULL,
 entity_id VARCHAR(36) NOT NULL,
 old_value JSON,
 new_value JSON,
 ip_address VARCHAR(45),
 user_agent TEXT,
 created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
 INDEX idx_user_id (user_id),
 INDEX idx_entity (entity_type, entity_id),
 INDEX idx_created_at (created_at)
);
```

## 4. Third-Party Integration Specifications

### 4.1 Corporate Authentication Integration

#### 4.1.1 Corporate SSO Integration
- **Protocol**: OAuth 2.0 / SAML 2.0
- **Authentication Flow**: Authorization Code Flow
- **Required Claims**:
 - User ID (corporate email)
 - First Name
 - Last Name
 - Department
 - Employee ID
 - Email Verification Status
- **Integration Points**:
 - Login endpoint
 - Token validation endpoint
 - User information endpoint
 - Logout endpoint
- **Refresh Policy**: Tokens expire after 8 hours; refresh tokens valid for 30 days

#### 4.1.2 Two-Factor Authentication
- **OTP Delivery Methods**: Corporate email, SMS to registered mobile
- **OTP Validity**: 5 minutes
- **Retry Limits**: 3 attempts before lockout
- **Lockout Duration**: 15 minutes

### 4.2 Mapping and Location Services

#### 4.2.1 Geocoding Service
- **Provider**: Google Maps API / Mapbox API
- **Functions**:
 - Forward geocoding (address to coordinates)
 - Reverse geocoding (coordinates to address)
 - Place autocomplete
- **Request Limits**: 10,000 requests per day
- **Caching Strategy**: Cache frequent locations for 30 days

#### 4.2.2 Route Calculation Service
- **Provider**: Google Maps Directions API / Mapbox Directions API
- **Functions**:
 - Optimal route calculation
 - Distance and duration estimation
 - Traffic-aware routing
 - Multi-stop route optimization
- **Request Format**:
 - Origin coordinates
 - Destination coordinates
 - Waypoints (for multiple pickups)
 - Departure time
 - Traffic model preference
- **Response Format**:
 - Route geometry (encoded polyline)
 - Step-by-step directions
 - Distance (meters)
 - Duration (seconds)
 - Traffic conditions

#### 4.2.3 Real-time Location Tracking
- **Provider**: Custom WebSocket solution with AWS API Gateway
- **Update Frequency**: Every 5 seconds when in active ride
- **Data Points**:
 - Latitude and longitude
 - Timestamp
 - Speed (if available)
 - Heading (if available)
- **Security**:
 - JWT authentication for WebSocket connections
 - Data encryption for location updates

### 4.3 Payment Gateway Integration

#### 4.3.1 Payment Processing
- **Provider**: Razorpay / Stripe
- **Supported Payment Methods**:
 - Credit/Debit Cards
 - UPI
 - NetBanking
 - Wallets
- **Integration Type**: Server-to-server API with client-side tokenization
- **Authentication**: API key and webhook signature verification
- **Currency**: INR (primary), with multi-currency support
- **Security Compliance**:
 - PCI-DSS Level 1
 - Tokenization for stored payment methods
 - 3D Secure for card transactions

#### 4.3.2 Bank Transfer API
- **Provider**: Banking Partner API
- **Functions**:
 - Funds transfer to driver bank accounts
 - Balance inquiry
 - Transaction status verification
- **Integration Type**: Server-to-server API with mutual TLS
- **Processing Time**: Same-day transfers initiated before 3 PM IST
- **Minimum Transfer Amount**: ₹100

### 4.4 Notification Services

#### 4.4.1 Push Notification Service
- **Provider**: Firebase Cloud Messaging (FCM)
- **Platform Support**:
 - Android
 - iOS
- **Notification Types**:
 - Ride updates
 - Payment confirmations
 - Safety alerts
 - System announcements
- **Payload Structure**:
 - Title
 - Body
 - Data payload (JSON)
 - Deep link URL
 - Priority level
- **Delivery Tracking**: Success/failure metrics for critical notifications

#### 4.4.2 Email Notification Service
- **Provider**: Amazon SES / SendGrid
- **Email Templates**:
 - Registration confirmation
 - OTP verification
 - Ride confirmations
 - Receipts
 - Critical alerts
- **Template Format**: Responsive HTML with plain text fallback
- **Tracking**: Open rates, click rates for marketing communications

#### 4.4.3 SMS Gateway
- **Provider**: Twilio / MSG91
- **Use Cases**:
 - OTP delivery
 - Critical ride updates
 - Emergency notifications
- **Message Format**: Unicode support for multiple languages
- **Delivery Reports**: Required for critical communications

### 4.5 Weather and Traffic API

#### 4.5.1 Weather Service
- **Provider**: OpenWeather API / AccuWeather
- **Data Points**:
 - Current weather conditions
 - Precipitation probability
 - Visibility
 - Wind speed
- **Update Frequency**: Hourly updates
- **Geographic Resolution**: City level

#### 4.5.2 Traffic Conditions Service
- **Provider**: Google Maps / TomTom Traffic API
- **Data Points**:
 - Traffic density
 - Incidents (accidents, closures)
 - Historical traffic patterns
 - Real-time traffic flow
- **Update Frequency**: Every 5 minutes
- **Integration**: Factor into fare calculation and ETA estimates

## 5. Security Implementation Specifications

### 5.1 Authentication and Authorization

#### 5.1.1 Authentication Mechanisms
- **Primary Authentication**: Corporate SSO integration with JWT token
- **Secondary Authentication**: OTP verification for sensitive actions
- **Token Management**:
 - Access tokens: 8-hour validity
 - Refresh tokens: 30-day validity, single-use
 - Token rotation policy
 - Token revocation on password change or security event

#### 5.1.2 Authorization Framework
- **Role-Based Access Control**:
 - Driver role
 - Rider role
 - Admin role (multiple levels)
 - System role (for automated processes)
- **Permission Structure**:
 - Resource-level permissions
 - Action-based permissions (create, read, update, delete)
 - Context-based permissions (own data vs. others' data)
- **Implementation**:
 - JWT claims for role and permission encoding
 - API Gateway authorization lambda for validation
 - Database-level permission enforcement

### 5.2 Data Protection

#### 5.2.1 Data Encryption
- **Data at Rest**:
 - AES-256 encryption for sensitive database fields
 - S3 server-side encryption for documents and media
 - Encrypted EBS volumes
- **Data in Transit**:
 - TLS 1.3 for all API communications
 - Certificate pinning in mobile applications
 - Secure WebSocket protocol (wss://)
- **Key Management**:
 - AWS KMS for encryption key management
 - Key rotation policy (annually)
 - Separate encryption contexts for different data categories

#### 5.2.2 Personal Data Handling
- **PII Classification**:
 - High sensitivity: Government IDs, financial information
 - Medium sensitivity: Contact information, location history
 - Low sensitivity: Preferences, ride history
- **Data Minimization**:
 - Collection limited to necessary information
 - Automated data pruning for expired records
- **Data Retention**:
 - Active ride data: Indefinite (linked to account)
 - Location tracking data: 30 days
 - Payment transaction records: 7 years (regulatory requirement)
 - Chat and communication logs: 90 days

### 5.3 API Security

#### 5.3.1 API Protection Measures
- **Rate Limiting**:
 - Authentication endpoints: 5 requests per minute per IP
 - Standard API endpoints: 60 requests per minute per user
 - Administrative endpoints: 30 requests per minute per user
- **Input Validation**:
 - Server-side validation for all inputs
 - Parameterized queries for database operations
 - JSON Schema validation for request bodies
- **Output Encoding**:
 - Context-appropriate encoding for all outputs
 - Content-Type enforcement

#### 5.3.2 API Gateway Security
- **Request Throttling**: Configured at API Gateway level
- **WAF Integration**: AWS WAF with OWASP Top 10 ruleset
- **Request Validation**: Request parameter validation
- **Usage Plans**: Separate plans for different client types

### 5.4 Mobile Application Security

#### 5.4.1 Mobile Security Controls
- **Certificate Pinning**: Prevent man-in-the-middle attacks
- **App Transport Security**: Enforce HTTPS connections
- **Secure Local Storage**:
 - Keychain/Keystore for sensitive data
 - Encrypted SQLite database
 - No sensitive data in shared preferences/user defaults
- **Jailbreak/Root Detection**:
 - Runtime checks for device integrity
 - Restricted functionality on compromised devices
- **Code Obfuscation**: Prevent reverse engineering
- **Tampering Detection**: Runtime integrity checks

#### 5.4.2 Secure Offline Operations
- **Offline Authentication**:
 - Time-limited offline access
 - Re-authentication required for sensitive operations
- **Data Synchronization**:
 - Secure queue for offline actions
 - Conflict resolution strategy
 - Integrity verification upon reconnection

### 5.5 Incident Response and Monitoring

#### 5.5.1 Security Monitoring
- **Log Management**:
 - Centralized logging with CloudWatch Logs
 - Log retention policy (90 days online, 7 years archived)
 - Log field encryption for sensitive data
- **Alerting**:
 - Unusual authentication patterns
 - Geographic anomalies
 - Rate limit breaches
 - Critical resource access
- **Metrics**:
 - Failed authentication attempts
 - API usage patterns
 - Permission denial events
 - Security control effectiveness

#### 5.5.2 Incident Response
- **Incident Classification**:
 - Severity levels (1-4)
 - Response time SLAs by severity
 - Escalation procedures
- **Response Actions**:
 - Account lockout procedures
 - Forced logout capabilities
 - Region/IP blocking
 - Evidence preservation

## 6. Performance Requirements

### 6.1 API Performance

#### 6.1.1 Response Time Targets
- **Critical Path APIs**:
 - Authentication: <500ms (P95)
 - Ride search: <1s (P95)
 - Booking confirmation: <1.5s (P95)
 - Location updates: <300ms (P95)
- **Background Operations**:
 - Payment processing: <3s (P95)
 - Ride history: <2s (P95)
 - Analytics operations: <5s (P95)
- **Batch Operations**:
 - Report generation: <30s
 - Mass notifications: <5 minutes for full delivery

#### 6.1.2 Throughput Requirements
- **Peak Concurrent Users**: 5,000
- **API Requests per Second**:
 - Authentication APIs: 100 RPS
 - Ride management APIs: 50 RPS
 - Location tracking: 200 RPS
 - Payment APIs: 30 RPS
- **Database Operations**:
 - Read operations: 1,000 per second
 - Write operations: 200 per second

### 6.2 Mobile Application Performance

#### 6.2.1 Application Responsiveness
- **Launch Time**: <3 seconds on mid-range devices
- **Screen Transition**: <300ms
- **Map Loading**: <2 seconds for initial load
- **Interactive Operations**: <100ms response to user input
- **Background Processing**: No UI thread blocking for network operations

#### 6.2.2 Resource Utilization
- **Memory Usage**:
 - Peak: <150MB
 - Background: <50MB
- **Battery Impact**:
 - <5% per hour during active ride tracking
 - <1% per hour when app is in background
- **Network Usage**:
 - <5MB per hour during active ride
 - <500KB per typical ride booking flow
 - Efficient caching for repeat data

### 6.3 Scalability Requirements

#### 6.3.1 User Base Scaling
- **Initial User Base**: 1,000 concurrent users
- **Target Growth**: Support 5x growth within 12 months
- **Scaling Model**: Horizontal scaling for all services
- **Auto-scaling Configuration**:
 - Scale out: When CPU utilization >70% for 5 minutes
 - Scale in: When CPU utilization <30% for 15 minutes

#### 6.3.2 Geographic Scaling
- **Initial Deployment**: Single region
- **Geographic Expansion**:
 - Multi-region capability for global offices
 - Data residency compliance
 - Region-specific configurations

### 6.4 Reliability and Availability

#### 6.4.1 Availability Targets
- **Core Service Availability**: 99.9% (8.76 hours downtime per year)
- **Ride Execution Functions**: 99.95% (4.38 hours downtime per year)
- **Safety Critical Features**: 99.99% (52.6 minutes downtime per year)
- **Maintenance Windows**: Scheduled during non-peak hours, max 4 hours per month

#### 6.4.2 Disaster Recovery
- **Recovery Time Objective (RTO)**: <4 hours
- **Recovery Point Objective (RPO)**: <1 hour
- **Backup Schedule**:
 - Database: Daily full backup, hourly incremental
 - Configuration: Version controlled with history
 - User content: Continuous backup
- **Failover Strategy**:
 - Database: Multi-AZ with automated failover
 - Application: Multi-AZ deployment
 - Critical services: Active-active configuration

## 7. Implementation Guidelines

### 7.1 Coding Standards

#### 7.1.1 Backend Development
- **Languages**: Node.js, Python, Java
- **Framework**: Express.js, FastAPI, Spring Boot
- **API Documentation**: OpenAPI 3.0
- **Code Style**: ESLint/Pylint/CheckStyle enforced
- **Unit Test Coverage**: Minimum 80% code coverage

#### 7.1.2 Mobile Development
- **iOS**:
 - Language: Swift 5+
 - Minimum iOS version: iOS 13.0
 - Architecture: MVVM
 - Dependencies: CocoaPods/Swift Package Manager
- **Android**:
 - Language: Kotlin
 - Minimum API Level: 24 (Android 7.0)
 - Architecture: MVVM
 - Dependencies: Gradle with version constraints

### 7.2 Deployment Guidelines

#### 7.2.1 CI/CD Pipeline
- **Source Control**: Git with feature branch workflow
- **Build Automation**: Jenkins/GitHub Actions
- **Artifact Repository**: AWS ECR for containers, S3 for packages
- **Deployment Automation**: AWS CloudFormation/Terraform
- **Environment Promotion**:
 - Development: Continuous deployment
 - Testing: Daily builds
 - Staging: Release candidate builds
 - Production: Approved releases only

#### 7.2.2 Infrastructure as Code
- **IaC Tool**: Terraform/AWS CDK
- **Environment Consistency**: Identical configuration across environments
- **Configuration Management**: Parameter Store/Secrets Manager
- **Resource Tagging**: Mandatory for cost allocation and ownership

### 7.3 Testing Strategy

#### 7.3.1 Testing Levels
- **Unit Testing**: Component level functionality
- **Integration Testing**: Service interaction validation
- **API Testing**: Contract verification
- **UI Testing**: User interface validation
- **Performance Testing**: Load and stress tests
- **Security Testing**: Vulnerability scanning and penetration testing

#### 7.3.2 Testing Automation
- **Unit Test Framework**: Jest/Pytest/JUnit
- **API Test Framework**: Postman/REST Assured
- **UI Test Framework**: Appium/XCTest/Espresso
- **Performance Test Tools**: JMeter/Gatling
- **Security Test Tools**: OWASP ZAP, SonarQube

### 7.4 Monitoring and Operations

#### 7.4.1 Application Monitoring
- **APM Solution**: New Relic/AppDynamics
- **Log Management**: ELK Stack/CloudWatch Logs
- **Metrics Collection**: Prometheus/CloudWatch
- **Alerting**: PagerDuty integration with escalation policies
- **Dashboards**: Grafana for operational visibility

#### 7.4.2 Operational Procedures
- **Incident Response**: Severity-based SLA
- **Change Management**: Approval workflow for production changes
- **Capacity Planning**: Quarterly review and adjustment
- **Backup Verification**: Monthly restoration testing
- **Security Patching**: Monthly cycle with risk assessment

## 8. Appendices

### 8.1 Glossary of Terms
- **API**: Application Programming Interface
- **JWT**: JSON Web Token
- **OTP**: One-Time Password
- **SSO**: Single Sign-On
- **ETA**: Estimated Time of Arrival
- **UPI**: Unified Payment Interface

### 8.2 Reference Materials
- AWS Well-Architected Framework
- OWASP API Security Top 10
- Mobile App Security Verification Standard
- GDPR and Data Protection Guidelines
- Corporate Security Standards# Technical Specifications: Corporate Ride-Sharing Application

## 1. Introduction

### 1.1 Purpose
This Technical Specifications document provides detailed technical requirements, specifications, and implementation guidelines for the Corporate Ride-Sharing Application. It serves as a bridge between the high-level design and implementation, offering developers concrete specifications for implementation.

### 1.2 Scope
This document covers API specifications, database schemas, integration specifications, performance requirements, and security implementations. It provides the technical foundation for all components outlined in the Business Requirements Document and High-Level Design.

### 1.3 References
- Business Requirements Document v1.0
- High-Level Design Document v1.0
- AWS Technical Architecture v1.0
- Data Flow Diagrams v1.0
- UI/UX Wireframes v1.0
- User Flow Document v1.0
- Implementation Plan v1.0
- Test Plan v1.0

## 2. API Specifications

### 2.1 API Design Principles

The Corporate Ride-Sharing Application will implement a RESTful API architecture with the following principles:
- Resource-oriented design
- Standard HTTP methods and response codes
- JWT-based authentication
- Versioned endpoints
- Comprehensive error handling
- Rate limiting for security

### 2.2 API Authentication and Authorization

#### 2.2.1 Authentication Endpoints

**Corporate SSO Authentication**
```
POST /api/v1/auth/corporate-sso
Request:
{
 "email": "string", // Corporate email address
 "redirectUrl": "string" // URL to redirect after successful authentication
}
Response:
{
 "authUrl": "string", // URL to SSO provider
 "state": "string" // State parameter for validation
}
```

**OTP Verification**
```
POST /api/v1/auth/verify-otp
Request:
{
 "email": "string", // Corporate email address
 "otp": "string" // One-time password
}
Response:
{
 "accessToken": "string",
 "refreshToken": "string",
 "expiresIn": "number"
}
```

**Token Refresh**
```
POST /api/v1/auth/refresh
Request:
{
 "refreshToken": "string"
}
Response:
{
 "accessToken": "string",
 "refreshToken": "string",
 "expiresIn": "number"
}
```

### 2.3 User Management APIs

#### 2.3.1 Driver Registration

**Register Driver**
```
POST /api/v1/users/drivers
Request:
{
 "startLocation": {
 "latitude": "number",
 "longitude": "number",
 "address": "string"
 },
 "endLocation": {
 "latitude": "number",
 "longitude": "number",
 "address": "string"
 },
 "maxDetourDistance": "number", // in kilometers
 "vehicle": {
 "make": "string",
 "model": "string",
 "color": "string",
 "licensePlate": "string",
 "capacity": "number"
 },
 "drivingLicense": {
 "number": "string",
 "expiryDate": "string" // ISO 8601 format
 },
 "genderPreferences": {
 "acceptOnlyFemaleRiders": "boolean",
 "acceptOneFemaleSeparately": "boolean"
 }
}
Response:
{
 "userId": "string",
 "driverId": "string",
 "status": "string", // "PENDING_VERIFICATION", "ACTIVE"
 "createdAt": "string" // ISO 8601 format
}
```

#### 2.3.2 Rider Registration

**Register Rider**
```
POST /api/v1/users/riders
Request:
{
 "startLocation": {
 "latitude": "number",
 "longitude": "number",
 "address": "string"
 },
 "endLocation": {
 "latitude": "number",
 "longitude": "number",
 "address": "string"
 },
 "preferredTravelTimes": {
 "morningDepartureTime": "string", // ISO 8601 time format
 "eveningDepartureTime": "string" // ISO 8601 time format
 },
 "genderPreferences": {
 "preferFemaleDrivers": "boolean"
 }
}
Response:
{
 "userId": "string",
 "riderId": "string",
 "status": "string", // "ACTIVE"
 "createdAt": "string" // ISO 8601 format
}
```

### 2.4 Ride Management APIs

#### 2.4.1 Driver Ride Offering

**Create Ride Offering**
```
POST /api/v1/rides/offerings
Request:
{
 "departureTime": "string", // ISO 8601 format
 "startLocation": { // Optional, default to profile
 "latitude": "number",
 "longitude": "number",
 "address": "string"
 },
 "endLocation": { // Optional, default to profile
 "latitude": "number",
 "longitude": "number",
 "address": "string"
 },
 "availableSeats": "number",
 "maxDetourDistance": "number" // in kilometers, optional
}
Response:
{
 "offeringId": "string",
 "status": "string", // "OPEN", "CLOSED", "IN_PROGRESS", "COMPLETED", "CANCELLED"
 "departureTime": "string", // ISO 8601 format
 "createdAt": "string" // ISO 8601 format
}
```

**Update Ride Offering**
```
PUT /api/v1/rides/offerings/{offeringId}
Request:
{
 "departureTime": "string", // ISO 8601 format
 "availableSeats": "number",
 "maxDetourDistance": "number", // in kilometers
 "status": "string" // "OPEN", "CLOSED"
}
Response:
{
 "offeringId": "string",
 "status": "string",
 "updatedAt": "string" // ISO 8601 format
}
```

#### 2.4.2 Rider Ride Booking

**Search Available Rides**
```
GET /api/v1/rides/search?departureTime={time}&startLat={latitude}&startLng={longitude}&endLat={latitude}&endLng={longitude}&radius={meters}
Response:
{
 "rides": [
 {
 "offeringId": "string",
 "driverId": "string",
 "driverName": "string",
 "driverRating": "number",
 "vehicleDetails": {
 "make": "string",
 "model": "string",
 "color": "string",
 "licensePlate": "string"
 },
 "departureTime": "string", // ISO 8601 format
 "estimatedArrivalTime": "string", // ISO 8601 format
 "pickupDistance": "number", // Distance from rider's start in meters
 "dropoffDistance": "number", // Distance to rider's destination in meters
 "estimatedFare": "number",
 "availableSeats": "number"
 }
 ],
 "totalResults": "number",
 "page": "number",
 "pageSize": "number"
}
```

**Book a Ride**
```
POST /api/v1/rides/bookings
Request:
{
 "offeringId": "string",
 "pickupLocation": {
 "latitude": "number",
 "longitude": "number",
 "address": "string"
 },
 "dropoffLocation": {
 "latitude": "number",
 "longitude": "number",
 "address": "string"
 }
}
Response:
{
 "bookingId": "string",
 "status": "string", // "PENDING", "CONFIRMED", "CANCELLED"
 "offeringId": "string",
 "pickupLocation": {
 "latitude": "number",
 "longitude": "number",
 "address": "string"
 },
 "dropoffLocation": {
 "latitude": "number",
 "longitude": "number",
 "address": "string"
 },
 "estimatedFare": "number",
 "createdAt": "string" // ISO 8601 format
}
```

### 2.5 Ride Execution APIs

#### 2.5.1 Driver Ride Management

**Start Ride**
```
POST /api/v1/rides/offerings/{offeringId}/start
Response:
{
 "offeringId": "string",
 "status": "string", // "IN_PROGRESS"
 "startTime": "string", // ISO 8601 format
 "riders": [
 {
 "riderId": "string",
 "riderName": "string",
 "bookingId": "string",
 "pickupLocation": {
 "latitude": "number",
 "longitude": "number",
 "address": "string"
 },
 "dropoffLocation": {
 "latitude": "number",
 "longitude": "number",
 "address": "string"
 },
 "status": "string" // "WAITING", "PICKED_UP", "DROPPED_OFF", "NO_SHOW"
 }
 ]
}
```

**Mark Rider Pickup**
```
POST /api/v1/rides/offerings/{offeringId}/bookings/{bookingId}/pickup
Response:
{
 "bookingId": "string",
 "status": "string", // "PICKED_UP"
 "pickupTime": "string" // ISO 8601 format
}
```

**Mark Rider No-Show**
```
POST /api/v1/rides/offerings/{offeringId}/bookings/{bookingId}/no-show
Response:
{
 "bookingId": "string",
 "status": "string", // "NO_SHOW"
 "noShowTime": "string", // ISO 8601 format
 "noShowFee": "number"
}
```

**Mark Rider Dropoff**
```
POST /api/v1/rides/offerings/{offeringId}/bookings/{bookingId}/dropoff
Response:
{
 "bookingId": "string",
 "status": "string", // "COMPLETED"
 "dropoffTime": "string", // ISO 8601 format
 "finalFare": "number"
}
```

**Complete Ride**
```
POST /api/v1/rides/offerings/{offeringId}/complete
Response:
{
 "offeringId": "string",
 "status": "string", // "COMPLETED"
 "completionTime": "string", // ISO 8601 format
 "totalFare": "number",
 "totalDistance": "number", // in kilometers
 "totalDuration": "number" // in minutes
}
```

#### 2.5.2 Real-time Location Updates

**Update Driver Location**
```
POST /api/v1/rides/offerings/{offeringId}/location
Request:
{
 "latitude": "number",
 "longitude": "number",
 "timestamp": "string" // ISO 8601 format
}
Response:
{
 "offeringId": "string",
 "received": "boolean"
}
```

**Get Driver Location**
```
GET /api/v1/rides/offerings/{offeringId}/location
Response:
{
 "offeringId": "string",
 "driverId": "string",
 "location": {
 "latitude": "number",
 "longitude": "number"
 },
 "timestamp": "string", // ISO 8601 format
 "estimatedArrival": {
 "pickupEta": "string", // ISO 8601 format
 "destinationEta": "string" // ISO 8601 format
 }
}
```

### 2.6 Payment APIs

#### 2.6.1 Wallet Management

**Get Wallet Balance**
```
GET /api/v1/payments/wallet
Response:
{
 "balance": "number",
 "currency": "string" // e.g., "INR"
}
```

**Add Funds to Wallet**
```
POST /api/v1/payments/wallet/add
Request:
{
 "amount": "number",
 "paymentMethod": "string", // "CREDIT_CARD", "BANK_ACCOUNT", "UPI"
 "paymentDetails": {
 // Payment method specific details
 }
}
Response:
{
 "transactionId": "string",
 "status": "string", // "PENDING", "COMPLETED", "FAILED"
 "amount": "number",
 "newBalance": "number",
 "transactionTime": "string" // ISO 8601 format
}
```

**Transfer to Bank Account**
```
POST /api/v1/payments/wallet/transfer
Request:
{
 "amount": "number",
 "bankAccount": {
 "accountNumber": "string",
 "ifscCode": "string",
 "accountName": "string"
 }
}
Response:
{
 "transferId": "string",
 "status": "string", // "INITIATED", "COMPLETED", "FAILED"
 "amount": "number",
 "estimatedCompletionTime": "string" // ISO 8601 format
}
```

#### 2.6.2 Fare Calculation

**Calculate Estimated Fare**
```
POST /api/v1/payments/calculate-fare
Request:
{
 "pickupLocation": {
 "latitude": "number",
 "longitude": "number"
 },
 "dropoffLocation": {
 "latitude": "number",
 "longitude": "number"
 },
 "time": "string", // ISO 8601 format
 "detourDistance": "number" // in kilometers
}
Response:
{
 "baseFare": "number",
 "distanceFare": "number",
 "timeFare": "number",
 "detourFare": "number",
 "peakTimeFactor": "number",
 "weatherFactor": "number",
 "trafficFactor": "number",
 "totalFare": "number",
 "currency": "string" // e.g., "INR"
}
```

### 2.7 Feedback and Rating APIs

**Submit Driver Rating**
```
POST /api/v1/feedback/drivers/{driverId}
Request:
{
 "bookingId": "string",
 "rating": "number", // 1-5
 "comments": "string",
 "isFavorite": "boolean"
}
Response:
{
 "feedbackId": "string",
 "status": "string" // "SUBMITTED"
}
```

**Submit Rider Rating**
```
POST /api/v1/feedback/riders/{riderId}
Request:
{
 "bookingId": "string",
 "rating": "number", // 1-5
 "comments": "string",
 "behaviors": [
 "string" // e.g., "PUNCTUAL", "POLITE", "DISRUPTIVE"
 ]
}
Response:
{
 "feedbackId": "string",
 "status": "string" // "SUBMITTED"
}
```

**Report Incident**
```
POST /api/v1/feedback/incidents
Request:
{
 "bookingId": "string",
 "incidentType": "string", // "SAFETY", "BEHAVIOR", "VEHICLE", "OTHER"
 "description": "string",
 "severity": "string", // "LOW", "MEDIUM", "HIGH", "CRITICAL"
 "attachments": [
 {
 "fileName": "string",
 "fileType": "string",
 "fileContent": "string" // base64 encoded
 }
 ]
}
Response:
{
 "incidentId": "string",
 "status": "string", // "REPORTED", "UNDER_INVESTIGATION", "RESOLVED"
 "expectedResolutionTime": "string" // ISO 8601 format
}
```

### 2.8 Safety Feature APIs

**Trigger SOS Alert**
```
POST /api/v1/safety/sos
Request:
{
 "bookingId": "string",
 "location": {
 "latitude": "number",
 "longitude": "number"
 },
 "alertType": "string" // "EMERGENCY", "SECURITY_CONCERN"
}
Response:
{
 "sosId": "string",
 "status": "string", // "TRIGGERED"
 "expectedResponseTime": "string" // ISO 8601 format
}
```

**Share Ride with Contact**
```
POST /api/v1/safety/share-ride
Request:
{
 "bookingId": "string",
 "contactName": "string",
 "contactPhone": "string",
 "contactEmail": "string",
 "shareLocation": "boolean",
 "shareDriverDetails": "boolean",
 "shareEta": "boolean"
}
Response:
{
 "shareId": "string",
 "trackingUrl": "string",
 "expiresAt": "string" // ISO 8601 format
}
```

## 3. Database Schema Definitions

### 3.1 User Schema

#### 3.1.1 Users Table
```sql
CREATE TABLE users (
 user_id VARCHAR(36) PRIMARY KEY,
 corporate_email VARCHAR(100) UNIQUE NOT NULL,
 phone_number VARCHAR(20) UNIQUE,
 first_name VARCHAR(50) NOT NULL,
 last_name VARCHAR(50) NOT NULL,
 gender ENUM('MALE', 'FEMALE', 'OTHER', 'PREFER_NOT_TO_SAY') NOT NULL,
 profile_image_url VARCHAR(255),
 department VARCHAR(100),
 employee_id VARCHAR(50),
 status ENUM('ACTIVE', 'INACTIVE', 'SUSPENDED', 'DELETED') NOT NULL DEFAULT 'ACTIVE',
 created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
 updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
 last_login TIMESTAMP,
 auth_provider ENUM('CORPORATE_SSO', 'EMAIL_PASSWORD') NOT NULL,
 INDEX idx_corporate_email (corporate_email),
 INDEX idx_status (status)
);
```

#### 3.1.2 Drivers Table
```sql
CREATE TABLE drivers (
 driver_id VARCHAR(36) PRIMARY KEY,
 user_id VARCHAR(36) NOT NULL,
 license_number VARCHAR(50) NOT NULL,
 license_expiry_date DATE NOT NULL,
 license_verification_status ENUM('PENDING', 'VERIFIED', 'REJECTED') NOT NULL,
 max_detour_distance DECIMAL(5,2) NOT NULL DEFAULT 0.5,
 total_rides INT NOT NULL DEFAULT 0,
 average_rating DECIMAL(3,2),
 status ENUM('ACTIVE', 'INACTIVE', 'SUSPENDED') NOT NULL DEFAULT 'ACTIVE',
 gender_preference_female_only BOOLEAN NOT NULL DEFAULT FALSE,
 created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
 updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
 FOREIGN KEY (user_id) REFERENCES users(user_id),
 INDEX idx_status (status)
);
```

#### 3.1.3 Vehicles Table
```sql
CREATE TABLE vehicles (
 vehicle_id VARCHAR(36) PRIMARY KEY,
 driver_id VARCHAR(36) NOT NULL,
 make VARCHAR(50) NOT NULL,
 model VARCHAR(50) NOT NULL,
 year INT NOT NULL,
 color VARCHAR(30) NOT NULL,
 license_plate VARCHAR(20) NOT NULL,
 capacity INT NOT NULL,
 is_active BOOLEAN NOT NULL DEFAULT TRUE,
 created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
 updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
 FOREIGN KEY (driver_id) REFERENCES drivers(driver_id),
 INDEX idx_license_plate (license_plate)
);
```

#### 3.1.4 Riders Table
```sql
CREATE TABLE riders (
 rider_id VARCHAR(36) PRIMARY KEY,
 user_id VARCHAR(36) NOT NULL,
 preferred_morning_time TIME,
 preferred_evening_time TIME,
 total_rides INT NOT NULL DEFAULT 0,
 average_rating DECIMAL(3,2),
 status ENUM('ACTIVE', 'INACTIVE', 'SUSPENDED') NOT NULL DEFAULT 'ACTIVE',
 gender_preference_female_drivers BOOLEAN NOT NULL DEFAULT FALSE,
 created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
 updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
 FOREIGN KEY (user_id) REFERENCES users(user_id),
 INDEX idx_status (status)
);
```

### 3.2 Location and Routes Schema

#### 3.2.1 Locations Table
```sql
CREATE TABLE locations (
 location_id VARCHAR(36) PRIMARY KEY,
 latitude DECIMAL(10,8) NOT NULL,
 longitude DECIMAL(11,8) NOT NULL,
 address TEXT,
 address_type ENUM('HOME', 'WORK', 'OTHER') NOT NULL,
 user_id VARCHAR(36) NOT NULL,
 is_default BOOLEAN NOT NULL DEFAULT FALSE,
 created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
 updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
 FOREIGN KEY (user_id) REFERENCES users(user_id),
 INDEX idx_user_location (user_id, address_type),
 SPATIAL INDEX idx_spatial (latitude, longitude)
);
```

#### 3.2.2 Driver Routes Table
```sql
CREATE TABLE driver_routes (
 route_id VARCHAR(36) PRIMARY KEY,
 driver_id VARCHAR(36) NOT NULL,
 start_location_id VARCHAR(36) NOT NULL,
 end_location_id VARCHAR(36) NOT NULL,
 is_active BOOLEAN NOT NULL DEFAULT TRUE,
 created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
 updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
 FOREIGN KEY (driver_id) REFERENCES drivers(driver_id),
 FOREIGN KEY (start_location_id) REFERENCES locations(location_id),
 FOREIGN KEY (end_location_id) REFERENCES locations(location_id),
 INDEX idx_driver_active (driver_id, is_active)
);
```

### 3.3 Ride Management Schema

#### 3.3.1 Ride Offerings Table
```sql
CREATE TABLE ride_offerings (
 offering_id VARCHAR(36) PRIMARY KEY,
 driver_id VARCHAR(36) NOT NULL,
 vehicle_id VARCHAR(36) NOT NULL,
 departure_time DATETIME NOT NULL,
 start_location_id VARCHAR(36) NOT NULL,
 end_location_id VARCHAR(36) NOT NULL,
 available_seats INT NOT NULL,
 max_detour_distance DECIMAL(5,2) NOT NULL,
 status ENUM('SCHEDULED', 'IN_PROGRESS', 'COMPLETED', 'CANCELLED') NOT NULL,
 cancellation_reason TEXT,
 created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
 updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
 FOREIGN KEY (driver_id) REFERENCES drivers(driver_id),
 FOREIGN KEY (vehicle_id) REFERENCES vehicles(vehicle_id),
 FOREIGN KEY (start_location_id) REFERENCES locations(location_id),
 FOREIGN KEY (end_location_id) REFERENCES locations(location_id),
 INDEX idx_departure_time (departure_time),
 INDEX idx_status (status)
);
```

#### 3.3.2 Ride Bookings Table
```sql
CREATE TABLE ride_bookings (
 booking_id VARCHAR(36) PRIMARY KEY,
 offering_id VARCHAR(36) NOT NULL,
 rider_id VARCHAR(36) NOT NULL,
 pickup_location_id VARCHAR(36) NOT NULL,
 dropoff_location_id VARCHAR(36) NOT NULL,
 status ENUM('PENDING', 'CONFIRMED', 'PICKED_UP', 'COMPLETED', 'CANCELLED', 'NO_SHOW') NOT NULL,
 estimated_pickup_time DATETIME,
 actual_pickup_time DATETIME,
 estimated_dropoff_time DATETIME,
 actual_dropoff_time DATETIME,
 waiting_start_time DATETIME,
 estimated_fare DECIMAL(10,2),
 final_fare DECIMAL(10,2),
 cancellation_reason TEXT,
 cancellation_fee DECIMAL(10,2),
 no_show_fee DECIMAL(10,2),
 created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
 updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
 FOREIGN KEY (offering_id) REFERENCES ride_offerings(offering_id),
 FOREIGN KEY (rider_id) REFERENCES riders(rider_id),
 FOREIGN KEY (pickup_location_id) REFERENCES locations(location_id),
 FOREIGN KEY (dropoff_location_id) REFERENCES locations(location_id),
 INDEX idx_offering_id (offering_id),
 INDEX idx_rider_id (rider_id),
 INDEX idx_status (status)
);
```

#### 3.3.3 Location Tracking Table
```sql
CREATE TABLE location_tracking (
 tracking_id VARCHAR(36) PRIMARY KEY,
 offering_id VARCHAR(36) NOT NULL,
 latitude DECIMAL(10,8) NOT NULL,
 longitude DECIMAL(11,8) NOT NULL,
 timestamp DATETIME NOT NULL,
 speed DECIMAL(5,2),
 heading INT,
 created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
 FOREIGN KEY (offering_id) REFERENCES ride_offerings(offering_id),
 INDEX idx_offering_timestamp (offering_id, timestamp),
 SPATIAL INDEX idx_spatial (latitude, longitude)
);
```

### 3.4 Payment Schema

#### 3.4.1 Wallets Table
```sql
CREATE TABLE wallets (
 wallet_id VARCHAR(36) PRIMARY KEY,
 user_id VARCHAR(36) NOT NULL UNIQUE,
 balance DECIMAL(10,2) NOT NULL DEFAULT 0.00,
 currency VARCHAR(3) NOT NULL DEFAULT 'INR',
 status ENUM('ACTIVE', 'FROZEN', 'CLOSED') NOT NULL DEFAULT 'ACTIVE',
 created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
 updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
 FOREIGN KEY (user_id) REFERENCES users(user_id),
 INDEX idx_user_id (user_id),
 INDEX idx_status (status)
);
```

#### 3.4.2 Payment Methods Table
```sql
CREATE TABLE payment_methods (
 payment_method_id VARCHAR(36) PRIMARY KEY,
 user_id VARCHAR(36) NOT NULL,
 payment_type ENUM('CREDIT_CARD', 'DEBIT_CARD', 'UPI', 'BANK_ACCOUNT', 'WALLET') NOT NULL,
 is_default BOOLEAN NOT NULL DEFAULT FALSE,
 status ENUM('ACTIVE', 'EXPIRED', 'REMOVED') NOT NULL DEFAULT 'ACTIVE',
 last_four_digits VARCHAR(4),
 expiry_date DATE,
 card_brand VARCHAR(20),
 bank_name VARCHAR(100),
 account_holder_name VARCHAR(100),
 created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
 updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
 FOREIGN KEY (user_id) REFERENCES users(user_id),
 INDEX idx_user_payment (user_id, payment_type, status)
);
```

#### 3.4.3 Transactions Table
```sql
CREATE TABLE transactions (
 transaction_id VARCHAR(36) PRIMARY KEY,
 wallet_id VARCHAR(36) NOT NULL,
 amount DECIMAL(10,2) NOT NULL,
 currency VARCHAR(3) NOT NULL DEFAULT 'INR',
 transaction_type ENUM('CREDIT', 'DEBIT', 'REFUND', 'FEE') NOT NULL,
 status ENUM('PENDING', 'COMPLETED', 'FAILED', 'REVERSED') NOT NULL,
 reference_id VARCHAR(36),
 reference_type ENUM('BOOKING', 'TOPUP', 'WITHDRAWAL', 'CANCELLATION', 'NO_SHOW', 'ADJUSTMENT'),
 payment_method_id VARCHAR(36),
 description TEXT,
 created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
 updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
 FOREIGN KEY (wallet_id) REFERENCES wallets(wallet_id),
 FOREIGN KEY (payment_method_id) REFERENCES payment_methods(payment_method_id),
 INDEX idx_wallet_id (wallet_id),
 INDEX idx_reference (reference_type, reference_id),
 INDEX idx_created_at (created_at)
);
```

### 3.5 Feedback and Rating Schema

#### 3.5.1 Driver Ratings Table
```sql
CREATE TABLE driver_ratings (
 rating_id VARCHAR(36) PRIMARY KEY,
 booking_id VARCHAR(36) NOT NULL UNIQUE,
 driver_id VARCHAR(36) NOT NULL,
 rider_id VARCHAR(36) NOT NULL,
 rating TINYINT NOT NULL CHECK (rating BETWEEN 1 AND 5),
 comments TEXT,
 is_favorite BOOLEAN NOT NULL DEFAULT FALSE,
 created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
 FOREIGN KEY (booking_id) REFERENCES ride_bookings(booking_id),
 FOREIGN KEY (driver_id) REFERENCES drivers(driver_id),
 FOREIGN KEY (rider_id) REFERENCES riders(rider_id),
 INDEX idx_driver_id (driver_id),
 INDEX idx_rider_id (rider_id)
);
```

#### 3.5.2 Rider Ratings Table
```sql
CREATE TABLE rider_ratings (
 rating_id VARCHAR(36) PRIMARY KEY,
 booking_id VARCHAR(36) NOT NULL UNIQUE,
 rider_id VARCHAR(36) NOT NULL,
 driver_id VARCHAR(36) NOT NULL,
 rating TINYINT NOT NULL CHECK (rating BETWEEN 1 AND 5),
 comments TEXT,
 behavior_tags JSON,
 created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
 FOREIGN KEY (booking_id) REFERENCES ride_bookings(booking_id),
 FOREIGN KEY (rider_id) REFERENCES riders(rider_id),
 FOREIGN KEY (driver_id) REFERENCES drivers(driver_id),
 INDEX idx_rider_id (rider_id),
 INDEX idx_driver_id (driver_id)
);
```

#### 3.5.3 Incident Reports Table
```sql
CREATE TABLE incident_reports (
 incident_id VARCHAR(36) PRIMARY KEY,
 booking_id VARCHAR(36) NOT NULL,
 reported_by_user_id VARCHAR(36) NOT NULL,
 reported_against_user_id VARCHAR(36),
 incident_type ENUM('SAFETY', 'BEHAVIOR', 'VEHICLE', 'DAMAGE', 'OTHER') NOT NULL,
 severity ENUM('LOW', 'MEDIUM', 'HIGH', 'CRITICAL') NOT NULL,
 description TEXT NOT NULL,
 status ENUM('REPORTED', 'UNDER_INVESTIGATION', 'RESOLVED', 'DISMISSED') NOT NULL DEFAULT 'REPORTED',
 resolution_notes TEXT,
 investigation_owner VARCHAR(100),
 created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
 updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
 FOREIGN KEY (booking_id) REFERENCES ride_bookings(booking_id),
 FOREIGN KEY (reported_by_user_id) REFERENCES users(user_id),
 FOREIGN KEY (reported_against_user_id) REFERENCES users(user_id),
 INDEX idx_status (status),
 INDEX idx_booking_id (booking_id),
 INDEX idx_severity (severity)
);
```

### 3.6 Safety Feature Schema

#### 3.6.1 SOS Alerts Table
```sql
CREATE TABLE sos_alerts (
 sos_id VARCHAR(36) PRIMARY KEY,
 user_id VARCHAR(36) NOT NULL,
 booking_id VARCHAR(36) NOT NULL,
 latitude DECIMAL(10,8) NOT NULL,
 longitude DECIMAL(11,8) NOT NULL,
 alert_type ENUM('EMERGENCY', 'SECURITY_CONCERN') NOT NULL,
 status ENUM('TRIGGERED', 'ACKNOWLEDGED', 'RESOLVED', 'FALSE_ALARM') NOT NULL DEFAULT 'TRIGGERED',
 resolved_by VARCHAR(100),
 resolution_notes TEXT,
 created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
 updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
 FOREIGN KEY (user_id) REFERENCES users(user_id),
 FOREIGN KEY (booking_id) REFERENCES ride_bookings(booking_id),
 INDEX idx_status (status),
 INDEX idx_user_id (user_id),
 SPATIAL INDEX idx_spatial (latitude, longitude)
);
```

#### 3.6.2 Ride Shares Table
```sql
CREATE TABLE ride_shares (
 share_id VARCHAR(36) PRIMARY KEY,
 booking_id VARCHAR(36) NOT NULL,
 user_id VARCHAR(36) NOT NULL,
 contact_name VARCHAR(100) NOT NULL,
 contact_phone VARCHAR(20),
 contact_email VARCHAR(100),
 tracking_code VARCHAR(10) NOT NULL,
 share_location BOOLEAN NOT NULL DEFAULT TRUE,
 share_driver_details BOOLEAN NOT NULL DEFAULT TRUE,
 share_eta BOOLEAN NOT NULL DEFAULT TRUE,
 expires_at DATETIME NOT NULL,
 created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
 FOREIGN KEY (booking_id) REFERENCES ride_bookings(booking_id),
 FOREIGN KEY (user_id) REFERENCES users(user_id),
 INDEX idx_tracking_code (tracking_code),
 INDEX idx_expires_at (expires_at)
);
```

### 3.7 System Management Schema

#### 3.7.1 System Parameters Table
```sql
CREATE TABLE system_parameters (
 param_id VARCHAR(36) PRIMARY KEY,
 param_name VARCHAR(50) NOT NULL UNIQUE,
 param_value TEXT NOT NULL,
 description TEXT,
 data_type ENUM('STRING', 'INTEGER', 'DECIMAL', 'BOOLEAN', 'JSON') NOT NULL,
 is_encrypted BOOLEAN NOT NULL DEFAULT FALSE,
 created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
 updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
 INDEX idx_param_name (param_name)
);
```

#### 3.7.2 Audit Logs Table
```sql
CREATE TABLE audit_logs (
 log_id VARCHAR(36) PRIMARY KEY,
 user_id VARCHAR(36),
 action VARCHAR(100) NOT NULL,
 entity_type VARCHAR(50) NOT NULL,
 entity_id VARCHAR(36) NOT NULL,
 old_value JSON,
 new_value JSON,
 ip_address VARCHAR(45),
 user_agent TEXT,
 created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
 INDEX idx_user_id (user_id),
 INDEX idx_entity (entity_type, entity_id),
 INDEX idx_created_at (created_at)
);
```

## 4. Third-Party Integration Specifications

### 4.1 Corporate Authentication Integration

#### 4.1.1 Corporate SSO Integration
- **Protocol**: OAuth 2.0 / SAML 2.0
- **Authentication Flow**: Authorization Code Flow
- **Required Claims**:
 - User ID (corporate email)
 - First Name
 - Last Name
 - Department
 - Employee ID
 - Email Verification Status
- **Integration Points**:
 - Login endpoint
 - Token validation endpoint
 - User information endpoint
 - Logout endpoint
- **Refresh Policy**: Tokens expire after 8 hours; refresh tokens valid for 30 days

#### 4.1.2 Two-Factor Authentication
- **OTP Delivery Methods**: Corporate email, SMS to registered mobile
- **OTP Validity**: 5 minutes
- **Retry Limits**: 3 attempts before lockout
- **Lockout Duration**: 15 minutes

### 4.2 Mapping and Location Services

#### 4.2.1 Geocoding Service
- **Provider**: Google Maps API / Mapbox API
- **Functions**:
 - Forward geocoding (address to coordinates)
 - Reverse geocoding (coordinates to address)
 - Place autocomplete
- **Request Limits**: 10,000 requests per day
- **Caching Strategy**: Cache frequent locations for 30 days

#### 4.2.2 Route Calculation Service
- **Provider**: Google Maps Directions API / Mapbox Directions API
- **Functions**:
 - Optimal route calculation
 - Distance and duration estimation
 - Traffic-aware routing
 - Multi-stop route optimization
- **Request Format**:
 - Origin coordinates
 - Destination coordinates
 - Waypoints (for multiple pickups)
 - Departure time
 - Traffic model preference
- **Response Format**:
 - Route geometry (encoded polyline)
 - Step-by-step directions
 - Distance (meters)
 - Duration (seconds)
 - Traffic conditions

#### 4.2.3 Real-time Location Tracking
- **Provider**: Custom WebSocket solution with AWS API Gateway
- **Update Frequency**: Every 5 seconds when in active ride
- **Data Points**:
 - Latitude and longitude
 - Timestamp
 - Speed (if available)
 - Heading (if available)
- **Security**:
 - JWT authentication for WebSocket connections
 - Data encryption for location updates

### 4.3 Payment Gateway Integration

#### 4.3.1 Payment Processing
- **Provider**: Razorpay / Stripe
- **Supported Payment Methods**:
 - Credit/Debit Cards
 - UPI
 - NetBanking
 - Wallets
- **Integration Type**: Server-to-server API with client-side tokenization
- **Authentication**: API key and webhook signature verification
- **Currency**: INR (primary), with multi-currency support
- **Security Compliance**:
 - PCI-DSS Level 1
 - Tokenization for stored payment methods
 - 3D Secure for card transactions

#### 4.3.2 Bank Transfer API
- **Provider**: Banking Partner API
- **Functions**:
 - Funds transfer to driver bank accounts
 - Balance inquiry
 - Transaction status verification
- **Integration Type**: Server-to-server API with mutual TLS
- **Processing Time**: Same-day transfers initiated before 3 PM IST
- **Minimum Transfer Amount**: ₹100

### 4.4 Notification Services

#### 4.4.1 Push Notification Service
- **Provider**: Firebase Cloud Messaging (FCM)
- **Platform Support**:
 - Android
 - iOS
- **Notification Types**:
 - Ride updates
 - Payment confirmations
 - Safety alerts
 - System announcements
- **Payload Structure**:
 - Title
 - Body
 - Data payload (JSON)
 - Deep link URL
 - Priority level
- **Delivery Tracking**: Success/failure metrics for critical notifications

#### 4.4.2 Email Notification Service
- **Provider**: Amazon SES / SendGrid
- **Email Templates**:
 - Registration confirmation
 - OTP verification
 - Ride confirmations
 - Receipts
 - Critical alerts
- **Template Format**: Responsive HTML with plain text fallback
- **Tracking**: Open rates, click rates for marketing communications

#### 4.4.3 SMS Gateway
- **Provider**: Twilio / MSG91
- **Use Cases**:
 - OTP delivery
 - Critical ride updates
 - Emergency notifications
- **Message Format**: Unicode support for multiple languages
- **Delivery Reports**: Required for critical communications

### 4.5 Weather and Traffic API

#### 4.5.1 Weather Service
- **Provider**: OpenWeather API / AccuWeather
- **Data Points**:
 - Current weather conditions
 - Precipitation probability
 - Visibility
 - Wind speed
- **Update Frequency**: Hourly updates
- **Geographic Resolution**: City level

#### 4.5.2 Traffic Conditions Service
- **Provider**: Google Maps / TomTom Traffic API
- **Data Points**:
 - Traffic density
 - Incidents (accidents, closures)
 - Historical traffic patterns
 - Real-time traffic flow
- **Update Frequency**: Every 5 minutes
- **Integration**: Factor into fare calculation and ETA estimates

## 5. Security Implementation Specifications

### 5.1 Authentication and Authorization

#### 5.1.1 Authentication Mechanisms
- **Primary Authentication**: Corporate SSO integration with JWT token
- **Secondary Authentication**: OTP verification for sensitive actions
- **Token Management**:
 - Access tokens: 8-hour validity
 - Refresh tokens: 30-day validity, single-use
 - Token rotation policy
 - Token revocation on password change or security event

#### 5.1.2 Authorization Framework
- **Role-Based Access Control**:
 - Driver role
 - Rider role
 - Admin role (multiple levels)
 - System role (for automated processes)
- **Permission Structure**:
 - Resource-level permissions
 - Action-based permissions (create, read, update, delete)
 - Context-based permissions (own data vs. others' data)
- **Implementation**:
 - JWT claims for role and permission encoding
 - API Gateway authorization lambda for validation
 - Database-level permission enforcement

### 5.2 Data Protection

#### 5.2.1 Data Encryption
- **Data at Rest**:
 - AES-256 encryption for sensitive database fields
 - S3 server-side encryption for documents and media
 - Encrypted EBS volumes
- **Data in Transit**:
 - TLS 1.3 for all API communications
 - Certificate pinning in mobile applications
 - Secure WebSocket protocol (wss://)
- **Key Management**:
 - AWS KMS for encryption key management
 - Key rotation policy (annually)
 - Separate encryption contexts for different data categories

#### 5.2.2 Personal Data Handling
- **PII Classification**:
 - High sensitivity: Government IDs, financial information
 - Medium sensitivity: Contact information, location history
 - Low sensitivity: Preferences, ride history
- **Data Minimization**:
 - Collection limited to necessary information
 - Automated data pruning for expired records
- **Data Retention**:
 - Active ride data: Indefinite (linked to account)
 - Location tracking data: 30 days
 - Payment transaction records: 7 years (regulatory requirement)
 - Chat and communication logs: 90 days

### 5.3 API Security

#### 5.3.1 API Protection Measures
- **Rate Limiting**:
 - Authentication endpoints: 5 requests per minute per IP
 - Standard API endpoints: 60 requests per minute per user
 - Administrative endpoints: 30 requests per minute per user
- **Input Validation**:
 - Server-side validation for all inputs
 - Parameterized queries for database operations
 - JSON Schema validation for request bodies
- **Output Encoding**:
 - Context-appropriate encoding for all outputs
 - Content-Type enforcement

#### 5.3.2 API Gateway Security
- **Request Throttling**: Configured at API Gateway level
- **WAF Integration**: AWS WAF with OWASP Top 10 ruleset
- **Request Validation**: Request parameter validation
- **Usage Plans**: Separate plans for different client types

### 5.4 Mobile Application Security

#### 5.4.1 Mobile Security Controls
- **Certificate Pinning**: Prevent man-in-the-middle attacks
- **App Transport Security**: Enforce HTTPS connections
- **Secure Local Storage**:
 - Keychain/Keystore for sensitive data
 - Encrypted SQLite database
 - No sensitive data in shared preferences/user defaults
- **Jailbreak/Root Detection**:
 - Runtime checks for device integrity
 - Restricted functionality on compromised devices
- **Code Obfuscation**: Prevent reverse engineering
- **Tampering Detection**: Runtime integrity checks

#### 5.4.2 Secure Offline Operations
- **Offline Authentication**:
 - Time-limited offline access
 - Re-authentication required for sensitive operations
- **Data Synchronization**:
 - Secure queue for offline actions
 - Conflict resolution strategy
 - Integrity verification upon reconnection

### 5.5 Incident Response and Monitoring

#### 5.5.1 Security Monitoring
- **Log Management**:
 - Centralized logging with CloudWatch Logs
 - Log retention policy (90 days online, 7 years archived)
 - Log field encryption for sensitive data
- **Alerting**:
 - Unusual authentication patterns
 - Geographic anomalies
 - Rate limit breaches
 - Critical resource access
- **Metrics**:
 - Failed authentication attempts
 - API usage patterns
 - Permission denial events
 - Security control effectiveness

#### 5.5.2 Incident Response
- **Incident Classification**:
 - Severity levels (1-4)
 - Response time SLAs by severity
 - Escalation procedures
- **Response Actions**:
 - Account lockout procedures
 - Forced logout capabilities
 - Region/IP blocking
 - Evidence preservation

## 6. Performance Requirements

### 6.1 API Performance

#### 6.1.1 Response Time Targets
- **Critical Path APIs**:
 - Authentication: <500ms (P95)
 - Ride search: <1s (P95)
 - Booking confirmation: <1.5s (P95)
 - Location updates: <300ms (P95)
- **Background Operations**:
 - Payment processing: <3s (P95)
 - Ride history: <2s (P95)
 - Analytics operations: <5s (P95)
- **Batch Operations**:
 - Report generation: <30s
 - Mass notifications: <5 minutes for full delivery

#### 6.1.2 Throughput Requirements
- **Peak Concurrent Users**: 5,000
- **API Requests per Second**:
 - Authentication APIs: 100 RPS
 - Ride management APIs: 50 RPS
 - Location tracking: 200 RPS
 - Payment APIs: 30 RPS
- **Database Operations**:
 - Read operations: 1,000 per second
 - Write operations: 200 per second

### 6.2 Mobile Application Performance

#### 6.2.1 Application Responsiveness
- **Launch Time**: <3 seconds on mid-range devices
- **Screen Transition**: <300ms
- **Map Loading**: <2 seconds for initial load
- **Interactive Operations**: <100ms response to user input
- **Background Processing**: No UI thread blocking for network operations

#### 6.2.2 Resource Utilization
- **Memory Usage**:
 - Peak: <150MB
 - Background: <50MB
- **Battery Impact**:
 - <5% per hour during active ride tracking
 - <1% per hour when app is in background
- **Network Usage**:
 - <5MB per hour during active ride
 - <500KB per typical ride booking flow
 - Efficient caching for repeat data

### 6.3 Scalability Requirements

#### 6.3.1 User Base Scaling
- **Initial User Base**: 1,000 concurrent users
- **Target Growth**: Support 5x growth within 12 months
- **Scaling Model**: Horizontal scaling for all services
- **Auto-scaling Configuration**:
 - Scale out: When CPU utilization >70% for 5 minutes
 - Scale in: When CPU utilization <30% for 15 minutes

#### 6.3.2 Geographic Scaling
- **Initial Deployment**: Single region
- **Geographic Expansion**:
 - Multi-region capability for global offices
 - Data residency compliance
 - Region-specific configurations

### 6.4 Reliability and Availability

#### 6.4.1 Availability Targets
- **Core Service Availability**: 99.9% (8.76 hours downtime per year)
- **Ride Execution Functions**: 99.95% (4.38 hours downtime per year)
- **Safety Critical Features**: 99.99% (52.6 minutes downtime per year)
- **Maintenance Windows**: Scheduled during non-peak hours, max 4 hours per month

#### 6.4.2 Disaster Recovery
- **Recovery Time Objective (RTO)**: <4 hours
- **Recovery Point Objective (RPO)**: <1 hour
- **Backup Schedule**:
 - Database: Daily full backup, hourly incremental
 - Configuration: Version controlled with history
 - User content: Continuous backup
- **Failover Strategy**:
 - Database: Multi-AZ with automated failover
 - Application: Multi-AZ deployment
 - Critical services: Active-active configuration

## 7. Implementation Guidelines

### 7.1 Coding Standards

#### 7.1.1 Backend Development
- **Languages**: Node.js, Python, Java
- **Framework**: Express.js, FastAPI, Spring Boot
- **API Documentation**: OpenAPI 3.0
- **Code Style**: ESLint/Pylint/CheckStyle enforced
- **Unit Test Coverage**: Minimum 80% code coverage

#### 7.1.2 Mobile Development
- **iOS**:
 - Language: Swift 5+
 - Minimum iOS version: iOS 13.0
 - Architecture: MVVM
 - Dependencies: CocoaPods/Swift Package Manager
- **Android**:
 - Language: Kotlin
 - Minimum API Level: 24 (Android 7.0)
 - Architecture: MVVM
 - Dependencies: Gradle with version constraints

### 7.2 Deployment Guidelines

#### 7.2.1 CI/CD Pipeline
- **Source Control**: Git with feature branch workflow
- **Build Automation**: Jenkins/GitHub Actions
- **Artifact Repository**: AWS ECR for containers, S3 for packages
- **Deployment Automation**: AWS CloudFormation/Terraform
- **Environment Promotion**:
 - Development: Continuous deployment
 - Testing: Daily builds
 - Staging: Release candidate builds
 - Production: Approved releases only

#### 7.2.2 Infrastructure as Code
- **IaC Tool**: Terraform/AWS CDK
- **Environment Consistency**: Identical configuration across environments
- **Configuration Management**: Parameter Store/Secrets Manager
- **Resource Tagging**: Mandatory for cost allocation and ownership

### 7.3 Testing Strategy

#### 7.3.1 Testing Levels
- **Unit Testing**: Component level functionality
- **Integration Testing**: Service interaction validation
- **API Testing**: Contract verification
- **UI Testing**: User interface validation
- **Performance Testing**: Load and stress tests
- **Security Testing**: Vulnerability scanning and penetration testing

#### 7.3.2 Testing Automation
- **Unit Test Framework**: Jest/Pytest/JUnit
- **API Test Framework**: Postman/REST Assured
- **UI Test Framework**: Appium/XCTest/Espresso
- **Performance Test Tools**: JMeter/Gatling
- **Security Test Tools**: OWASP ZAP, SonarQube

### 7.4 Monitoring and Operations

#### 7.4.1 Application Monitoring
- **APM Solution**: New Relic/AppDynamics
- **Log Management**: ELK Stack/CloudWatch Logs
- **Metrics Collection**: Prometheus/CloudWatch
- **Alerting**: PagerDuty integration with escalation policies
- **Dashboards**: Grafana for operational visibility

#### 7.4.2 Operational Procedures
- **Incident Response**: Severity-based SLA
- **Change Management**: Approval workflow for production changes
- **Capacity Planning**: Quarterly review and adjustment
- **Backup Verification**: Monthly restoration testing
- **Security Patching**: Monthly cycle with risk assessment

## 8. Appendices

### 8.1 Glossary of Terms
- **API**: Application Programming Interface
- **JWT**: JSON Web Token
- **OTP**: One-Time Password
- **SSO**: Single Sign-On
- **ETA**: Estimated Time of Arrival
- **UPI**: Unified Payment Interface

### 8.2 Reference Materials
- AWS Well-Architected Framework
- OWASP API Security Top 10
- Mobile App Security Verification Standard
- GDPR and Data Protection Guidelines
- Corporate Security Standards
