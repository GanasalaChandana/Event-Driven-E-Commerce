package com.shopflow.monolith;

import org.springframework.test.context.DynamicPropertyRegistry;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.PostgreSQLContainer;

/** Shared wiring for the integration tests: container properties and the settings the app needs to boot. */
final class IntegrationTestSupport {

    static final String ADMIN_EMAIL = "admin@shopflow.com";
    static final String ADMIN_PASSWORD = "Integration-Test-Admin-1";

    private IntegrationTestSupport() {}

    static PostgreSQLContainer<?> postgres() {
        return new PostgreSQLContainer<>("postgres:16-alpine");
    }

    static GenericContainer<?> redis() {
        return new GenericContainer<>("redis:7-alpine").withExposedPorts(6379);
    }

    static void register(DynamicPropertyRegistry registry, PostgreSQLContainer<?> postgres, GenericContainer<?> redis) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.data.redis.host", redis::getHost);
        registry.add("spring.data.redis.port", () -> redis.getMappedPort(6379));
    }

    /** Properties for {@code @SpringBootTest}; no Kafka, mail provider or Stripe is available in tests. */
    static final String[] APP_PROPERTIES = {
            "jwt.secret=integration-test-secret-key-that-is-long-enough-for-hs384-signing",
            "admin.password=" + ADMIN_PASSWORD,
            "management.tracing.enabled=false",
    };
}
