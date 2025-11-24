# Containerization Summary

This document provides an overview of the containerization and deployment setup for the Officemate Spring Boot application.

## Overview

The Officemate application is fully containerized using Docker and Docker Compose, supporting multiple deployment environments (development, staging, production) with automated deployment scripts and database migrations.

## Architecture

```
┌─────────────────────────────────────────────────────────────┐
│                     Docker Network                           │
│                                                              │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐     │
│  │  PostgreSQL  │  │    Redis     │  │   MongoDB    │     │
│  │   (Primary)  │  │   (Cache)    │  │ (Migration)  │     │
│  └──────────────┘  └──────────────┘  └──────────────┘     │
│         │                 │                  │              │
│         └─────────────────┴──────────────────┘              │
│                           │                                 │
│                  ┌────────▼────────┐                        │
│                  │  Spring Boot    │                        │
│                  │  Application    │                        │
│                  └────────┬────────┘                        │
│                           │                                 │
│                  ┌────────▼────────┐                        │
│                  │   LocalStack    │                        │
│                  │ (AWS Services)  │                        │
│                  └─────────────────┘                        │
└─────────────────────────────────────────────────────────────┘
```

## Components

### 1. Docker Images

#### Development Image (Dockerfile)
- **Purpose:** Fast iteration during development
- **Base:** eclipse-temurin:19-jre-alpine
- **Size:** ~200-250 MB
- **Features:**
  - Multi-stage build with Gradle caching
  - Skips tests for faster builds
  - Health check endpoint
  - Non-root user execution
  - Alpine Linux for minimal footprint

#### Production Image (Dockerfile.prod)
- **Purpose:** Production-ready deployment
- **Base:** eclipse-temurin:19-jre-alpine
- **Features:**
  - Full test suite execution during build
  - Production-optimized JVM settings
  - G1GC garbage collector
  - String deduplication
  - Enhanced security hardening
  - Comprehensive health checks

### 2. Docker Compose Configurations

#### docker-compose.yml (Development)
Services:
- **postgres**: PostgreSQL 15 (primary database)
- **redis**: Redis 7 (session storage, OTP caching)
- **mongodb**: MongoDB 7 (migration support)
- **localstack**: AWS services emulation
- **app**: Application (optional with `--profile full`)

Features:
- Health checks for all services
- Volume persistence
- Port mapping for local access
- Development-friendly configuration

#### docker-compose.prod.yml (Production)
Additional features:
- Resource limits (CPU: 2 cores, Memory: 2GB)
- Restart policies (unless-stopped)
- Secure password configuration
- Log volume mounts
- Production-optimized settings
- Network isolation

### 3. Database Migrations

**Location:** `scripts/db/migrations/`

**Naming Convention:** `V{version}__{description}.sql`

**Tracking:** PostgreSQL `schema_migrations` table

**Execution:**
- Automatic on container startup (via init scripts)
- Manual via `make migrate` or `scripts/db/run-migrations.sh`

**Current Migrations:**
- V1: Initial schema (user accounts, profiles, wallets, etc.)
- V2: Session metadata tables

### 4. Deployment Scripts

#### scripts/deploy-dev.sh
- Stops existing containers
- Starts infrastructure services
- Initializes databases
- Optionally starts application in Docker
- Interactive prompts for user control

#### scripts/deploy-staging.sh
- Runs test suite
- Builds production image
- Deploys to staging environment
- Runs database migrations
- Performs health checks

#### scripts/deploy-prod.sh
- **Safety Features:**
  - Double confirmation required
  - Full test suite execution
  - Automatic backup creation
  - Graceful shutdown (30s timeout)
  - Health check validation with retries
  - Rollback option on failure
  
- **Deployment Steps:**
  1. Confirm deployment (double check)
  2. Run full test suite
  3. Build production image
  4. Tag with version
  5. Create backup
  6. Stop existing containers
  7. Start new containers
  8. Run migrations
  9. Health checks (20 retries)
  10. Smoke tests

