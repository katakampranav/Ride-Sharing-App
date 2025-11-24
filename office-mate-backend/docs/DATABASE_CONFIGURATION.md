# Database Configuration Guide

## Overview

The OfficeMate application uses a hybrid database architecture with multiple data stores optimized for different use cases:

- **PostgreSQL**: Primary relational database for user accounts, profiles, wallets, and transactional data
- **Redis**: In-memory cache for sessions, OTP storage, and rate limiting
- **DynamoDB**: NoSQL database for route preferences and real-time location data
- **MongoDB**: Temporary support for migration from legacy system

## Database Architecture

### PostgreSQL (Primary Database)

**Purpose**: Stores structured relational data requiring ACID compliance

**Tables**:
- `user_accounts`: Phone numbers, corporate emails, verification status
- `user_profiles`: Basic user information
- `driver_profiles`: Driver-specific data with vehicle information
- `rider_profiles`: Rider preferences and settings
- `wallets`: Payment wallet data
- `payment_methods`: Payment method details
- `emergency_contacts`: Emergency contact information
- `family_sharing_contacts`: Family sharing settings
- `email_verifications`: Email verification records with expiry

**Configuration**:
```yaml
spring:
  datasource:
    postgresql:
      jdbc-url: jdbc:postgresql://localhost:5432/officemate
      username: postgres
      password: postgres
```

### Redis (Session & Cache)

**Purpose**: Fast in-memory storage for temporary data

**Use Cases**:
- User session storage with TTL
- OTP storage (5-minute expiration)
- Rate limiting counters
- Frequently accessed data caching

**Configuration**:
```yaml
spring:
  data:
    redis:
      host: localhost
      port: 6379
      database: 0
```

### DynamoDB (NoSQL)

**Purpose**: Scalable NoSQL storage for dynamic data

**Tables**:
- `route_preferences`: User route preferences with geospatial data
- `real_time_locations`: GPS coordinates during active rides
- `user_matching_data`: Matching preferences and history

**Configuration**:
```yaml
aws:
  region: us-east-1
  dynamodb:
    endpoint: http://localhost:4566  # LocalStack for development
    table-prefix: officemate
```

### MongoDB (Migration Support)

**Purpose**: Temporary support during migration from legacy system

**Configuration**:
```yaml
spring:
  data:
    mongodb:
      uri: mongodb://localhost:27017/officemate
      database: officemate
```

## Environment-Specific Configuration

### Development (`application-dev.yml`)

- Uses local PostgreSQL, Redis, MongoDB
- LocalStack for AWS services (DynamoDB, SNS, SES)
- Debug logging enabled
- H2 console enabled for testing

### Test (`application-test.yml`)

- Uses H2 in-memory database for PostgreSQL
- Embedded Redis for testing
- LocalStack for AWS services
- Minimal logging

### Production (`application-prod.yml`)

- Uses managed AWS RDS PostgreSQL
- AWS ElastiCache for Redis
- AWS DynamoDB
- SSL enabled for Redis
- Error details hidden from responses
- Production logging levels

## Running Locally

### Prerequisites

1. **PostgreSQL**:
```bash
docker run -d --name postgres \
  -e POSTGRES_PASSWORD=postgres \
  -e POSTGRES_DB=officemate \
  -p 5432:5432 \
  postgres:15
```

2. **Redis**:
```bash
docker run -d --name redis \
  -p 6379:6379 \
  redis:7-alpine
```

3. **MongoDB** (for migration):
```bash
docker run -d --name mongodb \
  -p 27017:27017 \
  mongo:7
```

4. **LocalStack** (for AWS services):
```bash
docker run -d --name localstack \
  -p 4566:4566 \
  -e SERVICES=dynamodb,sns,ses,kms \
  localstack/localstack
```

### Using Docker Compose

A `docker-compose.yml` file is provided for easy local setup:

```bash
docker-compose up -d
```

## Connection Pooling

PostgreSQL uses HikariCP for connection pooling:

- **Maximum Pool Size**: 10 (dev), 20 (prod)
- **Minimum Idle**: 5 (dev), 10 (prod)
- **Connection Timeout**: 30 seconds
- **Idle Timeout**: 10 minutes
- **Max Lifetime**: 30 minutes

## Migration Strategy

The application supports gradual migration from MongoDB to PostgreSQL:

1. **Phase 1**: Dual-write to both MongoDB and PostgreSQL
2. **Phase 2**: Read from PostgreSQL, fallback to MongoDB
3. **Phase 3**: PostgreSQL only, MongoDB decommissioned

## Security

- All database connections use SSL in production
- Credentials stored in environment variables
- Connection strings never hardcoded
- Redis password-protected in production
- AWS services use IAM roles for authentication

## Monitoring

Key metrics to monitor:

- PostgreSQL connection pool utilization
- Redis memory usage and hit rate
- DynamoDB read/write capacity units
- Query performance and slow queries
- Connection errors and timeouts

## Troubleshooting

### PostgreSQL Connection Issues

```bash
# Check if PostgreSQL is running
docker ps | grep postgres

# View PostgreSQL logs
docker logs postgres

# Test connection
psql -h localhost -U postgres -d officemate
```

### Redis Connection Issues

```bash
# Check if Redis is running
docker ps | grep redis

# Test connection
redis-cli ping
```

### DynamoDB Local Issues

```bash
# Check LocalStack status
curl http://localhost:4566/_localstack/health

# List DynamoDB tables
aws dynamodb list-tables --endpoint-url http://localhost:4566
```

## Additional Resources

- [PostgreSQL Documentation](https://www.postgresql.org/docs/)
- [Redis Documentation](https://redis.io/documentation)
- [DynamoDB Documentation](https://docs.aws.amazon.com/dynamodb/)
- [Spring Data JPA](https://spring.io/projects/spring-data-jpa)
- [Spring Data Redis](https://spring.io/projects/spring-data-redis)
