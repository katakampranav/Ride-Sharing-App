#!/bin/bash

# AWS Services Initialization Script for Local Development
# This script initializes LocalStack with required AWS services for Officemate

set -e

echo "=========================================="
echo "Initializing AWS Services (LocalStack)"
echo "=========================================="

# Configuration
LOCALSTACK_ENDPOINT="http://localhost:4566"
AWS_REGION="us-east-1"
AWS_ACCESS_KEY_ID="test"
AWS_SECRET_ACCESS_KEY="test"

# Export AWS credentials for awslocal
export AWS_ACCESS_KEY_ID=$AWS_ACCESS_KEY_ID
export AWS_SECRET_ACCESS_KEY=$AWS_SECRET_ACCESS_KEY
export AWS_DEFAULT_REGION=$AWS_REGION

# Check if LocalStack is running
echo "Checking LocalStack availability..."
if ! curl -s "$LOCALSTACK_ENDPOINT/_localstack/health" > /dev/null; then
echo "Error: LocalStack is not running at $LOCALSTACK_ENDPOINT"
echo "Please start LocalStack first: docker-compose up -d localstack"
exit 1
fi

echo "✓ LocalStack is running"
echo ""

# Function to check if awslocal is installed
check_awslocal() {
if ! command -v awslocal &> /dev/null; then
echo "awslocal not found. Installing..."
pip install awscli-local
fi
}

check_awslocal

# ==========================================
# 1. Initialize Amazon SNS
# ==========================================
echo "1. Initializing Amazon SNS..."

# Create SNS topic for notifications
TOPIC_ARN=$(awslocal sns create-topic \
--name dev-officemate-notifications \
--endpoint-url $LOCALSTACK_ENDPOINT \
--region $AWS_REGION \
--output text --query 'TopicArn' 2>/dev/null || echo "")

if [ -z "$TOPIC_ARN" ]; then
# Topic might already exist, get its ARN
TOPIC_ARN=$(awslocal sns list-topics \
--endpoint-url $LOCALSTACK_ENDPOINT \
--region $AWS_REGION \
--output text --query 'Topics[?contains(TopicArn, `dev-officemate-notifications`)].TopicArn' | head -1)
fi

echo " ✓ SNS Topic created: $TOPIC_ARN"

# Set SMS attributes
awslocal sns set-sms-attributes \
--attributes DefaultSMSType=Transactional,DefaultSenderID=DevOfficemate \
--endpoint-url $LOCALSTACK_ENDPOINT \
--region $AWS_REGION 2>/dev/null || true

echo " ✓ SMS attributes configured"
echo ""

# ==========================================
# 2. Initialize Amazon SES
# ==========================================
echo "2. Initializing Amazon SES..."

# Verify sender email addresses
SENDER_EMAILS=("dev@officemate.local" "noreply@officemate.local" "support@officemate.local")

for email in "${SENDER_EMAILS[@]}"; do
awslocal ses verify-email-identity \
--email-address "$email" \
--endpoint-url $LOCALSTACK_ENDPOINT \
--region $AWS_REGION 2>/dev/null || true
echo " ✓ Email verified: $email"
done

# Create configuration set
awslocal ses create-configuration-set \
--configuration-set Name=dev-officemate-emails \
--endpoint-url $LOCALSTACK_ENDPOINT \
--region $AWS_REGION 2>/dev/null || true

echo " ✓ Configuration set created: dev-officemate-emails"
echo ""

# ==========================================
# 3. Initialize AWS KMS
# ==========================================
echo "3. Initializing AWS KMS..."

# Create KMS key
KEY_ID=$(awslocal kms create-key \
--description "Officemate development encryption key" \
--endpoint-url $LOCALSTACK_ENDPOINT \
--region $AWS_REGION \
--output text --query 'KeyMetadata.KeyId' 2>/dev/null || echo "")

if [ -z "$KEY_ID" ]; then
# Get existing key
KEY_ID=$(awslocal kms list-keys \
--endpoint-url $LOCALSTACK_ENDPOINT \
--region $AWS_REGION \
--output text --query 'Keys[0].KeyId' | head -1)
fi