### 5. Makefile Commands

**Setup & Build:**
- `make setup` - Create .env from template
- `make build` - Build application with Gradle
- `make test` - Run test suite
- `make clean` - Clean build artifacts

**Docker Operations:**
- `make docker-build` - Build development image
- `make docker-build-prod` - Build production image
- `make docker-up` - Start infrastructure services
- `make docker-up-full` - Start all services including app
- `make docker-down` - Stop all services
- `make docker-logs` - View logs

**Database:**
- `make migrate` - Run database migrations
- `make init-db` - Initialize databases

**Development Workflow:**
- `make dev-start` - Start development environment
- `make dev-stop` - Stop development environment
- `make dev-reset` - Reset development environment

**Deployment:**
- `make deploy-dev` - Deploy to development
- `make deploy-staging` - Deploy to staging
- `make deploy-prod` - Deploy to production

**Monitoring:**
- `make health` - Check application health
- `make health-services` - Check all services health

## Environment Configuration

### Configuration Files

- `.env.example` - Template with all variables
- `.env` - Development (local)
- `.env.staging` - Staging environment
- `.env.production` - Production environment

### Key Configuration Areas

1. **Database Configuration**
   - PostgreSQL connection settings
   - Connection pool configuration
   - MongoDB URI for migration
   - Redis connection settings

2. **AWS Configuration**
   - Region and credentials
   - DynamoDB endpoints and tables
   - SNS settings for SMS
   - SES settings for email
   - KMS encryption keys

3. **Security Configuration**
   - JWT secrets and expiration
   - Account lockout settings
   - Rate limiting configuration
   - CAPTCHA settings

4. **Application Configuration**
   - Server port and threading
   - OTP settings
   - Validation rules
   - Vehicle and profile settings

## Health Checks

### Application Health Endpoints

- `/actuator/health` - Overall health status
- `/actuator/health/liveness` - Liveness probe
- `/actuator/health/readiness` - Readiness probe
- `/actuator/info` - Application information
- `/actuator/metrics` - Application metrics

### Service Health Checks

All services include Docker health checks:

**PostgreSQL:**
```bash
pg_isready -U postgres
```

**Redis:**
```bash
redis-cli ping
```

**MongoDB:**
```bash
mongosh --eval "db.adminCommand('ping')"
```

**LocalStack:**
```bash
curl -f http://localhost:4566/_localstack/health
```

**Application:**
```bash
curl -f http://localhost:8080/actuator/health
```

## Volumes and Persistence

### Development Volumes
- `postgres_data` - PostgreSQL data
- `redis_data` - Redis persistence
- `mongodb_data` - MongoDB data
- `localstack_data` - LocalStack state

### Production Volumes
- `postgres_data` - PostgreSQL data
- `redis_data` - Redis persistence
- `mongodb_data` - MongoDB data
- `app_logs` - Application logs

## Networking

### Development Network
- **Name:** officemate-network
- **Driver:** bridge
- **Services:** All services on same network

### Production Network
- **Name:** officemate-prod-network
- **Driver:** bridge
- **Isolation:** Network-level isolation

## Security Features

### Container Security
1. **Non-root User:** Application runs as `spring:spring`
2. **Read-only Filesystem:** Where possible
3. **Resource Limits:** CPU and memory constraints
4. **Health Checks:** Automatic restart on failure
5. **Secrets Management:** Environment variables (not hardcoded)

### Image Security
1. **Minimal Base Image:** Alpine Linux
2. **No Unnecessary Packages:** Only required dependencies
3. **Regular Updates:** Base image updates
4. **Vulnerability Scanning:** Recommended before deployment

### Network Security
1. **Internal Network:** Services communicate internally
2. **Port Exposure:** Only necessary ports exposed
3. **Firewall Rules:** Configure host firewall

## Monitoring and Logging

