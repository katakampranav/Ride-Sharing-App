package com.officemate.shared.service;

import com.officemate.config.properties.AwsProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.*;
import software.amazon.awssdk.services.dynamodb.waiters.DynamoDbWaiter;
import software.amazon.awssdk.core.waiters.WaiterResponse;

import java.util.List;

/**
 * Service to initialize DynamoDB tables for route preferences and real-time data.
 * Tables are created automatically on application startup if they don't exist.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DynamoDbTableInitializer {

    private final DynamoDbClient dynamoDbClient;
    private final AwsProperties awsProperties;

    /**
     * Initialize DynamoDB tables on application startup
     */
    @EventListener(ApplicationReadyEvent.class)
    public void initializeTables() {
        log.info("Initializing DynamoDB tables...");
        
        try {
            createRoutePreferencesTable();
            createUserMatchingTable();
            createRealTimeLocationTable();
            
            log.info("DynamoDB tables initialized successfully");
        } catch (Exception e) {
            log.error("Failed to initialize DynamoDB tables: {}", e.getMessage(), e);
            // Don't fail application startup if tables already exist
        }
    }

    /**
     * Create route_preferences table for storing user route preferences
     */
    private void createRoutePreferencesTable() {
        String tableName = getTableName("route-preferences");
        
        if (tableExists(tableName)) {
            log.info("Table {} already exists", tableName);
            return;
        }

        try {
            CreateTableRequest request = CreateTableRequest.builder()
                    .tableName(tableName)
                    .keySchema(
                            KeySchemaElement.builder()
                                    .attributeName("userId")
                                    .keyType(KeyType.HASH)
                                    .build(),
                            KeySchemaElement.builder()
                                    .attributeName("routeType")
                                    .keyType(KeyType.RANGE)
                                    .build()
                    )
                    .attributeDefinitions(
                            AttributeDefinition.builder()
                                    .attributeName("userId")
                                    .attributeType(ScalarAttributeType.S)
                                    .build(),
                            AttributeDefinition.builder()
                                    .attributeName("routeType")
                                    .attributeType(ScalarAttributeType.S)
                                    .build()
                    )
                    .billingMode(BillingMode.PAY_PER_REQUEST)
                    .build();

            dynamoDbClient.createTable(request);
            log.info("Created table: {}", tableName);
            
            // Wait for table to be active
            waitForTableActive(tableName);
            
        } catch (ResourceInUseException e) {
            log.info("Table {} already exists", tableName);
        } catch (DynamoDbException e) {
            log.error("Failed to create table {}: {}", tableName, e.getMessage(), e);
            throw e;
        }
    }

    /**
     * Create user_matching table for storing user matching preferences
     */
    private void createUserMatchingTable() {
        String tableName = getTableName("user-matching");
        
        if (tableExists(tableName)) {
            log.info("Table {} already exists", tableName);
            return;
        }

        try {
            CreateTableRequest request = CreateTableRequest.builder()
                    .tableName(tableName)
                    .keySchema(
                            KeySchemaElement.builder()
                                    .attributeName("userId")
                                    .keyType(KeyType.HASH)
                                    .build()
                    )
                    .attributeDefinitions(
                            AttributeDefinition.builder()
                                    .attributeName("userId")
                                    .attributeType(ScalarAttributeType.S)
                                    .build()
                    )
                    .billingMode(BillingMode.PAY_PER_REQUEST)
                    .build();

            dynamoDbClient.createTable(request);
            log.info("Created table: {}", tableName);
            
            // Wait for table to be active
            waitForTableActive(tableName);
            
        } catch (ResourceInUseException e) {
            log.info("Table {} already exists", tableName);
        } catch (DynamoDbException e) {
            log.error("Failed to create table {}: {}", tableName, e.getMessage(), e);
            throw e;
        }
    }

    /**
     * Create real_time_location table for storing GPS coordinates during rides
     */
    private void createRealTimeLocationTable() {
        String tableName = getTableName("real-time-location");
        
        if (tableExists(tableName)) {
            log.info("Table {} already exists", tableName);
            return;
        }

        try {
            CreateTableRequest request = CreateTableRequest.builder()
                    .tableName(tableName)
                    .keySchema(
                            KeySchemaElement.builder()
                                    .attributeName("rideId")
                                    .keyType(KeyType.HASH)
                                    .build(),
                            KeySchemaElement.builder()
                                    .attributeName("timestamp")
                                    .keyType(KeyType.RANGE)
                                    .build()
                    )
                    .attributeDefinitions(
                            AttributeDefinition.builder()
                                    .attributeName("rideId")
                                    .attributeType(ScalarAttributeType.S)
                                    .build(),
                            AttributeDefinition.builder()
                                    .attributeName("timestamp")
                                    .attributeType(ScalarAttributeType.N)
                                    .build()
                    )
                    .billingMode(BillingMode.PAY_PER_REQUEST)
                    .build();

            dynamoDbClient.createTable(request);
            log.info("Created table: {}", tableName);
            
            // Wait for table to be active
            waitForTableActive(tableName);
            
        } catch (ResourceInUseException e) {
            log.info("Table {} already exists", tableName);
        } catch (DynamoDbException e) {
            log.error("Failed to create table {}: {}", tableName, e.getMessage(), e);
            throw e;
        }
    }

    /**
     * Check if a table exists
     */
    private boolean tableExists(String tableName) {
        try {
            DescribeTableRequest request = DescribeTableRequest.builder()
                    .tableName(tableName)
                    .build();
            
            dynamoDbClient.describeTable(request);
            return true;
        } catch (ResourceNotFoundException e) {
            return false;
        }
    }

    /**
     * Wait for table to become active
     */
    private void waitForTableActive(String tableName) {
        try {
            DynamoDbWaiter waiter = dynamoDbClient.waiter();
            WaiterResponse<DescribeTableResponse> waiterResponse = waiter.waitUntilTableExists(
                    DescribeTableRequest.builder()
                            .tableName(tableName)
                            .build()
            );
            
            log.info("Table {} is now active", tableName);
            waiter.close();
        } catch (Exception e) {
            log.warn("Failed to wait for table {} to become active: {}", tableName, e.getMessage());
        }
    }

    /**
     * Get full table name with prefix
     */
    private String getTableName(String baseTableName) {
        String prefix = awsProperties.getDynamodb().getTablePrefix();
        return prefix + "_" + baseTableName;
    }
}
