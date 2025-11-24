# Quick Start: External Services Integration

## For Local Development

### 1. Start LocalStack and Infrastructure
```bash
# Start all infrastructure services (PostgreSQL, Redis, MongoDB, LocalStack)
make docker-up

# Wait for services to be ready (about 30 seconds)
```

### 2. Initialize LocalStack AWS Services
```bash
# For Linux/Mac
make init-localstack

# For Windows
scripts\init-localstack.bat
```

This will automatically set up:
- ✅ DynamoDB tables (route_preferences, user_matching, real_time_location)
- ✅ KMS encryption key with alias
- ✅ SES email verification
- ✅ SNS SMS configuration

### 3. Start the Application
```bash
# Run with dev profile
make run-dev

# Or using Gradle directly
./gradlew bootRun --args='--spring.profiles.active=dev'
```

### 4. Verify Services
```bash
# Check application health
curl http://localhost:8080/actuator/health

# Check LocalStack health
curl http://localhost:4566/_localstack/health
```

## For Production

### 1. Configure AWS Services

#### Create KMS Key
```bash
aws kms create-key --description "OfficeMate production encryption key"
aws kms create-alias --alias-name alias/officemate-prod --target-key-id <key-id>
```

#### Verify SES Domain
```bash
aws ses verify-domain-identity --domain officemate.com
```

#### Configure SNS
```bash
aws sns set-sms-attributes --attributes DefaultSMSType=Transactional
```

#### Create DynamoDB Tables
```bash
export DYNAMODB_TABLE_PREFIX=prod
./scripts/init-dynamodb-tables.sh
```

### 2. Set Environment Variables
```bash
export AWS_REGION=us-east-1
export AWS_ACCESS_KEY_ID=<your-access-key>
export AWS_SECRET_ACCESS_KEY=<your-secret-key>
export SNS_SENDER_ID=OfficeMate
export SES_FROM_EMAIL=noreply@officemate.com
export SES_FROM_NAME=OfficeMate
export KMS_KEY_ID=<your-kms-key-id>
export DYNAMODB_TABLE_PREFIX=prod
export SERVICE_API_KEY=$(openssl rand -base64 32)
```

### 3. Deploy Application
```bash
make deploy-prod
```

## Service Usage in Code

### Send SMS OTP
```java
@Autowired
private SnsService snsService;

// Send OTP SMS
String messageId = snsService.sendOtpSms("+1234567890", "123456");
```

### Send Email OTP
```java
@Autowired
private SesService sesService;

// Send OTP email
String messageId = sesService.sendOtpEmail("user@company.com", "123456");
```

### Encrypt/Decrypt Data
```java
@Autowired
private KmsService kmsService;

// Encrypt sensitive data
String encrypted = kmsService.encrypt("sensitive-data");

// Decrypt data
String decrypted = kmsService.decrypt(encrypted);
```

## Troubleshooting

### LocalStack not starting
```bash
# Check Docker is running
docker ps

# Restart LocalStack
docker compose restart localstack

# Check LocalStack logs
docker compose logs localstack
```

### Tables not created
```bash
# Manually run initialization
DYNAMODB_ENDPOINT=http://localhost:4566 ./scripts/init-dynamodb-tables.sh

# Verify tables exist
aws dynamodb list-tables --endpoint-url http://localhost:4566
```

### SMS/Email not sending
- **Local**: LocalStack logs all messages, check logs with `docker compose logs localstack`
- **Production**: Check AWS CloudWatch logs and SNS/SES dashboards

## Configuration Files

- **Main Config**: `src/main/resources/application.yml`
- **Dev Config**: `src/main/resources/application-dev.yml`
- **Prod Config**: `src/main/resources/application-prod.yml`
- **Docker Compose**: `docker-compose.yml`

## Documentation

- **Detailed Setup**: `docs/EXTERNAL_SERVICES_SETUP.md`
- **Integration Summary**: `docs/EXTERNAL_SERVICES_INTEGRATION_SUMMARY.md`

## Support

For issues:
1. Check application logs: `docker compose logs app`
2. Check LocalStack logs: `docker compose logs localstack`
3. Verify configuration in `application.yml`
4. Review detailed documentation in `docs/EXTERNAL_SERVICES_SETUP.md`
