# Docker Deployment Guide

This guide covers containerization and deployment of the Officemate Spring Boot application using Docker and Docker Compose.

## Table of Contents

- [Prerequisites](#prerequisites)
- [Quick Start](#quick-start)
- [Docker Images](#docker-images)
- [Docker Compose](#docker-compose)
- [Environment Configuration](#environment-configuration)
- [Database Migrations](#database-migrations)
- [Deployment Workflows](#deployment-workflows)
- [Monitoring and Health Checks](#monitoring-and-health-checks)
- [Troubleshooting](#troubleshooting)

## Prerequisites

- Docker 20.10 or higher
- Docker Compose V2
- Gradle 8.5 or higher (for local builds)
- Bash shell (for running scripts)

### Verify Installation

```bash
docker --version
docker compose version
./gradlew --version
```

## Quick Start

### 1. Initial Setup

```bash
# Create environment file from template
make setup

# Edit .env file with your configuration
# Update database passwords, JWT secrets, AWS credentials, etc.
```

### 2. Start Development Environment

```bash
# Start infrastructure services only (PostgreSQL, Redis, MongoDB, LocalStack)
make docker-up

# Initialize databases
make init-db

# Run application locally
make run
```

### 3. Start Full Environment (Application in Docker)

```bash
# Start all services including application
make docker-up-full

# View logs
make docker-logs
```

## Docker Images

### Development Image (Dockerfile)

The development Dockerfile is optimized for fast iteration:

- Multi-stage build with Gradle caching
- Skips tests for faster builds
- Uses Alpine-based JRE for smaller image size
- Includes health check endpoint
- Runs as non-root user for security

**Build:**
```bash
make docker-build
# or
docker build -t officemate:latest .
```

### Production Image (Dockerfile.prod)

The production Dockerfile includes additional optimizations:

- Runs full test suite during build
- Production-optimized JVM settings
- G1GC garbage collector
- String deduplication enabled
- Longer health check intervals
- Timezone configuration

**Build:**
```bash
make docker-build-prod
# or
docker build -f Dockerfile.prod -t officemate:prod .
```

### Image Details

- **Base Image:** eclipse-temurin:19-jre-alpine
- **Size:** ~200-250 MB
- **User:** Non-root (spring:spring)
- **Port:** 8080
- **Health Check:** /actuator/health

## Docker Compose

### Development (docker-compose.yml)

Services included:
- **postgres**: PostgreSQL 15 database
- **redis**: Redis 7 for caching and sessions
- **mongodb**: MongoDB 7 for migration support
- **localstack**: AWS services emulation (DynamoDB, SNS, SES, KMS)
- **app**: Application (optional, use `--profile full`)

**Commands:**
```bash
# Start infrastructure only
docker compose up -d

# Start all services
docker compose --profile full up -d

# Stop services
docker compose down

# View logs
docker compose logs -f

# Check status
docker compose ps
```

### Production (docker-compose.prod.yml)

Production configuration includes:
- Resource limits (CPU and memory)
- Longer health check intervals
- Restart policies
- Volume mounts for logs
- Secure password configuration
- Production-optimized settings

**Commands:**
```bash
# Start production services
docker compose -f docker-compose.prod.yml up -d

# View logs
docker compose -f docker-compose.prod.yml logs -f

# Stop services
docker compose -f docker-compose.prod.yml down
```

## Environment Configuration

### Environment Files

Create environment-specific files:

- `.env` - Development (local)
- `.env.staging` - Staging environment
- `.env.production` - Production environment

### Required Variables

**Database:**
```bash
POSTGRES_URL=jdbc:postgresql://localhost:5432/officemate_dev
POSTGRES_USERNAME=postgres
POSTGRES_PASSWORD=your_secure_password
```

**Redis:**
```bash
REDIS_HOST=localhost
REDIS_PORT=6379
REDIS_PASSWORD=your_redis_password
```

**AWS:**
```bash
AWS_REGION=us-east-1
AWS_ACCESS_KEY_ID=your_access_key
AWS_SECRET_ACCESS_KEY=your_secret_key
```

**Security:**
```bash
JWT_SECRET=your_jwt_secret_key_minimum_256_bits
```

See `.env.example` for complete list of configuration options.

## Database Migrations

### Running Migrations

Migrations are SQL files in `scripts/db/migrations/` directory.

**Automatic (Docker):**
```bash
# Migrations run automatically on first container start
docker compose up -d postgres
```

**Manual:**
```bash
# Run migration script
make migrate

# Or directly
bash scripts/db/run-migrations.sh
```

### Migration Files

Naming convention: `V{version}__{description}.sql`

Example:
- `V1__initial_schema.sql`
- `V2__add_session_metadata.sql`
- `V3__add_wallet_features.sql`

### Creating New Migrations

1. Create new SQL file in `scripts/db/migrations/`
2. Follow naming convention
3. Test locally first
4. Run migration script

```bash
# Create new migration
cat > scripts/db/migrations/V3__add_new_feature.sql << 'EOF'
-- Add new feature
CREATE TABLE new_feature (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    name VARCHAR(255) NOT NULL
);
EOF

# Run migrations
make migrate
```

## Deployment Workflows

### Development Deployment

```bash
# Using Makefile
make dev-start

# Or using script
bash scripts/deploy-dev.sh
```

This will:
1. Stop existing containers
2. Start infrastructure services
3. Initialize databases
4. Optionally start application in Docker

### Staging Deployment

```bash
# Using Makefile
make deploy-staging

# Or using script
bash scripts/deploy-staging.sh
```

This will:
1. Run tests
2. Build production image
3. Deploy to staging environment
4. Run migrations
5. Perform health checks

### Production Deployment

```bash
# Using Makefile
make deploy-prod

# Or using script
bash scripts/deploy-prod.sh
```

This will:
1. Run full test suite
2. Build production image
3. Create backup
4. Deploy with zero-downtime
5. Run migrations
6. Perform health checks
7. Run smoke tests

**Safety Features:**
- Double confirmation required
- Automatic backup creation
- Health check validation
- Rollback option on failure

## Monitoring and Health Checks

### Health Endpoints

**Liveness Probe:**
```bash
curl http://localhost:8080/actuator/health/liveness
```

**Readiness Probe:**
```bash
curl http://localhost:8080/actuator/health/readiness
```

**Full Health:**
```bash
curl http://localhost:8080/actuator/health
```

### Service Health Checks

**Check all services:**
```bash
make health-services
```

**Individual services:**
```bash
# PostgreSQL
docker compose exec postgres pg_isready -U postgres

# Redis
docker compose exec redis redis-cli ping

# MongoDB
docker compose exec mongodb mongosh --eval "db.adminCommand('ping')"
```

### Logs

**View all logs:**
```bash
make logs
```

**View application logs:**
```bash
make docker-logs-app
```

**Follow specific service:**
```bash
docker compose logs -f postgres
```

## Troubleshooting

### Application Won't Start

1. Check service health:
```bash
docker compose ps
make health-services
```

2. View application logs:
```bash
docker compose logs app
```

3. Check database connection:
```bash
docker compose exec postgres psql -U postgres -d officemate_dev -c "SELECT 1"
```

### Database Connection Issues

1. Verify PostgreSQL is running:
```bash
docker compose ps postgres
```

2. Check connection from host:
```bash
psql -h localhost -p 5432 -U postgres -d officemate_dev
```

3. Verify environment variables:
```bash
docker compose exec app env | grep POSTGRES
```

### Redis Connection Issues

1. Test Redis connection:
```bash
docker compose exec redis redis-cli ping
```

2. Check Redis logs:
```bash
docker compose logs redis
```

### Port Conflicts

If ports are already in use:

1. Check what's using the port:
```bash
# Windows
netstat -ano | findstr :8080

# Linux/Mac
lsof -i :8080
```

2. Change port in `.env`:
```bash
SERVER_PORT=8081
```

### Clean Restart

```bash
# Stop and remove all containers and volumes
make docker-down-volumes

# Start fresh
make dev-start
```

### Image Build Failures

1. Clear Docker cache:
```bash
docker builder prune -a
```

2. Rebuild without cache:
```bash
docker build --no-cache -t officemate:latest .
```

### Migration Failures

1. Check migration status:
```bash
docker compose exec postgres psql -U postgres -d officemate_dev -c "SELECT * FROM schema_migrations ORDER BY applied_at DESC"
```

2. Manually fix and re-run:
```bash
# Fix the migration file
# Then run
make migrate
```

## Best Practices

### Development

1. Use `make dev-start` for consistent setup
2. Run application locally, not in Docker (faster iteration)
3. Use Docker only for infrastructure services
4. Keep `.env` file updated

### Production

1. Always run tests before deployment
2. Use production Dockerfile
3. Set resource limits
4. Enable monitoring and logging
5. Use secrets management (not .env files)
6. Regular backups
7. Test rollback procedures

### Security

1. Never commit `.env` files
2. Use strong passwords
3. Rotate secrets regularly
4. Run as non-root user
5. Keep images updated
6. Scan for vulnerabilities

## Additional Resources

- [Docker Documentation](https://docs.docker.com/)
- [Docker Compose Documentation](https://docs.docker.com/compose/)
- [Spring Boot Docker Guide](https://spring.io/guides/topicals/spring-boot-docker/)
- [PostgreSQL Docker Hub](https://hub.docker.com/_/postgres)
- [Redis Docker Hub](https://hub.docker.com/_/redis)
