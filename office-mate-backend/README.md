# Officemate Backend

Corporate ride-sharing application backend built with Spring Boot, providing secure authentication, user profile management, wallet services, and safety features.

## Overview

Officemate is a corporate ride-sharing platform that enables employees to share rides with colleagues. The backend provides:

- **Mobile-first Authentication**: OTP-based registration and login
- **Corporate Email Verification**: Employee verification through corporate email
- **Comprehensive Profiles**: Driver and rider profiles with preferences
- **Wallet Integration**: Automated payment processing
- **Safety Features**: Emergency contacts and ride sharing
- **Multi-database Architecture**: PostgreSQL, Redis, MongoDB, and DynamoDB

## Technology Stack

- **Framework**: Spring Boot 3.2.0
- **Language**: Java 19
- **Databases**: 
  - PostgreSQL 15 (Primary relational database)
  - Redis 7 (Session storage and OTP caching)
  - MongoDB 7 (Migration support)
  - DynamoDB (Route preferences and real-time data)
- **Security**: Spring Security with JWT
- **Build Tool**: Gradle 8.5
- **Containerization**: Docker & Docker Compose
- **Cloud Services**: AWS SDK (SNS, SES, KMS, DynamoDB)

## Quick Start

### Prerequisites

- Java 19 or higher
- Docker Desktop
- Git

### Setup

```bash
# Clone repository
git clone <repository-url>
cd officemate-backend

# Create environment file
make setup

# Start infrastructure services
make docker-up

# Initialize databases
make init-db

# Run application
make run
```

### Verify Setup

```bash
# Check application health
curl http://localhost:8080/actuator/health

# Or run verification script
make verify
```

## Documentation

- **[Setup Guide](SETUP.md)** - Complete setup instructions
- **[Docker Deployment](DOCKER_DEPLOYMENT.md)** - Containerization and deployment guide
- **[Containerization Summary](CONTAINERIZATION_SUMMARY.md)** - Architecture overview
- **[Database Configuration](docs/DATABASE_CONFIGURATION.md)** - Database setup details
- **[Architecture Docs](docs/architecture/)** - System architecture documentation
- **[Requirements](docs/requirement/)** - Feature requirements

## Project Structure

```
officemate-backend/
├── src/
│   ├── main/
│   │   ├── java/com/officemate/
│   │   │   ├── config/          # Configuration classes
│   │   │   ├── modules/         # Feature modules
│   │   │   │   ├── auth/        # Authentication
│   │   │   │   ├── profile/     # User profiles
│   │   │   │   ├── wallet/      # Payment wallet
│   │   │   │   └── safety/      # Safety features
│   │   │   ├── shared/          # Shared utilities
│   │   │   └── OfficemateApplication.java
│   │   └── resources/
│   │       ├── application.yml
│   │       ├── application-dev.yml
│   │       ├── application-test.yml
│   │       └── application-prod.yml
│   └── test/                    # Test files
├── scripts/
│   ├── db/
│   │   ├── migrations/          # Database migrations
│   │   ├── init/                # Initialization scripts
│   │   └── run-migrations.sh
│   ├── deploy-dev.sh
│   ├── deploy-staging.sh
│   └── deploy-prod.sh
├── docs/                        # Documentation
├── Dockerfile                   # Development Docker image
├── Dockerfile.prod              # Production Docker image
├── docker-compose.yml           # Development services
├── docker-compose.prod.yml      # Production services
├── Makefile                     # Build automation
└── build.gradle.kts             # Gradle build configuration
```

## Available Commands

### Development

```bash
make setup          # Create .env from template
make build          # Build application
make test           # Run tests
make run            # Run application locally
make dev-start      # Start development environment
make dev-stop       # Stop development environment
make dev-reset      # Reset development environment
```

### Docker

```bash
make docker-build       # Build Docker image
make docker-up          # Start infrastructure services
make docker-up-full     # Start all services including app
make docker-down        # Stop all services
make docker-logs        # View logs
make docker-ps          # Show running containers
```

### Database

```bash
make migrate        # Run database migrations
make init-db        # Initialize databases
```

### Deployment

```bash
make deploy-dev         # Deploy to development
make deploy-staging     # Deploy to staging
make deploy-prod        # Deploy to production
```

### Monitoring

```bash
make health             # Check application health
make health-services    # Check all services health
```

## Development Workflow

### Daily Development

1. Start infrastructure services:
   ```bash
   make docker-up
   ```

2. Run application locally:
   ```bash
   make run
   ```

3. Make code changes (application auto-reloads)