### Application Logs
- **Location:** `/app/logs` (in container)
- **Volume Mount:** `app_logs` volume
- **Format:** Structured JSON (Logstash format)
- **Levels:** Configurable per environment

### Docker Logs
```bash
# View all logs
docker compose logs -f

# View specific service
docker compose logs -f app

# View last 100 lines
docker compose logs --tail=100 app
```

### Log Aggregation
- Logstash encoder configured
- Ready for ELK stack integration
- Structured logging with context

## Backup and Recovery

### Database Backups

**Manual Backup:**
```bash
docker compose exec postgres pg_dump -U postgres officemate_dev > backup.sql
```

**Restore:**
```bash
docker compose exec -T postgres psql -U postgres officemate_dev < backup.sql
```

### Volume Backups

**Backup volumes:**
```bash
docker run --rm -v postgres_data:/data -v $(pwd):/backup alpine tar czf /backup/postgres_backup.tar.gz /data
```

### Deployment Backups

Production deployment script automatically creates backups in `backups/` directory with timestamp.

## Performance Optimization

### JVM Settings

**Development:**
- `-XX:MaxRAMPercentage=75.0`
- Container-aware memory management

**Production:**
- `-XX:+UseG1GC` - G1 garbage collector
- `-XX:+UseStringDeduplication` - Reduce memory usage
- `-XX:+OptimizeStringConcat` - String optimization
- `-XX:MaxRAMPercentage=75.0` - Memory limits

### Database Connection Pooling

**Development:**
- Max Pool Size: 5
- Min Idle: 2

**Production:**
- Max Pool Size: 20
- Min Idle: 5
- Connection timeout: 30s

### Redis Configuration

**Development:**
- Max Active: 8
- Max Idle: 8

**Production:**
- Max Active: 16
- Max Idle: 8
- Timeout: 2000ms

## Troubleshooting

### Common Issues

1. **Port Conflicts**
   - Check with `netstat` or `lsof`
   - Change ports in `.env`

2. **Database Connection Failures**
   - Verify service health
   - Check credentials
   - Review logs

3. **Application Won't Start**
   - Check dependencies health
   - Review application logs
   - Verify environment variables

4. **Migration Failures**
   - Check migration syntax
   - Verify database state
   - Review migration logs

### Debug Commands

```bash
# Check service status
docker compose ps

# View service logs
docker compose logs [service]

# Execute commands in container
docker compose exec [service] [command]

# Inspect container
docker inspect [container]

# Check resource usage
docker stats
```

## Best Practices

### Development
1. Use infrastructure services in Docker
2. Run application locally for faster iteration
3. Keep `.env` file updated
4. Regular database backups

### Production
1. Always run tests before deployment
2. Use production Dockerfile
3. Set appropriate resource limits
4. Enable monitoring and alerting
5. Regular security updates
6. Automated backups
7. Test rollback procedures

### Security
1. Never commit secrets
2. Use strong passwords
3. Rotate credentials regularly
4. Keep images updated
5. Scan for vulnerabilities
6. Follow principle of least privilege

## Future Enhancements

### Planned Improvements
1. Kubernetes deployment manifests
2. Helm charts for easier deployment
3. CI/CD pipeline integration
4. Automated security scanning
5. Performance monitoring dashboards
6. Automated backup scheduling
7. Blue-green deployment support
8. Canary deployment strategy

### Monitoring Enhancements
1. Prometheus metrics export
2. Grafana dashboards
3. ELK stack integration
4. Distributed tracing (Jaeger/Zipkin)
5. APM integration

## Conclusion

The containerization setup provides:
- ✅ Consistent environments across dev/staging/prod
- ✅ Easy local development setup
- ✅ Automated deployment workflows
- ✅ Database migration management
- ✅ Health monitoring and checks
- ✅ Security best practices
- ✅ Comprehensive documentation
- ✅ Troubleshooting guides

The setup is production-ready and follows industry best practices for containerized Spring Boot applications.
