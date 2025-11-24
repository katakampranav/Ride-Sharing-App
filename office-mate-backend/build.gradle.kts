plugins {
    java
    id("org.springframework.boot") version "3.2.0"
    id("io.spring.dependency-management") version "1.1.4"
}

group = "com.officemate"
version = "1.0.0"

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(19)
    }
}

repositories {
    mavenCentral()
}

dependencies {
    // Spring Boot Starters
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    implementation("org.springframework.boot:spring-boot-starter-validation")
    implementation("org.springframework.boot:spring-boot-starter-security")
    implementation("org.springframework.boot:spring-boot-starter-aop")
    implementation("org.springframework.boot:spring-boot-starter-actuator")
    // PostgreSQL - Primary relational database
    runtimeOnly("org.postgresql:postgresql:42.7.1")
    
    // Redis - Session storage and OTP caching
    implementation("org.springframework.boot:spring-boot-starter-data-redis")
    implementation("redis.clients:jedis:5.1.0")
    
    // MongoDB - For migration support
    implementation("org.springframework.boot:spring-boot-starter-data-mongodb")
    
    // AWS SDK - DynamoDB for route preferences
    implementation(platform("software.amazon.awssdk:bom:2.21.0"))
    implementation("software.amazon.awssdk:dynamodb")
    implementation("software.amazon.awssdk:dynamodb-enhanced")
    
    // AWS SDK - SNS for SMS delivery
    implementation("software.amazon.awssdk:sns")
    
    // AWS SDK - SES for email delivery
    implementation("software.amazon.awssdk:ses")
    
    // AWS SDK - KMS for encryption
    implementation("software.amazon.awssdk:kms")
    
    // JWT Token Support
    implementation("io.jsonwebtoken:jjwt-api:0.12.3")
    runtimeOnly("io.jsonwebtoken:jjwt-impl:0.12.3")
    runtimeOnly("io.jsonwebtoken:jjwt-jackson:0.12.3")
    
    // Phone Number Validation
    implementation("com.googlecode.libphonenumber:libphonenumber:8.13.26")
    
    // Logstash encoder for structured logging
    implementation("net.logstash.logback:logstash-logback-encoder:7.4")
    
    // AspectJ for audit logging
    implementation("org.springframework:spring-aspects")
    
    // Lombok - Reduce boilerplate code
    compileOnly("org.projectlombok:lombok:1.18.34")
    annotationProcessor("org.projectlombok:lombok:1.18.34")
    testCompileOnly("org.projectlombok:lombok:1.18.34")
    testAnnotationProcessor("org.projectlombok:lombok:1.18.34")
    
    // Database (H2 for testing/development)
    runtimeOnly("com.h2database:h2")
    
    // Testing
    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("org.springframework.security:spring-security-test")
    testImplementation("org.testcontainers:testcontainers:1.19.3")
    testImplementation("org.testcontainers:postgresql:1.19.3")
    testImplementation("org.testcontainers:junit-jupiter:1.19.3")
}

tasks.withType<Test> {
    useJUnitPlatform()
}

tasks.named<org.springframework.boot.gradle.tasks.bundling.BootJar>("bootJar") {
    archiveFileName.set("officemate-${version}.jar")
    launchScript()
}