4. Run tests:
   ```bash
   make test
   ```

5. Stop services at end of day:
   ```bash
   make docker-down
   ```

### Making Database Changes

1. Create migration file in `scripts/db/migrations/`:
   ```sql
   -- V3__add_new_feature.sql
   CREATE TABLE new_feature (
       id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
       name VARCHAR(255) NOT NULL
   );
   ```

2. Run migration:
   ```bash
   make migrate
   ```

## API Endpoints

Base URL: `http://localhost:8080`

### Authentication APIs

#### Register New User
```http
POST /auth/register
Content-Type: application/json

{
  "phoneNumber": "+1234567890"
}

Response: 200 OK
{
  "userId": "uuid",
  "otpSent": true,
  "expiresAt": "2024-01-01T12:05:00"
}
```

#### Verify Mobile OTP (Registration)
```http
POST /auth/verify-mobile-otp
Content-Type: application/json

{
  "phoneNumber": "+1234567890",
  "otp": "123456"
}

Response: 200 OK
{
  "accessToken": "jwt_token",
  "refreshToken": "refresh_token",
  "userId": "uuid",
  "mobileVerified": true,
  "emailVerified": false,
  "profileComplete": false
}
```

#### Login
```http
POST /auth/login
Content-Type: application/json

{
  "phoneNumber": "+1234567890"
}

Response: 200 OK
{
  "userId": "uuid",
  "otpSent": true,
  "expiresAt": "2024-01-01T12:05:00"
}
```

#### Verify Login OTP
```http
POST /auth/verify-login-otp
Content-Type: application/json

{
  "phoneNumber": "+1234567890",
  "otp": "123456"
}

Response: 200 OK
{
  "accessToken": "jwt_token",
  "refreshToken": "refresh_token",
  "userId": "uuid",
  "mobileVerified": true,
  "emailVerified": true,
  "profileComplete": true
}
```

#### Refresh Token
```http
POST /auth/refresh
Content-Type: application/json

{
  "refreshToken": "refresh_token"
}

Response: 200 OK
{
  "accessToken": "new_jwt_token",
  "refreshToken": "new_refresh_token",
  "expiresAt": "2024-01-01T13:00:00"
}
```

#### Logout
```http
POST /auth/logout
Authorization: Bearer {access_token}

Response: 200 OK
{
  "message": "Logged out successfully"
}
```

#### Health Check
```http
GET /auth/health

Response: 200 OK
{
  "status": "UP"
}
```

### Corporate Email Verification APIs

#### Add Corporate Email
```http
POST /auth/add-corporate-email
Authorization: Bearer {access_token}
Content-Type: application/json

{
  "corporateEmail": "user@company.com"
}

Response: 200 OK
{
  "otpSent": true,
  "maskedEmail": "u***@company.com",
  "expiresAt": "2024-01-01T12:10:00"
}
```

#### Verify Email OTP
```http
POST /auth/verify-email-otp
Authorization: Bearer {access_token}
Content-Type: application/json

{
  "otp": "123456"
}

Response: 200 OK
{
  "verified": true,
  "corporateEmail": "user@company.com",
  "profileAccessEnabled": true
}
```

#### Resend Email OTP
```http
POST /auth/resend-email-otp
Authorization: Bearer {access_token}

Response: 200 OK
{
  "otpSent": true,
  "maskedEmail": "u***@company.com",
  "expiresAt": "2024-01-01T12:15:00"
}
```

#### Update Corporate Email
```http
POST /auth/update-corporate-email
Authorization: Bearer {access_token}
Content-Type: application/json

{
  "newCorporateEmail": "user@newcompany.com",
  "mobileOtp": "123456"
}

Response: 200 OK
{
  "otpSent": true,
  "maskedEmail": "u***@newcompany.com",
  "expiresAt": "2024-01-01T12:10:00"
}
```

### Profile Management APIs

#### Get User Profile
```http
GET /users/{userId}/profile
Authorization: Bearer {access_token}

Response: 200 OK
{
  "userId": "uuid",
  "firstName": "John",
  "lastName": "Doe",
  "phoneNumber": "+1234567890",
  "corporateEmail": "john@company.com",
  "profileImageUrl": "https://...",
  "dateOfBirth": "1990-01-01",
  "gender": "MALE",
  "isDriver": true,
  "isRider": true,
  "hasWallet": true,
  "mobileVerified": true,
  "emailVerified": true
}
```

