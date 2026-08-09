package com.campuscart.support;

import com.campuscart.common.util.SecureRandomTokens;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * Base class for integration tests that need the real schema.
 *
 * <p>Boots one MySQL 8 container shared across every subclass (started once, reused,
 * torn down at JVM exit) and points Spring's datasource at it. Flyway then applies the
 * production migrations and Hibernate runs its {@code validate} check, so these tests
 * exercise the exact schema shipped to production rather than an H2 approximation.</p>
 *
 * <p>Requires a running Docker daemon.</p>
 */
@SpringBootTest
@Testcontainers
public abstract class AbstractMySqlIntegrationTest {

    private static final String TEST_JWT_SECRET = SecureRandomTokens.urlSafeToken(32);

    // Static + manually started so the single container instance is reused by all
    // subclasses (JUnit's @Container would start/stop it per test class).
    private static final MySQLContainer<?> MYSQL =
            new MySQLContainer<>("mysql:8.0")
                    .withDatabaseName("campuscart")
                    .withUsername("campuscart")
                    .withPassword("campuscart");

    static {
        MYSQL.start();
    }

    @DynamicPropertySource
    static void datasourceProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", MYSQL::getJdbcUrl);
        registry.add("spring.datasource.username", MYSQL::getUsername);
        registry.add("spring.datasource.password", MYSQL::getPassword);
        // Test-only key; production configuration has no default JWT secret.
        registry.add("security.jwt.secret", () -> TEST_JWT_SECRET);
    }
}
