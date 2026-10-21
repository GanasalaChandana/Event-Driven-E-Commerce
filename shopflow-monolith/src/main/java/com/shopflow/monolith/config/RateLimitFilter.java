package com.shopflow.monolith.config;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Duration;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class RateLimitFilter extends OncePerRequestFilter {

    // Upper bound on tracked clients so a flood of distinct IPs can't exhaust memory
    private static final int MAX_TRACKED_CLIENTS = 50_000;

    private final ConcurrentHashMap<String, Bucket> authBuckets = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, Bucket> apiBuckets = new ConcurrentHashMap<>();

    /**
     * Header set by the edge proxy that carries the real client IP and that clients cannot
     * override. Render sits behind Cloudflare, which always overwrites CF-Connecting-IP.
     * Set RATE_LIMIT_CLIENT_IP_HEADER to an empty value to fall back to X-Forwarded-For.
     */
    @Value("${ratelimit.client-ip-header:CF-Connecting-IP}")
    private String clientIpHeader;

    private Bucket authBucket(String key) {
        return bucketFor(authBuckets, key, 10);
    }

    private Bucket apiBucket(String key) {
        return bucketFor(apiBuckets, key, 100);
    }

    private static Bucket bucketFor(ConcurrentHashMap<String, Bucket> buckets, String key, int perMinute) {
        if (buckets.size() > MAX_TRACKED_CLIENTS) {
            buckets.clear();
        }
        return buckets.computeIfAbsent(key, k ->
                Bucket.builder()
                        .addLimit(Bandwidth.builder().capacity(perMinute).refillGreedy(perMinute, Duration.ofMinutes(1)).build())
                        .build());
    }

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
                                    @NonNull HttpServletResponse response,
                                    @NonNull FilterChain chain) throws ServletException, IOException {
        String ip = resolveClientIp(request);
        String path = request.getRequestURI();

        boolean isAuth = path.startsWith("/api/v1/auth/");
        Bucket bucket = isAuth ? authBucket(ip) : apiBucket(ip);

        if (bucket.tryConsume(1)) {
            chain.doFilter(request, response);
        } else {
            response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.getWriter().write(
                    "{\"status\":429,\"title\":\"Too Many Requests\",\"detail\":\"Rate limit exceeded. Try again later.\"}"
            );
        }
    }

    String resolveClientIp(HttpServletRequest request) {
        if (clientIpHeader != null && !clientIpHeader.isBlank()) {
            String edgeIp = request.getHeader(clientIpHeader);
            if (edgeIp != null && !edgeIp.isBlank()) {
                return edgeIp.trim();
            }
        }
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
