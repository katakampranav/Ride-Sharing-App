# External Services Integration Summary

## Overview

This document summarizes the external service integrations configured for the OfficeMate application as part of task 11.3.

## Implemented Components

### 1. AWS Service Wrapper Classes

#### SnsService (`src/main/java/com/officemate/shared/service/SnsService.java`)
- **Purpose**: Send SMS messages for mobile OTP verification
- **Features**:
  - Send generic SMS messages
  - Send OTP SMS with formatted template
  - Phone number masking for security logging
  - Configurable SMS attributes (sender ID, SMS type, max price)
- **Configuration**: Uses `aws.sns.*` properties from application.yml

#### SesService (`src/main/java/com/officemate/shared/service/SesService.java`)
- **Purpose**: Send emails for corporate email OTP verification
- **Features**:
  - Send HTML and text emails
  - Send OTP emails with professional HTML templates
  - Send email change notifications
  - Email masking for security logging
  - Support for SES configuration sets
- **Configuration**: Uses `aws.ses.*` properties from application.yml

#### KmsService (`src/main/java/com/officemate/shared/service/KmsService.java`)
- **Purpose**: Encryption and key management for sensitive data
- **Features**:
  - Encrypt/decrypt data using KMS
  - Generate data keys for envelope encryption
  - Sign messages (for JWT token signing)
  - Verify signatures
  - Support for both key ID and key alias
- **Configuration**: Uses `aws.kms.*` properties from application.yml

### 2. DynamoDB Table Initialization

#### DynamoDbTableInitializer (`src/main/java/com/officemate/shared/service/DynamoDbTableInitializer.java`)
- **Purpose**: Automatically create DynamoDB tables on application startup
- **Tables Created**:
  1. **route_preferences**: Stores user route preferences with userId and routeType as keys
  2. **user_matching**: Stores user matching data with userId as key
  3. **real_time_location**: Stores GPS coordinates during rides with rideId and timestamp as keys
- **Features**:
  - Automatic table creation if not exists
  - Table existence checking
  - Wait for table to become active
  - Environment-specific table prefixes
  - Pay-per-request billing mode

### 3. Service-to-Service Authentication

#### ServiceAuthenticationConfig (`src/main/java/com/officemate/config/ServiceAuthenticationConfig.java`)
- **Purpose**: Provide authentication for internal service communication
- **Features**:
  - API key generation and management
  - Service authentication headers configuration
  - Password encoder for API key hashing
  - Enable/disable service authentication
- **Configuration**: Uses `app.service-auth.*` properties from application.yml

### 4. Initialization Scripts

#### For Linux/Mac (`scripts/init-dynamodb-tables.sh`)
- Creates DynamoDB tables using AWS CLI
- Supports custom endpoints for LocalStack
- Environment variable configuration

#### For Windows (`scripts/init-dynamodb-tables.bat`)
- Windows batch script version
- Same functionality as Linux script

#### LocalStack Initialization (`scripts/init-localstack.sh` and `scripts/init-localstack.bat`)
- Complete LocalStack setup for local development
- Initializes all AWS services:
  - DynamoDB tables
  - KMS keys and aliases
  - SES domain and email verification
  - SNS SMS attributes
- Waits for LocalStack to be ready before initialization

### 5. Configuration Updates

#### application.yml
- Added service-to-service authentication configuration
- All AWS service configurations already present

#### Makefile
- Added `init-localstack` target for LocalStack initialization
- Added `init-aws` target for production AWS setup
- Updated `dev-start` to include LocalStack initialization

#### docker-compose.yml
- LocalStack service already configured
- Supports DynamoDB, SNS, SES, and KMS emulation

### 6. Documentation

#### EXTERNAL_SERVICES_SETUP.md (`docs/EXTERNAL_SERVICES_SETUP.md`)
Comprehensive guide covering:
- SNS configuration and setup
- SES configuration and setup
- KMS configuration and setup
- DynamoDB table creation
- LocalStack setup for local development
- IAM permissions required
- Service-to-service authentication
- Environment variables summary
- Monitoring and logging
- Troubleshooting guide

## Configuration Properties

### AWS Region and Credentials
```yaml
aws:
  region: ${AWS_REGION:us-east-1}
  access-key-id: ${AWS_ACCESS_KEY_ID:}
  secret-access-key: ${AWS_SECRET_ACCESS_KEY:}
```

### SNS Configuration
```yaml
aws:
  sns:
    sender-id: ${SNS_SENDER_ID:OfficeMate}
    sms-type: ${SNS_SMS_TYPE:Transactional}
    max-price: ${SNS_MAX_PRICE:0.50}
```

### SES Configuration
```yaml
aws:
  ses:
    from-email: ${SES_FROM_EMAIL:noreply@officemate.com}
    from-name: ${SES_FROM_NAME:OfficeMate}
    configuration-set: ${SES_CONFIGURATION_SET:}
```

### KMS Configuration
```yaml
aws:
  kms:
    key-id: ${KMS_KEY_ID:}
    key-alias: ${KMS_KEY_ALIAS:alias/officemate}
```

