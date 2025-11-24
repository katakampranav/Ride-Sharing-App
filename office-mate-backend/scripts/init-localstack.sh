#!/bin/bash

# Script to initialize AWS services in LocalStack for local development
# This script sets up SNS, SES, KMS, and DynamoDB in LocalStack

set -e

# Configuration
ENDPOINT="http://localhost:4566"
REGION="us-east-1"

echo "Initializing LocalStack AWS services..."
echo "Endpoint: $ENDPOINT"
echo "Region: $REGION"

# Wait for LocalStack to be ready
echo "Waiting for LocalStack to be ready..."
until curl -s "$ENDPOINT/_localstack/health" | grep -q "\"dynamodb\": \"available\""; do
    echo "Waiting for LocalStack services..."
    sleep 2
done
echo "LocalStack is ready!"

# Initialize DynamoDB tables
echo ""
echo "Initializing DynamoDB tables..."
DYNAMODB_ENDPOINT=$ENDPOINT DYNAMODB_TABLE_PREFIX=dev AWS_REGION=$REGION ./scripts/init-dynamodb-tables.sh

# Create KMS key
echo ""
echo "Creating KMS key..."
KMS_KEY_ID=$(aws kms create-key \
    --description "OfficeMate development encryption key" \
    --endpoint-url "$ENDPOINT" \
    --region "$REGION" \
    --query 'KeyMetadata.KeyId' \
    --output text)

echo "KMS Key created: $KMS_KEY_ID"

# Create KMS alias
aws kms create-alias \
    --alias-name alias/officemate-dev \
    --target-key-id "$KMS_KEY_ID" \
    --endpoint-url "$ENDPOINT" \
    --region "$REGION"

echo "KMS alias created: alias/officemate-dev"

# Verify SES domain
echo ""
echo "Verifying SES domain..."
aws ses verify-domain-identity \
    --domain officemate.local \
    --endpoint-url "$ENDPOINT" \
    --region "$REGION"

echo "SES domain verified: officemate.local"

# Verify SES email address
aws ses verify-email-identity \
    --email-address dev@officemate.local \
    --endpoint-url "$ENDPOINT" \
    --region "$REGION"

echo "SES email verified: dev@officemate.local"

# Set SNS SMS attributes
echo ""
echo "Configuring SNS SMS attributes..."
aws sns set-sms-attributes \
    --attributes DefaultSMSType=Transactional \
    --endpoint-url "$ENDPOINT" \
    --region "$REGION"

echo "SNS SMS attributes configured"

echo ""
echo "LocalStack initialization complete!"
echo ""
echo "Services initialized:"
echo "  - DynamoDB: Tables created (dev_route_preferences, dev_user_matching, dev_real_time_location)"
echo "  - KMS: Key created ($KMS_KEY_ID) with alias (alias/officemate-dev)"
echo "  - SES: Domain verified (officemate.local), Email verified (dev@officemate.local)"
echo "  - SNS: SMS attributes configured"
echo ""
echo "You can now start the application with:"
echo "  ./gradlew bootRun --args='--spring.profiles.active=dev'"
