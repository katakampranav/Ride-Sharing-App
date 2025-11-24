# Officemate Backend Setup Guide

Complete setup guide for the Officemate Spring Boot application.

## Table of Contents

- [Prerequisites](#prerequisites)
- [Quick Start](#quick-start)
- [Detailed Setup](#detailed-setup)
- [Running the Application](#running-the-application)
- [Development Workflow](#development-workflow)
- [Testing](#testing)
- [Troubleshooting](#troubleshooting)

## Prerequisites

### Required Software

1. **Java Development Kit (JDK) 19**
   - Download from [Eclipse Temurin](https://adoptium.net/)
   - Verify: `java -version`

2. **Docker Desktop**
   - Download from [Docker](https://www.docker.com/products/docker-desktop)
   - Verify: `docker --version` and `docker compose version`

3. **Git**
   - Download from [Git](https://git-scm.com/)
   - Verify: `git --version`

### Optional Tools

- **PostgreSQL Client** (for database access)
- **Redis CLI** (for cache inspection)
- **Postman** or **curl** (for API testing)
- **IDE** (IntelliJ IDEA, VS Code, Eclipse)

## Quick Start

### 1. Clone Repository

```bash
git clone <repository-url>
cd officemate-backend
```

### 2. Setup Environment

```bash
# Create .env file from template
make setup

# Or manually
cp .env.example .env
```

### 3. Configure Environment

Edit `.env` file and update:
- Database passwords
- JWT secret (minimum 256 bits)
- AWS credentials (or use LocalStack defaults)

### 4. Start Infrastructure

```bash
# Start all infrastructure services
make docker-up

# Wait for services to be healthy (about 30 seconds)
```

### 5. Initialize Database

```bash
# Run database migrations
make init-db
```

### 6. Run Application

```bash
# Run locally (recommended for development)
make run

# Or run in Docker
make docker-up-full
```

### 7. Verify Setup

```bash
# Check application health
curl http://localhost:8080/actuator/health

# Or use the verification script
make verify
```

## Detailed Setup

### Step 1: Environment Configuration

The `.env` file contains all configuration variables. Key sections:

#### Database Configuration
```bash
POSTGRES_URL=jdbc:postgresql://localhost:5432/officemate_dev
POSTGRES_USERNAME=postgres
POSTGRES_PASSWORD=your_secure_password  # CHANGE THIS
```

#### Security Configuration
```bash
JWT_SECRET=your_jwt_secret_key_minimum_256_bits  # CHANGE THIS
JWT_EXPIRATION=3600000  # 1 hour
JWT_REFRESH_EXPIRATION=86400000  # 24 hours
```

#### AWS Configuration (LocalStack for Development)
```bash
AWS_REGION=us-east-1
AWS_ACCESS_KEY_ID=test
AWS_SECRET_ACCESS_KEY=test
DYNAMODB_ENDPOINT=http://localhost:4566
```

### Step 2: Docker Services

Start infrastructure services:

```bash
# Start services
docker compose up -d

# Check status
docker compose ps

# View logs
docker compose logs -f
```

Services started:
- **PostgreSQL** (port 5432) - Primary database
- **Redis** (port 6379) - Session storage and OTP caching
- **MongoDB** (port 27017) - Migration support
- **LocalStack** (port 4566) - AWS services emulation

### Step 3: Database Initialization

#### Automatic Initialization

Database schema is created automatically on first startup via init scripts in `scripts/db/init/`.

#### Manual Migration

Run migrations manually:

```bash
# Run all pending migrations
bash scripts/db/run-migrations.sh

# Or use Makefile
make migrate
```

#### Verify Database

```bash
# Connect to PostgreSQL
docker compose exec postgres psql -U postgres -d officemate_dev

# List tables
\dt

# Check migrations
SELECT * FROM schema_migrations;

# Exit
\q
```

### Step 4: DynamoDB Setup

Initialize DynamoDB tables in LocalStack:

```bash
# Run initialization script
bash scripts/init-dynamodb.sh

# Verify tables
aws dynamodb list-tables --endpoint-url http://localhost:4566 --region us-east-1
```

## Running the Application

### Option 1: Run Locally (Recommended for Development)

```bash
# Using Makefile
make run

# Or using Gradle directly
./gradlew bootRun

# With specific profile
SPRING_PROFILES_ACTIVE=dev ./gradlew bootRun
```

**Advantages:**
- Faster iteration (no container rebuild)
- Easier debugging
- Direct IDE integration
- Hot reload support

### Option 2: Run in Docker

```bash
# Build and start application container
make docker-up-full

# View application logs
make docker-logs-app

# Stop application
make docker-down
```

**Advantages:**
- Production-like environment
- Isolated from host system
- Easy to share exact environment

### Accessing the Application

Once running, access:
- **Application:** http://localhost:8080
- **Health Check:** http://localhost:8080/actuator/health
- **API Documentation:** http://localhost:8080/swagger-ui.html (if configured)

## Development Workflow

### Daily Development

```bash
# 1. Start infrastructure (once per day)
make docker-up

# 2. Run application locally
make run

# 3. Make code changes
# Application auto-reloads with Spring DevTools

# 4. Run tests
make test

# 5. Stop infrastructure (end of day)
make docker-down
```

### Making Database Changes

```bash
# 1. Create new migration file
cat > scripts/db/migrations/V3__add_new_feature.sql << 'EOF'
-- Add new feature
CREATE TABLE new_feature (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    name VARCHAR(255) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
EOF

# 2. Run migration
make migrate

# 3. Verify in database
docker compose exec postgres psql -U postgres -d officemate_dev -c "\d new_feature"
```

### Resetting Development Environment

```bash
# Complete reset (removes all data)
make dev-reset

# Or manually
make docker-down-volumes
make docker-up
make init-db
```

## Testing

### Unit Tests

```bash
# Run all tests
make test

# Or with Gradle
./gradlew test

# Run specific test class
./gradlew test --tests "com.officemate.modules.auth.service.MobileAuthServiceTest"
```

### Integration Tests

```bash
# Run integration tests
./gradlew integrationTest

# With Testcontainers (requires Docker)
./gradlew test -Dspring.profiles.active=test
```

### Manual API Testing

```bash
# Health check
curl http://localhost:8080/actuator/health

# Register user (example)
curl -X POST http://localhost:8080/auth/register \
  -H "Content-Type: application/json" \
  -d '{"phoneNumber": "+1234567890"}'
```

## Troubleshooting

### Application Won't Start

**Check Java version:**
```bash
java -version
# Should be Java 19
```

**Check if port 8080 is in use:**
```bash
# Windows
netstat -ano | findstr :8080

# Linux/Mac
lsof -i :8080
```

**Solution:** Change port in `.env`:
```bash
SERVER_PORT=8081
```

### Database Connection Failed

**Check PostgreSQL is running:**
```bash
docker compose ps postgres
```

**Check connection:**
```bash
docker compose exec postgres pg_isready -U postgres
```

**View PostgreSQL logs:**
```bash
docker compose logs postgres
```

**Solution:** Restart PostgreSQL:
```bash
docker compose restart postgres
```

### Redis Connection Failed

**Check Redis is running:**
```bash
docker compose ps redis
```

**Test connection:**
```bash
docker compose exec redis redis-cli ping
# Should return: PONG
```

**Solution:** Restart Redis:
```bash
docker compose restart redis
```

### LocalStack Issues

**Check LocalStack health:**
```bash
curl http://localhost:4566/_localstack/health
```

**Reinitialize DynamoDB:**
```bash
bash scripts/init-dynamodb.sh
```

### Build Failures

**Clean and rebuild:**
```bash
make clean
make build
```

**Clear Gradle cache:**
```bash
./gradlew clean --refresh-dependencies
```

### Docker Issues

**Check Docker is running:**
```bash
docker info
```

**Clean Docker resources:**
```bash
# Remove stopped containers
docker container prune

# Remove unused images
docker image prune

# Remove unused volumes
docker volume prune
```

**Complete Docker reset:**
```bash
make docker-down-volumes
docker system prune -a
make docker-up
```

### Migration Failures

**Check migration status:**
```bash
docker compose exec postgres psql -U postgres -d officemate_dev \
  -c "SELECT * FROM schema_migrations ORDER BY applied_at DESC"
```

**Manually fix migration:**
1. Edit the migration file
2. Delete the failed migration record from `schema_migrations`
3. Re-run migrations

```bash
# Delete failed migration record
docker compose exec postgres psql -U postgres -d officemate_dev \
  -c "DELETE FROM schema_migrations WHERE version = 'V3__failed_migration'"

# Re-run migrations
make migrate
```

## Additional Resources

### Documentation
- [Docker Deployment Guide](DOCKER_DEPLOYMENT.md)
- [Containerization Summary](CONTAINERIZATION_SUMMARY.md)
- [API Documentation](docs/API.md) (if available)
- [Architecture Documentation](docs/architecture/)

### Useful Commands

```bash
# View all available make commands
make help

# Check setup verification
make verify

# View service health
make health-services

# View application logs
make docker-logs-app

# Access PostgreSQL shell
docker compose exec postgres psql -U postgres -d officemate_dev

# Access Redis CLI
docker compose exec redis redis-cli

# Access MongoDB shell
docker compose exec mongodb mongosh officemate_dev
```

### Getting Help

1. Check logs: `make docker-logs`
2. Run verification: `make verify`
3. Check documentation in `docs/` directory
4. Review error messages carefully
5. Search for similar issues in project documentation

## Next Steps

After successful setup:

1. **Explore the API**
   - Review API endpoints
   - Test with Postman or curl
   - Read API documentation

2. **Understand the Architecture**
   - Review design documents in `docs/architecture/`
   - Understand data models
   - Review service interactions

3. **Start Development**
   - Pick a task from the backlog
   - Create a feature branch
   - Follow development workflow
   - Write tests
   - Submit pull request

4. **Configure IDE**
   - Import Gradle project
   - Configure code style
   - Set up debugging
   - Install recommended plugins

Happy coding! 🚀
