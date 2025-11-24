@echo off
REM Script to initialize AWS services in LocalStack for local development (Windows)
REM This script sets up SNS, SES, KMS, and DynamoDB in LocalStack

setlocal enabledelayedexpansion

REM Configuration
set ENDPOINT=http://localhost:4566
set REGION=us-east-1

echo Initializing LocalStack AWS services...
echo Endpoint: %ENDPOINT%
echo Region: %REGION%

REM Wait for LocalStack to be ready
echo Waiting for LocalStack to be ready...
:wait_loop
curl -s "%ENDPOINT%/_localstack/health" | findstr /C:"\"dynamodb\": \"available\"" >nul 2>&1
if errorlevel 1 (
    echo Waiting for LocalStack services...
    timeout /t 2 /nobreak >nul
    goto wait_loop
)
echo LocalStack is ready!

REM Initialize DynamoDB tables
echo.
echo Initializing DynamoDB tables...
set DYNAMODB_ENDPOINT=%ENDPOINT%
set DYNAMODB_TABLE_PREFIX=dev
set AWS_REGION=%REGION%
call scripts\init-dynamodb-tables.bat

REM Create KMS key
echo.
echo Creating KMS key...
for /f "tokens=*" %%i in ('aws kms create-key --description "OfficeMate development encryption key" --endpoint-url "%ENDPOINT%" --region "%REGION%" --query "KeyMetadata.KeyId" --output text') do set KMS_KEY_ID=%%i

echo KMS Key created: %KMS_KEY_ID%

REM Create KMS alias
aws kms create-alias ^
    --alias-name alias/officemate-dev ^
    --target-key-id "%KMS_KEY_ID%" ^
    --endpoint-url "%ENDPOINT%" ^
    --region "%REGION%"

echo KMS alias created: alias/officemate-dev

REM Verify SES domain
echo.
echo Verifying SES domain...
aws ses verify-domain-identity ^
    --domain officemate.local ^
    --endpoint-url "%ENDPOINT%" ^
    --region "%REGION%"

echo SES domain verified: officemate.local

REM Verify SES email address
aws ses verify-email-identity ^
    --email-address dev@officemate.local ^
    --endpoint-url "%ENDPOINT%" ^
    --region "%REGION%"

echo SES email verified: dev@officemate.local

REM Set SNS SMS attributes
echo.
echo Configuring SNS SMS attributes...
aws sns set-sms-attributes ^
    --attributes DefaultSMSType=Transactional ^
    --endpoint-url "%ENDPOINT%" ^
    --region "%REGION%"

echo SNS SMS attributes configured

echo.
echo LocalStack initialization complete!
echo.
echo Services initialized:
echo   - DynamoDB: Tables created (dev_route_preferences, dev_user_matching, dev_real_time_location)
echo   - KMS: Key created (%KMS_KEY_ID%) with alias (alias/officemate-dev)
echo   - SES: Domain verified (officemate.local), Email verified (dev@officemate.local)
echo   - SNS: SMS attributes configured
echo.
echo You can now start the application with:
echo   gradlew.bat bootRun --args="--spring.profiles.active=dev"

endlocal
