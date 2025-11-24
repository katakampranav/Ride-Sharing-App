# External Services Setup Guide

This document provides instructions for configuring external AWS services required by the OfficeMate application.

## Overview

The application integrates with the following AWS services:
- **Amazon SNS**: SMS delivery for mobile OTP verification
- **Amazon SES**: Email delivery for corporate email OTP verification
- **AWS KMS**: Encryption and key management for sensitive data
- **Amazon DynamoDB**: NoSQL database for route preferences and real-time location data

## Prerequisites

- AWS Account with appropriate permissions
- AWS CLI installed and configured
- For local development: LocalStack or AWS credentials

## 1. Amazon SNS Configuration

### Purpose
SNS is used to send SMS messages for mobile OTP verification during user registration and login.

### Setup Steps

1. **Enable SMS in AWS SNS**
   ```bash
   # Set SMS preferences
   aws sns set-sms-attributes \
     --attributes DefaultSMSType=Transactional
   ```

2. **Configure Spending Limits**
   ```bash
   # Set monthly spending limit (in USD)
   aws sns set-sms-attributes \
     --attributes MonthlySpendLimit=100
   ```

3. **Request Production Access** (if needed)
   - By default, SNS is in sandbox mode with limited sending
   - Request production access through AWS Support for higher limits

4. **Environment Variables**
   ```bash
   export AWS_REGION=us-east-1
   export SNS_SENDER_ID=OfficeMate
   export SNS_SMS_TYPE=Transactional
   export SNS_MAX_PRICE=0.50
   ```

### Testing
```bash
# Test SMS sending
aws sns publish \
  --phone-number "+1234567890" \
  --message "Test message from OfficeMate"
```

## 2. Amazon SES Configuration

### Purpose
SES is used to send email OTPs for corporate email verification.

### Setup Steps

1. **Verify Email Domain**
   ```bash
   # Verify your sending domain
   aws ses verify-domain-identity --domain officemate.com
   ```

2. **Configure DKIM**
   ```bash
   # Enable DKIM signing
   aws ses set-identity-dkim-enabled \
     --identity officemate.com \
     --dkim-enabled
   ```

3. **Create Configuration Set**
   ```bash
   # Create configuration set for tracking
   aws ses create-configuration-set \
     --configuration-set Name=officemate-prod
   ```

4. **Request Production Access**
   - By default, SES is in sandbox mode
   - Request production access through AWS Support
   - Verify recipient email addresses in sandbox mode

5. **Environment Variables**
   ```bash
   export SES_FROM_EMAIL=noreply@officemate.com
   export SES_FROM_NAME=OfficeMate
   export SES_CONFIGURATION_SET=officemate-prod
   ```

### Testing
```bash
# Test email sending
aws ses send-email \
  --from "noreply@officemate.com" \
  --destination "ToAddresses=test@example.com" \
  --message "Subject={Data=Test},Body={Text={Data=Test message}}"
```

## 3. AWS KMS Configuration

### Purpose
KMS is used for encrypting sensitive data and signing JWT tokens.

### Setup Steps

1. **Create KMS Key**
   ```bash
   # Create a symmetric encryption key
   aws kms create-key \
     --description "OfficeMate encryption key" \
     --key-usage ENCRYPT_DECRYPT
   ```

2. **Create Key Alias**
   ```bash
   # Create an alias for easier reference
   aws kms create-alias \
     --alias-name alias/officemate-prod \
     --target-key-id <key-id-from-previous-step>
   ```

3. **Set Key Policy**
   ```bash
   # Update key policy to allow application access
   aws kms put-key-policy \
     --key-id <key-id> \
     --policy-name default \
     --policy file://kms-key-policy.json
   ```

4. **Environment Variables**
   ```bash
   export KMS_KEY_ID=<your-key-id>
   export KMS_KEY_ALIAS=alias/officemate-prod
   ```

