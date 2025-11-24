package com.officemate.config.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Data
@Configuration
@ConfigurationProperties(prefix = "app.session")
public class SessionProperties {
    private Integer maxConcurrentSessions = 5;
    private Integer tokenBlacklistCleanupHours = 24;
}
