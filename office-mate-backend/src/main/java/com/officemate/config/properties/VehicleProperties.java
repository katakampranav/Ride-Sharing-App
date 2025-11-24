package com.officemate.config.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Data
@Configuration
@ConfigurationProperties(prefix = "app.vehicle")
public class VehicleProperties {
    private TypesProperties types = new TypesProperties();
    private FuelTypesProperties fuelTypes = new FuelTypesProperties();
    private CapacityProperties capacity = new CapacityProperties();

    @Data
    public static class TypesProperties {
        private List<String> allowed = List.of("CAR", "MOTORCYCLE", "SCOOTER", "BICYCLE");
    }

    @Data
    public static class FuelTypesProperties {
        private List<String> allowed = List.of("PETROL", "DIESEL", "ELECTRIC", "HYBRID", "CNG");
    }

    @Data
    public static class CapacityProperties {
        private VehicleCapacity car = new VehicleCapacity(1, 7);
        private VehicleCapacity motorcycle = new VehicleCapacity(1, 2);
        private VehicleCapacity scooter = new VehicleCapacity(1, 2);
        private VehicleCapacity bicycle = new VehicleCapacity(1, 1);
    }

    @Data
    public static class VehicleCapacity {
        private Integer min;
        private Integer max;

        public VehicleCapacity() {
        }

        public VehicleCapacity(Integer min, Integer max) {
            this.min = min;
            this.max = max;
        }
    }
}
