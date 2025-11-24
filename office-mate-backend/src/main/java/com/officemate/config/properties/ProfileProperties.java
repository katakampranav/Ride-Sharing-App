package com.officemate.config.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Data
@Configuration
@ConfigurationProperties(prefix = "app.profile")
public class ProfileProperties {
    private DriverProperties driver = new DriverProperties();
    private RiderProperties rider = new RiderProperties();

    @Data
    public static class DriverProperties {
        private Integer maxDetourDistance = 500;
        private Boolean licenseVerificationRequired = true;
    }

    @Data
    public static class RiderProperties {
        private GenderPreferencesProperties genderPreferences = new GenderPreferencesProperties();
        private Integer maxFavoriteDrivers = 10;
    }

    @Data
    public static class GenderPreferencesProperties {
        private List<String> allowed = List.of("FEMALE_ONLY", "MALE_SINGLE_FEMALE", "MALE_ALL_FEMALE", "NO_PREFERENCE");
    }
}
