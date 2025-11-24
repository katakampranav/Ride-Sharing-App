@echo off
REM Script to initialize DynamoDB tables for local development (Windows)
REM This script creates the required tables in LocalStack or AWS DynamoDB

setlocal enabledelayedexpansion

REM Configuration
if "%DYNAMODB_ENDPOINT%"=="" set DYNAMODB_ENDPOINT=http://localhost:4566
if "%AWS_REGION%"=="" set AWS_REGION=us-east-1
if "%DYNAMODB_TABLE_PREFIX%"=="" set DYNAMODB_TABLE_PREFIX=dev

echo Initializing DynamoDB tables...
echo Endpoint: %DYNAMODB_ENDPOINT%
echo Region: %AWS_REGION%
echo Table Prefix: %DYNAMODB_TABLE_PREFIX%

REM Create route_preferences table
echo.
echo Creating route_preferences table...
aws dynamodb describe-table --table-name "%DYNAMODB_TABLE_PREFIX%_route_preferences" --endpoint-url "%DYNAMODB_ENDPOINT%" --region "%AWS_REGION%" >nul 2>&1
if errorlevel 1 (
    echo Table does not exist, creating...
    aws dynamodb create-table ^
        --table-name "%DYNAMODB_TABLE_PREFIX%_route_preferences" ^
        --key-schema AttributeName=userId,KeyType=HASH AttributeName=routeType,KeyType=RANGE ^
        --attribute-definitions AttributeName=userId,AttributeType=S AttributeName=routeType,AttributeType=S ^
        --billing-mode PAY_PER_REQUEST ^
        --endpoint-url "%DYNAMODB_ENDPOINT%" ^
        --region "%AWS_REGION%"
    echo Table created successfully
) else (
    echo Table already exists
)

REM Create user_matching table
echo.
echo Creating user_matching table...
aws dynamodb describe-table --table-name "%DYNAMODB_TABLE_PREFIX%_user_matching" --endpoint-url "%DYNAMODB_ENDPOINT%" --region "%AWS_REGION%" >nul 2>&1
if errorlevel 1 (
    echo Table does not exist, creating...
    aws dynamodb create-table ^
        --table-name "%DYNAMODB_TABLE_PREFIX%_user_matching" ^
        --key-schema AttributeName=userId,KeyType=HASH ^
        --attribute-definitions AttributeName=userId,AttributeType=S ^
        --billing-mode PAY_PER_REQUEST ^
        --endpoint-url "%DYNAMODB_ENDPOINT%" ^
        --region "%AWS_REGION%"
    echo Table created successfully
) else (
    echo Table already exists
)

REM Create real_time_location table
echo.
echo Creating real_time_location table...
aws dynamodb describe-table --table-name "%DYNAMODB_TABLE_PREFIX%_real_time_location" --endpoint-url "%DYNAMODB_ENDPOINT%" --region "%AWS_REGION%" >nul 2>&1
if errorlevel 1 (
    echo Table does not exist, creating...
    aws dynamodb create-table ^
        --table-name "%DYNAMODB_TABLE_PREFIX%_real_time_location" ^
        --key-schema AttributeName=rideId,KeyType=HASH AttributeName=timestamp,KeyType=RANGE ^
        --attribute-definitions AttributeName=rideId,AttributeType=S AttributeName=timestamp,AttributeType=N ^
        --billing-mode PAY_PER_REQUEST ^
        --endpoint-url "%DYNAMODB_ENDPOINT%" ^
        --region "%AWS_REGION%"
    echo Table created successfully
) else (
    echo Table already exists
)

echo.
echo DynamoDB tables initialization complete!
echo.
echo List of tables:
aws dynamodb list-tables --endpoint-url "%DYNAMODB_ENDPOINT%" --region "%AWS_REGION%"

endlocal
