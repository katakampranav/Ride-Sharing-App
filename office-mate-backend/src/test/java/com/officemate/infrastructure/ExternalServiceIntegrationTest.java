package com.officemate.infrastructure;

import com.officemate.modules.auth.entity.UserAccount;
import com.officemate.modules.auth.repository.UserAccountRepository;
import com.officemate.shared.enums.AccountStatus;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Disabled;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests external service integrations using Testcontainers.
 * Verifies connectivity and operations with PostgreSQL and Redis using real containers.
 */
@SpringBootTest
@ActiveProfiles("test")
@Testcontainers
@Disabled
class ExternalServiceIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>(DockerImageName.parse("postgres:15-alpine"))
            .withDatabaseName("testdb")
            .withUsername("test")
            .withPassword("test");

    @Container
    static GenericContainer<?> redis = new GenericContainer<>(DockerImageName.parse("redis:7-alpine"))
            .withExposedPorts(6379);

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.data.redis.host", redis::getHost);
        registry.add("spring.data.redis.port", () -> redis.getMappedPort(6379).toString());
    }

    @Autowired
    private UserAccountRepository userAccountRepository;

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    @Test
    void postgresContainerIsRunning() {
        assertThat(postgres.isRunning()).isTrue();
    }

    @Test
    void redisContainerIsRunning() {
        assertThat(redis.isRunning()).isTrue();
    }

    @Test
    void canConnectToPostgresContainer() {
        UserAccount account = UserAccount.builder()
                .phoneNumber("+1111111111")
                .phoneVerified(true)
                .accountStatus(AccountStatus.ACTIVE)
                .build();

        UserAccount saved = userAccountRepository.save(account);
        assertThat(saved.getUserId()).isNotNull();

        UserAccount found = userAccountRepository.findById(saved.getUserId()).orElse(null);
        assertThat(found).isNotNull();
        assertThat(found.getPhoneNumber()).isEqualTo("+1111111111");
    }

    @Test
    void canConnectToRedisContainer() {
        String testKey = "testcontainer:test:" + System.currentTimeMillis();
        String testValue = "container-test-value";

        redisTemplate.opsForValue().set(testKey, testValue);
        Object retrieved = redisTemplate.opsForValue().get(testKey);

        assertThat(retrieved).isEqualTo(testValue);

        redisTemplate.delete(testKey);
    }

    @Test
    void redisExpirationWorks() throws InterruptedException {
        String testKey = "testcontainer:expiry:" + System.currentTimeMillis();
        String testValue = "expiring-value";

        redisTemplate.opsForValue().set(testKey, testValue, 2, TimeUnit.SECONDS);
        assertThat(redisTemplate.hasKey(testKey)).isTrue();

        Thread.sleep(3000);
        assertThat(redisTemplate.hasKey(testKey)).isFalse();
    }

    @Test
    void postgresTransactionsWork() {
        UserAccount account1 = UserAccount.builder()
                .phoneNumber("+2222222222")
                .phoneVerified(true)
                .accountStatus(AccountStatus.ACTIVE)
                .build();

        UserAccount account2 = UserAccount.builder()
                .phoneNumber("+3333333333")
                .phoneVerified(true)
                .accountStatus(AccountStatus.ACTIVE)
                .build();

        userAccountRepository.save(account1);
        userAccountRepository.save(account2);

        long count = userAccountRepository.count();
        assertThat(count).isGreaterThanOrEqualTo(2);
    }
}
