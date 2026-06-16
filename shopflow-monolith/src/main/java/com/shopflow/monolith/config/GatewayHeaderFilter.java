package com.shopflow.monolith.config;

import com.shopflow.user.service.JwtService;
import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class GatewayHeaderFilter extends OncePerRequestFilter {

    private final JwtService jwtService;

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
                                    @NonNull HttpServletResponse response,
                                    @NonNull FilterChain chain)
            throws ServletException, IOException {

        String authHeader = request.getHeader("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            chain.doFilter(request, response);
            return;
        }

        try {
            String token = authHeader.substring(7);
            Claims claims = jwtService.extractAllClaims(token);

            String userId = claims.get("userId", String.class);
            String email  = claims.get("email",  String.class);
            String role   = claims.get("role",   String.class);

            if (userId != null && email != null && role != null) {
                chain.doFilter(new HeaderInjectingWrapper(request, userId, email, role), response);
                return;
            }
        } catch (Exception e) {
            log.debug("Could not extract gateway headers from JWT: {}", e.getMessage());
        }

        chain.doFilter(request, response);
    }

    private static class HeaderInjectingWrapper extends HttpServletRequestWrapper {

        private final Map<String, String> extraHeaders;

        HeaderInjectingWrapper(HttpServletRequest request, String userId, String email, String role) {
            super(request);
            extraHeaders = new HashMap<>();
            extraHeaders.put("X-User-Id", userId);
            extraHeaders.put("X-User-Email", email);
            extraHeaders.put("X-User-Role", role);
        }

        @Override
        public String getHeader(String name) {
            String val = extraHeaders.get(name);
            return val != null ? val : super.getHeader(name);
        }

        @Override
        public Enumeration<String> getHeaders(String name) {
            String val = extraHeaders.get(name);
            if (val != null) return Collections.enumeration(Collections.singletonList(val));
            return super.getHeaders(name);
        }

        @Override
        public Enumeration<String> getHeaderNames() {
            java.util.List<String> names = Collections.list(super.getHeaderNames());
            names.addAll(extraHeaders.keySet());
            return Collections.enumeration(names);
        }
    }
}
