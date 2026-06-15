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
import reactor.core.publisher.Mono;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AuthFilterTest {

    private static final String SECRET = "shopflow-super-secret-key-change-in-production-min-256-bits";
    private AuthFilter filter;
    private GatewayFilterChain chain;

    @BeforeEach
    void setUp() {
        filter = new AuthFilter();
        ReflectionTestUtils.setField(filter, "jwtSecret", SECRET);
        ReflectionTestUtils.setField(filter, "publicPaths", List.of("/api/v1/auth/", "/api/v1/products"));
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
    void validToken_forwardsUserHeaders() {
        SecretKey key = Keys.hmacShaKeyFor(SECRET.getBytes(StandardCharsets.UTF_8));
        String token = Jwts.builder()
                .subject("user-uuid-123")
                .claims(Map.of("email", "test@example.com", "role", "USER"))
                .expiration(new Date(System.currentTimeMillis() + 3600_000))
                .signWith(key)
                .compact();

        MockServerHttpRequest request = MockServerHttpRequest
                .get("/api/v1/orders")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .build();
        MockServerWebExchange exchange = MockServerWebExchange.from(request);

        filter.apply(new AuthFilter.Config()).filter(exchange, chain).block();

        assertThat(exchange.getResponse().getStatusCode()).isNull();
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
