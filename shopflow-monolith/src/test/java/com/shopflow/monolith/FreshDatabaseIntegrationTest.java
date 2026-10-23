package com.shopflow.monolith;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

/**
 * Boots the whole monolith against an empty Postgres. Passing means Flyway built the schema and Hibernate's
 * {@code ddl-auto: validate} agreed with it, and that the order flow works end to end on top of it.
 */
@Testcontainers(disabledWithoutDocker = true)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = IntegrationTestSupport.APP_PROPERTIES)
class FreshDatabaseIntegrationTest {

    @Container static final PostgreSQLContainer<?> POSTGRES = IntegrationTestSupport.postgres();
    @Container static final GenericContainer<?> REDIS = IntegrationTestSupport.redis();

    @DynamicPropertySource
    static void containers(DynamicPropertyRegistry registry) {
        IntegrationTestSupport.register(registry, POSTGRES, REDIS);
    }

    @Autowired TestRestTemplate http;
    @Autowired JdbcTemplate jdbc;

    @Test
    void flywayBuildsTheWholeSchemaFromScratch() {
        List<Map<String, Object>> history = jdbc.queryForList(
                "select version, success from flyway_schema_history where version is not null order by installed_rank");
        assertThat(history).hasSize(1);
        assertThat(history.get(0)).containsEntry("version", "1").containsEntry("success", true);

        List<String> tables = jdbc.queryForList(
                "select table_name from information_schema.tables where table_schema = 'public'", String.class);
        assertThat(tables).contains("users", "addresses", "categories", "products", "inventory",
                "stock_reservations", "orders", "order_items");
    }

    @Test
    void anOrderIsPlacedConfirmedAndRestockedWhenCancelled() {
        String admin = login(IntegrationTestSupport.ADMIN_EMAIL, IntegrationTestSupport.ADMIN_PASSWORD);

        String sku = "IT-" + UUID.randomUUID().toString().substring(0, 8);
        JsonNode product = post("/api/v1/products", admin,
                Map.of("name", "Integration Widget", "description", "test", "price", 25.00, "sku", sku))
                .getBody();
        String productId = product.get("id").asText();
        post("/api/v1/inventory", admin, Map.of("productId", productId, "productName", "Integration Widget", "quantityToAdd", 10));

        String email = "buyer-" + UUID.randomUUID().toString().substring(0, 8) + "@test.com";
        String user = register("Test Buyer", email, "buyer-password-1");

        // An order must carry a delivery address
        ResponseEntity<JsonNode> noAddress = post("/api/v1/orders", user,
                Map.of("items", List.of(Map.of("productId", productId, "quantity", 1))));
        assertThat(noAddress.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);

        ResponseEntity<JsonNode> placed = post("/api/v1/orders", user, Map.of(
                "items", List.of(Map.of("productId", productId, "quantity", 2)),
                "shippingAddress", Map.of("fullName", " Jane Doe ", "line1", "1 Main St", "city", "Irving",
                        "state", "TX", "postalCode", "75038", "country", "us")));
        assertThat(placed.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        JsonNode order = placed.getBody();
        String orderId = order.get("id").asText();
        assertThat(order.get("totalAmount").decimalValue()).isEqualByComparingTo("50.00");
        assertThat(order.get("shippingAddress").get("fullName").asText()).isEqualTo("Jane Doe");
        assertThat(order.get("shippingAddress").get("country").asText()).isEqualTo("US");

        // Payments are off here, so fulfilment confirms the order on its own.
        // State is polled straight from the database — the API rate limit would trip on HTTP polling.
        await().atMost(Duration.ofSeconds(30)).pollInterval(Duration.ofMillis(250))
                .untilAsserted(() -> assertThat(orderStatus(orderId)).isEqualTo("CONFIRMED"));
        assertThat(availableStock(productId)).isEqualTo(8);

        ResponseEntity<JsonNode> mine = get("/api/v1/orders/" + orderId, user);
        assertThat(mine.getBody().get("status").asText()).isEqualTo("CONFIRMED");

        // Admin cancels the confirmed order: the stock goes back on the shelf
        ResponseEntity<JsonNode> cancelled = exchange(HttpMethod.PATCH, "/api/v1/admin/orders/" + orderId + "/status",
                admin, Map.of("status", "CANCELLED"));
        assertThat(cancelled.getStatusCode().is2xxSuccessful()).isTrue();
        await().atMost(Duration.ofSeconds(30)).pollInterval(Duration.ofMillis(250))
                .untilAsserted(() -> assertThat(availableStock(productId)).isEqualTo(10));
        assertThat(orderStatus(orderId)).isEqualTo("CANCELLED");
    }

    @Test
    void addressBookIsPerUser() {
        String alice = register("Alice", "alice-" + UUID.randomUUID() + "@test.com", "alice-password-1");
        String bob = register("Bob", "bob-" + UUID.randomUUID() + "@test.com", "bob-password-1");

        Map<String, Object> address = Map.of("fullName", "Alice A", "line1", "9 Elm St", "city", "Austin",
                "state", "TX", "postalCode", "73301", "country", "US");
        ResponseEntity<JsonNode> created = post("/api/v1/users/me/addresses", alice, address);
        assertThat(created.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(created.getBody().get("defaultAddress").asBoolean()).isTrue();

        assertThat(get("/api/v1/users/me/addresses", alice).getBody().size()).isEqualTo(1);
        assertThat(get("/api/v1/users/me/addresses", bob).getBody().size()).isZero();
        assertThat(exchange(HttpMethod.DELETE, "/api/v1/users/me/addresses/" + created.getBody().get("id").asText(), bob, null)
                .getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    // ------------------------------------------------------------------ helpers

    private String orderStatus(String orderId) {
        return jdbc.queryForObject("select status from orders where id = ?::uuid", String.class, orderId);
    }

    private int availableStock(String productId) {
        return jdbc.queryForObject(
                "select quantity - reserved_quantity from inventory where product_id = ?::uuid", Integer.class, productId);
    }

    private String login(String email, String password) {
        ResponseEntity<JsonNode> res = http.postForEntity("/api/v1/auth/login",
                Map.of("email", email, "password", password), JsonNode.class);
        assertThat(res.getStatusCode()).as("login " + email).isEqualTo(HttpStatus.OK);
        return res.getBody().get("token").asText();
    }

    private String register(String name, String email, String password) {
        ResponseEntity<JsonNode> res = http.postForEntity("/api/v1/auth/register",
                Map.of("name", name, "email", email, "password", password), JsonNode.class);
        assertThat(res.getStatusCode().is2xxSuccessful()).as("register " + email).isTrue();
        return res.getBody().get("token").asText();
    }

    private ResponseEntity<JsonNode> get(String path, String token) {
        return exchange(HttpMethod.GET, path, token, null);
    }

    private ResponseEntity<JsonNode> post(String path, String token, Object body) {
        return exchange(HttpMethod.POST, path, token, body);
    }

    private ResponseEntity<JsonNode> exchange(HttpMethod method, String path, String token, Object body) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);
        return http.exchange(path, method, new HttpEntity<>(body, headers), JsonNode.class);
    }
}
