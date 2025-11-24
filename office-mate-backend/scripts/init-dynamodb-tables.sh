#!/bin/bash

# Script to initialize DynamoDB tables for local development
# This script creates the required tables in LocalStack or AWS DynamoDB

set -e

# Configuration
ENDPOINT="${DYNAMODB_ENDPOINT:-http://localhost:4566}"
REGION="${AWS_REGION:-us-east-1}"
TABLE_PREFIX="${DYNAMODB_TABLE_PREFIX:-dev}"

echo "Initializing DynamoDB tables..."
echo "Endpoint: $ENDPOINT"
echo "Region: $REGION"
echo "Table Prefix: $TABLE_PREFIX"

# Function to create table if it doesn't exist
create_table_if_not_exists() {
    local table_name=$1
    local key_schema=$2
    local attribute_definitions=$3
    
    echo "Checking if table $table_name exists..."
    
    if aws dynamodb describe-table --table-name "$table_name" --endpoint-url "$ENDPOINT" --region "$REGION" 2>/dev/null; then
        echo "Table $table_name already exists"
    else
        echo "Creating table $table_name..."
        aws dynamodb create-table \
            --table-name "$table_name" \
            --key-schema $key_schema \
            --attribute-definitions $attribute_definitions \
            --billing-mode PAY_PER_REQUEST \
            --endpoint-url "$ENDPOINT" \
            --region "$REGION"
        
        echo "Table $table_name created successfully"
    fi
}

# Create route_preferences table
echo ""
echo "Creating route_preferences table..."
create_table_if_not_exists \
    "${TABLE_PREFIX}_route_preferences" \
    "AttributeName=userId,KeyType=HASH AttributeName=routeType,KeyType=RANGE" \
    "AttributeName=userId,AttributeType=S AttributeName=routeType,AttributeType=S"

# Create user_matching table
echo ""
echo "Creating user_matching table..."
create_table_if_not_exists \
    "${TABLE_PREFIX}_user_matching" \
    "AttributeName=userId,KeyType=HASH" \
    "AttributeName=userId,AttributeType=S"

# Create real_time_location table
echo ""
echo "Creating real_time_location table..."
create_table_if_not_exists \
    "${TABLE_PREFIX}_real_time_location" \
    "AttributeName=rideId,KeyType=HASH AttributeName=timestamp,KeyType=RANGE" \
    "AttributeName=rideId,AttributeType=S AttributeName=timestamp,AttributeType=N"

echo ""
echo "DynamoDB tables initialization complete!"
echo ""
echo "List of tables:"
aws dynamodb list-tables --endpoint-url "$ENDPOINT" --region "$REGION"
