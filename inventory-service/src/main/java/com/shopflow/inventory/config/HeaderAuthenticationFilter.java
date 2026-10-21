package com.shopflow.inventory.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;
import java.util.Locale;

/**
 * Builds the Spring Security principal from the identity headers set by the API Gateway
 * after it has verified the JWT. The gateway strips these headers from incoming client
 * requests, so this is only safe while the service is reachable solely through the gateway.
 *
 * Not a Spring bean on purpose: it is wired into this service's SecurityConfig only, so the
 * monolith (which verifies JWTs itself) never picks it up.
 */
public class HeaderAuthenticationFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
                                    @NonNull HttpServletResponse response,
                                    @NonNull FilterChain chain) throws ServletException, IOException {
        String email = request.getHeader("X-User-Email");
        String role = request.getHeader("X-User-Role");

        if (email != null && !email.isBlank() && role != null && !role.isBlank()
                && SecurityContextHolder.getContext().getAuthentication() == null) {
            var authority = new SimpleGrantedAuthority("ROLE_" + role.trim().toUpperCase(Locale.ROOT));
            var auth = new UsernamePasswordAuthenticationToken(email.trim(), null, List.of(authority));
            SecurityContextHolder.getContext().setAuthentication(auth);
        }
        chain.doFilter(request, response);
    }
}
