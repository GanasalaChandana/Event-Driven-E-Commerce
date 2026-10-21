package com.shopflow.gateway.filter;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * Verifies the JWT and forwards the caller's identity to downstream services as
 * X-User-Id / X-User-Email / X-User-Role headers.
 *
 * Downstream services trust those headers blindly, so this filter ALWAYS removes any
 * X-User-* headers the client sent — including on public paths — before forwarding.
 */
@Component
public class AuthFilter extends AbstractGatewayFilterFactory<AuthFilter.Config> {

    private static final int MIN_SECRET_BYTES = 32;

    @Value("${jwt.secret}")
    private String jwtSecret;

    /** Public for every HTTP method (login, register, health). */
    @Value("${gateway.public-paths}")
    private List<String> publicPaths;

    /** Public only for read methods (browsing the catalogue). */
    @Value("${gateway.public-read-paths:}")
    private List<String> publicReadPaths = List.of();

    private SecretKey key;

    public AuthFilter() {
        super(Config.class);
    }

    @PostConstruct
    void init() {
        if (jwtSecret == null || jwtSecret.getBytes(StandardCharsets.UTF_8).length < MIN_SECRET_BYTES) {
            throw new IllegalStateException(
                    "JWT_SECRET must be set and at least " + MIN_SECRET_BYTES + " bytes long");
        }
        key = Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8));
    }

    @Override
    public GatewayFilter apply(Config config) {
        return (exchange, chain) -> {
            ServerHttpRequest request = exchange.getRequest();
            String path = request.getURI().getPath();
            String authHeader = request.getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
            boolean hasBearer = authHeader != null && authHeader.startsWith("Bearer ");

            Identity identity = hasBearer ? verify(authHeader.substring(7)) : null;

            if (isPublic(request.getMethod(), path)) {
                // Public endpoints still get the caller's identity when a valid token is sent
                // (e.g. the admin-only stock list under the public-read /inventory prefix);
                // otherwise any client-supplied identity headers are dropped.
                return chain.filter(identity != null ? withIdentity(exchange, identity) : stripIdentityHeaders(exchange));
            }

            if (identity == null) {
                return onError(exchange, HttpStatus.UNAUTHORIZED);
            }
            return chain.filter(withIdentity(exchange, identity));
        };
    }

    private record Identity(String userId, String email, String role) {}

    /** Returns the verified identity, or null for an invalid/expired token or one missing claims. */
    private Identity verify(String token) {
        try {
            Claims claims = Jwts.parser()
                    .verifyWith(key)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();

            // The subject is the email; the user's UUID travels in the "userId" claim
            String userId = claims.get("userId", String.class);
            String email = claims.get("email", String.class);
            String role = claims.get("role", String.class);
            if (userId == null || email == null || role == null) {
                return null;
            }
            return new Identity(userId, email, role);
        } catch (Exception e) {
            return null;
        }
    }

    private ServerWebExchange withIdentity(ServerWebExchange exchange, Identity identity) {
        return exchange.mutate()
                .request(r -> r.headers(h -> {
                    removeIdentityHeaders(h);
                    h.set("X-User-Id", identity.userId());
                    h.set("X-User-Email", identity.email());
                    h.set("X-User-Role", identity.role());
                }))
                .build();
    }

    boolean isPublic(HttpMethod method, String path) {
        if (publicPaths.stream().anyMatch(path::startsWith)) {
            return true;
        }
        boolean readOnly = HttpMethod.GET.equals(method) || HttpMethod.HEAD.equals(method)
                || HttpMethod.OPTIONS.equals(method);
        return readOnly && publicReadPaths.stream().anyMatch(path::startsWith);
    }

    private ServerWebExchange stripIdentityHeaders(ServerWebExchange exchange) {
        return exchange.mutate()
                .request(r -> r.headers(AuthFilter::removeIdentityHeaders))
                .build();
    }

    private static void removeIdentityHeaders(HttpHeaders headers) {
        headers.remove("X-User-Id");
        headers.remove("X-User-Email");
        headers.remove("X-User-Role");
    }

    private Mono<Void> onError(ServerWebExchange exchange, HttpStatus status) {
        exchange.getResponse().setStatusCode(status);
        return exchange.getResponse().setComplete();
    }

    public static class Config {}
}
