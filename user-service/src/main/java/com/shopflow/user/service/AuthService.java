package com.shopflow.user.service;

import com.shopflow.user.dto.AuthResponse;
import com.shopflow.user.dto.LoginRequest;
import com.shopflow.user.dto.RegisterRequest;
import com.shopflow.user.dto.UserResponse;
import com.shopflow.user.entity.User;
import com.shopflow.user.event.UserRegisteredApplicationEvent;
import com.shopflow.user.event.UserRegisteredEvent;
import com.shopflow.user.exception.EmailAlreadyExistsException;
import com.shopflow.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Locale;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;
    private final UserDetailsService userDetailsService;
    private final KafkaTemplate<String, UserRegisteredEvent> kafkaTemplate;
    private final ApplicationEventPublisher applicationEventPublisher;

    @Value("${kafka.topics.user-registered:user.registered}")
    private String userRegisteredTopic;

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        String email = normalizeEmail(request.getEmail());
        if (userRepository.existsByEmailIgnoreCase(email)) {
            throw new EmailAlreadyExistsException(email);
        }

        User user = User.builder()
                .name(request.getName().trim())
                .email(email)
                .password(passwordEncoder.encode(request.getPassword()))
                .role(User.Role.USER)
                .build();

        try {
            // Flush now so a concurrent registration with the same email surfaces as 409, not 500
            user = userRepository.saveAndFlush(user);
        } catch (DataIntegrityViolationException e) {
            throw new EmailAlreadyExistsException(email);
        }
        log.info("User registered: {}", user.getEmail());

        // Publish event — fire and forget, ignore failures when Kafka is unavailable
        try {
            kafkaTemplate.send(userRegisteredTopic, user.getId().toString(),
                    UserRegisteredEvent.builder()
                            .userId(user.getId())
                            .name(user.getName())
                            .email(user.getEmail())
                            .registeredAt(LocalDateTime.now())
                            .build())
                    .whenComplete((result, ex) -> {
                        if (ex != null) log.warn("Kafka unavailable, skipping user.registered event: {}", ex.getMessage());
                    });
        } catch (Exception e) {
            log.warn("Kafka send error: {}", e.getMessage());
        }

        applicationEventPublisher.publishEvent(
                new UserRegisteredApplicationEvent(this, user.getEmail(), user.getName()));

        String token = generateToken(user);
        return new AuthResponse(token, UserResponse.from(user));
    }

    public AuthResponse login(LoginRequest request) {
        String email = normalizeEmail(request.getEmail());
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(email, request.getPassword())
        );

        User user = userRepository.findByEmailIgnoreCase(email).orElseThrow();
        String token = generateToken(user);
        return new AuthResponse(token, UserResponse.from(user));
    }

    private static String normalizeEmail(String email) {
        return email == null ? null : email.trim().toLowerCase(Locale.ROOT);
    }

    private String generateToken(User user) {
        UserDetails userDetails = userDetailsService.loadUserByUsername(user.getEmail());
        return jwtService.generateToken(userDetails, Map.of(
                "userId", user.getId().toString(),
                "email", user.getEmail(),
                "role", user.getRole().name(),
                "name", user.getName()
        ));
    }
}
