package com.officemate.config.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Data
@Configuration
@ConfigurationProperties(prefix = "app.cancellation.tracking")
public class CancellationProperties {
    private Boolean enabled = true;
    private Integer maxPerMonth = 5;
    private Integer suspensionDurationMonths = 3;
}
