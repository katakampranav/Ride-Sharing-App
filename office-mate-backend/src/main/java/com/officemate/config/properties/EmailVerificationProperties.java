package com.officemate.config.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Data
@Configuration
@ConfigurationProperties(prefix = "app.email.verification")
public class EmailVerificationProperties {
    private Integer otpExpirationMinutes = 10;
    private Integer maxAttempts = 3;
    private Integer resendCooldownSeconds = 60;
}
