package com.shopflow.monolith;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Starts the monolith against a database that looks like production did before Flyway: tables built by Hibernate,
 * no Flyway history, real data. This is the upgrade path the live deployment takes — Flyway must adopt the database
 * as version 0, V1 must change nothing that matters, existing rows must survive, and Hibernate's schema validation
 * must still pass.
 */
@Testcontainers(disabledWithoutDocker = true)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class LegacyDatabaseIntegrationTest {

    @Container
    static final PostgreSQLContainer<?> POSTGRES = IntegrationTestSupport.postgres().withInitScript("legacy-schema.sql");
    @Container
    static final GenericContainer<?> REDIS = IntegrationTestSupport.redis();

    @DynamicPropertySource
    static void containers(DynamicPropertyRegistry registry) {
        IntegrationTestSupport.register(registry, POSTGRES, REDIS);
    }

    @Autowired TestRestTemplate http;
    @Autowired JdbcTemplate jdbc;

    @Test
    void flywayAdoptsTheExistingDatabaseAndKeepsItsData() {
        List<Map<String, Object>> history = jdbc.queryForList(
                "select version, description from flyway_schema_history where version is not null order by installed_rank");
        assertThat(history).extracting(row -> row.get("version")).containsExactly("0", "1");

        // Existing rows are untouched...
        assertThat(jdbc.queryForObject("select count(*) from users where email = 'legacy@shopflow.test'", Integer.class)).isEqualTo(1);
        assertThat(jdbc.queryForObject(
                "select status from orders where id = '11111111-1111-1111-1111-111111111111'::uuid", String.class)).isEqualTo("CONFIRMED");
        assertThat(jdbc.queryForObject("select count(*) from order_items", Integer.class)).isEqualTo(1);

        // ...and the columns added after the first release now exist (empty for old orders)
        assertThat(jdbc.queryForObject(
                "select ship_line1 from orders where id = '11111111-1111-1111-1111-111111111111'::uuid", String.class)).isNull();
        assertThat(jdbc.queryForList(
                "select column_name from information_schema.columns where table_name = 'orders' and column_name in ('payment_intent_id','ship_phone')",
                String.class)).hasSize(2);
    }

    @Test
    void theApiServesOverTheUpgradedDatabase() {
        assertThat(http.getForEntity("/api/v1/products", String.class).getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(http.getForEntity("/api/v1/categories", String.class).getBody()).contains("Electronics");
    }
}
