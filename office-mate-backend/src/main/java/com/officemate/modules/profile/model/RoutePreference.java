package com.officemate.modules.profile.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbBean;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbPartitionKey;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbSortKey;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbAttribute;

import java.time.Instant;
import java.util.List;

/**
 * DynamoDB model for route preferences.
 * Stores driver and rider route information with geospatial data.
 * Uses composite key: userId (partition key) + routeType (sort key)
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@DynamoDbBean
public class RoutePreference {

    /**
     * User's unique identifier (partition key)
     */
    private String userId;

    /**
     * Route type: HOME_TO_WORK or WORK_TO_HOME (sort key)
     */
    private String routeType;

    /**
     * Starting point latitude
     */
    private Double startLatitude;

    /**
     * Starting point longitude
     */
    private Double startLongitude;

    /**
     * Human-readable starting address
     */
    private String startAddress;

    /**
     * Destination latitude
     */
    private Double endLatitude;

    /**
     * Destination longitude
     */
    private Double endLongitude;

    /**
     * Human-readable destination address
     */
    private String endAddress;

    /**
     * Preferred start times in HH:mm format
     */
    private List<String> preferredStartTimes;

    /**
     * Flag indicating if this route is currently active
     */
    private boolean isActive;

    /**
     * Timestamp when the route preference was created
     */
    private Instant createdAt;

    /**
     * Timestamp when the route preference was last updated
     */
    private Instant updatedAt;

    @DynamoDbPartitionKey
    @DynamoDbAttribute("userId")
    public String getUserId() {
        return userId;
    }

    @DynamoDbSortKey
    @DynamoDbAttribute("routeType")
    public String getRouteType() {
        return routeType;
    }

    @DynamoDbAttribute("startLatitude")
    public Double getStartLatitude() {
        return startLatitude;
    }

    @DynamoDbAttribute("startLongitude")
    public Double getStartLongitude() {
        return startLongitude;
    }

    @DynamoDbAttribute("startAddress")
    public String getStartAddress() {
        return startAddress;
    }

    @DynamoDbAttribute("endLatitude")
    public Double getEndLatitude() {
        return endLatitude;
    }

    @DynamoDbAttribute("endLongitude")
    public Double getEndLongitude() {
        return endLongitude;
    }

    @DynamoDbAttribute("endAddress")
    public String getEndAddress() {
        return endAddress;
    }

    @DynamoDbAttribute("preferredStartTimes")
    public List<String> getPreferredStartTimes() {
        return preferredStartTimes;
    }

    @DynamoDbAttribute("isActive")
    public boolean isActive() {
        return isActive;
    }

    @DynamoDbAttribute("createdAt")
    public Instant getCreatedAt() {
        return createdAt;
    }

    @DynamoDbAttribute("updatedAt")
    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
