#!/bin/bash

# Production deployment script
# This script deploys the application to production environment

set -e

echo "=========================================="
echo "Officemate Production Deployment"
echo "=========================================="
echo ""

# Colors for output
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Check if Docker is running
if ! docker info > /dev/null 2>&1; then
    echo -e "${RED}Error: Docker is not running${NC}"
    exit 1
fi

# Load environment variables
if [ -f .env.production ]; then
    echo -e "${GREEN}✓ Loading environment variables from .env.production${NC}"
    export $(cat .env.production | grep -v '^#' | xargs)
else
    echo -e "${RED}Error: .env.production file not found${NC}"
    exit 1
fi

# Confirm deployment with double check
echo -e "${RED}⚠⚠⚠ WARNING: You are about to deploy to PRODUCTION environment ⚠⚠⚠${NC}"
echo ""
read -p "Are you absolutely sure you want to continue? (yes/no) " -r
echo
if [[ ! $REPLY == "yes" ]]; then
    echo "Deployment cancelled"
    exit 0
fi

# Second confirmation
echo ""
echo -e "${YELLOW}This is your last chance to cancel.${NC}"
read -p "Type 'DEPLOY' to confirm: " -r
echo
if [[ ! $REPLY == "DEPLOY" ]]; then
    echo "Deployment cancelled"
    exit 0
fi

# Run full test suite
echo ""
echo -e "${BLUE}Running full test suite...${NC}"
./gradlew clean test
echo -e "${GREEN}✓ All tests passed${NC}"

# Build production Docker image
echo ""
echo -e "${BLUE}Building production Docker image...${NC}"
docker build -f Dockerfile.prod -t officemate:prod .
echo -e "${GREEN}✓ Docker image built${NC}"

# Tag image with version
VERSION=$(grep "^version" build.gradle.kts | cut -d'"' -f2)
docker tag officemate:prod officemate:$VERSION
echo -e "${GREEN}✓ Image tagged as officemate:$VERSION${NC}"

# Create backup of current deployment
echo ""
echo -e "${BLUE}Creating backup...${NC}"
BACKUP_DIR="backups/$(date +%Y%m%d_%H%M%S)"
mkdir -p $BACKUP_DIR
docker compose -f docker-compose.prod.yml config > $BACKUP_DIR/docker-compose.yml
cp .env.production $BACKUP_DIR/.env.production
echo -e "${GREEN}✓ Backup created at $BACKUP_DIR${NC}"

# Stop existing containers (with graceful shutdown)
echo ""
echo "Stopping existing containers..."
docker compose -f docker-compose.prod.yml down --timeout 30

# Start services
echo ""
echo "Starting production services..."
docker compose -f docker-compose.prod.yml up -d

# Wait for services to be healthy
echo ""
echo "Waiting for services to be healthy..."
sleep 60

# Run database migrations
echo ""
echo -e "${BLUE}Running database migrations...${NC}"
POSTGRES_HOST=localhost \
POSTGRES_PORT=5432 \
POSTGRES_DB=${POSTGRES_DB:-officemate_prod} \
POSTGRES_USER=${POSTGRES_USER:-postgres} \
POSTGRES_PASSWORD=${POSTGRES_PASSWORD} \
bash scripts/db/run-migrations.sh
echo -e "${GREEN}✓ Migrations complete${NC}"

# Health check with retries
echo ""
echo "Performing health checks..."
MAX_RETRIES=20
RETRY_COUNT=0
while [ $RETRY_COUNT -lt $MAX_RETRIES ]; do
    if curl -f http://localhost:8080/actuator/health > /dev/null 2>&1; then
        echo -e "${GREEN}✓ Application is healthy${NC}"
        break
    else
        RETRY_COUNT=$((RETRY_COUNT + 1))
        echo "Waiting for application... ($RETRY_COUNT/$MAX_RETRIES)"
        sleep 15
    fi
done

if [ $RETRY_COUNT -eq $MAX_RETRIES ]; then
    echo -e "${RED}✗ Application failed to start${NC}"
    echo "Checking logs..."
    docker compose -f docker-compose.prod.yml logs app
    
    # Rollback option
    echo ""
    read -p "Do you want to rollback to previous version? (y/N) " -n 1 -r
    echo
    if [[ $REPLY =~ ^[Yy]$ ]]; then
        echo "Rolling back..."
        docker compose -f docker-compose.prod.yml down
        # Restore from backup would go here
        echo -e "${YELLOW}Please manually restore from backup at $BACKUP_DIR${NC}"
    fi
    exit 1
fi

# Smoke tests
echo ""
echo -e "${BLUE}Running smoke tests...${NC}"
HEALTH_STATUS=$(curl -s http://localhost:8080/actuator/health | grep -o '"status":"[^"]*"' | cut -d'"' -f4)
if [ "$HEALTH_STATUS" == "UP" ]; then
    echo -e "${GREEN}✓ Smoke tests passed${NC}"
else
    echo -e "${RED}✗ Smoke tests failed${NC}"
    exit 1
fi

# Display deployment info
echo ""
echo "=========================================="
echo -e "${GREEN}Production Deployment Complete!${NC}"
echo "=========================================="
echo ""
echo "Deployment Details:"
echo "  Version:        $VERSION"
echo "  Timestamp:      $(date)"
echo "  Backup:         $BACKUP_DIR"
echo ""
echo "Service URLs:"
echo "  Application:    http://localhost:8080"
echo "  Health Check:   http://localhost:8080/actuator/health"
echo ""
echo "To view logs:"
echo "  docker compose -f docker-compose.prod.yml logs -f"
echo ""
echo "To monitor application:"
echo "  watch -n 5 'curl -s http://localhost:8080/actuator/health'"
echo ""
echo -e "${YELLOW}Remember to:${NC}"
echo "  1. Monitor application logs for errors"
echo "  2. Check application metrics"
echo "  3. Verify all features are working"
echo "  4. Update deployment documentation"
echo ""
