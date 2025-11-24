#!/bin/bash

# Development deployment script
# This script deploys the application in development mode

set -e

echo "=========================================="
echo "Officemate Development Deployment"
echo "=========================================="
echo ""

# Colors for output
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
NC='\033[0m' # No Color

# Check if Docker is running
if ! docker info > /dev/null 2>&1; then
    echo -e "${RED}Error: Docker is not running${NC}"
    exit 1
fi

# Check if docker-compose is available
if ! command -v docker-compose &> /dev/null; then
    echo -e "${RED}Error: docker-compose is not installed${NC}"
    exit 1
fi

# Load environment variables
if [ -f .env ]; then
    echo -e "${GREEN}✓ Loading environment variables from .env${NC}"
    export $(cat .env | grep -v '^#' | xargs)
else
    echo -e "${YELLOW}⚠ No .env file found, using defaults${NC}"
fi

# Stop existing containers
echo ""
echo "Stopping existing containers..."
docker compose down

# Clean up old images (optional)
read -p "Do you want to remove old images? (y/N) " -n 1 -r
echo
if [[ $REPLY =~ ^[Yy]$ ]]; then
    echo "Removing old images..."
    docker compose down --rmi local
fi

# Start infrastructure services
echo ""
echo "Starting infrastructure services..."
docker compose up -d postgres redis mongodb localstack

# Wait for services to be healthy
echo ""
echo "Waiting for services to be healthy..."
sleep 10

# Check service health
echo ""
echo "Checking service health..."
docker compose ps

# Initialize DynamoDB tables
echo ""
echo "Initializing DynamoDB tables..."
if [ -f scripts/init-dynamodb.sh ]; then
    chmod +x scripts/init-dynamodb.sh
    ./scripts/init-dynamodb.sh
else
    echo -e "${YELLOW}⚠ DynamoDB initialization script not found${NC}"
fi

# Build and start the application
echo ""
read -p "Do you want to run the application in Docker? (y/N) " -n 1 -r
echo
if [[ $REPLY =~ ^[Yy]$ ]]; then
    echo "Building and starting application container..."
    docker compose --profile full up -d app
    
    echo ""
    echo "Waiting for application to start..."
    sleep 30
    
    # Check application health
    echo "Checking application health..."
    curl -f http://localhost:8080/actuator/health || echo -e "${YELLOW}⚠ Application not ready yet${NC}"
else
    echo ""
    echo -e "${GREEN}Infrastructure services are running!${NC}"
    echo ""
    echo "To start the application locally, run:"
    echo "  make run"
    echo "  or"
    echo "  ./gradlew bootRun"
fi

# Display service URLs
echo ""
echo "=========================================="
echo -e "${GREEN}Deployment Complete!${NC}"
echo "=========================================="
echo ""
echo "Service URLs:"
echo "  Application:    http://localhost:8080"
echo "  Health Check:   http://localhost:8080/actuator/health"
echo "  PostgreSQL:     localhost:5432"
echo "  Redis:          localhost:6379"
echo "  MongoDB:        localhost:27017"
echo "  LocalStack:     http://localhost:4566"
echo ""
echo "To view logs:"
echo "  make logs"
echo "  or"
echo "  docker compose logs -f [service-name]"
echo ""
echo "To stop services:"
echo "  make down"
echo "  or"
echo "  docker compose down"
echo ""

