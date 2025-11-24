package com.officemate.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.mongodb.repository.config.EnableMongoRepositories;

/**
 * MongoDB configuration for migration support.
 * MongoDB is used temporarily during migration from legacy system.
 * This will be phased out as data is migrated to PostgreSQL and DynamoDB.
 */
@Configuration
@EnableMongoRepositories(basePackages = "com.officemate.legacy.repository")
public class MongoDBConfig {
    // MongoDB configuration is handled by Spring Boot auto-configuration
    // This class exists to enable MongoDB repositories in a specific package
    // to avoid conflicts with JPA repositories
}
