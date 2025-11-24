package com.officemate.config.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.HashMap;
import java.util.Map;

@Data
@Configuration
@ConfigurationProperties(prefix = "aws")
public class AwsProperties {
    private String region;
    private String accessKeyId;
    private String secretAccessKey;
    private DynamoDbProperties dynamodb = new DynamoDbProperties();
    private SnsProperties sns = new SnsProperties();
    private SesProperties ses = new SesProperties();
    private KmsProperties kms = new KmsProperties();

    @Data
    public static class DynamoDbProperties {
        private String endpoint;
        private String tablePrefix;
        private Map<String, String> tables = new HashMap<>();
        private Integer readCapacityUnits = 5;
        private Integer writeCapacityUnits = 5;
    }

    @Data
    public static class SnsProperties {
        private String senderId;
        private String smsType = "Transactional";
        private String maxPrice = "0.50";
    }

    @Data
    public static class SesProperties {
        private String fromEmail;
        private String fromName;
        private String configurationSet;
    }

    @Data
    public static class KmsProperties {
        private String keyId;
        private String keyAlias;
    }
}
