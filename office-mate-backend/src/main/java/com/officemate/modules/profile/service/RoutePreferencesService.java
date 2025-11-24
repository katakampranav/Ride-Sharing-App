package com.officemate.modules.profile.service;

import com.officemate.modules.profile.model.RoutePreference;
import com.officemate.shared.dto.RoutePreferencesDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.enhanced.dynamodb.Key;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;
import software.amazon.awssdk.enhanced.dynamodb.model.QueryConditional;
import software.amazon.awssdk.services.dynamodb.model.DynamoDbException;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Service for managing route preferences in DynamoDB.
 * Handles storage and retrieval of driver and rider route preferences
 * with geospatial indexing support.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class RoutePreferencesService {

    private final DynamoDbEnhancedClient dynamoDbEnhancedClient;

    @Value("${aws.dynamodb.table-prefix}")
    private String tablePrefix;

    private static final String ROUTE_PREFERENCES_TABLE = "route_preferences";
    private static final String HOME_TO_WORK = "HOME_TO_WORK";
    private static final String WORK_TO_HOME = "WORK_TO_HOME";

    /**
     * Saves driver route preferences to DynamoDB.
     * 
     * @param userId The driver's unique identifier
     * @param routeDTO Route preferences DTO
     */
    public void saveDriverRoutePreferences(UUID userId, RoutePreferencesDTO routeDTO) {
        log.info("Saving route preferences for driver: {}", userId);

        try {
            DynamoDbTable<RoutePreference> table = getRoutePreferencesTable();

            // Save HOME_TO_WORK route
            RoutePreference homeToWork = buildRoutePreference(userId, HOME_TO_WORK, routeDTO);
            table.putItem(homeToWork);

            // Save WORK_TO_HOME route (reverse direction)
            RoutePreference workToHome = buildReverseRoutePreference(userId, WORK_TO_HOME, routeDTO);
            table.putItem(workToHome);

            log.info("Successfully saved route preferences for driver: {}", userId);
        } catch (DynamoDbException e) {
            log.error("Failed to save route preferences for driver {}: {}", userId, e.getMessage(), e);
            throw new RuntimeException("Failed to save route preferences", e);
        }
    }

    /**
     * Updates driver route preferences in DynamoDB.
     * 
     * @param userId The driver's unique identifier
     * @param routeDTO Updated route preferences DTO
     */
    public void updateDriverRoutePreferences(UUID userId, RoutePreferencesDTO routeDTO) {
        log.info("Updating route preferences for driver: {}", userId);
        
        // For updates, we simply overwrite the existing items
        saveDriverRoutePreferences(userId, routeDTO);
    }

    /**
     * Retrieves driver route preferences from DynamoDB.
     * 
     * @param userId The driver's unique identifier
     * @param routeType Route type (HOME_TO_WORK or WORK_TO_HOME)
     * @return RoutePreference entity or null if not found
     */
    public RoutePreference getDriverRoutePreferences(UUID userId, String routeType) {
        log.debug("Retrieving route preferences for driver: {} with type: {}", userId, routeType);

        try {
            DynamoDbTable<RoutePreference> table = getRoutePreferencesTable();

            Key key = Key.builder()
                .partitionValue(userId.toString())
                .sortValue(routeType)
                .build();

            RoutePreference preference = table.getItem(key);
            
            if (preference != null) {
                log.debug("Found route preferences for driver: {}", userId);
            } else {
                log.debug("No route preferences found for driver: {}", userId);
            }

            return preference;
        } catch (DynamoDbException e) {
            log.error("Failed to retrieve route preferences for driver {}: {}", userId, e.getMessage(), e);
            throw new RuntimeException("Failed to retrieve route preferences", e);
        }
    }

    /**
     * Retrieves all route preferences for a driver.
     * 
     * @param userId The driver's unique identifier
     * @return List of RoutePreference entities
     */
    public List<RoutePreference> getAllDriverRoutePreferences(UUID userId) {
        log.debug("Retrieving all route preferences for driver: {}", userId);

        try {
            DynamoDbTable<RoutePreference> table = getRoutePreferencesTable();

            QueryConditional queryConditional = QueryConditional
                .keyEqualTo(Key.builder().partitionValue(userId.toString()).build());

            List<RoutePreference> preferences = table.query(queryConditional)
                .items()
                .stream()
                .collect(Collectors.toList());

            log.debug("Found {} route preferences for driver: {}", preferences.size(), userId);
            return preferences;
        } catch (DynamoDbException e) {
            log.error("Failed to retrieve all route preferences for driver {}: {}", userId, e.getMessage(), e);
            throw new RuntimeException("Failed to retrieve route preferences", e);
        }
    }

    /**
     * Deletes driver route preferences from DynamoDB.
     * 
     * @param userId The driver's unique identifier
     */
    public void deleteDriverRoutePreferences(UUID userId) {
        log.info("Deleting route preferences for driver: {}", userId);

        try {
            DynamoDbTable<RoutePreference> table = getRoutePreferencesTable();

            // Delete HOME_TO_WORK route
            Key homeToWorkKey = Key.builder()
                .partitionValue(userId.toString())
                .sortValue(HOME_TO_WORK)
                .build();
            table.deleteItem(homeToWorkKey);

            // Delete WORK_TO_HOME route
            Key workToHomeKey = Key.builder()
                .partitionValue(userId.toString())
                .sortValue(WORK_TO_HOME)
                .build();
            table.deleteItem(workToHomeKey);

            log.info("Successfully deleted route preferences for driver: {}", userId);
        } catch (DynamoDbException e) {
            log.error("Failed to delete route preferences for driver {}: {}", userId, e.getMessage(), e);
            throw new RuntimeException("Failed to delete route preferences", e);
        }
    }

    /**
     * Saves rider route preferences to DynamoDB.
     * 
     * @param userId The rider's unique identifier
     * @param routeDTO Route preferences DTO
     */
    public void saveRiderRoutePreferences(UUID userId, RoutePreferencesDTO routeDTO) {
        log.info("Saving route preferences for rider: {}", userId);
        
        // Riders use the same structure as drivers
        saveDriverRoutePreferences(userId, routeDTO);
    }

    /**
     * Updates rider route preferences in DynamoDB.
     * 
     * @param userId The rider's unique identifier
     * @param routeDTO Updated route preferences DTO
     */
    public void updateRiderRoutePreferences(UUID userId, RoutePreferencesDTO routeDTO) {
        log.info("Updating route preferences for rider: {}", userId);
        
        updateDriverRoutePreferences(userId, routeDTO);
    }

    /**
     * Deletes rider route preferences from DynamoDB.
     * 
     * @param userId The rider's unique identifier
     */
    public void deleteRiderRoutePreferences(UUID userId) {
        log.info("Deleting route preferences for rider: {}", userId);
        
        deleteDriverRoutePreferences(userId);
    }

    /**
     * Gets the DynamoDB table for route preferences.
     * 
     * @return DynamoDbTable instance
     */
    private DynamoDbTable<RoutePreference> getRoutePreferencesTable() {
        String tableName = tablePrefix + "_" + ROUTE_PREFERENCES_TABLE;
        return dynamoDbEnhancedClient.table(tableName, TableSchema.fromBean(RoutePreference.class));
    }

    /**
     * Builds a RoutePreference entity from DTO.
     * 
     * @param userId User's unique identifier
     * @param routeType Route type (HOME_TO_WORK or WORK_TO_HOME)
     * @param routeDTO Route preferences DTO
     * @return RoutePreference entity
     */
    private RoutePreference buildRoutePreference(UUID userId, String routeType, RoutePreferencesDTO routeDTO) {
        RoutePreference preference = new RoutePreference();
        preference.setUserId(userId.toString());
        preference.setRouteType(routeType);
        preference.setStartLatitude(routeDTO.getStartLatitude());
        preference.setStartLongitude(routeDTO.getStartLongitude());
        preference.setStartAddress(routeDTO.getStartAddress());
        preference.setEndLatitude(routeDTO.getEndLatitude());
        preference.setEndLongitude(routeDTO.getEndLongitude());
        preference.setEndAddress(routeDTO.getEndAddress());
        preference.setPreferredStartTimes(routeDTO.getPreferredStartTimes());
        preference.setActive(routeDTO.isActive());
        preference.setCreatedAt(Instant.now());
        preference.setUpdatedAt(Instant.now());
        return preference;
    }

    /**
     * Builds a reverse RoutePreference entity (swaps start and end coordinates).
     * 
     * @param userId User's unique identifier
     * @param routeType Route type (WORK_TO_HOME)
     * @param routeDTO Route preferences DTO
     * @return RoutePreference entity with reversed coordinates
     */
    private RoutePreference buildReverseRoutePreference(UUID userId, String routeType, RoutePreferencesDTO routeDTO) {
        RoutePreference preference = new RoutePreference();
        preference.setUserId(userId.toString());
        preference.setRouteType(routeType);
        // Swap start and end for return journey
        preference.setStartLatitude(routeDTO.getEndLatitude());
        preference.setStartLongitude(routeDTO.getEndLongitude());
        preference.setStartAddress(routeDTO.getEndAddress());
        preference.setEndLatitude(routeDTO.getStartLatitude());
        preference.setEndLongitude(routeDTO.getStartLongitude());
        preference.setEndAddress(routeDTO.getStartAddress());
        preference.setPreferredStartTimes(routeDTO.getPreferredStartTimes());
        preference.setActive(routeDTO.isActive());
        preference.setCreatedAt(Instant.now());
        preference.setUpdatedAt(Instant.now());
        return preference;
    }
}