### Key Policy Example
```json
{
  "Version": "2012-10-17",
  "Statement": [
    {
      "Sid": "Enable IAM User Permissions",
      "Effect": "Allow",
      "Principal": {
        "AWS": "arn:aws:iam::ACCOUNT-ID:root"
      },
      "Action": "kms:*",
      "Resource": "*"
    },
    {
      "Sid": "Allow application to use the key",
      "Effect": "Allow",
      "Principal": {
        "AWS": "arn:aws:iam::ACCOUNT-ID:role/officemate-app-role"
      },
      "Action": [
        "kms:Encrypt",
        "kms:Decrypt",
        "kms:GenerateDataKey",
        "kms:Sign",
        "kms:Verify"
      ],
      "Resource": "*"
    }
  ]
}
```

## 4. Amazon DynamoDB Configuration

### Purpose
DynamoDB stores route preferences, user matching data, and real-time location information.

### Setup Steps

1. **Create Tables Using Script**
   ```bash
   # For Linux/Mac
   ./scripts/init-dynamodb-tables.sh
   
   # For Windows
   scripts\init-dynamodb-tables.bat
   ```

2. **Or Create Tables Manually**
   ```bash
   # Route preferences table
   aws dynamodb create-table \
     --table-name prod_route_preferences \
     --key-schema \
       AttributeName=userId,KeyType=HASH \
       AttributeName=routeType,KeyType=RANGE \
     --attribute-definitions \
       AttributeName=userId,AttributeType=S \
       AttributeName=routeType,AttributeType=S \
     --billing-mode PAY_PER_REQUEST
   
   # User matching table
   aws dynamodb create-table \
     --table-name prod_user_matching \
     --key-schema AttributeName=userId,KeyType=HASH \
     --attribute-definitions AttributeName=userId,AttributeType=S \
     --billing-mode PAY_PER_REQUEST
   
   # Real-time location table
   aws dynamodb create-table \
     --table-name prod_real_time_location \
     --key-schema \
       AttributeName=rideId,KeyType=HASH \
       AttributeName=timestamp,KeyType=RANGE \
     --attribute-definitions \
       AttributeName=rideId,AttributeType=S \
       AttributeName=timestamp,AttributeType=N \
     --billing-mode PAY_PER_REQUEST
   ```

3. **Enable Point-in-Time Recovery** (recommended for production)
   ```bash
   aws dynamodb update-continuous-backups \
     --table-name prod_route_preferences \
     --point-in-time-recovery-specification PointInTimeRecoveryEnabled=true
   ```

4. **Environment Variables**
   ```bash
   export DYNAMODB_TABLE_PREFIX=prod
   export DYNAMODB_READ_CAPACITY=10
   export DYNAMODB_WRITE_CAPACITY=10
   ```

## 5. Local Development with LocalStack

### Setup LocalStack

1. **Install LocalStack**
   ```bash
   pip install localstack
   ```

2. **Start LocalStack**
   ```bash
   localstack start
   ```

3. **Configure Environment**
   ```bash
   export AWS_REGION=us-east-1
   export AWS_ACCESS_KEY_ID=test
   export AWS_SECRET_ACCESS_KEY=test
   export DYNAMODB_ENDPOINT=http://localhost:4566
   export SNS_ENDPOINT=http://localhost:4566
   export SES_ENDPOINT=http://localhost:4566
   export KMS_ENDPOINT=http://localhost:4566
   ```

4. **Initialize Tables**
   ```bash
   DYNAMODB_ENDPOINT=http://localhost:4566 ./scripts/init-dynamodb-tables.sh
   ```

## 6. IAM Permissions

### Required IAM Policy

Create an IAM role or user with the following policy:

```json
{
  "Version": "2012-10-17",
  "Statement": [
    {
      "Effect": "Allow",
      "Action": [
        "sns:Publish",
        "sns:SetSMSAttributes"
      ],
      "Resource": "*"
    },
    {
      "Effect": "Allow",
      "Action": [
        "ses:SendEmail",
        "ses:SendRawEmail"
      ],
      "Resource": "*"
    },
    {
      "Effect": "Allow",
      "Action": [
        "kms:Encrypt",
        "kms:Decrypt",
        "kms:GenerateDataKey",
        "kms:Sign",
        "kms:Verify"
      ],
      "Resource": "arn:aws:kms:REGION:ACCOUNT-ID:key/*"
    },
    {
      "Effect": "Allow",
      "Action": [
        "dynamodb:PutItem",
        "dynamodb:GetItem",
        "dynamodb:UpdateItem",
        "dynamodb:DeleteItem",
        "dynamodb:Query",
        "dynamodb:Scan",
        "dynamodb:DescribeTable",
        "dynamodb:CreateTable"
      ],
      "Resource": "arn:aws:dynamodb:REGION:ACCOUNT-ID:table/prod_*"
    }
  ]
}
```