#### Update User Profile
```http
PUT /users/{userId}/profile
Authorization: Bearer {access_token}
Content-Type: application/json

{
  "firstName": "John",
  "lastName": "Smith",
  "dateOfBirth": "1990-01-01",
  "gender": "MALE",
  "profileImageUrl": "https://..."
}

Response: 200 OK
{
  "userId": "uuid",
  "firstName": "John",
  "lastName": "Smith",
  ...
}
```

#### Create Driver Profile
```http
POST /users/{userId}/profile/driver-profile
Authorization: Bearer {access_token}
Content-Type: application/json

{
  "licenseNumber": "DL123456",
  "licenseExpiry": "2028-12-31",
  "vehicleType": "CAR",
  "vehicleMake": "Toyota",
  "vehicleModel": "Camry",
  "vehicleYear": 2022,
  "licensePlate": "ABC123",
  "vehicleCapacity": 4,
  "fuelType": "PETROL",
  "maxDetourDistance": 500,
  "routePreferences": {
    "startLatitude": 37.7749,
    "startLongitude": -122.4194,
    "endLatitude": 37.3382,
    "endLongitude": -121.8863,
    "preferredStartTimes": ["08:00", "17:00"]
  }
}

Response: 201 Created
{
  "driverId": "uuid",
  "licenseNumber": "DL123456",
  "vehicleType": "CAR",
  "vehicleInfo": {...},
  "routePreferences": {...}
}
```

#### Update Driver Profile
```http
PUT /users/{userId}/profile/driver-profile
Authorization: Bearer {access_token}
Content-Type: application/json

{
  "vehicleModel": "Corolla",
  "maxDetourDistance": 300
}

Response: 200 OK
{
  "driverId": "uuid",
  ...
}
```

#### Create Rider Profile
```http
POST /users/{userId}/profile/rider-profile
Authorization: Bearer {access_token}
Content-Type: application/json

{
  "genderPreference": "NO_PREFERENCE",
  "vehicleTypePreferences": ["CAR", "MOTORCYCLE"],
  "routePreferences": {
    "startLatitude": 37.7749,
    "startLongitude": -122.4194,
    "endLatitude": 37.3382,
    "endLongitude": -121.8863,
    "preferredStartTimes": ["08:00", "17:00"]
  }
}

Response: 201 Created
{
  "riderId": "uuid",
  "genderPreference": "NO_PREFERENCE",
  "vehicleTypePreferences": ["CAR", "MOTORCYCLE"],
  "routePreferences": {...}
}
```

#### Update Rider Profile
```http
PUT /users/{userId}/profile/rider-profile
Authorization: Bearer {access_token}
Content-Type: application/json

{
  "genderPreference": "FEMALE_ONLY",
  "vehicleTypePreferences": ["CAR"]
}

Response: 200 OK
{
  "riderId": "uuid",
  ...
}
```

### Wallet Management APIs

#### Get Wallet Status
```http
GET /users/{userId}/wallet
Authorization: Bearer {access_token}

Response: 200 OK
{
  "walletId": "uuid",
  "balance": 100.00,
  "autoReloadEnabled": true,
  "autoReloadThreshold": 50.00,
  "autoReloadAmount": 100.00,
  "bankLinked": false,
  "paymentMethods": [
    {
      "methodId": "uuid",
      "methodType": "CREDIT_CARD",
      "identifier": "****1234",
      "isPrimary": true,
      "isVerified": true
    }
  ]
}
```

#### Add Payment Method
```http
POST /users/{userId}/wallet/payment-methods
Authorization: Bearer {access_token}
Content-Type: application/json

{
  "methodType": "CREDIT_CARD",
  "identifier": "4111111111111111",
  "metadata": {
    "cardholderName": "John Doe",
    "expiryMonth": "12",
    "expiryYear": "2025",
    "cvv": "123"
  }
}

Response: 201 Created
{
  "walletId": "uuid",
  "balance": 100.00,
  "paymentMethods": [...]
}
```

#### Configure Auto-Reload
```http
PUT /users/{userId}/wallet/auto-reload
Authorization: Bearer {access_token}
Content-Type: application/json

{
  "enabled": true,
  "threshold": 50.00,
  "amount": 100.00
}

Response: 200 OK
{
  "walletId": "uuid",
  "autoReloadEnabled": true,
  "autoReloadThreshold": 50.00,
  "autoReloadAmount": 100.00,
  ...
}
```

#### Link Bank Account
```http
POST /users/{userId}/wallet/bank-account
Authorization: Bearer {access_token}
Content-Type: application/json

{
  "accountNumber": "1234567890",
  "routingNumber": "021000021",
  "accountHolderName": "John Doe",
  "bankName": "Chase Bank"
}

Response: 200 OK
{
  "walletId": "uuid",
  "bankLinked": true,
  ...
}
```