### DynamoDB Configuration
```yaml
aws:
  dynamodb:
    endpoint: ${DYNAMODB_ENDPOINT:}
    table-prefix: ${DYNAMODB_TABLE_PREFIX:officemate}
    tables:
      route-preferences: ${DYNAMODB_TABLE_ROUTE_PREFERENCES:route_preferences}
      user-matching: ${DYNAMODB_TABLE_USER_MATCHING:user_matching}
      real-time-location: ${DYNAMODB_TABLE_REAL_TIME_LOCATION:real_time_location}
    read-capacity-units: ${DYNAMODB_READ_CAPACITY:5}
    write-capacity-units: ${DYNAMODB_WRITE_CAPACITY:5}
```

### Service Authentication Configuration
```yaml
app:
  service-auth:
    enabled: ${SERVICE_AUTH_ENABLED:true}
    api-key: ${SERVICE_API_KEY:}
```

## Usage Examples

### Sending SMS OTP
```java
@Autowired
private SnsService snsService;

public void sendMobileOtp(String phoneNumber, String otp) {
    String messageId = snsService.sendOtpSms(phoneNumber, otp);
    log.info("SMS sent with message ID: {}", messageId);
}
```

### Sending Email OTP
```java
@Autowired
private SesService sesService;

public void sendEmailOtp(String email, String otp) {
    String messageId = sesService.sendOtpEmail(email, otp);
    log.info("Email sent with message ID: {}", messageId);
}
```

### Encrypting Data
```java
@Autowired
private KmsService kmsService;

public String encryptSensitiveData(String plaintext) {
    return kmsService.encrypt(plaintext);
}

public String decryptSensitiveData(String ciphertext) {
    return kmsService.decrypt(ciphertext);
}
```

## Local Development Setup

### 1. Start Infrastructure Services
```bash
make docker-up
```

### 2. Initialize LocalStack
```bash
make init-localstack
```

### 3. Start Application
```bash
make run-dev
```

## Production Setup

### 1. Configure AWS Services
Follow the detailed guide in `docs/EXTERNAL_SERVICES_SETUP.md`

### 2. Set Environment Variables
```bash
export AWS_REGION=us-east-1
export AWS_ACCESS_KEY_ID=<your-key>
export AWS_SECRET_ACCESS_KEY=<your-secret>
export SNS_SENDER_ID=OfficeMate
export SES_FROM_EMAIL=noreply@officemate.com
export KMS_KEY_ID=<your-kms-key-id>
export DYNAMODB_TABLE_PREFIX=prod
export SERVICE_API_KEY=<generated-api-key>
```

### 3. Initialize DynamoDB Tables
```bash
./scripts/init-dynamodb-tables.sh
```

### 4. Deploy Application
```bash
make deploy-prod
```

## Testing

### Test SNS Integration
```bash
# Using AWS CLI
aws sns publish \
  --phone-number "+1234567890" \
  --message "Test OTP: 123456"
```

### Test SES Integration
```bash
# Using AWS CLI
aws ses send-email \
  --from "noreply@officemate.com" \
  --destination "ToAddresses=test@example.com" \
  --message "Subject={Data=Test},Body={Text={Data=Test OTP: 123456}}"
```

### Test DynamoDB Tables
```bash
# List tables
aws dynamodb list-tables

# Describe table
aws dynamodb describe-table --table-name prod_route_preferences
```

## Security Considerations

1. **API Keys**: Store service API keys securely in environment variables or secrets manager
2. **KMS Keys**: Use separate KMS keys for different environments
3. **IAM Permissions**: Follow principle of least privilege for IAM roles
4. **Logging**: Sensitive data (phone numbers, emails) are masked in logs
5. **Encryption**: All sensitive data encrypted at rest and in transit

## Monitoring

### CloudWatch Metrics to Monitor
- SNS: `NumberOfMessagesSent`, `NumberOfNotificationsFailed`
- SES: `Send`, `Bounce`, `Complaint`
- KMS: `Encrypt`, `Decrypt`, `Sign`, `Verify`
- DynamoDB: `ConsumedReadCapacityUnits`, `ConsumedWriteCapacityUnits`

### Application Logs
- All AWS service calls are logged with INFO level
- Errors are logged with ERROR level including AWS error details
- Sensitive data is masked in logs

## Troubleshooting

### Common Issues

1. **SMS not delivered**
   - Check phone number format (E.164)
   - Verify SNS spending limits
   - Check AWS account is out of sandbox mode

2. **Email bounces**
   - Verify domain and DKIM configuration
   - Check SES is out of sandbox mode
   - Verify recipient email in sandbox mode

3. **KMS access denied**
   - Check IAM permissions
   - Verify key policy allows application access

4. **DynamoDB table not found**
   - Run initialization script
   - Check table prefix configuration

## Next Steps

1. Configure production AWS services following the setup guide
2. Set up CloudWatch alarms for service monitoring
3. Configure backup and disaster recovery for DynamoDB tables
4. Implement retry logic and circuit breakers for AWS service calls
5. Set up cost monitoring and budgets for AWS services

## References

- [AWS SNS Documentation](https://docs.aws.amazon.com/sns/)
- [AWS SES Documentation](https://docs.aws.amazon.com/ses/)
- [AWS KMS Documentation](https://docs.aws.amazon.com/kms/)
- [AWS DynamoDB Documentation](https://docs.aws.amazon.com/dynamodb/)
- [LocalStack Documentation](https://docs.localstack.cloud/)