## 7. Service-to-Service Authentication

### Configuration

1. **Generate API Key**
   ```bash
   # Generate a secure random API key
   openssl rand -base64 32
   ```

2. **Set Environment Variable**
   ```bash
   export SERVICE_API_KEY=<generated-api-key>
   export SERVICE_AUTH_ENABLED=true
   ```

3. **Use in Service Calls**
   ```bash
   curl -H "X-Service-Auth: true" \
        -H "X-Service-Name: officemate-backend" \
        -H "Authorization: Bearer <api-key>" \
        https://api.officemate.com/internal/endpoint
   ```

## 8. Environment Variables Summary

### Production
```bash
# AWS Configuration
export AWS_REGION=us-east-1
export AWS_ACCESS_KEY_ID=<your-access-key>
export AWS_SECRET_ACCESS_KEY=<your-secret-key>

# SNS Configuration
export SNS_SENDER_ID=OfficeMate
export SNS_SMS_TYPE=Transactional
export SNS_MAX_PRICE=0.50

# SES Configuration
export SES_FROM_EMAIL=noreply@officemate.com
export SES_FROM_NAME=OfficeMate
export SES_CONFIGURATION_SET=officemate-prod

# KMS Configuration
export KMS_KEY_ID=<your-kms-key-id>
export KMS_KEY_ALIAS=alias/officemate-prod

# DynamoDB Configuration
export DYNAMODB_TABLE_PREFIX=prod
export DYNAMODB_READ_CAPACITY=10
export DYNAMODB_WRITE_CAPACITY=10

# Service Authentication
export SERVICE_API_KEY=<your-api-key>
export SERVICE_AUTH_ENABLED=true
```

### Development
```bash
# AWS Configuration (LocalStack)
export AWS_REGION=us-east-1
export AWS_ACCESS_KEY_ID=test
export AWS_SECRET_ACCESS_KEY=test

# Service Endpoints (LocalStack)
export DYNAMODB_ENDPOINT=http://localhost:4566

# SNS Configuration
export SNS_SENDER_ID=OfficeMate-Dev
export SNS_SMS_TYPE=Transactional

# SES Configuration
export SES_FROM_EMAIL=dev@officemate.local
export SES_FROM_NAME=OfficeMate Dev

# KMS Configuration
export KMS_KEY_ID=dev-key-id
export KMS_KEY_ALIAS=alias/officemate-dev

# DynamoDB Configuration
export DYNAMODB_TABLE_PREFIX=dev

# Service Authentication
export SERVICE_AUTH_ENABLED=false
```

## 9. Monitoring and Logging

### CloudWatch Metrics

Monitor the following metrics:
- SNS: `NumberOfMessagesSent`, `NumberOfNotificationsFailed`
- SES: `Send`, `Bounce`, `Complaint`
- KMS: `Encrypt`, `Decrypt`, `Sign`, `Verify`
- DynamoDB: `ConsumedReadCapacityUnits`, `ConsumedWriteCapacityUnits`

### CloudWatch Logs

Enable logging for:
- SNS delivery status
- SES sending events
- DynamoDB operations

## 10. Troubleshooting

### SNS Issues
- **SMS not delivered**: Check phone number format (E.164), verify spending limits
- **Throttling**: Increase sending rate limits through AWS Support

### SES Issues
- **Email bounces**: Verify domain and DKIM configuration
- **Sandbox mode**: Request production access or verify recipient emails

### KMS Issues
- **Access denied**: Check IAM permissions and key policy
- **Key not found**: Verify key ID or alias

### DynamoDB Issues
- **Table not found**: Run initialization script
- **Throttling**: Increase provisioned capacity or use on-demand billing

## Support

For issues with AWS services:
- AWS Support: https://console.aws.amazon.com/support/
- AWS Documentation: https://docs.aws.amazon.com/

For application-specific issues:
- Check application logs
- Review configuration in `application.yml`
- Verify environment variables
