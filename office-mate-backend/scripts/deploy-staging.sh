#!/bin/bash

# Staging deployment script
# This script deploys the application to staging environment

set -e

echo "=========================================="
echo "Officemate Staging Deployment"
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
if [ -f .env.staging ]; then
    echo -e "${GREEN}✓ Loading environment variables from .env.staging${NC}"
    export $(cat .env.staging | grep -v '^#' | xargs)
else
    echo -e "${RED}Error: .env.staging file not found${NC}"
    exit 1
fi

# Confirm deployment
echo -e "${YELLOW}⚠ You are about to deploy to STAGING environment${NC}"
read -p "Do you want to continue? (y/N) " -n 1 -r
echo
if [[ ! $REPLY =~ ^[Yy]$ ]]; then
    echo "Deployment cancelled"
    exit 0
fi

# Run tests before deployment
echo ""
echo -e "${BLUE}Running tests...${NC}"
./gradlew test
echo -e "${GREEN}✓ Tests passed${NC}"

# Build production Docker image
echo ""
echo -e "${BLUE}Building production Docker image...${NC}"
docker build -f Dockerfile.prod -t officemate:staging .
echo -e "${GREEN}✓ Docker image built${NC}"

# Stop existing containers
echo ""
echo "Stopping existing containers..."
docker compose -f docker-compose.prod.yml down

# Start services
echo ""
echo "Starting staging services..."
docker compose -f docker-compose.prod.yml up -d

# Wait for services to be healthy
echo ""
echo "Waiting for services to be healthy..."
sleep 30

# Run database migrations
echo ""
echo -e "${BLUE}Running database migrations...${NC}"
bash scripts/db/run-migrations.sh
echo -e "${GREEN}✓ Migrations complete${NC}"

# Check application health
echo ""
echo "Checking application health..."
MAX_RETRIES=10
RETRY_COUNT=0
while [ $RETRY_COUNT -lt $MAX_RETRIES ]; do
    if curl -f http://localhost:8080/actuator/health > /dev/null 2>&1; then
        echo -e "${GREEN}✓ Application is healthy${NC}"
        break
    else
        RETRY_COUNT=$((RETRY_COUNT + 1))
        echo "Waiting for application... ($RETRY_COUNT/$MAX_RETRIES)"
        sleep 10
    fi
done

if [ $RETRY_COUNT -eq $MAX_RETRIES ]; then
    echo -e "${RED}✗ Application failed to start${NC}"
    echo "Checking logs..."
    docker compose -f docker-compose.prod.yml logs app
    exit 1
fi

# Display deployment info
echo ""
echo "=========================================="
echo -e "${GREEN}Staging Deployment Complete!${NC}"
echo "=========================================="
echo ""
echo "Service URLs:"
echo "  Application:    http://localhost:8080"
echo "  Health Check:   http://localhost:8080/actuator/health"
echo ""
echo "To view logs:"
echo "  docker compose -f docker-compose.prod.yml logs -f"
echo ""
echo "To stop services:"
echo "  docker compose -f docker-compose.prod.yml down"
echo ""
