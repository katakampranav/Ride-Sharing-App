#!/bin/bash

# Script to initialize DynamoDB tables in LocalStack for local development
# Run this after starting LocalStack with docker-compose

set -e

ENDPOINT="http://localhost:4566"
REGION="us-east-1"

echo "Initializing DynamoDB tables in LocalStack..."

# Create Route Preferences Table
echo "Creating dev_route_preferences table..."
aws dynamodb create-table \
  --table-name dev_route_preferences \
  --attribute-definitions \
    AttributeName=userId,AttributeType=S \
    AttributeName=routeType,AttributeType=S \
  --key-schema \
    AttributeName=userId,KeyType=HASH \
    AttributeName=routeType,KeyType=RANGE \
  --billing-mode PAY_PER_REQUEST \
  --endpoint-url $ENDPOINT \
  --region $REGION \
  --no-cli-pager

echo "✓ dev_route_preferences table created"

# Create Location Tracking Table
echo "Creating dev_location_tracking table..."
aws dynamodb create-table \
  --table-name dev_location_tracking \
  --attribute-definitions \
    AttributeName=rideId,AttributeType=S \
    AttributeName=timestamp,AttributeType=N \
  --key-schema \
    AttributeName=rideId,KeyType=HASH \
    AttributeName=timestamp,KeyType=RANGE \
  --billing-mode PAY_PER_REQUEST \
  --endpoint-url $ENDPOINT \
  --region $REGION \
  --no-cli-pager

echo "✓ dev_location_tracking table created"

# List all tables to verify
echo ""
echo "Listing all DynamoDB tables:"
aws dynamodb list-tables \
  --endpoint-url $ENDPOINT \
  --region $REGION \
  --no-cli-pager

echo ""
echo "✓ DynamoDB initialization complete!"
echo ""
echo "You can view tables at: http://localhost:4566"