### Safety Features APIs

#### Add Emergency Contact
```http
POST /users/{userId}/safety/emergency-contacts
Authorization: Bearer {access_token}
Content-Type: application/json

{
  "name": "Jane Doe",
  "phoneNumber": "+1234567891",
  "relationship": "Spouse",
  "isPrimary": true
}

Response: 201 Created
{
  "contactId": "uuid",
  "name": "Jane Doe",
  "phoneNumber": "+1234567891",
  "relationship": "Spouse",
  "isPrimary": true
}
```

#### Get Emergency Contacts
```http
GET /users/{userId}/safety/emergency-contacts
Authorization: Bearer {access_token}

Response: 200 OK
[
  {
    "contactId": "uuid",
    "name": "Jane Doe",
    "phoneNumber": "+1234567891",
    "relationship": "Spouse",
    "isPrimary": true
  }
]
```

#### Update Emergency Contact
```http
PUT /users/{userId}/safety/emergency-contacts/{contactId}
Authorization: Bearer {access_token}
Content-Type: application/json

{
  "name": "Jane Smith",
  "phoneNumber": "+1234567891"
}

Response: 200 OK
{
  "contactId": "uuid",
  "name": "Jane Smith",
  ...
}
```

#### Delete Emergency Contact
```http
DELETE /users/{userId}/safety/emergency-contacts/{contactId}
Authorization: Bearer {access_token}

Response: 204 No Content
```

#### Add Family Sharing Contact
```http
POST /users/{userId}/safety/family-sharing
Authorization: Bearer {access_token}
Content-Type: application/json

{
  "name": "John Doe Sr.",
  "phoneNumber": "+1234567892",
  "email": "john.sr@email.com",
  "receiveRideUpdates": true
}

Response: 201 Created
{
  "sharingId": "uuid",
  "name": "John Doe Sr.",
  "phoneNumber": "+1234567892",
  "email": "john.sr@email.com",
  "receiveRideUpdates": true
}
```

#### Get Family Sharing Contacts
```http
GET /users/{userId}/safety/family-sharing
Authorization: Bearer {access_token}

Response: 200 OK
[
  {
    "sharingId": "uuid",
    "name": "John Doe Sr.",
    "phoneNumber": "+1234567892",
    "email": "john.sr@email.com",
    "receiveRideUpdates": true
  }
]
```

#### Update Family Sharing Contact
```http
PUT /users/{userId}/safety/family-sharing/{sharingId}
Authorization: Bearer {access_token}
Content-Type: application/json

{
  "receiveRideUpdates": false
}

Response: 200 OK
{
  "sharingId": "uuid",
  ...
}
```

#### Delete Family Sharing Contact
```http
DELETE /users/{userId}/safety/family-sharing/{sharingId}
Authorization: Bearer {access_token}

Response: 204 No Content
```

#### Get Safety Settings
```http
GET /users/{userId}/safety/safety-settings
Authorization: Bearer {access_token}

Response: 200 OK
{
  "userId": "uuid",
  "emergencyContacts": [...],
  "familySharingContacts": [...],
  "sosEnabled": true,
  "locationSharingEnabled": true
}
```

#### Update Safety Settings
```http
PUT /users/{userId}/safety/safety-settings
Authorization: Bearer {access_token}
Content-Type: application/json

{
  "sosEnabled": true,
  "locationSharingEnabled": true
}

Response: 200 OK
{
  "userId": "uuid",
  "sosEnabled": true,
  "locationSharingEnabled": true,
  ...
}
```

### Health & Monitoring APIs

#### Application Health
```http
GET /actuator/health

Response: 200 OK
{
  "status": "UP"
}
```

#### Liveness Probe
```http
GET /actuator/health/liveness

Response: 200 OK
{
  "status": "UP"
}
```

#### Readiness Probe
```http
GET /actuator/health/readiness

Response: 200 OK
{
  "status": "UP"
}
```

#### Application Metrics
```http
GET /actuator/metrics

Response: 200 OK
{
  "names": [
    "jvm.memory.used",
    "http.server.requests",
    ...
  ]
}
```

### API Authentication

Most endpoints require JWT authentication. Include the access token in the Authorization header:

```http
Authorization: Bearer {access_token}
```

### API Response Codes

