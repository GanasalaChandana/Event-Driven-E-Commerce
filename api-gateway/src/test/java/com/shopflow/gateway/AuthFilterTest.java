package com.shopflow.gateway;

import com.shopflow.gateway.filter.AuthFilter;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AuthFilterTest {

    private static final String SECRET = "test-only-secret-key-for-unit-tests-at-least-32-bytes-long";
    private AuthFilter filter;
    private GatewayFilterChain chain;

    @BeforeEach
    void setUp() {
        filter = new AuthFilter();
        ReflectionTestUtils.setField(filter, "jwtSecret", SECRET);
        ReflectionTestUtils.setField(filter, "publicPaths", List.of("/api/v1/auth/", "/actuator"));
        ReflectionTestUtils.setField(filter, "publicReadPaths", List.of("/api/v1/products", "/api/v1/categories"));
        ReflectionTestUtils.invokeMethod(filter, "init");
        chain = mock(GatewayFilterChain.class);
        when(chain.filter(any())).thenReturn(Mono.empty());
    }

    @Test
    void publicPath_passesWithoutToken() {
        MockServerHttpRequest request = MockServerHttpRequest
                .get("/api/v1/auth/login")
                .build();
        MockServerWebExchange exchange = MockServerWebExchange.from(request);

        filter.apply(new AuthFilter.Config()).filter(exchange, chain).block();

        assertThat(exchange.getResponse().getStatusCode()).isNull();
    }

    @Test
    void missingToken_returns401() {
        MockServerHttpRequest request = MockServerHttpRequest
                .get("/api/v1/orders")
                .build();
        MockServerWebExchange exchange = MockServerWebExchange.from(request);

        filter.apply(new AuthFilter.Config()).filter(exchange, chain).block();

        assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void validToken_forwardsIdentityHeaders_usingUserIdClaimNotEmail() {
        String token = tokenFor("user-uuid-123", "test@example.com", "USER");

        MockServerHttpRequest request = MockServerHttpRequest
                .get("/api/v1/orders")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .build();
        MockServerWebExchange exchange = MockServerWebExchange.from(request);

        ServerWebExchange forwarded = run(exchange);

        assertThat(exchange.getResponse().getStatusCode()).isNull();
        HttpHeaders headers = forwarded.getRequest().getHeaders();
        assertThat(headers.getFirst("X-User-Id")).isEqualTo("user-uuid-123");
        assertThat(headers.getFirst("X-User-Email")).isEqualTo("test@example.com");
        assertThat(headers.getFirst("X-User-Role")).isEqualTo("USER");
    }

    @Test
    void spoofedIdentityHeaders_areOverwrittenWithVerifiedOnes() {
        String token = tokenFor("real-id", "real@example.com", "USER");

        MockServerHttpRequest request = MockServerHttpRequest
                .get("/api/v1/orders")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .header("X-User-Role", "ADMIN")
                .header("X-User-Id", "someone-else")
                .build();

        ServerWebExchange forwarded = run(MockServerWebExchange.from(request));

        assertThat(forwarded.getRequest().getHeaders().get("X-User-Role")).containsExactly("USER");
        assertThat(forwarded.getRequest().getHeaders().get("X-User-Id")).containsExactly("real-id");
    }

    @Test
    void spoofedIdentityHeaders_areStrippedOnPublicPaths() {
        MockServerHttpRequest request = MockServerHttpRequest
                .get("/api/v1/products")
                .header("X-User-Role", "ADMIN")
                .header("X-User-Email", "admin@shopflow.com")
                .build();

        ServerWebExchange forwarded = run(MockServerWebExchange.from(request));

        assertThat(forwarded.getRequest().getHeaders().containsKey("X-User-Role")).isFalse();
        assertThat(forwarded.getRequest().getHeaders().containsKey("X-User-Email")).isFalse();
    }

    @Test
    void publicPath_withValidToken_stillForwardsIdentity() {
        String token = tokenFor("admin-id", "admin@shopflow.com", "ADMIN");
        MockServerHttpRequest request = MockServerHttpRequest
                .get("/api/v1/inventory")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .build();

        ServerWebExchange forwarded = run(MockServerWebExchange.from(request));

        assertThat(forwarded.getRequest().getHeaders().getFirst("X-User-Role")).isEqualTo("ADMIN");
    }

    @Test
    void publicReadPath_allowsGetButNotWrites() {
        MockServerWebExchange get = MockServerWebExchange.from(MockServerHttpRequest.get("/api/v1/products/abc").build());
        run(get);
        assertThat(get.getResponse().getStatusCode()).isNull();

        MockServerWebExchange post = MockServerWebExchange.from(MockServerHttpRequest.post("/api/v1/products").build());
        run(post);
        assertThat(post.getResponse().getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);

        MockServerWebExchange delete = MockServerWebExchange.from(MockServerHttpRequest.delete("/api/v1/categories/abc").build());
        run(delete);
        assertThat(delete.getResponse().getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void tokenWithoutUserIdClaim_returns401() {
        String token = Jwts.builder()
                .subject("a@b.com")
                .claims(Map.of("email", "a@b.com", "role", "USER"))
                .expiration(new Date(System.currentTimeMillis() + 3600_000))
                .signWith(Keys.hmacShaKeyFor(SECRET.getBytes(StandardCharsets.UTF_8)))
                .compact();

        MockServerWebExchange exchange = MockServerWebExchange.from(MockServerHttpRequest
                .get("/api/v1/orders").header(HttpHeaders.AUTHORIZATION, "Bearer " + token).build());
        run(exchange);

        assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void expiredToken_returns401() {
        String token = Jwts.builder()
                .subject("a@b.com")
                .claims(Map.of("userId", "id", "email", "a@b.com", "role", "USER"))
                .expiration(new Date(System.currentTimeMillis() - 1000))
                .signWith(Keys.hmacShaKeyFor(SECRET.getBytes(StandardCharsets.UTF_8)))
                .compact();

        MockServerWebExchange exchange = MockServerWebExchange.from(MockServerHttpRequest
                .get("/api/v1/orders").header(HttpHeaders.AUTHORIZATION, "Bearer " + token).build());
        run(exchange);

        assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void shortSecret_failsFast() {
        AuthFilter weak = new AuthFilter();
        ReflectionTestUtils.setField(weak, "jwtSecret", "too-short");
        org.assertj.core.api.Assertions.assertThatThrownBy(() -> ReflectionTestUtils.invokeMethod(weak, "init"))
                .isInstanceOf(IllegalStateException.class);
    }

    private String tokenFor(String userId, String email, String role) {
        SecretKey key = Keys.hmacShaKeyFor(SECRET.getBytes(StandardCharsets.UTF_8));
        return Jwts.builder()
                .subject(email)
                .claims(Map.of("userId", userId, "email", email, "role", role))
                .expiration(new Date(System.currentTimeMillis() + 3600_000))
                .signWith(key)
                .compact();
    }

    /** Runs the filter and returns the exchange the downstream chain actually received. */
    private ServerWebExchange run(MockServerWebExchange exchange) {
        AtomicReference<ServerWebExchange> seen = new AtomicReference<>(exchange);
        GatewayFilterChain capturing = e -> {
            seen.set(e);
            return Mono.empty();
        };
        filter.apply(new AuthFilter.Config()).filter(exchange, capturing).block();
        return seen.get();
    }

    @Test
    void invalidToken_returns401() {
        MockServerHttpRequest request = MockServerHttpRequest
                .get("/api/v1/orders")
                .header(HttpHeaders.AUTHORIZATION, "Bearer this.is.not.valid")
                .build();
        MockServerWebExchange exchange = MockServerWebExchange.from(request);

        filter.apply(new AuthFilter.Config()).filter(exchange, chain).block();

        assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }
}
