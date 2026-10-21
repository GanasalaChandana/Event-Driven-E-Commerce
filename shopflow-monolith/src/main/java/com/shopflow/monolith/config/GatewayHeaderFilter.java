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
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
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

        // Clients must never be able to supply their own identity headers
        HttpServletRequest sanitized = new HeaderInjectingWrapper(request, null, null, null);

        String authHeader = request.getHeader("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            chain.doFilter(sanitized, response);
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

        chain.doFilter(sanitized, response);
    }

    /** Overrides X-User-* with verified values — or hides them entirely when the values are null. */
    private static class HeaderInjectingWrapper extends HttpServletRequestWrapper {

        private static final List<String> IDENTITY_HEADERS = List.of("X-User-Id", "X-User-Email", "X-User-Role");

        private final Map<String, String> verified = new HashMap<>();

        HeaderInjectingWrapper(HttpServletRequest request, String userId, String email, String role) {
            super(request);
            if (userId != null) verified.put("x-user-id", userId);
            if (email != null) verified.put("x-user-email", email);
            if (role != null) verified.put("x-user-role", role);
        }

        private static boolean isIdentityHeader(String name) {
            return name != null && IDENTITY_HEADERS.stream().anyMatch(h -> h.equalsIgnoreCase(name));
        }

        @Override
        public String getHeader(String name) {
            if (isIdentityHeader(name)) return verified.get(name.toLowerCase(Locale.ROOT));
            return super.getHeader(name);
        }

        @Override
        public Enumeration<String> getHeaders(String name) {
            if (isIdentityHeader(name)) {
                String val = verified.get(name.toLowerCase(Locale.ROOT));
                return val == null ? Collections.emptyEnumeration() : Collections.enumeration(List.of(val));
            }
            return super.getHeaders(name);
        }

        @Override
        public Enumeration<String> getHeaderNames() {
            List<String> names = new ArrayList<>();
            for (String n : Collections.list(super.getHeaderNames())) {
                if (!isIdentityHeader(n)) names.add(n);
            }
            verified.keySet().forEach(names::add);
            return Collections.enumeration(names);
        }
    }
}