- `200 OK` - Request successful
- `201 Created` - Resource created successfully
- `204 No Content` - Request successful, no content to return
- `400 Bad Request` - Invalid request data
- `401 Unauthorized` - Missing or invalid authentication
- `403 Forbidden` - Insufficient permissions
- `404 Not Found` - Resource not found
- `409 Conflict` - Resource conflict (e.g., duplicate email)
- `429 Too Many Requests` - Rate limit exceeded
- `500 Internal Server Error` - Server error

### Enum Values

#### Vehicle Types
- `CAR`
- `MOTORCYCLE`
- `SCOOTER`
- `BICYCLE`

#### Fuel Types
- `PETROL`
- `DIESEL`
- `ELECTRIC`
- `HYBRID`
- `CNG`

#### Gender Preferences
- `FEMALE_ONLY` - Female riders prefer female drivers only
- `MALE_SINGLE_FEMALE` - Male riders prefer single female riders
- `MALE_ALL_FEMALE` - Male riders prefer all female riders
- `NO_PREFERENCE` - No gender preference

#### Payment Method Types
- `CREDIT_CARD`
- `BANK_ACCOUNT`
- `UPI`

#### Account Status
- `ACTIVE` - Account is active
- `SUSPENDED` - Account is suspended
- `PENDING_EMAIL` - Awaiting email verification

## Environment Configuration

Key environment variables (see `.env.example` for complete list):

```bash
# Application
SERVER_PORT=8080
SPRING_PROFILES_ACTIVE=dev

# PostgreSQL
POSTGRES_URL=jdbc:postgresql://localhost:5432/officemate_dev
POSTGRES_USERNAME=postgres
POSTGRES_PASSWORD=your_password

# Redis
REDIS_HOST=localhost
REDIS_PORT=6379

# Security
JWT_SECRET=your_jwt_secret_key
JWT_EXPIRATION=3600000

# AWS
AWS_REGION=us-east-1
AWS_ACCESS_KEY_ID=your_access_key
AWS_SECRET_ACCESS_KEY=your_secret_key
```

## Testing

### Run All Tests

```bash
make test
```

### Run Specific Test

```bash
./gradlew test --tests "com.officemate.modules.auth.service.MobileAuthServiceTest"
```

### Integration Tests

```bash
./gradlew integrationTest
```

## Deployment

### Development

```bash
make deploy-dev
```

### Staging

```bash
make deploy-staging
```

### Production

```bash
make deploy-prod
```

Production deployment includes:
- Full test suite execution
- Automatic backup creation
- Health check validation
- Rollback option on failure

## Monitoring

### Health Checks

```bash
# Application health
curl http://localhost:8080/actuator/health

# Liveness probe
curl http://localhost:8080/actuator/health/liveness

# Readiness probe
curl http://localhost:8080/actuator/health/readiness
```

### Logs

```bash
# View all logs
make logs

# View application logs
make docker-logs-app

# View specific service
docker compose logs -f postgres
```

## Troubleshooting

### Application Won't Start

1. Check service health:
   ```bash
   make health-services
   ```

2. View logs:
   ```bash
   make docker-logs
   ```

3. Verify environment variables:
   ```bash
   cat .env
   ```

### Database Connection Issues

```bash
# Check PostgreSQL
docker compose ps postgres

# Test connection
docker compose exec postgres pg_isready -U postgres
```

### Complete Reset

```bash
# Stop and remove all containers and volumes
make docker-down-volumes

# Start fresh
make dev-start
```

See [SETUP.md](SETUP.md) for detailed troubleshooting guide.

## Contributing

1. Create a feature branch
2. Make your changes
3. Write tests
4. Run tests: `make test`
5. Submit pull request

## Architecture

The application follows a modular architecture with clear separation of concerns:

- **Modules**: Feature-based organization (auth, profile, wallet, safety)
- **Shared**: Common utilities and DTOs
- **Config**: Configuration classes
- **Multi-database**: PostgreSQL, Redis, MongoDB, DynamoDB

See [Architecture Documentation](docs/architecture/) for details.

## Security

- JWT-based authentication
- OTP verification for mobile and email
- Password hashing with BCrypt
- Rate limiting
- Input validation and sanitization
- SQL injection prevention
- XSS protection
- CORS configuration

## License

[Add your license here]

## Support

For issues and questions:
- Check [SETUP.md](SETUP.md) for setup help
- Review [DOCKER_DEPLOYMENT.md](DOCKER_DEPLOYMENT.md) for deployment issues
- Check logs: `make docker-logs`
- Run verification: `make verify`

## Project Status

Active development. See `.kiro/specs/` for feature specifications and implementation plans.
