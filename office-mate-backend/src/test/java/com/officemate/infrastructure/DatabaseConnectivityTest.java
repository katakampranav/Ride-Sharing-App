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

import javax.sql.DataSource;
import java.sql.Connection;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests database connectivity for PostgreSQL and Redis.
 * Verifies that connections can be established and basic operations work.
 */
@SpringBootTest
@ActiveProfiles("test")
@Disabled
class DatabaseConnectivityTest {

    @Autowired
    private DataSource dataSource;

    @Autowired
    private UserAccountRepository userAccountRepository;

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    @Test
    void postgresConnectionIsEstablished() throws Exception {
        try (Connection connection = dataSource.getConnection()) {
            assertThat(connection).isNotNull();
            assertThat(connection.isValid(5)).isTrue();
        }
    }

    @Test
    void postgresRepositoryOperationsWork() {
        // Create test user account
        UserAccount account = UserAccount.builder()
                .phoneNumber("+1234567890")
                .phoneVerified(true)
                .accountStatus(AccountStatus.ACTIVE)
                .build();

        // Save and verify
        UserAccount saved = userAccountRepository.save(account);
        assertThat(saved.getUserId()).isNotNull();
        assertThat(saved.getPhoneNumber()).isEqualTo("+1234567890");

        // Find and verify
        UserAccount found = userAccountRepository.findByPhoneNumber("+1234567890").orElse(null);
        assertThat(found).isNotNull();
        assertThat(found.getUserId()).isEqualTo(saved.getUserId());

        // Cleanup
        userAccountRepository.delete(saved);
    }

    @Test
    void redisConnectionIsEstablished() {
        assertThat(redisTemplate).isNotNull();
        assertThat(redisTemplate.getConnectionFactory()).isNotNull();
    }

    @Test
    void redisOperationsWork() {
        String testKey = "test:connectivity:" + System.currentTimeMillis();
        String testValue = "test-value";

        // Set value
        redisTemplate.opsForValue().set(testKey, testValue);

        // Get value
        Object retrieved = redisTemplate.opsForValue().get(testKey);
        assertThat(retrieved).isEqualTo(testValue);

        // Delete value
        redisTemplate.delete(testKey);
        assertThat(redisTemplate.hasKey(testKey)).isFalse();
    }
}