echo " ✓ KMS Key created: $KEY_ID"

# Create alias
awslocal kms create-alias \
--alias-name alias/dev-officemate-encryption \
--target-key-id "$KEY_ID" \
--endpoint-url $LOCALSTACK_ENDPOINT \
--region $AWS_REGION 2>/dev/null || true

echo " ✓ KMS Alias created: alias/dev-officemate-encryption"
echo ""

# ==========================================
# 4. Initialize DynamoDB Tables
# ==========================================
echo "4. Initializing DynamoDB Tables..."

# Create Route Preferences Table
echo " Creating route_preferences table..."
awslocal dynamodb create-table \
--table-name dev_route_preferences \
--attribute-definitions \
AttributeName=userId,AttributeType=S \
AttributeName=routeType,AttributeType=S \
--key-schema \
AttributeName=userId,KeyType=HASH \
AttributeName=routeType,KeyType=RANGE \
--billing-mode PAY_PER_REQUEST \
--endpoint-url $LOCALSTACK_ENDPOINT \
--region $AWS_REGION 2>/dev/null || echo " (Table already exists)"

echo " ✓ Table created: dev_route_preferences"

# Create Location Tracking Table
echo " Creating location_tracking table..."
awslocal dynamodb create-table \
--table-name dev_location_tracking \
--attribute-definitions \
AttributeName=rideId,AttributeType=S \
AttributeName=timestamp,AttributeType=N \
--key-schema \
AttributeName=rideId,KeyType=HASH \
AttributeName=timestamp,KeyType=RANGE \
--billing-mode PAY_PER_REQUEST \
--stream-specification StreamEnabled=true,StreamViewType=NEW_AND_OLD_IMAGES \
--endpoint-url $LOCALSTACK_ENDPOINT \
--region $AWS_REGION 2>/dev/null || echo " (Table already exists)"

echo " ✓ Table created: dev_location_tracking"
echo ""

# ==========================================
# 5. Verify Setup
# ==========================================
echo "5. Verifying setup..."

# Check SNS
TOPIC_COUNT=$(awslocal sns list-topics \
--endpoint-url $LOCALSTACK_ENDPOINT \
--region $AWS_REGION \
--output text --query 'length(Topics)' 2>/dev/null || echo "0")
echo " ✓ SNS Topics: $TOPIC_COUNT"

# Check SES
EMAIL_COUNT=$(awslocal ses list-identities \
--endpoint-url $LOCALSTACK_ENDPOINT \
--region $AWS_REGION \
--output text --query 'length(Identities)' 2>/dev/null || echo "0")
echo " ✓ SES Verified Emails: $EMAIL_COUNT"

# Check KMS
KEY_COUNT=$(awslocal kms list-keys \
--endpoint-url $LOCALSTACK_ENDPOINT \
--region $AWS_REGION \
--output text --query 'length(Keys)' 2>/dev/null || echo "0")
echo " ✓ KMS Keys: $KEY_COUNT"

# Check DynamoDB
TABLE_COUNT=$(awslocal dynamodb list-tables \
--endpoint-url $LOCALSTACK_ENDPOINT \
--region $AWS_REGION \
--output text --query 'length(TableNames)' 2>/dev/null || echo "0")
echo " ✓ DynamoDB Tables: $TABLE_COUNT"

echo ""
echo "=========================================="
echo "AWS Services Initialization Complete!"
echo "=========================================="
echo ""
echo "Configuration Summary:"
echo " SNS Topic ARN: $TOPIC_ARN"
echo " KMS Key ID: $KEY_ID"
echo " KMS Alias: alias/dev-officemate-encryption"
echo " DynamoDB Tables: dev_route_preferences, dev_location_tracking"
echo ""
echo "Update your .env file with:"
echo " AWS_SNS_TOPIC_ARN=$TOPIC_ARN"
echo " AWS_KMS_KEY_ID=$KEY_ID"
echo ""
echo "Services are ready for local development!"